package com.paperscanner.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.paperscanner.app.R
import com.paperscanner.app.databinding.ActivityGalleryCropBinding
import com.paperscanner.app.scanner.ImageProcessor
import com.paperscanner.app.util.FileUtils
import com.paperscanner.app.util.ScannerDataManager
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.InputStream

class GalleryCropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryCropBinding
    private val dataManager = ScannerDataManager
    private var sourceBitmap: Bitmap? = null

    companion object {
        private const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun start(context: Context, uri: Uri) {
            val intent = Intent(context, GalleryCropActivity::class.java)
            intent.putExtra(EXTRA_IMAGE_URI, uri)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        if (uri == null) {
            finish()
            return
        }

        loadImage(uri)
        setupUI()
        updateUI()
    }

    private fun loadImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            sourceBitmap = BitmapFactory.decodeStream(inputStream)

            sourceBitmap?.let {
                binding.ivSourceImage.setImageBitmap(it)
            } ?: run {
                Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            showExitConfirmDialog()
        }

        binding.btnCrop.setOnClickListener {
            cropAndEnhance()
        }

        binding.btnFinish.setOnClickListener {
            if (dataManager.getCount() > 0) {
                openPreview()
            }
        }
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

    private fun cropAndEnhance() {
        val bitmap = sourceBitmap ?: return

        try {
            val src = Mat()
            Utils.bitmapToMat(bitmap, src)

            val cropPointsF = binding.cropOverlayView.getCropPoints()
            val cropPoints = FileUtils.pointFListToPoint(cropPointsF)
            val cropped = ImageProcessor.cropQuadrilateral(src, cropPoints)
            val enhanced = ImageProcessor.enhanceDocument(cropped)

            val outputBitmap = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(enhanced, outputBitmap)

            src.release()
            cropped.release()
            enhanced.release()

            dataManager.addImage(outputBitmap)
            updateUI()

            Toast.makeText(this, "裁剪成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认退出")
            .setMessage("确定要退出吗？未保存的图片将丢失。")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openPreview() {
        val intent = Intent(this, PreviewActivity::class.java)
        startActivity(intent)
        finish()
    }
}
