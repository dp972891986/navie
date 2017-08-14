package com.sc.android.navidemo.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sc.android.navidemo.R;

import java.util.List;

/**
 * Created by Administrator on 2016/1/22.
 * ClassName:RecomandAdapter <br/>
 * Function: 显示的poi列表 <br/>
 * Date: 2015年4月3日 上午11:29:45 <br/>
 * @author yiyi.qi
 * @version
 * @since JDK 1.6
 * @see
 */
public class RecomandAdapter extends BaseAdapter {

    private Context context;
    private int resource;
    private List<PositionEntity> mPositionEntities;
    public RecomandAdapter(Context context, int resource, List<PositionEntity> mPositionEntities) {
        this.context = context;
        this.resource = resource;
        this.mPositionEntities = mPositionEntities;
    }
    @Override
    public int getCount() {
        return mPositionEntities.size();
    }
    @Override
    public Object getItem(int position) {
        return mPositionEntities.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView=View.inflate(context, resource,null);
            holder=new ViewHolder();
            holder.addressBig = (TextView) convertView.findViewById(R.id.addressBig);
            holder.addressSmall = (TextView) convertView.findViewById(R.id.addressSmall);
            convertView.setTag(holder);
        } else {
            holder=(ViewHolder) convertView.getTag();
        }
        holder.addressBig.setText(mPositionEntities.get(position).getAddress());
        holder.addressSmall.setText(mPositionEntities.get(position).getDistrict());

        return convertView;
    }
    class ViewHolder{
        TextView addressBig;
        TextView addressSmall;
    }
}
