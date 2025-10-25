package cl.icripto.xmrpos.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val httpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }
}

@Serializable
data class MempoolHashesResponse(
    @SerialName("tx_hashes")
    val txHashes: List<String> = emptyList()
)

@Serializable
data class TxsHashesRequest(
    @SerialName("txs_hashes")
    val txsHashes: List<String>,
    @SerialName("decode_as_json")
    val decodeAsJson: Boolean = true
)

@Serializable
data class EcdhInfo(
    val amount: String
)

@Serializable
data class RctSignatures(
    val ecdhInfo: List<EcdhInfo>
)

@Serializable
data class TaggedKey(
    val key: String
)

@Serializable
data class VoutTarget(
    @SerialName("tagged_key")
    val taggedKey: TaggedKey
)

@Serializable
data class Vout(
    val amount: Long,
    val target: VoutTarget
)

@Serializable
data class TransactionDetails(
    val extra: List<Int>,
    @SerialName("rct_signatures")
    val rctSignatures: RctSignatures,
    val vout: List<Vout>
)

fun getTxPublicKeyFromExtra(extra: List<Int>): String? {
    val txPubKeyTag = 1
    val pubKeyLength = 32
    if (extra.isEmpty() || extra[0] != txPubKeyTag) {
        return null
    }
    if (extra.size < pubKeyLength + 1) {
        return null
    }
    val pubkeyBytes = extra.subList(1, pubKeyLength + 1)
    return pubkeyBytes.joinToString("") { String.format("%02x", it and 0xFF) }
}

suspend fun fetchMempoolHashes(serverUrl: String): MempoolHashesResponse? {
    Log.d("MoneroRpc", "Attempting to fetch mempool hashes...")
    val url = serverUrl.removeSuffix("/")
    return try {
        httpClient.get("$url/get_transaction_pool_hashes").body()
    } catch (e: Exception) {
        Log.e("MoneroRpc", "Error fetching mempool hashes: ${e.message}", e)
        null
    }
}

suspend fun fetchFirstTransactionDetails(serverUrl: String, hashes: List<String>): TransactionDetails? {
    Log.d("MoneroRpc", "Attempting to fetch transaction details...")
    val url = serverUrl.removeSuffix("/")


    val httpResponse: HttpResponse? = try {
        httpClient.post("$url/get_transactions") {
            contentType(ContentType.Application.Json)
             setBody(TxsHashesRequest(txsHashes = hashes))
        }
    } catch (e: Exception) {
        Log.e("MoneroRpc", "Error fetching transactions: ${e.message}", e)
        return null
    }

    if (httpResponse?.status?.value == 200) {
        val responseBody: JsonObject = httpResponse.body()
        val status = responseBody["status"]?.jsonPrimitive?.content
        if (status != "OK") {
            Log.e("MoneroRpc", "Node returned status: $status")
            return null
        }

        val txsArray = responseBody["txs"]?.jsonArray
        if (txsArray != null && txsArray.isNotEmpty()) {
            val firstTx = txsArray.first().jsonObject
            val asJsonString = firstTx["as_json"]?.jsonPrimitive?.content

            if (!asJsonString.isNullOrBlank()) {
                return try {
                    val jsonParser = Json { ignoreUnknownKeys = true }
                    jsonParser.decodeFromString<TransactionDetails>(asJsonString)
                } catch (e: Exception) {
                    Log.e("MoneroRpc", "Error parsing transaction details: ${e.message}", e)
                    null
                }
            } else {
                Log.e("MoneroRpc", "as_json field is null or blank")
            }
        }
    } else {
        Log.e("MoneroRpc", "Failed to get transactions: ${httpResponse?.status?.value}")
    }
    return null
}
