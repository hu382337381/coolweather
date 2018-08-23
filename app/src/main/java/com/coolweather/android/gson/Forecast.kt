package com.coolweather.android.gson

import com.google.gson.annotations.SerializedName

class Forecast {
    var date: String? = null

    @SerializedName("tmp")
    var temperature: Temperature? = null

    @SerializedName("cond")
    var more: More? = null

    class Temperature {
        var max: String? = null
        var min: String? = null
    }

    class More {
        @SerializedName("txt_d")
        var info: String? = null
    }
}