import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_app/app.dart';

void main() {
  testWidgets('App builds and renders the placeholder home screen',
      (WidgetTester tester) async {
    await tester.pumpWidget(const App());

    // Phase 1 smoke test only: prove the widget tree builds without
    // throwing. It is replaced by real widget tests per screen in Phase 12
    // (AGENTS.md Section 13.2).
    expect(find.byType(MaterialApp), findsOneWidget);
    expect(find.byType(Scaffold), findsOneWidget);
    expect(find.text('Indoor Navigation'), findsOneWidget);
  });
}
