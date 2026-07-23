import '../tool.dart';

/// Factory that creates the full set of built-in tools for the Go teacher agent.
///
/// These tools are provided with callbacks so they can interact with the
/// app's services (game library, SGF parser, KataGo engine, knowledge base,
/// student profile store, report saver).
class BuiltinTools {
  /// Creates all tools with the given service callbacks.
  static List<Tool> createAll({
    required Future<List<Map<String, dynamic>>> Function(Map<String, String> filters)
        findGames,
    required Future<Map<String, dynamic>> Function(String sgfPath)
        readGameRecord,
    required Future<Map<String, dynamic>> Function(String sgfPath, {int moveNumber})
        analyzePosition,
    required Future<Map<String, dynamic>> Function(List<String> sgfPaths)
        analyzeGameBatch,
    required Future<String> Function({int moveNumber, bool showCoordinates, bool showCandidates})
        captureTeachingImage,
    required Future<List<Map<String, dynamic>>> Function(String query, {int limit})
        searchKnowledge,
    required Future<Map<String, dynamic>> Function() readStudentProfile,
    required Future<void> Function(Map<String, dynamic> profile)
        writeStudentProfile,
    required Future<String> Function(Map<String, dynamic> report)
        saveAnalysis,
  }) {
    return [
      Tool(
        name: 'library.findGames',
        description:
            'Search the game library by player name, source, date range, or recent N games.',
        parameters: {
          'player': const PropertySchema(
              type: 'string',
              description: 'Player name to filter by',
              required: false),
          'source': const PropertySchema(
              type: 'string',
              description: 'Source of the games (e.g. "fox", "tygem", "ogs")',
              required: false),
          'date_from': const PropertySchema(
              type: 'string',
              description: 'Start date (YYYY-MM-DD)',
              required: false),
          'date_to': const PropertySchema(
              type: 'string',
              description: 'End date (YYYY-MM-DD)',
              required: false),
          'limit': const PropertySchema(
              type: 'integer',
              description: 'Max number of games to return (default 10)',
              required: false),
        },
        execute: (args) => findGames(Map<String, String>.from(
            args.map((k, v) => MapEntry(k, v.toString())))),
      ),

      Tool(
        name: 'sgf.readGameRecord',
        description:
            'Read an SGF file and return the main line, game info, and current position.',
        parameters: {
          'sgf_path': const PropertySchema(
              type: 'string',
              description: 'Path to the SGF file',
              required: true),
        },
        execute: (args) => readGameRecord(args['sgf_path'] as String),
      ),

      Tool(
        name: 'katago.analyzePosition',
        description:
            'Analyze the current position with KataGo. Returns win rate, score lead, and top candidate moves.',
        parameters: {
          'sgf_path': const PropertySchema(
              type: 'string',
              description: 'Path to the SGF file',
              required: true),
          'move_number': const PropertySchema(
              type: 'integer',
              description: 'Move number to analyze (default: last move)',
              required: false),
        },
        execute: (args) => analyzePosition(
          args['sgf_path'] as String,
          moveNumber: (args['move_number'] as int?) ?? -1,
        ),
      ),

      Tool(
        name: 'katago.analyzeGameBatch',
        description:
            'Batch analyze one or more complete games with KataGo to find problem moves.',
        parameters: {
          'sgf_paths': const PropertySchema(
              type: 'array',
              description: 'List of SGF file paths to analyze',
              required: true),
        },
        execute: (args) {
          final paths = (args['sgf_paths'] as List)
              .map((e) => e.toString())
              .toList();
          return analyzeGameBatch(paths);
        },
      ),

      Tool(
        name: 'board.captureTeachingImage',
        description:
            'Generate a screenshot of the current board position with coordinates, last move marker, and AI recommended points.',
        parameters: {
          'move_number': const PropertySchema(
              type: 'integer',
              description: 'Move number to capture (default: last)',
              required: false),
          'show_coordinates': const PropertySchema(
              type: 'boolean',
              description: 'Show coordinate labels (default: true)',
              required: false),
          'show_candidates': const PropertySchema(
              type: 'boolean',
              description: 'Show AI candidate markers (default: true)',
              required: false),
        },
        execute: (args) => captureTeachingImage(
          moveNumber: args['move_number'] as int?,
          showCoordinates: (args['show_coordinates'] as bool?) ?? true,
          showCandidates: (args['show_candidates'] as bool?) ?? true,
        ),
      ),

      Tool(
        name: 'knowledge.searchLocal',
        description:
            'Search the local Go knowledge card library for relevant teaching materials.',
        parameters: {
          'query': const PropertySchema(
              type: 'string',
              description: 'Search query (e.g. "3-3 invasion joseki")',
              required: true),
          'limit': const PropertySchema(
              type: 'integer',
              description: 'Max cards to return (default 4)',
              required: false),
        },
        execute: (args) => searchKnowledge(
          args['query'] as String,
          limit: (args['limit'] as int?) ?? 4,
        ),
      ),

      Tool(
        name: 'studentProfile.read',
        description:
            'Read the student\'s long-term profile including level, skill scores, and recent weaknesses.',
        parameters: {},
        execute: (_) => readStudentProfile(),
      ),

      Tool(
        name: 'studentProfile.write',
        description:
            'Update the student profile with new skill scores, weaknesses, or training plans.',
        parameters: {
          'profile': const PropertySchema(
              type: 'object',
              description: 'Updated profile fields',
              required: true),
        },
        execute: (args) =>
            writeStudentProfile(args['profile'] as Map<String, dynamic>),
      ),

      Tool(
        name: 'report.saveAnalysis',
        description:
            'Save an analysis report for the current move, full game, or multi-game training plan.',
        parameters: {
          'type': const PropertySchema(
            type: 'string',
            description: 'Report type',
            enumValues: ['current_move', 'full_game', 'multi_game', 'training_plan'],
          ),
          'content': const PropertySchema(
              type: 'string',
              description: 'Report content (markdown)',
              required: true),
          'sgf_path': const PropertySchema(
              type: 'string',
              description: 'Associated SGF path',
              required: false),
        },
        execute: (args) => saveAnalysis(args),
      ),
    ];
  }
}
