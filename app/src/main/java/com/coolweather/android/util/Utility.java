package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * 解析和处理服务器返回的省级数据，并保存到本地数据库（使用LitePal）
     * @param response 服务器返回的JSON格式省级数据
     * @return 是否解析并保存成功（true成功，false失败）
     */
    public static boolean handleProvinceResponse(String response) {
        // 1. 检查响应数据是否为空
        if (!TextUtils.isEmpty(response)) {
            try {
                // 2. 将JSON字符串转换为JSONArray对象
                // 因为服务器返回的是省份数组，所以直接转换为JSONArray
                JSONArray allProvinces = new JSONArray(response);

                // 3. 遍历所有省份数据
                for (int i = 0; i < allProvinces.length(); i++) {
                    // 3.1 获取数组中第i个省份的JSON对象
                    JSONObject provinceObject = allProvinces.getJSONObject(i);

                    // 3.2 创建Province实体对象
                    Province province = new Province();
                    // 从JSON对象中获取"name"字段值作为省份名称
                    province.setProvinceName(provinceObject.getString("name"));
                    // 从JSON对象中获取"id"字段值作为省份编码
                    province.setProvinceCode(provinceObject.getInt("id"));

                    // 3.3 使用LitePal的save()方法将省份数据保存到数据库
                    // 注意：Province类需要继承LitePalSupport才能使用save()方法
                    province.save();
                }
                return true; // 4. 所有省份数据保存成功，返回true
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false; // 6. 如果数据为空或解析过程中出现异常，返回false
    }

    /**
     * 解析和处理服务器返回的市级数据，并保存到本地数据库
     * @param response 服务器返回的JSON格式市级数据
     * @param provinceId 这些城市所属的省份ID
     * @return 是否解析并保存成功
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        // 1. 检查响应数据是否为空
        if(!TextUtils.isEmpty(response)) {
            try{
                // 2. 将JSON字符串转换为JSONArray对象
                JSONArray allCities = new JSONArray(response);

                // 3. 遍历所有城市数据
                for (int i = 0; i < allCities.length(); i++) {
                    // 3.1 获取单个城市的JSON对象
                    JSONObject cityObject = allCities.getJSONObject(i);

                    // 3.2 创建City实体对象
                    City city = new City();
                    // 设置城市名称
                    city.setCityName(cityObject.getString("name"));
                    // 设置城市编码
                    city.setCityCode(cityObject.getInt("id"));
                    // 设置所属省份ID，建立关联关系
                    city.setProvinceId(provinceId);

                    // 3.3 保存到数据库
                    city.save();
                }
                return true; // 4. 所有城市数据保存成功
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false; // 6. 数据为空或解析失败
    }

    /**
     * 解析和处理服务器返回的县级数据，并保存到本地数据库
     * @param response 服务器返回的JSON格式县级数据
     * @param cityId 这些县区所属的城市ID
     * @return 是否解析并保存成功
     */
    public static boolean handleCountyResponse(String response, int cityId) {
        // 1. 检查响应数据是否为空
        if(!TextUtils.isEmpty(response)) {
            try {
                // 2. 将JSON字符串转换为JSONArray对象
                JSONArray allCounties = new JSONArray(response);

                // 3. 遍历所有县区数据
                for (int i = 0; i <allCounties.length(); i++) {
                    // 3.1 获取单个县区的JSON对象
                    JSONObject countyObject = allCounties.getJSONObject(i);

                    // 3.2 创建County实体对象
                    County county = new County();
                    // 设置县区名称
                    county.setCountyName(countyObject.getString("name"));
                    // 设置天气ID（可能是用于获取天气信息的唯一标识）
                    county.setWeatherId(countyObject.getString("weather_id"));
                    // 设置所属城市ID，建立关联关系
                    county.setCityId(cityId);

                    // 3.3 保存到数据库
                    county.save();
                }
                return true; // 4. 所有县区数据保存成功
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false; // 6. 数据为空或解析失败
    }
}