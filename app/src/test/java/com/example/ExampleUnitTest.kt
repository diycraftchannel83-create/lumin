package com.example

import com.example.engine.SplineCurveLut
import com.example.model.CurvePoint
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun splineCurveLut_linearRemainsIdentity() {
    val lut = SplineCurveLut.buildLut(listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
    assertEquals(256, lut.size)
    assertEquals(0, lut[0])
    assertEquals(128, lut[128])
    assertEquals(255, lut[255])
  }

  @Test
  fun splineCurveLut_sCurveShapesMidtonesMonotonically() {
    val lut = SplineCurveLut.buildLut(
      listOf(
        CurvePoint(0f, 0f),
        CurvePoint(0.25f, 0.15f),
        CurvePoint(0.5f, 0.5f),
        CurvePoint(0.75f, 0.85f),
        CurvePoint(1f, 1f)
      )
    )
    assertTrue(lut[64] < 64)
    assertEquals(128, lut[128])
    assertTrue(lut[192] > 192)
    for (i in 1 until 256) {
      assertTrue("LUT must remain monotonic at $i", lut[i] >= lut[i - 1])
    }
  }
}
