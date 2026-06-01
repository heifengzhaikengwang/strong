package com.paperscanner.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paperscanner.app.scanner.ImageProcessor
import com.paperscanner.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _capturedImages = MutableLiveData<MutableList<Bitmap>>(mutableListOf())
    val capturedImages: LiveData<MutableList<Bitmap>> = _capturedImages

    private val _currentProcessing = MutableLiveData<Boolean>(false)
    val currentProcessing: LiveData<Boolean> = _currentProcessing

    private val _pageCount = MutableLiveData<Int>(0)
    val pageCount: LiveData<Int> = _pageCount

    init {
        System.loadLibrary("opencv_java4")
        if (!OpenCVLoader.initLocal()) {
            OpenCVLoader.initDebug()
        }
    }

    fun addCapturedImage(bitmap: Bitmap, cropPoints: List<PointF>) {
        viewModelScope.launch {
            _currentProcessing.value = true

            val processedBitmap = withContext(Dispatchers.Default) {
                val points = FileUtils.pointFListToPoint(cropPoints)
                ImageProcessor.processForPreview(bitmap, points)
            }

            val currentList = _capturedImages.value ?: mutableListOf()
            currentList.add(processedBitmap)
            _capturedImages.value = currentList
            _pageCount.value = currentList.size

            _currentProcessing.value = false
        }
    }

    fun processImage(bitmap: Bitmap, cropPoints: List<PointF>, targetWidth: Int, targetHeight: Int): Bitmap {
        val points = FileUtils.pointFListToPoint(cropPoints)
        return ImageProcessor.cropAndEnhance(bitmap, points, targetWidth, targetHeight)
    }

    fun removeImage(index: Int) {
        val currentList = _capturedImages.value ?: return
        if (index in currentList.indices) {
            currentList[index].recycle()
            currentList.removeAt(index)
            _capturedImages.value = currentList
            _pageCount.value = currentList.size
        }
    }

    fun clearAll() {
        val currentList = _capturedImages.value ?: return
        currentList.forEach { it.recycle() }
        _capturedImages.value = mutableListOf()
        _pageCount.value = 0
    }

    fun getImages(): List<Bitmap> {
        return _capturedImages.value?.toList() ?: emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        _capturedImages.value?.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
