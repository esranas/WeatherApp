package com.esrannas.weatherapp.source.remote

import com.esrannas.weatherapp.data.model.response.CurrentWeather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather?")
    fun getCurrentWeather(
        @Query("q") city: String,
        @Query("units") units: String,
        @Query("appid") appid: String,
    ):Call<CurrentWeather>
}