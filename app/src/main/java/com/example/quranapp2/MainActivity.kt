package com.example.quranapp2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var tabLayout: TabLayout = findViewById(R.id.tabLayout)
        var viewPager2: ViewPager2 = findViewById(R.id.viewPager2)

        val adapter = TabAdapter(supportFragmentManager, lifecycle)

        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when(position) {
                0 -> {
                    tab.text = getString(R.string.juz)
                    tab.icon = getDrawable(R.drawable.juz)
                }
                1 -> {
                    tab.text = getString(R.string.surah)
                    tab.icon = getDrawable(R.drawable.surah)
                }
            }
        }.attach()
    }
}