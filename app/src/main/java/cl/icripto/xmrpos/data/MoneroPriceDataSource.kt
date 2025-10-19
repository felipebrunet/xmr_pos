package cl.icripto.xmrpos.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MoneroPriceDataSource {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @Serializable
    data class CoinGeckoResponse(
        val monero: Map<String, Double>
    )

    suspend fun getMoneroPrice(fiatCurrency: String): Double? {
        return try {
            val response: CoinGeckoResponse = httpClient.get("https://api.coingecko.com/api/v3/simple/price?ids=monero&vs_currencies=$fiatCurrency").body()
            response.monero[fiatCurrency.lowercase()]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
