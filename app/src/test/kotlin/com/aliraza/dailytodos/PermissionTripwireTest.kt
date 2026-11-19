package com.aliraza.dailytodos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the merged manifest that ships in the APK, so a library that adds a permission fails the build.
 * The allowed set is PLAN.md section 5; changing it needs a deliberate edit here.
 */
class PermissionTripwireTest {

    /** Permission name to its `maxSdkVersion`, or null when it applies on every version. */
    private val allowed = mapOf(
        "android.permission.POST_NOTIFICATIONS" to null,
        "android.permission.USE_EXACT_ALARM" to null,
        "android.permission.SCHEDULE_EXACT_ALARM" to "32",
        "android.permission.RECEIVE_BOOT_COMPLETED" to null,
        "android.permission.VIBRATE" to null,
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to null,
        // androidx.core declares and uses this signature permission so that receivers it registers at
        // runtime are not exported. It is private to this app and grants nothing to anyone else.
        "com.aliraza.dailytodos.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to null,
    )

    private val requested: Map<String, String?> by lazy { readRequestedPermissions() }

    @Test
    fun mergedManifestRequestsExactlyTheAllowedPermissions() {
        assertEquals(allowed, requested)
    }

    @Test
    fun mergedManifestNeverRequestsInternet() {
        assertFalse("android.permission.INTERNET" in requested)
    }

    private fun readRequestedPermissions(): Map<String, String?> {
        val path = checkNotNull(System.getProperty("tripwire.mergedManifest")) {
            "The build must pass the merged manifest path; run this test through Gradle."
        }
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(path))
        return PERMISSION_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { index -> nodes.item(index) as Element }
        }.associate { element ->
            val maxSdk = element.getAttributeNS(ANDROID_NS, "maxSdkVersion").ifEmpty { null }
            element.getAttributeNS(ANDROID_NS, "name") to maxSdk
        }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")
    }
}
