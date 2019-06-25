package be.appfoundry.sceneform_cobelab

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable

open class PointerDrawable : Drawable() {
    private val paint: Paint = Paint()
    var enabled = false

    override fun setAlpha(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(p0: ColorFilter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(canvas: Canvas) {
        // Draw Circle in green when enabled
        val cx: Float = bounds.width() / 2.0f
        val cy: Float = bounds.height() / 2.0f
        if (enabled) {
            // paint.color = Color.parseColor("#334334")
            paint.color = Color.GREEN
            canvas.drawCircle(cx, cy, 10.0f, paint)
        } else {
            paint.color = Color.GRAY
            canvas.drawText("X", cx, cy, paint)
        }

    }
}