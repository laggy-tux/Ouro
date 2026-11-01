package com.example.red_panda.data.paging

import com.example.red_panda.data.local.LocalPhotoItem

sealed class TimelineItem {
    data class Header(val title: String) : TimelineItem()
    data class Photo(val photo: LocalPhotoItem) : TimelineItem()
}
