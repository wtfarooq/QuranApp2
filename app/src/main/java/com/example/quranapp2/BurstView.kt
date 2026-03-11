package com.example.quranapp2

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat

class BurstView(context: Context) : View(context) {

    private val lineCount = 8
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val isNightMode =
            context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        color = ContextCompat.getColor(
            context,
            if (isNightMode) R.color.iconTint else R.color.colorAccent
        )
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private var progress = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val maxRadius = 50f

    fun startAt(cx: Float, cy: Float) {
        centerX = cx
        centerY = cy

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = AccelerateInterpolator(0.8f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (parent as? android.view.ViewGroup)?.removeView(this@BurstView)
                }
            })
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.alpha = ((1f - progress) * 255).toInt()

        val iconEdge = 36f
        val innerRadius = iconEdge + maxRadius * 0.2f * progress
        val lineLength = maxRadius * 0.18f * (1f - progress)

        for (i in 0 until lineCount) {
            val angle = Math.toRadians((i * 360.0 / lineCount) - 90.0)
            val cos = Math.cos(angle).toFloat()
            val sin = Math.sin(angle).toFloat()

            val startX = centerX + cos * innerRadius
            val startY = centerY + sin * innerRadius
            val endX = centerX + cos * (innerRadius + lineLength)
            val endY = centerY + sin * (innerRadius + lineLength)

            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
}
