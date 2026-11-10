package io.github.vlukyanets.lemonflow

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Constitution I and II: no network, and nothing leaves the device through backups. */
@RunWith(AndroidJUnit4::class)
class ManifestPrivacyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun requestsNoInternetPermission() {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
        val requested = info.requestedPermissions.orEmpty().toList()
        assertFalse(
            "INTERNET must not be requested: $requested",
            Manifest.permission.INTERNET in requested
        )
    }

    @Test
    fun backupIsDisabled() {
        val flags = context.applicationInfo.flags
        assertEquals(0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
