/// The type of engine backend.
enum EngineType {
  /// Runs on-device (KataGo / Leela Zero compiled for ARM).
  local,

  /// Connects to a remote compute host (cloud GPU or personal PC).
  remote,
}

/// Configuration passed to an engine at initialization.
final class EngineConfig {
  final EngineType type;

  /// Path to the engine binary or weights file (local), or host URL (remote).
  final String? enginePath;

  /// Path to the neural network weights file. Only used for local engines.
  final String? weightsPath;

  /// Number of threads for MCTS search. Default: device CPU count.
  final int? threads;

  /// Maximum memory (MB) to use for the search tree. 0 = unbounded.
  final int? maxMemoryMb;

  /// Remote host connection string (e.g. "192.168.1.5:8080" or "wss://cloud.example.com").
  final String? host;

  /// Authentication token for cloud platforms (智星云, 算云, etc.).
  final String? authToken;

  /// Maximum number of visits per move (0 = unlimited, limited by time).
  final int? maxVisits;

  /// Playout limit per move (0 = unlimited).
  final int? maxPlayouts;

  /// Any engine-specific parameters serialized as JSON.
  final Map<String, dynamic>? extra;

  const EngineConfig({
    this.type = EngineType.local,
    this.enginePath,
    this.weightsPath,
    this.threads,
    this.maxMemoryMb,
    this.host,
    this.authToken,
    this.maxVisits,
    this.maxPlayouts,
    this.extra,
  });

  EngineConfig copyWith({
    EngineType? type,
    String? enginePath,
    String? weightsPath,
    int? threads,
    int? maxMemoryMb,
    String? host,
    String? authToken,
    int? maxVisits,
    int? maxPlayouts,
    Map<String, dynamic>? extra,
  }) {
    return EngineConfig(
      type: type ?? this.type,
      enginePath: enginePath ?? this.enginePath,
      weightsPath: weightsPath ?? this.weightsPath,
      threads: threads ?? this.threads,
      maxMemoryMb: maxMemoryMb ?? this.maxMemoryMb,
      host: host ?? this.host,
      authToken: authToken ?? this.authToken,
      maxVisits: maxVisits ?? this.maxVisits,
      maxPlayouts: maxPlayouts ?? this.maxPlayouts,
      extra: extra ?? this.extra,
    );
  }
}
