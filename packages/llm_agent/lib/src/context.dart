import 'package:analysis_core/analysis_core.dart';

/// Context assembled for the LLM before each analysis/tutoring request.
///
/// This bundles everything the LLM needs to produce a useful teaching response:
/// - Board state and screenshot
/// - KataGo analysis data (raw JSON)
/// - Current move info, candidates, win-rate changes
/// - Knowledge cards from local library
/// - Student profile and recent weaknesses
final class AgentContext {
  /// SGF string for the current position.
  final String? currentSgf;

  /// Base64-encoded PNG of the current board.
  final String? boardImageBase64;

  /// The raw analysis result from KataGo.
  final AnalysisResult? analysisResult;

  /// The actual move played (if analyzing a game).
  final String? actualMove; // SGF coordinate

  /// Top candidate moves with win-rate / score lead.
  final String? candidateSummary;

  /// Win-rate delta between before and after the actual move.
  final double? winRateDelta;

  /// Relevant knowledge cards (2–4) retrieved from the local library.
  final List<KnowledgeCardRef> knowledgeCards;

  /// Student profile summary.
  final StudentProfileSnapshot? studentProfile;

  /// Recent common problems for this student.
  final List<String>? recentWeaknesses;

  /// The specific question the user asked (if any).
  final String? userQuestion;

  const AgentContext({
    this.currentSgf,
    this.boardImageBase64,
    this.analysisResult,
    this.actualMove,
    this.candidateSummary,
    this.winRateDelta,
    this.knowledgeCards = const [],
    this.studentProfile,
    this.recentWeaknesses,
    this.userQuestion,
  });

  /// Builds the prompt context for the LLM.
  String buildTextContext() {
    final buf = StringBuffer();
    buf.writeln('## Current Position Analysis');
    if (analysisResult != null) {
      buf.writeln('- Move number: ${analysisResult!.moveNumber}');
      buf.writeln('- Win rate (current player): ${_fmtPercent(analysisResult!.rootWinRate)}');
      buf.writeln('- Score lead: ${analysisResult!.rootScoreLead.toStringAsFixed(1)} points');
      if (candidateSummary != null) {
        buf.writeln('- Top candidates: $candidateSummary');
      }
      if (actualMove != null) {
        buf.writeln('- Actual move played: $actualMove');
      }
      if (winRateDelta != null) {
        buf.writeln('- Win-rate change: ${_fmtPercent(winRateDelta!)}');
      }
    }

    if (knowledgeCards.isNotEmpty) {
      buf.writeln('\n## Relevant Knowledge Cards');
      for (final card in knowledgeCards) {
        buf.writeln('- [${card.title}] ${card.summary}');
      }
    }

    if (studentProfile != null) {
      buf.writeln('\n## Student Profile');
      buf.writeln('- Estimated level: ${studentProfile!.level}');
      buf.writeln('- Skill scores: ${studentProfile!.skillScores}');
      if (recentWeaknesses != null && recentWeaknesses!.isNotEmpty) {
        buf.writeln('- Recent weaknesses: ${recentWeaknesses!.join(", ")}');
      }
    }

    if (userQuestion != null) {
      buf.writeln('\n## Student Question');
      buf.writeln(userQuestion);
    }

    return buf.toString();
  }

  String _fmtPercent(double v) => '${(v * 100).toStringAsFixed(1)}%';
}

/// Lightweight reference to a knowledge card (summary only for context).
final class KnowledgeCardRef {
  final String id;
  final String title;
  final String summary;
  final String category;

  const KnowledgeCardRef({
    required this.id,
    required this.title,
    required this.summary,
    required this.category,
  });
}

/// Snapshot of student profile included in agent context.
final class StudentProfileSnapshot {
  final String id;
  final String name;
  final String level;
  final Map<String, double> skillScores;
  final int totalGamesAnalyzed;

  const StudentProfileSnapshot({
    required this.id,
    required this.name,
    required this.level,
    required this.skillScores,
    required this.totalGamesAnalyzed,
  });
}
