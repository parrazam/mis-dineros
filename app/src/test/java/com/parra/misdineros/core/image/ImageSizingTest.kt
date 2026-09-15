package com.parra.misdineros.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun `inSampleSize es la mayor potencia de 2 que mantiene ambos lados sobre el objetivo`() {
        assertEquals(1, ImageSizing.inSampleSize(500, 500, 512))
        assertEquals(1, ImageSizing.inSampleSize(1000, 600, 512))   // 600/2 < 512
        assertEquals(2, ImageSizing.inSampleSize(1024, 1024, 512))
        assertEquals(4, ImageSizing.inSampleSize(4000, 3000, 512))   // 4000/8=500 < 512
        assertEquals(8, ImageSizing.inSampleSize(4032, 4096, 500))
    }

    @Test
    fun `inSampleSize con dimensiones invalidas es 1`() {
        assertEquals(1, ImageSizing.inSampleSize(0, 100, 512))
        assertEquals(1, ImageSizing.inSampleSize(100, -1, 512))
        assertEquals(1, ImageSizing.inSampleSize(100, 100, 0))
    }

    @Test
    fun `fitWithin reduce solo si el lado mayor supera el maximo`() {
        assertEquals(300 to 200, ImageSizing.fitWithin(300, 200, 512))
        assertEquals(512 to 384, ImageSizing.fitWithin(4000, 3000, 512))
        assertEquals(256 to 512, ImageSizing.fitWithin(1000, 2000, 512))
        assertEquals(512 to 1, ImageSizing.fitWithin(100000, 10, 512))
    }
}
