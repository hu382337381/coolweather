package com.coolweather.android

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.GravityCompat
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.coolweather.android.R.layout.forecast
import com.coolweather.android.gson.Weather
import com.coolweather.android.service.AutoUpdateService
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.aqi.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.forecast_item.*
import kotlinx.android.synthetic.main.forecast_item.view.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.suggestion.*
import kotlinx.android.synthetic.main.title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startService
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.toast
import java.io.IOException

class WeatherActivity : AppCompatActivity() {
    private var mweatherId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)

        swipe_refresh.setColorSchemeResources(R.color.colorPrimary)
        nav_button.onClick {
            drawer_layout.openDrawer(GravityCompat.START)
        }

        var prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var weatherString = prefs.getString("weather", null)
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            val weather = Utility.handleWeatherResponse(weatherString)
            weather?.let {
                mweatherId = it.basic?.weatherId
                showWeatherInfo(it)
            }
        } else {
            //无缓存时去服务器查询天气数据
            mweatherId = intent.getStringExtra("weather_id")
            weather_layout.visibility = View.INVISIBLE
            requestWeather(mweatherId)
        }

//        swipe_refresh.setOnRefreshListener {
//            requestWeather(weatherId)
//        }
        swipe_refresh.onRefresh {
            requestWeather(mweatherId)
        }

        var bingPic = prefs.getString("bing_pic", null)
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bing_pic_img)
        } else {
            loadBingPic()
        }
    }

    /**
     * 根据天气id请求城市天气信息
     */
    fun requestWeather(weatherId: String?) {
        val weatherUrl =
            "http://guolin.tech/api/weather?cityid=${weatherId}&key=88ec82763fb04d30b8361004e3b98636"
        HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    toast("获取天气信息失败")
                    swipe_refresh.isRefreshing = false
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body()!!.string()
                val weather = Utility.handleWeatherResponse(responseText)
                runOnUiThread {
                    if (weather != null && weather.status == "ok") {
                        val editor =
                            PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity)
                                .edit()
                        editor.putString("weather", responseText)
                        editor.apply()
                        mweatherId = weatherId
                        showWeatherInfo(weather)
                    } else {
                        toast("获取天气信息失败")
                    }
                    swipe_refresh.isRefreshing = false
                }
            }

        })
        loadBingPic()
    }

    /**
     * 加载必应每日一图
     */
    private fun loadBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body()?.string()
                val editor =
                    PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
                runOnUiThread {
                    Glide.with(this@WeatherActivity).load(bingPic).into(bing_pic_img)
                }
            }
        })
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    fun showWeatherInfo(weather: Weather) {
        val cityName = weather.basic?.cityName
        val updateTime = weather.basic?.update?.updateTime?.split(" ")?.get(1)
        val degree = (weather.now?.temperature ?: "") + "℃"
        val weatherInfo = weather.now?.more?.info
        title_city.text = cityName
        title_update_time.text = updateTime
        degree_text.text = degree
        weather_info_text.text = weatherInfo
        forecast_layout.removeAllViews()
        weather.forecastList?.let {
            for (forecast in it) {
                val view = LayoutInflater.from(this)
                    .inflate(R.layout.forecast_item, forecast_layout, false)
                view.date_text.text = forecast.date
                view.info_text.text = forecast.more?.info
                view.max_text.text = (forecast.temperature?.max ?: "") + "℃"
                view.min_text.text = (forecast.temperature?.min ?: "") + "℃"
                forecast_layout.addView(view)
            }
        }
        weather.aqi?.let {
            aqi_text.text = it.city?.aqi
            pm25_text.text = it.city?.pm25
        }
        val comfort = "舒适度：${weather.suggestion?.comfort?.info}"
        val carWash = "洗车指数：${weather.suggestion?.carWash?.info}"
        val sport = "运动建议：${weather.suggestion?.sport?.info}"
        comfort_text.text = comfort
        car_wash_text.text = carWash
        sport_text.text = sport
        weather_layout.visibility = View.VISIBLE
        startService<AutoUpdateService>()
    }
}
