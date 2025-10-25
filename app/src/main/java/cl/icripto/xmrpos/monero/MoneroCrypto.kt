package cl.icripto.xmrpos.monero

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes an integer into a variable-length integer format.
 */
private fun encodeVarint(value: Int): ByteArray {
    val out = mutableListOf<Byte>()
    var v = value
    while (v >= 0x80) {
        out.add(((v and 0x7F) or 0x80).toByte())
        v = v ushr 7
    }
    out.add(v.toByte())
    return out.toByteArray()
}

/**
 * Decodes a transaction amount and verifies it against an expected value.
 */
fun verifyAmount(
    privateViewKeyHex: String,
    txPublicKeyHex: String,
    ecdhAmountHex: String,
    outputIndex: Int,
    amountToReceive: Double
) {
    val privateViewKey = privateViewKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val txPublicKey = txPublicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val ecdhAmount = ecdhAmountHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    val subaddress = MoneroSubaddress()

    // 8 * private_view_key
    val svk2 = ByteArray(32).apply { MoneroSubaddress.scalarAdd(this, privateViewKey, privateViewKey) }
    val svk4 = ByteArray(32).apply { MoneroSubaddress.scalarAdd(this, svk2, svk2) }
    val svk8 = ByteArray(32).apply { MoneroSubaddress.scalarAdd(this, svk4, svk4) }

    // key_derivation = scalarmult(svk_8, tx_public_key)
    val keyDerivation = ByteArray(32).apply { MoneroSubaddress.scalarmult(this, svk8, txPublicKey) }

    val indexBytes = encodeVarint(outputIndex)
    val derivationHashInput = keyDerivation + indexBytes
    val derivationHash = subaddress.keccakHash(derivationHashInput)
    val derivationScalar = ByteArray(32).apply { MoneroSubaddress.scalarReduce(this, derivationHash) }

    // Amount decryption
    val amountKeyInput = "amount".toByteArray(Charsets.UTF_8) + derivationScalar
    val amountKey = subaddress.keccakHash(amountKeyInput)

    val decryptedBytes = ByteArray(ecdhAmount.size)
    for (i in ecdhAmount.indices) {
        decryptedBytes[i] = (ecdhAmount[i].toInt() xor amountKey[i].toInt()).toByte()
    }

    // Convert little-endian bytes to a Long.
    val decryptedAmount = ByteBuffer.wrap(decryptedBytes.reversedArray()).order(ByteOrder.BIG_ENDIAN).long

    val amountXmr = decryptedAmount / 1_000_000_000_000.0

    if (amountToReceive == amountXmr) {
        Log.d("MoneroCrypto", "Success!!! Amount match: $amountXmr")
    } else {
        Log.d("MoneroCrypto", "Amount does not match. Required amount: $amountToReceive, Detected amount $amountXmr")
    }
}
