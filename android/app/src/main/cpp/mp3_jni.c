// Puente mínimo entre Kotlin y LAME. Ver Mp3Encoder.kt.
#include <jni.h>
#include "lame.h"

#define FN(name) Java_com_bloqq_mayusbloc_audio_Mp3Encoder_##name

JNIEXPORT jlong JNICALL FN(nativeInit)(JNIEnv *env, jclass c, jint sampleRate, jint channels, jint kbps) {
    lame_t l = lame_init();
    if (!l) return 0;
    lame_set_in_samplerate(l, sampleRate);
    lame_set_out_samplerate(l, sampleRate);
    lame_set_num_channels(l, channels);
    lame_set_mode(l, channels == 1 ? MONO : JOINT_STEREO);
    lame_set_brate(l, kbps);      // tasa fija (CBR)
    lame_set_quality(l, 2);       // calidad alta
    lame_set_bWriteVbrTag(l, 0);  // sin cabecera extra: no hay que reescribir el archivo al final
    if (lame_init_params(l) < 0) { lame_close(l); return 0; }
    return (jlong)(intptr_t)l;
}

// pcm: muestras de 16 bits intercaladas (L R L R...) si es estéreo. Devuelve bytes escritos en out.
JNIEXPORT jint JNICALL FN(nativeEncode)(JNIEnv *env, jclass c, jlong h, jshortArray pcm, jint frames,
                                        jint channels, jbyteArray out) {
    lame_t l = (lame_t)(intptr_t)h;
    jshort *in = (*env)->GetShortArrayElements(env, pcm, NULL);
    jbyte *o = (*env)->GetByteArrayElements(env, out, NULL);
    jsize cap = (*env)->GetArrayLength(env, out);
    int n = channels == 1
        ? lame_encode_buffer(l, in, in, frames, (unsigned char *)o, cap)
        : lame_encode_buffer_interleaved(l, in, frames, (unsigned char *)o, cap);
    (*env)->ReleaseShortArrayElements(env, pcm, in, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, o, 0);
    return n;
}

JNIEXPORT jint JNICALL FN(nativeFlush)(JNIEnv *env, jclass c, jlong h, jbyteArray out) {
    lame_t l = (lame_t)(intptr_t)h;
    jbyte *o = (*env)->GetByteArrayElements(env, out, NULL);
    jsize cap = (*env)->GetArrayLength(env, out);
    int n = lame_encode_flush(l, (unsigned char *)o, cap);
    (*env)->ReleaseByteArrayElements(env, out, o, 0);
    return n;
}

JNIEXPORT void JNICALL FN(nativeClose)(JNIEnv *env, jclass c, jlong h) {
    lame_close((lame_t)(intptr_t)h);
}
