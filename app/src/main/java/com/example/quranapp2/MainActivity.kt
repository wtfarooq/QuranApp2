package com.example.quranapp2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val lastPage = getSharedPreferences("reading", MODE_PRIVATE).getInt("lastPage", 0)
            if (lastPage > 0) {
                val intent = Intent(this, PageActivity::class.java)
                intent.putExtra("pageNum", lastPage)
                startActivity(intent)
            }
        }

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
    }
}