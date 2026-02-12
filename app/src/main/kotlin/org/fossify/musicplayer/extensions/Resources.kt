package org.fossify.musicplayer.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.musicplayer.R
private var coverArtHeight: Int = 0
fun Resources.getCoverArtHeight(): Int {
    return if (coverArtHeight == 0) {
        getDimension(R.dimen.top_art_height).toInt()
    } else {
        coverArtHeight
    }
}
