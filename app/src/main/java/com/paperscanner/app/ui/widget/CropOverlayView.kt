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
import kotlin.math.hypot

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val A4_RATIO = 210f / 297f
        private const val CORNER_RADIUS = 24f
        private const val BORDER_WIDTH = 3f
        private const val TOUCH_TOLERANCE = 48f
        private const val MIN_SIZE_RATIO = 0.2f
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = BORDER_WIDTH
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    private var corners = mutableListOf<PointF>()
    private var activeCornerIndex = -1

    private val path = Path()
    private var cropRect = RectF()

    var onCornersChanged: ((List<PointF>) -> Unit)? = null

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (corners.isEmpty()) {
            initDefaultCorners()
        }
    }

    private fun initDefaultCorners() {
        corners.clear()

        val paddingH = width * 0.25f
        val paddingV = height * 0.3f

        val availableWidth = width - 2 * paddingH
        val availableHeight = availableWidth / A4_RATIO

        val left = paddingH
        val top = (height - availableHeight) / 2

        corners.add(PointF(left, top))
        corners.add(PointF(left + availableWidth, top))
        corners.add(PointF(left + availableWidth, top + availableHeight))
        corners.add(PointF(left, top + availableHeight))

        updateCropRect()
        onCornersChanged?.invoke(corners.toList())
    }

    private fun updateCropRect() {
        if (corners.size != 4) return

        val xs = corners.map { it.x }
        val ys = corners.map { it.y }

        cropRect.set(
            xs.minOrNull() ?: 0f,
            ys.minOrNull() ?: 0f,
            xs.maxOrNull() ?: 0f,
            ys.maxOrNull() ?: 0f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (corners.size != 4) return

        path.reset()
        path.moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) {
            path.lineTo(corners[i].x, corners[i].y)
        }
        path.close()

        val overlayPath = Path()
        overlayPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        overlayPath.op(path, Path.Op.DIFFERENCE)

        canvas.drawPath(overlayPath, overlayPaint)

        canvas.drawPath(path, borderPaint)

        drawGrid(canvas)

        for (i in corners.indices) {
            val corner = corners[i]
            val isActive = i == activeCornerIndex
            val radius = if (isActive) CORNER_RADIUS * 1.2f else CORNER_RADIUS
            cornerPaint.color = if (isActive) Color.parseColor("#FF7043") else Color.parseColor("#FF5722")
            canvas.drawCircle(corner.x, corner.y, radius, cornerPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        if (corners.size != 4) return

        val top = corners[0]
        val bottom = corners[3]

        for (i in 1..3) {
            val ratio = i / 4f
            val leftX = top.x + (corners[1].x - top.x) * ratio
            val leftY = top.y + (corners[1].y - top.y) * ratio
            val rightX = bottom.x + (corners[2].x - bottom.x) * ratio
            val rightY = bottom.y + (corners[2].y - bottom.y) * ratio

            canvas.drawLine(leftX, leftY, rightX, rightY, gridPaint)
        }

        for (i in 1..3) {
            val ratio = i / 4f
            val topX = top.x + (bottom.x - top.x) * ratio
            val topY = top.y + (bottom.y - top.y) * ratio
            val bottomX = corners[1].x + (corners[2].x - corners[1].x) * ratio
            val bottomY = corners[1].y + (corners[2].y - corners[1].y) * ratio

            canvas.drawLine(topX, topY, bottomX, bottomY, gridPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeCornerIndex = findNearestCorner(event.x, event.y)
                if (activeCornerIndex >= 0) {
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeCornerIndex >= 0) {
                    val newX = event.x.coerceIn(0f, width.toFloat())
                    val newY = event.y.coerceIn(0f, height.toFloat())

                    adjustAdjacentCorners(activeCornerIndex, newX, newY)

                    corners[activeCornerIndex].x = newX
                    corners[activeCornerIndex].y = newY

                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeCornerIndex >= 0) {
                    activeCornerIndex = -1
                    updateCropRect()
                    onCornersChanged?.invoke(corners.toList())
                    invalidate()
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun findNearestCorner(x: Float, y: Float): Int {
        var nearestIndex = -1
        var nearestDist = TOUCH_TOLERANCE

        for (i in corners.indices) {
            val dist = hypot(x - corners[i].x, y - corners[i].y)
            if (dist < nearestDist) {
                nearestDist = dist
                nearestIndex = i
            }
        }

        return nearestIndex
    }

    private fun adjustAdjacentCorners(index: Int, newX: Float, newY: Float) {
        val oppositeIndex = (index + 2) % 4

        val opposite = corners[oppositeIndex]
        val minWidth = width * MIN_SIZE_RATIO
        val minHeight = height * MIN_SIZE_RATIO

        when (index) {
            0, 1 -> {
                val bottomY = corners[(index + 2) % 4].y
                val newHeight = bottomY - newY
                if (newHeight < minHeight) {
                    corners[index].y = bottomY - minHeight
                } else {
                    corners[index].y = newY
                }
            }

            2, 3 -> {
                val topY = corners[(index + 2) % 4].y
                val newHeight = newY - topY
                if (newHeight < minHeight) {
                    corners[index].y = topY + minHeight
                } else {
                    corners[index].y = newY
                }
            }
        }

        when (index) {
            0, 3 -> {
                val rightX = corners[(index + 1) % 4].x
                val newWidth = rightX - newX
                if (newWidth < minWidth) {
                    corners[index].x = rightX - minWidth
                } else {
                    corners[index].x = newX
                }
            }

            1, 2 -> {
                val leftX = corners[(index + 3) % 4].x
                val newWidth = newX - leftX
                if (newWidth < minWidth) {
                    corners[index].x = leftX + minWidth
                } else {
                    corners[index].x = newX
                }
            }
        }
    }

    fun getCropPoints(): List<PointF> {
        return corners.toList()
    }

    fun getCropRect(): RectF {
        return RectF(cropRect)
    }

    fun resetToDefault() {
        initDefaultCorners()
        invalidate()
    }

    fun setCropRect(rect: RectF) {
        corners.clear()
        corners.add(PointF(rect.left, rect.top))
        corners.add(PointF(rect.right, rect.top))
        corners.add(PointF(rect.right, rect.bottom))
        corners.add(PointF(rect.left, rect.bottom))
        updateCropRect()
        invalidate()
    }
}
