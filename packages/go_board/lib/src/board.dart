import 'stone.dart';

/// Represents one move made on the board.
final class Move {
  final StoneColor player;
  final Coordinate coordinate;
  final int moveNumber; // 0-based: move 0 = first move

  const Move({
    required this.player,
    required this.coordinate,
    required this.moveNumber,
  });

  bool get isPass => coordinate.isPass;

  @override
  String toString() => 'Move#$moveNumber $player $coordinate';
}

/// The complete state of a Go game at a given point in time.
///
/// This is an **immutable** value object. Every mutating operation
/// returns a new [BoardState]. History is preserved in [moveHistory].
final class BoardState {
  final int boardSize; // typically 9, 13, or 19
  final StoneColor currentPlayer;

  /// Flat list of length boardSize*boardSize. `null` = empty intersection.
  final List<StoneColor?> grid;

  /// Every move played so far (including passes), in order.
  final List<Move> moveHistory;

  /// Position hashes used for super-ko detection (positional superko).
  final Set<int> previousPositionHashes;

  /// Game metadata carried from SGF.
  final double komi;
  final String? blackPlayer;
  final String? whitePlayer;
  final String? result;

  const BoardState._({
    required this.boardSize,
    required this.currentPlayer,
    required this.grid,
    required this.moveHistory,
    required this.previousPositionHashes,
    this.komi = 6.5,
    this.blackPlayer,
    this.whitePlayer,
    this.result,
  });

  /// Creates an empty board.
  factory BoardState.empty({
    int boardSize = 19,
    double komi = 6.5,
    String? blackPlayer,
    String? whitePlayer,
  }) {
    return BoardState._(
      boardSize: boardSize,
      currentPlayer: StoneColor.black,
      grid: List.filled(boardSize * boardSize, null, growable: false),
      moveHistory: const [],
      previousPositionHashes: const {},
      komi: komi,
      blackPlayer: blackPlayer,
      whitePlayer: whitePlayer,
    );
  }

  // ── grid access ──────────────────────────────────────────────────

  StoneColor? stoneAt(Coordinate c) {
    if (c.isPass || c.x < 0 || c.x >= boardSize || c.y < 0 || c.y >= boardSize) {
      return null;
    }
    return grid[c.y * boardSize + c.x];
  }

  int _index(Coordinate c) => c.y * boardSize + c.x;

  // ── move validation ──────────────────────────────────────────────

  /// Returns true if [c] is a legal move.
  bool isValidMove(Coordinate c) {
    if (c.isPass) return true;
    if (c.x < 0 || c.x >= boardSize || c.y < 0 || c.y >= boardSize) return false;
    if (grid[_index(c)] != null) return false; // occupied
    if (_isSuicide(c)) return false;
    if (_violatesSimpleKo(c)) return false;
    return true;
  }

  /// Would playing at [c] capture zero enemy stones AND leave the placed
  /// stone with zero liberties? If so, it's suicide (illegal in most rulesets).
  bool _isSuicide(Coordinate c) {
    final testGrid = _cloneGrid();
    testGrid[_index(c)] = currentPlayer;
    final enemy = opposite(currentPlayer);

    // Check if we capture any enemy groups
    for (final n in c.neighbours(boardSize)) {
      if (testGrid[_index(n)] == enemy) {
        if (_countLiberties(testGrid, n) == 0) return false; // captures enemy
      }
    }
    // No enemy captured — check own liberties
    return _countLiberties(testGrid, c) == 0;
  }

  bool _violatesSimpleKo(Coordinate c) {
    if (moveHistory.isEmpty) return false;
    final hypothetical = _play(c);
    return previousPositionHashes.contains(hypothetical._positionHash());
  }

  // ── play a move ──────────────────────────────────────────────────

  /// Play a move. Returns the new board state. Does NOT validate — caller
  /// should call [isValidMove] first.
  BoardState _play(Coordinate c) {
    final newGrid = _cloneGrid();
    if (!c.isPass) {
      newGrid[_index(c)] = currentPlayer;
      // Remove captured enemy stones
      final enemy = opposite(currentPlayer);
      for (final n in c.neighbours(boardSize)) {
        if (newGrid[_index(n)] == enemy && _countLiberties(newGrid, n) == 0) {
          _removeGroup(newGrid, n);
        }
      }
    }
    final newMoves = [...moveHistory, Move(
      player: currentPlayer,
      coordinate: c,
      moveNumber: moveHistory.length,
    )];
    final newHashes = {...previousPositionHashes, _positionHash()};

    return BoardState._(
      boardSize: boardSize,
      currentPlayer: enemy,
      grid: newGrid,
      moveHistory: newMoves,
      previousPositionHashes: newHashes,
      komi: komi,
      blackPlayer: blackPlayer,
      whitePlayer: whitePlayer,
      result: result,
    );
  }

  /// Public method: validate then play.
  BoardState? playMove(Coordinate c) {
    if (!isValidMove(c)) return null;
    return _play(c);
  }

  BoardState pass() => _play(Coordinate.pass);
  BoardState resign() => copyWith(result: currentPlayer == StoneColor.black ? 'W+R' : 'B+R');

  // ── captures ─────────────────────────────────────────────────────

