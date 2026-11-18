import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../settings/data/settings_repository.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _accessible = false;
  int? _lastBuildingId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final settings = SettingsRepository();
    final value = await settings.accessibleRoutes();
    final buildingId = await settings.lastBuildingId();
    if (mounted)
      setState(() {
        _accessible = value;
        _lastBuildingId = buildingId;
      });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 600),
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.x4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () => context.push('/admin/login'),
                      child: const Text('Admin'),
                    ),
                  ),
                  const Spacer(),
                  Text(
                    'Indoor navigation',
                    style: Theme.of(context).textTheme.headlineLarge,
                  ),
                  const SizedBox(height: AppSpacing.x2),
                  Text(
                    'Choose a building, then select a destination and route.',
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const SizedBox(height: AppSpacing.x6),
                  Semantics(
                    label: 'Only show step-free routes',
                    child: SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('Only show step-free routes'),
                      subtitle: Text('Use step-free routes by default.'),
                      value: _accessible,
                      onChanged: (value) async {
                        setState(() => _accessible = value);
                        await SettingsRepository().setAccessibleRoutes(value);
                      },
                    ),
                  ),
                  const Spacer(),
                  if (_lastBuildingId case final buildingId?) ...<Widget>[
                    OutlinedButton(
                      onPressed: () => context.go('/buildings/$buildingId/map'),
                      child: const Text('Continue in last building'),
                    ),
                    const SizedBox(height: AppSpacing.x2),
                  ],
                  FilledButton(
                    onPressed: () => context.go('/buildings'),
                    child: const Text('Choose a building'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
