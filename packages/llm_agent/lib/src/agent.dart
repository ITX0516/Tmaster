import 'dart:convert';
import 'context.dart';
import 'provider.dart';
import 'tool.dart';

/// The AI Go Teacher — an LLM-powered agent that can call tools.
///
/// It receives the analysis context (board screenshot, KataGo data, knowledge
/// cards, student profile), and produces human-readable teaching feedback.
///
/// The agent uses tool-calling to:
/// - Search the game library for similar positions
/// - Read SGF records
/// - Request deeper KataGo analysis on specific variations
/// - Retrieve knowledge cards
/// - Read/write student profiles
/// - Save analysis reports
///
/// Usage:
/// ```dart
/// final agent = GoTeacherAgent(
///   provider: openAiProvider,
///   registry: toolRegistry,
///   systemPrompt: teachingPrompt,
/// );
/// final response = await agent.teach(context: agentContext);
/// ```
class GoTeacherAgent {
  final LlmProvider _provider;
  final ToolRegistry _registry;
  final String _systemPrompt;
  final int _maxToolRounds;

  GoTeacherAgent({
    required LlmProvider provider,
    required ToolRegistry registry,
    String? systemPrompt,
    int maxToolRounds = 5,
  })  : _provider = provider,
        _registry = registry,
        _systemPrompt = systemPrompt ?? _defaultSystemPrompt,
        _maxToolRounds = maxToolRounds;

  /// Main entry point: given the analysis context, produce teaching feedback.
  ///
  /// Returns a stream of text chunks so the UI can display progressively.
  Stream<String> teach({required AgentContext context}) async* {
    final messages = <Map<String, dynamic>>[
      ChatMessage.system(_systemPrompt),
      ChatMessage.user(context.buildTextContext()),
    ];

    // Add board image if available
    if (context.boardImageBase64 != null) {
      messages.add(ChatMessage.userWithImages(
        'Here is the current board position:',
        [context.boardImageBase64!],
      ));
    }

    // Agent loop: LLM may call tools, then we execute and continue
    for (var round = 0; round < _maxToolRounds; round++) {
      final response = await _provider.chat(
        messages: messages,
        tools: _registry.toFunctionDefinitions(),
        temperature: 0.7,
        maxTokens: 2048,
      );

      // If the LLM wants to call tools
      if (response.hasToolCalls) {
        // Add assistant's tool call to message history
        for (final tc in response.toolCalls!) {
          messages.add({
            'role': 'assistant',
            'content': null,
            'tool_calls': [
              {
                'id': tc.id,
                'type': 'function',
                'function': {
                  'name': tc.name,
                  'arguments': jsonEncode(tc.arguments),
                },
              },
            ],
          });
        }

        // Execute tools and add results
        for (final tc in response.toolCalls!) {
          final tool = _registry.get(tc.name);
          String result;
          if (tool == null) {
            result = 'Error: unknown tool "${tc.name}"';
          } else {
            try {
              final output = await tool.execute(tc.arguments);
              result = output.toString();
            } catch (e) {
              result = 'Tool error: $e';
            }
          }
          messages.add(ChatMessage.toolResult(tc.id, result));
        }

        // Continue loop — LLM sees tool results
        continue;
      }

      // LLM produced a text response — emit it
      if (response.textContent != null && response.textContent!.isNotEmpty) {
        yield response.textContent!;
      }
      return; // Done
    }
  }

  static const _defaultSystemPrompt = '''
You are a professional Go teacher. Your role is to help students understand
their games and improve.

GUIDELINES:
1. Be patient and encouraging. Focus on 1-2 key lessons per position.
2. Explain WHY a move was good or bad, not just THAT it was good or bad.
3. Use the KataGo analysis data to support your explanations with concrete
   win-rate changes and variations.
4. Refer to the knowledge cards provided to give students learning resources.
5. Tailor your explanation to the student's level and recent weaknesses.
6. When the student has a specific question, answer it directly first, then
   provide broader context.
7. Keep explanations concise. For beginners, avoid overly complex variations.
   For stronger players, include deeper reading.
8. End with one actionable takeaway the student can practice this week.

FORMAT:
- Start with a brief assessment of the position.
- Point out the key moment or decision.
- Explain what happened and why.
- Show a better alternative if applicable.
- Recommend 1 specific thing to work on.
''';
}
