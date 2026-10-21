package io.github.bijojp.weekendvibes.updates

import io.github.bijojp.weekendvibes.domain.Release
import io.github.bijojp.weekendvibes.domain.Updates
import io.github.bijojp.weekendvibes.imports.PageFetcher
import java.io.IOException

/**
 * Check for updates (ADR 0002): one request to GitHub, made only when the user taps it in Settings.
 * Downloading and installing are left to the browser and Android's installer.
 */
class UpdateChecker(private val currentVersion: String) {
    /** The latest release when it's newer than this build, or null when this build is current. Throws when GitHub can't be asked. */
    suspend fun newerRelease(): Release? {
        val response = PageFetcher.json(LATEST_RELEASE, userAgent = "WeekendVibes/$currentVersion")
        if (response.status != 200) throw IOException("GitHub answered ${response.status}")
        val latest = Updates.parseLatest(response.html) ?: throw IOException("Not a release")
        return latest.takeIf { Updates.isNewer(it.version, currentVersion) }
    }

    companion object {
        /** Every release, newest first; opened when the check itself fails. */
        const val RELEASES_PAGE = "https://github.com/bijojp/weekendvibes/releases"
        private const val LATEST_RELEASE = "https://api.github.com/repos/bijojp/weekendvibes/releases/latest"
    }
}
