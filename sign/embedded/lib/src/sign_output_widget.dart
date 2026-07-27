import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';

import 'generated/schema/landmark.pb.dart';

class SignOutputWidget extends StatefulWidget {
  const SignOutputWidget({
    super.key,
    this.frames = const <LandmarkFrame>[],
    this.frameBatches,
    this.autoPlay = true,
    this.frameInterval = const Duration(milliseconds: 84),
    this.placeholder = 'Text/Speech to Sign 결과를 기다리는 중입니다.',
    this.showDebugOverlay = true,
    this.showControls = true,
  });

  final List<LandmarkFrame> frames;
  final Stream<List<LandmarkFrame>>? frameBatches;
  final bool autoPlay;
  final Duration frameInterval;
  final String placeholder;
  final bool showDebugOverlay;
  final bool showControls;

  @override
  State<SignOutputWidget> createState() => _SignOutputWidgetState();
}

class _SignOutputWidgetState extends State<SignOutputWidget> {
  StreamSubscription<List<LandmarkFrame>>? _subscription;
  Timer? _timer;
  List<LandmarkFrame> _frames = const <LandmarkFrame>[];
  int _frameIndex = 0;
  bool _playing = true;
  int _speedIndex = 1;

  static const List<double> _speedOptions = <double>[0.5, 1.0, 1.5, 2.0];

  @override
  void initState() {
    super.initState();
    _frames = List<LandmarkFrame>.of(widget.frames);
    _playing = widget.autoPlay;
    _subscribe();
    _syncTimer();
  }

  @override
  void didUpdateWidget(covariant SignOutputWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(oldWidget.frames, widget.frames)) {
      _frames = List<LandmarkFrame>.of(widget.frames);
      _frameIndex = 0;
    }
    if (oldWidget.frameBatches != widget.frameBatches) {
      unawaited(_subscription?.cancel() ?? Future<void>.value());
      _subscribe();
    }
    if (oldWidget.autoPlay != widget.autoPlay ||
        oldWidget.frameInterval != widget.frameInterval ||
        oldWidget.frames.length != widget.frames.length) {
      _playing = widget.autoPlay;
      _syncTimer();
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    unawaited(_subscription?.cancel() ?? Future<void>.value());
    super.dispose();
  }

  void _subscribe() {
    final frameBatches = widget.frameBatches;
    if (frameBatches == null) {
      return;
    }
    _subscription = frameBatches.listen((frames) {
      if (!mounted) {
        return;
      }
      setState(() {
        _frames = List<LandmarkFrame>.of(frames);
        _frameIndex = 0;
      });
      _syncTimer();
    });
  }

  void _syncTimer() {
    _timer?.cancel();
    _timer = null;
    if (!_playing || _frames.length <= 1) {
      return;
    }
    _timer = Timer.periodic(_effectiveFrameInterval, (_) {
      if (!mounted || _frames.isEmpty) {
        return;
      }
      setState(() {
        _frameIndex = (_frameIndex + 1) % _frames.length;
      });
    });
  }

  Duration get _effectiveFrameInterval {
    final speed = _speedOptions[_speedIndex];
    final millis = max(
      16,
      (widget.frameInterval.inMilliseconds / speed).round(),
    );
    return Duration(milliseconds: millis);
  }

  void _togglePlayback() {
    if (_frames.length <= 1) {
      return;
    }
    setState(() {
      _playing = !_playing;
    });
    _syncTimer();
  }

  void _replay() {
    setState(() {
      _frameIndex = 0;
      _playing = widget.autoPlay || _frames.length > 1;
    });
    _syncTimer();
  }

