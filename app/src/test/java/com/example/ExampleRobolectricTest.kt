package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ImageProcessor
import com.example.model.EditorStateSnapshot
import com.example.model.MaskAdjustments
import com.example.model.MaskLayer
import com.example.model.MaskType
import com.example.model.PhotoAdjustments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PhotoStudio", appName)
    assertEquals(
      "ca-app-pub-1254774552944426/9533896500",
      context.getString(R.string.admob_app_open_unit_id)
    )
    assertEquals(
      "ca-app-pub-1254774552944426/3774492347",
      context.getString(R.string.admob_interstitial_unit_id)
    )
  }

  @Test
  fun `image processor applies exposure and radial mask locally`() {
    val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(100, 100, 100))

    val mask = MaskLayer(
      id = "m1",
      name = "Radial 1",
      type = MaskType.RADIAL,
      centerX = 0.5f,
      centerY = 0.5f,
      radiusX = 0.25f,
      radiusY = 0.25f,
      feather = 0.1f,
      adjustments = MaskAdjustments(exposure = 1.0f)
    )
    val snapshot = EditorStateSnapshot(
      adjustments = PhotoAdjustments(exposure = 0f),
      masks = listOf(mask),
      selectedMaskId = "m1"
    )
    val out = ImageProcessor.renderProcessedBitmap(bmp, snapshot)
    val centerPixel = out.getPixel(20, 20)
    val cornerPixel = out.getPixel(1, 1)

    assertTrue(Color.red(centerPixel) > Color.red(cornerPixel))
    assertEquals(100, Color.red(cornerPixel))
  }

  @Test
  fun `image processor composites shape and text blend layers`() {
    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(40, 40, 40))

    val shapeLayer = com.example.model.BlendLayer(
      id = "b1",
      name = "Shape 1",
      kind = com.example.model.BlendLayerKind.SHAPE,
      shapeType = com.example.model.BlendShapeType.DISC,
      blendMode = com.example.model.LayerBlendMode.NORMAL,
      opacity = 1.0f,
      colorArgb = Color.rgb(255, 180, 0),
      posX = 0.5f,
      posY = 0.5f,
      scale = 1.0f
    )
    val snapshot = EditorStateSnapshot(
      blendLayers = listOf(shapeLayer),
      selectedBlendLayerId = "b1"
    )
    val out = ImageProcessor.renderProcessedBitmap(bmp, snapshot)
    val centerPixel = out.getPixel(60, 60)
    assertTrue(Color.red(centerPixel) > 150)
  }

  @Test
  fun `image processor composites imported gallery shape with white knockout and tint`() {
    val base = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    base.eraseColor(Color.rgb(20, 30, 80))

    // Create a synthetic 60x60 gallery shape bitmap: white background with a black center square
    val shapeBmp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
    shapeBmp.eraseColor(Color.WHITE)
    for (y in 20 until 40) {
      for (x in 20 until 40) {
        shapeBmp.setPixel(x, y, Color.BLACK)
      }
    }

    val galleryLayer = com.example.model.BlendLayer(
      id = "g1",
      name = "Import 1",
      kind = com.example.model.BlendLayerKind.GALLERY_SHAPE,
      blendMode = com.example.model.LayerBlendMode.NORMAL,
      opacity = 1.0f,
      colorArgb = Color.rgb(255, 160, 0),
      posX = 0.5f,
      posY = 0.5f,
      scale = 1.0f,
      galleryAssetId = "asset_1",
      galleryCutout = com.example.model.GalleryShapeCutout.DROP_WHITE,
      tintGalleryShape = true
    )
    val snapshot = EditorStateSnapshot(
      blendLayers = listOf(galleryLayer),
      selectedBlendLayerId = "g1"
    )
    val out = ImageProcessor.renderProcessedBitmap(
      baseSource = base,
      state = snapshot,
      galleryShapeBitmaps = mapOf("asset_1" to shapeBmp)
    )

    // Center pixel should be tinted amber/orange
    val centerPixel = out.getPixel(60, 60)
    assertTrue(Color.red(centerPixel) > 200)
    assertTrue(Color.blue(centerPixel) < 50)

    // Outer region of the 60x60 shape (which was white) should be knocked out, preserving blue base
    val knockedOutCorner = out.getPixel(35, 35)
    assertEquals(20, Color.red(knockedOutCorner))
    assertEquals(80, Color.blue(knockedOutCorner))
  }

  @Test
  fun `exposure color matrix filter brightens and darkens image accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("lumina_premium_remove_ads", context.getString(R.string.play_product_remove_ads))

    val base = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
    base.eraseColor(Color.rgb(80, 100, 120))

    val brighter = ImageProcessor.applyExposureColorFilter(base, 1.0f)
    val darker = ImageProcessor.applyExposureColorFilter(base, -1.0f)

    assertEquals(160, Color.red(brighter.getPixel(10, 10)))
    assertEquals(40, Color.red(darker.getPixel(10, 10)))
  }

  @Test
  fun `custom tone curve control points remap image luminance`() {
    val base = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
    base.eraseColor(Color.rgb(128, 128, 128))

    val liftedMidtones = com.example.model.ToneCurvesState(
      rgbPoints = listOf(
        com.example.model.CurvePoint(0f, 0f),
        com.example.model.CurvePoint(0.5f, 0.78f),
        com.example.model.CurvePoint(1f, 1f)
      )
    )
    val crushedMidtones = com.example.model.ToneCurvesState(
      rgbPoints = listOf(
        com.example.model.CurvePoint(0f, 0f),
        com.example.model.CurvePoint(0.5f, 0.22f),
        com.example.model.CurvePoint(1f, 1f)
      )
    )

    val liftedOut = ImageProcessor.renderProcessedBitmap(
      baseSource = base,
      state = EditorStateSnapshot(curves = liftedMidtones)
    )
    val crushedOut = ImageProcessor.renderProcessedBitmap(
      baseSource = base,
      state = EditorStateSnapshot(curves = crushedMidtones)
    )

    val liftedLuma = Color.red(liftedOut.getPixel(10, 10))
    val crushedLuma = Color.red(crushedOut.getPixel(10, 10))
    assertTrue("Expected lifted midtone > 185, was $liftedLuma", liftedLuma > 185)
    assertTrue("Expected crushed midtone < 75, was $crushedLuma", crushedLuma < 75)
  }
}