  /// Returns the set of stones that would be captured if [c] were played.
  Set<Coordinate> capturesAt(Coordinate c) {
    if (c.isPass) return {};
    final captured = <Coordinate>{};
    final testGrid = _cloneGrid();
    testGrid[_index(c)] = currentPlayer;
    final enemy = opposite(currentPlayer);
    for (final n in c.neighbours(boardSize)) {
      if (testGrid[_index(n)] == enemy && _countLiberties(testGrid, n) == 0) {
        _collectGroup(testGrid, n).forEach((coord) => captured.add(coord));
      }
    }
    return captured;
  }

  // ── liberties helper ─────────────────────────────────────────────

  int _countLiberties(List<StoneColor?> g, Coordinate start) {
    final color = g[_index(start)]!;
    final visited = <int>{};
    final stack = [start];
    int liberties = 0;

    while (stack.isNotEmpty) {
      final c = stack.removeLast();
      final idx = _index(c);
      if (visited.contains(idx)) continue;
      visited.add(idx);
      for (final n in c.neighbours(boardSize)) {
        final ni = _index(n);
        if (g[ni] == null) {
          liberties++;
        } else if (g[ni] == color && !visited.contains(ni)) {
          stack.add(n);
        }
      }
    }
    return liberties;
  }

  void _removeGroup(List<StoneColor?> g, Coordinate start) {
    final color = g[_index(start)]!;
    final stack = [start];
    while (stack.isNotEmpty) {
      final c = stack.removeLast();
      if (g[_index(c)] != color) continue;
      g[_index(c)] = null;
      for (final n in c.neighbours(boardSize)) {
        if (g[_index(n)] == color) stack.add(n);
      }
    }
  }

  Set<Coordinate> _collectGroup(List<StoneColor?> g, Coordinate start) {
    final color = g[_index(start)]!;
    final visited = <Coordinate>{};
    final stack = [start];
    while (stack.isNotEmpty) {
      final c = stack.removeLast();
      if (visited.contains(c)) continue;
      visited.add(c);
      for (final n in c.neighbours(boardSize)) {
        if (g[_index(n)] == color && !visited.contains(n)) stack.add(n);
      }
    }
    return visited;
  }

  // ── scoring (Chinese area scoring) ───────────────────────────────

  /// Returns territory score from Black's perspective. Positive = Black leads.
  double scoreChinese() {
    double blackArea = 0;
    double whiteArea = 0;
    final visited = <int>{};

    for (var y = 0; y < boardSize; y++) {
      for (var x = 0; x < boardSize; x++) {
        final idx = y * boardSize + x;
        if (visited.contains(idx)) continue;

        final stone = grid[idx];
        if (stone != null) {
          if (stone == StoneColor.black) {
            blackArea++;
          } else {
            whiteArea++;
          }
          visited.add(idx);
          continue;
        }
        // Empty point — flood fill to find territory owner
        final region = <int>[];
        final borders = <StoneColor>{};
        final stack = [idx];
        while (stack.isNotEmpty) {
          final ci = stack.removeLast();
          if (visited.contains(ci)) continue;
          visited.add(ci);
          if (grid[ci] != null) {
            borders.add(grid[ci]!);
            continue;
          }
          region.add(ci);
          final cx = ci % boardSize;
          final cy = ci ~/ boardSize;
          for (final n in Coordinate(cx, cy).neighbours(boardSize)) {
            final ni = _index(n);
            if (!visited.contains(ni)) stack.add(ni);
          }
        }
        if (borders.length == 1) {
          if (borders.first == StoneColor.black) {
            blackArea += region.length;
          } else {
            whiteArea += region.length;
          }
        }
        // If borders contain both colors (or neither), it's neutral (dame)
      }
    }

    return blackArea - whiteArea - komi;
  }

  // ── hash ─────────────────────────────────────────────────────────

  int _positionHash() {
    int h = 17;
    for (final s in grid) {
      h = h * 31 + (s == null ? 0 : (s == StoneColor.black ? 1 : 2));
    }
    h = h * 31 + (currentPlayer == StoneColor.black ? 1 : 0);
    return h;
  }

  // ── copy ─────────────────────────────────────────────────────────

  List<StoneColor?> _cloneGrid() => List.of(grid, growable: false);

  BoardState copyWith({
    int? boardSize,
    StoneColor? currentPlayer,
    List<StoneColor?>? grid,
    List<Move>? moveHistory,
    double? komi,
    String? result,
    String? blackPlayer,
    String? whitePlayer,
  }) {
    return BoardState._(
      boardSize: boardSize ?? this.boardSize,
      currentPlayer: currentPlayer ?? this.currentPlayer,
      grid: grid ?? this.grid,
      moveHistory: moveHistory ?? this.moveHistory,
      previousPositionHashes: previousPositionHashes,
      komi: komi ?? this.komi,
      result: result ?? this.result,
      blackPlayer: blackPlayer ?? this.blackPlayer,
      whitePlayer: whitePlayer ?? this.whitePlayer,
    );
  }

  // ── navigation ───────────────────────────────────────────────────

  int get moveCount => moveHistory.length;

  String get currentPlayerLabel =>
      currentPlayer == StoneColor.black ? 'B' : 'W';
}
