package com.esrannas.weatherapp

import android.app.Application
import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.esrannas.weatherapp.common.Constants.BASE_URL
import com.esrannas.weatherapp.source.remote.ApiInterface
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainApplication:Application() {

    companion object{
        var weatherService: ApiInterface?=null


        fun provideRetrofit(context: Context){

            val chucker= ChuckerInterceptor.Builder(context).build()

            val okHttpClient= OkHttpClient.Builder().apply {
                addInterceptor(
                    Interceptor {chain->
                        val builder=chain.request().newBuilder()
                        builder.header("appid","d93a7582c191ad38e996fe02ec1d203c")
                        return@Interceptor chain.proceed(builder.build())
                    }

                )
                addInterceptor(chucker)
            }.build()

            //Create retrofit
            val retrofit= Retrofit.Builder().apply {
                addConverterFactory(GsonConverterFactory.create())
                baseUrl(BASE_URL)
                client(okHttpClient)
            }.build()

            //Adding service to retrofit
            weatherService=retrofit.create(ApiInterface::class.java)
        }
    }
}