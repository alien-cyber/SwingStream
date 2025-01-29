package com.example.baseball

import android.graphics.Bitmap


data class Video(
    val title: String,       // Video title
    val url: String,         // Video URL
    val thumbnail: Bitmap?    // Thumbnail as a Bitmap
)
