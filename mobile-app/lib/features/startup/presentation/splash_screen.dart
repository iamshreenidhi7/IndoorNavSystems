import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../core/widgets/full_state_view.dart';

class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen> {
  String? _configurationError;

  @override
  void initState() {
    super.initState();
    _checkConfiguration();
  }

  void _checkConfiguration() {
    try {
      ref.read(appConfigProvider);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) context.go('/home');
      });
    } on FormatException catch (error) {
      setState(() => _configurationError = error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_configurationError case final message?) {
      return Scaffold(
        body: FullStateView(
          icon: Icons.settings_alert_outlined,
          title: 'App configuration needed',
          message: message,
          primaryAction: _checkConfiguration,
          primaryActionLabel: 'Retry',
        ),
      );
    }
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
