package com.cateye.vtm.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONReader;
import com.canyinghao.candialog.CanDialog;
import com.canyinghao.candialog.CanDialogInterface;
import com.cateye.android.entity.ContourFromNet;
import com.cateye.android.entity.ContourMPData;
import com.cateye.android.entity.MapSourceFromNet;
import com.cateye.android.vtm.MainActivity;
import com.cateye.android.vtm.MainActivity.LAYER_GROUP_ENUM;
import com.cateye.android.vtm.R;
import com.cateye.vtm.adapter.LayerManagerAdapter;
import com.cateye.vtm.fragment.base.BaseFragment;
import com.cateye.vtm.util.CatEyeMapManager;
import com.cateye.vtm.util.SystemConstant;
import com.github.lazylibrary.util.StringUtils;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.convert.StringConvert;
import com.lzy.okgo.model.Response;
import com.lzy.okrx2.adapter.ObservableResponse;
import com.tencent.map.geolocation.TencentLocation;
import com.vondear.rxtool.RxLogTool;
import com.vondear.rxtool.view.RxToast;
import com.vondear.rxui.view.dialog.RxDialog;
import com.vondear.rxui.view.dialog.RxDialogLoading;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jeo.carto.Carto;
import org.jeo.map.Style;
import org.jeo.vector.VectorDataset;
import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
import org.oscim.android.filepicker.FilePicker;
import org.oscim.android.theme.AssetsRenderTheme;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapElement;
import org.oscim.core.MapPosition;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.ContourLineLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.LocationLayer;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.MapEventLayer2;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.scalebar.CatEyeMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.test.JeoTest;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.ThemeUtils;
import org.oscim.theme.VtmThemes;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.oscim.tiling.source.geojson.ContourGeojsonTileSource;
import org.oscim.tiling.source.geojson.GeojsonTileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MapInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.cateye.vtm.util.SystemConstant.URL_CONTOUR_CALCULATE;
import static com.cateye.vtm.util.SystemConstant.URL_MAP_SOURCE_NET;

/**
 * Created by zhangdezhi1702 on 2018/3/15.
 */
//@Puppet(containerViewId = R.id.layer_main_cateye_bottom)
public class CatEyeMainFragment extends BaseFragment {
    private MapView mapView;//地图控件
    private Map mMap;
    private MapPreferences mPrefs;

    static final int SELECT_MAP_FILE = 0;
    static final int SELECT_THEME_FILE = SELECT_MAP_FILE + 1;
    static final int SELECT_GEOJSON_FILE = SELECT_MAP_FILE + 2;
    static final int SELECT_CONTOUR_FILE = SELECT_MAP_FILE + 3;

    private static final Tag ISSEA_TAG = new Tag("natural", "issea");
    private static final Tag NOSEA_TAG = new Tag("natural", "nosea");
    private static final Tag SEA_TAG = new Tag("natural", "sea");
    private static final Tag CONTOUR_TAG = new Tag("contour", "1000");//等高线

//    private List<TileSource> mTileSourceList;//当前正在显示的tileSource的集合


    private ImageView chk_draw_point, chk_draw_line, chk_draw_polygon;//绘制点线面
    private ImageView img_location;//获取当前位置的按钮
    private ImageView img_map_source_selector;
    private ImageView img_contour_selector;//加载等高线数据的按钮
    private ImageView img_change_contour_color;//修改等高线地图显示颜色的按钮
    private ImageView img_select_project;//选择当前项目的按钮
    private List<ImageView> chkDrawPointLinePolygonList;
    private FrameLayout layer_fragment;//用来显示fragment的布局文件
//    private java.util.Map<String, MapSourceFromNet.DataBean> netDataSourceMap;//用来记录用户勾选了哪些网络数据显示

    private LocationLayer locationLayer;//显示当前位置的图层
    private final MapPosition mapPosition = new MapPosition();//更新地图位置
    private boolean isMapCenterFollowLocation = true;//地图中心是否需要跟随当前定位位置

    private List<MapSourceFromNet.DataBean> layerDataBeanList;//记录图层管理中的图层信息
    private View layerManagerRootView;//图层管理对话框的根视图
    private LayerManagerAdapter layerManagerAdapter;//图层管理对应的adapter
    private List<MapSourceFromNet.DataBean> multiTimeLayerList;//记录拥有多个时序图层的list，如果存在，则需要提供切换时序的控件

    @Override
    public int getFragmentLayoutId() {
        return R.layout.fragment_main_cateye;
    }

