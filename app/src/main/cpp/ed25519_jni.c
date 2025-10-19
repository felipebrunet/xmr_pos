#include <jni.h>
#include <string.h>
#include <sodium.h>

JNIEXPORT void JNICALL
Java_cl_icripto_xmrpos_monero_MoneroSubaddress_edwardsAdd(
        JNIEnv *env, jclass clazz, jbyteArray r_array, jbyteArray p_array, jbyteArray q_array) {

    jbyte *r = (*env)->GetByteArrayElements(env, r_array, NULL);
    jbyte *p = (*env)->GetByteArrayElements(env, p_array, NULL);
    jbyte *q = (*env)->GetByteArrayElements(env, q_array, NULL);

    crypto_core_ed25519_add((unsigned char *) r, (const unsigned char *) p, (const unsigned char *) q);

    (*env)->ReleaseByteArrayElements(env, r_array, r, 0);
    (*env)->ReleaseByteArrayElements(env, p_array, p, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, q_array, q, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_cl_icripto_xmrpos_monero_MoneroSubaddress_scalarmultBase(
        JNIEnv *env, jclass clazz, jbyteArray q_array, jbyteArray n_array) {

    jbyte *q = (*env)->GetByteArrayElements(env, q_array, NULL);
    jbyte *n = (*env)->GetByteArrayElements(env, n_array, NULL);

    crypto_scalarmult_ed25519_base_noclamp((unsigned char *) q, (const unsigned char *) n);

    (*env)->ReleaseByteArrayElements(env, q_array, q, 0);
    (*env)->ReleaseByteArrayElements(env, n_array, n, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_cl_icripto_xmrpos_monero_MoneroSubaddress_scalarmult(
        JNIEnv *env, jclass clazz, jbyteArray q_array, jbyteArray n_array, jbyteArray p_array) {

    jbyte *q = (*env)->GetByteArrayElements(env, q_array, NULL);
    jbyte *n = (*env)->GetByteArrayElements(env, n_array, NULL);
    jbyte *p = (*env)->GetByteArrayElements(env, p_array, NULL);

    crypto_scalarmult_ed25519_noclamp((unsigned char *) q, (const unsigned char *) n, (const unsigned char *) p);

    (*env)->ReleaseByteArrayElements(env, q_array, q, 0);
    (*env)->ReleaseByteArrayElements(env, n_array, n, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, p_array, p, JNI_ABORT);
}


// Corrected scalarReduce function in ed25519_jni.c
JNIEXPORT void JNICALL
Java_cl_icripto_xmrpos_monero_MoneroSubaddress_scalarReduce(
        JNIEnv *env, jclass clazz, jbyteArray r_array, jbyteArray s_array) {

    // Get the input byte array from Kotlin
    jbyte *s = (*env)->GetByteArrayElements(env, s_array, NULL);
    jsize s_len = (*env)->GetArrayLength(env, s_array);

    // Create a 64-byte array and zero it out
    unsigned char padded_s[64];
    memset(padded_s, 0, sizeof(padded_s));

    // Copy the input hash (32 bytes) into the padded array
    memcpy(padded_s, (const unsigned char *)s, s_len);

    // Get the output byte array from Kotlin
    jbyte *r = (*env)->GetByteArrayElements(env, r_array, NULL);

    // Call the libsodium function with the padded 64-byte input
    crypto_core_ed25519_scalar_reduce((unsigned char *) r, padded_s);

    // Release memory
    (*env)->ReleaseByteArrayElements(env, r_array, r, 0);
    (*env)->ReleaseByteArrayElements(env, s_array, s, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_cl_icripto_xmrpos_monero_MoneroSubaddress_scalarAdd(
        JNIEnv *env, jclass clazz, jbyteArray r_array, jbyteArray p_array, jbyteArray q_array) {

    jbyte *r = (*env)->GetByteArrayElements(env, r_array, NULL);
    jbyte *p = (*env)->GetByteArrayElements(env, p_array, NULL);
    jbyte *q = (*env)->GetByteArrayElements(env, q_array, NULL);

    crypto_core_ed25519_scalar_add((unsigned char *) r, (const unsigned char *) p, (const unsigned char *) q);

    (*env)->ReleaseByteArrayElements(env, r_array, r, 0);
    (*env)->ReleaseByteArrayElements(env, p_array, p, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, q_array, q, JNI_ABORT);
}
