/// The color of a stone on the board.
enum StoneColor { black, white }

/// Flips black→white, white→black.
StoneColor opposite(StoneColor c) =>
    c == StoneColor.black ? StoneColor.white : StoneColor.black;

/// A coordinate on the Go board. (0,0) is top-left (the Tengen on a 19×19).
/// `pass` is represented by the sentinel value (-1, -1).
final class Coordinate {
  final int x; // column, 0-indexed
  final int y; // row,    0-indexed

  const Coordinate(this.x, this.y);

  static const pass = Coordinate(-1, -1);

  bool get isPass => x < 0;

  /// Returns the 4 orthogonal neighbours (up, right, down, left).
  List<Coordinate> neighbours(int boardSize) {
    // first check if this is a valid coordinate on the board
    if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) {
      return [];
    }
    final result = <Coordinate>[];
    if (y > 0) result.add(Coordinate(x, y - 1));
    if (x < boardSize - 1) result.add(Coordinate(x + 1, y));
    if (y < boardSize - 1) result.add(Coordinate(x, y + 1));
    if (x > 0) result.add(Coordinate(x - 1, y));
    return result;
  }

  /// SGF-style label like "pd", "aa", or "tt" for pass.
  String toSgf(int boardSize) {
    if (isPass) return 'tt';
    return '${String.fromCharCode(97 + x)}${String.fromCharCode(97 + y)}';
  }

  /// Parse from SGF label.
  factory Coordinate.fromSgf(String label) {
    if (label == 'tt' || label.isEmpty) return Coordinate.pass;
    return Coordinate(
      label.codeUnitAt(0) - 97,
      label.codeUnitAt(1) - 97,
    );
  }

  @override
  bool operator ==(Object other) =>
      other is Coordinate && other.x == x && other.y == y;

  @override
  int get hashCode => Object.hash(x, y);

  @override
  String toString() => isPass ? 'pass' : '($x,$y)';
}
