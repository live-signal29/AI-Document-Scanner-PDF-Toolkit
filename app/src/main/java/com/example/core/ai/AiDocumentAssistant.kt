package com.example.core.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedEntities(
    val dates: List<String>,
    val amounts: List<String>,
    val emails: List<String>,
    val phoneNumbers: List<String>,
    val names: List<String>
)

data class AiResponse(
    val content: String,
    val isOnlineAi: Boolean,
    val statusMessage: String
)

object AiDocumentAssistant {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the active API key (User custom key in Settings, or BuildConfig from .env).
     */
    fun getResolvedApiKey(customKey: String?): String? {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") key.trim() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks whether online AI service is configured.
     */
    fun isAiOnlineAvailable(customKey: String? = null): Boolean {
        return getResolvedApiKey(customKey) != null
    }

    /**
     * Generate document summary (Short or Detailed).
     */
    suspend fun summarizeDocument(
        text: String,
        isDetailed: Boolean,
        customKey: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext AiResponse(
                content = "No text content found in document to summarize.",
                isOnlineAi = false,
                statusMessage = "Empty document"
            )
        }

        val apiKey = getResolvedApiKey(customKey)
        if (apiKey != null) {
            val prompt = if (isDetailed) {
                "Provide a comprehensive, highly structured, in-depth analytical summary of the following document. Include key sections, implications, and action items:\n\n$text"
            } else {
                "Provide a crisp 2-to-3 sentence executive overview summarizing the core purpose and key outcome of the following document:\n\n$text"
            }
            val onlineResult = callGeminiApi(prompt, apiKey)
            if (onlineResult != null) {
                return@withContext AiResponse(
                    content = onlineResult,
                    isOnlineAi = true,
                    statusMessage = "Generated with Gemini AI"
                )
            }
        }

        // Offline / Local Intelligence fallback
        val localSummary = localExtractiveSummary(text, maxSentences = if (isDetailed) 6 else 2)
        AiResponse(
            content = localSummary,
            isOnlineAi = false,
            statusMessage = "Generated via On-Device Local Intelligence (Offline Mode)"
        )
    }

    /**
     * Extract key bullet points.
     */
    suspend fun extractKeyPoints(
        text: String,
        customKey: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext AiResponse("No text available", false, "Empty")

        val apiKey = getResolvedApiKey(customKey)
        if (apiKey != null) {
            val prompt = "Extract the top 5 to 8 most important takeaways, obligations, dates, or highlights from this document as clean bullet points:\n\n$text"
            val onlineResult = callGeminiApi(prompt, apiKey)
            if (onlineResult != null) {
                return@withContext AiResponse(onlineResult, true, "Generated with Gemini AI")
            }
        }

        // Local bullet extraction
        val points = localExtractKeySentences(text)
        val formatted = points.mapIndexed { idx, pt -> "• $pt" }.joinToString("\n\n")
        AiResponse(
            content = formatted.ifEmpty { "• " + text.take(150) + "…" },
            isOnlineAi = false,
            statusMessage = "Extracted via Local Heuristics (Offline Mode)"
        )
    }

    /**
     * Extract structured entities: Dates, Amounts, Emails, Phone Numbers, Names.
     */
    suspend fun extractEntities(
        text: String,
        customKey: String? = null
    ): Pair<ExtractedEntities, AiResponse> = withContext(Dispatchers.IO) {
        // Run robust on-device local pattern extractions
        val dates = mutableListOf<String>()
        val amounts = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val phones = mutableListOf<String>()
        val names = mutableListOf<String>()

        // 1. Email Regex
        val emailMatcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").matcher(text)
        while (emailMatcher.find()) {
            val match = emailMatcher.group()
            if (!emails.contains(match)) emails.add(match)
        }

        // 2. Dates Regex (DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD, Month Day Year)
        val datePattern = Pattern.compile(
            "\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
        )
        val dateMatcher = datePattern.matcher(text)
        while (dateMatcher.find()) {
            val match = dateMatcher.group()
            if (!dates.contains(match)) dates.add(match)
        }

        // 3. Currency / Amounts Regex ($1,234.56, €99, £45.00, USD 500, etc.)
        val amountPattern = Pattern.compile(
            "(?:[$€£¥₹]|USD|EUR|GBP|CAD|AUD|INR|PKR|AED)\\s?\\d{1,3}(?:[,.]\\d{3})*(?:[.,]\\d{1,2})?|\\b\\d{1,3}(?:,\\d{3})+(?:\\.\\d{2})?\\b"
        )
        val amountMatcher = amountPattern.matcher(text)
        while (amountMatcher.find()) {
            val match = amountMatcher.group().trim()
            if (!amounts.contains(match) && match.length > 1) amounts.add(match)
        }

        // 4. Phone Numbers Regex
        val phonePattern = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
        )
        val phoneMatcher = phonePattern.matcher(text)
        while (phoneMatcher.find()) {
            val match = phoneMatcher.group().trim()
            if (!phones.contains(match) && match.length >= 7) phones.add(match)
        }

        // 5. Proper Names heuristic / Online AI enhancement
        val apiKey = getResolvedApiKey(customKey)
        var status = "Local Entity Recognition (100% Offline)"
        var isOnline = false

