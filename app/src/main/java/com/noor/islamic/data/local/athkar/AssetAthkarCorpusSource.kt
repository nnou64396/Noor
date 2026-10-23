package com.noor.islamic.data.local.athkar

import android.content.Context
import android.util.Log
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the adhkar corpus from the APK.
 *
 * The asset is read once and cached, because it is read-only bundled content:
 * there is no network path, no refresh and no partial update. Any problem
 * (missing file, wrong shape, failed validation) resolves to
 * [AthkarCorpus.empty] with a note, so the feature shows an explained state
 * instead of crashing — and, critically, so a rejected corpus can never leak
 * onto the screen.
 */
class AssetAthkarCorpusSource(
    private val context: Context,
    private val assetPath: String = DEFAULT_ASSET_PATH,
    private val parser: AthkarCorpusParser = AthkarCorpusParser(),
    private val validator: AthkarCorpusValidator = AthkarCorpusValidator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Reads [assetPath] as UTF-8. Injected so the fallback paths are testable. */
    private val assetReader: (String) -> String = { path ->
        context.assets.open(path).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).readText()
        }
    },
) : AthkarCorpusLoader {

    @Volatile
    private var cached: AthkarCorpus? = null

    override suspend fun load(): AthkarCorpus = cached ?: withContext(ioDispatcher) {
        synchronized(this@AssetAthkarCorpusSource) {
            cached ?: readAndValidate().also { cached = it }
        }
    }

    private fun readAndValidate(): AthkarCorpus {
        val json = runCatching { readAsset() }
            .getOrElse { return unavailable("adhkar asset could not be read: ${it.message}") }

        val corpus = runCatching { parser.parse(json) }
            .getOrElse {
                return unavailable("adhkar asset is malformed: ${it.message}")
            }

        // A corpus that fails the content rules is dropped entirely rather than
        // partially displayed.
        val problems = runCatching { validator.validate(corpus) }
            .getOrElse {
                return unavailable("adhkar validation failed: ${it.message}")
            }
        if (problems.isNotEmpty()) {
            return unavailable("adhkar asset was rejected: ${problems.first()}")
        }
        return corpus
    }

    /**
     * Keeps the user-facing state generic while leaving a safe diagnostic trail
     * for developers. Corpus errors describe the asset shape, never user data.
     */
    private fun unavailable(note: String): AthkarCorpus {
        Log.w(TAG, note)
        return AthkarCorpus.empty(note)
    }

    private fun readAsset(): String = assetReader(assetPath)

    companion object {
        private const val TAG = "AthkarCorpusSource"
        const val DEFAULT_ASSET_PATH = "athkar/adhkar.json"
    }
}
