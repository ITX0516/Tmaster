import 'package:go_board/go_board.dart';
import 'analysis_types.dart';
import 'engine_config.dart';

/// Abstract interface for any Go AI engine.
///
/// Implementations:
/// - [KataGoEngine] — local KataGo via FFI
/// - [LeelaZeroEngine] — local Leela Zero via FFI
/// - [RemoteEngineProxy] — connects to cloud or personal PC
abstract class GoEngine {
  String get name;
  String get version;
  EngineType get type;

  /// Initialize the engine. Must be called before any other method.
  Future<void> initialize(EngineConfig config);

  /// Analyze a position. Returns a stream so partial results can be displayed
  /// as the engine searches deeper (Lizzie-style incremental updates).
  Stream<AnalysisResult> analyze(
    BoardState state, {
    int maxVariations,
    Duration? maxTime,
  });

  /// Request a single move (for play mode).
  Future<Move> generateMove(
    BoardState state, {
    Duration? timeLimit,
    double? temperature,
  });

  /// Whether the engine is initialized and ready.
  bool get isReady;

  /// Current engine configuration.
  EngineConfig get config;

  /// Release all resources (processes, connections, memory).
  Future<void> dispose();
}

/// Engine lifecycle state.
enum EngineState { uninitialized, initializing, ready, error, disposed }
