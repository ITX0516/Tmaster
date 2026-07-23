import 'package:go_board/go_board.dart';
import 'analysis_types.dart';
import 'engine_interface.dart';

/// Hawk-Eye: full-spectrum game analyzer.
///
/// Given a completed game (SGF) and an engine, it:
/// 1. Analyzes every move in the game
/// 2. Identifies problem moves (big win-rate drops)
/// 3. Highlights good moves (matching AI top choice)
/// 4. Calculates AI match rate
///
/// Usage:
/// ```dart
/// final hawkEye = HawkEyeAnalyzer(engine);
/// final summary = await hawkEye.analyzeGame(boardState);
/// ```
class HawkEyeAnalyzer {
  final GoEngine _engine;
  final HawkEyeConfig _config;

  HawkEyeAnalyzer(this._engine, {HawkEyeConfig? config})
      : _config = config ?? const HawkEyeConfig();

  /// Analyze an entire game from the initial [boardState] through all moves.
  Future<GameAnalysisSummary> analyzeGame(BoardState initialBoard) async {
    final analyses = <AnalysisResult>[];
    final problemMoves = <ProblemMove>[];

    // Walk through every position in the game
    BoardState current = initialBoard;
    for (var i = 0; i <= initialBoard.moveCount; i++) {
      // Navigate to move i
      current = _navigateToMove(initialBoard, i);

      final result = await _engine
          .analyze(current, maxVariations: _config.topCandidates)
          .last; // Take final result

      analyses.add(result);

      // Compare actual next move with AI recommendation
      if (i < initialBoard.moveCount) {
        final actualMove = initialBoard.moveHistory[i];
        if (!actualMove.isPass) {
          final problem = _detectProblemMove(
            actualMove,
            result,
            moveNumber: i,
          );
          if (problem != null) problemMoves.add(problem);
        }
      }
    }

    return _summarize(initialBoard, analyses, problemMoves);
  }

  /// Analyze a single position, comparing the actual move to AI recommendations.
  ProblemMove? analyzeSingleMove(
    BoardState state,
    Move actualMove,
    int moveNumber,
  ) async {
    final result = await _engine
        .analyze(state, maxVariations: _config.topCandidates)
        .last;
    return _detectProblemMove(actualMove, result, moveNumber: moveNumber);
  }

  ProblemMove? _detectProblemMove(
    Move actual,
    AnalysisResult result, {
    required int moveNumber,
  }) {
    if (actual.isPass || result.candidates.isEmpty) return null;

    final bestCandidate = result.candidates.first;

    // Find the actual move among candidates
    final actualCandidate = result.candidates
        .where((c) => c.coordinate == actual.coordinate)
        .firstOrNull;

    double winRateLoss;
    double scoreLoss;

    if (actualCandidate != null) {
      winRateLoss = actualCandidate.winRateLoss(bestCandidate.winRate);
      scoreLoss = bestCandidate.scoreLead - actualCandidate.scoreLead;
    } else {
      // Actual move not in top candidates — assume it's worse than the worst candidate
      final worst = result.candidates.last;
      winRateLoss = worst.winRateLoss(bestCandidate.winRate) + 0.05;
      scoreLoss = _config.defaultScoreLoss;
    }

    final severity = _classifySeverity(winRateLoss);
    if (severity == ProblemSeverity.minor && !_config.reportMinor) return null;

    return ProblemMove(
      moveNumber: moveNumber,
      actualMove: actual.coordinate,
      bestCandidate: bestCandidate,
      winRateLoss: winRateLoss,
      scoreLoss: scoreLoss,
      severity: severity,
      category: _guessCategory(actual.coordinate, bestCandidate.coordinate, winRateLoss),
    );
  }

  ProblemSeverity _classifySeverity(double winRateLoss) {
    if (winRateLoss < _config.minorThreshold) return ProblemSeverity.minor;
    if (winRateLoss < _config.moderateThreshold) return ProblemSeverity.moderate;
    if (winRateLoss < _config.criticalThreshold) return ProblemSeverity.major;
    return ProblemSeverity.critical;
  }

  ProblemCategory _guessCategory(
      Coordinate actual, Coordinate best, double loss) {
    // Simple heuristic based on coordinate distance
    final dx = (actual.x - best.x).abs();
    final dy = (actual.y - best.y).abs();
    if (dx + dy <= 1) return ProblemCategory.shape;
    if (dx + dy <= 3) return ProblemCategory.direction;
    if (loss > 0.15) return ProblemCategory.lifeAndDeath;
    return ProblemCategory.other;
  }

  GameAnalysisSummary _summarize(
    BoardState initialBoard,
    List<AnalysisResult> analyses,
    List<ProblemMove> problems,
  ) {
    final totalMoves = initialBoard.moveCount;
    double totalLoss = 0;
    int aiMatches = 0;
    double blackWr = 0.5, whiteWr = 0.5;
    int blackCount = 0, whiteCount = 0;

    for (final a in analyses) {
      if (a.candidates.isNotEmpty) {
        // Check if player matched AI top choice
        if (a.moveNumber < totalMoves) {
          final actual = initialBoard.moveHistory[a.moveNumber];
          if (!actual.isPass &&
              a.candidates.first.coordinate == actual.coordinate) {
            aiMatches++;
          }
        }
      }

      // Track win-rate by color
      final player =
          a.moveNumber % 2 == 0 ? StoneColor.black : StoneColor.white;
      if (player == StoneColor.black) {
        blackWr = a.rootWinRate;
        blackCount++;
      } else {
        whiteWr = a.rootWinRate;
        whiteCount++;
      }
    }

    for (final p in problems) {
      totalLoss += p.winRateLoss;
    }

    return GameAnalysisSummary(
      totalMoves: totalMoves,
      moveAnalyses: analyses,
      problemMoves: problems,
      averageWinRateLoss: problems.isEmpty ? 0 : totalLoss / problems.length,
      aiMatchRate: totalMoves == 0 ? 0 : aiMatches / totalMoves,
      blackAvgWinRate: blackCount > 0 ? blackWr : 0.5,
      whiteAvgWinRate: whiteCount > 0 ? whiteWr : 0.5,
    );
  }

  BoardState _navigateToMove(BoardState initial, int target) {
    BoardState state = BoardState.empty(
      boardSize: initial.boardSize,
      komi: initial.komi,
    );
    for (var i = 0; i < target && i < initial.moveHistory.length; i++) {
      final m = initial.moveHistory[i];
      final next = m.isPass ? state.pass() : state.playMove(m.coordinate);
      if (next == null) break;
      state = next;
    }
    return state;
  }
}

/// Configuration for the Hawk-Eye analyzer.
class HawkEyeConfig {
  final int topCandidates;
  final bool reportMinor;

  /// Thresholds for problem severity (win-rate loss).
  final double minorThreshold;    // default 0.02
  final double moderateThreshold; // default 0.05
  final double criticalThreshold; // default 0.15

  /// Default score loss when actual move is not in candidate list.
  final double defaultScoreLoss;

  const HawkEyeConfig({
    this.topCandidates = 5,
    this.reportMinor = false,
    this.minorThreshold = 0.02,
    this.moderateThreshold = 0.05,
    this.criticalThreshold = 0.15,
    this.defaultScoreLoss = 10.0,
  });
}
