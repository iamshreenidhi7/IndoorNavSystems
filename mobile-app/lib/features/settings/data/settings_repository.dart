import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../routing/domain/route_preference.dart';

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  return SettingsRepository();
});

class SettingsRepository {
  static const _lastBuildingKey = 'lastBuildingId';
  static const _preferenceKey = 'routePreference';
  static const _accessibleKey = 'accessibleRoutes';

  Future<int?> lastBuildingId() async {
    return (await SharedPreferences.getInstance()).getInt(_lastBuildingKey);
  }

  Future<void> saveLastBuilding(int buildingId) async {
    await (await SharedPreferences.getInstance()).setInt(
      _lastBuildingKey,
      buildingId,
    );
  }

  Future<RoutePreference> preference() async {
    final value = (await SharedPreferences.getInstance()).getString(
      _preferenceKey,
    );
    if (value == null) return RoutePreference.shortest;
    try {
      return RoutePreference.fromCode(value);
    } on FormatException {
      return RoutePreference.shortest;
    }
  }

  Future<void> savePreference(RoutePreference preference) async {
    await (await SharedPreferences.getInstance()).setString(
      _preferenceKey,
      preference.code,
    );
  }

  Future<bool> accessibleRoutes() async {
    return (await SharedPreferences.getInstance()).getBool(_accessibleKey) ??
        false;
  }

  Future<void> setAccessibleRoutes(bool enabled) async {
    await (await SharedPreferences.getInstance()).setBool(
      _accessibleKey,
      enabled,
    );
  }
}
