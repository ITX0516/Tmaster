import 'model.dart';

/// Parses an SGF string into an [SgfNode] tree.
///
/// Handles FF[4] standard SGF: parentheses for tree branching, semicolons for
/// node delimiters, and property name/value syntax.
///
/// Throws [SgfParseException] on malformed input.
class SgfParser {
  final String input;
  int _pos = 0;

  SgfParser(this.input);

  SgfNode parse() {
    _skipWhitespace();
    final root = _parseCollection();
    if (root.children.isEmpty) {
      throw SgfParseException('Empty game tree');
    }
    return root.children.first;
  }

  SgfNode _parseCollection() {
    _expect('(');
    final nodes = <SgfNode>[];
    int id = 0;
    while (_pos < input.length && input[_pos] != ')') {
      if (input[_pos] == '(') {
        // Sub-tree (variation) — attach to last node
        final child = _parseCollection();
        if (nodes.isNotEmpty) {
          nodes[nodes.length - 1] =
              nodes.last.copyWith(children: [...nodes.last.children, child]);
        } else {
          nodes.add(SgfNode(id: id++, children: [child]));
        }
      } else {
        nodes.add(_parseNode(id++));
      }
    }
    _expect(')');
    if (nodes.isEmpty) return SgfNode();
    // Link nodes into a chain: each node has the next as its first child
    for (var i = nodes.length - 1; i > 0; i--) {
      final children = nodes[i].children.isNotEmpty
          ? nodes[i].children
          : [nodes[i]];
      nodes[i - 1] = nodes[i - 1].copyWith(
        children: [nodes[i]],
      );
    }
    return nodes.first;
  }

  SgfNode _parseNode(int id) {
    _expect(';');
    final props = <String, SgfProperty>{};
    while (_pos < input.length && _isPropStart(input[_pos])) {
      final prop = _parseProperty();
      props[prop.name] = prop;
    }
    return SgfNode(id: id, properties: props);
  }

  SgfProperty _parseProperty() {
    final name = _readPropName();
    final values = <String>[];
    while (_pos < input.length && input[_pos] == '[') {
      values.add(_readPropValue());
    }
    return SgfProperty(name, values);
  }

  String _readPropName() {
    final start = _pos;
    while (_pos < input.length && _isAlphaUpper(input[_pos])) {
      _pos++;
    }
    if (_pos == start) throw SgfParseException('Expected property name at $_pos');
    return input.substring(start, _pos);
  }

  String _readPropValue() {
    _expect('[');
    final buf = StringBuffer();
    while (_pos < input.length && input[_pos] != ']') {
      if (input[_pos] == '\\') {
        _pos++;
        if (_pos < input.length) {
          buf.write(input[_pos]);
          _pos++;
        }
      } else {
        buf.write(input[_pos]);
        _pos++;
      }
    }
    _expect(']');
    return buf.toString();
  }

  void _skipWhitespace() {
    while (_pos < input.length && (input[_pos] == ' ' || input[_pos] == '\n' || input[_pos] == '\r' || input[_pos] == '\t')) {
      _pos++;
    }
  }

  void _expect(String ch) {
    _skipWhitespace();
    if (_pos >= input.length || input[_pos] != ch) {
      throw SgfParseException('Expected "$ch" at $_pos, got "${_pos < input.length ? input[_pos] : 'EOF'}"');
    }
    _pos++;
  }

  bool _isPropStart(String ch) => _isAlphaUpper(ch);

  bool _isAlphaUpper(String ch) {
    final c = ch.codeUnitAt(0);
    return (c >= 65 && c <= 90);
  }
}

class SgfParseException implements Exception {
  final String message;
  const SgfParseException(this.message);
  @override
  String toString() => 'SgfParseException: $message';
}

/// Extension to help with immutable node copying.
extension _SgfNodeCopy on SgfNode {
  SgfNode copyWith({
    int? id,
    Map<String, SgfProperty>? properties,
    List<SgfNode>? children,
  }) {
    return SgfNode(
      id: id ?? this.id,
      properties: properties ?? this.properties,
      children: children ?? this.children,
    );
  }
}
