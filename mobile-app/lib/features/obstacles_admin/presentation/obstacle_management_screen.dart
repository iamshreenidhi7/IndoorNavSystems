import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/widgets/full_state_view.dart';
import '../data/obstacle_repository.dart';
import '../domain/admin_obstacle.dart';

class ObstacleManagementScreen extends ConsumerStatefulWidget {
  const ObstacleManagementScreen({required this.buildingId, super.key});

  final int buildingId;

  @override
  ConsumerState<ObstacleManagementScreen> createState() =>
      _ObstacleManagementScreenState();
}

class _ObstacleManagementScreenState
    extends ConsumerState<ObstacleManagementScreen> {
  String? _filter = 'ACTIVE';
  bool _loading = true;
  AppFailure? _failure;
  List<AdminObstacle> _obstacles = const <AdminObstacle>[];

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Admin obstacles')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreate,
        icon: const Icon(Icons.add),
        label: const Text('Add obstacle'),
      ),
      body: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.all(16),
            child: SegmentedButton<String?>(
              segments: const <ButtonSegment<String?>>[
                ButtonSegment<String?>(value: 'ACTIVE', label: Text('Active')),
                ButtonSegment<String?>(
                  value: 'RESOLVED',
                  label: Text('Resolved'),
                ),
                ButtonSegment<String?>(value: null, label: Text('All')),
              ],
              selected: <String?>{_filter},
              onSelectionChanged: (selection) {
                setState(() => _filter = selection.single);
                _load();
              },
            ),
          ),
          Expanded(child: _content()),
        ],
      ),
    );
  }

  Widget _content() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_failure case final failure?) {
      return FullStateView(
        icon: Icons.cloud_off_outlined,
        title: failure.title,
        message: failure.message,
        primaryAction: _load,
        primaryActionLabel: 'Retry',
      );
    }
    if (_obstacles.isEmpty) {
      return const FullStateView(
        icon: Icons.check_circle_outline,
        title: 'No active obstacles',
        message: 'Everything is open.',
      );
    }
    return ListView.separated(
      itemCount: _obstacles.length,
      separatorBuilder: (context, index) => const Divider(),
      itemBuilder: (context, index) {
        final obstacle = _obstacles[index];
        return ListTile(
          leading: const Icon(Icons.block_outlined),
          title: Text(obstacle.targetSummary),
          subtitle: Text(obstacle.description),
          trailing: Text(obstacle.status == 'ACTIVE' ? 'Active' : 'Resolved'),
          onTap: () => _showEdit(obstacle),
        );
      },
    );
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _failure = null;
    });
    final result = await ref
        .read(obstacleRepositoryProvider)
        .list(buildingId: widget.buildingId, status: _filter);
    if (!mounted) return;
    switch (result) {
      case Success<List<AdminObstacle>>(value: final items):
        setState(() {
          _loading = false;
          _obstacles = items;
        });
      case Failure<List<AdminObstacle>>(error: final error):
        setState(() {
          _loading = false;
          _failure = error is AppFailure
              ? error
              : const InvalidResponseFailure();
        });
    }
  }

  Future<void> _showCreate() async {
    final created = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _ObstacleForm(buildingId: widget.buildingId),
    );
    if (created ?? false) _load();
  }

  Future<void> _showEdit(AdminObstacle obstacle) async {
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) =>
          _ObstacleForm(buildingId: widget.buildingId, obstacle: obstacle),
    );
    if (changed ?? false) _load();
  }
}

class _ObstacleForm extends ConsumerStatefulWidget {
  const _ObstacleForm({required this.buildingId, this.obstacle});

  final int buildingId;
  final AdminObstacle? obstacle;

  @override
  ConsumerState<_ObstacleForm> createState() => _ObstacleFormState();
}

