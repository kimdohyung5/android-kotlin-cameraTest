package com.kimdo.cameratestup.models

data class LineData(
    val after_hyphen: Boolean,
    val cnt: List<List<Int>>,
    val confidence: Double,
    val confidence_rate: Double,
    val included: Boolean,
    val text: String,
    val type: String
)