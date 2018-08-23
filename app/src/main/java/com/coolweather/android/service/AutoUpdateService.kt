package com.coolweather.android.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jetbrains.anko.alarmManager
import java.io.IOException

class AutoUpdateService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateWeather()
        updateBingPic()
        val anHour = 8 * 60 * 60 * 1000//八小时的毫秒数
//        val anHour = 5 * 1000
        val triggerAtTime = SystemClock.elapsedRealtime() + anHour
        val i = Intent(this, AutoUpdateService::class.java)
        val pi = PendingIntent.getService(this, 0, i, 0)
        alarmManager.cancel(pi)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 更新天气信息
     */
    private fun updateWeather() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        weatherString?.let {
            //有缓存时直接解析天气数据
            val weather = Utility.handleWeatherResponse(weatherString)
            val weatherId = weather?.basic?.weatherId
            val weatherUrl =
                "http://guolin.tech/api/weather?cityid=${weatherId}&key=88ec82763fb04d30b8361004e3b98636"
            HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                @Throws(Exception::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseText = response.body()!!.string()
                    val weather = Utility.handleWeatherResponse(responseText)
                    if (weather != null && weather.status == "ok") {
                        val editor =
                            PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateService)
                                .edit()
                        editor.putString("weather", responseText)
                        editor.apply()
                        Log.d("AutoUpdateServiceTest","天气更新成功")
                    }
                }

            })
        }
    }

    /**
     * 更新必应每日一图
     */
    private fun updateBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body()?.string()
                val editor = PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateService).edit()
                editor.putString("bing_pic",bingPic)
                editor.apply()
                Log.d("AutoUpdateServiceTest","图片更新成功")
            }
        })
    }
}
