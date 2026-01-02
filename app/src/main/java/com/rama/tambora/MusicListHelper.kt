package com.rama.tambora

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MusicListHelper(
    private val context: Context,
    private val listView: ListView
) {

    private val prefs =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    private val pm = context.packageManager
    private val apps = mutableListOf<ResolveInfo>()

    private var openActionsFor: String? = null

    fun setup() {
        loadApps()
        sortApps()
        setupAdapter()
        setupScrollListener()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        apps.clear()
        apps.addAll(pm.queryIntentActivities(intent, 0))
    }

    private fun sortApps() {
        apps.sortWith(
            compareByDescending<ResolveInfo> {
                prefs.getBoolean(it.activityInfo.packageName, false)
            }.thenBy {
                it.loadLabel(pm).toString().lowercase()
            }
        )
    }

    private fun launchApp(pkg: String, adapter: ArrayAdapter<*>) {
        val intent = pm.getLaunchIntentForPackage(pkg)

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(
                context,
                "Unable to launch app",
                Toast.LENGTH_SHORT
            ).show()

            openActionsFor = null
            sortApps()
            adapter.notifyDataSetChanged()
        }
    }

    private fun openRowActions(pkg: String, adapter: ArrayAdapter<*>) {
        openActionsFor = pkg
        adapter.notifyDataSetChanged()
    }

    private fun closeRowActions(adapter: ArrayAdapter<*>) {
        openActionsFor = null
        adapter.notifyDataSetChanged()
    }

    private fun setupAdapter() {
        val adapter = object : ArrayAdapter<ResolveInfo>(
            context,
            R.layout.music_list_item,
            R.id.title,
            apps
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)
                val app = getItem(position) ?: return view

                val pkg = app.activityInfo.packageName

                val label = view.findViewById<TextView>(R.id.title)

                label.text = app.loadLabel(pm)

                val launchClick = View.OnClickListener {
                    launchApp(pkg, this)
                }

                label.setOnClickListener(launchClick)

                label.setOnLongClickListener {
                    openRowActions(pkg, this)
                    true
                }

                return view
            }
        }

        listView.adapter = adapter
    }

    // ------------------------------------------------------------------------
    // Scroll behavior
    // ------------------------------------------------------------------------

    private fun setupScrollListener() {
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {

            override fun onScrollStateChanged(
                view: AbsListView?,
                scrollState: Int
            ) {
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE &&
                    openActionsFor != null
                ) {
                    openActionsFor = null
                    (listView.adapter as ArrayAdapter<*>)
                        .notifyDataSetChanged()
                }
            }

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) = Unit
        })
    }
}
