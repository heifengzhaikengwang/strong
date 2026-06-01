package com.paperscanner.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperscanner.app.R
import com.paperscanner.app.databinding.ActivityPreviewBinding
import com.paperscanner.app.ui.adapter.PreviewAdapter
import com.paperscanner.app.util.FileUtils
import com.paperscanner.app.viewmodel.ScanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private val viewModel: ScanViewModel by viewModels()
    private lateinit var previewAdapter: PreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        setupBackHandler()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            showExitConfirmDialog()
        }

        binding.btnDelete.setOnClickListener {
            deleteCurrentImage()
        }

        binding.btnContinue.setOnClickListener {
            openScanner()
        }

        binding.btnShare.setOnClickListener {
            shareImages()
        }

        binding.btnSave.setOnClickListener {
            saveImages()
        }
    }

    private fun setupRecyclerView() {
        previewAdapter = PreviewAdapter { position ->
            previewAdapter.setSelectedPosition(position)
        }

        binding.rvPreview.apply {
            layoutManager = LinearLayoutManager(
                this@PreviewActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = previewAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.capturedImages.observe(this) { images ->
            previewAdapter.submitList(images.toList())
            if (images.isEmpty()) {
                finish()
            }
        }

        viewModel.pageCount.observe(this) { count ->
            previewAdapter.setTotalCount(count)
        }
    }

    private fun deleteCurrentImage() {
        val position = previewAdapter.getSelectedPosition()
        if (position >= 0) {
            AlertDialog.Builder(this)
                .setTitle("删除照片")
                .setMessage("确定要删除第 ${position + 1} 页吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.removeImage(position)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun openScanner() {
        finish()
    }

    private fun shareImages() {
        val images = viewModel.getImages()
        if (images.isEmpty()) {
            Toast.makeText(this, "没有可分享的图片", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uris = mutableListOf<Uri>()

                for ((index, bitmap) in images.withIndex()) {
                    val file = File(cacheDir, "share_$index.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    val uri = FileProvider.getUriForFile(
                        this@PreviewActivity,
                        "${packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }

                withContext(Dispatchers.Main) {
                    showProgress(false)

                    if (uris.size == 1) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uris[0])
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "分享扫描件"))
                    } else {
                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/jpeg"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "分享扫描件"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showProgress(false)
                    Toast.makeText(this@PreviewActivity, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImages() {
        val images = viewModel.getImages()
        if (images.isEmpty()) {
            Toast.makeText(this, "没有可保存的图片", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)

        CoroutineScope(Dispatchers.IO).launch {
            var savedCount = 0

            for (bitmap in images) {
                val uri = FileUtils.saveBitmap(this@PreviewActivity, bitmap)
                if (uri != null) {
                    savedCount++
                }
            }

            withContext(Dispatchers.Main) {
                showProgress(false)
                Toast.makeText(
                    this@PreviewActivity,
                    "已保存 $savedCount 张图片到相册",
                    Toast.LENGTH_LONG
                ).show()

                viewModel.clearAll()
                finish()
            }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出预览")
            .setMessage("确定要退出吗？未保存的内容将会丢失。")
            .setPositiveButton("退出") { _, _ ->
                viewModel.clearAll()
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmDialog()
            }
        })
    }
}
