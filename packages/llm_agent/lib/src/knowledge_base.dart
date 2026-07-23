/// A Go knowledge card stored in the local library.
///
/// Knowledge cards cover specific Go concepts: joseki, life-and-death,
/// fuseki, tesuji, etc. Each card has tags and a difficulty level so
/// the agent can retrieve the most relevant ones for a given position.
final class KnowledgeCard {
  final String id;
  final String title;
  final String content;       // Full educational content (markdown)
  final List<String> tags;    // e.g. ["joseki", "3-3", "invasion"]
  final String category;      // "joseki", "fuseki", "life_and_death", etc.
  final KnowledgeLevel level; // target audience
  final List<String>? sgfHashes; // position fingerprints for matching

  const KnowledgeCard({
    required this.id,
    required this.title,
    required this.content,
    required this.tags,
    required this.category,
    this.level = KnowledgeLevel.intermediate,
    this.sgfHashes,
  });
}

enum KnowledgeLevel { beginner, intermediate, advanced }

/// Interface for the local knowledge base.
abstract class KnowledgeBase {
  /// Full-text search across cards.
  Future<List<KnowledgeCard>> search(String query, {int limit = 4});

  /// Find cards matching a specific position hash.
  Future<List<KnowledgeCard>> searchByPosition(String positionHash);

  /// Add a card to the library.
  Future<void> addCard(KnowledgeCard card);

  /// Remove a card.
  Future<void> removeCard(String id);

  /// Get all categories.
  Future<List<String>> getCategories();
}

/// In-memory implementation for development/testing.
class InMemoryKnowledgeBase implements KnowledgeBase {
  final List<KnowledgeCard> _cards = [];

  @override
  Future<List<KnowledgeCard>> search(String query, {int limit = 4}) async {
    final lowerQuery = query.toLowerCase();
    final results = _cards.where((card) {
      if (card.title.toLowerCase().contains(lowerQuery)) return true;
      if (card.tags.any((t) => t.toLowerCase().contains(lowerQuery))) return true;
      if (card.category.toLowerCase().contains(lowerQuery)) return true;
      return false;
    }).toList();
    return results.take(limit).toList();
  }

  @override
  Future<List<KnowledgeCard>> searchByPosition(String positionHash) async {
    return _cards
        .where((c) => c.sgfHashes != null && c.sgfHashes!.contains(positionHash))
        .toList();
  }

  @override
  Future<void> addCard(KnowledgeCard card) async {
    _cards.removeWhere((c) => c.id == card.id);
    _cards.add(card);
  }

  @override
  Future<void> removeCard(String id) async {
    _cards.removeWhere((c) => c.id == id);
  }

  @override
  Future<List<String>> getCategories() async {
    return _cards.map((c) => c.category).toSet().toList();
  }
}
