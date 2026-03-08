package com.example.quranapp2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.ProgressBar
import androidx.viewpager2.widget.ViewPager2
import com.example.quranapp2.db.DatabaseHelper

class PageActivity : AppCompatActivity() {

    companion object {
        var oldBackgroundColor: Int? = null
        var transitioning = false
        private const val ICON_HIDE_DELAY = 4000L
        private const val ICON_FADE_DURATION = 300L
        private const val ICON_SLIDE_DP = 24f
    }

    private var pageNum: Int? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var bookmarkBtn: ImageButton
    private lateinit var darkModeBtn: ImageButton
    private lateinit var juzProgress: ProgressBar
    private lateinit var adapter: PageAdapter
    private val hideHandler = Handler(Looper.getMainLooper())
    private var iconsVisible = true
    private val hideIconsRunnable = Runnable { fadeOutIcons() }
    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        val mode = getSharedPreferences("settings", MODE_PRIVATE)
            .getInt("nightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_page)

        insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        darkModeBtn = findViewById(R.id.darkModeBtn)
        updateDarkModeIcon(darkModeBtn)
        playTransitionOverlay()
        playIconSpinAnimation(darkModeBtn)
        scheduleHideIcons()

        darkModeBtn.setOnClickListener {
            adapter.saveVisibleScrollPosition(viewPager)
            oldBackgroundColor = resolveBackgroundColor()
            transitioning = true
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            val newMode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            getSharedPreferences("settings", MODE_PRIVATE).edit { putInt("nightMode", newMode) }
            AppCompatDelegate.setDefaultNightMode(newMode)
        }

        if(savedInstanceState == null)
            pageNum = intent.getIntExtra("pageNum", 0)
        else
            pageNum = savedInstanceState.getInt("rotatePageNum")
        @Suppress("DiscouragedApi")
        val list = Array(604) { i ->
            resources.getIdentifier("q${i + 1}", "drawable", packageName)
        }

        viewPager = findViewById(R.id.pageViewPager2)
        bookmarkBtn = findViewById(R.id.bookmarkBtn)
        juzProgress = findViewById(R.id.juzProgress)

        val dp8 = (8 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(viewPager) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, bars.bottom)
            (bookmarkBtn.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = bars.top + dp8
                marginStart = bars.left + dp8
            }
            (darkModeBtn.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = bars.top + dp8
                marginEnd = bars.right + dp8
            }
            (juzProgress.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = bars.bottom
            }
            bookmarkBtn.requestLayout()
            darkModeBtn.requestLayout()
            juzProgress.requestLayout()
            insets
        }

        val dbHelper = DatabaseHelper(this)
        adapter = PageAdapter(list, dbHelper)

        viewPager.alpha = 0f
        viewPager.adapter = adapter
        viewPager.setCurrentItem(pageNum!! - 1, false)
        viewPager.post {
            viewPager.animate().alpha(1f).setDuration(250).start()
        }

        updateBookmarkIcon(currentPage())

        val juzStarts = IntArray(31) { i -> if (i <= 1) 1 else (i - 1) * 20 + 2 }
        val juzProgressValues = FloatArray(605)
        for (p in 1..604) {
            val juz = juzStarts.indexOfLast { it <= p }
            val startPage = juzStarts[juz]
            val endPage = if (juz < 30) juzStarts[juz + 1] - 1 else 604
            val totalPages = endPage - startPage
            juzProgressValues[p] = if (totalPages > 0)
                (p - startPage).toFloat() / totalPages * 1000f
            else 0f
        }

