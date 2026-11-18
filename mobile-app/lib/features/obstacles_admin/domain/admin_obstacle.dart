class AdminObstacle {
  const AdminObstacle({
    required this.id,
    required this.buildingId,
    required this.targetType,
    required this.targetId,
    required this.targetSummary,
    required this.obstacleType,
    required this.description,
    required this.status,
    required this.version,
  });

  factory AdminObstacle.fromJson(Map<String, Object?> json) {
    final targetType = _requiredString(json, 'targetType');
    final targetId = targetType == 'EDGE' ? json['edgeId'] : json['nodeId'];
    if (targetId is! int) {
      throw const FormatException('Obstacle target ID is missing.');
    }
    return AdminObstacle(
      id: _requiredInt(json, 'id'),
      buildingId: _requiredInt(json, 'buildingId'),
      targetType: targetType,
      targetId: targetId,
      targetSummary: _requiredString(json, 'targetSummary'),
      obstacleType: _requiredString(json, 'obstacleType'),
      description: _requiredString(json, 'description'),
      status: _requiredString(json, 'status'),
      version: _requiredInt(json, 'version'),
    );
  }

  final int id;
  final int buildingId;
  final String targetType;
  final int targetId;
  final String targetSummary;
  final String obstacleType;
  final String description;
  final String status;
  final int version;
}

int _requiredInt(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! int) throw FormatException('$key must be an integer.');
  return value;
}

String _requiredString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! String || value.isEmpty) {
    throw FormatException('$key must be a non-empty string.');
  }
  return value;
}
