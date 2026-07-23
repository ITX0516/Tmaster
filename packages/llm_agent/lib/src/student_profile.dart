/// Long-term student profile for personalized teaching.
final class StudentProfile {
  final String id;
  final String name;
  final String level;           // e.g. "18k", "1d", "7d"
  final Map<String, double> skillScores; // fuseki, chuban, shuban, etc.
  final List<WeakPointRecord> weakPoints;
  final List<TrainingPlan> activePlans;
  final GameStats stats;

  const StudentProfile({
    required this.id,
    required this.name,
    this.level = 'unknown',
    this.skillScores = const {},
    this.weakPoints = const [],
    this.activePlans = const [],
    this.stats = const GameStats(),
  });

  StudentProfile copyWith({
    String? level,
    Map<String, double>? skillScores,
    List<WeakPointRecord>? weakPoints,
    List<TrainingPlan>? activePlans,
    GameStats? stats,
  }) {
    return StudentProfile(
      id: id,
      name: name,
      level: level ?? this.level,
      skillScores: skillScores ?? this.skillScores,
      weakPoints: weakPoints ?? this.weakPoints,
      activePlans: activePlans ?? this.activePlans,
      stats: stats ?? this.stats,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'level': level,
        'skillScores': skillScores,
        'weakPoints': weakPoints.map((w) => w.toJson()).toList(),
        'activePlans': activePlans.map((p) => p.toJson()).toList(),
        'stats': stats.toJson(),
      };

  factory StudentProfile.fromJson(Map<String, dynamic> json) => StudentProfile(
        id: json['id'] as String,
        name: json['name'] as String,
        level: json['level'] as String? ?? 'unknown',
        skillScores: (json['skillScores'] as Map<String, dynamic>?)
                ?.map((k, v) => MapEntry(k, (v as num).toDouble())) ??
            {},
        weakPoints: (json['weakPoints'] as List?)
                ?.map((e) =>
                    WeakPointRecord.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        activePlans: (json['activePlans'] as List?)
                ?.map(
                    (e) => TrainingPlan.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        stats: json['stats'] != null
            ? GameStats.fromJson(json['stats'] as Map<String, dynamic>)
            : const GameStats(),
      );
}

/// A recurring weakness identified across multiple games.
final class WeakPointRecord {
  final String category;
  final int frequency;
  final List<String> exampleGameIds;
  final DateTime lastSeen;

  const WeakPointRecord({
    required this.category,
    required this.frequency,
    this.exampleGameIds = const [],
    required this.lastSeen,
  });

  Map<String, dynamic> toJson() => {
        'category': category,
        'frequency': frequency,
        'exampleGameIds': exampleGameIds,
        'lastSeen': lastSeen.toIso8601String(),
      };

  factory WeakPointRecord.fromJson(Map<String, dynamic> json) =>
      WeakPointRecord(
        category: json['category'] as String,
        frequency: json['frequency'] as int,
        exampleGameIds: (json['exampleGameIds'] as List?)
                ?.map((e) => e.toString())
                .toList() ??
            [],
        lastSeen: DateTime.parse(json['lastSeen'] as String),
      );
}

/// A training plan the student is currently working on.
final class TrainingPlan {
  final String id;
  final String title;
  final String description;
  final String focusArea;
  final DateTime created;
  final DateTime? completed;
  final double progress; // 0.0 to 1.0

  const TrainingPlan({
    required this.id,
    required this.title,
    required this.description,
    required this.focusArea,
    required this.created,
    this.completed,
    this.progress = 0.0,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'description': description,
        'focusArea': focusArea,
        'created': created.toIso8601String(),
        'completed': completed?.toIso8601String(),
        'progress': progress,
      };

  factory TrainingPlan.fromJson(Map<String, dynamic> json) => TrainingPlan(
        id: json['id'] as String,
        title: json['title'] as String,
        description: json['description'] as String,
        focusArea: json['focusArea'] as String,
        created: DateTime.parse(json['created'] as String),
        completed: json['completed'] != null
            ? DateTime.parse(json['completed'] as String)
            : null,
        progress: (json['progress'] as num?)?.toDouble() ?? 0.0,
      );
}

/// Summary statistics for the student.
final class GameStats {
  final int totalGames;
  final int wins;
  final int losses;
  final int draws;
  final DateTime? firstGame;
  final DateTime? lastGame;

  const GameStats({
    this.totalGames = 0,
    this.wins = 0,
    this.losses = 0,
    this.draws = 0,
    this.firstGame,
    this.lastGame,
  });

  double get winRate => totalGames > 0 ? wins / totalGames : 0.0;

  Map<String, dynamic> toJson() => {
        'totalGames': totalGames,
        'wins': wins,
        'losses': losses,
        'draws': draws,
        'firstGame': firstGame?.toIso8601String(),
        'lastGame': lastGame?.toIso8601String(),
      };

  factory GameStats.fromJson(Map<String, dynamic> json) => GameStats(
        totalGames: json['totalGames'] as int? ?? 0,
        wins: json['wins'] as int? ?? 0,
        losses: json['losses'] as int? ?? 0,
        draws: json['draws'] as int? ?? 0,
        firstGame: json['firstGame'] != null
            ? DateTime.parse(json['firstGame'] as String)
            : null,
        lastGame: json['lastGame'] != null
            ? DateTime.parse(json['lastGame'] as String)
            : null,
      );
}
