package com.doomscrollpreventer.ui.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.doomscrollpreventer.R
import com.doomscrollpreventer.service.ScrollDetectorService
import com.doomscrollpreventer.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsFragment : Fragment() {

    private lateinit var switchMonitoring: MaterialSwitch
    private lateinit var tvWarningTime: TextView
    private lateinit var tvBlockTime: TextView
    private lateinit var seekWarning: SeekBar
    private lateinit var seekBlock: SeekBar
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var tvOverlayStatus: TextView
    private lateinit var btnOverlay: MaterialButton
    private lateinit var appTogglesContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchMonitoring = view.findViewById(R.id.switchMonitoring)
        tvWarningTime = view.findViewById(R.id.tvWarningTime)
        tvBlockTime = view.findViewById(R.id.tvBlockTime)
        seekWarning = view.findViewById(R.id.seekWarning)
        seekBlock = view.findViewById(R.id.seekBlock)
        tvAccessibilityStatus = view.findViewById(R.id.tvAccessibilityStatus)
        btnAccessibility = view.findViewById(R.id.btnAccessibility)
        tvOverlayStatus = view.findViewById(R.id.tvOverlayStatus)
        btnOverlay = view.findViewById(R.id.btnOverlay)
        appTogglesContainer = view.findViewById(R.id.appTogglesContainer)

        setupMonitoringToggle()
        setupTimeLimits()
        setupPermissionButtons()
        setupAppToggles()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
    }

    private fun setupMonitoringToggle() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        switchMonitoring.isChecked = prefs.getBoolean(Constants.PREF_MONITORING_ENABLED, true)

        switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Constants.PREF_MONITORING_ENABLED, isChecked).apply()
        }
    }

    private fun setupTimeLimits() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        // Warning slider (5-55 minutes, step 5)
        val warningMs = prefs.getLong(Constants.PREF_WARNING_LIMIT, Constants.DEFAULT_WARNING_LIMIT_MS)
        val warningMin = (warningMs / 60000).toInt()
        seekWarning.max = 10 // 5 to 55 in steps of 5 = 11 positions (0..10)
        seekWarning.progress = (warningMin / 5) - 1
        tvWarningTime.text = "${warningMin} min"

        seekWarning.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = (progress + 1) * 5
                tvWarningTime.text = "${minutes} min"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val minutes = ((seekBar?.progress ?: 2) + 1) * 5
                prefs.edit().putLong(Constants.PREF_WARNING_LIMIT, minutes * 60000L).apply()
            }
        })

        // Block slider (10-60 minutes, step 5)
        val blockMs = prefs.getLong(Constants.PREF_BLOCK_LIMIT, Constants.DEFAULT_BLOCK_LIMIT_MS)
        val blockMin = (blockMs / 60000).toInt()
        seekBlock.max = 10 // 10 to 60 in steps of 5 = 11 positions
        seekBlock.progress = (blockMin / 5) - 2
        tvBlockTime.text = "${blockMin} min"

        seekBlock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = (progress + 2) * 5
                tvBlockTime.text = "${minutes} min"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val minutes = ((seekBar?.progress ?: 4) + 2) * 5
                prefs.edit().putLong(Constants.PREF_BLOCK_LIMIT, minutes * 60000L).apply()
            }
        })
    }

    private fun setupPermissionButtons() {
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
        }
    }

    private fun updatePermissionStatuses() {
        // Check Accessibility Service
        val am = requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val isAccessibilityEnabled = enabled.any {
            it.resolveInfo.serviceInfo.name == ScrollDetectorService::class.java.name
        }

        tvAccessibilityStatus.text = if (isAccessibilityEnabled) "Enabled" else "Disabled"
        tvAccessibilityStatus.setTextColor(
            requireContext().getColor(
                if (isAccessibilityEnabled) R.color.status_good else R.color.status_danger
            )
        )

        // Check Overlay Permission
        val canDrawOverlays = Settings.canDrawOverlays(requireContext())
        tvOverlayStatus.text = if (canDrawOverlays) "Enabled" else "Disabled"
        tvOverlayStatus.setTextColor(
            requireContext().getColor(
                if (canDrawOverlays) R.color.status_good else R.color.status_danger
            )
        )
    }

    private fun setupAppToggles() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val enabledApps = prefs.getStringSet(Constants.PREF_ENABLED_APPS, null)
            ?: Constants.TARGET_APPS.keys.toMutableSet()

        appTogglesContainer.removeAllViews()

        Constants.TARGET_APPS.forEach { (packageName, displayName) ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_app_toggle, appTogglesContainer, false)

            itemView.findViewById<TextView>(R.id.tvAppToggleName).text = displayName

            val toggle = itemView.findViewById<MaterialSwitch>(R.id.switchApp)
            toggle.isChecked = packageName in enabledApps

            toggle.setOnCheckedChangeListener { _, isChecked ->
                val currentSet = prefs.getStringSet(Constants.PREF_ENABLED_APPS, null)
                    ?.toMutableSet() ?: Constants.TARGET_APPS.keys.toMutableSet()
                if (isChecked) currentSet.add(packageName) else currentSet.remove(packageName)
                prefs.edit().putStringSet(Constants.PREF_ENABLED_APPS, currentSet).apply()
            }

            appTogglesContainer.addView(itemView)
        }
    }
}