    @Override
    public void initView(View rootView) {
        mapView = (MapView) rootView.findViewById(R.id.mapView);

        mMap = mapView.map();

        layer_fragment = (FrameLayout) rootView.findViewById(R.id.layer_main_cateye_bottom);

        chk_draw_point = (ImageView) rootView.findViewById(R.id.chk_draw_vector_point);
        chk_draw_line = (ImageView) rootView.findViewById(R.id.chk_draw_vector_line);
        chk_draw_polygon = (ImageView) rootView.findViewById(R.id.chk_draw_vector_polygon);
        img_select_project = rootView.findViewById(R.id.img_project);
        chkDrawPointLinePolygonList = new ArrayList<>();
        chkDrawPointLinePolygonList.add(chk_draw_point);
        chkDrawPointLinePolygonList.add(chk_draw_line);
        chkDrawPointLinePolygonList.add(chk_draw_polygon);
        multiTimeLayerList = new ArrayList<>();

        //选择地图资源
        img_map_source_selector = (ImageView) rootView.findViewById(R.id.img_map_source_select);
        img_contour_selector = (ImageView) rootView.findViewById(R.id.img_contour_select);
        img_change_contour_color = (ImageView) rootView.findViewById(R.id.img_change_contour_color);
        img_location = (ImageView) rootView.findViewById(R.id.img_location);

        initData();
        initScaleBar();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        //选择当前操作项目的按钮
        img_select_project.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).setCurrentProject();//弹出选择当前项目的对话框
            }
        });

        img_change_contour_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder colorDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View dialogview = inflater.inflate(R.layout.color_picker, null);
                final ColorPicker picker = (ColorPicker) dialogview.findViewById(R.id.color_picker);
                SVBar svBar = (SVBar) dialogview.findViewById(R.id.color_svbar);
                OpacityBar opacityBar = (OpacityBar) dialogview.findViewById(R.id.color_opacitybar);
                picker.addSVBar(svBar);
                picker.addOpacityBar(opacityBar);
                colorDialogBuilder.setTitle("选择等高线的显示颜色");
                colorDialogBuilder.setView(dialogview);
                colorDialogBuilder.setPositiveButton(R.string.confirmStr,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //设置等高线的显示颜色
                                if (mMap.layers() != null && !mMap.layers().isEmpty()) {
                                    for (Layer layer : mMap.layers()) {
                                        if (layer.isEnabled() && layer instanceof VectorTileLayer)
                                            ((VectorTileLayer) layer).addHook(new VectorTileLayer.TileLoaderThemeHook() {
                                                @Override
                                                public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
                                                    if (element.tags.containsKey("contour") || element.tags.containsKey("CONTOUR")) {
                                                        if (style instanceof LineStyle) {
//                                                            ((LineStyle)style).color=
                                                        }
                                                    }
                                                    return false;
                                                }

                                                @Override
                                                public void complete(MapTile tile, boolean success) {
                                                }
                                            });
                                    }
                                }
                            }
                        });
                colorDialogBuilder.setNegativeButton(R.string.cancelStr,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog colorPickerDialog = colorDialogBuilder.create();
                colorPickerDialog.show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    public static CatEyeMainFragment newInstance(Bundle bundle) {
        CatEyeMainFragment catEyeMainFragment = new CatEyeMainFragment();
        catEyeMainFragment.setArguments(bundle);
        return catEyeMainFragment;
    }

    //初始化数据
    private void initData() {
//        netDataSourceMap = new LinkedHashMap<String, MapSourceFromNet.DataBean>();
        //初始化MapManager，方便全局使用map对象
        CatEyeMapManager.getInstance(getActivity()).init(mapView);
        mPrefs = new MapPreferences(this.getTag(), getActivity());
//        mTileSourceList = new ArrayList<>();

        //向地图中添加地图图层分组
        for (LAYER_GROUP_ENUM group_enum : LAYER_GROUP_ENUM.values()) {
            mMap.layers().addGroup(group_enum.orderIndex);
        }

        chk_draw_point.setOnClickListener(mainFragmentClickListener);
        chk_draw_line.setOnClickListener(mainFragmentClickListener);
        chk_draw_polygon.setOnClickListener(mainFragmentClickListener);

        img_map_source_selector.setOnClickListener(mainFragmentClickListener);
        img_contour_selector.setOnClickListener(mainFragmentClickListener);//选择等高线文件并显示

        locationLayer = new LocationLayer(mMap);
        locationLayer.locationRenderer.setShader("location_1_reverse");
        locationLayer.setEnabled(false);
        mMap.layers().add(locationLayer, LAYER_GROUP_ENUM.LOCATION_GROUP.orderIndex);

        img_location.setOnClickListener(new View.OnClickListener() {//定位到当前位置
            @Override
            public void onClick(View view) {
                TencentLocation location = ((MainActivity) getActivity()).getCurrentLocation();
                if (location != null) {//有位置信息，或至少曾经定位过
                    mMap.getMapPosition(mapPosition);
                    mapPosition.setPosition(location.getLatitude(), location.getLongitude());
                    mMap.animator().animateTo(mapPosition);
                    isMapCenterFollowLocation = false;
                } else {
                    RxToast.info("无法获取到定位信息!");
                }
            }
        });
    }

    View.OnClickListener mainFragmentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.chk_draw_vector_point) {//开始绘制点
                //判断是否被添加进Reggier
                setDrawPointLinePolygonButtonState(view, chkDrawPointLinePolygonList);
                if (view.isSelected()) {//选中
                    //自动弹出绘制点线面的fragment
                    DrawPointLinePolygonFragment fragment = findFragment(DrawPointLinePolygonFragment.class);
                    Bundle pointBundle = new Bundle();
                    pointBundle.putSerializable(com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.class.getSimpleName(), com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.DRAW_POINT);
                    if (fragment != null) {
                        fragment.setArguments(pointBundle);
                        start(fragment);
                    } else {
                        loadRootFragment(R.id.layer_main_cateye_bottom, com.cateye.vtm.fragment.DrawPointLinePolygonFragment.newInstance(pointBundle));
                    }
                } else {//不选中
                    popChild();
                }
            } else if (view.getId() == R.id.chk_draw_vector_line) {//开始绘制线
                //判断是否被添加进Reggier
                setDrawPointLinePolygonButtonState(view, chkDrawPointLinePolygonList);
                if (view.isSelected()) {//选中
                    //自动弹出绘制点线面的fragment
                    DrawPointLinePolygonFragment fragment = findFragment(DrawPointLinePolygonFragment.class);
                    //自动弹出绘制点线面的fragment
                    Bundle lineBundle = new Bundle();
                    lineBundle.putSerializable(com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.class.getSimpleName(), com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.DRAW_LINE);
                    if (fragment != null) {
                        fragment.setArguments(lineBundle);
                        start(fragment);
                    } else {
                        loadRootFragment(R.id.layer_main_cateye_bottom, com.cateye.vtm.fragment.DrawPointLinePolygonFragment.newInstance(lineBundle));
                    }
                } else {//不选中
                    popChild();
                }
            } else if (view.getId() == R.id.chk_draw_vector_polygon) {//开始绘制面
                //判断是否被添加进Reggier
                setDrawPointLinePolygonButtonState(view, chkDrawPointLinePolygonList);
                if (view.isSelected()) {//选中
                    //自动弹出绘制点线面的fragment
                    DrawPointLinePolygonFragment fragment = findFragment(DrawPointLinePolygonFragment.class);
                    //自动弹出绘制点线面的fragment
                    Bundle polygonBundle = new Bundle();
                    polygonBundle.putSerializable(com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.class.getSimpleName(), com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.DRAW_POLYGON);
                    if (fragment != null) {
                        fragment.setArguments(polygonBundle);
                        start(fragment);
                    } else {
                        loadRootFragment(R.id.layer_main_cateye_bottom, com.cateye.vtm.fragment.DrawPointLinePolygonFragment.newInstance(polygonBundle));
                    }
                } else {//不选中
                    popChild();
                }
            } else if (view.getId() == R.id.img_map_source_select) {//选择地图资源
                if (layerDataBeanList != null&&!layerDataBeanList.isEmpty()) {
                    showLayerManagerDialog(layerDataBeanList);
                } else {
                    if (SystemConstant.CURRENT_PROJECTS_ID < 0) {//没有获取到当前作业的项目ID，提示用户
                        RxToast.info("无法获取当前作业项目，请检查您的网络设置");
                    } else {
                        getMapDataSourceFromNet();
                    }
                }
            } else if (view.getId() == R.id.img_contour_select) {//选择等高线文件
                final RxDialog dialog = new RxDialog(getContext());
                View layer_select_map_source = LayoutInflater.from(getContext()).inflate(R.layout.layer_select_contour_source, null);
                dialog.setContentView(layer_select_map_source);
                dialog.setCancelable(true);
                dialog.show();
                //本地等高线资源
                layer_select_map_source.findViewById(R.id.tv_map_contour_local).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivityForResult(new Intent(getActivity(), MainActivity.ContourFilePicker.class),
                                SELECT_CONTOUR_FILE);
                        dialog.dismiss();
                    }
                });
                //手动绘制等高线
                layer_select_map_source.findViewById(R.id.tv_map_contour_draw).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        //进入绘制线界面，绘制完成后获取到绘制到的线的点位集合
                        //自动弹出绘制点线面的fragment
                        Bundle lineBundle = new Bundle();
                        lineBundle.putSerializable(com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.class.getSimpleName(), com.cateye.vtm.fragment.DrawPointLinePolygonFragment.DRAW_STATE.DRAW_LINE);
                        lineBundle.putInt(SystemConstant.DRAW_USAGE, SystemConstant.DRAW_CONTOUR_LINE);
                        loadRootFragment(R.id.layer_main_cateye_bottom, com.cateye.vtm.fragment.DrawPointLinePolygonFragment.newInstance(lineBundle));
                    }
                });
            }
        }
    };

    /**
     * 从网络获取地图资源
     */
    private void getMapDataSourceFromNet() {
        final RxDialogLoading rxDialogLoading = new RxDialogLoading(getContext());
        OkGo.<String>get(URL_MAP_SOURCE_NET.replace(SystemConstant.USER_ID, SystemConstant.CURRENT_PROJECTS_ID + "")).tag(this).converter(new StringConvert()).adapt(new ObservableResponse<String>()).subscribeOn(Schedulers.io()).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                rxDialogLoading.show();
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Response<String>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Response<String> stringResponse) {
                String resultStr = stringResponse.body();
                MapSourceFromNet mapSourceFromNet = JSON.parseObject(resultStr, MapSourceFromNet.class);
                if (mapSourceFromNet != null) {
                    List<MapSourceFromNet.DataBean> dataBeanList = mapSourceFromNet.getData();
                    if (dataBeanList != null && !dataBeanList.isEmpty()) {
                        Observable.fromIterable(dataBeanList).subscribeOn(Schedulers.computation())/*.filter(new Predicate<MapSourceFromNet.DataBean>() {
                            @Override
                            public boolean test(MapSourceFromNet.DataBean dataBean) throws Exception {
                                if (dataBean != null && dataBean.getExtension() != null && (dataBean.getExtension().contains("png") || dataBean.getExtension().contains("json") || dataBean.getExtension().contains("jpg") || dataBean.getExtension().contains("jpeg")) && dataBean.getHref() != null && dataBean.getHref().contains("/xyz/")) {
                                    return true;
                                }
                                return false;
                            }
                        })*/.toList().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<List<MapSourceFromNet.DataBean>>() {
                            @Override
                            public void accept(List<MapSourceFromNet.DataBean> dataBeanList) throws Exception {
                                if (dataBeanList != null) {
                                    layerDataBeanList = dataBeanList;
                                    showLayerManagerDialog(dataBeanList);
                                }else {
                                    RxToast.warning("当前项目没有可作业的图层，请联系系统管理员确认！");
                                }
                            }
                        });
                    }else {
                        RxToast.warning("当前项目没有可作业的图层，请联系系统管理员确认！");
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                RxToast.info("请求失败，请检查网络!", Toast.LENGTH_SHORT);
                RxLogTool.saveLogFile(e.toString());
                if (rxDialogLoading != null && rxDialogLoading.isShowing()) {
                    rxDialogLoading.dismiss();
                }
            }

            @Override
            public void onComplete() {
                if (rxDialogLoading != null && rxDialogLoading.isShowing()) {
                    rxDialogLoading.dismiss();
                }
            }
        });
    }

    /**
     * @param :
     * @return :
     * @method : showLayerManagerDialog
     * @Author : xiaoxiao
     * @Describe : 显示图层管理的对话框
     * @Date : 2018/6/27
     */
    private MapSourceFromNet.DataBean mDraggedEntity;
    private int dragBeginPosition = -1;

    private void showLayerManagerDialog(final List<MapSourceFromNet.DataBean> dataBeanList) {
        //使用ExpandableListView展示二级列表
        layerManagerRootView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_layer_manager, null);
        ExpandableListView expandableListView = (ExpandableListView) layerManagerRootView.findViewById(R.id.sadLv_layerlist);

        layerManagerAdapter = new LayerManagerAdapter(getActivity(), dataBeanList);
        expandableListView.setAdapter(layerManagerAdapter);

        //增加按钮
        TextView tv_add = (TextView) layerManagerRootView.findViewById(R.id.tv_layerlist_add);
        tv_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getActivity(), MainActivity.MapFilePicker.class),
                        SELECT_MAP_FILE);
            }
        });
        new CanDialog.Builder(getActivity()).setView(layerManagerRootView).setNegativeButton("取消", true, null).setPositiveButton("确定", true, new CanDialogInterface.OnClickListener() {
            @Override
            public void onClick(CanDialog dialog, int checkItem, CharSequence text, boolean[] checkItems) {
                clearAllMapLayers();
                //清空多图层列表list数据，重新筛选获取
                multiTimeLayerList.clear();
                //根据当前的资源选择，显示对应的图层
                for (MapSourceFromNet.DataBean dataBean : dataBeanList) {
                    boolean isShow = dataBean.isShow();
                    if (isShow) {//设置为选中可显示状态
                        if (dataBean.getMaps().get(0).getExtension().contains("json")) {
                            ContourGeojsonTileSource mTileSource = ContourGeojsonTileSource.builder()
                                    .url(dataBean.getMaps().get(0).getHref()).tilePath("/{X}/{Y}/{Z}.json" /*+ stringDataBeanMap.get(key).getExtension()*/)
                                    .zoomMax(18).build();
                            mTileSource.setOption(SystemConstant.LAYER_KEY_ID, dataBean.getId() + "");
                            createGeoJsonTileLayer(getActivity(), mTileSource, true, dataBean.getGroup());
                        } else if (dataBean.getMaps().get(0).getExtension().contains(".map")) {
                            addLocalMapFileLayer(dataBean.getMaps().get(0).getHref());
                        } else {
                            BitmapTileSource mTileSource = BitmapTileSource.builder()
                                    .url(dataBean.getMaps().get(0).getHref()).tilePath("/{X}/{Y}/{Z}." + dataBean.getMaps().get(0).getExtension())
                                    .zoomMax(18).build();
                            createBitmapTileLayer(getActivity(), mTileSource, true, dataBean.getGroup());
                            mTileSource.setOption(SystemConstant.LAYER_KEY_ID, dataBean.getId() + "");
                        }

                        if (dataBean.getMaps() != null && dataBean.getMaps().size() > 1) {
                            multiTimeLayerList.add(dataBean);
                        }
                    }
                }
                showMultiTimeLayerSelectFragment(multiTimeLayerList);
                mMap.clearMap();
            }
        }).show();
    }

    /**
     * @param : multiTimeLayerList - 多时序显示数据
     * @return :
     * @method : showMultiTimeLayerSelectFragment
     * @Author : xiaoxiao
     * @Describe : 显示时序选择控件
     * @Date : 2018/8/31
     */
    private void showMultiTimeLayerSelectFragment(List<MapSourceFromNet.DataBean> multiTimeLayerList) {
        if (multiTimeLayerList != null && !multiTimeLayerList.isEmpty()) {
            MultiTimeLayerSelectFragment fragment = findFragment(MultiTimeLayerSelectFragment.class);
            //自动弹出绘制点线面的fragment
            Bundle bundle = new Bundle();
            bundle.putSerializable(SystemConstant.BUNDLE_MULTI_TIME_SELECTOR_DATA, (ArrayList) multiTimeLayerList);
            if (fragment != null) {
                fragment.setArguments(bundle);
                start(fragment);
            } else {
                loadRootFragment(R.id.layer_main_cateye_top, MultiTimeLayerSelectFragment.newInstance(bundle));
            }
        }
    }

    /**
     * method : setDrawPointLinePolygonButtonState
     * Author : xiaoxiao
     * Describe : 设置绘制点线面时三个按钮的状态
     * param :
     * return :
     * Date : 2018/4/26
     */
    private void setDrawPointLinePolygonButtonState(View clickView, List<ImageView> radioButtonViewList) {
        if (clickView != null) {
            if (clickView.isSelected()) {
                clickView.setSelected(false);
                for (View v : radioButtonViewList) {
                    v.setEnabled(true);
                    v.setSelected(false);
                }
            } else {
                clickView.setSelected(true);
                for (View v : radioButtonViewList) {
                    if (v != clickView) {
                        v.setEnabled(false);
                    }
                }
            }
        }
    }

    private void initScaleBar() {
        //scale的图层到操作分组中
        CatEyeMapScaleBar mMapScaleBar = new CatEyeMapScaleBar(mMap);
        mMapScaleBar.setScaleBarMode(CatEyeMapScaleBar.ScaleBarMode.BOTH);
        mMapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
        mMapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
        mMapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

        MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mMap, mMapScaleBar);
        BitmapRenderer renderer = mapScaleBarLayer.getRenderer();
        renderer.setPosition(GLViewport.Position.BOTTOM_LEFT);
        renderer.setOffset(5 * CanvasAdapter.getScale(), 0);
        mMap.layers().add(mapScaleBarLayer, LAYER_GROUP_ENUM.OPERTOR_GROUP.orderIndex);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == SELECT_MAP_FILE) {//选择本地地图文件显示
            if (resultCode != getActivity().RESULT_OK || intent == null || intent.getStringExtra(FilePicker.SELECTED_FILE) == null) {
//                finish();
                return;
            }

            String file = intent.getStringExtra(FilePicker.SELECTED_FILE);

            //增加本地layer的dataBean到dataBeanList中
            if (file != null) {
                //判断当前图层中是否已经存在选择的文件，如果存在，则不再添加
                if (layerDataBeanList != null && !layerDataBeanList.isEmpty()) {
                    for (MapSourceFromNet.DataBean dataBean : layerDataBeanList) {
                        if (dataBean.getMaps()!=null) {
                            for (MapSourceFromNet.DataBean.MapsBean mapsBean:dataBean.getMaps()){
                                if (file.equals(mapsBean.getHref())){
                                    RxToast.info("已经添加过相同的图层！无法再次添加！");
                                    return;
                                }
                            }
                        }
                    }
                }
                File mapFile = new File(file);
                if (mapFile.exists()) {
                    MapSourceFromNet.DataBean.MapsBean mapFileDataBean = new MapSourceFromNet.DataBean.MapsBean();
                    mapFileDataBean.setAbstractX(mapFile.getName());
                    mapFileDataBean.setHref(file);
                    mapFileDataBean.setGroup(LAYER_GROUP_ENUM.BASE_VECTOR_GROUP.name);
                    mapFileDataBean.setExtension(".map");

                    MapSourceFromNet.DataBean localDataBean = new MapSourceFromNet.DataBean();
                    localDataBean.setGroup(LAYER_GROUP_ENUM.BASE_VECTOR_GROUP.name);
                    localDataBean.setMemo(mapFile.getName());
//                    localDataBean.setKind(-1);
//                    localDataBean.setId(-1);
//                    localDataBean.setType(-1);
                    localDataBean.setMaps(new ArrayList<MapSourceFromNet.DataBean.MapsBean>());
                    localDataBean.getMaps().add(mapFileDataBean);
                    if (layerDataBeanList != null) {
                        layerDataBeanList.add(localDataBean);
                        if (layerManagerAdapter != null) {
                            layerManagerAdapter.sortListDataAndGroup(layerDataBeanList);
                            layerManagerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        } else if (requestCode == SELECT_THEME_FILE) {//选择本地style文件显示
            if (resultCode != getActivity().RESULT_OK || intent == null || intent.getStringExtra(FilePicker.SELECTED_FILE) == null) {
                return;
            }

            String file = intent.getStringExtra(FilePicker.SELECTED_FILE);
            ExternalRenderTheme externalRenderTheme = new ExternalRenderTheme(file);

            // Use tessellation with sea and land for Mapsforge themes
            if (ThemeUtils.isMapsforgeTheme(externalRenderTheme)) {
                //遍历所有的地图图层，添加hook
                if (mMap.layers() != null && !mMap.layers().isEmpty()) {
                    for (Layer layer : mMap.layers()) {
                        if (layer.isEnabled() && layer instanceof OsmTileLayer)
                            ((OsmTileLayer) layer).addHook(new VectorTileLayer.TileLoaderThemeHook() {
                                @Override
                                public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
                                    if (element.tags.contains(ISSEA_TAG) || element.tags.contains(SEA_TAG) || element.tags.contains(NOSEA_TAG)) {
                                        if (style instanceof AreaStyle)
                                            ((AreaStyle) style).mesh = true;
                                    }
                                    return false;
                                }

                                @Override
                                public void complete(MapTile tile, boolean success) {
                                }
                            });
                    }
                }

            }
            mMap.setTheme(externalRenderTheme);
        } else if (requestCode == SELECT_GEOJSON_FILE) {
            if (resultCode != getActivity().RESULT_OK || intent == null || intent.getStringExtra(FilePicker.SELECTED_FILE) == null) {
                return;
            }
            String filePath = intent.getStringExtra(FilePicker.SELECTED_FILE);
            File geoJsonFile = new File(filePath);
            if (geoJsonFile.exists() && geoJsonFile.isFile()) {
                FileInputStream geoInputStream = null;
                try {
                    geoInputStream = new FileInputStream(geoJsonFile);
                    loadJson(geoInputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == SELECT_CONTOUR_FILE) {
            try {
                if (resultCode != getActivity().RESULT_OK || intent == null || intent.getStringExtra(FilePicker.SELECTED_FILE) == null) {
                    return;
                }
                String filePath = intent.getStringExtra(FilePicker.SELECTED_FILE);
                File geoJsonFile = new File(filePath);
                if (geoJsonFile.exists() && geoJsonFile.isFile()) {
                    JSONReader reader = null;
                    reader = new JSONReader(new FileReader(filePath));
                    //此处使用list的int数组记录从文件中读取到的数据
                    List<ContourMPData> xyzList = new ArrayList<>();
                    reader.startArray();
                    while (reader.hasNext()) {
                        JSONArray jsonArray = (JSONArray) reader.readObject();
                        if (jsonArray != null) {
                            ContourMPData contourMPData = new ContourMPData();
                            contourMPData.setGeoPoint(new GeoPoint(((BigDecimal) jsonArray.get(1)).doubleValue(), ((BigDecimal) jsonArray.get(0)).doubleValue()));
                            contourMPData.setmHeight(((BigDecimal) jsonArray.get(2)).floatValue());
                            xyzList.add(contourMPData);
                        }
                    }
                    reader.endArray();
                    reader.close();
                    //自动弹出绘制高度折线的fragment
                    Bundle pointBundle = new Bundle();
                    pointBundle.putSerializable(SystemConstant.DATA_CONTOUR_CHART, (Serializable) xyzList);
                    loadRootFragment(R.id.layer_main_cateye_bottom, ContourMPChartFragment.newInstance(pointBundle));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                RxToast.error("您选择的文件不符合等高线文件读取标准");
            }
        }
    }

    /**
     * 增加本地地图layer
     */
    private void addLocalMapFileLayer(String localMapFilePath) {
        MapFileTileSource mTileSource = new MapFileTileSource();
        mTileSource.setPreferredLanguage("zh");

        if (mTileSource.setMapFile(localMapFilePath)) {
            //设置当前的文件选择的layer为地图的基础图层(第一层)==此处去掉此设置
            VectorTileLayer mTileLayer = new OsmTileLayer(mMap);
            mTileLayer.setTileSource(mTileSource);
            mMap.layers().add(mTileLayer, LAYER_GROUP_ENUM.BASE_VECTOR_GROUP.orderIndex);
//            LabelLayer labelLayer = new LabelLayer(mMap, mTileLayer);
            mMap.layers().add(new LabelLayer(mMap, mTileLayer), LAYER_GROUP_ENUM.OTHER_GROUP.orderIndex);
            mMap.layers().add(new BuildingLayer(mMap, mTileLayer), LAYER_GROUP_ENUM.OTHER_GROUP.orderIndex);

            MapInfo info = mTileSource.getMapInfo();
            MapPosition pos = new MapPosition();
            pos.setByBoundingBox(info.boundingBox, Tile.SIZE * 4, Tile.SIZE * 4);
            mMap.animator().animateTo(pos);
            loadTheme(null, true);

            mPrefs.clear();
//            mTileSourceList.add(mTileSource);
        }
    }

    /**
     * 设置地图样式
     */
    protected void loadTheme(final String styleId, boolean isAllLayers) {
        if (!StringUtils.isEmpty(styleId)) {
            mMap.setTheme(new AssetsRenderTheme(getActivity().getAssets(), "", "vtm/stylemenu.xml", new XmlRenderThemeMenuCallback() {
                @Override
                public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
                    // Use the selected style or the default
                    String style = styleId != null ? styleId : renderThemeStyleMenu.getDefaultValue();

                    // Retrieve the layer from the style id
                    XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
                    if (renderThemeStyleLayer == null) {
                        System.err.println("Invalid style " + style);
                        return null;
                    }

                    // First get the selected layer's categories that are enabled together
                    Set<String> categories = renderThemeStyleLayer.getCategories();

                    // Then add the selected layer's overlays that are enabled individually
                    // Here we use the style menu, but users can use their own preferences
                    for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                        if (overlay.isEnabled())
                            categories.addAll(overlay.getCategories());
                    }

                    // This is the whole categories set to be enabled
                    return categories;
                }
            }), isAllLayers);
        } else {
            mMap.setTheme(VtmThemes.DEFAULT, isAllLayers);
        }
    }

    private void createBitmapTileLayer(Context mContext, BitmapTileSource mTileSource, boolean USE_CACHE, String layerGroup) {
        if (mTileSource == null)
            return;

        if (USE_CACHE) {
            String cacheFile = mTileSource.getUrl()
                    .toString()
                    .replaceFirst("https?://", "")
                    .replaceAll("/", "-");

            RxLogTool.i("use bitmap cache {}", cacheFile);
            TileCache mCache = new TileCache(mContext, null, cacheFile);
            mCache.setCacheSize(512 * (1 << 10));
            mTileSource.setCache(mCache);
        }

        BitmapTileLayer mBitmapLayer = new BitmapTileLayer(mMap, mTileSource);
        mMap.layers().add(mBitmapLayer, LAYER_GROUP_ENUM.getGroupByName(layerGroup).orderIndex);
        mMap.updateMap(true);

        MapPosition mapPosition = mMap.getMapPosition();
        mapPosition.setPosition(mapPosition.getLatitude(), mapPosition.getLongitude() + 0.0000001);
        mMap.setMapPosition(mapPosition);
    }

    private void createGeoJsonTileLayer(Context mContext, GeojsonTileSource mTileSource, boolean USE_CACHE, String layerGroup) {
        if (mTileSource == null)
            return;

        if (USE_CACHE) {
            String cacheFile = mTileSource.getUrl()
                    .toString()
                    .replaceFirst("https?://", "")
                    .replaceAll("/", "-");

            RxLogTool.i("use geoJson cache {}", cacheFile);
            TileCache mCache = new TileCache(mContext, null, cacheFile);
            mCache.setCacheSize(512 * (1 << 10));
            mTileSource.setCache(mCache);
        }

        VectorTileLayer mVectorTileLayer = new VectorTileLayer(mMap, mTileSource);
        mMap.layers().add(mVectorTileLayer, LAYER_GROUP_ENUM.getGroupByName(layerGroup).orderIndex);
        mMap.layers().add(new LabelLayer(mMap, mVectorTileLayer), LAYER_GROUP_ENUM.OTHER_GROUP.orderIndex);
        loadTheme(null, true);
        mMap.updateMap(true);
    }

    /**
     * 加载指定的GeoJsonlayer
     */
    void loadJson(InputStream is) {
        RxToast.info("got data");

        VectorDataset data = JeoTest.readGeoJson(is);

        Style style = null;

        try {
            style = Carto.parse("" +
                    "#qqq {" +
                    "  line-width: 2;" +
                    "  line-color: #f09;" +
                    "  polygon-fill: #44111111;" +
                    "  " +
                    "}" +
                    "#states {" +
                    "  line-width: 2.2;" +
                    "  line-color: #c80;" +
                    "  polygon-fill: #44111111;" +
                    "  " +
                    "}"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextStyle textStyle = TextStyle.builder()
                .isCaption(true)
                .fontSize(16 * CanvasAdapter.getScale()).color(Color.BLACK)
                .strokeWidth(2.2f * CanvasAdapter.getScale()).strokeColor(Color.WHITE)
                .build();
        ContourLineLayer contourLineLayer = new ContourLineLayer(mMap, data, style, textStyle);
        mMap.layers().add(contourLineLayer, LAYER_GROUP_ENUM.OTHER_GROUP.orderIndex);

        RxToast.info("data ready");
        mMap.updateMap(true);
    }

    @Override
    public boolean onBackPressedSupport() {
        return false;
    }

    @Subscribe
    public void onEventMainThread(Message msg) {
        switch (msg.what) {
            case SystemConstant.MSG_WHAT_DRAW_POINT_LINE_POLYGON_DESTROY://绘制点线面结束
                if (chkDrawPointLinePolygonList != null) {
                    for (ImageView chk : chkDrawPointLinePolygonList) {
                        if (!chk.isEnabled()) {
                            chk.setEnabled(true);
                        }
                        if (chk.isSelected()) {
                            chk.setSelected(false);
                        }
                    }
                }
                break;
            case SystemConstant.MSG_WHAT_LOCATION_UPDATE://位置有更新
                if (msg.obj != null) {
                    TencentLocation location = (TencentLocation) msg.obj;
                    locationLayer.setEnabled(true);
                    locationLayer.setPosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());

                    // Follow location
                    if (isMapCenterFollowLocation) {
                        mMap.getMapPosition(mapPosition);
                        mapPosition.setPosition(location.getLatitude(), location.getLongitude());
                        mMap.animator().animateTo(mapPosition);
                        isMapCenterFollowLocation = false;
                    }
                }
                break;
            case SystemConstant.MSG_WHAT_MAIN_AREA_HIDEN_VISIBLE:
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    boolean isHiden = bundle.getBoolean(SystemConstant.BUNDLE_AREA_HIDEN_STATE);
                    BUTTON_AREA button_area = (BUTTON_AREA) bundle.getSerializable(SystemConstant.BUNDLE_BUTTON_AREA);
                    hideOrShowButtonArea(isHiden, button_area);
                }
                break;
            case SystemConstant.MSG_WHAT_DRAW_RESULT://获取到绘制的点集合
                if (msg.arg1 == SystemConstant.DRAW_CONTOUR_LINE) {
                    List<GeoPoint> geoPointList = (List<GeoPoint>) msg.obj;
                    if (geoPointList != null && geoPointList.size() > 1) {
                        StringBuilder contourParam = new StringBuilder();
                        String layerName = null;
                        double gujiaoLatMin = 36.1688086262;
                        double gujiaoLonMin = 110.8021029688;
                        double gujiaoLatMax = 39.3333699398;
                        double gujiaoLonMax = 113.1415834394;

                        double jingzhuangLatMin = 33.0335361398;
                        double jingzhuangLonMin = 103.8403611975;
                        double jingzhuangLatMax = 36.0227737918;
                        double jingzhuangLonMax = 107.0461587400;
                        for (GeoPoint geoPoint : geoPointList) {
                            contourParam.append(geoPoint.getLongitude()).append(",").append(geoPoint.getLatitude()).append(";");
                        }

                        if (geoPointList.get(0).getLongitude() < gujiaoLonMax && geoPointList.get(0).getLongitude() > gujiaoLonMin && geoPointList.get(0).getLatitude() < gujiaoLatMax && geoPointList.get(0).getLatitude() > gujiaoLatMin) {
                            layerName = "gujiao";
                        }
                        if (geoPointList.get(0).getLongitude() < jingzhuangLonMax && geoPointList.get(0).getLongitude() > jingzhuangLonMin && geoPointList.get(0).getLatitude() < jingzhuangLatMax && geoPointList.get(0).getLatitude() > jingzhuangLatMin) {
                            layerName = "jingzhuang";
                        }
                        if (layerName == null) {
                            RxToast.info("绘制的线不在指定区域内！");
                            return;
                        }

                        final RxDialogLoading rxDialogLoading = new RxDialogLoading(getContext());
                        OkGo.<String>get(URL_CONTOUR_CALCULATE).params("xys", contourParam.toString()).tag(this).params("layerName", layerName).converter(new StringConvert()).adapt(new ObservableResponse<String>()).subscribeOn(Schedulers.io()).doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                rxDialogLoading.show();
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Response<String>>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(Response<String> stringResponse) {
                                String resultStr = stringResponse.body();
                                ContourFromNet contourFromNet = JSON.parseObject(resultStr, ContourFromNet.class);
                                if (contourFromNet.isSuccess()) {
                                    if (contourFromNet != null) {
                                        List<ContourFromNet.Contour> contourList = contourFromNet.getData();
                                        if (contourList != null && !contourList.isEmpty()) {
                                            List<ContourMPData> contourMPDataList = new ArrayList<>();
                                            for (ContourFromNet.Contour contour : contourList) {
                                                ContourMPData contourMPData = new ContourMPData();
                                                contourMPData.setGeoPoint(new GeoPoint(contour.getLatitude(), contour.getLongitude()));
                                                contourMPData.setmHeight(contour.getHeight());
                                                contourMPDataList.add(contourMPData);
                                            }
                                            //自动弹出绘制高度折线的fragment
                                            Bundle pointBundle = new Bundle();
                                            pointBundle.putSerializable(SystemConstant.DATA_CONTOUR_CHART, (Serializable) contourMPDataList);
                                            loadRootFragment(R.id.layer_main_cateye_bottom, ContourMPChartFragment.newInstance(pointBundle));
                                        } else {
                                            RxToast.error("绘制的区域无法获取到高度信息!");
                                        }
                                    }
                                } else {
                                    RxToast.error("计算等高线失败");
                                }

                            }

                            @Override
                            public void onError(Throwable e) {
                                RxToast.info("请求失败，请检查网络!", Toast.LENGTH_SHORT);
                                RxLogTool.saveLogFile(e.toString());
                            }

                            @Override
                            public void onComplete() {
                                rxDialogLoading.dismiss();
                            }
                        });
                    } else {
                        RxToast.error("绘制的线至少需要包含两个点");
                    }
                }
                break;
            case SystemConstant.MSG_WHAT_DRAW_LAYER_TIME_SELECT:
                int dataBeanId = msg.arg1;
                int mapLayerIndex = msg.arg2;
                replaceMultiLayerIndex(dataBeanId, mapLayerIndex);
                break;
        }
    }

    private void replaceMultiLayerIndex(int dataBeanId, int layerIndex) {
        //首先遍历所有的图层数据，找出指定id的图层数据
        MapSourceFromNet.DataBean replaceDataBean = null;
        if (layerDataBeanList != null && !layerDataBeanList.isEmpty()) {
            for (MapSourceFromNet.DataBean dataBean : layerDataBeanList) {
                if (dataBeanId == dataBean.getId()) {
                    replaceDataBean = dataBean;
                    break;
                }
            }
            //如果能找到指定的数据，则遍历图层列表,将原有的该资源对应的layer移除
            if (replaceDataBean != null) {
                Iterator iterator = mMap.layers().iterator();
                while (iterator.hasNext()) {
                    Layer layer = (Layer) iterator.next();
                    if (layer instanceof BitmapTileLayer) {
                        String id = ((BitmapTileLayer) layer).getTileSource().getOption(SystemConstant.LAYER_KEY_ID);
                        if (id != null && id.equals(dataBeanId + "")) {
                            iterator.remove();
                        }
                    }
                }
                BitmapTileSource mTileSource = BitmapTileSource.builder()
                        .url(replaceDataBean.getMaps().get(layerIndex).getHref()).tilePath("/{X}/{Y}/{Z}." + replaceDataBean.getMaps().get(layerIndex).getExtension())
                        .zoomMax(18).build();
                createBitmapTileLayer(getActivity(), mTileSource, true, replaceDataBean.getGroup());
                mTileSource.setOption(SystemConstant.LAYER_KEY_ID, replaceDataBean.getId() + "");
            }
        }
    }

    /**
     * @param :
     * @return :
     * @method : hideButton
     * @Author : xiaoxiao
     * @Describe : 隐藏右侧按钮列表
     * @Date : 2018/5/24
     */
    private void hideOrShowButtonArea(boolean isVisible, BUTTON_AREA button_area) {
        switch (button_area) {
            case ALL:
                rootView.findViewById(R.id.layer_main_fragment_right).setVisibility(isVisible ? View.VISIBLE : View.GONE);
                rootView.findViewById(R.id.img_location).setVisibility(isVisible ? View.VISIBLE : View.GONE);
                break;
            case LOCATION:
                rootView.findViewById(R.id.img_location).setVisibility(isVisible ? View.VISIBLE : View.GONE);
                break;
            case BOTTOM_LEFT:
                rootView.findViewById(R.id.img_location).setVisibility(isVisible ? View.VISIBLE : View.GONE);
                break;
            case BOTTOM_RIGHT:
                rootView.findViewById(R.id.layer_main_fragment_right_bottom).setVisibility(isVisible ? View.VISIBLE : View.GONE);
                break;
        }
    }

    public enum BUTTON_AREA {
        ALL/*所有按钮*/, LOCATION/*定位按钮*/, BOTTOM_LEFT/*左下角*/, BOTTOM_RIGHT/*右下角*/
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                break;
        }
    }

    /**
     * @method : clearAllLayers
     * @Author : xiaoxiao
     * @Describe : 清空地图上所有图层
     * @param :
     * @return :
     * @Date : 2018/9/21
    */
    public void clearAllMapLayers(){
        if (mMap!=null&&mMap.layers()!=null){
            Iterator mapLayerIterator = mMap.layers().iterator();
            while (mapLayerIterator.hasNext()) {
                Layer layer = (Layer) mapLayerIterator.next();
                if (!(layer instanceof MapEventLayer) && !(layer instanceof MapEventLayer2) && !(layer instanceof LocationLayer) && !(layer instanceof MapScaleBarLayer)) {
                    mapLayerIterator.remove();
                }
            }
        }
    }

    public List<MapSourceFromNet.DataBean> getLayerDataBeanList() {
        return layerDataBeanList;
    }

    public List<MapSourceFromNet.DataBean> getMultiTimeLayerList() {
        return multiTimeLayerList;
    }
}
