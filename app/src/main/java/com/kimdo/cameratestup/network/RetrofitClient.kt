package com.kimdo.cameratestup.network

//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object RetrofitClient {
//    private const val baseurl = "https://dev-brs-ocr.wjtb.kr/"
//
//    private val retrofit: Retrofit.Builder by lazy {
//        Retrofit.Builder()
//            .baseUrl(baseurl)
//            .addConverterFactory(GsonConverterFactory.create())
//    }
//
//    val instance: RetrofitApiService by lazy {
//        retrofit.build().create( RetrofitApiService::class.java)
//    }
//
//}