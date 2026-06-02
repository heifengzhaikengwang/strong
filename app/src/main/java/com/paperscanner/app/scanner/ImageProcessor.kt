package com.paperscanner.app.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

object ImageProcessor {

    fun cropAndEnhance(
        bitmap: Bitmap,
        cropPoints: List<Point>,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val cropped = cropQuadrilateral(src, cropPoints)

        val enhanced = enhanceDocument(cropped)

        val result = if (targetWidth > 0 && targetHeight > 0) {
            val resized = Mat()
            Imgproc.resize(enhanced, resized, Size(targetWidth.toDouble(), targetHeight.toDouble()))
            resized
        } else {
            enhanced
        }

        val outputBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, outputBitmap)

        src.release()
        cropped.release()
        enhanced.release()
        result.release()

        return outputBitmap
    }

    private fun cropQuadrilateral(src: Mat, points: List<Point>): Mat {
        if (points.size != 4) {
            return src.clone()
        }

        val orderedPoints = orderPoints(points)
        val (tl, tr, br, bl) = orderedPoints

        val widthA = sqrt((br.x - bl.x) * (br.x - bl.x) + (br.y - bl.y) * (br.y - bl.y))
        val widthB = sqrt((tr.x - tl.x) * (tr.x - tl.x) + (tr.y - tl.y) * (tr.y - tl.y))
        val maxWidth = maxOf(widthA.toInt(), widthB.toInt())

        val heightA = sqrt((tr.x - br.x) * (tr.x - br.x) + (tr.y - br.y) * (tr.y - br.y))
        val heightB = sqrt((tl.x - bl.x) * (tl.x - bl.x) + (tl.y - bl.y) * (tl.y - bl.y))
        val maxHeight = maxOf(heightA.toInt(), heightB.toInt())

        val srcPoints = MatOfPoint2f(
            Point(tl.x.toDouble(), tl.y.toDouble()),
            Point(tr.x.toDouble(), tr.y.toDouble()),
            Point(br.x.toDouble(), br.y.toDouble()),
            Point(bl.x.toDouble(), bl.y.toDouble())
        )

        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )

        val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        val warped = Mat()
        Imgproc.warpPerspective(src, warped, transformMatrix, Size(maxWidth.toDouble(), maxHeight.toDouble()))

        transformMatrix.release()
        srcPoints.release()
        dstPoints.release()

        return warped
    }

    private fun orderPoints(points: List<Point>): List<Point> {
        val pts = points.map { Point(it.x.toDouble(), it.y.toDouble()) }.toMutableList()

        pts.sortBy { it.x }
        val leftMost = pts.take(2).sortedBy { it.y }
        val rightMost = pts.drop(2).sortedBy { it.y }

        val tl = leftMost[0]
        val bl = leftMost[1]
        val tr = rightMost[0]
        val br = rightMost[1]

        return listOf(tl, tr, br, bl)
    }

    fun enhanceDocument(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        val (blurSize, sharpenCenter, sharpenSurround, blockSize, constant) = 
            if (EnhanceParams.autoMode) {
                val brightness = calculateBrightness(gray)
                val contrast = calculateContrast(gray)
                val noise = calculateNoise(gray)
                val params = EnhanceParams.getAutoParams(brightness, contrast, noise)
                arrayOf(params[0].toDouble(), params[1].toDouble(), params[2].toDouble(), 
                        params[3].toInt(), params[4].toDouble())
            } else {
                arrayOf(EnhanceParams.blurSize.toDouble(), 
                        EnhanceParams.sharpenCenter.toDouble(), 
                        EnhanceParams.sharpenSurround.toDouble(),
                        EnhanceParams.thresholdBlockSize, 
                        EnhanceParams.thresholdConstant)
            }

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(blurSize, blurSize), 0.0)

        val sharpenKernel = Mat(3, 3, org.opencv.core.CvType.CV_32F)
        sharpenKernel.put(0, 0,
            0.0, sharpenSurround, 0.0,
            sharpenSurround, sharpenCenter, sharpenSurround,
            0.0, sharpenSurround, 0.0
        )

        val sharpened = Mat()
        Imgproc.filter2D(blurred, sharpened, -1, sharpenKernel)

        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            sharpened,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            blockSize,
            constant
        )

        sharpenKernel.release()
        gray.release()
        blurred.release()
        sharpened.release()

        return thresh
    }

    private fun calculateBrightness(src: Mat): Double {
        val mean = org.opencv.core.Mat()
        val std = org.opencv.core.Mat()
        Imgproc.meanStdDev(src, mean, std)
        val brightness = mean.get(0, 0)[0] / 255.0
        mean.release()
        std.release()
        return brightness
    }

    private fun calculateContrast(src: Mat): Double {
        val mean = org.opencv.core.Mat()
        val std = org.opencv.core.Mat()
        Imgproc.meanStdDev(src, mean, std)
        val contrast = std.get(0, 0)[0] / 255.0
        mean.release()
        std.release()
        return contrast
    }

    private fun calculateNoise(src: Mat): Double {
        val laplacian = Mat()
        Imgproc.Laplacian(src, laplacian, org.opencv.core.CvType.CV_64F)
        val mean = org.opencv.core.Mat()
        val std = org.opencv.core.Mat()
        Imgproc.meanStdDev(laplacian, mean, std)
        val noise = std.get(0, 0)[0] / 100.0
        laplacian.release()
        mean.release()
        std.release()
        return noise
    }

    fun detectEdges(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(blurred, edges, 50.0, 150.0)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.dilate(edges, edges, kernel)
        Imgproc.erode(edges, edges, kernel)

        gray.release()
        blurred.release()
        kernel.release()

        return edges
    }

    fun findDocumentCorners(edges: Mat, width: Int, height: Int): List<Point> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()

        Imgproc.findContours(edges.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        hierarchy.release()

        if (contours.isEmpty()) {
            return emptyList()
        }

        val maxContour = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return emptyList()

        val contourArea = Imgproc.contourArea(maxContour)
        val imageArea = width * height.toDouble()

        if (contourArea < imageArea * 0.1) {
            return emptyList()
        }

        val epsilon = 0.02 * Imgproc.arcLength(maxContour, true)
        val approx = MatOfPoint2f()
        maxContour.convertTo(approx, org.opencv.core.CvType.CV_32F)
        Imgproc.approxPolyDP(approx, approx, epsilon, true)

        val approxPoints = approx.toArray()
        approx.release()
        maxContour.release()

        if (approxPoints.size == 4) {
            return orderPoints(approxPoints.toList())
        }

        return emptyList()
    }

    fun cropRectangle(bitmap: Bitmap, cropRect: android.graphics.Rect): Bitmap {
        val x = maxOf(0, cropRect.left)
        val y = maxOf(0, cropRect.top)
        val width = minOf(bitmap.width - x, cropRect.width())
        val height = minOf(bitmap.height - y, cropRect.height())

        if (width <= 0 || height <= 0) {
            return bitmap
        }

        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    fun processForPreview(bitmap: Bitmap, cropPoints: List<Point>): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val cropped = cropQuadrilateral(src, cropPoints)
        val enhanced = enhanceDocument(cropped)

        val outputBitmap = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(enhanced, outputBitmap)

        src.release()
        cropped.release()
        enhanced.release()

        return outputBitmap
    }
}
