package com.example.red_panda.ui.model

import com.example.red_panda.data.local.entity.PhotoRefEntity

data class EventMock(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val totalPhotos: Int,
    val thumbnailColors: List<Long>,
    val remainingPhotosCount: Int,
    val placeTag: String = "All Places",
    val personTag: String = "All People",
    val note: String = "",
    val photoUris: List<String> = emptyList(),
    val photoRefs: List<PhotoRefEntity> = emptyList()
)

data class SuggestedEvent(
    val dateString: String,
    val photoCount: Int,
    val sampleUris: List<String>,
    val photoUris: List<String>
)

data class Album(
    val id: String,
    val title: String,
    val photoCount: Int,
    val section: String, // "Recently" or "Other Albums"
    val isFavorite: Boolean = false,
    val coverColor: Long = 0xFF2C2C30,
    val coverPhotoUri: String? = null,
    val coverPhotoId: Long? = null,
    val events: List<EventMock> = emptyList()
)

val mockAlbums = listOf(
    Album(
        id = "1",
        title = "My Favorites",
        photoCount = 108,
        section = "Recently",
        isFavorite = true,
        coverColor = 0xFF4A3525,
        events = listOf(
            EventMock(
                id = "e101",
                title = "Sunset Coast",
                date = "Oct 12-15",
                location = "Big Sur, California",
                totalPhotos = 42,
                thumbnailColors = listOf(0xFF8B5E3C, 0xFFA67C52, 0xFFC49A6C, 0xFF5C3D2E, 0xFF3D2E24),
                remainingPhotosCount = 37,
                placeTag = "Big Sur",
                personTag = "Friends"
            ),
            EventMock(
                id = "e102",
                title = "Yosemite Hike",
                date = "Aug 20-24",
                location = "Yosemite National Park",
                totalPhotos = 66,
                thumbnailColors = listOf(0xFF2E4A32, 0xFF3D5C42, 0xFF527C5B, 0xFF1E3823, 0xFF142417),
                remainingPhotosCount = 61,
                placeTag = "Yosemite",
                personTag = "Family"
            )
        )
    ),
    Album(
        id = "2",
        title = "United States",
        photoCount = 274,
        section = "Recently",
        isFavorite = true,
        coverColor = 0xFF284035,
        events = listOf(
            EventMock(
                id = "e201",
                title = "Statue of Liberty & NYC",
                date = "Jul 4-10",
                location = "New York City, NY",
                totalPhotos = 120,
                thumbnailColors = listOf(0xFF385263, 0xFF4A6B80, 0xFF6389A3, 0xFF283B47, 0xFF1A2730),
                remainingPhotosCount = 115,
                placeTag = "New York",
                personTag = "Friends"
            ),
            EventMock(
                id = "e202",
                title = "Grand Canyon Trail",
                date = "May 1-6",
                location = "Arizona, USA",
                totalPhotos = 154,
                thumbnailColors = listOf(0xFF8C4A32, 0xFFA85E42, 0xFFC7785B, 0xFF693320, 0xFF472114),
                remainingPhotosCount = 149,
                placeTag = "Arizona",
                personTag = "Family"
            )
        )
    ),
    Album(
        id = "3",
        title = "People & Places",
        photoCount = 223,
        section = "Recently",
        isFavorite = false,
        coverColor = 0xFF35354A,
        events = listOf(
            EventMock(
                id = "e301",
                title = "Marcshire",
                date = "Dec 5-26",
                location = "North Treva, Switzerland",
                totalPhotos = 85,
                thumbnailColors = listOf(0xFF4A5568, 0xFF718096, 0xFFA0AEC0, 0xFF2D3748, 0xFF1A202C),
                remainingPhotosCount = 12,
                placeTag = "Switzerland",
                personTag = "Friends"
            ),
            EventMock(
                id = "e302",
                title = "South Coralie",
                date = "May 8-30",
                location = "Maanbus, Western Sahare",
                totalPhotos = 98,
                thumbnailColors = listOf(0xFF7B341E, 0xFF9C4221, 0xFFC05621, 0xFF522312, 0xFF33150B),
                remainingPhotosCount = 8,
                placeTag = "Western Sahare",
                personTag = "Family"
            ),
            EventMock(
                id = "e303",
                title = "Alpine Heights",
                date = "Jan 12-18",
                location = "Zermatt, Switzerland",
                totalPhotos = 40,
                thumbnailColors = listOf(0xFF2B6CB0, 0xFF3182CE, 0xFF4299E1, 0xFF1A365D, 0xFF0F2942),
                remainingPhotosCount = 15,
                placeTag = "Switzerland",
                personTag = "Friends"
            )
        )
    ),
    Album(
        id = "4",
        title = "Last Summer",
        photoCount = 393,
        section = "Recently",
        isFavorite = true,
        coverColor = 0xFF4A2535,
        events = listOf(
            EventMock(
                id = "e401",
                title = "Amalfi Coast Drive",
                date = "Jul 15-28",
                location = "Amalfi, Italy",
                totalPhotos = 210,
                thumbnailColors = listOf(0xFF2C7A7B, 0xFF319795, 0xFF38B2AC, 0xFF1D4044, 0xFF12282A),
                remainingPhotosCount = 25,
                placeTag = "Italy",
                personTag = "Friends"
            ),
            EventMock(
                id = "e402",
                title = "Santorini Sunsets",
                date = "Aug 1-10",
                location = "Santorini, Greece",
                totalPhotos = 183,
                thumbnailColors = listOf(0xFF805AD5, 0xFF9F7AEA, 0xFFB794F4, 0xFF553C9A, 0xFF322659),
                remainingPhotosCount = 18,
                placeTag = "Greece",
                personTag = "Family"
            )
        )
    ),
    Album(
        id = "5",
        title = "Happy New Year",
        photoCount = 87,
        section = "Other Albums",
        isFavorite = false,
        coverColor = 0xFF4A2525,
        events = listOf(
            EventMock(
                id = "e501",
                title = "Times Square Countdown",
                date = "Dec 31 - Jan 1",
                location = "New York, USA",
                totalPhotos = 87,
                thumbnailColors = listOf(0xFF9B2C2C, 0xFFC53030, 0xFFE53E3E, 0xFF742A2A, 0xFF4A1C1C),
                remainingPhotosCount = 10,
                placeTag = "New York",
                personTag = "Friends"
            )
        )
    ),
    Album(
        id = "6",
        title = "Kleinmouth",
        photoCount = 230,
        section = "Other Albums",
        isFavorite = false,
        coverColor = 0xFF254A35,
        events = listOf(
            EventMock(
                id = "e601",
                title = "Coastal Bay Retreat",
                date = "Jun 10-18",
                location = "Kleinmouth Bay, Western Cape",
                totalPhotos = 230,
                thumbnailColors = listOf(0xFF276749, 0xFF2F855A, 0xFF38A169, 0xFF1C4532, 0xFF122C20),
                remainingPhotosCount = 22,
                placeTag = "South Africa",
                personTag = "Family"
            )
        )
    ),
    Album(
        id = "7",
        title = "Anguilla",
        photoCount = 78,
        section = "Other Albums",
        isFavorite = true,
        coverColor = 0xFF25354A,
        events = listOf(
            EventMock(
                id = "e701",
                title = "Shoal Bay East",
                date = "Nov 12-19",
                location = "Anguilla, Caribbean",
                totalPhotos = 78,
                thumbnailColors = listOf(0xFF00A3C4, 0xFF00B5D8, 0xFF00C49F, 0xFF006B7D, 0xFF00434E),
                remainingPhotosCount = 14,
                placeTag = "Caribbean",
                personTag = "Friends"
            )
        )
    ),
    Album(
        id = "8",
        title = "San Francisco",
        photoCount = 142,
        section = "Other Albums",
        isFavorite = false,
        coverColor = 0xFF3D3D3D,
        events = listOf(
            EventMock(
                id = "e801",
                title = "Golden Gate Park",
                date = "Apr 5-9",
                location = "San Francisco, CA",
                totalPhotos = 142,
                thumbnailColors = listOf(0xFF4A5568, 0xFF718096, 0xFFA0AEC0, 0xFF2D3748, 0xFF1A202C),
                remainingPhotosCount = 16,
                placeTag = "San Francisco",
                personTag = "Family"
            )
        )
    )
)
