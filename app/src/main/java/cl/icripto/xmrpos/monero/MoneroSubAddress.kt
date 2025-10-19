
package cl.icripto.xmrpos.monero

import cl.icripto.xmrpos.Base58.decodeBase58
import cl.icripto.xmrpos.Base58.encodeBase58
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.crypto.digests.KeccakDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.plus
import kotlin.collections.sliceArray
import kotlin.math.pow

class MoneroSubaddress {

    companion object {
        init {
            System.loadLibrary("ed25519_wrapper")
        }

        @JvmStatic
        external fun edwardsAdd(
            r: ByteArray,
            p: ByteArray,
            q: ByteArray
        )

        @JvmStatic
        external fun scalarmultBase(
            q: ByteArray,
            n: ByteArray
        )

        @JvmStatic
        external fun scalarmult(
            q: ByteArray,
            n: ByteArray,
            p: ByteArray
        )

        @JvmStatic
        external fun scalarReduce(
            r: ByteArray,
            s: ByteArray
        )

        @JvmStatic
        external fun scalarAdd(
            r: ByteArray,
            p: ByteArray,
            q: ByteArray
        )
    }

    private fun keccakHash(data: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        digest.update(data, 0, data.size)
        val hash = ByteArray(digest.digestSize)
        digest.doFinal(hash, 0)
        return hash
    }

    fun getAddressFinal(baseAddress: String, secretVk: String, major: Int, minor: Int): String {
        if (major < 0 || major >= 2.0.pow(32.0).toInt()) {
            throw kotlin.IllegalArgumentException("Major index $major is outside uint32 range")
        }
        if (minor < 0 || minor >= 2.0.pow(32.0).toInt()) {
            throw kotlin.IllegalArgumentException("Minor index $minor is outside uint32 range")
        }
        if (major == 0 && minor == 0) {
            return baseAddress
        }

        val masterSvk = Hex.decode(secretVk)
        val masterPsk = baseAddress.decodeBase58().sliceArray(1..32)
        val hsData = "SubAddr\u0000".toByteArray(Charsets.UTF_8) +
                masterSvk +
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(major).array() +
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(minor).array()
        val m = keccakHash(hsData)

        // D = master_psk + m * B
        val reducedScalar = ByteArray(32)
        scalarReduce(reducedScalar, m)

        val tempScalarMultB = ByteArray(32)
        scalarmultBase(tempScalarMultB, reducedScalar)

        val D = ByteArray(32)
        edwardsAdd(D, masterPsk, tempScalarMultB)
        // C = master_svk * D
        val C = ByteArray(32)
        scalarmult(C, masterSvk, D)

        val data = byteArrayOf(0x2A) + D + C
        val checksum = keccakHash(data).sliceArray(0..3)
        return (data + checksum).encodeBase58()
    }
}
