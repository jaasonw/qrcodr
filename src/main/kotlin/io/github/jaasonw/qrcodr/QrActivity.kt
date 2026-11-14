package io.github.jaasonw.qrcodr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val shared = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
    if (shared.isNullOrBlank()) return finish()

    val link = stripTrackers(extractLink(shared))
    val sizePx = (minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels) * 0.75).toInt()
    val pad = (16 * resources.displayMetrics.density).toInt()

    window.attributes = window.attributes.apply { screenBrightness = 1f }
    setFinishOnTouchOutside(true)
    setContentView(
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          gravity = Gravity.CENTER_HORIZONTAL
          setPadding(pad, pad, pad, pad)
          addView(
              ImageView(this@QrActivity).apply {
                setImageBitmap(qrBitmap(link, sizePx))
                contentDescription = "QR code for $link"
              },
              LinearLayout.LayoutParams(sizePx, sizePx),
          )
          addView(
              TextView(this@QrActivity).apply {
                text = link
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextIsSelectable(true)
                setPadding(0, pad, 0, 0)
              },
              LinearLayout.LayoutParams(sizePx, LinearLayout.LayoutParams.WRAP_CONTENT),
          )
        }
    )
  }

  private fun qrBitmap(text: String, sizePx: Int): Bitmap {
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    val n = matrix.width
    val pixels = IntArray(n * n) { if (matrix[it % n, it / n]) Color.BLACK else Color.WHITE }
    val modules = Bitmap.createBitmap(pixels, n, n, Bitmap.Config.RGB_565)
    // Integer scale with no filtering keeps module edges sharp.
    return Bitmap.createScaledBitmap(modules, sizePx / n * n, sizePx / n * n, false)
  }
}
