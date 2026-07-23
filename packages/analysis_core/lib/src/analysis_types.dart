import 'package:go_board/go_board.dart';

/// A single candidate move returned by the AI engine.
final class MoveCandidate {
  final Coordinate coordinate;
  final double winRate;    // 0.0–1.0, from current player's perspective
  final double scoreLead;  // positive = current player leads
  final int visits;        // number of MCTS visits
  final int order;         // 1 = best, 2 = second best, etc.
  final List<VariationNode> principalVariation;

  const MoveCandidate({
    required this.coordinate,
    required this.winRate,
    required this.scoreLead,
    required this.visits,
    required this.order,
    this.principalVariation = const [],
  });

  /// Loss in win-rate compared to the best move (for problem-move detection).
  double winRateLoss(double bestWinRate) => bestWinRate - winRate;
}

/// One node in a variation tree.
final class VariationNode {
  final Coordinate move;
  final StoneColor player;
  final double winRate;
  final double scoreLead;
  final int visits;

  const VariationNode({
    required this.move,
    required this.player,
    required this.winRate,
    required this.scoreLead,
    required this.visits,
  });
}

/// The result of analyzing one position.
final class AnalysisResult {
  final int moveNumber;
  final List<MoveCandidate> candidates; // sorted by order (best first)
  final double rootWinRate;             // from current player's view
  final double rootScoreLead;
  final int totalVisits;
  final int depth;                      // max depth searched
  final List<VariationNode> principalVariation;

  const AnalysisResult({
    required this.moveNumber,
    required this.candidates,
    required this.rootWinRate,
    required this.rootScoreLead,
    required this.totalVisits,
    required this.depth,
    this.principalVariation = const [],
  });

  /// The best move candidate, if any.
  MoveCandidate? get bestMove =>
      candidates.isNotEmpty ? candidates.first : null;
}

/// Statistics from a full-game analysis.
final class GameAnalysisSummary {
  final int totalMoves;
  final List<AnalysisResult> moveAnalyses;
  final List<ProblemMove> problemMoves;
  final double averageWinRateLoss;
  final double aiMatchRate; // 0.0–1.0
  final double blackAvgWinRate;
  final double whiteAvgWinRate;
  final String? gameCommentary;

  const GameAnalysisSummary({
    required this.totalMoves,
    required this.moveAnalyses,
    required this.problemMoves,
    required this.averageWinRateLoss,
    required this.aiMatchRate,
    required this.blackAvgWinRate,
    required this.whiteAvgWinRate,
    this.gameCommentary,
  });
}

/// Severity classification for a problem move.
enum ProblemSeverity {
  minor,     // < 2% win-rate loss
  moderate,  // 2–5%
  major,     // 5–15%
  critical,  // > 15%
}

/// A detected problem move.
final class ProblemMove {
  final int moveNumber;
  final Coordinate actualMove;
  final MoveCandidate? bestCandidate;
  final double winRateLoss;
  final double scoreLoss;
  final ProblemSeverity severity;
  final ProblemCategory category;

  const ProblemMove({
    required this.moveNumber,
    required this.actualMove,
    this.bestCandidate,
    required this.winRateLoss,
    required this.scoreLoss,
    required this.severity,
    required this.category,
  });
}

/// Categorizes what kind of mistake was made.
enum ProblemCategory {
  direction,      // wrong strategic direction
  lifeAndDeath,   // reading / life-and-death mistake
  joseki,         // joseki deviation
  endgame,        // suboptimal endgame
  shape,          // bad shape
  tenuki,         // missed urgent move
  senteGote,      // sente/gote misjudgment
  overplay,       // too aggressive
  passive,        // too passive
  other,
}
