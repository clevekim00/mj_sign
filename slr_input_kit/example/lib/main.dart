import 'dart:async';

import 'package:flutter/material.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

import 'samples/demo_landmark_frame_source.dart';
import 'samples/sample_profile.dart';
import 'samples/sample_profiles.dart';

void main() {
  runApp(const SlrInputKitSampleApp());
}

class SlrInputKitSampleApp extends StatelessWidget {
  const SlrInputKitSampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    const ink = Color(0xFF0B1220);
    const surface = Color(0xFFF8FAFC);

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'LinguaSign Platform Samples',
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: surface,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF0F766E),
          brightness: Brightness.light,
        ),
        textTheme: ThemeData.light().textTheme.apply(
          bodyColor: ink,
          displayColor: ink,
        ),
      ),
      home: const PlatformSampleGallery(),
    );
  }
}

class PlatformSampleGallery extends StatefulWidget {
  const PlatformSampleGallery({super.key});

  @override
  State<PlatformSampleGallery> createState() => _PlatformSampleGalleryState();
}

class _PlatformSampleGalleryState extends State<PlatformSampleGallery> {
  final TextEditingController _resultController = TextEditingController();
  final TextEditingController _bridgeUrlController = TextEditingController();
  final TextEditingController _synthesisController = TextEditingController();
  final List<String> _timeline = <String>[];

  late PlatformSampleProfile _selectedProfile;
  late SignModelProfile _selectedModelProfile;
  List<SignModelProfile> _modelProfiles = const <SignModelProfile>[];
  late DemoLandmarkFrameSource _landmarkFrameSource;
  late String _activeBridgeUrl;
  SignSynthesisResult? _synthesisResult;
  String? _synthesisError;
  String? _profileLoadError;
  bool _synthesizing = false;
  bool _loadingProfiles = false;
  int _profileRequestSerial = 0;
  int _sourceRevision = 0;

  @override
  void initState() {
    super.initState();
    _selectedProfile = platformSampleProfiles.first;
    _modelProfiles = _fallbackProfiles();
    _selectedModelProfile = _selectNearestProfile(
      _modelProfiles,
      _profileFromPlatform(_selectedProfile),
      _modelProfiles.first,
    );
    _activeBridgeUrl = _selectedProfile.recommendedBridgeUrl;
    _bridgeUrlController.text = _activeBridgeUrl;
    _synthesisController.text = _selectedProfile.synthesisPrompt;
    _landmarkFrameSource = _createSource(_selectedProfile);
    unawaited(_loadModelProfiles());
  }

  @override
  void dispose() {
    _resultController.dispose();
    _bridgeUrlController.dispose();
    _synthesisController.dispose();
    super.dispose();
  }

  void _selectProfile(PlatformSampleProfile profile) {
    setState(() {
      _selectedProfile = profile;
      _selectedModelProfile = _selectNearestProfile(
        _modelProfiles,
        _profileFromPlatform(profile),
        _modelProfiles.first,
      );
      _activeBridgeUrl = profile.recommendedBridgeUrl;
      _bridgeUrlController.text = _activeBridgeUrl;
      _synthesisController.text = profile.synthesisPrompt;
      _landmarkFrameSource = _createSource(profile);
      _synthesisResult = null;
      _synthesisError = null;
      _sourceRevision++;
      _timeline.clear();
      _resultController.clear();
    });
  }

  Future<void> _synthesizeText() async {
    await _synthesize(sourceType: 'text');
  }

  Future<void> _synthesizeSpeech() async {
    await _synthesize(sourceType: 'speech');
  }

