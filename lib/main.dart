// MÁYUS BLOC — Hito 1: prototipo técnico.
// Una sola pantalla, fea a propósito: sirve para probar el audio, no el diseño.
import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'audio/audio_engine.dart';

/// Último error de la interfaz, para mostrarlo en pantalla en vez de perderlo.
final ValueNotifier<String?> uiError = ValueNotifier(null);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  FlutterError.onError = (d) => uiError.value = '${d.exceptionAsString()}\n${d.stack}';
  PlatformDispatcher.instance.onError = (e, st) {
    uiError.value = '$e\n$st';
    return true;
  };
  // Si un pedazo de la pantalla falla, se ve el motivo en vez de un cuadro gris.
  ErrorWidget.builder = (d) => Material(
        color: const Color(0xFF400000),
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Text(d.exceptionAsString(), style: const TextStyle(color: Colors.white, fontSize: 12)),
        ),
      );
  runApp(const ProtoApp());
}

const appVersion = '0.1.1';

class ProtoApp extends StatelessWidget {
  const ProtoApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'MÁYUS BLOC',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          brightness: Brightness.dark,
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.amber, brightness: Brightness.dark),
          scaffoldBackgroundColor: const Color(0xFF101010),
        ),
        home: const ProtoScreen(),
      );
}

class ProtoScreen extends StatefulWidget {
  const ProtoScreen({super.key});
  @override
  State<ProtoScreen> createState() => _ProtoScreenState();
}

class _ProtoScreenState extends State<ProtoScreen> {
  final AudioEngine audio = AndroidAudioEngine();
  List<BeatInfo> beats = [];
  List<Take> takes = [];
  Map status = {};
  String? selectedBeat; // beat elegido para grabar encima
  String? openTake; // toma con el panel abierto
  bool overBeat = true;
  bool busy = false;
  bool warnedHeadphones = false;
  double voiceGain = 1.0, beatGain = 0.6, nudgeMs = 0;
  String message = 'Cargando beats…';
  String device = '';
  String? crash; // detalle del último cierre inesperado
  Map storage = {};
  int ticks = 0;
  Timer? timer;

  @override
  void initState() {
    super.initState();
    _init();
    // Consulta liviana del estado, varias veces por segundo.
    timer = Timer.periodic(const Duration(milliseconds: 250), (_) async {
      try {
        final s = await audio.status();
        // El espacio en disco se consulta cada tanto, no en cada vuelta.
        if (ticks++ % 20 == 0) storage = await audio.storage();
        if (mounted) setState(() => status = s);
      } catch (_) {}
    });
  }

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  Future<void> _init() async {
    try {
      device = await audio.deviceInfo();
      crash = await audio.lastCrash();
    } catch (_) {}
    await _guard(() async {
      beats = await audio.loadBeats();
      selectedBeat = beats.isNotEmpty ? beats.first.id : null;
      takes = await audio.listTakes();
      final rescued = takes.where((t) => t.recovered).length;
      message = rescued > 0 ? 'Se recuperaron $rescued grabaciones interrumpidas.' : 'Listo.';
    });
  }

  /// Ejecuta una acción mostrando cualquier error en pantalla, en vez de romper la app.
  Future<void> _guard(Future<void> Function() action) async {
    setState(() => busy = true);
    try {
      await action();
    } catch (e) {
      message = 'Error: ${e is Exception ? e.toString() : e}';
    }
    if (mounted) setState(() => busy = false);
  }

  bool get recording => status['recording'] == true;

  Future<void> _toggleRecord() async {
    if (recording) {
      await _guard(() async {
        final t = await audio.stopRecording();
        takes = await audio.listTakes();
        openTake = t.name;
        nudgeMs = 0;
        message = 'Guardada: ${t.name}';
      });
      return;
    }
    final perm = await audio.requestMic();
    if (perm != 'ok') {
      setState(() => message = perm == 'bloqueado'
          ? 'El micrófono está bloqueado para esta app. Tocá "Ajustes" para permitirlo.'
          : 'Sin permiso de micrófono no se puede grabar.');
      return;
    }
    final beatId = overBeat ? selectedBeat : null;
    if (beatId != null && status['headphones'] != true && !warnedHeadphones) {
      warnedHeadphones = true;
      setState(() => message =
          'Ojo: sin auriculares el micrófono también graba el beat. Tocá grabar de nuevo para seguir igual.');
      return;
    }
    await _guard(() async {
      await audio.startRecording(beatId: beatId);
      message = beatId == null ? 'Grabando voz sola…' : 'Grabando sobre el beat…';
    });
  }

