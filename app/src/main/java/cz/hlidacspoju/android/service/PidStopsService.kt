package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.PidStopGroup
import cz.hlidacspoju.android.model.PidStopsDocument
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer

/**
 * Downloads and caches the public PID stop register (stop names, platforms, lines, directions)
 * from https://data.pid.cz/stops/json/stops.json. No API key required.
 */
class PidStopsService(
    private val api: PidStopsApi,
    private val cacheFile: File
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: PidStopsDocument? = null

    // Precomputed (group, normalizedName) pairs for the currently cached document, so searching
    // on every keystroke only needs to normalize the (short) query instead of every stop name.
    private var normalizedNamesCache: Pair<PidStopsDocument, List<Pair<PidStopGroup, String>>>? = null

    /** Returns the cached stop list, loading it from disk if needed but never hitting the network. */
    suspend fun getCached(): PidStopsDocument {
        cached?.let { return it }

        if (cacheFile.exists()) {
            runCatching {
                json.decodeFromString<PidStopsDocument>(cacheFile.readText())
            }.getOrNull()?.let {
                cached = it
                return it
            }
        }

        return refresh()
    }

    /** Downloads a fresh copy from the network, caches it to disk and in memory. */
    suspend fun refresh(): PidStopsDocument {
        val doc = api.getStops()

        cacheFile.parentFile?.mkdirs()
        val tmpFile = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        tmpFile.writeText(json.encodeToString(PidStopsDocument.serializer(), doc))
        tmpFile.copyTo(cacheFile, overwrite = true)
        tmpFile.delete()

        cached = doc
        return doc
    }

    /** Finds stop groups whose name contains the given (diacritics-insensitive) substring. */
    fun searchStopGroups(doc: PidStopsDocument, query: String): List<PidStopGroup> {
        if (query.isBlank()) return doc.stopGroups

        val normalizedQuery = normalize(query.trim())
        val normalizedNames = normalizedNamesFor(doc)
        return normalizedNames
            .filter { (_, normalizedName) -> normalizedName.contains(normalizedQuery, ignoreCase = true) }
            .map { it.first }
    }

    /** Exact (trim/diacritics-insensitive) match against a stop group name, used to auto-confirm
     * a typed stop name without requiring the user to pick it from the dropdown. */
    fun findExactMatch(doc: PidStopsDocument, query: String): PidStopGroup? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null
        val normalizedQuery = normalize(trimmed)
        return normalizedNamesFor(doc)
            .firstOrNull { (_, normalizedName) -> normalizedName.equals(normalizedQuery, ignoreCase = true) }
            ?.first
    }

    private fun normalizedNamesFor(doc: PidStopsDocument): List<Pair<PidStopGroup, String>> {
        normalizedNamesCache?.let { (cachedDoc, names) -> if (cachedDoc === doc) return names }

        val names = doc.stopGroups.map { it to normalize(it.name) }
        normalizedNamesCache = doc to names
        return names
    }

    private fun normalize(input: String): String {
        val formD = Normalizer.normalize(input, Normalizer.Form.NFD)
        return formD.replace(diacriticsRegex, "")
    }

    companion object {
        private val diacriticsRegex = Regex("\\p{Mn}+")
    }
}
