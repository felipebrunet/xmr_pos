package cl.icripto.xmrpos.monero

import cl.icripto.xmrpos.Base58.decodeBase58

fun getPublicSpendKeyHex(address: String): String? {
    return try {
        // Decode the Base58 address to a byte array.
        val decodedBytes = address.decodeBase58()

        // Convert the byte array to a hex string.
        val decodedHexString = decodedBytes.joinToString("") {
            String.format("%02x", it)
        }

        // The public spend key is the first 32 bytes (64 hex characters) after the network byte (2 hex characters).
        if (decodedHexString.length >= 66) {
            decodedHexString.substring(2, 66)
        } else {
            null
        }
    } catch (e: Exception) {
        // Silently ignore exceptions during decoding and return null.
        null
    }
}
