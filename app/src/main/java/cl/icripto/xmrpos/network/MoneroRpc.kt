package cl.icripto.xmrpos.network


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

@Serializable
data class JsonRpcBlockCountRequest(
    val jsonrpc: String = "2.0",
    val id: String = "0",
    val method: String = "get_block_count"
)

@Serializable
data class JsonRpcGetBlockRequest(
    val jsonrpc: String = "2.0",
    val id: String = "0",
    val method: String = "get_block",
    val params: GetBlockParams
)

@Serializable
data class JsonRpcResponse<T>(
    val result: T
)

@Serializable
data class BlockCountResult(
    val count: Long
)

@Serializable
data class BlockResult(
    @SerialName("tx_hashes")
    val txHashes: List<String>? = null
)

@Serializable
data class GetBlockParams(
    val height: Long
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
//    Log.d("MoneroRpc", "Attempting to fetch mempool hashes...")
    val url = serverUrl.removeSuffix("/")
    return try {
        httpClient.get("$url/get_transaction_pool_hashes").body()
    } catch (e: Exception) {
//        Log.e("MoneroRpc", "Error fetching mempool hashes: ${e.message}", e)
        null
    }
}

suspend fun fetchLastBlockHashes(serverUrl: String): List<String> {
    val jsonRpcUrl = serverUrl.removeSuffix("/") + "/json_rpc"
    val allTxHashes = mutableListOf<String>()

    // 1. Get current block height
    val height: Long
    try {
        val blockCountRequest = JsonRpcBlockCountRequest()
        val response: JsonRpcResponse<BlockCountResult> = httpClient.post(jsonRpcUrl) {
            contentType(ContentType.Application.Json)
            setBody(blockCountRequest)
        }.body()
        height = response.result.count
//        Log.d("MoneroRpc", "Current block height: $height")
    } catch (e: Exception) {
//        Log.e("MoneroRpc", "Error fetching block count: ${e.message}", e)
        return emptyList()
    }

    // 2. Fetch last 3 blocks
    for (i in 1..3) {
        val blockHeight = height - i
        if (blockHeight < 0) continue

        try {
            val getBlockParams = GetBlockParams(height = blockHeight)
            val getBlockRequest = JsonRpcGetBlockRequest(params = getBlockParams)
            val response: JsonRpcResponse<BlockResult> = httpClient.post(jsonRpcUrl) {
                contentType(ContentType.Application.Json)
                setBody(getBlockRequest)
            }.body()

            response.result.txHashes?.let {
                if (it.isNotEmpty()) {
//                    Log.d("PaymentScreen", "Found ${it.size} transactions in block $blockHeight")
                    allTxHashes.addAll(it)
                }
            }
        } catch (e: Exception) {
//            Log.e("PaymentScreen", "Error fetching block at height $blockHeight: ${e.message}", e)
            // Continue to next block even if one fails
        }
    }

    return allTxHashes
}

suspend fun fetchTransactionDetails(serverUrl: String, hashes: List<String>): List<TransactionDetails> {
//    Log.d("MoneroRpc", "Attempting to fetch transaction details for ${hashes.size} hashes...")
    val url = serverUrl.removeSuffix("/")
    val transactionDetailsList = mutableListOf<TransactionDetails>()

    if (hashes.isEmpty()) {
        return emptyList()
    }

    val httpResponse: HttpResponse? = try {
        httpClient.post("$url/get_transactions") {
            contentType(ContentType.Application.Json)
            setBody(TxsHashesRequest(txsHashes = hashes))
        }
    } catch (e: Exception) {
//        Log.e("MoneroRpc", "Error fetching transactions: ${e.message}", e)
        return emptyList()
    }

    if (httpResponse?.status?.value == 200) {
        val responseBody: JsonObject = httpResponse.body()
        val status = responseBody["status"]?.jsonPrimitive?.content
        if (status != "OK") {
//            Log.e("MoneroRpc", "Node returned status: $status")
            return emptyList()
        }

        val txsArray = responseBody["txs"]?.jsonArray
        if (txsArray != null && txsArray.isNotEmpty()) {
            val jsonParser = Json { ignoreUnknownKeys = true }
            for (txElement in txsArray) {
                val txObject = txElement.jsonObject
                val asJsonString = txObject["as_json"]?.jsonPrimitive?.content

                if (!asJsonString.isNullOrBlank()) {
                    try {
                        val details = jsonParser.decodeFromString<TransactionDetails>(asJsonString)
                        transactionDetailsList.add(details)
                    } catch (e: Exception) {
//                        Log.e("MoneroRpc", "Error parsing transaction details: ${e.message}", e)
                    }
                } else {
//                    Log.e("MoneroRpc", "as_json field is null or blank for a transaction")
                }
            }
        }
    } else {
//        Log.e("MoneroRpc", "Failed to get transactions: ${httpResponse?.status?.value}")
    }
    return transactionDetailsList
}
