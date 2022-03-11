package com.kimdo.cameratestup.models

data class RecogResponse(
    val code: String,
    val fulls3path: String,
    val ocrresult: Ocrresult,
    val recresult: Recresult,
    val showtext_encoded: String
)