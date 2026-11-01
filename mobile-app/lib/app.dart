import 'package:flutter/material.dart';

/// Root widget (ARCHITECTURE.md Section 9.1).
///
/// Phase 1 goal only: prove the app builds and starts. The real theme
/// (DESIGN.md Sections 3-5, 9), router and Splash → Home → Building flow
/// (F1, Phase 12) replace this placeholder once their dependencies are
/// approved and `core/theme`, `core/router` are implemented.
class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Indoor Navigation', // DESIGN.md [ASSUMPTION DA-2]: neutral
      // text wordmark; the product has no name or logo yet.
      theme: ThemeData(useMaterial3: true),
      home: const _SkeletonHome(),
    );
  }
}

/// Placeholder home screen, replaced by the real Splash/Home screens in
/// Phase 12. Exists only so Phase 1's CI and `docker compose up` check has
/// something real to render and a smoke test has something to pump.
class _SkeletonHome extends StatelessWidget {
  const _SkeletonHome();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Text('Indoor Navigation'),
      ),
    );
  }
}
