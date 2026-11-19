package com.aliraza.dailytodos

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The shipped manifest opts in to predictive back, so the previous page can peek during a back swipe. */
class PredictiveBackManifestTest {

    @Test
    fun theApplicationOptsInToPredictiveBack() {
        val path = checkNotNull(System.getProperty("tripwire.mergedManifest")) {
            "The build must pass the merged manifest path; run this test through Gradle."
        }
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(path))
        val application = document.getElementsByTagName("application").item(0) as Element
        assertEquals("true", application.getAttributeNS(ANDROID_NS, "enableOnBackInvokedCallback"))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
