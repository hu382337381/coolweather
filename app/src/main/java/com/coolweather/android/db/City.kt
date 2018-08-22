package com.coolweather.android.db

import org.litepal.crud.LitePalSupport

class City : LitePalSupport() {
    var id: Int? = null
    var cityName: String? = null
    var cityCode: Int? = null
    var provinceId: Int? = null
}