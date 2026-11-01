import 'package:flutter/material.dart';

import 'app.dart';

/// Entry point (ARCHITECTURE.md Section 9.1).
///
/// This is deliberately a plain [runApp] call for now. State management
/// (`ProviderScope`), routing and networking are wired up in Phase 12 once
/// their dependencies (flutter_riverpod, go_router, dio) are approved per
/// AGENTS.md Section 8.1; adding them earlier would be scope this skeleton
/// task does not need.
void main() {
  runApp(const App());
}
