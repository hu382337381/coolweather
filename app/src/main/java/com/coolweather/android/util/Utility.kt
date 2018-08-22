package com.coolweather.android.util

import android.text.TextUtils
import android.view.TextureView
import com.coolweather.android.db.City
import com.coolweather.android.db.County
import com.coolweather.android.db.Province
import org.json.JSONArray
import org.w3c.dom.Text

object Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    fun handleProvinceResponse(response: String): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allProvinces = JSONArray(response)
                for (i in 0 until allProvinces.length()) {
                    val provinceObject = allProvinces.getJSONObject(i)
                    val province = Province()
                    province.provinceName = provinceObject.getString("name")
                    province.provinceCode = provinceObject.getInt("id")
                    province.save()
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    fun handleCityResponse(response: String, provinceId: Int): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allCities = JSONArray(response)
                for (i in 0 until allCities.length()) {
                    val cityObject = allCities.getJSONObject(i)
                    val city = City()
                    city.cityName = cityObject.getString("name")
                    city.cityCode = cityObject.getInt("id")
                    city.provinceId = provinceId
                    city.save()
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    fun handleCountyResponse(response: String, cityId: Int): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allCounties = JSONArray(response)
                for (i in 0 until allCounties.length()) {
                    val countyObject = allCounties.getJSONObject(i)
                    val county = County()
                    county.countyName = countyObject.getString("name")
                    county.weatherId = countyObject.getString("weather_id")
                    county.cityId = cityId
                    county.save()
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }
}