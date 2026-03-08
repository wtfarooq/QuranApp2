package com.example.quranapp2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

class JuzCompletionDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_JUZ = "juz"
        private const val ARG_TOTAL_MS = "total_ms"
        private const val ARG_PAGE_COUNT = "page_count"
        private const val ARG_COMPARISON_PERCENT = "comparison_percent"
        private const val ARG_HAS_COMPARISON = "has_comparison"
        private const val ARG_NEXT_JUZ_NAME = "next_juz_name"
        private const val ARG_IS_PERSONAL_BEST = "is_personal_best"
        private const val ARG_COMPLETED_JUZ_COUNT = "completed_juz_count"
        private const val ARG_NEXT_JUZ_DESCRIPTION = "next_juz_description"

        fun newInstance(
            juz: Int,
            totalMs: Long,
            pageCount: Int,
            comparisonPercent: Float?,
            nextJuzName: String,
            isPersonalBest: Boolean,
            completedJuzCount: Int,
            nextJuzDescription: String
        ): JuzCompletionDialogFragment {
            return JuzCompletionDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_JUZ, juz)
                    putLong(ARG_TOTAL_MS, totalMs)
                    putInt(ARG_PAGE_COUNT, pageCount)
                    putBoolean(ARG_HAS_COMPARISON, comparisonPercent != null)
                    putFloat(ARG_COMPARISON_PERCENT, comparisonPercent ?: 0f)
                    putString(ARG_NEXT_JUZ_NAME, nextJuzName)
                    putBoolean(ARG_IS_PERSONAL_BEST, isPersonalBest)
                    putInt(ARG_COMPLETED_JUZ_COUNT, completedJuzCount)
                    putString(ARG_NEXT_JUZ_DESCRIPTION, nextJuzDescription)
                }
            }
        }

        private val neutralMessages = arrayOf(
            "Masha'Allah! You've successfully finished this milestone.",
            "Another Juz in the books. Keep going!",
            "Consistency is key. Well done!",
            "Your dedication is inspiring. Keep it up!"
        )

        private val fasterMessages = arrayOf(
            "You're picking up speed. Masha'Allah!",
            "Faster than your usual pace. Impressive!",
            "You're on a roll! Keep the momentum going."
        )

        private val slowerMessages = arrayOf(
            "Slow and steady wins the race. Well done!",
            "Every page counts. Great effort!",
            "Take your time \u2014 what matters is that you keep going."
        )

        private val firstMessages = arrayOf(
            "Your first Juz completed. A great beginning!",
            "The journey of a thousand miles begins with a single step.",
            "You've taken the first step. Keep going!"
        )

        private val samePaceMessages = arrayOf(
            "Steady as she goes. Masha'Allah!",
            "Consistent pace. You're doing great!",
            "Right on track. Keep it up!"
        )

        private val personalBestMessages = arrayOf(
            "New record! You're on fire. Masha'Allah!",
            "Your fastest Juz yet. Incredible effort!",
            "A new personal best! Keep pushing forward."
        )
    }

    override fun getTheme(): Int = R.style.Theme_QuranApp2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_juz_completion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.alpha = 0f
        view.translationY = view.resources.displayMetrics.density * 80
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        val args = requireArguments()
        val juz = args.getInt(ARG_JUZ)
        val totalMs = args.getLong(ARG_TOTAL_MS)
        val pageCount = args.getInt(ARG_PAGE_COUNT)
        val hasComparison = args.getBoolean(ARG_HAS_COMPARISON)
        val comparisonPercent = args.getFloat(ARG_COMPARISON_PERCENT)
        val nextJuzName = args.getString(ARG_NEXT_JUZ_NAME, "")
        val isPersonalBest = args.getBoolean(ARG_IS_PERSONAL_BEST)
        val completedJuzCount = args.getInt(ARG_COMPLETED_JUZ_COUNT)
        val nextJuzDescription = args.getString(ARG_NEXT_JUZ_DESCRIPTION, "")

        // Personal best badge + glow
        if (isPersonalBest) {
            view.findViewById<View>(R.id.personalBestBadge).visibility = View.VISIBLE
            view.findViewById<View>(R.id.personalBestGlow).visibility = View.VISIBLE
        }

        view.findViewById<TextView>(R.id.completionTitle).text =
            getString(R.string.juz_completed, juz)

        view.findViewById<TextView>(R.id.completionSubtitle).text =
            pickSubtitle(hasComparison, comparisonPercent, isPersonalBest)

        view.findViewById<TextView>(R.id.totalTimeValue).text = formatDuration(totalMs)

        val avgMs = if (pageCount > 0) totalMs / pageCount else 0L
        view.findViewById<TextView>(R.id.avgPageTimeValue).text = formatDuration(avgMs)

        // Comparison banner
        val banner = view.findViewById<MaterialCardView>(R.id.comparisonBanner)
        if (hasComparison) {
            banner.visibility = View.VISIBLE
            val titleView = view.findViewById<TextView>(R.id.comparisonTitle)
            val subtitleView = view.findViewById<TextView>(R.id.comparisonSubtitle)
            val absPercent = abs(comparisonPercent).toInt()

            if (absPercent < 1) {
                titleView.text = getString(R.string.same_pace)
                subtitleView.visibility = View.GONE
            } else if (comparisonPercent > 0) {
                titleView.text = getString(R.string.percent_faster, absPercent)
            } else {
                titleView.text = getString(R.string.percent_slower, absPercent)
            }
        } else {
            banner.visibility = View.GONE
        }

        // Overall progress
        val progressPercent = completedJuzCount * 100 / 30
        view.findViewById<TextView>(R.id.overallProgressPercent).text =
            getString(R.string.progress_percent, progressPercent)
        view.findViewById<ProgressBar>(R.id.overallProgressBar).progress = completedJuzCount

        // Next Juz preview
        val previewCard = view.findViewById<MaterialCardView>(R.id.nextJuzPreviewCard)
        if (juz < 30 && nextJuzDescription.isNotEmpty()) {
            previewCard.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.nextUpSurahText).text = nextJuzDescription
            previewCard.setOnClickListener { dismiss() }
        } else {
            previewCard.visibility = View.GONE
        }

        // Start next Juz button
        val nextBtn = view.findViewById<MaterialButton>(R.id.startNextJuzBtn)
        if (juz < 30) {
            val nextJuz = juz + 1
            nextBtn.text = getString(R.string.start_next_juz, nextJuz, nextJuzName)
            nextBtn.setOnClickListener { dismiss() }
        } else {
            nextBtn.visibility = View.GONE
        }

        // Share button
        view.findViewById<View>(R.id.shareBtn).setOnClickListener {
            val text = getString(R.string.share_text, juz)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, null))
        }

        view.findViewById<View>(R.id.closeBtn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.backToHomeBtn).setOnClickListener {
            dismiss()
            activity?.finish()
        }
    }

    override fun dismiss() {
        val view = view ?: return super.dismiss()
        view.animate()
            .alpha(0f)
            .translationY(view.resources.displayMetrics.density * 80)
            .setDuration(250)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .withEndAction { super.dismiss() }
            .start()
    }

    private fun pickSubtitle(
        hasComparison: Boolean,
        comparisonPercent: Float,
        isPersonalBest: Boolean
    ): String {
        val pool = when {
            isPersonalBest && hasComparison -> personalBestMessages
            !hasComparison -> firstMessages
            abs(comparisonPercent) < 1f -> samePaceMessages
            comparisonPercent > 0 -> fasterMessages
            comparisonPercent < 0 -> slowerMessages
            else -> neutralMessages
        }
        return pool.random()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
