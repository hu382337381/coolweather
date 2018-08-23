package com.coolweather.android

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.coolweather.android.db.City
import com.coolweather.android.db.County
import com.coolweather.android.db.Province
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import kotlinx.android.synthetic.main.choose_area.*
import kotlinx.android.synthetic.main.choose_area.view.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.support.v4.toast
import org.litepal.LitePal
import java.io.IOException

class ChooseAreaFragment : Fragment() {
    companion object {
        const val LEVEL_PROVINCE = 0
        const val LEVEL_CITY = 1
        const val LEVEL_COUNTY = 2
    }

    private lateinit var adapter: ArrayAdapter<String>
    private val dataList = ArrayList<String>()
    private var pDialog: ProgressDialog? = null
    /**
     * 省列表
     */
    private lateinit var provinceList: List<Province>
    /**
     * 市列表
     */
    private lateinit var cityList: List<City>
    /**
     * 县列表
     */
    private lateinit var countyList: List<County>
    /**
     * 选中的省份
     */
    private lateinit var selectedProvince: Province
    /**
     * 选中的城市
     */
    private lateinit var selectedCity: City
    /**
     * 当前选中的级别
     */
    private var currentLevel = 0
    private var listView: ListView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.choose_area, container, false)
        adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, dataList)
        view.list_view.adapter = adapter
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        /*list_view.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (currentLevel) {
                    LEVEL_PROVINCE -> {
                        selectedProvince = provinceList[position]
                        queryCities()
                    }
                    LEVEL_CITY -> {
                        selectedCity = cityList[position]
                        queryCounties()
                    }
                }
            }
        }*/

        list_view.setOnItemClickListener { parent, view, position, id ->
            when (currentLevel) {
                LEVEL_PROVINCE -> {
                    selectedProvince = provinceList[position]
                    queryCities()
                }
                LEVEL_CITY -> {
                    selectedCity = cityList[position]
                    queryCounties()
                }
                LEVEL_COUNTY -> {
                    val weatherId = countyList[position].weatherId
                    val intent = Intent(activity, WeatherActivity::class.java)
                    intent.putExtra("weather_id", weatherId)
                    startActivity(intent)
                    activity?.finish()
                }
            }
        }

        back_button.onClick {
            when (currentLevel) {
                LEVEL_COUNTY -> queryCities()
                LEVEL_CITY -> queryProvinces()
            }
        }
        queryProvinces()
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private fun queryProvinces() {
        title_text.text = "中国"
        back_button.visibility = View.GONE
        provinceList = LitePal.findAll(Province::class.java)
        if (provinceList.size > 0) {
            dataList.clear()
            for (province in provinceList) {
                dataList.add(province.provinceName!!)
            }
            adapter.notifyDataSetChanged()
            list_view.setSelection(0)
            currentLevel = LEVEL_PROVINCE
        } else {
            val address = "http://guolin.tech/api/china"
            queryFromServer(address, "province")
        }
    }

    /**
     * 查询选中省份内所有的市，优先从数据库查询，如果没有查到再去服务器中查询
     */
    private fun queryCities() {
        title_text.text = selectedProvince.provinceName
        back_button.visibility = View.VISIBLE
        cityList =
                LitePal.where("provinceid=?", selectedProvince.id.toString()).find(City::class.java)
        if (cityList.size > 0) {
            dataList.clear()
            for (city in cityList) {
                dataList.add(city.cityName!!)
            }
            adapter.notifyDataSetChanged()
            list_view.setSelection(0)
            currentLevel = LEVEL_CITY
        } else {
            val provinceCode = selectedProvince.provinceCode
            val address = "http://guolin.tech/api/china/${provinceCode}"
            queryFromServer(address, "city")
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private fun queryCounties() {
        title_text.text = selectedCity.cityName
        back_button.visibility = View.VISIBLE
        countyList = LitePal.where("cityid=?", selectedCity.id.toString()).find(County::class.java)
        if (countyList.size > 0) {
            dataList.clear()
            for (county in countyList) {
                dataList.add(county.countyName!!)
            }
            adapter.notifyDataSetChanged()
            list_view!!.setSelection(0)
            currentLevel = LEVEL_COUNTY
        } else {
            val provinceCode = selectedProvince.provinceCode
            val cityCode = selectedCity.cityCode
            val address = "http://guolin.tech/api/china/${provinceCode}/${cityCode}"
            queryFromServer(address, "county")
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private fun queryFromServer(address: String, type: String) {
        showProgressDialog()
        HttpUtil.sendOkHttpRequest(address, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //通过runOnUIThread()方法回到主线程处理逻辑
                activity?.runOnUiThread {
                    closeProgressDialog()
                    toast("加载失败")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body()!!.string()
                var result = false
                when (type) {
                    "province" -> result = Utility.handleProvinceResponse(responseText)
                    "city" -> result =
                            Utility.handleCityResponse(responseText, selectedProvince.id)
                    "county" -> result =
                            Utility.handleCountyResponse(responseText, selectedCity.id)
                }
                if (result) {
                    activity?.runOnUiThread {
                        closeProgressDialog()
                        when (type) {
                            "province" -> queryProvinces()
                            "city" -> queryCities()
                            "county" -> queryCounties()
                        }
                    }
                }
            }
        })
    }

    /**
     * 显示进度对话框
     */
    private fun showProgressDialog() {
        if (pDialog == null) {
            pDialog = ProgressDialog(activity)
            pDialog?.setMessage("正在加载")
            pDialog?.setCanceledOnTouchOutside(false)
        }
        pDialog?.show()
    }

    /**
     * 关闭进度对话框
     */
    private fun closeProgressDialog() {
        pDialog?.dismiss()
    }
}