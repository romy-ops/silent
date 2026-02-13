package com.example.silentguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixBackgroundView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.parseColor("#00FF41") // Matrix Green
        textSize = 40f
        isAntiAlias = true
    }

    private var columnCount = 0
    private var txtPosByColumn = IntArray(0)
    private val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columnCount = w / 40
        txtPosByColumn = IntArray(columnCount) { Random.nextInt(h / 40) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw a semi-transparent black overlay to create the "trail" effect
        canvas.drawColor(Color.argb(50, 0, 0, 0))

        for (i in 0 until columnCount) {
            val char = chars[Random.nextInt(chars.size)]
            val x = (i * 40).toFloat()
            val y = (txtPosByColumn[i] * 40).toFloat()

            canvas.drawText(char.toString(), x, y, paint)

            // Reset column to top if it goes off-screen or randomly
            if (txtPosByColumn[i] * 40 > height && Random.nextFloat() > 0.975) {
                txtPosByColumn[i] = 0
            } else {
                txtPosByColumn[i]++
            }
        }
        // Redraw immediately for smooth animation
        postInvalidateDelayed(50)
    }
}