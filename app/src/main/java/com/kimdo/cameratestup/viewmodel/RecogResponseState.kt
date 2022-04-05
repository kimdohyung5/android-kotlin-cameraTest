package com.kimdo.cameratestup.viewmodel

import com.kimdo.cameratestup.models.RecogResponse

data class RecogResponseState (
    val isLoading: Boolean = false,
    val response: RecogResponse? = null,
    val error: String = ""
)