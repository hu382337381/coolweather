package com.coolweather.android.db

import org.litepal.crud.LitePalSupport

class Province : LitePalSupport() {
    var id: Int = 0
    var provinceName: String? = null
    var provinceCode: Int = 0
}
