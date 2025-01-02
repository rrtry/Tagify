package com.rrtry.tagify.data.entities

data class TrackWithTags(
    val track: Track,
    var tag: Tag?,
    val tags: List<Tag>,
    var apply: Boolean = tag != null,
)
