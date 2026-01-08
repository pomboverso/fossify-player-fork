package com.rama.tambora

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.Locale
import android.media.MediaPlayer

// ------------------------------------------------------------------------
// Models
// ------------------------------------------------------------------------

data class ParsedSongName(
    val artists: List<String>,
    val title: String,
    val countries: List<String>,
    val languages: List<String>
)

data class Song(
    val id: Long,
    val artists: List<String>,
    val title: String,
    val countries: List<String>,
    val languages: List<String>,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val uri: String
)

// ------------------------------------------------------------------------
// Helper
// ------------------------------------------------------------------------

private var mediaPlayer: MediaPlayer? = null

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
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        val musicPath = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC
        ).absolutePath

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$musicPath/%")

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                val parentFolder = path.substringBeforeLast('/')

                // Only include files directly inside Music/
                if (parentFolder != musicPath) continue

                val id = cursor.getLong(idCol)
                val rawName = cursor.getString(nameCol)
                val parsed = parseSongName(rawName)

                songs.add(
                    Song(
                        id = id,
                        artists = parsed.artists,
                        title = parsed.title,
                        countries = parsed.countries,
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
        val countryPart = parts.getOrNull(2)
        val languagePart = parts.getOrNull(3)

        val artists = artistPart.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val countries =
            countryPart?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val languages =
            languagePart?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        return ParsedSongName(
            artists = artists,
            title = titlePart,
            countries = countries,
            languages = languages
        )
    }

    // --------------------------------------------------------------------
    // ISO display helpers
    // --------------------------------------------------------------------

    private fun countryDisplayName(code: String): String {
        val upper = code.uppercase()

        return Locale("", upper).displayCountry.takeIf { it.isNotBlank() }
            ?: upper
    }

    private fun languageDisplayName(code: String): String {
        val lower = code.lowercase()
        return Locale(lower).displayLanguage.replaceFirstChar { it.uppercase() }
            .takeIf { it.isNotBlank() } ?: code.uppercase()
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
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.music_list_item, parent, false)

                val song = getItem(position) ?: return view

                val titleView = view.findViewById<TextView>(R.id.title)
                val artistView = view.findViewById<TextView>(R.id.artist)
                val tagsContainer = view.findViewById<LinearLayout>(R.id.tags_container)
                val timeView = view.findViewById<TextView>(R.id.time)
                val formatView = view.findViewById<TextView>(R.id.format)
                val sizeView = view.findViewById<TextView>(R.id.size)
                val actions = view.findViewById<View>(R.id.actions_button)

                titleView.text = song.title
                artistView.text = song.artists.joinToString(", ")

                // Clear old tags for recycled views
                tagsContainer.removeAllViews()

                // Country tags (green)
                song.countries.forEach { code ->
                    val tag = LayoutInflater.from(context)
                        .inflate(R.layout.music_tag, tagsContainer, false) as TextView
                    tag.text = countryDisplayName(code)
                    tag.setBackgroundColor(0xFF92ECC2.toInt())
                    tagsContainer.addView(tag)
                }

                // Language tags (blue/default)
                song.languages.forEach { code ->
                    val tag = LayoutInflater.from(context)
                        .inflate(R.layout.music_tag, tagsContainer, false) as TextView
                    tag.text = languageDisplayName(code)
                    tagsContainer.addView(tag)
                }

                timeView.text = formatDuration(song.durationMs)
                formatView.text = song.mimeType?.substringAfter("/") ?: "audio"
                sizeView.text = formatSize(song.sizeBytes)

                titleView.setOnClickListener { playSong(song) }

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
        mediaPlayer?.release()  // release previous
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, Uri.parse(song.uri))
            prepare()
            start()
        }
    }

    // --------------------------------------------------------------------
    // Scroll behavior
    // --------------------------------------------------------------------

    private fun setupScrollListener() {
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE &&
                    openActionsFor != null
                ) {
                    openActionsFor = null
                    (listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024f * 1024f)
        return "%.1f MB".format(mb)
    }
}
