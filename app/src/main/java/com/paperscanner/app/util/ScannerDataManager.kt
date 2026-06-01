package com.paperscanner.app.util

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicReference

object ScannerDataManager {
    private val capturedImagesRef = AtomicReference<MutableList<Bitmap>>(mutableListOf())

    fun getImages(): MutableList<Bitmap> {
        return capturedImagesRef.get()
    }

    fun addImage(bitmap: Bitmap) {
        capturedImagesRef.get().add(bitmap)
    }

    fun removeImage(index: Int) {
        val images = capturedImagesRef.get()
        if (index in images.indices) {
            images.removeAt(index)
        }
    }

    fun clearAll() {
        val images = capturedImagesRef.get()
        images.forEach { if (!it.isRecycled) it.recycle() }
        capturedImagesRef.set(mutableListOf())
    }

    fun getCount(): Int {
        return capturedImagesRef.get().size
    }
}
