package com.example.engine
import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
/** Offline classical enhancement: gentle noise smoothing, unsharp mask, local contrast. Not neural AI. */
object LocalEnhancementEngine {
 fun enhance(source: Bitmap, sharpen: Float=0.6f, denoise: Float=0.2f, localContrast: Float=0.25f): Bitmap {
  require(sharpen in 0f..1f && denoise in 0f..1f && localContrast in 0f..1f)
  val w=source.width; val h=source.height; val p=IntArray(w*h); source.getPixels(p,0,w,0,0,w,h)
  val blur=boxBlur(p,w,h); val out=IntArray(p.size); val sharp=sharpen*1.35f; val contrast=localContrast*0.45f
  for(i in p.indices) {
   val c=p[i]; val b=blur[i]; val a=c ushr 24 and 255
   val r=c ushr 16 and 255; val g=c ushr 8 and 255; val blue=c and 255
   val br=b ushr 16 and 255; val bg=b ushr 8 and 255; val bb=b and 255
   val edge=max(abs(r-br),max(abs(g-bg),abs(blue-bb)))
   val smooth=(denoise*(1f-edge/72f).coerceIn(0f,1f)).coerceAtMost(0.35f)
   fun ch(v:Int,bv:Int):Int { var n=v+(v-bv)*sharp; n=n*(1f+contrast); n=n*(1f-smooth)+bv*smooth; return n.toInt().coerceIn(0,255) }
   out[i]=(a shl 24) or (ch(r,br) shl 16) or (ch(g,bg) shl 8) or ch(blue,bb)
  }
  return Bitmap.createBitmap(out,w,h,Bitmap.Config.ARGB_8888)
 }
 fun upscale2x(source:Bitmap):Bitmap {
  val w=source.width*2; val h=source.height*2
  require(w>0 && h>0 && w.toLong()*h<=40_000_000L) { "2× upscale exceeds safe pixel limit." }
  return Bitmap.createScaledBitmap(source,w,h,true)
 }
 private fun boxBlur(p:IntArray,w:Int,h:Int):IntArray {
  val o=IntArray(p.size)
  for(y in 0 until h) for(x in 0 until w) {
   var a=0;var r=0;var g=0;var b=0;var n=0
   for(dy in -1..1) for(dx in -1..1) { val c=p[(y+dy).coerceIn(0,h-1)*w+(x+dx).coerceIn(0,w-1); a+=c ushr 24 and 255;r+=c ushr 16 and 255;g+=c ushr 8 and 255;b+=c and 255;n++ }
   o[y*w+x]=(a/n shl 24) or (r/n shl 16) or (g/n shl 8) or b/n
  }; return o
 }
}