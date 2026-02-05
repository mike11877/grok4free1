package com.doomscrollpreventer.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.doomscrollpreventer.MainActivity
import com.doomscrollpreventer.R
import com.doomscrollpreventer.service.ScrollDetectorService
import com.doomscrollpreventer.util.Constants
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0

    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepDescription: TextView
    private lateinit var tvStepNumber: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: TextView

    private val steps = listOf(
        OnboardingStep(
            "Welcome",
            "DoomScroll Preventer helps you take control of your scrolling habits.\n\nIt monitors target apps and gently reminds you when you've been scrolling too long — with a warning at 15 minutes and a full block at 30 minutes.",
            null,
            "Next"
        ),
        OnboardingStep(
            "Accessibility Service",
            "We need the Accessibility Service permission to detect when you're scrolling in apps like TikTok, Instagram, Twitter, Reddit, YouTube, and Facebook.\n\nFind \"DoomScroll Preventer\" in the list and enable it.",
            "Open Accessibility Settings",
            "I've Enabled It"
        ),
        OnboardingStep(
            "Overlay Permission",
            "We need permission to display warnings and block screens over other apps when your time limit is reached.",
            "Grant Overlay Permission",
            "I've Granted It"
        ),
        OnboardingStep(
            "You're All Set!",
            "DoomScroll Preventer is now monitoring your scrolling.\n\n• Warning at 15 minutes\n• Full block at 30 minutes\n• View your stats on the dashboard\n• Customize limits in settings\n\nTake back your time!",
            null,
            "Get Started"
        )
    )

    data class OnboardingStep(
        val title: String,
        val description: String,
        val actionButton: String?,
        val nextButton: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        tvStepTitle = findViewById(R.id.tvStepTitle)
        tvStepDescription = findViewById(R.id.tvStepDescription)
        tvStepNumber = findViewById(R.id.tvStepNumber)
        btnAction = findViewById(R.id.btnAction)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        btnNext.setOnClickListener { nextStep() }
        btnSkip.setOnClickListener { completeOnboarding() }

        showStep(0)
    }

    override fun onResume() {
        super.onResume()
        showStep(currentStep)
    }

    private fun showStep(step: Int) {
        currentStep = step
        val s = steps[step]

        tvStepTitle.text = s.title
        tvStepDescription.text = s.description
        tvStepNumber.text = "Step ${step + 1} of ${steps.size}"
        btnNext.text = s.nextButton

        if (s.actionButton != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = s.actionButton
            btnAction.setOnClickListener {
                when (step) {
                    1 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    2 -> {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                }
            }
        } else {
            btnAction.visibility = View.GONE
        }

        btnSkip.visibility = if (step < steps.size - 1) View.VISIBLE else View.GONE
    }

    private fun nextStep() {
        if (currentStep < steps.size - 1) {
            showStep(currentStep + 1)
        } else {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.PREF_ONBOARDING_COMPLETE, true)
            .putBoolean(Constants.PREF_MONITORING_ENABLED, true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