  @override
  Widget build(BuildContext context) {
    final playingBeat = status['playingBeatId'];
    return Scaffold(
      appBar: AppBar(title: const Text('MÁYUS BLOC · prototipo 1')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          if (crash != null) _errorCard('La app se cerró por un error la vez anterior', crash!, () async {
            await audio.clearCrash();
            setState(() => crash = null);
          }),
          ValueListenableBuilder<String?>(
            valueListenable: uiError,
            builder: (_, e, _) => e == null
                ? const SizedBox.shrink()
                : _errorCard('Error de pantalla', e, () => uiError.value = null),
          ),
          if (status['lastError'] != null) _errorCard('Error de audio', '${status['lastError']}', null),
          _statusCard(),
          const _Title('1 · Beats (loop)'),
          for (final b in beats)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: IconButton(
                icon: Icon(selectedBeat == b.id ? Icons.radio_button_checked : Icons.radio_button_unchecked),
                onPressed: recording ? null : () => setState(() => selectedBeat = b.id),
              ),
              title: Text(b.name),
              subtitle: Text('${b.style} · ${b.bpm.round()} BPM · ${b.bars} compases · ${b.seconds} s'),
              trailing: IconButton.filledTonal(
                iconSize: 32,
                icon: Icon(playingBeat == b.id ? Icons.stop : Icons.play_arrow),
                onPressed: recording
                    ? null
                    : () => _guard(() => playingBeat == b.id ? audio.stopPlayback() : audio.playBeat(b.id)),
              ),
            ),
          const _Title('2 · Grabar'),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Sobre el beat elegido'),
            subtitle: const Text('Apagado = voz sola (acapella)'),
            value: overBeat,
            onChanged: recording ? null : (v) => setState(() => overBeat = v),
          ),
          SizedBox(
            height: 72,
            child: FilledButton.icon(
              style: FilledButton.styleFrom(backgroundColor: recording ? Colors.red : null),
              icon: Icon(recording ? Icons.stop : Icons.mic, size: 32),
              label: Text(recording ? 'PARAR  ${_time(((status['recSeconds'] ?? 0.0) as num).toDouble())}' : 'GRABAR',
                  style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
              onPressed: busy && !recording ? null : _toggleRecord,
            ),
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: recording ? ((status['level'] ?? 0.0) as num).toDouble().clamp(0.0, 1.0) : 0, minHeight: 8),
          const _Title('3 · Tomas'),
          if (takes.isEmpty) const Text('Todavía no hay grabaciones.'),
          for (final t in takes) _takeTile(t),
          const _Title('4 · Ajustes de prueba'),
          Wrap(spacing: 8, runSpacing: 8, children: [
            OutlinedButton(onPressed: busy || recording ? null : _calibrate, child: const Text('Calibrar latencia')),
            OutlinedButton(
              onPressed: recording
                  ? null
                  : () => _guard(() async {
                        final raw = status['micSource'] != 'crudo';
                        await audio.setRawMic(raw);
                        message = raw ? 'Micrófono en modo crudo.' : 'Micrófono en modo normal.';
                      }),
              child: Text('Micrófono: ${status['micSource'] ?? '…'}'),
            ),
            OutlinedButton(onPressed: audio.openSettings, child: const Text('Ajustes')),
          ]),
        ],
      ),
    );
  }

  Widget _statusCard() {
    final mb = ((storage['storageBytes'] ?? 0) as num) / 1e6;
    final freeGb = ((storage['freeBytes'] ?? 0) as num) / 1e9;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(message, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text('Salida: ${status['route'] ?? '…'}\n'
              'Sincronía de la última toma: ${status['lastSync'] ?? '…'}\n'
              'Calibración para esta salida: ${((status['calMs'] ?? 0.0) as num).toStringAsFixed(0)} ms\n'
              'Cortes de audio detectados: ${status['underruns'] ?? 0}\n'
              'Grabaciones: ${mb.toStringAsFixed(1)} MB · libre: ${freeGb.toStringAsFixed(1)} GB\n'
              '$device · versión $appVersion'),
        ]),
      ),
    );
  }

  /// Cartel rojo con el detalle de un error y un botón para copiarlo.
  Widget _errorCard(String title, String detail, VoidCallback? onClose) => Card(
        color: const Color(0xFF4A1010),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            Text(detail.length > 700 ? '${detail.substring(0, 700)}…' : detail, style: const TextStyle(fontSize: 12)),
            Row(children: [
              TextButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: '$device · $appVersion\n$title\n$detail'));
                  setState(() => message = 'Error copiado. Pegalo en el chat.');
                },
                child: const Text('Copiar'),
              ),
              if (onClose != null) TextButton(onPressed: onClose, child: const Text('Cerrar')),
            ]),
          ]),
        ),
      );

  Widget _takeTile(Take t) {
    final open = openTake == t.name;
    final playing = status['playingFile'] == t.path;
    final hasBeat = t.beatId != null;
    return Card(
      child: Column(children: [
        ListTile(
          title: Text(t.name),
          subtitle: Text('${_time(t.seconds)} · ${(t.bytes / 1e6).toStringAsFixed(1)} MB · '
              '${hasBeat ? 'sobre beat' : 'voz sola'}'
              '${hasBeat && !t.headphones ? ' · sin auriculares' : ''}'
              '${t.recovered ? ' · RECUPERADA' : ''}'),
          leading: IconButton(
            iconSize: 32,
            icon: Icon(playing ? Icons.stop : Icons.play_arrow),
            onPressed: recording ? null : () => _guard(() => playing ? audio.stopPlayback() : audio.playFile(t.path)),
          ),
          trailing: Icon(open ? Icons.expand_less : Icons.expand_more),
          onTap: () => setState(() {
            openTake = open ? null : t.name;
            nudgeMs = 0;
          }),
        ),
        if (open)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              if (hasBeat) ...[
                Text('Desfase medido: ${t.offsetMs.toStringAsFixed(0)} ms '
                    '(${t.syncMeasured ? 'con marcas de tiempo' : 'estimado'}) · '
                    'calibración: ${t.calMs.toStringAsFixed(0)} ms · ${t.route}'),
                _slider('Voz', voiceGain, 0, 2, (v) => voiceGain = v, '${(voiceGain * 100).round()} %'),
                _slider('Beat', beatGain, 0, 1, (v) => beatGain = v, '${(beatGain * 100).round()} %'),
                _slider('Ajuste fino', nudgeMs, -300, 300, (v) => nudgeMs = v.roundToDouble(), '${nudgeMs.round()} ms'),
                FilledButton.tonalIcon(
                  icon: const Icon(Icons.hearing),
                  label: const Text('Escuchar mezcla'),
                  onPressed: busy || recording
                      ? null
                      : () => _guard(() async {
                            final path = await audio.mixPreview(t.name, voiceGain, beatGain, nudgeMs);
                            await audio.playFile(path);
                          }),
                ),
                const SizedBox(height: 8),
              ],
              const Text('Exportar y compartir:'),
              Wrap(spacing: 8, children: [
                _exportButton(t, 'voz', 'wav'),
                _exportButton(t, 'voz', 'mp3'),
                if (hasBeat) _exportButton(t, 'mezcla', 'wav'),
                if (hasBeat) _exportButton(t, 'mezcla', 'mp3'),
                TextButton(
                  onPressed: busy || recording
                      ? null
                      : () => _guard(() async {
                            await audio.deleteTake(t.name);
                            takes = await audio.listTakes();
                            message = 'Borrada: ${t.name}';
                          }),
                  child: const Text('Borrar', style: TextStyle(color: Colors.redAccent)),
                ),
              ]),
            ]),
          ),
      ]),
    );
  }

  Widget _exportButton(Take t, String kind, String format) => OutlinedButton(
        onPressed: busy || recording
            ? null
            : () => _guard(() async {
                  final sw = Stopwatch()..start();
                  final r = await audio.export(t.name, kind, format,
                      voiceGain: voiceGain, beatGain: beatGain, nudgeMs: nudgeMs);
                  message = '${r['file']} · ${((r['bytes'] as num) / 1e6).toStringAsFixed(2)} MB · '
                      'armado en ${(sw.elapsedMilliseconds / 1000).toStringAsFixed(1)} s';
                }),
        child: Text('$kind ${format.toUpperCase()}'),
      );

  Widget _slider(String label, double value, double min, double max, void Function(double) set, String shown) => Row(
        children: [
          SizedBox(width: 84, child: Text(label)),
          Expanded(
            child: Slider(value: value, min: min, max: max, onChanged: (v) => setState(() => set(v))),
          ),
          SizedBox(width: 64, child: Text(shown, textAlign: TextAlign.end)),
        ],
      );

  Future<void> _calibrate() async {
    setState(() => message = 'Calibrando: van a sonar 7 clics. Con auriculares, acercá uno al micrófono (abajo del celular).');
    await _guard(() async {
      await Future.delayed(const Duration(seconds: 3));
      final r = await audio.calibrate();
      message = r['ok'] == true
          ? 'Calibrado para ${r['route']}: ${(r['ms'] as num).toStringAsFixed(0)} ms de corrección.'
          : 'No escuché los clics con claridad. Subí el volumen o acercá más el auricular al micrófono.';
    });
  }

  String _time(double s) {
    final m = s ~/ 60, r = (s % 60).floor();
    return '$m:${r.toString().padLeft(2, '0')}';
  }
}

class _Title extends StatelessWidget {
  final String text;
  const _Title(this.text);
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 20, bottom: 6),
        child: Text(text.toUpperCase(), style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, letterSpacing: 1)),
      );
}