  Future<void> _synthesize({required String sourceType}) async {
    final text = _synthesisController.text.trim();
    if (text.isEmpty || _synthesizing) {
      return;
    }

    setState(() {
      _synthesizing = true;
      _synthesisError = null;
    });

    final client = SignSynthesisHttpClient(baseUrl: _activeHttpBaseUrl);
    try {
      final result = sourceType == 'speech'
          ? await client.synthesizeSpeechTranscript(
              sessionId: 'sample-${_selectedProfile.id}-sts',
              transcript: text,
              locale: _selectedModelProfile.locale,
              signLanguage: _selectedModelProfile.signLanguage,
              modelProfile: _selectedModelProfile.modelProfile,
            )
          : await client.synthesizeText(
              sessionId: 'sample-${_selectedProfile.id}-t2s',
              text: text,
              locale: _selectedModelProfile.locale,
              signLanguage: _selectedModelProfile.signLanguage,
              modelProfile: _selectedModelProfile.modelProfile,
            );
      if (!mounted) {
        return;
      }
      setState(() {
        _synthesisResult = result;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _synthesisError = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _synthesizing = false;
        });
      }
    }
  }

  String get _activeHttpBaseUrl {
    final uri = Uri.parse(_activeBridgeUrl);
    final scheme = uri.scheme == 'wss' ? 'https' : 'http';
    final port = uri.hasPort ? ':${uri.port}' : '';
    return '$scheme://${uri.host}$port';
  }

  void _applyBridgeUrl() {
    final nextUrl = _bridgeUrlController.text.trim();
    if (nextUrl.isEmpty || nextUrl == _activeBridgeUrl) {
      return;
    }
    setState(() {
      _activeBridgeUrl = nextUrl;
      _sourceRevision++;
    });
    unawaited(_loadModelProfiles());
  }

  Future<void> _loadModelProfiles() async {
    final requestSerial = ++_profileRequestSerial;
    final requestBaseUrl = _activeHttpBaseUrl;
    setState(() {
      _loadingProfiles = true;
      _profileLoadError = null;
    });
    try {
      final catalog = await SignModelProfileHttpClient(
        baseUrl: requestBaseUrl,
      ).fetchProfiles();
      if (!mounted || requestSerial != _profileRequestSerial) {
        return;
      }
      setState(() {
        _modelProfiles = catalog.profiles;
        _selectedModelProfile = _selectNearestProfile(
          catalog.profiles,
          _selectedModelProfile,
          catalog.defaultProfile,
        );
      });
    } catch (error) {
      if (!mounted || requestSerial != _profileRequestSerial) {
        return;
      }
      setState(() {
        _modelProfiles = _fallbackProfiles();
        _selectedModelProfile = _selectNearestProfile(
          _modelProfiles,
          _selectedModelProfile,
          _profileFromPlatform(_selectedProfile),
        );
        _profileLoadError = 'Using bundled profiles: $error';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loadingProfiles = false;
        });
      }
    }
  }

  void _selectModelProfile(SignModelProfile profile) {
    setState(() {
      _selectedModelProfile = profile;
      _synthesisResult = null;
      _synthesisError = null;
      _sourceRevision++;
    });
  }

  void _handleRecognizedText(String text) {
    setState(() {
      _resultController.text = text;
      _timeline.insert(0, text);
      if (_timeline.length > 5) {
        _timeline.removeLast();
      }
    });
  }

  DemoLandmarkFrameSource _createSource(PlatformSampleProfile profile) {
    return DemoLandmarkFrameSource(profile: profile);
  }

  SignModelProfile _profileFromPlatform(PlatformSampleProfile profile) {
    return SignModelProfile(
      locale: profile.locale,
      signLanguage: profile.signLanguage,
      modelProfile: profile.modelProfile,
    );
  }

  SignModelProfile _selectNearestProfile(
    List<SignModelProfile> profiles,
    SignModelProfile current,
    SignModelProfile fallback,
  ) {
    return profiles.firstWhere(
      (profile) => profile.modelProfile == current.modelProfile,
      orElse: () => profiles.firstWhere(
        (profile) => profile.signLanguage == current.signLanguage,
        orElse: () => fallback,
      ),
    );
  }

  List<SignModelProfile> _fallbackProfiles() {
    return platformSampleProfiles
        .map(_profileFromPlatform)
        .fold(<String, SignModelProfile>{}, (profiles, profile) {
          profiles[profile.modelProfile] = profile;
          return profiles;
        })
        .values
        .toList(growable: false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 960;
            final content = wide
                ? Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(child: _buildControlPanel()),
                      const SizedBox(width: 24),
                      Expanded(child: _buildBridgePanel()),
                    ],
                  )
                : Column(
                    children: [
                      _buildControlPanel(),
                      const SizedBox(height: 24),
                      _buildBridgePanel(),
                    ],
                  );

            return SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 1180),
                  child: content,
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildControlPanel() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _HeroCard(profile: _selectedProfile),
        const SizedBox(height: 20),
        _PlatformSelector(
          selectedProfile: _selectedProfile,
          onChanged: _selectProfile,
        ),
        const SizedBox(height: 20),
        _BridgeUrlCard(
          controller: _bridgeUrlController,
          activeBridgeUrl: _activeBridgeUrl,
          onApply: _applyBridgeUrl,
        ),
        const SizedBox(height: 20),
        _ModelProfileCard(
          profiles: _modelProfiles,
          selectedProfile: _selectedModelProfile,
          loading: _loadingProfiles,
          error: _profileLoadError,
          accentColor: _selectedProfile.accentColor,
          onChanged: _selectModelProfile,
          onRefresh: _loadModelProfiles,
        ),
        const SizedBox(height: 20),
        _ChecklistCard(
          title: 'Platform setup',
          items: _selectedProfile.setupChecklist,
          accentColor: _selectedProfile.accentColor,
        ),
        const SizedBox(height: 20),
        _ChecklistCard(
          title: 'Production upgrade path',
          items: _selectedProfile.productionChecklist,
          accentColor: _selectedProfile.accentColor,
        ),
      ],
    );
  }

  Widget _buildBridgePanel() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          height: 430,
          child: SlrInputWidget(
            key: ValueKey(
              '${_selectedProfile.id}-${_selectedModelProfile.modelProfile}-$_activeBridgeUrl-$_sourceRevision',
            ),
            bridgeUrl: _activeBridgeUrl,
            sessionId: 'sample-${_selectedProfile.id}',
            languageContext: _selectedModelProfile.toLanguageContext(),
            landmarkFrameSource: _landmarkFrameSource,
            disposeLandmarkFrameSource: true,
            onSignRecognized: _handleRecognizedText,
            placeholder: '수어 입력을 기다리는 중입니다...',
          ),
        ),
        const SizedBox(height: 20),
        _ResultCard(
          controller: _resultController,
          timeline: _timeline,
          accentColor: _selectedProfile.accentColor,
        ),
        const SizedBox(height: 20),
        _SynthesisCard(
          controller: _synthesisController,
          result: _synthesisResult,
          error: _synthesisError,
          synthesizing: _synthesizing,
          httpBaseUrl: _activeHttpBaseUrl,
          profile: _selectedProfile,
          modelProfile: _selectedModelProfile,
          onTextToSign: _synthesizeText,
          onSpeechToSign: _synthesizeSpeech,
        ),
        const SizedBox(height: 20),
        _RunCommandCard(profile: _selectedProfile),
      ],
    );
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({required this.profile});

  final PlatformSampleProfile profile;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            const Color(0xFF07111F),
            Color.alphaBlend(
              profile.accentColor.withValues(alpha: 0.32),
              const Color(0xFF0F172A),
            ),
          ],
        ),
        borderRadius: BorderRadius.circular(32),
        boxShadow: [
          BoxShadow(
            color: profile.accentColor.withValues(alpha: 0.18),
            blurRadius: 36,
            offset: const Offset(0, 20),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: Colors.white.withValues(alpha: 0.13),
                child: Icon(profile.icon, color: profile.accentColor, size: 30),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  'LinguaSign samples',
                  style: textTheme.titleMedium?.copyWith(
                    color: Colors.white70,
                    letterSpacing: 1.2,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 28),
          Text(
            profile.title,
            style: textTheme.displaySmall?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.w900,
              height: 0.98,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            profile.tagline,
            style: textTheme.titleMedium?.copyWith(
              color: Colors.white70,
              height: 1.45,
            ),
          ),
          const SizedBox(height: 24),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              _Pill(label: 'WebSocket bridge', color: profile.accentColor),
              _Pill(label: 'protobuf landmarks', color: profile.accentColor),
              _Pill(label: profile.modelProfile, color: profile.accentColor),
            ],
          ),
        ],
      ),
    );
  }
}