  void _cycleSpeed() {
    setState(() {
      _speedIndex = (_speedIndex + 1) % _speedOptions.length;
    });
    _syncTimer();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final currentFrame = _frames.isEmpty ? null : _frames[_frameIndex];

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF08111F), Color(0xFF12343B), Color(0xFF0F766E)],
        ),
        borderRadius: BorderRadius.circular(28),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF0F766E).withValues(alpha: 0.24),
            blurRadius: 32,
            offset: const Offset(0, 18),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Stack(
          children: [
            Positioned.fill(
              child: CustomPaint(
                painter: _SynthesisLandmarkPainter(frame: currentFrame),
              ),
            ),
            if (currentFrame == null)
              Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    widget.placeholder,
                    textAlign: TextAlign.center,
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            if (widget.showDebugOverlay)
              Positioned(
                left: 16,
                right: widget.showControls ? 150 : 16,
                bottom: 16,
                child: _PlaybackStatusPill(
                  frameCount: _frames.length,
                  frameIndex: _frames.isEmpty ? 0 : _frameIndex + 1,
                  playing: _playing,
                  speed: _speedOptions[_speedIndex],
                ),
              ),
            if (widget.showControls)
              Positioned(
                right: 16,
                bottom: 16,
                child: _PlaybackControls(
                  hasFrames: _frames.isNotEmpty,
                  canPlay: _frames.length > 1,
                  playing: _playing,
                  speed: _speedOptions[_speedIndex],
                  onPlayPause: _togglePlayback,
                  onReplay: _replay,
                  onSpeed: _cycleSpeed,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _PlaybackStatusPill extends StatelessWidget {
  const _PlaybackStatusPill({
    required this.frameCount,
    required this.frameIndex,
    required this.playing,
    required this.speed,
  });

  final int frameCount;
  final int frameIndex;
  final bool playing;
  final double speed;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.32),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: Colors.white24),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        child: Text(
          frameCount == 0
              ? 'Sign synthesis playback ready'
              : 'Sign synthesis playback $frameIndex / $frameCount ${playing ? "playing" : "paused"} ${speed.toStringAsFixed(1)}x',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: Colors.white.withValues(alpha: 0.84),
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

class _PlaybackControls extends StatelessWidget {
  const _PlaybackControls({
    required this.hasFrames,
    required this.canPlay,
    required this.playing,
    required this.speed,
    required this.onPlayPause,
    required this.onReplay,
    required this.onSpeed,
  });

  final bool hasFrames;
  final bool canPlay;
  final bool playing;
  final double speed;
  final VoidCallback onPlayPause;
  final VoidCallback onReplay;
  final VoidCallback onSpeed;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.32),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: Colors.white24),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              tooltip: playing ? 'Pause playback' : 'Play motion',
              visualDensity: VisualDensity.compact,
              onPressed: canPlay ? onPlayPause : null,
              icon: Icon(
                playing ? Icons.pause_rounded : Icons.play_arrow_rounded,
                color: canPlay ? Colors.white : Colors.white38,
              ),
            ),
            IconButton(
              tooltip: 'Replay motion',
              visualDensity: VisualDensity.compact,
              onPressed: hasFrames ? onReplay : null,
              icon: Icon(
                Icons.replay_rounded,
                color: hasFrames ? Colors.white : Colors.white38,
              ),
            ),
            TextButton(
              onPressed: hasFrames ? onSpeed : null,
              child: Text(
                '${speed.toStringAsFixed(1)}x',
                style: TextStyle(
                  color: hasFrames ? Colors.white : Colors.white38,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SynthesisLandmarkPainter extends CustomPainter {
  const _SynthesisLandmarkPainter({required this.frame});

  final LandmarkFrame? frame;

  @override
  void paint(Canvas canvas, Size size) {
    _paintBackdrop(canvas, size);

    final frame = this.frame;
    if (frame == null) {
      _paintEmptyAvatar(canvas, size);
      return;
    }

    _drawPoints(canvas, size, frame.faceContour, const Color(0xFF99F6E4), 2.4);
    _drawPoints(canvas, size, frame.pose, const Color(0xFFE2E8F0), 3.8);
    _drawPoints(canvas, size, frame.leftHand, const Color(0xFF38BDF8), 4.4);
    _drawPoints(canvas, size, frame.rightHand, const Color(0xFFFACC15), 4.4);
  }

  void _paintBackdrop(Canvas canvas, Size size) {
    final center = Offset(size.width * 0.5, size.height * 0.45);
    final paint = Paint()
      ..shader =
          RadialGradient(
            colors: [
              Colors.white.withValues(alpha: 0.18),
              Colors.white.withValues(alpha: 0),
            ],
          ).createShader(
            Rect.fromCircle(center: center, radius: size.shortestSide),
          );
    canvas.drawCircle(center, size.shortestSide * 0.54, paint);
  }

  void _paintEmptyAvatar(Canvas canvas, Size size) {
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..color = Colors.white.withValues(alpha: 0.24);
    final center = Offset(size.width / 2, size.height * 0.46);
    canvas.drawCircle(center.translate(0, -size.height * 0.16), 32, paint);
    canvas.drawLine(
      center.translate(0, -size.height * 0.08),
      center.translate(0, size.height * 0.16),
      paint,
    );
    canvas.drawLine(
      center.translate(-size.width * 0.18, 0),
      center.translate(size.width * 0.18, 0),
      paint,
    );
  }

  void _drawPoints(
    Canvas canvas,
    Size size,
    List<Point3D> points,
    Color color,
    double radius,
  ) {
    if (points.isEmpty) {
      return;
    }

    final linePaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4
      ..strokeCap = StrokeCap.round
      ..color = color.withValues(alpha: 0.44);
    final pointPaint = Paint()..color = color;
    final glowPaint = Paint()
      ..color = color.withValues(alpha: 0.18)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8);

    Offset? previous;
    for (final point in points) {
      final offset = Offset(
        point.x.clamp(0.0, 1.0).toDouble() * size.width,
        point.y.clamp(0.0, 1.0).toDouble() * size.height,
      );
      if (previous != null &&
          (offset - previous).distance < size.shortestSide * 0.25) {
        canvas.drawLine(previous, offset, linePaint);
      }
      canvas.drawCircle(offset, radius * 2.4, glowPaint);
      canvas.drawCircle(
        offset,
        radius + min(point.z.abs() * 8, 2.4),
        pointPaint,
      );
      previous = offset;
    }
  }

  @override
  bool shouldRepaint(covariant _SynthesisLandmarkPainter oldDelegate) {
    return oldDelegate.frame != frame;
  }
}
