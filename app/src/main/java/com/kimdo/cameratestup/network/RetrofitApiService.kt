package com.kimdo.cameratestup.network

import com.kimdo.cameratestup.models.InputInfo
import com.kimdo.cameratestup.models.LoginResponse
import com.kimdo.cameratestup.models.RecogResponse
import okhttp3.MultipartBody

import retrofit2.Call
import retrofit2.http.*

interface RetrofitApiService {

    @Multipart
    @POST("api/upload/")
    fun getOCR_Result(@Part file : MultipartBody.Part,
                      @Part("learner_id") learner_id:String ): Call<RecogResponse>


//    @FormUrlEncoded
//    @POST("admin/report/")
//    fun getOCR_Report(@Field("awsurl") awsurl:String,
//                      @Field("recresult") recresult:String,
//                      @Field("ocrdata") ocrdata:String,
//                      @Field("reason") reason:String ): Call<ResponseBody>

    @POST("api/login")
    fun postLogin(@Body input: InputInfo) : Call<LoginResponse>

}