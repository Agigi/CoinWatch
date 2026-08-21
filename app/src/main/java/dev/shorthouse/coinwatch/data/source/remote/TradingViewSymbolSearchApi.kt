package dev.shorthouse.coinwatch.data.source.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder

data class TradingViewSymbolResult(
    val symbol: String,
    val fullName: String,
    val description: String,
    val exchange: String,
    val type: String = "",
    val typespecs: String = "",
    val logoUrl: String = "",
    val exchangeLogoUrl: String = ""
)

class TradingViewSymbolSearchApi {
    private val client = OkHttpClient()

    suspend fun searchSymbols(query: String): List<TradingViewSymbolResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(q, "UTF-8")

        val urls = listOf(
            "https://symbol-search.tradingview.com/symbol_search/v3/?text=$encodedQuery",
            "https://symbol-search.tradingview.com/symbol_search/?text=$encodedQuery"
        )

        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Origin", "https://www.tradingview.com")
                .header("Referer", "https://www.tradingview.com/")
                .header("Accept", "application/json, text/plain, */*")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: continue
                    val results = parseResponseBody(body)
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                } else {
                    Timber.w("Search request to $url returned code ${response.code}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error calling TradingView Symbol Search API: $url")
            }
        }
        emptyList()
    }

    private fun parseResponseBody(body: String): List<TradingViewSymbolResult> {
        val results = mutableListOf<TradingViewSymbolResult>()
        try {
            val jsonArray: JSONArray = if (body.trim().startsWith("{")) {
                val jsonObj = JSONObject(body)
                jsonObj.optJSONArray("symbols") ?: JSONArray()
            } else {
                JSONArray(body)
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val symbol = item.optString("symbol")
                if (symbol.isEmpty()) continue

                val exchange = item.optString("exchange")
                val prefix = item.optString("prefix", exchange)
                val ex = if (exchange.isNotEmpty()) exchange else prefix
                val rawFullName = item.optString("full_name")
                val fullName = if (rawFullName.isNotEmpty()) rawFullName else if (ex.isNotEmpty()) "$ex:$symbol" else symbol
                val description = item.optString("description", symbol)
                val type = item.optString("type")

                // Extract typespecs list (e.g. "spot crypto defi")
                val specsList = mutableListOf<String>()
                if (type.isNotEmpty()) specsList.add(type)
                val specsArr = item.optJSONArray("typespecs")
                if (specsArr != null) {
                    for (j in 0 until specsArr.length()) {
                        val s = specsArr.optString(j)
                        if (s.isNotEmpty() && !specsList.contains(s)) {
                            specsList.add(s)
                        }
                    }
                }
                val typespecsStr = specsList.joinToString(" ")

                // Logos from TradingView CDN
                val logoid = item.optString("logoid")
                val baseLogoId = item.optString("base-currency-logoid")
                val effLogoId = if (logoid.isNotEmpty()) logoid else baseLogoId
                val logoUrl = if (effLogoId.isNotEmpty()) {
                    "https://s3-symbol-logo.tradingview.com/$effLogoId.svg"
                } else ""

                val providerId = item.optString("provider_id")
                val exchangeLogo = item.optString("exchange_logo")
                val effExLogo = if (exchangeLogo.isNotEmpty()) exchangeLogo else if (providerId.isNotEmpty()) "provider/$providerId" else ""
                val exchangeLogoUrl = if (effExLogo.isNotEmpty()) {
                    "https://s3-symbol-logo.tradingview.com/$effExLogo.svg"
                } else ""

                results.add(
                    TradingViewSymbolResult(
                        symbol = symbol,
                        fullName = if (fullName.contains(":") || ex.isEmpty()) fullName else "$ex:$symbol",
                        description = description,
                        exchange = ex,
                        type = type,
                        typespecs = typespecsStr,
                        logoUrl = logoUrl,
                        exchangeLogoUrl = exchangeLogoUrl
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing TradingView response body")
        }
        return results
    }
}
