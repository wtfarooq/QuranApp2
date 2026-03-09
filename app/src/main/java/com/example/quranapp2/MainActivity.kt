package com.example.quranapp2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    companion object {
        var oldBackgroundColor: Int? = null
        var transitioning = false
    }

    private var currentAppBarOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedNightMode()
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val isDarkModeTransition = transitioning

        val darkModeBtn: ImageButton = findViewById(R.id.darkModeBtn)
        updateDarkModeIcon(darkModeBtn)

        playTransitionOverlay()
        playIconSpinAnimation(darkModeBtn)

        val appBarLayout: AppBarLayout = findViewById(R.id.appBarLayout)
        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsingToolbar)
        val toolbar: androidx.appcompat.widget.Toolbar? = findViewById(R.id.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            currentAppBarOffset = verticalOffset
            if (toolbar != null) {
                val totalRange = appBar.totalScrollRange.toFloat()
                if (totalRange > 0) {
                    val expandRatio = 1f + verticalOffset / totalRange
                    val maxTranslation = collapsingToolbar.height - toolbar.height
                    darkModeBtn.translationY = maxTranslation * expandRatio
                }
            }
        }

        darkModeBtn.setOnClickListener {
            oldBackgroundColor = resolveBackgroundColor()
            transitioning = true
            saveAppBarOffset()
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            val newMode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            getSharedPreferences("settings", MODE_PRIVATE).edit { putInt("nightMode", newMode) }
            AppCompatDelegate.setDefaultNightMode(newMode)
        }

        updateContinueCard()

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager2: ViewPager2 = findViewById(R.id.viewPager2)

        val adapter = TabAdapter(this)

        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when(position) {
                0 -> {
                    tab.text = getString(R.string.juz)
                    tab.icon = ContextCompat.getDrawable(this,R.drawable.juz)
                }
                1 -> {
                    tab.text = getString(R.string.surah)
                    tab.icon = ContextCompat.getDrawable(this,R.drawable.surah)
                }
                2 -> {
                    tab.text = getString(R.string.bookmarks)
                    tab.icon = ContextCompat.getDrawable(this,R.drawable.bookmark_filled)
                }
            }
        }.attach()

        if (isDarkModeTransition) {
            val savedCollapsed = getSharedPreferences("ui_state", MODE_PRIVATE)
                .getBoolean("appBarCollapsed", false)
            if (savedCollapsed) {
                appBarLayout.post {
                    appBarLayout.setExpanded(false, false)
                }
            }
        }

        AppUpdateChecker.checkForUpdate(this)
    }

    override fun onResume() {
        super.onResume()
        updateContinueCard()
    }

    private fun saveAppBarOffset() {
        getSharedPreferences("ui_state", MODE_PRIVATE)
            .edit { putBoolean("appBarCollapsed", currentAppBarOffset != 0) }
    }

    private fun playTransitionOverlay() {
        val color = oldBackgroundColor ?: return
        oldBackgroundColor = null

        val overlay = View(this)
        overlay.setBackgroundColor(color)
        val decorView = window.decorView as ViewGroup
        decorView.addView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        overlay.animate()
            .alpha(0f)
            .setDuration(350)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    decorView.removeView(overlay)
                }
            })
            .start()
    }

    private fun playIconSpinAnimation(btn: ImageButton) {
        if (!transitioning) return
        transitioning = false
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val direction = if (isNight) 360f else -360f
        btn.rotation = 0f
        btn.scaleX = 0f
        btn.scaleY = 0f
        btn.animate()
            .rotationBy(direction)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    private fun resolveBackgroundColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return typedValue.data
    }

    private fun applySavedNightMode() {
        val mode = getSharedPreferences("settings", MODE_PRIVATE)
            .getInt("nightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun updateContinueCard() {
        val card: View = findViewById(R.id.continueReadingCard)
        val text: TextView = findViewById(R.id.continueReadingText)
        val lastPage = getSharedPreferences("reading", MODE_PRIVATE).getInt("lastPage", 0)

        if (lastPage > 0) {
            text.text = getString(R.string.continue_reading, lastPage)
            card.visibility = View.VISIBLE
            card.setOnClickListener {
                val intent = Intent(this, PageActivity::class.java)
                intent.putExtra("pageNum", lastPage)
                startActivity(intent)
            }
        } else {
            card.visibility = View.GONE
        }
    }

    private fun updateDarkModeIcon(btn: ImageButton) {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        btn.setImageResource(if (isNight) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
    }
}