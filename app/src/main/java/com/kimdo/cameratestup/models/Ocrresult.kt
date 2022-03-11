package com.kimdo.cameratestup.models

data class Ocrresult(
    val auto_rotate_confidence: Double,
    val auto_rotate_degrees: Int,
    val confidence: Double,
    val confidence_rate: Double,
    val is_handwritten: Boolean,
    val is_printed: Boolean,
    val latex_styled: String,
    val line_data: List<LineData>,
    val request_id: String,
    val text: String
)