class _ObstacleFormState extends ConsumerState<_ObstacleForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _targetId;
  late final TextEditingController _description;
  String _targetType = 'EDGE';
  String _obstacleType = 'BLOCKED_CORRIDOR';
  String _status = 'ACTIVE';
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    final obstacle = widget.obstacle;
    _targetId = TextEditingController(
      text: obstacle?.targetId.toString() ?? '',
    );
    _description = TextEditingController(text: obstacle?.description ?? '');
    _targetType = obstacle?.targetType ?? _targetType;
    _obstacleType = obstacle?.obstacleType ?? _obstacleType;
    _status = obstacle?.status ?? _status;
  }

  @override
  void dispose() {
    _targetId.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final editing = widget.obstacle != null;
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          16,
          16,
          16,
          16 + MediaQuery.viewInsetsOf(context).bottom,
        ),
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Text(
                  editing ? 'Edit obstacle' : 'Add obstacle',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 16),
                if (!editing)
                  DropdownButtonFormField<String>(
                    value: _targetType,
                    items: const <DropdownMenuItem<String>>[
                      DropdownMenuItem(
                        value: 'EDGE',
                        child: Text('Connection'),
                      ),
                      DropdownMenuItem(value: 'NODE', child: Text('Node')),
                    ],
                    onChanged: (value) =>
                        setState(() => _targetType = value ?? _targetType),
                    decoration: const InputDecoration(
                      labelText: 'Target type',
                      border: OutlineInputBorder(),
                    ),
                  ),
                if (!editing) ...<Widget>[
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _targetId,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Target ID',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) => int.tryParse(value ?? '') == null
                        ? 'Enter a valid target ID.'
                        : null,
                  ),
                ],
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: _obstacleType,
                  items: _obstacleTypes
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(_label(value)),
                        ),
                      )
                      .toList(growable: false),
                  onChanged: (value) =>
                      setState(() => _obstacleType = value ?? _obstacleType),
                  decoration: const InputDecoration(
                    labelText: 'Closed or blocked type',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _description,
                  minLines: 2,
                  maxLines: 4,
                  maxLength: 200,
                  decoration: const InputDecoration(
                    labelText: 'Description',
                    border: OutlineInputBorder(),
                  ),
                  validator: (value) => value == null || value.trim().length < 3
                      ? 'Enter at least 3 characters.'
                      : null,
                ),
                if (editing) ...<Widget>[
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _status,
                    items: const <DropdownMenuItem<String>>[
                      DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
                      DropdownMenuItem(
                        value: 'RESOLVED',
                        child: Text('Resolved'),
                      ),
                    ],
                    onChanged: (value) =>
                        setState(() => _status = value ?? _status),
                    decoration: const InputDecoration(
                      labelText: 'Status',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _submitting ? null : _save,
                  child: Text(editing ? 'Save obstacle' : 'Add obstacle'),
                ),
                if (editing) ...<Widget>[
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: _submitting ? null : _delete,
                    child: const Text('Delete obstacle'),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _save() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _submitting = true);
    final existing = widget.obstacle;
    final result = existing == null
        ? await ref
              .read(obstacleRepositoryProvider)
              .create(
                targetType: _targetType,
                targetId: int.parse(_targetId.text),
                obstacleType: _obstacleType,
                description: _description.text.trim(),
              )
        : await ref
              .read(obstacleRepositoryProvider)
              .update(
                AdminObstacle(
                  id: existing.id,
                  buildingId: existing.buildingId,
                  targetType: existing.targetType,
                  targetId: existing.targetId,
                  targetSummary: existing.targetSummary,
                  obstacleType: _obstacleType,
                  description: _description.text.trim(),
                  status: _status,
                  version: existing.version,
                ),
              );
    if (!mounted) return;
    if (result is Success<AdminObstacle>) Navigator.of(context).pop(true);
    if (result is Failure<AdminObstacle>) setState(() => _submitting = false);
  }

  Future<void> _delete() async {
    final existing = widget.obstacle;
    if (existing == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete obstacle?'),
        content: Text('Delete ${existing.targetSummary}?'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    setState(() => _submitting = true);
    final result = await ref
        .read(obstacleRepositoryProvider)
        .delete(existing.id);
    if (!mounted) return;
    if (result is Success<void>) Navigator.of(context).pop(true);
    if (result is Failure<void>) setState(() => _submitting = false);
  }
}

const _obstacleTypes = <String>[
  'BLOCKED_CORRIDOR',
  'CLOSED_ROOM',
  'BROKEN_ELEVATOR',
  'CLOSED_STAIRCASE',
  'CONSTRUCTION_AREA',
  'RESTRICTED_CONNECTION',
];

String _label(String value) => value.toLowerCase().replaceAll('_', ' ');
