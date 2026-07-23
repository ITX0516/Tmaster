/// The SGF node tree model.
///
/// SGF uses a property-list tree structure:
///   (;GM[1]SZ[19] ;B[pd] ;W[dp] ...)
///
/// Each node is a `SgfNode` with a map of property→values and a list of children.
/// The "main line" is index 0 of each child list.

final class SgfProperty {
  final String name;   // e.g. "B", "W", "SZ", "C"
  final List<String> values; // each value is the content between [ ] brackets

  const SgfProperty(this.name, this.values);

  @override
  String toString() => values.map((v) => '$name[$v]').join('');
}

final class SgfNode {
  final int id; // unique ID within the tree
  final Map<String, SgfProperty> properties;
  final List<SgfNode> children;

  const SgfNode({
    this.id = 0,
    this.properties = const {},
    this.children = const [],
  });

  bool get isRoot => id == 0;

  /// Get a property value, or null.
  String? prop(String name) {
    final p = properties[name];
    return p != null && p.values.isNotEmpty ? p.values.first : null;
  }

  /// All values for a property.
  List<String>? propAll(String name) => properties[name]?.values;

  /// Convenience: get board size.
  int get boardSize => int.tryParse(prop('SZ') ?? '19') ?? 19;

  /// Convenience: get komi.
  double get komi => double.tryParse(prop('KM') ?? '6.5') ?? 6.5;

  SgfNode addChild(SgfNode child) =>
      SgfNode(id: id, properties: properties, children: [...children, child]);

  SgfNode setProp(String name, String value) {
    final newProps = Map<String, SgfProperty>.from(properties);
    newProps[name] = SgfProperty(name, [value]);
    return SgfNode(id: id, properties: newProps, children: children);
  }

  @override
  String toString() {
    final propStr = properties.values.map((p) => p.toString()).join('');
    if (children.isEmpty) return ';${propStr.isEmpty ? '' : propStr}';
    final kids = children.map((c) => c.toString()).join('\n');
    return ';${propStr.isEmpty ? '' : propStr}\n$kids';
  }
}
