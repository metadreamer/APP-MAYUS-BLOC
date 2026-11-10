import 'dart:convert';

import 'package:flutter/services.dart';

/// Un beat del manifiesto (assets/beats/manifest.json).
class BeatInfo {
  final String id, name, style, file;
  final double bpm, seconds;
  final int bars;
  BeatInfo(Map<String, dynamic> j)
      : id = j['id'],
        name = j['name'],
        style = j['style'],
        file = j['file'],
        bpm = (j['bpm'] as num).toDouble(),
        seconds = (j['seconds'] as num).toDouble(),
        bars = j['bars'];
}

/// Una voz grabada.
class Take {
  final String name, path, route;
  final String? beatId;
  final double seconds, offsetMs, calMs;
  final int bytes;
  final bool syncMeasured, headphones, recovered;
  Take(Map m)
      : name = m['name'],
        path = m['path'],
        route = m['route'] ?? '',
        beatId = m['beatId'],
        seconds = (m['seconds'] as num).toDouble(),
        offsetMs = (m['offsetMs'] as num).toDouble(),
        calMs = (m['calMs'] as num).toDouble(),
        bytes = (m['bytes'] as num).toInt(),
        syncMeasured = m['syncMeasured'] ?? false,
        headphones = m['headphones'] ?? false,
        recovered = m['recovered'] ?? false;
}

/// Capa de audio por plataforma. La interfaz solo conoce esta clase:
/// para sumar iOS se escribe otra implementación, sin tocar las pantallas.
abstract class AudioEngine {
  Future<List<BeatInfo>> loadBeats();
  Future<void> playBeat(String id);
  Future<void> playFile(String path);
  Future<void> stopPlayback();

  /// Devuelve "ok", "denegado" o "bloqueado".
  Future<String> requestMic();
  Future<void> openSettings();
  Future<void> startRecording({String? beatId});
  Future<Take> stopRecording();
  Future<List<Take>> listTakes();
  Future<void> deleteTake(String name);
  Future<Map> calibrate();
  Future<void> setRawMic(bool raw);

  /// Arma la mezcla y devuelve la ruta del WAV, para escucharla.
  Future<String> mixPreview(String name, double voiceGain, double beatGain, double nudgeMs);

  /// kind: "voz" o "mezcla". format: "wav" o "mp3". Abre el menú de compartir.
  Future<Map> export(String name, String kind, String format,
      {double voiceGain = 1, double beatGain = 0.6, double nudgeMs = 0});
  Future<Map> status();
  Future<Map> storage();

  /// Detalle del último cierre inesperado, si lo hubo.
  Future<String?> lastCrash();
  Future<void> clearCrash();
  Future<String> deviceInfo();
}

/// Implementación para Android: habla con el motor en Kotlin (MainActivity.kt).
class AndroidAudioEngine implements AudioEngine {
  static const _ch = MethodChannel('mayusbloc/audio');

  @override
  Future<List<BeatInfo>> loadBeats() async {
    final manifest = jsonDecode(await rootBundle.loadString('assets/beats/manifest.json'));
    final beats = [for (final j in manifest['beats']) BeatInfo(j)];
    for (final b in beats) {
      await _ch.invokeMethod('loadBeat',
          {'id': b.id, 'asset': 'assets/beats/${b.file}', 'bpm': b.bpm, 'bars': b.bars});
    }
    return beats;
  }

  @override
  Future<void> playBeat(String id) => _ch.invokeMethod('playBeat', {'id': id});
  @override
  Future<void> playFile(String path) => _ch.invokeMethod('playFile', {'path': path});
  @override
  Future<void> stopPlayback() => _ch.invokeMethod('stopPlayback');
  @override
  Future<String> requestMic() async => await _ch.invokeMethod('requestMic');
  @override
  Future<void> openSettings() => _ch.invokeMethod('openSettings');
  @override
  Future<void> startRecording({String? beatId}) =>
      _ch.invokeMethod('startRecording', {'beatId': beatId});
  @override
  Future<Take> stopRecording() async => Take(await _ch.invokeMethod('stopRecording'));
  @override
  Future<List<Take>> listTakes() async =>
      [for (final m in await _ch.invokeMethod('listTakes')) Take(m)];
  @override
  Future<void> deleteTake(String name) => _ch.invokeMethod('deleteTake', {'name': name});
  @override
  Future<Map> calibrate() async => await _ch.invokeMethod('calibrate');
  @override
  Future<void> setRawMic(bool raw) => _ch.invokeMethod('setRawMic', {'raw': raw});
  @override
  Future<String> mixPreview(String name, double voiceGain, double beatGain, double nudgeMs) async =>
      await _ch.invokeMethod('mixPreview',
          {'name': name, 'voiceGain': voiceGain, 'beatGain': beatGain, 'nudgeMs': nudgeMs});
  @override
  Future<Map> export(String name, String kind, String format,
          {double voiceGain = 1, double beatGain = 0.6, double nudgeMs = 0}) async =>
      await _ch.invokeMethod('export', {
        'name': name,
        'kind': kind,
        'format': format,
        'voiceGain': voiceGain,
        'beatGain': beatGain,
        'nudgeMs': nudgeMs,
      });
  @override
  Future<Map> status() async => await _ch.invokeMethod('status');
  @override
  Future<Map> storage() async => await _ch.invokeMethod('storage');
  @override
  Future<String?> lastCrash() async => await _ch.invokeMethod('lastCrash');
  @override
  Future<void> clearCrash() => _ch.invokeMethod('clearCrash');
  @override
  Future<String> deviceInfo() async => await _ch.invokeMethod('deviceInfo');
}
