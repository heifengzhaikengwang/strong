package com.paperscanner.app.util

object EnhanceParams {
    var blurSize: Int = 5
    var sharpenCenter: Float = 3.0f
    var sharpenSurround: Float = -0.5f
    var thresholdBlockSize: Int = 15
    var thresholdConstant: Double = 8.0

    fun resetToDefault() {
        blurSize = 5
        sharpenCenter = 3.0f
        sharpenSurround = -0.5f
        thresholdBlockSize = 15
        thresholdConstant = 8.0
    }

    fun toArray(): FloatArray {
        return floatArrayOf(
            blurSize.toFloat(),
            sharpenCenter,
            sharpenSurround,
            thresholdBlockSize.toFloat(),
            thresholdConstant.toFloat()
        )
    }
}