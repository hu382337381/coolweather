package com.coolweather.android.db

import org.litepal.crud.LitePalSupport

class County : LitePalSupport() {
    var id: Int = 0
    var countyName: String? = null
    var weatherId: String? = null
    var cityId: Int = 0
}