        juzProgress.max = 1000
        juzProgress.progress = juzProgressValues[currentPage()].toInt()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBookmarkIcon(position + 1)
                saveLastPage(position + 1)
                juzProgress.progress = juzProgressValues[position + 1].toInt()
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val curPage = (position + 1).coerceIn(1, 604)
                val nxtPage = (position + 2).coerceIn(1, 604)
                val interpolated = juzProgressValues[curPage] +
                    (juzProgressValues[nxtPage] - juzProgressValues[curPage]) * positionOffset
                juzProgress.progress = interpolated.toInt()
            }
        })

        saveLastPage(currentPage())

        bookmarkBtn.setOnClickListener {
            val isCurrentlyBookmarked = getSharedPreferences("bookmarks", MODE_PRIVATE)
                .getStringSet("pages", mutableSetOf())!!.contains(currentPage().toString())
            if (!isCurrentlyBookmarked) {
                playBookmarkAnimation()
            }
            toggleBookmark(currentPage())
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (!iconsVisible) {
                fadeInIcons()
            }
            scheduleHideIcons()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun scheduleHideIcons() {
        hideHandler.removeCallbacks(hideIconsRunnable)
        hideHandler.postDelayed(hideIconsRunnable, ICON_HIDE_DELAY)
    }

    private val slidePx by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ICON_SLIDE_DP, resources.displayMetrics)
    }

    private fun fadeOutIcons() {
        if (!iconsVisible) return
        iconsVisible = false
        bookmarkBtn.animate().cancel()
        darkModeBtn.animate().cancel()
        juzProgress.animate().cancel()
        bookmarkBtn.animate().alpha(0f).translationY(-slidePx).setDuration(ICON_FADE_DURATION).start()
        darkModeBtn.animate().alpha(0f).translationY(-slidePx).setDuration(ICON_FADE_DURATION).start()
        juzProgress.animate().alpha(0f).translationY(slidePx).setDuration(ICON_FADE_DURATION).start()
    }

    private fun fadeInIcons() {
        iconsVisible = true
        bookmarkBtn.animate().cancel()
        darkModeBtn.animate().cancel()
        juzProgress.animate().cancel()
        bookmarkBtn.translationY = -slidePx
        darkModeBtn.translationY = -slidePx
        juzProgress.translationY = slidePx
        bookmarkBtn.animate().alpha(1f).translationY(0f).setDuration(ICON_FADE_DURATION).start()
        darkModeBtn.animate().alpha(1f).translationY(0f).setDuration(ICON_FADE_DURATION).start()
        juzProgress.animate().alpha(1f).translationY(0f).setDuration(ICON_FADE_DURATION).start()
    }

    private fun currentPage(): Int = viewPager.currentItem + 1

    private fun saveLastPage(page: Int) {
        getSharedPreferences("reading", MODE_PRIVATE)
            .edit { putInt("lastPage", page) }
    }

    private fun toggleBookmark(page: Int) {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val bookmarks = prefs.getStringSet("pages", mutableSetOf())!!.toMutableSet()
        val pageStr = page.toString()
        if (bookmarks.contains(pageStr)) {
            bookmarks.remove(pageStr)
        } else {
            bookmarks.add(pageStr)
        }
        prefs.edit { putStringSet("pages", bookmarks) }
        updateBookmarkIcon(page)
    }

    private fun playBookmarkAnimation() {
        bookmarkBtn.animate()
            .scaleX(1.1f).scaleY(1.1f)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                bookmarkBtn.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(3f))
                    .start()
            }
            .start()

        val burst = BurstView(this)
        val parent = bookmarkBtn.parent as ViewGroup
        parent.addView(burst, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        val cx = bookmarkBtn.x + bookmarkBtn.width / 2f
        val cy = bookmarkBtn.y + bookmarkBtn.height / 2f
        burst.startAt(cx, cy)
    }

    private fun updateBookmarkIcon(page: Int) {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val bookmarks = prefs.getStringSet("pages", mutableSetOf())!!
        if (bookmarks.contains(page.toString())) {
            bookmarkBtn.setImageResource(R.drawable.bookmark_filled)
            bookmarkBtn.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            bookmarkBtn.setImageResource(R.drawable.bookmark)
            bookmarkBtn.setColorFilter(ContextCompat.getColor(this, R.color.iconTint))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapter.saveVisibleScrollPosition(viewPager)
        if(pageNum != null)
            outState.putInt("rotatePageNum", viewPager.currentItem+1)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) PageAdapter.clearScrollPositions()
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

    private fun updateDarkModeIcon(btn: ImageButton) {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        btn.setImageResource(if (isNight) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
    }
}