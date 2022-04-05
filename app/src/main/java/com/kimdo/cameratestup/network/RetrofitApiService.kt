package com.kimdo.cameratestup.network

import com.kimdo.cameratestup.models.InputInfo
import com.kimdo.cameratestup.models.LoginResponse
import com.kimdo.cameratestup.models.RecogResponse
import okhttp3.MultipartBody

import retrofit2.http.*
import javax.inject.Singleton

@Singleton
interface RetrofitApiService {

    @Multipart
    @POST("api/v1/upload/")
    suspend fun ocrResultV1(@Part file : MultipartBody.Part,
                            @Part("learner_id") learner_id:String ): RecogResponse

    @Multipart
    @POST("api/v2/upload/")
    suspend fun ocrResultV2(@Part file : MultipartBody.Part,
                            @Part("learner_id") learner_id:String ): RecogResponse

    @POST("api/login")
    suspend fun postLogin(@Body input: InputInfo) : LoginResponse

//    @FormUrlEncoded
//    @POST("admin/report/")
//    fun getOCR_Report(@Field("awsurl") awsurl:String,
//                      @Field("recresult") recresult:String,
//                      @Field("ocrdata") ocrdata:String,
//                      @Field("reason") reason:String ): Call<ResponseBody>

    //    @Multipart
//    @POST("api/v1/upload/")
//    fun getOCR_Result_v1(@Part file : MultipartBody.Part,
//                      @Part("learner_id") learner_id:String ): Call<RecogResponse>
//
//    @Multipart
//    @POST("api/v2/upload/")
//    fun getOCR_Result_v2(@Part file : MultipartBody.Part,
//                      @Part("learner_id") learner_id:String ): Call<RecogResponse>
//
////    @FormUrlEncoded
////    @POST("admin/report/")
////    fun getOCR_Report(@Field("awsurl") awsurl:String,
////                      @Field("recresult") recresult:String,
////                      @Field("ocrdata") ocrdata:String,
////                      @Field("reason") reason:String ): Call<ResponseBody>
//
//    @POST("api/login")
//    fun postLogin(@Body input: InputInfo) : Call<LoginResponse>


}