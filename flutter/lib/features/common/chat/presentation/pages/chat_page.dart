import 'dart:async';

import 'package:flutter/material.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/features/common/chat/domain/chat_availability.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_message.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_session.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/app_radii.dart';

/// Small, shared rider/driver chat surface. The screen deliberately owns only
/// the conversation transport; ride lifecycle remains owned by each role's
/// active-ride page and is supplied through [rideStatus].
class ChatPage extends StatefulWidget {
  final int rideId;
  final int currentUserId;
  final String role;
  final String? rideStatus;
  final int? riderId;
  final int? driverId;
  final ChatRemoteService? datasource;
  final ChatRealtimeTransport? realtime;
  final Future<String?> Function()? rideStatusReader;

  const ChatPage({
    super.key,
    required this.rideId,
    required this.currentUserId,
    required this.role,
    this.rideStatus,
    this.riderId,
    this.driverId,
    this.datasource,
    this.realtime,
    this.rideStatusReader,
  });

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> with WidgetsBindingObserver {
  late final ChatRemoteService _datasource;
  late final ChatRealtimeTransport _realtime;
  final TextEditingController _composer = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final GlobalKey _messageListKey = GlobalKey();
  final List<ChatMessage> _messages = [];
  final Map<String, int> _messageIndexById = {};
  final Map<String, BuildContext> _messageContexts = {};
  StreamSubscription<ChatMessage>? _messageSubscription;
  Timer? _rideStatusTimer;
  ChatSession? _session;
  String? _status;
  String? _error;
  bool _loading = true;
  bool _sending = false;
  bool _resyncing = false;
  bool _refreshingStatus = false;
  bool _appActive = true;
  bool _visible = true;
  bool _scrollScheduled = false;

  bool get _chatAvailable => ChatAvailability.isAvailable(_status);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _datasource = widget.datasource ?? sl<ChatRemoteDatasource>();
    _realtime = widget.realtime ?? sl<ChatRealtimeService>();
    _status = widget.rideStatus?.toUpperCase();
    _messageSubscription = _realtime.messages.listen(_onRealtimeMessage);
    final lifecycle = WidgetsBinding.instance.lifecycleState;
    _appActive = lifecycle == null || lifecycle == AppLifecycleState.resumed;
    unawaited(_loadConversation());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _visible = TickerMode.valuesOf(context).enabled;
    _updateStatusPolling();
  }

