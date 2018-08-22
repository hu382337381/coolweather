package com.coolweather.android.db

import org.litepal.crud.LitePalSupport

class Country : LitePalSupport() {
    var id: Int? = null
    var countryName: String? = null
    var weatherId: String? = null
    var cityId: Int? = null
}