package com.rama.tambora

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

// ------------------------------------------------------------------------
// Models
// ------------------------------------------------------------------------

data class ParsedSongName(
    val artists: List<String>,
    val title: String,
    val extra: String?,
    val languages: List<String>
)

data class Song(
    val id: Long,
    val artists: List<String>,
    val title: String,
    val extra: String?,
    val languages: List<String>,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val uri: String
)

// ------------------------------------------------------------------------
// Helper
// ------------------------------------------------------------------------

class MusicListHelper(
    private val context: Context,
    private val listView: ListView
) {

    private val songs = mutableListOf<Song>()
    private var openActionsFor: String? = null

    // --------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------

    fun setup() {
        loadSongs()
        setupAdapter()
        setupScrollListener()
    }

    // --------------------------------------------------------------------
    // MediaStore loading
    // --------------------------------------------------------------------

    private fun loadSongs() {
        songs.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DISPLAY_NAME
            )
            val durationCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DURATION
            )
            val sizeCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.SIZE
            )
            val mimeCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.MIME_TYPE
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawName = cursor.getString(nameCol)

                val parsed = parseSongName(rawName)

                songs.add(
                    Song(
                        id = id,
                        artists = parsed.artists,
                        title = parsed.title,
                        extra = parsed.extra,
                        languages = parsed.languages,
                        durationMs = cursor.getLong(durationCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()
                    )
                )
            }
        }
    }

    // --------------------------------------------------------------------
    // Filename parser
    // --------------------------------------------------------------------

    private fun parseSongName(rawName: String): ParsedSongName {
        val name = rawName.substringBeforeLast('.')

        val parts = name.split(" - ").map { it.trim() }

        val artistPart = parts.getOrNull(0).orEmpty()
        val titlePart = parts.getOrNull(1).orEmpty()
        val langPart = parts.getOrNull(2)

        val artists = artistPart
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val titleRegex = Regex("""^(.*?)\s*(?:\((.*?)\))?$""")
        val match = titleRegex.find(titlePart)

        val title = match?.groupValues?.get(1)?.trim().orEmpty()
        val extra = match?.groupValues?.getOrNull(2)?.trim()

        val languages = langPart
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        return ParsedSongName(
            artists = artists,
            title = title,
            extra = extra,
            languages = languages
        )
    }

    // --------------------------------------------------------------------
    // Adapter
    // --------------------------------------------------------------------

    private fun setupAdapter() {
        val adapter = object : ArrayAdapter<Song>(
            context,
            R.layout.music_list_item,
            songs
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = convertView
                    ?: View.inflate(context, R.layout.music_list_item, null)

                val song = getItem(position) ?: return view

                val titleView = view.findViewById<TextView>(R.id.title)
                val artistView = view.findViewById<TextView>(R.id.artist)
                val langView = view.findViewById<TextView>(R.id.lang)
                val timeView = view.findViewById<TextView>(R.id.time)
                val formatView = view.findViewById<TextView>(R.id.format)
                val sizeView = view.findViewById<TextView>(R.id.size)
                val actions = view.findViewById<View>(R.id.actions_button)

                titleView.text = song.title +
                        (song.extra?.let { " ($it)" } ?: "")

                artistView.text = song.artists.joinToString(", ")
                langView.text = song.languages.joinToString(", ")

                timeView.text = formatDuration(song.durationMs)
                formatView.text = song.mimeType?.substringAfter("/") ?: "audio"
                sizeView.text = formatSize(song.sizeBytes)

                titleView.setOnClickListener {
                    playSong(song)
                }

                titleView.setOnLongClickListener {
                    openActionsFor = song.uri
                    notifyDataSetChanged()
                    true
                }

                actions.setOnClickListener {
                    openActionsFor = song.uri
                    notifyDataSetChanged()
                }

                return view
            }
        }

        listView.adapter = adapter
    }

    // --------------------------------------------------------------------
    // Playback
    // --------------------------------------------------------------------

    private fun playSong(song: Song) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(song.uri), "audio/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Unable to play song",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --------------------------------------------------------------------
    // Scroll behavior
    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------
    // Utils
    // --------------------------------------------------------------------

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024f * 1024f)
        return "%.1f MB".format(mb)
    }
}