class _PlatformSelector extends StatelessWidget {
  const _PlatformSelector({
    required this.selectedProfile,
    required this.onChanged,
  });

  final PlatformSampleProfile selectedProfile;
  final ValueChanged<PlatformSampleProfile> onChanged;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Choose a platform sample',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 14),
          DropdownButtonFormField<PlatformSampleProfile>(
            initialValue: selectedProfile,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Target platform',
            ),
            items: platformSampleProfiles
                .map(
                  (profile) => DropdownMenuItem(
                    value: profile,
                    child: Row(
                      children: [
                        Icon(profile.icon, color: profile.accentColor),
                        const SizedBox(width: 10),
                        Text(profile.title),
                      ],
                    ),
                  ),
                )
                .toList(),
            onChanged: (profile) {
              if (profile != null) {
                onChanged(profile);
              }
            },
          ),
          const SizedBox(height: 14),
          Text(
            selectedProfile.landmarkSource,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: const Color(0xFF475569),
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }
}

class _BridgeUrlCard extends StatelessWidget {
  const _BridgeUrlCard({
    required this.controller,
    required this.activeBridgeUrl,
    required this.onApply,
  });

  final TextEditingController controller;
  final String activeBridgeUrl;
  final VoidCallback onApply;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Bridge connection',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: controller,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'WebSocket URL',
              helperText: 'Apply reconnects the sample widget with this URL.',
            ),
            onSubmitted: (_) => onApply(),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: Text(
                  'Active: $activeBridgeUrl',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: const Color(0xFF64748B),
                  ),
                ),
              ),
              FilledButton.icon(
                onPressed: onApply,
                icon: const Icon(Icons.cable_outlined),
                label: const Text('Apply'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ModelProfileCard extends StatelessWidget {
  const _ModelProfileCard({
    required this.profiles,
    required this.selectedProfile,
    required this.loading,
    required this.error,
    required this.accentColor,
    required this.onChanged,
    required this.onRefresh,
  });

  final List<SignModelProfile> profiles;
  final SignModelProfile selectedProfile;
  final bool loading;
  final String? error;
  final Color accentColor;
  final ValueChanged<SignModelProfile> onChanged;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Model profile',
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              IconButton(
                onPressed: loading ? null : onRefresh,
                icon: loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.refresh),
                tooltip: 'Refresh profiles',
              ),
            ],
          ),
          const SizedBox(height: 14),
          DropdownButtonFormField<SignModelProfile>(
            initialValue: selectedProfile,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Serving profile',
            ),
            items: profiles
                .map(
                  (profile) => DropdownMenuItem(
                    value: profile,
                    child: Text(
                      '${profile.locale} · ${profile.signLanguage} · ${profile.modelProfile}',
                    ),
                  ),
                )
                .toList(),
            onChanged: (profile) {
              if (profile != null) {
                onChanged(profile);
              }
            },
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _Pill(label: selectedProfile.protocolVersion, color: accentColor),
              if (selectedProfile.isDefault)
                _Pill(label: 'default', color: accentColor),
            ],
          ),
          if (error != null) ...[
            const SizedBox(height: 12),
            Text(
              error!,
              style: theme.textTheme.bodySmall?.copyWith(
                color: const Color(0xFFB45309),
                height: 1.35,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _ChecklistCard extends StatelessWidget {
  const _ChecklistCard({
    required this.title,
    required this.items,
    required this.accentColor,
  });

  final String title;
  final List<String> items;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 12),
          for (final item in items) ...[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.check_circle, color: accentColor, size: 20),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    item,
                    style: Theme.of(
                      context,
                    ).textTheme.bodyMedium?.copyWith(height: 1.45),
                  ),
                ),
              ],
            ),
            if (item != items.last) const SizedBox(height: 10),
          ],
        ],
      ),
    );
  }
}

