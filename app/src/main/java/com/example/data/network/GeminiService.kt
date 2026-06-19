package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class MoshiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    @Json(name = "parts") val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentRequest(
    @Json(name = "contents") val contents: List<MoshiContent>
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<MoshiCandidate>?
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    @Json(name = "content") val content: MoshiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: MoshiGenerateContentRequest
    ): MoshiGenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    /**
     * Generates custom esports-style cyberpunk commentary for safe, exciting laser battles.
     */
    suspend fun generateEsportsCommentary(
        gameMode: String,
        playerScore: Int,
        opponentScore: Int,
        events: List<String>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackCommentary(gameMode, playerScore, opponentScore, events)
        }

        val eventSummaryString = if (events.isNotEmpty()) {
            events.joinToString(separator = "\n- ", prefix = "\n- ")
        } else {
            "No significant collisions, standard photon exchange."
        }

        val systemPrompt = "You are NEON-CORE, the over-the-top Cyberpunk E-sports Synthesizer Arena Commentator. " +
                "Reconstruct these laser PVP events into a rapid-fire, adrenaline-packed, 3-sentence summary of the combat. " +
                "Use high-tech sci-fi gaming vocabulary (e.g., photon cascades, specular deflection, capacitor burn, relativistic vectors). " +
                "Conclude with a social media style highlight shout-out and hashtags like #NeonLaserArena #PhotonDevastation."

        val userPrompt = """
            Match Mode: $gameMode
            Final Score: Player ($playerScore) vs Opponent ($opponentScore)
            Key Match Events Recorded:
            $eventSummaryString
            
            Synthesize an epic highlight review based on the system instructions. Keep it compact for screen sharing!
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\n$userPrompt"

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(fullPrompt)))
            )
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "COMMUNICATOR TRANSCEIVER OFFLINE: Null reply received from the Neon-Core downlink."
        } catch (e: Exception) {
            "Downlink error: ${e.message}\n\n" + getLocalFallbackCommentary(gameMode, playerScore, opponentScore, events)
        }
    }

    private fun getLocalFallbackCommentary(
        gameMode: String,
        playerScore: Int,
        opponentScore: Int,
        events: List<String>
    ): String {
        val didPlayerWin = playerScore >= opponentScore
        val winnerStr = if (didPlayerWin) "PLAYER" else "OPPONENT"
        val laserStyleList = listOf("RICCOCHET PULSE", "HYPERBEAM BLAST", "PHOTON CRUCIBLE")
        val randStyle = laserStyleList.random()
        return """
            [NEON DOWNLINK SIM_COM V1.0]
            UNBELIEVABLE LASER EXCHANGE! Match complete in mode "$gameMode". 
            With a final score of $playerScore to $opponentScore, $winnerStr secures dominance in the arena using dynamic $randStyle tactics!
            Reflected photon paths and extreme kinetic vectors registered on all scanners.
            
            #NeonLaserArena #SpaceDuel #$randStyle #CyberSports
        """.trimIndent()
    }
}
