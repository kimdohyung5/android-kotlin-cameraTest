package com.kimdo.cameratestup.repository

import android.util.Log
import com.kimdo.cameratestup.models.RecogResponse
import com.kimdo.cameratestup.network.RetrofitApiService
import com.kimdo.cameratestup.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import java.lang.Exception
import javax.inject.Inject

class ApiRepository @Inject constructor (private val api:RetrofitApiService) {
    fun ocrResultV1(file: MultipartBody.Part, learner_id:String): Flow<Resource<RecogResponse>> = flow {
        try {
            emit( Resource.Loading())
            val response = api.ocrResultV1(file, learner_id)
            Log.d("ApiRepository v1", "response ${response}")
            emit( Resource.Success(response) )
        } catch(e: Exception) {
            Log.d("ApiRepository v1", "error")
            emit(Resource.Error("couldn't reach server... check your internet connection."))
        }
    }

    fun ocrResultV2(file: MultipartBody.Part, learner_id:String): Flow<Resource<RecogResponse>> = flow {
        try {
            emit( Resource.Loading())
            val response = api.ocrResultV2(file, learner_id)
            Log.d("ApiRepository v2", "response ${response}")
            emit( Resource.Success(response) )
        } catch(e: Exception) {
            Log.d("ApiRepository v2", "error")
            emit(Resource.Error("couldn't reach server... check your internet connection."))
        }
    }

}