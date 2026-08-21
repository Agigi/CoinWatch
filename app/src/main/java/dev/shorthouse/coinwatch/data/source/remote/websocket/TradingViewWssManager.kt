package dev.shorthouse.coinwatch.data.source.remote.websocket

import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.math.BigDecimal
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class CoinRealtimeData(
    val id: String,
    val symbol: String,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val currentPrice: Price = Price("0"),
    val priceChangePercentage24h: Percentage = Percentage("0"),
    val highPrice: Price = Price("0"),
    val lowPrice: Price = Price("0"),
    val volume: String = "0"
)

class TradingViewWssManager() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var isHandshakeDone: Boolean = false
    private var isPaused: Boolean = false

    private val _coinsData = MutableStateFlow<Map<String, CoinRealtimeData>>(emptyMap())
    val coinsData: StateFlow<Map<String, CoinRealtimeData>> = _coinsData.asStateFlow()

    private val _chartCandles = MutableStateFlow<List<CandleEntry>>(emptyList())
    val chartCandles: StateFlow<List<CandleEntry>> = _chartCandles.asStateFlow()

    private val subscribedSymbols = mutableSetOf<String>()
    private var activeChartSymbol: String? = null
    private var activeTimeframe: String = "1D"
    private var activeChartSessionId: String? = null

    private var quoteSession = generateSessionId("qs_")

    private val okHttpClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    init {
        connect()
    }

    private fun generateSessionId(prefix: String): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder()
        for (i in 0 until 12) {
            sb.append(chars.random())
        }
        return "$prefix$sb"
    }

    fun pauseConnection() {
        isPaused = true
        Timber.d("Pausing WSS connection (App moved to background)")
        disconnect()
    }

    fun resumeConnection() {
        if (isPaused || webSocket == null) {
            isPaused = false
            Timber.d("Resuming WSS connection (App returned to foreground)")
            connect()
        }
    }

    private fun connect() {
        isHandshakeDone = false
        val url = "wss://data.tradingview.com/socket.io/websocket"

        Timber.d("Connecting directly to TradingView WSS: $url")
        val request = Request.Builder()
            .url(url)
            .addHeader("Origin", "https://www.tradingview.com")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
            )
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Connected to TradingView WSS socket. Initializing sessions...")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleRawMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "TradingView WSS Connection Failure")
                isHandshakeDone = false
                if (!isPaused) {
                    scope.launch {
                        kotlinx.coroutines.delay(5000)
                        if (!isPaused) {
                            connect()
                        }
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WSS Proxy closed: $reason")
                isHandshakeDone = false
            }
        })
    }

    private fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isHandshakeDone = false
    }

    private fun sendJson(json: String) {
        val packet = "~m~${json.length}~m~$json"
        webSocket?.send(packet)
    }

    private fun sendHeartbeat(num: String) {
        val payload = "~h~$num"
        val packet = "~m~${payload.length}~m~$payload"
        webSocket?.send(packet)
    }

    private fun initQuoteSession() {
        quoteSession = generateSessionId("qs_")
        sendJson("""{"m":"quote_create_session","p":["$quoteSession"]}""")
        sendJson("""{"m":"quote_set_fields","p":["$quoteSession","ch","chp","short_name","exchange","original_name","description","lp","ask","bid","open_price","high_price","low_price","prev_close_price","volume","spread","logoid","base-currency-logoid","currency-logoid","country","country_code","provider_id","sector","listed_exchange","industry","dividend_amount_recent","dividend_ex_date_recent","dividend_payout_ratio_ttm","earnings_release_date_fq_h","earnings_per_share_forecast_fq_h","earnings_per_share_fq_h","earnings_fiscal_period_fq_h","current_session","rtc","rch","rchp","rtc_time"]}""")
        sendJson("""{"m":"set_auth_token","p":["widget_user_token"]}""")
    }

    fun subscribeSymbols(symbols: List<String>) {
        val newSymbols = symbols.filter { subscribedSymbols.add(it) }
        if (newSymbols.isNotEmpty() && isHandshakeDone) {
            for (symbol in newSymbols) {
                sendJson("""{"m":"quote_add_symbols","p":["$quoteSession","$symbol"]}""")
            }
        }
    }

    private fun resubscribeAll() {
        for (symbol in subscribedSymbols) {
            sendJson("""{"m":"quote_add_symbols","p":["$quoteSession","$symbol"]}""")
        }
        activeChartSymbol?.let {
            requestChartData(it, activeTimeframe)
        }
    }

    fun requestChartData(symbol: String, timeframe: String) {
        _chartCandles.value = emptyList()
        activeChartSymbol = symbol
        activeTimeframe = timeframe

        val chartSession = generateSessionId("cs_")
        activeChartSessionId = chartSession
        val symbolId = generateSessionId("sym_")

        val tvResolution = when (timeframe) {
            "1m" -> "1"
            "3m" -> "3"
            "5m" -> "5"
            "15m" -> "15"
            "1h" -> "60"
            "4h" -> "240"
            "1D" -> "1D"
            "1W" -> "1W"
            "1M" -> "1M"
            else -> "1D"
        }

        if (isHandshakeDone) {
            sendJson("""{"m":"chart_create_session","p":["$chartSession",""]}""")
            sendJson("""{"m":"switch_timezone","p":["$chartSession","Etc/UTC"]}""")
            sendJson("""{"m":"resolve_symbol","p":["$chartSession","$symbolId","={\"session\":\"regular\",\"symbol\":\"$symbol\"}"]}""")
            sendJson("""{"m":"create_series","p":["$chartSession","s1","s1","$symbolId","$tvResolution",800,""]}""")
        }
    }

    fun clearChartData() {
        _chartCandles.value = emptyList()
        activeChartSymbol = null
        activeChartSessionId = null
    }

    private fun handleRawMessage(text: String) {
        if (!isHandshakeDone) {
            isHandshakeDone = true
            Timber.d("Received first message from WSS Proxy! Handshake complete. Initializing TV sessions...")
            initQuoteSession()
            resubscribeAll()
        }

        var remaining = text
        while (remaining.startsWith("~m~")) {
            val secondTilde = remaining.indexOf("~m~", 3)
            if (secondTilde == -1) break

            val lenStr = remaining.substring(3, secondTilde)
            val len = lenStr.toIntOrNull() ?: break

            val startIdx = secondTilde + 3
            if (startIdx + len > remaining.length) break

            val payload = remaining.substring(startIdx, startIdx + len)
            remaining = remaining.substring(startIdx + len)

            processPayload(payload)
        }
    }

    private fun processPayload(payload: String) {
        if (payload.startsWith("~h~")) {
            val num = payload.substring(3)
            sendHeartbeat(num)
            return
        }

        val jsonStr = if (payload.startsWith("~j~")) {
            payload.substring(3)
        } else if (payload.startsWith("{")) {
            payload
        } else {
            null
        }

        if (jsonStr != null) {
            try {
                val json = JSONObject(jsonStr)
                val m = json.optString("m")
                val p = json.optJSONArray("p")

                when (m) {
                    "qsd" -> parseQuoteData(p)
                    "timescale_update", "du", "data_update" -> parseChartData(p)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing JSON payload")
            }
        }
    }

    private fun parseQuoteData(p: JSONArray?) {
        if (p == null || p.length() < 2) return
        val dataObj = p.optJSONObject(1) ?: return
        val symbol = dataObj.optString("n")
        if (symbol.isEmpty()) return
        val values = dataObj.optJSONObject("v") ?: return

        val currentMap = _coinsData.value.toMutableMap()
        val existing = currentMap[symbol] ?: CoinRealtimeData(
            id = symbol,
            symbol = symbol.substringAfter(":"),
            name = symbol.substringAfter(":")
        )

        var updatedDesc = existing.description
        if (values.has("description")) {
            val desc = values.optString("description")
            if (desc.isNotEmpty()) updatedDesc = desc
        }

        var updatedImageUrl = existing.imageUrl
        val logoid = values.optString("logoid")
        val baseLogoId = values.optString("base-currency-logoid")
        val effLogoId = if (logoid.isNotEmpty()) logoid else baseLogoId
        if (effLogoId.isNotEmpty()) {
            updatedImageUrl = "https://s3-symbol-logo.tradingview.com/$effLogoId.svg"
        }

        var updatedPrice = existing.currentPrice
        if (values.has("lp")) {
            val lp = values.optDouble("lp", Double.NaN)
            if (!lp.isNaN()) {
                updatedPrice = Price(lp.toString())
            }
        }

        var updatedChp = existing.priceChangePercentage24h
        if (values.has("chp")) {
            val chp = values.optDouble("chp", Double.NaN)
            if (!chp.isNaN()) {
                updatedChp = Percentage(chp.toString())
            }
        }

        var updatedHigh = existing.highPrice
        if (values.has("high_price")) {
            val hp = values.optDouble("high_price", Double.NaN)
            if (!hp.isNaN()) {
                updatedHigh = Price(hp.toString())
            }
        }

        var updatedLow = existing.lowPrice
        if (values.has("low_price")) {
            val lp = values.optDouble("low_price", Double.NaN)
            if (!lp.isNaN()) {
                updatedLow = Price(lp.toString())
            }
        }

        var updatedVol = existing.volume
        if (values.has("volume")) {
            val vol = values.optDouble("volume", Double.NaN)
            if (!vol.isNaN()) {
                updatedVol = vol.toString()
            }
        }

        val updated = existing.copy(
            description = updatedDesc,
            imageUrl = updatedImageUrl,
            currentPrice = updatedPrice,
            priceChangePercentage24h = updatedChp,
            highPrice = updatedHigh,
            lowPrice = updatedLow,
            volume = updatedVol
        )

        currentMap[symbol] = updated
        _coinsData.value = currentMap
    }

    private fun parseChartData(p: JSONArray?) {
        if (p == null || p.length() < 2) return
        val sessionInPacket = p.optString(0)
        val currentSession = activeChartSessionId
        if (currentSession != null && sessionInPacket.isNotEmpty() && sessionInPacket != currentSession) {
            return
        }
        val payload = p.optJSONObject(1) ?: return

        val seriesObj = payload.optJSONObject("s1")
            ?: payload.keys().asSequence().mapNotNull { payload.optJSONObject(it) }.firstOrNull()
            ?: return

        val barsArray = seriesObj.optJSONArray("s") ?: seriesObj.optJSONArray("series") ?: return

        val currentCandles = _chartCandles.value.toMutableList()

        for (i in 0 until barsArray.length()) {
            val barObj = barsArray.optJSONObject(i) ?: continue
            val v = barObj.optJSONArray("v") ?: continue
            if (v.length() >= 5) {
                val time = v.optLong(0)
                val open = BigDecimal(v.optDouble(1).toString())
                val high = BigDecimal(v.optDouble(2).toString())
                val low = BigDecimal(v.optDouble(3).toString())
                val close = BigDecimal(v.optDouble(4).toString())
                val volume = if (v.length() >= 6) BigDecimal(v.optDouble(5).toString()) else BigDecimal.ZERO

                val candle = CandleEntry(open, high, low, close, volume, time)

                val existingIndex = currentCandles.indexOfFirst { it.timestamp == time }
                if (existingIndex != -1) {
                    currentCandles[existingIndex] = candle
                } else {
                    currentCandles.add(candle)
                }
            }
        }

        if (currentCandles.isNotEmpty()) {
            currentCandles.sortBy { it.timestamp }
            _chartCandles.value = currentCandles
        }
    }
}
