package com.esrannas.weatherapp.ui


import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esrannas.weatherapp.R
import com.esrannas.weatherapp.common.viewBinding
import com.esrannas.weatherapp.data.forecastModels.ForecastData
import com.esrannas.weatherapp.databinding.ActivityMainBinding
import com.esrannas.weatherapp.databinding.BottomSheetLayoutBinding
import com.esrannas.weatherapp.di.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val sheetLayoutBinding by viewBinding(BottomSheetLayoutBinding::inflate)
    private lateinit var pollutionFragment: PollutionFragment
    private var city: String = "Ankara"
    private lateinit var dialog: BottomSheetDialog
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetLayoutBinding.root)
        setContentView(binding.root)

        pollutionFragment = PollutionFragment()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    city = query
                }
                getCurrentWeather(city)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //fetchLocation()
        getCurrentWeather(city)

        with(binding) {
            tvForecast.setOnClickListener {
                openDialog()
            }

            tvLocation.setOnClickListener {
                fetchLocation()
            }
        }
    }


    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101
            )
            return
        }
        task.addOnSuccessListener {
            val geocoder = Geocoder(this, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(
                    it.latitude, it.longitude, 1
                ) { addresses -> city = addresses[0].locality }
            } else {
                val address =
                    geocoder.getFromLocation(it.latitude, it.longitude, 1) as List<Address>

                city = address[0].locality
            }
            getCurrentWeather(city)
        }
    }


    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getForecast() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getForecast(
                    "Ankara", "metric", applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {

                    val data = response.body()!!

                    val forecastArray: ArrayList<ForecastData> =
                        data.list as ArrayList<ForecastData>

                    val adapter = RvAdapter(forecastArray)
                    sheetLayoutBinding.rvForecast.adapter = adapter
                    sheetLayoutBinding.tvSheet.text = "Five days forecast in ${data.city?.name}"

                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    city, "metric", applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "App error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "App error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!
                    val iconId = data.weather?.get(0)?.icon
                    val imgUrl = "https://openweathermap.org/img/wn/$iconId.png"
                    Picasso.get().load(imgUrl).into(binding.imgWeather)
                    with(binding) {

                        tvSunrise.text =dateFormatConverter(data.sys?.sunrise?.toLong() ?:0)
                        tvSunset.text = data.sys?.sunset?.toLong()?.let { dateFormatConverter(it)

                        }
                        apply {
                            tvStatus.text = data.weather?.get(0)?.description
                            tvWind.text = "${data.wind?.speed} KM/H"
                            tvLocation.text = "${data.name}\n${data.sys?.country}"
                            tvTemp.text = "${data.main?.temp?.toInt()}°C"
                            tvFeelsLike.text = "Feels like: ${data.main?.feelsLike?.toInt()}°C"
                            tvMinTemp.text = "Min temp: ${data.main?.tempMin?.toInt()}°C"
                            tvMaxTemp.text = "Max temp: ${data.main?.tempMax?.toInt()}°C"
                            tvHumidity.text = "${data.main?.humidity} %"
                            tvPressure.text = "${data.main?.pressure} hPa"
                            tvUpdateTime.text = "Last Update: ${
                                SimpleDateFormat(
                                    "hh:mm a", Locale.ENGLISH
                                ).format(data.dt?.toLong())
                            }"

                            data.coord?.lat?.let {
                                data.coord.lon?.let { it1 ->
                                    getPollution(
                                        it, it1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getPollution(lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getPollution(
                    lat, lon, "metric", applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {

                    val data = response.body()!!

                    val num = data.list[0].main?.aqi

                    binding.tvAirQuality.text = when (num) {
                        1 -> getString(R.string.good)
                        2 -> getString(R.string.fair)
                        3 -> getString(R.string.moderate)
                        4 -> getString(R.string.poor)
                        5 -> getString(R.string.very_poor)
                        else -> "no data"
                    }
                    binding.layoutPollution.setOnClickListener {
                        val bundle = Bundle()
                        data.list[0].components?.co?.let { it1 ->
                            bundle.putDouble(
                                "co", it1
                            )
                        }
                        data.list[0].components?.nh3?.let { it1 ->
                            bundle.putDouble(
                                "nh3", it1
                            )
                        }
                        data.list[0].components?.no?.let { it1 ->
                            bundle.putDouble(
                                "no", it1
                            )
                        }
                        data.list[0].components?.no2?.let { it1 ->
                            bundle.putDouble(
                                "no2", it1
                            )
                        }
                        data.list[0].components?.o3?.let { it1 ->
                            bundle.putDouble(
                                "o3", it1
                            )
                        }
                        data.list[0].components?.pm10?.let { it1 ->
                            bundle.putDouble(
                                "pm10", it1
                            )
                        }
                        data.list[0].components?.pm25?.let { it1 ->
                            bundle.putDouble(
                                "pm2_5", it1
                            )
                        }
                        data.list[0].components?.so2?.let { it1 ->
                            bundle.putDouble(
                                "so2", it1
                            )
                        }

                        pollutionFragment.arguments = bundle

                        supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frameLayout, pollutionFragment).addToBackStack(null)
                                .commit()
                        }
                    }
                }
            }
        }
    }

    private fun openDialog() {
        getForecast()

        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, 1, RecyclerView.HORIZONTAL, false)
        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat(
            "hh:mm a", Locale.ENGLISH
        ).format(Date(date * 1000))
    }
}

