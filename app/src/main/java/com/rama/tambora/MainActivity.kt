package com.rama.tambora

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ListView

class MainActivity : Activity() {
    private lateinit var listView: ListView

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_home)

        val root = findViewById<View>(R.id.root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft + dp(32),
                insets.systemWindowInsetTop + dp(32),
                insets.systemWindowInsetRight + dp(32),
                insets.systemWindowInsetBottom + dp(32)
            )
            insets
        }

        listView = findViewById(R.id.musicList)

        MusicListHelper(this, listView).setup()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
