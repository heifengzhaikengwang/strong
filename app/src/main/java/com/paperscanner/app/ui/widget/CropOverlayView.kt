package com.paperscanner.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.paperscanner.app.R

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corners = mutableListOf<PointF>()
    private val cornerRadius = 24f
    private val cornerTouchRadius = 48f
    private var draggedCornerIndex = -1
    private val minSizeRatio = 0.2f

    var onCornersChanged: ((List<PointF>) -> Unit)? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE

        initDefaultCorners()
    }

    private fun initDefaultCorners() {
        post {
            val width = width.toFloat()
            val height = height.toFloat()
            
            if (width > 0 && height > 0) {
                val horizontalPadding = width * 0.25f
                val verticalPadding = height * 0.3f
                
                corners.clear()
                corners.add(PointF(horizontalPadding, verticalPadding))
                corners.add(PointF(width - horizontalPadding, verticalPadding))
                corners.add(PointF(width - horizontalPadding, height - verticalPadding))
                corners.add(PointF(horizontalPadding, height - verticalPadding))
                
                invalidate()
            }
        }
    }

    fun getCropPoints(): List<PointF> {
        return corners.toList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (corners.size < 4) return

        val overlayPath = Path()
        overlayPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        
        val cropPath = Path()
        cropPath.moveTo(corners[0].x, corners[0].y)
        cropPath.lineTo(corners[1].x, corners[1].y)
        cropPath.lineTo(corners[2].x, corners[2].y)
        cropPath.lineTo(corners[3].x, corners[3].y)
        cropPath.close()
        
        overlayPath.op(cropPath, Path.Op.DIFFERENCE)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#80000000")
        canvas.drawPath(overlayPath, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 3f
        canvas.drawPath(cropPath, paint)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF5722")
        for (corner in corners) {
            canvas.drawCircle(corner.x, corner.y, cornerRadius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                draggedCornerIndex = findTouchedCorner(event.x, event.y)
                return draggedCornerIndex != -1
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (draggedCornerIndex != -1) {
                    updateCorner(draggedCornerIndex, event.x, event.y)
                    invalidate()
                    onCornersChanged?.invoke(getCropPoints())
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedCornerIndex = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedCorner(x: Float, y: Float): Int {
        for (i in corners.indices) {
            val dx = corners[i].x - x
            val dy = corners[i].y - y
            if (dx * dx + dy * dy <= cornerTouchRadius * cornerTouchRadius) {
                return i
            }
        }
        return -1
    }

    private fun updateCorner(index: Int, newX: Float, newY: Float) {
        val minX = width * minSizeRatio
        val minY = height * minSizeRatio
        val maxX = width - minX
        val maxY = height - minY
        
        val clampedX = newX.coerceIn(minX, maxX)
        val clampedY = newY.coerceIn(minY, maxY)
        
        when (index) {
            0 -> {
                corners[0].x = clampedX
                corners[0].y = clampedY
                corners[1].y = clampedY
                corners[3].x = clampedX
            }
            1 -> {
                corners[1].x = clampedX
                corners[1].y = clampedY
                corners[0].y = clampedY
                corners[2].x = clampedX
            }
            2 -> {
                corners[2].x = clampedX
                corners[2].y = clampedY
                corners[1].x = clampedX
                corners[3].y = clampedY
            }
            3 -> {
                corners[3].x = clampedX
                corners[3].y = clampedY
                corners[0].x = clampedX
                corners[2].y = clampedY
            }
        }
        
        ensureValidCorners()
    }

    private fun ensureValidCorners() {
        val minX = width * minSizeRatio
        val minY = height * minSizeRatio
        val maxX = width - minX
        val maxY = height - minY
        
        val left = corners[0].x.coerceAtLeast(minX)
        val top = corners[0].y.coerceAtLeast(minY)
        val right = corners[2].x.coerceAtMost(maxX)
        val bottom = corners[2].y.coerceAtMost(maxY)
        
        if (right - left < width * 0.1f || bottom - top < height * 0.1f) {
            return
        }
        
        corners[0].set(left, top)
        corners[1].set(right, top)
        corners[2].set(right, bottom)
        corners[3].set(left, bottom)
    }
}
