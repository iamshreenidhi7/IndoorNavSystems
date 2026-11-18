import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/features/map/domain/floor_map.dart';

void main() {
  test('parses a floor map with point-of-interest markers', () {
    final floorMap = FloorMap.fromJson(
      svgContent: '<svg viewBox="0 0 100 80"></svg>',
      json: const <String, Object?>{
        'floor': <String, Object?>{
          'id': 2,
          'buildingId': 1,
          'level': 0,
          'displayName': 'G',
        },
        'plan': <String, Object?>{
          'url': '/assets/floorplans/sample/ground.svg',
          'width': 100,
          'height': 80,
        },
        'nodes': <Object?>[
          <String, Object?>{'id': 10, 'x': 20, 'y': 30},
        ],
        'edges': <Object?>[
          <String, Object?>{
            'fromNodeId': 10,
            'toNodeId': 11,
            'blocked': true,
            'obstacle': <String, Object?>{
              'description': 'Closed for cleaning.',
            },
          },
        ],
        'pointsOfInterest': <Object?>[
          <String, Object?>{'id': 4, 'name': 'Information', 'nodeId': 10},
        ],
      },
    );

    expect(floorMap.floor.displayName, 'G');
    expect(floorMap.plan.width, 100);
    expect(floorMap.nodes.single.x, 20);
    expect(floorMap.pointsOfInterest.single.nodeId, 10);
    expect(floorMap.edges.single.blocked, isTrue);
    expect(floorMap.edges.single.obstacleDescription, 'Closed for cleaning.');
  });

  test('rejects a map with a non-numeric plan dimension', () {
    expect(
      () => FloorMap.fromJson(
        svgContent: '<svg></svg>',
        json: const <String, Object?>{
          'floor': <String, Object?>{
            'id': 2,
            'buildingId': 1,
            'level': 0,
            'displayName': 'G',
          },
          'plan': <String, Object?>{
            'url': '/assets/floorplans/sample/ground.svg',
            'width': '100',
            'height': 80,
          },
          'nodes': <Object?>[],
          'edges': <Object?>[],
          'pointsOfInterest': <Object?>[],
        },
      ),
      throwsFormatException,
    );
  });
}
