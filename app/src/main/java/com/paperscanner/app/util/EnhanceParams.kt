package com.paperscanner.app.util

enum class AutoModeLevel {
    WEAK,
    NORMAL,
    STRONG
}

object EnhanceParams {
    var autoMode: Boolean = false
    var autoModeLevel: AutoModeLevel = AutoModeLevel.NORMAL

    var blurSize: Int = 5
    var sharpenCenter: Float = 3.0f
    var sharpenSurround: Float = -0.5f
    var thresholdBlockSize: Int = 15
    var thresholdConstant: Double = 8.0

    fun resetToDefault() {
        autoMode = false
        autoModeLevel = AutoModeLevel.NORMAL
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

    fun getAutoParams(brightness: Double, contrast: Double, noiseLevel: Double): FloatArray {
        val factor = when (autoModeLevel) {
            AutoModeLevel.WEAK -> 0.5
            AutoModeLevel.NORMAL -> 1.0
            AutoModeLevel.STRONG -> 1.5
        }

        var blur = 5.0
        var sharpenC = 3.0
        var sharpenS = -0.5
        var block = 15.0
        var constant = 8.0

        if (brightness < 0.3) {
            constant = 5.0 * factor
        } else if (brightness > 0.7) {
            constant = 12.0 * factor
        }

        if (contrast < 0.2) {
            sharpenC = 4.0 * factor
            sharpenS = -0.8 * factor
        } else if (contrast > 0.5) {
            sharpenC = 2.0 * factor
            sharpenS = -0.3 * factor
        }

        if (noiseLevel > 0.1) {
            blur = 7.0 * factor
            block = 21.0 * factor
        }

        return floatArrayOf(
            blur.toFloat(),
            sharpenC.toFloat(),
            sharpenS.toFloat(),
            block.toInt().toFloat(),
            constant.toFloat()
        )
    }
}