package com.paperscanner.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.paperscanner.app.databinding.ActivitySettingsBinding
import com.paperscanner.app.util.EnhanceParams

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupUI()
            loadCurrentParams()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打开设置失败", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.sbBlurSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvBlurValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.sbSharpenCenter.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10.0f
                binding.tvSharpenCenterValue.text = String.format("%.1f", value)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.sbSharpenSurround.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = -(progress / 10.0f)
                binding.tvSharpenSurroundValue.text = String.format("%.1f", value)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.sbThresholdBlock.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress % 2 == 0) {
                    seekBar?.progress = progress + 1
                }
                binding.tvThresholdBlockValue.text = seekBar?.progress?.toString()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.sbThresholdConstant.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvThresholdConstantValue.text = String.format("%.0f", progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
            updateAutoModeUI(isChecked)
        }

        binding.btnLevelWeak.setOnClickListener { selectAutoLevel(EnhanceParams.AutoModeLevel.WEAK) }
        binding.btnLevelNormal.setOnClickListener { selectAutoLevel(EnhanceParams.AutoModeLevel.NORMAL) }
        binding.btnLevelStrong.setOnClickListener { selectAutoLevel(EnhanceParams.AutoModeLevel.STRONG) }

        binding.btnReset.setOnClickListener {
            EnhanceParams.resetToDefault()
            loadCurrentParams()
            Toast.makeText(this, "已恢复默认值", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            saveParams()
            finish()
        }
    }

    private fun loadCurrentParams() {
        binding.switchAutoMode.isChecked = EnhanceParams.autoMode
        updateAutoModeUI(EnhanceParams.autoMode)
        selectAutoLevel(EnhanceParams.autoModeLevel)
        
        binding.sbBlurSize.progress = EnhanceParams.blurSize
        binding.sbSharpenCenter.progress = (EnhanceParams.sharpenCenter * 10).toInt()
        binding.sbSharpenSurround.progress = (-EnhanceParams.sharpenSurround * 10).toInt()
        binding.sbThresholdBlock.progress = EnhanceParams.thresholdBlockSize
        binding.sbThresholdConstant.progress = EnhanceParams.thresholdConstant.toInt()
        
        binding.tvBlurValue.text = EnhanceParams.blurSize.toString()
        binding.tvSharpenCenterValue.text = String.format("%.1f", EnhanceParams.sharpenCenter)
        binding.tvSharpenSurroundValue.text = String.format("%.1f", EnhanceParams.sharpenSurround)
        binding.tvThresholdBlockValue.text = EnhanceParams.thresholdBlockSize.toString()
        binding.tvThresholdConstantValue.text = String.format("%.0f", EnhanceParams.thresholdConstant)
    }

    private fun updateAutoModeUI(isAuto: Boolean) {
        binding.layoutAutoLevel.visibility = if (isAuto) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutManualParams.visibility = if (isAuto) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun selectAutoLevel(level: EnhanceParams.AutoModeLevel) {
        EnhanceParams.autoModeLevel = level
        
        binding.btnLevelWeak.apply {
            setTextColor(if (level == EnhanceParams.AutoModeLevel.WEAK) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            setBackgroundResource(if (level == EnhanceParams.AutoModeLevel.WEAK) com.paperscanner.app.R.drawable.bg_circle_semi_transparent else com.paperscanner.app.R.drawable.btn_gray_bg)
        }
        
        binding.btnLevelNormal.apply {
            setTextColor(if (level == EnhanceParams.AutoModeLevel.NORMAL) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            setBackgroundResource(if (level == EnhanceParams.AutoModeLevel.NORMAL) com.paperscanner.app.R.drawable.bg_circle_semi_transparent else com.paperscanner.app.R.drawable.btn_gray_bg)
        }
        
        binding.btnLevelStrong.apply {
            setTextColor(if (level == EnhanceParams.AutoModeLevel.STRONG) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            setBackgroundResource(if (level == EnhanceParams.AutoModeLevel.STRONG) com.paperscanner.app.R.drawable.bg_circle_semi_transparent else com.paperscanner.app.R.drawable.btn_gray_bg)
        }
    }

    private fun saveParams() {
        EnhanceParams.autoMode = binding.switchAutoMode.isChecked
        
        if (!EnhanceParams.autoMode) {
            EnhanceParams.blurSize = binding.sbBlurSize.progress
            EnhanceParams.sharpenCenter = binding.sbSharpenCenter.progress / 10.0f
            EnhanceParams.sharpenSurround = -(binding.sbSharpenSurround.progress / 10.0f)
            EnhanceParams.thresholdBlockSize = binding.sbThresholdBlock.progress
            EnhanceParams.thresholdConstant = binding.sbThresholdConstant.progress.toFloat()
        }
        
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
    }
}
