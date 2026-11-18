import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/buildings/presentation/building_selection_screen.dart';
import '../../features/buildings/presentation/home_screen.dart';
import '../../features/auth/presentation/admin_login_screen.dart';
import '../../features/map/presentation/indoor_map_screen.dart';
import '../../features/search/presentation/destination_search_screen.dart';
import '../../features/search/presentation/start_location_screen.dart';
import '../../features/obstacles_admin/presentation/obstacle_management_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: <RouteBase>[
      GoRoute(path: '/', builder: (context, state) => const HomeScreen()),
      GoRoute(
        path: '/admin/login',
        builder: (context, state) => const AdminLoginScreen(),
      ),
      GoRoute(
        path: '/buildings',
        builder: (context, state) => const BuildingSelectionScreen(),
      ),
      GoRoute(
        path: '/buildings/:buildingId/map',
        builder: (context, state) {
          final buildingId = int.tryParse(
            state.pathParameters['buildingId'] ?? '',
          );
          if (buildingId == null || buildingId < 1) {
            throw const FormatException('A valid building ID is required.');
          }
          return IndoorMapScreen(buildingId: buildingId);
        },
      ),
      GoRoute(
        path: '/buildings/:buildingId/destinations',
        builder: (context, state) => DestinationSearchScreen(
          buildingId: _buildingId(state.pathParameters),
        ),
      ),
      GoRoute(
        path: '/buildings/:buildingId/start',
        builder: (context, state) =>
            StartLocationScreen(buildingId: _buildingId(state.pathParameters)),
      ),
      GoRoute(
        path: '/buildings/:buildingId/obstacles',
        builder: (context, state) => ObstacleManagementScreen(
          buildingId: _buildingId(state.pathParameters),
        ),
      ),
    ],
  );
});

int _buildingId(Map<String, String> pathParameters) {
  final buildingId = int.tryParse(pathParameters['buildingId'] ?? '');
  if (buildingId == null || buildingId < 1) {
    throw const FormatException('A valid building ID is required.');
  }
  return buildingId;
}
