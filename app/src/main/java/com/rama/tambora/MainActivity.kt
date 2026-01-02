package com.rama.tambora

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ListView

private const val PERMISSION_REQ = 100

class MainActivity : Activity() {

    private fun hasAudioPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAudioPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                PERMISSION_REQ
            )
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQ
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQ &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            setupMusicList()
        }
    }

    private fun setupMusicList() {
        val listView = findViewById<ListView>(R.id.music_list)
        MusicListHelper(this, listView).setup()
    }

    private lateinit var listView: ListView

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_home)

        if (hasAudioPermission()) {
            setupMusicList()
        } else {
            requestAudioPermission()
        }

        val root = findViewById<View>(R.id.root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft + dp(4),
                insets.systemWindowInsetTop + dp(4),
                insets.systemWindowInsetRight + dp(4),
                insets.systemWindowInsetBottom + dp(4)
            )
            insets
        }

        listView = findViewById(R.id.music_list)

        MusicListHelper(this, listView).setup()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
