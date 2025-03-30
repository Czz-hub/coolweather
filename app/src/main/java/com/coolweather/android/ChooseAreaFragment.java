package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.litepal.LitePal;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 地区选择Fragment，用于选择省-市-县三级行政区域
 */
public class ChooseAreaFragment extends Fragment {
    // 定义三级行政级别常量
    public static final int LEVEL_PROVINCE = 0;  // 省级别标识
    public static final int LEVEL_CITY = 1;      // 市级别标识
    public static final int LEVEL_COUNTY = 2;    // 县级别标识

    // UI控件
    private ProgressDialog progressDialog;       // 加载进度对话框
    private TextView titleText;                 // 标题文本，显示当前级别
    private Button backButton;                   // 返回上一级按钮
    private ListView listView;                   // 显示地区列表的视图

    // 数据相关
    private ArrayAdapter<String> adapter;        // 列表适配器，用于绑定数据到ListView
    private List<String> dataList = new ArrayList<>(); // 存储显示在列表中的字符串数据

    // 地区数据列表
    private List<Province> provinceList;         // 省份数据列表
    private List<City> cityList;                // 城市数据列表
    private List<County> countyList;            // 县区数据列表

    // 当前选中项
    private Province selectedProvince;          // 当前选中的省份对象
    private City selectedCity;                 // 当前选中的城市对象
    private int currentLevel;                  // 当前所处的级别(省/市/县)

    /**
     * 创建Fragment的视图
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态
     * @return 创建的视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 将choose_area.xml布局文件实例化为View对象
        View view = inflater.inflate(R.layout.choose_area, container, false);

        // 初始化视图组件
        titleText = (TextView) view.findViewById(R.id.title_text);  // 标题文本视图
        backButton = (Button) view.findViewById(R.id.back_button);  // 返回按钮
        listView = (ListView) view.findViewById(R.id.list_view);    // 列表视图

        // 创建ArrayAdapter适配器，使用系统提供的简单列表项布局
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);  // 为ListView设置适配器

        return view;  // 返回创建的视图
    }

    /**
     * 当Activity创建完成后调用，初始化数据和事件
     * @param savedInstanceState 保存的状态
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 设置列表项点击事件监听器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * 列表项点击回调方法
             * @param parent 父AdapterView
             * @param view 被点击的视图
             * @param position 点击位置
             * @param id 行ID
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE) {
                    // 当前是省级，点击后进入市级选择
                    selectedProvince = provinceList.get(position);  // 保存选中的省份
                    queryCities();  // 查询该省下的城市
                } else if(currentLevel == LEVEL_CITY) {
                    // 当前是市级，点击后进入县级选择
                    selectedCity = cityList.get(position);  // 保存选中的城市
                    queryCounties();  // 查询该市下的县区
                }
            }
        });

        // 设置返回按钮点击事件监听器
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY) {
                    // 当前是县级，返回市级
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    // 当前是市级，返回省级
                    queryProvinces();
                }
            }
        });

        // 初始加载省级数据
        queryProvinces();
    }

    /**
     * 查询所有省份数据
     * 优先从本地数据库查询，如果不存在则从服务器获取
     */
    private void queryProvinces() {
        titleText.setText("中国");  // 设置标题为"中国"
        backButton.setVisibility(View.GONE);  // 省级没有上一级，隐藏返回按钮

        // 从LitePal数据库查询所有省份数据
        provinceList = LitePal.findAll(Province.class);

        if(provinceList.size() > 0) {
            // 数据库中有数据，直接显示
            dataList.clear();  // 清空现有数据
            for(Province province : provinceList) {
                dataList.add(province.getProvinceName());  // 添加省份名称到显示列表
            }
            adapter.notifyDataSetChanged();  // 通知适配器数据已改变
            listView.setSelection(0);       // 将列表滚动到顶部
            currentLevel = LEVEL_PROVINCE; // 设置当前级别为省级
        } else {
            // 数据库中无数据，从服务器获取
            String address = "http://guolin.tech/api/china";  // 省份数据API地址
            queryFromServer(address, "province");  // 从服务器查询省份数据
        }
    }

    /**
     * 查询当前选中省份下的城市数据
     * 优先从本地数据库查询，如果不存在则从服务器获取
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());  // 设置标题为省份名称
        backButton.setVisibility(View.VISIBLE);  // 显示返回按钮

        // 从LitePal数据库查询该省份下的城市数据
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);

        if(cityList.size() > 0) {
            // 数据库中有数据，直接显示
            dataList.clear();
            for(City city : cityList) {
                dataList.add(city.getCityName());  // 添加城市名称到显示列表
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;  // 设置当前级别为市级
        } else {
            // 数据库中无数据，从服务器获取
            int provinceCode = selectedProvince.getProvinceCode();  // 获取省份代码
            String address = "http://guolin.tech/api/china/" + provinceCode;  // 城市数据API地址
            queryFromServer(address, "city");  // 从服务器查询城市数据
        }
    }

    /**
     * 查询当前选中城市下的县区数据
     * 优先从本地数据库查询，如果不存在则从服务器获取
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());  // 设置标题为城市名称
        backButton.setVisibility(View.VISIBLE);  // 显示返回按钮

        // 从LitePal数据库查询该城市下的县区数据
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);

        if(countyList.size() > 0) {
            // 数据库中有数据，直接显示
            dataList.clear();
            for(County county : countyList) {
                dataList.add(county.getCountyName());  // 添加县区名称到显示列表
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;  // 设置当前级别为县级
        } else {
            // 数据库中无数据，从服务器获取
            int provinceCode = selectedProvince.getProvinceCode();  // 省份代码
            int cityCode = selectedCity.getCityCode();             // 城市代码
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;  // 县区数据API地址
            queryFromServer(address, "county");  // 从服务器查询县区数据
        }
    }

    /**
     * 从服务器查询地区数据
     * @param address 请求地址
     * @param type 数据类型(province/city/county)
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();  // 显示加载进度对话框

        // 使用HttpUtil发送HTTP请求
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            /**
             * 请求失败回调
             * @param call 调用对象
             * @param e 异常信息
             */
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 回到UI线程处理
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();  // 关闭进度对话框
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();  // 显示失败提示
                    }
                });
            }

            /**
             * 请求成功回调
             * @param call 调用对象
             * @param response 响应对象
             */
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();  // 获取响应体字符串
                boolean result = false;

                // 根据不同类型调用不同的数据处理方法
                if("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);  // 处理省份数据
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());  // 处理城市数据
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());  // 处理县区数据
                }

                if(result) {
                    // 数据处理成功，回到UI线程更新界面
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();  // 关闭进度对话框

                            // 根据数据类型重新查询并显示
                            if("province".equals(type)) {
                                queryProvinces();  // 重新查询省份
                            } else if ("city".equals(type)) {
                                queryCities();    // 重新查询城市
                            } else if ("county".equals(type)) {
                                queryCounties(); // 重新查询县区
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示加载进度对话框
     */
    private void showProgressDialog() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());  // 创建进度对话框
            progressDialog.setMessage("正在加载");               // 设置提示信息
            progressDialog.setCanceledOnTouchOutside(false);    // 设置点击外部不消失
        }
        progressDialog.show();  // 显示对话框
    }

    /**
     * 关闭加载进度对话框
     */
    private void closeProgressDialog() {
        if(progressDialog != null) {
            progressDialog.dismiss();  // 关闭对话框
        }
    }
}