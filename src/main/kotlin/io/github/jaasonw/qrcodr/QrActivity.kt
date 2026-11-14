package io.github.jaasonw.qrcodr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrActivity : Activity() {
  private lateinit var qr: ImageView
  private lateinit var link: EditText
  private lateinit var params: LinearLayout
  private var sizePx = 0
  private var pad = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val shared = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
    if (shared.isNullOrBlank()) return finish()

    sizePx = (minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels) * 0.75).toInt()
    pad = (16 * resources.displayMetrics.density).toInt()

    window.attributes = window.attributes.apply { screenBrightness = 1f }
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    setFinishOnTouchOutside(true)

    qr = ImageView(this)
    link =
        EditText(this).apply {
          inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
          imeOptions = EditorInfo.IME_ACTION_DONE
          // Single-line input so Enter means Done, but still wrap long URLs visually.
          setHorizontallyScrolling(false)
          maxLines = 4
          setOnEditorActionListener { _, _, _ -> clearFocus().let { true } }
          textSize = 14f
          setText(stripTrackers(extractLink(shared)))
          addTextChangedListener(
              object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) = render()
              })
        }
    params =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          visibility = View.GONE
        }
    link.setOnFocusChangeListener { _, focused ->
      params.visibility = if (focused) View.VISIBLE else View.GONE
      if (!focused) hideKeyboard()
    }

    setContentView(
        ScrollView(this).apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var imeShown = false
            // Only react to the keyboard closing; insets also arrive before it finishes opening.
            setOnApplyWindowInsetsListener { v, insets ->
              val shown = insets.isVisible(WindowInsets.Type.ime())
              if (imeShown && !shown) link.clearFocus()
              imeShown = shown
              v.onApplyWindowInsets(insets)
            }
          }
          addView(
              LinearLayout(this@QrActivity).apply {
                // Focusable root takes focus from the link on clearFocus() and on taps on the QR
                // code or padding.
                isFocusableInTouchMode = true
                setOnClickListener { link.clearFocus() }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(pad, pad, pad, pad)
                addView(qr, LinearLayout.LayoutParams(sizePx, sizePx))
                addView(link, LinearLayout.LayoutParams(sizePx, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad / 2 })
                addView(params, LinearLayout.LayoutParams(sizePx, LinearLayout.LayoutParams.WRAP_CONTENT))
              })
        })
    render()
  }

  private fun render() {
    val text = link.text.toString().trim()
    // Empty or over-capacity text has no QR code; hide the image instead of crashing.
    val bitmap = runCatching { qrBitmap(text, sizePx) }.getOrNull()
    qr.visibility = if (bitmap == null) View.INVISIBLE else View.VISIBLE
    bitmap?.let(qr::setImageBitmap)

    params.removeAllViews()
    val parts = UrlParts.parse(text)
    parts.params.forEachIndexed { i, param -> params.addView(paramRow("✕  $param") { setLink(parts.without(i).toString()) }) }
    if (parts.params.size > 1) params.addView(paramRow("✕  Remove all parameters") { setLink(parts.copy(params = emptyList()).toString()) })
  }

  private fun paramRow(label: String, onTap: () -> Unit) =
      TextView(this).apply {
        text = label
        textSize = 14f
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, pad / 2, 0, pad / 2)
        setOnClickListener { onTap() }
      }

  private fun hideKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.insetsController?.hide(WindowInsets.Type.ime())
    else getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(link.windowToken, 0)
  }

  private fun setLink(url: String) {
    link.setText(url)
    link.setSelection(url.length)
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