class _ResultCard extends StatelessWidget {
  const _ResultCard({
    required this.controller,
    required this.timeline,
    required this.accentColor,
  });

  final TextEditingController controller;
  final List<String> timeline;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Recognized text',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: controller,
            minLines: 3,
            maxLines: 5,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: '수어 인식 결과',
            ),
          ),
          const SizedBox(height: 14),
          if (timeline.isEmpty)
            Text(
              'Final bridge results will appear here.',
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: const Color(0xFF64748B)),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: timeline
                  .map((entry) => _Pill(label: entry, color: accentColor))
                  .toList(),
            ),
        ],
      ),
    );
  }
}

class _SynthesisCard extends StatelessWidget {
  const _SynthesisCard({
    required this.controller,
    required this.result,
    required this.error,
    required this.synthesizing,
    required this.httpBaseUrl,
    required this.profile,
    required this.modelProfile,
    required this.onTextToSign,
    required this.onSpeechToSign,
  });

  final TextEditingController controller;
  final SignSynthesisResult? result;
  final String? error;
  final bool synthesizing;
  final String httpBaseUrl;
  final PlatformSampleProfile profile;
  final SignModelProfile modelProfile;
  final VoidCallback onTextToSign;
  final VoidCallback onSpeechToSign;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final result = this.result;
    final errorText = error;
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'SignGemma T2S / STS',
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            '$httpBaseUrl · ${modelProfile.locale} · ${modelProfile.signLanguage} · ${modelProfile.modelProfile}',
            style: theme.textTheme.bodySmall?.copyWith(
              color: const Color(0xFF64748B),
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: controller,
            minLines: 2,
            maxLines: 4,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Text or speech transcript',
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              FilledButton.icon(
                onPressed: synthesizing ? null : onTextToSign,
                icon: const Icon(Icons.pan_tool_alt_outlined),
                label: const Text('Text to Sign'),
              ),
              OutlinedButton.icon(
                onPressed: synthesizing ? null : onSpeechToSign,
                icon: const Icon(Icons.mic_none),
                label: const Text('Speech to Sign'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SizedBox(
            height: 260,
            child: SignOutputWidget(
              frames: result?.frames ?? const [],
              placeholder: synthesizing
                  ? 'Spring Boot synthesis 호출 중입니다...'
                  : 'Sign motion preview',
            ),
          ),
          const SizedBox(height: 12),
          if (errorText != null)
            Text(
              errorText,
              style: theme.textTheme.bodySmall?.copyWith(
                color: const Color(0xFFB91C1C),
                fontWeight: FontWeight.w700,
              ),
            )
          else if (result != null)
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final gloss in result.glosses)
                  _Pill(label: gloss, color: profile.accentColor),
              ],
            )
          else
            Text(
              'Spring Boot mock provider returns SignPlan + landmark frames; later this same contract can point at an official SignGemma-compatible provider.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: const Color(0xFF64748B),
                height: 1.45,
              ),
            ),
        ],
      ),
    );
  }
}

class _RunCommandCard extends StatelessWidget {
  const _RunCommandCard({required this.profile});

  final PlatformSampleProfile profile;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Run command',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF0F172A),
              borderRadius: BorderRadius.circular(18),
            ),
            child: Text(
              profile.runCommand,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Colors.white,
                fontFamily: 'monospace',
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Card extends StatelessWidget {
  const _Card({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF0F172A).withValues(alpha: 0.07),
            blurRadius: 24,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.38)),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
          color: const Color(0xFF0F172A),
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
