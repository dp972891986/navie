package com.sc.android.navidemo.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.enums.PathPlanningStrategy;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.sc.android.navidemo.R;
import com.sc.android.navidemo.util.Cantant;
import com.sc.android.navidemo.util.CarLatLngBean;
import com.sc.android.navidemo.util.DrivingRouteOverlay;
import com.sc.android.navidemo.util.TTSController;
import com.sc.android.navidemo.util.ToastUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationSource, AMapLocationListener, OnClickListener,
        RouteSearch.OnRouteSearchListener, AMap.OnCameraChangeListener, AMapNaviListener {
    //基础地图声明
    private MapView                                  mapView;
    private AMap                                     aMap;
    //定位相关
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient                       mLocationClient;
    private AMapLocationClientOption                 mLocationOption;
    private double                                   mLatitude, mLongitude;//定位点经纬度坐标值
    private MyLocationStyle myLocationStyle;//自定义定位点

    private DriveRouteResult driveResult;// 驾车模式查询结果
    private RouteSearch      routeSearch;
    private int drivingMode = RouteSearch.DrivingDefault;// 驾车默认模式

    //路径导航
    private AMapNavi      aMapNavi;
    private RouteOverLay  mRouteOverLay;
    private TTSController mTtsManager;//语音
    // 起点终点列表
    private ArrayList<NaviLatLng> mStartPoints = new ArrayList<>();
    private ArrayList<NaviLatLng> mEndPoints   = new ArrayList<>();
    private Polyline mVirtureRoad;
    private Marker   mMoveMarker;

    public static MainActivity instance;

    //接乘客时的起止点坐标
    private LatLonPoint startPoint = null;
    private LatLonPoint endPoint   = null;

    //控件声明
    private TextView mStartTxt, mEndTxt;
    private Button mNaviBtn, mCallNaviBtn;

    // 通过设置间隔时间和距离可以控制速度和图标移动的距离
    private static final int                      TIME_INTERVAL = 80;
    private static final double                   DISTANCE      = 0.0001;
    private              ArrayList<CarLatLngBean> list          = new ArrayList<>();

    private float speed;
    private int   sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //为了尽最大可能避免内存泄露问题，建议这么写
        mTtsManager = TTSController.getInstance(getApplicationContext());
        mTtsManager.init();
        mTtsManager.startSpeaking();

        //为了尽最大可能避免内存泄露问题，建议这么写
        aMapNavi = AMapNavi.getInstance(getApplicationContext());

        aMapNavi.addAMapNaviListener(this);
        aMapNavi.addAMapNaviListener(mTtsManager);
        aMapNavi.setEmulatorNaviSpeed(sp);
        aMapNavi.isGpsReady();
        instance = this;
        init(savedInstanceState);
    }

    //实例化控件并设置事件监听
    private void init(Bundle savedInstanceState) {
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        mRouteOverLay = new RouteOverLay(aMap, null);
        routeSearch = new RouteSearch(this);
        routeSearch.setRouteSearchListener(this);
        mStartTxt = (TextView) findViewById(R.id.start_address_txt);
        mEndTxt = (TextView) findViewById(R.id.end_address_txt);
        mEndTxt.setOnClickListener(this);
        mNaviBtn = (Button) findViewById(R.id.navi_road_btn);
        mNaviBtn.setOnClickListener(this);
        mCallNaviBtn = (Button) findViewById(R.id.call_navi_btn);
        mCallNaviBtn.setOnClickListener(this);
    }

    //设置地图的一些基本属性
    private void setUpMap() {
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ring));
        myLocationStyle.strokeColor(Color.BLACK);
        myLocationStyle.strokeWidth(0);
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色  
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        // 设置为true表示系统定位按钮显示并响应点击，false表示隐藏，默认是false
        aMap.setLocationSource(this);// 设置定位监听
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setOnCameraChangeListener(this);
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        initGPS();
        isConn(this);
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (null != mLocationClient) {
            mLocationClient.onDestroy();
        }
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                mLatitude = amapLocation.getLatitude();
                mLongitude = amapLocation.getLongitude();
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(mLatitude, mLongitude)));
                aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
                startPoint = new LatLonPoint(mLatitude, mLongitude);
                mStartTxt.setText(amapLocation.getPoiName());
                speed = amapLocation.getSpeed();
                sp = (int) amapLocation.getSpeed();
                Log.e("????????????????", "asihdfoiash" + sp);
            }
        } else {
            String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
            Log.e("AmapErr", errText);
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setInterval(5000);// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms）

            //            // 设置是否允许模拟位置,默认为false，不允许模拟位置
            //            mLocationOption.setMockEnable(true);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mLocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.end_address_txt:
                Intent intentEnd = new Intent(this, AddressChildActivity.class);
                startActivityForResult(intentEnd, 1);
                break;
            case R.id.navi_road_btn:
                //                initGPS();
                searchRouteResult(startPoint, endPoint);
                //                turnGPSOn();
                break;
            case R.id.call_navi_btn:
                Intent intent =    new Intent(MainActivity.this, NavigationInterfaceActivity.class);
                Bundle bundle = new Bundle();
                bundle.putDouble("startLatitude", startPoint.getLatitude());
                bundle.putDouble("startLongitude", startPoint.getLongitude());
                bundle.putDouble("endLatitude", endPoint.getLatitude());
                bundle.putDouble("endLongitude", endPoint.getLongitude());
                bundle.putFloat("speed", speed);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
        }
    }

    /**
     * 判断GPS是否打开，若开启状态则不做任何操作
     * 若未开启状态则弹出对话框，点击取消下次继续弹出，点击确定则跳转到GPS控制界面，返回后继续后继操作
     */
    private void initGPS() {
        LocationManager locationManager =  (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(MainActivity.this, "请打开GPS", Toast.LENGTH_SHORT).show();
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("请打开GPS连接");
            dialog.setMessage("为方便司机更容易接到您，请先打开GPS");
            dialog.setPositiveButton("设置", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // 转到手机设置界面，用户设置GPS
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    Toast.makeText(MainActivity.this, "打开后直接点击返回键即可，若不打开返回下次将再次出现", Toast.LENGTH_SHORT).show();
                    startActivityForResult(intent, 0); // 设置完成后返回到原来的界面
                }
            });
            dialog.setNeutralButton("取消", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });
            dialog.show();
        } else {
            searchRouteResult(startPoint, endPoint);//路径规划
            // 弹出Toast
            //          Toast.makeText(TrainDetailsActivity.this, "GPS is ready",Toast.LENGTH_LONG).show();
            //          // 弹出对话框
            //          new AlertDialog.Builder(this).setMessage("GPS is ready").setPositiveButton("OK", null).show();
        }
    }

    /**
     * 判断网络连接是否已开
     * true 已打开  false 未打开
     */
    public static boolean isConn(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
            searchNetwork(context);//弹出提示对话框
        }
        return false;
    }

    /**
     * 判断网络是否连接成功，连接成功不做任何操作
     * 未连接则弹出对话框提示用户设置网络连接
     */
    public static void searchNetwork(final Context context) {
        //提示对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("网络设置提示").setMessage("网络连接不可用,是否进行设置?").setPositiveButton("设置", new DialogInterface
                .OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = null;
                //判断手机系统的版本  即API大于10 就是3.0或以上版本
                if (android.os.Build.VERSION.SDK_INT > 10) {
                    intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                } else {
                    intent = new Intent();
                    ComponentName component = new ComponentName("com.android.settings", "com.android.settings" +
                            ".WirelessSettings");
                    intent.setComponent(component);
                    intent.setAction("android.intent.action.VIEW");
                }
                context.startActivity(intent);
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    /**
     * 强制开启GPS方法，此方法在4.0及以上版本都不能使用
     */
    public void turnGPSOn() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.sendBroadcast(intent);
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.contains("gps")) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.sendBroadcast(poke);
        }
    }

    /**
     * 接受 选择地址 的值
     */
    private double longitude; //终点纬度
    private double latitude;  //终点经度

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Cantant.ADDRESSID_MAP) {
            Bundle b = data.getExtras();
            String address = b.getString("addressName");
            longitude = b.getDouble("longitude");
            latitude = b.getDouble("latitude");
            Toast.makeText(this, address + "(" + longitude + "," + latitude + ")", Toast.LENGTH_SHORT).show();
            endPoint = new LatLonPoint(latitude, longitude);
            mEndTxt.setText(address);
        } else if (resultCode == 0) {
            //            Toast.makeText(MainActivity.this, "请重新开始导航", Toast.LENGTH_SHORT).show();
            //            searchRouteResult(startPoint, endPoint);
            //            initGPS();
        }
    }

    /**
     * 开始搜索路径规划
     */
    public void searchRouteResult(LatLonPoint startPoint, LatLonPoint endPoint) {
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, drivingMode, null, null, "");
        // 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        routeSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
    }

    //公交路径规划
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    /**
     * 驾车路径规划回调接口
     */
    private NaviLatLng startLL;
    private NaviLatLng endLL;

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (i == 0) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null && driveRouteResult.getPaths().size()
                    > 0) {
                driveResult = driveRouteResult;
                DrivePath drivePath = driveRouteResult.getPaths().get(0);

                startLL = new NaviLatLng(driveRouteResult.getStartPos().getLatitude(), driveRouteResult.getStartPos()
                        .getLongitude());
                endLL = new NaviLatLng(driveRouteResult.getTargetPos().getLatitude(), driveRouteResult.getTargetPos()
                        .getLongitude());
                mStartPoints.add(startLL);
                mEndPoints.add(endLL);
                boolean isSuccess = aMapNavi.calculateDriveRoute(mStartPoints, mEndPoints, null, PathPlanningStrategy
                        .DRIVING_DEFAULT);
                if (!isSuccess) {
                    Toast.makeText(this, "路线计算失败,检查参数情况", Toast.LENGTH_SHORT).show();
                }
                DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(this, aMap, drivePath,
                        driveRouteResult.getStartPos(), driveRouteResult.getTargetPos());
                drivingRouteOverlay.removeFromMap();
                drivingRouteOverlay.addToMap();
                drivingRouteOverlay.zoomToSpan();
            } else {
                ToastUtil.show(this, R.string.no_result);
            }
        } else if (i == 27) {
            ToastUtil.show(this, R.string.error_network);
        } else if (i == 32) {
            ToastUtil.show(this, R.string.error_key);
        } else {
            ToastUtil.show(this, getString(R.string.error_other) + i);
        }
    }

    //步行路径规划
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    //地图移动事件处理
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    //地图移动完成事件处理
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {

    }

    /**
     * 模拟语音导航路径行驶
     */
    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {
        aMapNavi.calculateDriveRoute(mStartPoints, mEndPoints, null, PathPlanningStrategy.DRIVING_DEFAULT);
    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onCalculateRouteSuccess() {
        //        mRouteOverLay.removeFromMap();
        AMapNaviPath naviPath = aMapNavi.getNaviPath();
        aMapNavi.startNavi(1);
        aMapNavi.startGPS();
        if (naviPath == null) {
            return;
        }
        // 获取路径规划线路，显示到地图上
        //        mRouteOverLay.setAMapNaviPath(naviPath);
        //        mRouteOverLay.addToMap();
        mRouteOverLay.zoomToSpan();
        mRouteOverLay.setEmulateGPSLocationVisible();
    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onReCalculateRouteForYaw() {
        aMap.clear();
        searchRouteResult(startPoint, endPoint);
        aMapNavi.calculateDriveRoute(mStartPoints, mEndPoints, null, PathPlanningStrategy.DRIVING_DEFAULT);
    }

    @Override
    public void onReCalculateRouteForTrafficJam() {
        aMap.clear();
        searchRouteResult(startPoint, endPoint);
        aMapNavi.calculateDriveRoute(mStartPoints, mEndPoints, null, PathPlanningStrategy.DRIVING_DEFAULT);
    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {
        b = true;
    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {
        Toast.makeText(this, "现在的速度是：" + sp, Toast.LENGTH_SHORT).show();
        CarLatLngBean bean = new CarLatLngBean();
        bean.setLatitude(naviInfo.getCoord().getLatitude());
        bean.setLongitude(naviInfo.getCoord().getLongitude());
        list.add(bean);
        initRoadData(list);
        if (list.size() > 2) {
            moveLooper();
        } else {
            return;
        }
    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {
        Toast.makeText(this, "导航计算失败！" + aMapLaneInfos, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    public void initRoadData(ArrayList<CarLatLngBean> list) {
        PolylineOptions polylineOptions = new PolylineOptions();
        if (list.size() > 2) {
            int counts = list.size();
            for (int i = 0; i < counts; i++) {
                polylineOptions.add(new LatLng(list.get(i).getLatitude(), list.get(i).getLongitude()));
            }
            polylineOptions.width(0);
            polylineOptions.color(Color.RED);
            mVirtureRoad = aMap.addPolyline(polylineOptions);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.setFlat(true);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi_bearing));
            markerOptions.position(polylineOptions.getPoints().get(0));
            if (mMoveMarker != null)
                mMoveMarker.remove();
            list.clear();
            mMoveMarker = aMap.addMarker(markerOptions);
            mMoveMarker.showInfoWindow();
            mMoveMarker.setRotateAngle((float) getAngle(0));
        } else {
            return;
        }
    }

    /**
     * 根据点获取图标转的角度
     */
    private double getAngle(int startIndex) {
        if ((startIndex + 1) >= mVirtureRoad.getPoints().size()) {
            throw new RuntimeException("index out of bonds");
        }
        LatLng startPoint = mVirtureRoad.getPoints().get(startIndex);
        LatLng endPoint = mVirtureRoad.getPoints().get(startIndex + 1);
        return getAngle(startPoint, endPoint);
    }

    /**
     * 根据两点算取图标转的角度
     */
    private double getAngle(LatLng fromPoint, LatLng toPoint) {
        double slope = getSlope(fromPoint, toPoint);
        if (slope == Double.MAX_VALUE) {
            if (toPoint.latitude > fromPoint.latitude) {
                return 0;
            } else {
                return 180;
            }
        }
        float deltAngle = 0;
        if ((toPoint.latitude - fromPoint.latitude) * slope < 0) {
            deltAngle = 180;
        }
        double radio = Math.atan(slope);
        double angle = 180 * (radio / Math.PI) + deltAngle - 90;
        return angle;
    }

    /**
     * 根据点和斜率算取截距
     */
    private double getInterception(double slope, LatLng point) {

        double interception = point.latitude - slope * point.longitude;
        return interception;
    }

    /**
     * 算取斜率
     */
    private double getSlope(int startIndex) {
        if ((startIndex + 1) >= mVirtureRoad.getPoints().size()) {
            throw new RuntimeException("index out of bonds");
        }
        LatLng startPoint = mVirtureRoad.getPoints().get(startIndex);
        LatLng endPoint = mVirtureRoad.getPoints().get(startIndex + 1);
        return getSlope(startPoint, endPoint);
    }

    /**
     * 算斜率
     */
    private double getSlope(LatLng fromPoint, LatLng toPoint) {
        if (toPoint.longitude == fromPoint.longitude) {
            return Double.MAX_VALUE;
        }
        double slope = ((toPoint.latitude - fromPoint.latitude) / (toPoint.longitude - fromPoint.longitude));
        return slope;

    }

    /**
     * 计算x方向每次移动的距离
     */
    private double getXMoveDistance(double slope) {
        if (slope == Double.MAX_VALUE) {
            return DISTANCE;
        }
        return Math.abs((DISTANCE * slope) / Math.sqrt(1 + slope * slope));
    }

    /**
     * 进行移动逻辑
     */
    public void moveLooper() {
        new Thread() {
            public void run() {
                for (int i = 0; i < mVirtureRoad.getPoints().size() - 1; i++) {
                    LatLng startPoint = mVirtureRoad.getPoints().get(i);
                    LatLng endPoint = mVirtureRoad.getPoints().get(i + 1);
                    mMoveMarker.setPosition(startPoint);
                    mMoveMarker.setRotateAngle((float) getAngle(startPoint, endPoint));
                    double slope = getSlope(startPoint, endPoint);
                    //是不是正向的标示（向上设为正向）
                    boolean isReverse = (startPoint.latitude > endPoint.latitude);

                    double intercept = getInterception(slope, startPoint);
                    double xMoveDistance = isReverse ? getXMoveDistance(slope) : -1 * getXMoveDistance(slope);
                    for (double j = startPoint.latitude; !((j > endPoint.latitude) ^ isReverse); j = j -
                            xMoveDistance) {
                        LatLng latLng = null;
                        if (slope != Double.MAX_VALUE) {
                            latLng = new LatLng(j, (j - intercept) / slope);
                        } else {
                            latLng = new LatLng(j, startPoint.longitude);
                        }
                        mMoveMarker.setPosition(latLng);
                        try {
                            Thread.sleep(TIME_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }
}
