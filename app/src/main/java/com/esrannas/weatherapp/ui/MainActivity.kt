package com.esrannas.weatherapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.esrannas.weatherapp.MainApplication
import com.esrannas.weatherapp.R
import com.esrannas.weatherapp.common.viewBinding
import com.esrannas.weatherapp.data.model.response.CurrentWeather
import com.esrannas.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        MainApplication.provideRetrofit(this)

        getCurrentWeather()
    }

    private fun getCurrentWeather() {
        MainApplication.weatherService?.getCurrentWeather("Ankara", "metric", applicationContext.getString(R.string.api_key))
            ?.enqueue(object : Callback<CurrentWeather> {
                override fun onResponse(call: Call<CurrentWeather>, response: Response<CurrentWeather>) {
                    if (response.isSuccessful) {
                        val weather = response.body()

                        if (weather?.main != null) {
                            // Ensure the 'temp' field is what you expect it to be in the 'main' object
                            val temperature = weather.main.temp ?: "N/A"
                            binding.tvTemp.text = "Temp: $temperature"
                        } else {
                            binding.tvTemp.text = "Weather data not available"
                        }
                    } else {
                        binding.tvTemp.text = "Failed to fetch weather data (${response.code()})"
                    }
                }

                override fun onFailure(call: Call<CurrentWeather>, t: Throwable) {
                    Log.e("API_CALL_FAILURE", "Failed to fetch weather data: ${t.message}")
                    binding.tvTemp.text = "Failed to fetch weather data. Please try again later."
                }
            })
    }

}


