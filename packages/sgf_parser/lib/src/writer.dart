import 'model.dart';

/// Writes an [SgfNode] tree to a valid SGF string.
class SgfWriter {
  String write(SgfNode root) {
    final buf = StringBuffer();
    buf.write('(');
    _writeNode(root, buf);
    buf.write(')');
    return buf.toString();
  }

  void _writeNode(SgfNode node, StringBuffer buf) {
    buf.write(';');
    for (final prop in node.properties.values) {
      buf.write(prop.name);
      for (final v in prop.values) {
        buf.write('[$v]');
      }
    }
    for (final child in node.children) {
      if (child.children.isEmpty && child.properties.isEmpty) continue;
      buf.write('\n(');
      _writeNode(child, buf);
      buf.write(')');
    }
  }
}
