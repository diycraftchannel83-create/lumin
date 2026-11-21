package com.example.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealEsrganModelCatalogTest {
    @Test
    fun selectedModelsHaveExpectedScaleAndSecureSources() {
        val photo = RealEsrganModelCatalog.find("realesrgan-x4plus")
        val anime = RealEsrganModelCatalog.find("realesrgan-x4plus-anime")
        val fast = RealEsrganModelCatalog.find("realesr-animevideov3-x2")

        assertNotNull(photo)
        assertNotNull(anime)
        assertNotNull(fast)
        assertEquals(4, photo!!.scale)
        assertEquals(4, anime!!.scale)
        assertEquals(2, fast!!.scale)
        listOf(photo, anime, fast).forEach { model ->
            assertTrue(model.paramUrl.startsWith("https://"))
            assertTrue(model.binUrl.startsWith("https://"))
            assertTrue(model.paramFileName.endsWith(".param"))
            assertTrue(model.binFileName.endsWith(".bin"))
        }
    }

    @Test
    fun catalogReportsApproximateTotalDownloadSize() {
        assertEquals(43.5f, RealEsrganModelCatalog.totalSelectedModelSizeMb, 0.2f)
    }
}