        if (apiKey != null) {
            val prompt = "Extract any specific person names, company entities, or signatories from the following text as a comma-separated list. Only return the names:\n\n$text"
            val aiNames = callGeminiApi(prompt, apiKey)
            if (!aiNames.isNullOrBlank()) {
                aiNames.split(",").forEach { n ->
                    val clean = n.trim().removePrefix("- ").removePrefix("• ")
                    if (clean.isNotBlank() && clean.length > 2 && !names.contains(clean)) {
                        names.add(clean)
                    }
                }
                status = "Entities verified with Gemini AI"
                isOnline = true
            }
        }

        if (names.isEmpty()) {
            // Local fallback for names (Looking for common prefixes like Mr./Ms./Dr. or "Signed by")
            val namePattern = Pattern.compile("(?:Mr\\.|Mrs\\.|Ms\\.|Dr\\.|Prof\\.|Signed by|Name:)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)")
            val nMatcher = namePattern.matcher(text)
            while (nMatcher.find()) {
                val group = nMatcher.group(1)
                if (group != null && !names.contains(group)) names.add(group)
            }
        }

        val entities = ExtractedEntities(dates, amounts, emails, phones, names)
        val response = AiResponse(
            content = "Extracted ${dates.size} date(s), ${amounts.size} amount(s), ${emails.size} email(s), ${phones.size} phone(s), ${names.size} name(s).",
            isOnlineAi = isOnline,
            statusMessage = status
        )
        Pair(entities, response)
    }

    /**
     * Ask questions about document content (Q&A).
     */
    suspend fun askDocumentQuestion(
        documentText: String,
        question: String,
        customKey: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey(customKey)
        if (apiKey != null) {
            val prompt = "You are a professional document analysis assistant. Based ONLY on the provided document text, answer the user's question accurately and clearly.\n\nDocument text:\n$documentText\n\nQuestion:\n$question"
            val onlineResult = callGeminiApi(prompt, apiKey)
            if (onlineResult != null) {
                return@withContext AiResponse(onlineResult, true, "Answered with Gemini AI")
            }
        }

        // Local search for keywords in question
        val keywords = question.lowercase().split(" ")
            .filter { it.length > 3 && it !in listOf("what", "when", "where", "which", "about", "this", "that", "from", "with") }

        val matchingSentences = documentText.split(Regex("(?<=[.?!\\n])\\s+"))
            .filter { sentence -> keywords.any { kw -> sentence.contains(kw, ignoreCase = true) } }

        val content = if (matchingSentences.isNotEmpty()) {
            "Relevant findings from document:\n\n" + matchingSentences.take(4).joinToString("\n\n")
        } else {
            "Could not locate an exact match for '$question' offline. To enable comprehensive AI question answering, configure your Gemini API key in Settings."
        }

        AiResponse(
            content = content,
            isOnlineAi = false,
            statusMessage = "Local Keyword Scan (Offline Mode)"
        )
    }

    /**
     * Translate document text into target language.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String,
        customKey: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey(customKey)
        if (apiKey != null) {
            val prompt = "Translate the following document text accurately into $targetLanguage. Preserve document formatting and paragraph structure:\n\n$text"
            val onlineResult = callGeminiApi(prompt, apiKey)
            if (onlineResult != null) {
                return@withContext AiResponse(onlineResult, true, "Translated into $targetLanguage via Gemini AI")
            }
        }

        AiResponse(
            content = "Translation to $targetLanguage requires an active internet connection and configured Gemini API key. Please configure your API key in Settings.",
            isOnlineAi = false,
            statusMessage = "AI Translation Offline"
        )
    }

    /**
     * Calls the Gemini REST API securely.
     */
    private fun callGeminiApi(prompt: String, apiKey: String): String? {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    }
                    put(partObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return null
            }
            val bodyString = response.body?.string() ?: return null
            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            return parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Extractive sentence ranker for 100% offline smart document summary.
     */
    private fun localExtractiveSummary(text: String, maxSentences: Int): String {
        val sentences = text.split(Regex("(?<=[.?!\\n])\\s+")).map { it.trim() }.filter { it.length > 20 }
        if (sentences.isEmpty()) return text.take(200)
        if (sentences.size <= maxSentences) return sentences.joinToString(" ")

        // Compute word frequencies
        val wordFreq = mutableMapOf<String, Int>()
        val words = text.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 3 }
        words.forEach { w -> wordFreq[w] = (wordFreq[w] ?: 0) + 1 }

        val scoredSentences = sentences.mapIndexed { idx, s ->
            var score = 0
            val sWords = s.lowercase().split(Regex("[^a-zA-Z0-9]+"))
            sWords.forEach { w -> score += wordFreq[w] ?: 0 }
            // Boost beginning sentences
            if (idx == 0) score = (score * 1.5).toInt()
            Pair(s, score)
        }

        return scoredSentences.sortedByDescending { it.second }
            .take(maxSentences)
            .map { it.first }
            .joinToString(" ")
    }

    private fun localExtractKeySentences(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.?!\\n])\\s+")).map { it.trim() }.filter { it.length in 25..200 }
        if (sentences.isEmpty()) return listOf(text.take(150))
        return sentences.take(6)
    }
}
