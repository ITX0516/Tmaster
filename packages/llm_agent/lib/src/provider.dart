/// Abstract LLM provider for multi-modal chat completion.
///
/// Implementations: OpenAI, Anthropic, local models (via Ollama/etc.)
abstract class LlmProvider {
  String get modelId;

  /// Send a chat completion request with optional images and tools.
  ///
  /// [messages] is a list of message maps with "role" and "content".
  /// Content can be text or image arrays (for multi-modal).
  /// [tools] is the list of available tool definitions (from [ToolRegistry]).
  /// Returns the LLM's text response and/or tool calls.
  Future<LlmResponse> chat({
    required List<Map<String, dynamic>> messages,
    List<Map<String, dynamic>>? tools,
    String? systemPrompt,
    double temperature = 0.7,
    int maxTokens = 2048,
  });
}

/// Unified response from any LLM provider.
final class LlmResponse {
  final String? textContent;
  final List<LlmToolCall>? toolCalls;

  const LlmResponse({this.textContent, this.toolCalls});

  bool get hasToolCalls => toolCalls != null && toolCalls!.isNotEmpty;
}

/// A tool call requested by the LLM.
final class LlmToolCall {
  final String id;
  final String name;
  final Map<String, dynamic> arguments;

  const LlmToolCall({
    required this.id,
    required this.name,
    required this.arguments,
  });
}

/// Simple message builders.
class ChatMessage {
  static Map<String, dynamic> system(String content) => {
        'role': 'system',
        'content': content,
      };

  static Map<String, dynamic> user(String content) => {
        'role': 'user',
        'content': content,
      };

  /// Multi-modal user message with text + images.
  static Map<String, dynamic> userWithImages(
    String text,
    List<String> base64Images, [
    String imageType = 'image/png',
  ]) {
    final content = <Map<String, dynamic>>[
      {'type': 'text', 'text': text},
      for (final img in base64Images)
        {
          'type': 'image_url',
          'image_url': {'url': 'data:$imageType;base64,$img'},
        },
    ];
    return {'role': 'user', 'content': content};
  }

  static Map<String, dynamic> assistant(String content) => {
        'role': 'assistant',
        'content': content,
      };

  static Map<String, dynamic> toolResult(
      String toolCallId, String content) => {
        'role': 'tool',
        'tool_call_id': toolCallId,
        'content': content,
      };
}
