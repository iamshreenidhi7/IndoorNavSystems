import '../../floors/domain/floor.dart';

class FloorMap {
  const FloorMap({
    required this.floor,
    required this.plan,
    required this.nodes,
    required this.edges,
    required this.pointsOfInterest,
    required this.svgContent,
  });

  factory FloorMap.fromJson({
    required Map<String, Object?> json,
    required String svgContent,
  }) {
    return FloorMap(
      floor: Floor.fromJson(_requiredObject(json, 'floor')),
      plan: FloorPlan.fromJson(_requiredObject(json, 'plan')),
      nodes: _requiredList(
        json,
        'nodes',
      ).map(MapNode.fromJson).toList(growable: false),
      edges: _requiredList(
        json,
        'edges',
      ).map(MapEdge.fromJson).toList(growable: false),
      pointsOfInterest: _requiredList(
        json,
        'pointsOfInterest',
      ).map(MapPointOfInterest.fromJson).toList(growable: false),
      svgContent: svgContent,
    );
  }

  final Floor floor;
  final FloorPlan plan;
  final List<MapNode> nodes;
  final List<MapEdge> edges;
  final List<MapPointOfInterest> pointsOfInterest;
  final String svgContent;

  static Map<String, Object?> _requiredObject(
    Map<String, Object?> json,
    String key,
  ) {
    final value = json[key];
    if (value is! Map<Object?, Object?>) {
      throw FormatException('$key must be an object.');
    }
    return _mapFromObject(value);
  }

  static List<Map<String, Object?>> _requiredList(
    Map<String, Object?> json,
    String key,
  ) {
    final value = json[key];
    if (value is! List<Object?>) {
      throw FormatException('$key must be a list.');
    }
    return value
        .map((item) {
          if (item is! Map<Object?, Object?>) {
            throw const FormatException('List item must be an object.');
          }
          return _mapFromObject(item);
        })
        .toList(growable: false);
  }

  static Map<String, Object?> _mapFromObject(Map<Object?, Object?> value) {
    final object = <String, Object?>{};
    for (final entry in value.entries) {
      if (entry.key is! String) {
        throw const FormatException('Object has an invalid key.');
      }
      object[entry.key as String] = entry.value;
    }
    return object;
  }
}

class FloorPlan {
  const FloorPlan({
    required this.url,
    required this.width,
    required this.height,
  });

  factory FloorPlan.fromJson(Map<String, Object?> json) {
    return FloorPlan(
      url: _requiredString(json, 'url'),
      width: _requiredNumber(json, 'width'),
      height: _requiredNumber(json, 'height'),
    );
  }

  final String url;
  final double width;
  final double height;
}

class MapNode {
  const MapNode({required this.id, required this.x, required this.y});

  factory MapNode.fromJson(Map<String, Object?> json) {
    return MapNode(
      id: _requiredInt(json, 'id'),
      x: _requiredNumber(json, 'x'),
      y: _requiredNumber(json, 'y'),
    );
  }

  final int id;
  final double x;
  final double y;
}

class MapEdge {
  const MapEdge({
    required this.fromNodeId,
    required this.toNodeId,
    required this.blocked,
    required this.obstacleDescription,
  });

  factory MapEdge.fromJson(Map<String, Object?> json) {
    final obstacle = json['obstacle'];
    String? description;
    if (obstacle is Map<Object?, Object?> &&
        obstacle['description'] is String) {
      description = obstacle['description'] as String;
    }
    return MapEdge(
      fromNodeId: _requiredInt(json, 'fromNodeId'),
      toNodeId: _requiredInt(json, 'toNodeId'),
      blocked: json['blocked'] is bool ? json['blocked'] as bool : false,
      obstacleDescription: description,
    );
  }

  final int fromNodeId;
  final int toNodeId;
  final bool blocked;
  final String? obstacleDescription;
}

class MapPointOfInterest {
  const MapPointOfInterest({
    required this.id,
    required this.name,
    required this.nodeId,
  });

  factory MapPointOfInterest.fromJson(Map<String, Object?> json) {
    return MapPointOfInterest(
      id: _requiredInt(json, 'id'),
      name: _requiredString(json, 'name'),
      nodeId: _requiredInt(json, 'nodeId'),
    );
  }

  final int id;
  final String name;
  final int nodeId;
}

int _requiredInt(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! int) {
    throw FormatException('$key must be an integer.');
  }
  return value;
}

double _requiredNumber(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is num) {
    return value.toDouble();
  }
  throw FormatException('$key must be a number.');
}

String _requiredString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! String || value.isEmpty) {
    throw FormatException('$key must be a non-empty string.');
  }
  return value;
}
