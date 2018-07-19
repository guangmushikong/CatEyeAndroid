package com.cateye.vtm.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cateye.android.vtm.MainActivity;
import com.cateye.android.vtm.R;
import com.cateye.vtm.fragment.base.BaseDrawFragment;
import com.cateye.vtm.fragment.base.BaseFragment;
import com.cateye.vtm.util.CatEyeMapManager;
import com.cateye.vtm.util.SystemConstant;
import com.vondear.rxtool.view.RxToast;

import org.greenrobot.eventbus.EventBus;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiaoxiao on 2018/3/21.
 */
//@Puppet
public class DrawPointLinePolygonFragment extends BaseDrawFragment {
    private TextView tv_last, tv_clear, tv_finish;
    protected MapEventsReceiver mapEventsReceiver;

    private int drawUsage = -1;

    @Override
    public int getFragmentLayoutId() {
        return R.layout.fragment_draw_point_line_polygon;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            //获取当前的绘制状态
            currentDrawState = (DRAW_STATE) savedInstanceState.getSerializable(DRAW_STATE.class.getSimpleName());
            drawUsage = savedInstanceState.getInt(SystemConstant.DRAW_USAGE);
        }
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            currentDrawState = (DRAW_STATE) bundle.getSerializable(DRAW_STATE.class.getSimpleName());
            drawUsage = bundle.getInt(SystemConstant.DRAW_USAGE);
        }
    }

    @Override
    public void onNewBundle(Bundle args) {
        super.onNewBundle(args);
        if (args != null) {
            //获取当前的绘制状态
            currentDrawState = (DRAW_STATE) args.getSerializable(DRAW_STATE.class.getSimpleName());
        }
    }

    @Override
    public void initView(View rootView) {
        tv_last = (TextView) rootView.findViewById(R.id.tv_draw_last);
        tv_clear = (TextView) rootView.findViewById(R.id.tv_draw_clear);
        tv_finish = (TextView) rootView.findViewById(R.id.tv_draw_finish);

        if (currentDrawState == DRAW_STATE.DRAW_POINT) {
            tv_last.setVisibility(View.GONE);
            tv_clear.setVisibility(View.GONE);
        }

        tv_last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDrawState != DRAW_STATE.DRAW_NONE) {
                    if (markerLayer.getItemList() == null || markerLayer.getItemList().isEmpty()) {
                        RxToast.info("没有需要清除的点!", Toast.LENGTH_SHORT);
                        return;
                    }
                    if (markerLayer.getItemList() != null && !markerLayer.getItemList().isEmpty()) {
                        markerLayer.removeItem(markerLayer.getItemList().size() - 1);
//                        markerLayer.map().updateMap(true);//重绘
                        markerLayer.update();
                    }
                    if (currentDrawState == DRAW_STATE.DRAW_LINE) {//绘制线
                        if (polylineOverlay.getPoints() != null && !polylineOverlay.getPoints().isEmpty()) {
                            polylineOverlay.getPoints().remove(polylineOverlay.getPoints().size() - 1);
                            redrawPolyline(polylineOverlay);
                        }
                    } else if (currentDrawState == DRAW_STATE.DRAW_POLYGON) {//绘制面
                        if (polygonOverlay.getPoints() != null && !polygonOverlay.getPoints().isEmpty()) {
                            polygonOverlay.getPoints().remove(polygonOverlay.getPoints().size() - 1);
                            redrawPolygon(polygonOverlay);
                        }
                    }
                }
            }
        });

        tv_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (markerLayer.getItemList() == null || markerLayer.getItemList().isEmpty()) {
                    RxToast.info("没有需要清除的点!", Toast.LENGTH_SHORT);
                    return;
                }
                if (markerLayer.getItemList() != null && !markerLayer.getItemList().isEmpty()) {
                    markerLayer.removeAllItems();
                    markerLayer.map().updateMap(true);//重绘
                }
                if (currentDrawState == DRAW_STATE.DRAW_LINE) {
                    polylineOverlay.getPoints().clear();
                    redrawPolyline(polylineOverlay);
                } else if (currentDrawState == DRAW_STATE.DRAW_POLYGON) {
                    polygonOverlay.getPoints().clear();
                    redrawPolygon(polygonOverlay);
                }

            }
        });

        tv_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle drawBundle = new Bundle();
                List<GeoPoint> geoPointList = new ArrayList<>();
                if (markerLayer != null && markerLayer.getItemList() != null && !markerLayer.getItemList().isEmpty()) {
                    for (MarkerItem item : markerLayer.getItemList()) {
                        geoPointList.add(item.geoPoint);
                    }
                    drawBundle.putSerializable(SystemConstant.DRAW_POINT_LIST, (Serializable) geoPointList);
                }
                Message msg = Message.obtain();
                msg.what = SystemConstant.MSG_WHAT_DRAW_RESULT;
                msg.obj = geoPointList;
                msg.arg1 = drawUsage;
                EventBus.getDefault().post(msg);
                pop();//退出当前界面
            }
        });

        //添加一个操作图层，监听用户在地图上的点击事件
        mapEventsReceiver = new MapEventsReceiver(CatEyeMapManager.getInstance(getActivity()).getCatEyeMap());
        CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().add(mapEventsReceiver, MainActivity.LAYER_GROUP_ENUM.OPERTOR_GROUP.orderIndex);
    }

    /**
     * @param :
     * @return :
     * @method :
     * @Author : xiaoxiao
     * @Describe :
     * @Date : 2018/5/28
     */


    public static BaseFragment newInstance(Bundle bundle) {
        DrawPointLinePolygonFragment drawPointLinePolygonFragment = new DrawPointLinePolygonFragment();
        drawPointLinePolygonFragment.setArguments(bundle);
        return drawPointLinePolygonFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //通知主界面隐藏部分重新显示
        setMainFragmentAreaVisible(CatEyeMainFragment.BUTTON_AREA.ALL, false);
    }

    @Override
    public void onStop() {
        super.onStop();

        //当前界面被返回时，移除绘制的图层
        if (mapEventsReceiver != null) {
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(mapEventsReceiver);
        }
        //如果点位的layer没有数据，则移除
        if (markerLayer != null && markerLayer.size() <= 0) {
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(markerLayer);
        }
        //如果线的layer没有数据，则移除
        if (polylineOverlay != null && polylineOverlay.getPoints() != null && polylineOverlay.getPoints().size()<2) {
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(polylineOverlay);
            if (currentDrawState==DRAW_STATE.DRAW_LINE){
                CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(markerLayer);
            }
        }
        //如果面的layer没有数据，则移除
        if (polygonOverlay != null && polygonOverlay.getPoints() != null && polygonOverlay.getPoints().size() < 3) {
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(polygonOverlay);
            if (currentDrawState==DRAW_STATE.DRAW_POLYGON){
                CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(markerLayer);
            }
        }
        //判断绘制的用途，某些用途下，绘制结束后就不需要再显示，也需要移除掉layer
        if (drawUsage == SystemConstant.DRAW_CONTOUR_LINE) {
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(markerLayer);
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(polylineOverlay);
            CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().layers().remove(polygonOverlay);
        }
        CatEyeMapManager.getInstance(getActivity()).getCatEyeMap().updateMap(true);

        //通知主界面绘制点线面结束
        Message msg = new Message();
        msg.what = SystemConstant.MSG_WHAT_DRAW_POINT_LINE_POLYGON_DESTROY;
        EventBus.getDefault().post(msg);

        //通知主界面隐藏部分重新显示
        setMainFragmentAreaVisible(CatEyeMainFragment.BUTTON_AREA.ALL, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                DRAW_STATE currentState = getCurrentDrawState();

                if (currentState != DRAW_STATE.DRAW_NONE) {//如果当前是绘制模式，则自动添加marker
                    markerLayer.addItem(new MarkerItem("", "", p));
                    markerLayer.update();
                    //如果当前是绘制线模式，则增加pathLayer
                    if (currentState == DRAW_STATE.DRAW_LINE) {
                        polylineOverlay.addPoint(p);
                        redrawPolyline(polylineOverlay);
                    }
                    if (currentState == DRAW_STATE.DRAW_POLYGON) {
                        polygonOverlay.addPoint(p);
                        redrawPolygon(polygonOverlay);
                    }
                    markerLayer.map().updateMap(true);
                    return true;
                }
            }
            return false;
        }
    }
}
