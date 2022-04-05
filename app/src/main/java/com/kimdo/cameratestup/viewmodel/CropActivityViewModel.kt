package com.kimdo.cameratestup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimdo.cameratestup.models.RecogResponse
import com.kimdo.cameratestup.repository.ApiRepository
import com.kimdo.cameratestup.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class CropActivityViewModel @Inject constructor(private val repository: ApiRepository) : ViewModel() {

    private val _recog = MutableLiveData<RecogResponseState>()
    val recog: LiveData<RecogResponseState>
        get() = _recog

    fun setValue(response: Resource<RecogResponse>) {
        when( response ) {
            is Resource.Success -> {
                _recog.value = RecogResponseState(response = response.data )
            }
            is Resource.Error -> {
                _recog.value = RecogResponseState( error = response.message ?: "An unexpected error occurred")
            }
            is Resource.Loading -> {
                _recog.value = RecogResponseState( isLoading = true )
            }
        }
    }


    fun ocrResultV1(file: MultipartBody.Part, learner_id:String) {
        repository.ocrResultV1(file, learner_id).onEach { result ->
            setValue(result)
        }.launchIn(viewModelScope)
    }

    fun ocrResultV2(file: MultipartBody.Part, learner_id:String) {
        repository.ocrResultV2(file, learner_id).onEach { result ->
            setValue(result)
        }.launchIn(viewModelScope)
    }


}