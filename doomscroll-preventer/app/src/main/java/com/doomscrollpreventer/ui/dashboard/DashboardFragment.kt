package com.doomscrollpreventer.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.doomscrollpreventer.R
import com.doomscrollpreventer.data.AppUsageSummary
import com.doomscrollpreventer.util.Constants
import com.doomscrollpreventer.util.TimeUtils
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var tvTotalTime: TextView
    private lateinit var tvTotalLabel: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var progressCircle: ProgressBar
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var appBreakdownContainer: LinearLayout
    private lateinit var chartContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        tvTotalLabel = view.findViewById(R.id.tvTotalLabel)
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage)
        progressCircle = view.findViewById(R.id.progressCircle)
        toggleGroup = view.findViewById(R.id.togglePeriod)
        appBreakdownContainer = view.findViewById(R.id.appBreakdownContainer)
        chartContainer = view.findViewById(R.id.chartContainer)

        setupToggle()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupToggle() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val period = when (checkedId) {
                    R.id.btnDaily -> 0
                    R.id.btnWeekly -> 1
                    R.id.btnMonthly -> 2
                    else -> 0
                }
                viewModel.selectPeriod(period)
            }
        }
    }

    private fun observeData() {
        viewModel.todayTotal.observe(viewLifecycleOwner) { totalMs ->
            updateProgressRing(totalMs)
        }

        viewModel.periodTotal.observe(viewLifecycleOwner) { totalMs ->
            tvTotalTime.text = TimeUtils.formatDuration(totalMs)
        }

        viewModel.appBreakdown.observe(viewLifecycleOwner) { apps ->
            renderAppBreakdown(apps)
        }

        viewModel.dailyTotals.observe(viewLifecycleOwner) { dailies ->
            if (dailies.isNotEmpty()) {
                chartContainer.visibility = View.VISIBLE
                renderBarChart(dailies)
            } else {
                chartContainer.visibility = View.GONE
            }
        }

        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            tvTotalLabel.text = when (period) {
                0 -> "Today's Screen Time"
                1 -> "This Week"
                2 -> "This Month"
                else -> "Total"
            }
        }
    }

    private fun updateProgressRing(totalMs: Long) {
        val blockLimit = requireContext().getSharedPreferences(Constants.PREFS_NAME, 0)
            .getLong(Constants.PREF_BLOCK_LIMIT, Constants.DEFAULT_BLOCK_LIMIT_MS)
        val progress = ((totalMs.toFloat() / blockLimit) * 100).toInt().coerceIn(0, 100)
        progressCircle.progress = progress

        tvStatusMessage.text = when {
            totalMs == 0L -> "No scrolling detected today. Great job!"
            totalMs < Constants.DEFAULT_WARNING_LIMIT_MS -> "You're doing well. Keep it up!"
            totalMs < Constants.DEFAULT_BLOCK_LIMIT_MS -> "Getting close to your limit..."
            else -> "You've hit your daily limit."
        }

        val color = when {
            progress < 50 -> R.color.status_good
            progress < 80 -> R.color.status_warning
            else -> R.color.status_danger
        }
        progressCircle.progressTintList = ContextCompat.getColorStateList(requireContext(), color)
    }

    private fun renderAppBreakdown(apps: List<AppUsageSummary>) {
        appBreakdownContainer.removeAllViews()

        if (apps.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "No usage data yet"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 14f
                setPadding(0, 32, 0, 32)
            }
            appBreakdownContainer.addView(emptyView)
            return
        }

        val maxTime = apps.maxOfOrNull { it.totalScrollTimeMs } ?: 1L

        apps.forEach { app ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_app_usage, appBreakdownContainer, false)

            itemView.findViewById<TextView>(R.id.tvAppName).text = app.appDisplayName
            itemView.findViewById<TextView>(R.id.tvAppTime).text =
                TimeUtils.formatDuration(app.totalScrollTimeMs)

            val progressBar = itemView.findViewById<ProgressBar>(R.id.progressAppUsage)
            progressBar.max = 100
            progressBar.progress = ((app.totalScrollTimeMs.toFloat() / maxTime) * 100).toInt()

            appBreakdownContainer.addView(itemView)
        }
    }

    private fun renderBarChart(dailies: List<com.doomscrollpreventer.data.DailyTotal>) {
        chartContainer.removeAllViews()

        val maxMs = dailies.maxOfOrNull { it.totalMs } ?: 1L
        val barMaxHeight = 120 // dp

        val chartRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 0)
        }

        dailies.forEach { daily ->
            val barWrapper = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(4, 0, 4, 0)
            }

            val heightDp = ((daily.totalMs.toFloat() / maxMs) * barMaxHeight).toInt().coerceAtLeast(4)
            val density = resources.displayMetrics.density

            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (24 * density).toInt(),
                    (heightDp * density).toInt()
                ).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
                setBackgroundResource(R.drawable.rounded_bar)
            }

            val label = TextView(requireContext()).apply {
                text = TimeUtils.getDayLabel(daily.dateKey)
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }

            barWrapper.addView(bar)
            barWrapper.addView(label)
            chartRow.addView(barWrapper)
        }

        chartContainer.addView(chartRow)
    }
}