  void _updateStatusPolling() {
    if (!_appActive || !_visible || !_chatAvailable) {
      _rideStatusTimer?.cancel();
      _rideStatusTimer = null;
      return;
    }
    if (widget.rideStatusReader != null) {
      _rideStatusTimer ??= Timer.periodic(
        const Duration(seconds: 5),
        (_) => unawaited(_refreshRideStatus()),
      );
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _messageSubscription?.cancel();
    _rideStatusTimer?.cancel();
    _realtime.disconnect();
    _composer.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _appActive = state == AppLifecycleState.resumed;
    _updateStatusPolling();
    if (!_appActive || !_visible) return;
    final sessionId = _session?.sessionId;
    if (sessionId == null || !_chatAvailable) return;
    _realtime.connect(sessionId);
    unawaited(_refreshRideStatus());
    unawaited(_refreshMessages(sessionId));
  }

  Future<void> _refreshMessages(int sessionId) async {
    if (_resyncing) return;
    _resyncing = true;
    final followLatest = _isNearLatest;
    final readingAnchor = followLatest ? null : _captureReadingAnchor();
    try {
      final history = await _datasource.getMessages(sessionId);
      if (!mounted || _session?.sessionId != sessionId) return;
      setState(() {
        final seen = history.map((message) => message.messageId).toSet();
        final newer = _messages
            .where((message) => !seen.contains(message.messageId))
            .toList();
        _messages
          ..clear()
          ..addAll(history)
          ..addAll(newer);
        _rebuildMessageIndex();
      });
      if (followLatest) {
        _scrollToEnd();
      } else {
        _restoreReadingAnchor(readingAnchor);
      }
    } catch (_) {
      // The current conversation stays visible until the connection recovers.
    } finally {
      _resyncing = false;
    }
  }

  Future<void> _loadConversation() async {
    if (!_chatAvailable) {
      if (mounted) setState(() => _loading = false);
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final session = await _datasource.getSessionByRide(widget.rideId);
      if (!mounted) return;
      if (session == null || session.sessionId == null) {
        setState(() {
          _loading = false;
          _error = 'Chat is still preparing. Try again in a moment.';
        });
        return;
      }
      final sessionStatus = session.rideStatus;
      if (!session.active ||
          (sessionStatus != null &&
              !ChatAvailability.isAvailable(sessionStatus))) {
        _disableChat(
          session.active ? (sessionStatus ?? 'COMPLETED') : 'COMPLETED',
        );
        return;
      }
      final history = await _datasource.getMessages(session.sessionId!);
      if (!mounted) return;
      _session = session;
      _messages
        ..clear()
        ..addAll(history);
      _rebuildMessageIndex();
      _realtime.connect(session.sessionId!);
      setState(() {
        _loading = false;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error.toString();
      });
    }
  }

  Future<void> _refreshRideStatus() async {
    final reader = widget.rideStatusReader;
    if (reader == null ||
        !mounted ||
        !_appActive ||
        !_visible ||
        _refreshingStatus) {
      return;
    }
    _refreshingStatus = true;
    try {
      final status = await reader();
      if (!mounted || status == null) return;
      final normalized = status.toUpperCase();
      if (ChatAvailability.isTerminal(normalized) ||
          !ChatAvailability.isAvailable(normalized)) {
        _disableChat(normalized);
      } else if (normalized != _status) {
        setState(() => _status = normalized);
      }
    } catch (_) {
      // Keep the existing conversation usable across a temporary status
      // gateway failure. The next lifecycle poll will enforce the boundary.
    } finally {
      _refreshingStatus = false;
    }
  }

  void _disableChat(String status) {
    if (!mounted) return;
    if (_status == status && !_loading && _error != null) return;
    _realtime.disconnect();
    setState(() {
      _status = status;
      _loading = false;
      _error = ChatAvailability.isTerminal(status)
          ? 'Chat ended because this ride is no longer active.'
          : 'Chat becomes available after the driver accepts the ride.';
    });
    _updateStatusPolling();
  }

  void _rebuildMessageIndex() {
    _messageIndexById.clear();
    for (var index = 0; index < _messages.length; index++) {
      _messageIndexById[_messages[index].messageId] = index;
    }
  }

  bool get _isNearLatest =>
      !_scrollController.hasClients || _scrollController.offset <= 80;

  /// Keep a visible message in place when new messages are inserted below it.
  /// Only mounted, lazily built bubbles participate, so this stays bounded by
  /// the viewport rather than the entire conversation history.
  ({BuildContext context, double top})? _captureReadingAnchor() {
    final viewport = _messageListKey.currentContext?.findRenderObject();
    if (viewport is! RenderBox || !viewport.hasSize) return null;
    final top = viewport.localToGlobal(Offset.zero).dy;
    final bottom = top + viewport.size.height;
    for (final context in _messageContexts.values) {
      final box = context.findRenderObject();
      if (box is! RenderBox || !box.attached || !box.hasSize) continue;
      final messageTop = box.localToGlobal(Offset.zero).dy;
      if (messageTop >= top && messageTop + box.size.height <= bottom) {
        return (context: context, top: messageTop);
      }
    }
    return null;
  }

  void _restoreReadingAnchor(({BuildContext context, double top})? anchor) {
    if (anchor == null) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted ||
          !anchor.context.mounted ||
          !_scrollController.hasClients) {
        return;
      }
      final box = anchor.context.findRenderObject();
      if (box is! RenderBox || !box.attached || !box.hasSize) return;
      final adjustment = anchor.top - box.localToGlobal(Offset.zero).dy;
      if (adjustment.abs() < 1) return;
      final position = _scrollController.position;
      _scrollController.jumpTo(
        (_scrollController.offset + adjustment).clamp(
          position.minScrollExtent,
          position.maxScrollExtent,
        ),
      );
    });
  }

  void _onRealtimeMessage(ChatMessage message) {
    if (!mounted || message.sessionId != _session?.sessionId) return;
    final index = _messageIndexById[message.messageId];
    if (index != null) {
      final previous = _messages[index];
      if (previous.messageContent == message.messageContent &&
          previous.edited == message.edited &&
          previous.deleted == message.deleted &&
          previous.sentById == message.sentById &&
          previous.messageType == message.messageType) {
        return;
      }
    }
    final followLatest =
        index == null &&
        (_isNearLatest || message.sentById == widget.currentUserId);
    final readingAnchor = followLatest ? null : _captureReadingAnchor();
    setState(() {
      if (index == null) {
        _messageIndexById[message.messageId] = _messages.length;
        _messages.add(message);
      } else {
        _messages[index] = message;
      }
    });
    if (followLatest) {
      _scrollToEnd();
    } else {
      _restoreReadingAnchor(readingAnchor);
    }
  }

  Future<void> _send() async {
    final text = _composer.text.trim();
    final sessionId = _session?.sessionId;
    if (text.isEmpty || sessionId == null || !_chatAvailable || _sending) {
      return;
    }
    final message = ChatMessage.create(
      sentById: widget.currentUserId,
      sessionId: sessionId,
      content: text,
    );
    setState(() {
      _sending = true;
      _error = null;
    });
    try {
      // REST is the durable write path. chat-service validates the participant,
      // stores the message, and publishes one Kafka event for both WebSocket
      // fan-out and notification delivery. The local append keeps the UI
      // responsive; the WebSocket event is deduplicated by message id.
      await _datasource.sendMessage(message);
      if (!mounted) return;
      if (_composer.text.trim() == text) _composer.clear();
      _onRealtimeMessage(message);
      setState(() => _sending = false);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _sending = false;
        _error = error.toString();
      });
    }
  }

  void _scrollToEnd() {
    if (_scrollScheduled) return;
    _scrollScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scrollScheduled = false;
      if (!mounted || !_scrollController.hasClients) return;
      final end = _scrollController.position.minScrollExtent;
      if ((_scrollController.offset - end).abs() < 1) return;
      if (MediaQuery.disableAnimationsOf(context)) {
        _scrollController.jumpTo(end);
        return;
      }
      _scrollController.animateTo(
        end,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context, hasSubtitle: true),
        title: const Text('Ride chat'),
        subtitle: 'Ride #${widget.rideId}',
      ),
      body: AppGlassBackground(
        child: SafeArea(
          top: false,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final lineHeight = MediaQuery.textScalerOf(context).scale(24);
              final composerLines =
                  ((constraints.maxHeight * .42 - 44) / lineHeight)
                      .floor()
                      .clamp(1, 4);
              return Column(
                children: [
                  Expanded(
                    child: _loading
                        ? const SingleChildScrollView(
                            child: Padding(
                              padding: EdgeInsets.all(20),
                              child: AppShimmer(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    ShimmerBlock(
                                      width: 240,
                                      height: 72,
                                      radius: 18,
                                    ),
                                    SizedBox(height: 18),
                                    Align(
                                      alignment: Alignment.centerRight,
                                      child: ShimmerBlock(
                                        width: 220,
                                        height: 62,
                                        radius: 18,
                                      ),
                                    ),
                                    SizedBox(height: 18),
                                    ShimmerBlock(
                                      width: 180,
                                      height: 56,
                                      radius: 18,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          )
                        : !_chatAvailable || _error != null && _session == null
                        ? _UnavailableChat(
                            message:
                                _error ??
                                'Chat becomes available after the driver accepts the ride.',
                            onRetry: _chatAvailable ? _loadConversation : null,
                          )
                        : _messages.isEmpty
                        ? _EmptyChat(
                            partner: widget.role.toUpperCase() == 'DRIVER'
                                ? 'rider'
                                : 'driver',
                          )
                        : ListView.builder(
                            key: _messageListKey,
                            controller: _scrollController,
                            reverse: true,
                            keyboardDismissBehavior:
                                ScrollViewKeyboardDismissBehavior.onDrag,
                            padding: const EdgeInsets.fromLTRB(16, 14, 16, 18),
                            itemCount: _messages.length,
                            findChildIndexCallback: (key) {
                              final index =
                                  _messageIndexById[(key as ValueKey<String>)
                                      .value];
                              return index == null
                                  ? null
                                  : _messages.length - 1 - index;
                            },
                            itemBuilder: (context, index) {
                              final message =
                                  _messages[_messages.length - 1 - index];
                              return _MessageBubble(
                                key: ValueKey(message.messageId),
                                message: message,
                                own: message.sentById == widget.currentUserId,
                                onMounted: (context) =>
                                    _messageContexts[message.messageId] =
                                        context,
                                onUnmounted: (context) {
                                  if (identical(
                                    _messageContexts[message.messageId],
                                    context,
                                  )) {
                                    _messageContexts.remove(message.messageId);
                                  }
                                },
                              );
                            },
                          ),
                  ),
                  if (_error != null && _session != null)
                    ConstrainedBox(
                      constraints: BoxConstraints(
                        maxHeight: constraints.maxHeight * .24,
                      ),
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                        child: Text(
                          _error!,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: scheme.error,
                          ),
                        ),
                      ),
                    ),
                  if (_chatAvailable && _session != null)
                    _Composer(
                      controller: _composer,
                      sending: _sending,
                      onSend: _send,
                      maxLines: composerLines,
                    ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  final TextEditingController controller;
  final bool sending;
  final VoidCallback onSend;
  final int maxLines;

  const _Composer({
    required this.controller,
    required this.sending,
    required this.onSend,
    required this.maxLines,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SafeArea(
      top: false,
      minimum: const EdgeInsets.fromLTRB(12, 8, 12, 12),
      child: GlassSurface(
        borderRadius: AppRadii.cardRadius,
        padding: const EdgeInsets.fromLTRB(12, 4, 6, 4),
        child: Row(
          children: [
            Expanded(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight:
                      MediaQuery.textScalerOf(context).scale(24) * maxLines +
                      20,
                ),
                child: TextField(
                  controller: controller,
                  minLines: 1,
                  // Keep pasted line breaks even when the viewport only has
                  // room to display one line. The editor scrolls within its cap.
                  maxLines: null,
                  keyboardType: TextInputType.multiline,
                  textCapitalization: TextCapitalization.sentences,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => onSend(),
                  decoration: const InputDecoration(
                    hintText: 'Write a message…',
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    filled: false,
                    contentPadding: EdgeInsets.symmetric(vertical: 10),
                  ),
                ),
              ),
            ),
            IconButton(
              onPressed: sending ? null : onSend,
              tooltip: 'Send message',
              color: scheme.primary,
              icon: sending
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const AppIcon(SwiftIcons.send, size: 20),
            ),
          ],
        ),
      ),
    );
  }
}

class _MessageBubble extends StatefulWidget {
  final ChatMessage message;
  final bool own;
  final ValueChanged<BuildContext> onMounted;
  final ValueChanged<BuildContext> onUnmounted;

  const _MessageBubble({
    super.key,
    required this.message,
    required this.own,
    required this.onMounted,
    required this.onUnmounted,
  });

  @override
  State<_MessageBubble> createState() => _MessageBubbleState();
}

class _MessageBubbleState extends State<_MessageBubble> {
  @override
  void initState() {
    super.initState();
    widget.onMounted(context);
  }

  @override
  void activate() {
    super.activate();
    widget.onMounted(context);
  }

  @override
  void deactivate() {
    widget.onUnmounted(context);
    super.deactivate();
  }

  @override
  void dispose() {
    widget.onUnmounted(context);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final message = widget.message;
    final own = widget.own;
    final scheme = Theme.of(context).colorScheme;
    final color = own
        ? Color.alphaBlend(
            scheme.primary.withValues(alpha: .08),
            scheme.surface,
          )
        : scheme.surface;
    return Align(
      alignment: own ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 310),
        margin: const EdgeInsets.only(bottom: 9),
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 10),
        decoration: BoxDecoration(
          color: color,
          border: Border.all(color: Theme.of(context).dividerColor),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(own ? 16 : 4),
            bottomRight: Radius.circular(own ? 4 : 16),
          ),
        ),
        child: message.deleted
            ? Text(
                'This message was deleted',
                style: Theme.of(
                  context,
                ).textTheme.bodyMedium?.copyWith(fontStyle: FontStyle.italic),
              )
            : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(message.messageContent),
                  if (message.edited || message.messageType == 'REPLY') ...[
                    const SizedBox(height: 4),
                    Text(
                      message.edited ? 'edited' : 'reply',
                      style: Theme.of(context).textTheme.labelSmall,
                    ),
                  ],
                ],
              ),
      ),
    );
  }
}

class _EmptyChat extends StatelessWidget {
  const _EmptyChat({required this.partner});

  final String partner;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Text(
          'Say hello to your $partner',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ),
    );
  }
}

class _UnavailableChat extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;

  const _UnavailableChat({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppIcon(
              SwiftIcons.messageCircle,
              size: 24,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(height: 14),
            Text(message, textAlign: TextAlign.center),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: onRetry,
                child: const Text('Try again'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
