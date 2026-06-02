package com.paperscanner.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.paperscanner.app.R
import com.paperscanner.app.databinding.ActivityScannerBinding
import com.paperscanner.app.scanner.ImageProcessor
import com.paperscanner.app.util.ScannerDataManager
import com.paperscanner.app.viewmodel.ScanViewModel
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private val viewModel: ScanViewModel by viewModels()
    private val dataManager = ScannerDataManager

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            GalleryCropActivity.start(this, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        observeViewModel()
        startCamera()
        setupBackHandler()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            showExitConfirmDialog()
        }

        binding.btnSettings.setOnClickListener {
            openSettings()
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnFinish.setOnClickListener {
            if (dataManager.getCount() > 0) {
                openPreview()
            }
        }

        binding.cropOverlayView.onCornersChanged = { corners ->
            Log.d("CropOverlay", "Corners changed: $corners")
        }
        
        updateUI()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun updateUI() {
        val count = dataManager.getCount()
        if (count > 0) {
            binding.tvPageCount.visibility = View.VISIBLE
            binding.tvPageCount.text = count.toString()
            binding.btnFinish.visibility = View.VISIBLE
        } else {
            binding.tvPageCount.visibility = View.GONE
            binding.btnFinish.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewModel.currentProcessing.observe(this) { isProcessing ->
            binding.btnCapture.isEnabled = !isProcessing
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.btnCapture.isEnabled = false

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        binding.btnCapture.isEnabled = true
                        Toast.makeText(
                            this@ScannerActivity,
                            "拍照失败: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun processImage(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image)

        val scaledBitmap = scaleBitmapToPreview(bitmap, image.imageInfo.rotationDegrees)
        bitmap.recycle()

        val cropPoints = binding.cropOverlayView.getCropPoints()
        val previewWidth = binding.previewView.width
        val previewHeight = binding.previewView.height

        val scaledCropPoints = scaleCropPointsToOriginal(
            cropPoints,
            scaledBitmap.width.toFloat(),
            scaledBitmap.height.toFloat(),
            previewWidth.toFloat(),
            previewHeight.toFloat()
        )

        viewModel.addCapturedImage(scaledBitmap, scaledCropPoints) {
            runOnUiThread {
                updateUI()
                binding.btnCapture.isEnabled = true
                Toast.makeText(this, "拍照成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapToPreview(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val previewWidth = binding.previewView.width
        val previewHeight = binding.previewView.height

        if (previewWidth <= 0 || previewHeight <= 0) {
            return bitmap
        }

        val scaleX = previewWidth.toFloat() / bitmap.width
        val scaleY = previewHeight.toFloat() / bitmap.height
        val scale = maxOf(scaleX, scaleY)

        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun scaleCropPointsToOriginal(
        previewPoints: List<PointF>,
        originalWidth: Float,
        originalHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): List<PointF> {
        if (previewWidth <= 0 || previewHeight <= 0) {
            return previewPoints
        }

        val scaleX = originalWidth / previewWidth
        val scaleY = originalHeight / previewHeight

        return previewPoints.map { point ->
            PointF(point.x * scaleX, point.y * scaleY)
        }
    }

    private fun openPreview() {
        val intent = Intent(this, PreviewActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmDialog() {
        if ((viewModel.capturedImages.value?.size ?: 0) > 0) {
            AlertDialog.Builder(this)
                .setTitle("退出扫描")
                .setMessage("您还有未保存的扫描内容，确定要退出吗？")
                .setPositiveButton("退出") { _, _ ->
                    viewModel.clearAll()
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            finish()
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmDialog()
            }
        })
    }

    private fun processGalleryImage(uri: Uri) {
        binding.btnCapture.isEnabled = false

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap != null) {
                val src = Mat()
                Utils.bitmapToMat(bitmap, src)
                
                val enhanced = ImageProcessor.enhanceDocument(src)
                val outputBitmap = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(enhanced, outputBitmap)
                
                src.release()
                enhanced.release()
                
                dataManager.addImage(outputBitmap)
                updateUI()
                binding.btnCapture.isEnabled = true
                Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnCapture.isEnabled = true
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            binding.btnCapture.isEnabled = true
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ScannerActivity"
    }
}
