package com.kimdo.cameratestup.di


import com.kimdo.cameratestup.network.RetrofitApiService
import com.kimdo.cameratestup.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton



@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRetrofitApiService(): RetrofitApiService
        = Retrofit.Builder()
        .baseUrl(Constants.baseurl)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create( RetrofitApiService::class.java)

}