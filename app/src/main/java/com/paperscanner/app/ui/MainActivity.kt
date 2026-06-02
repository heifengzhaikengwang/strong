package com.paperscanner.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperscanner.app.databinding.ActivityMainBinding
import com.paperscanner.app.ui.adapter.ScanHistoryAdapter
import com.paperscanner.app.viewmodel.ScanViewModel
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ScanViewModel by viewModels()
    private lateinit var historyAdapter: ScanHistoryAdapter

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openScanner()
        } else {
            Toast.makeText(this, "需要相机权限才能扫描", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupHistoryRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun setupUI() {
        binding.btnStartScan.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        binding.ivSettings.setOnClickListener {
            openSettings()
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = ScanHistoryAdapter { file ->
            Toast.makeText(this, "点击: ${file.name}", Toast.LENGTH_SHORT).show()
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.capturedImages.observe(this) { images ->
            updateHistoryVisibility(images.size > 0)
        }
    }

    private fun refreshHistory() {
        val files = getScannedFiles()
        historyAdapter.submitList(files)
        updateHistoryVisibility(files.isNotEmpty())
    }

    private fun getScannedFiles(): List<File> {
        val directory = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?.let { java.io.File(it, "PaperScanner") }
        return directory?.listFiles { file ->
            file.isFile && file.name.startsWith("SCAN_") && file.name.endsWith(".jpg")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun updateHistoryVisibility(hasHistory: Boolean) {
        binding.tvHistoryTitle.visibility = if (hasHistory) View.VISIBLE else View.GONE
        binding.rvHistory.visibility = if (hasHistory) View.VISIBLE else View.GONE
        binding.tvNoHistory.visibility = if (hasHistory) View.GONE else View.VISIBLE
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openScanner()
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openScanner() {
        viewModel.clearAll()
        val intent = Intent(this, ScannerActivity::class.java)
        startActivity(intent)
    }
}
