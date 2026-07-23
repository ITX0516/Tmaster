/// Schema for a tool parameter, used for LLM function-calling.
final class PropertySchema {
  final String type;       // "string", "number", "integer", "boolean", "array", "object"
  final String description;
  final bool required;
  final List<String>? enumValues;

  const PropertySchema({
    required this.type,
    required this.description,
    this.required = true,
    this.enumValues,
  });

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{
      'type': type,
      'description': description,
    };
    if (enumValues != null) json['enum'] = enumValues;
    return json;
  }
}

/// A tool that the LLM agent can invoke.
///
/// Analogous to an MCP tool definition. Each tool has a name, description,
/// parameter schema, and an async executor function.
final class Tool {
  final String name;
  final String description;
  final Map<String, PropertySchema> parameters;

  /// The function that executes this tool. Receives named parameters,
  /// returns a JSON-serializable result.
  final Future<dynamic> Function(Map<String, dynamic> args) execute;

  const Tool({
    required this.name,
    required this.description,
    required this.parameters,
    required this.execute,
  });

  /// Generates the OpenAI/Anthropic-compatible tool definition JSON.
  Map<String, dynamic> toFunctionDefinition() {
    final props = <String, dynamic>{};
    final required = <String>[];
    for (final entry in parameters.entries) {
      props[entry.key] = entry.value.toJson();
      if (entry.value.required) required.add(entry.key);
    }
    return {
      'type': 'function',
      'function': {
        'name': name,
        'description': description,
        'parameters': {
          'type': 'object',
          'properties': props,
          'required': required,
        },
      },
    };
  }
}

/// Registry of all available tools the agent can use.
class ToolRegistry {
  final Map<String, Tool> _tools = {};

  void register(Tool tool) {
    _tools[tool.name] = tool;
  }

  void registerAll(Iterable<Tool> tools) {
    for (final t in tools) {
      _tools[t.name] = t;
    }
  }

  Tool? get(String name) => _tools[name];

  List<Tool> get all => _tools.values.toList();

  List<Map<String, dynamic>> toFunctionDefinitions() =>
      _tools.values.map((t) => t.toFunctionDefinition()).toList();
}
