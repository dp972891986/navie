package com.sc.android.navidemo.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.Tip;
import com.sc.android.navidemo.R;
import com.sc.android.navidemo.util.AMapUtil;
import com.sc.android.navidemo.util.Cantant;
import com.sc.android.navidemo.util.PositionEntity;
import com.sc.android.navidemo.util.RecomandAdapter;
import com.sc.android.navidemo.util.RouteTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 添加子地址界面
 * 使用地图poi搜索关键字
 */
public class AddressChildActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, TextWatcher {

    private EditText addChildEt; //地址输入框
    private TextView addChildTv; //取消操作
    private ListView addChildListView; //item点击事件

    private RecomandAdapter mRecomandAdapter; //显示搜索结果的适配器

    private List<HashMap<String, Object>> addCommList;
    public static AddressChildActivity instance;
    private SQLiteDatabase mSQLiteDataBase;
    private String addId;  //匹配作用的传值
    private RouteTask mRouteTask;
    private List<PositionEntity> mPositionEntities;
    private int resource = R.layout.view_recommond; // item布局文件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_child);
        instance = this;
        addChildListView = (ListView)findViewById(R.id.add_address_list);
        addChildListView.setOnItemClickListener(this);
        mRouteTask = RouteTask.getInstance(getApplicationContext());

        addChildEt = (EditText)findViewById(R.id.add_child_edit);
        addChildEt.addTextChangedListener(this);

        addChildTv = (TextView)findViewById(R.id.add_child_txt);
        addChildTv.setOnClickListener(this);

        Intent intent = getIntent();
        addId = intent.getStringExtra("addId");
    }

    //item 点击事件
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            PositionEntity entity = (PositionEntity) mRecomandAdapter.getItem(position);
            Intent intent = new Intent();
            Bundle b = new Bundle();
            b.putString("addressName", entity.getAddress());
            b.putDouble("longitude", entity.getLongitude());
            b.putDouble("latitude", entity.getLatitude());
            intent.putExtras(b);
            setResult(Cantant.ADDRESSID_MAP, intent);
            mRouteTask.setEndPoint(entity);
            mRouteTask.search();
            AddressChildActivity.this.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 输入框操作  根据输入的关键字进行搜索
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String newText = s.toString().trim();
        Inputtips inputTips = new Inputtips(AddressChildActivity.this,
                new Inputtips.InputtipsListener() {

                    @Override
                    public void onGetInputtips(List<Tip> tipList, int rCode) {
                        if (rCode == 0) {// 正确返回
                            mPositionEntities = new ArrayList<>();
                            for (int i = 0; i < tipList.size(); i++) {
                                PositionEntity p = new PositionEntity();
                                LatLng l;
                                p.setAddress(tipList.get(i).getName());  //地址
                                p.setDistrict(tipList.get(i).getDistrict()); //区
                                l = AMapUtil.convertToLatLng(tipList.get(i).getPoint());
                                p.setLatitude(l.latitude);
                                p.setLongitude(l.longitude);
                                mPositionEntities.add(p);
                            }
                            mRecomandAdapter = new RecomandAdapter(AddressChildActivity.this, resource, mPositionEntities);
                            addChildListView.setAdapter(mRecomandAdapter);
                            mRecomandAdapter.notifyDataSetChanged();
                        }
                    }
                });
        try {
            inputTips.requestInputtips(newText, "成都");// 第一个参数表示提示关键字，第二个参数默认代表全国，也可以为城市区号
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_child_txt:
                AddressChildActivity.this.finish();
                break;
        }
    }

    //手机返回键代码
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
