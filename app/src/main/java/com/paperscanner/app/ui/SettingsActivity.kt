package com.paperscanner.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.paperscanner.app.R
import com.paperscanner.app.util.AutoModeLevel
import com.paperscanner.app.util.EnhanceParams

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchAutoMode: Switch
    private lateinit var layoutAutoLevel: View
    private lateinit var layoutManualParams: View
    private lateinit var btnLevelWeak: Button
    private lateinit var btnLevelNormal: Button
    private lateinit var btnLevelStrong: Button

    private lateinit var sbBlurSize: SeekBar
    private lateinit var sbSharpenCenter: SeekBar
    private lateinit var sbSharpenSurround: SeekBar
    private lateinit var sbThresholdBlock: SeekBar
    private lateinit var sbThresholdConstant: SeekBar

    private lateinit var tvBlurValue: TextView
    private lateinit var tvSharpenCenterValue: TextView
    private lateinit var tvSharpenSurroundValue: TextView
    private lateinit var tvThresholdBlockValue: TextView
    private lateinit var tvThresholdConstantValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            initViews()
            setupListeners()
            loadCurrentParams()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        switchAutoMode = findViewById(R.id.switchAutoMode)
        layoutAutoLevel = findViewById(R.id.layoutAutoLevel)
        layoutManualParams = findViewById(R.id.layoutManualParams)
        btnLevelWeak = findViewById(R.id.btnLevelWeak)
        btnLevelNormal = findViewById(R.id.btnLevelNormal)
        btnLevelStrong = findViewById(R.id.btnLevelStrong)

        sbBlurSize = findViewById(R.id.sbBlurSize)
        sbSharpenCenter = findViewById(R.id.sbSharpenCenter)
        sbSharpenSurround = findViewById(R.id.sbSharpenSurround)
        sbThresholdBlock = findViewById(R.id.sbThresholdBlock)
        sbThresholdConstant = findViewById(R.id.sbThresholdConstant)

        tvBlurValue = findViewById(R.id.tvBlurValue)
        tvSharpenCenterValue = findViewById(R.id.tvSharpenCenterValue)
        tvSharpenSurroundValue = findViewById(R.id.tvSharpenSurroundValue)
        tvThresholdBlockValue = findViewById(R.id.tvThresholdBlockValue)
        tvThresholdConstantValue = findViewById(R.id.tvThresholdConstantValue)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnReset).setOnClickListener {
            resetParams()
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            saveParams()
        }
    }

    private fun setupListeners() {
        switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
            updateUIForAutoMode(isChecked)
        }

        btnLevelWeak.setOnClickListener {
            selectLevel(AutoModeLevel.WEAK)
        }

        btnLevelNormal.setOnClickListener {
            selectLevel(AutoModeLevel.NORMAL)
        }

        btnLevelStrong.setOnClickListener {
            selectLevel(AutoModeLevel.STRONG)
        }

        sbBlurSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBlurValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbSharpenCenter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10.0f
                tvSharpenCenterValue.text = String.format("%.1f", value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbSharpenSurround.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = -progress / 10.0f
                tvSharpenSurroundValue.text = String.format("%.1f", value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbThresholdBlock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvThresholdBlockValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbThresholdConstant.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvThresholdConstantValue.text = String.format("%.1f", progress.toDouble())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateUIForAutoMode(isAutoMode: Boolean) {
        layoutAutoLevel.visibility = if (isAutoMode) View.VISIBLE else View.GONE
        layoutManualParams.visibility = if (isAutoMode) View.GONE else View.VISIBLE
    }

    private fun selectLevel(level: AutoModeLevel) {
        btnLevelWeak.setBackgroundResource(R.drawable.btn_gray_bg)
        btnLevelWeak.setTextColor(resources.getColor(R.color.black))
        btnLevelNormal.setBackgroundResource(R.drawable.btn_gray_bg)
        btnLevelNormal.setTextColor(resources.getColor(R.color.black))
        btnLevelStrong.setBackgroundResource(R.drawable.btn_gray_bg)
        btnLevelStrong.setTextColor(resources.getColor(R.color.black))

        when (level) {
            AutoModeLevel.WEAK -> {
                btnLevelWeak.setBackgroundColor(resources.getColor(R.color.primary))
                btnLevelWeak.setTextColor(resources.getColor(R.color.white))
            }
            AutoModeLevel.NORMAL -> {
                btnLevelNormal.setBackgroundColor(resources.getColor(R.color.primary))
                btnLevelNormal.setTextColor(resources.getColor(R.color.white))
            }
            AutoModeLevel.STRONG -> {
                btnLevelStrong.setBackgroundColor(resources.getColor(R.color.primary))
                btnLevelStrong.setTextColor(resources.getColor(R.color.white))
            }
        }

        EnhanceParams.autoModeLevel = level
    }

    private fun loadCurrentParams() {
        switchAutoMode.isChecked = EnhanceParams.autoMode
        updateUIForAutoMode(EnhanceParams.autoMode)

        selectLevel(EnhanceParams.autoModeLevel)

        sbBlurSize.progress = EnhanceParams.blurSize
        tvBlurValue.text = EnhanceParams.blurSize.toString()

        sbSharpenCenter.progress = (EnhanceParams.sharpenCenter * 10).toInt()
        tvSharpenCenterValue.text = String.format("%.1f", EnhanceParams.sharpenCenter)

        sbSharpenSurround.progress = (-EnhanceParams.sharpenSurround * 10).toInt()
        tvSharpenSurroundValue.text = String.format("%.1f", EnhanceParams.sharpenSurround)

        sbThresholdBlock.progress = EnhanceParams.thresholdBlockSize
        tvThresholdBlockValue.text = EnhanceParams.thresholdBlockSize.toString()

        sbThresholdConstant.progress = EnhanceParams.thresholdConstant.toInt()
        tvThresholdConstantValue.text = String.format("%.1f", EnhanceParams.thresholdConstant)
    }

    private fun resetParams() {
        EnhanceParams.resetToDefault()
        loadCurrentParams()
        Toast.makeText(this, "已恢复默认参数", Toast.LENGTH_SHORT).show()
    }

    private fun saveParams() {
        EnhanceParams.autoMode = switchAutoMode.isChecked

        if (!EnhanceParams.autoMode) {
            EnhanceParams.blurSize = sbBlurSize.progress
            EnhanceParams.sharpenCenter = (sbSharpenCenter.progress / 10.0f)
            EnhanceParams.sharpenSurround = -(sbSharpenSurround.progress / 10.0f)
            EnhanceParams.thresholdBlockSize = sbThresholdBlock.progress
            EnhanceParams.thresholdConstant = sbThresholdConstant.progress.toDouble()
        }

        Toast.makeText(this, "参数已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}