package com.coolweather.android.db

import org.litepal.crud.LitePalSupport

class Province : LitePalSupport() {
    var id: Int? = null
    var provinceName: String? = null
    var provinceCode: Int? = null
}
