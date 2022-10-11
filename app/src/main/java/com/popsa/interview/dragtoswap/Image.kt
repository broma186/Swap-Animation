package com.popsa.interview.dragtoswap

import android.net.Uri

data class Image(
    val imageFilename: String
) {

    val imageUrl: Uri
        get() = Uri.parse("file:///android_asset/$imageFilename")
}