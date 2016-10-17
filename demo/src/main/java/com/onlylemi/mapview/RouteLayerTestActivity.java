package com.onlylemi.mapview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.onlylemi.mapview.library.MapView;
import com.onlylemi.mapview.library.MapViewListener;
import com.onlylemi.mapview.library.layer.LocationLayer;
import com.onlylemi.mapview.library.layer.MarkLayer;
import com.onlylemi.mapview.library.layer.RouteLayer;
import com.onlylemi.mapview.library.utils.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouteLayerTestActivity extends AppCompatActivity {

    private MapView mapView;

    private MarkLayer     markLayer;
    private RouteLayer    routeLayer;
    private LocationLayer locationLayer;

    private List<PointF>  nodes;
    private List<PointF>  nodesContract;
    private List<PointF>  marks;
    private List<String>  marksName;
    private List<Integer> routeList;
    private List<PointF>  routeNodes;

    private RoutePositionChanger routePositionChanger = new RoutePositionChanger(
            new RoutePositionChanger.RoutePositionChangerCallback() {

                @Override
                public void onCallback(PointF point) {

                    nodes.add(point);
                    routeList.remove(0);
                    routeList.add(0, nodes.size() - 1);

                    if (isPassRouteNode(point)) {
                        routeList.remove(1);
                    }

                    routeLayer.setNodeList(nodes);
                    routeLayer.setRouteList(routeList);
                    locationLayer.setCurrentPosition(point);
                    mapView.refresh();

                    nodes.remove(nodes.size() - 1);
                }
            }, (float) 20, (float) 5);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_layer_test);

        nodes = TestData.getNodesList();
        nodesContract = TestData.getNodesContactList();
        marks = TestData.getMarks();
        marksName = TestData.getMarksName();
        MapUtils.init(nodes.size(), nodesContract.size());

        mapView = (MapView) findViewById(R.id.mapview);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open("map.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mapView.loadMap(bitmap);
        mapView.setMapViewListener(new MapViewListener() {
            @Override
            public void onMapLoadSuccess() {

                locationLayer = new LocationLayer(mapView, marks.get(39));
                locationLayer.setOpenCompass(true);
                locationLayer.setCompassIndicatorCircleRotateDegree(60);
                locationLayer.setCompassIndicatorArrowRotateDegree(-30);
                mapView.addLayer(locationLayer);

                routeLayer = new RouteLayer(mapView);
                mapView.addLayer(routeLayer);

                markLayer = new MarkLayer(mapView, marks, marksName);
                mapView.addLayer(markLayer);
                markLayer.setMarkIsClickListener(new MarkLayer.MarkIsClickListener() {
                    @Override
                    public void markIsClick(int num) {

                        if (routePositionChanger.isRunning()) {
                            routePositionChanger.stop();
                        }

                        nodes = TestData.getNodesList();
                        nodesContract = TestData.getNodesContactList();

                        PointF target = new PointF(marks.get(num).x, marks.get(num).y);
                        routeList = MapUtils.getShortestDistanceBetweenTwoPoints
                                (locationLayer.getCurrentPosition(), target, nodes, nodesContract);
                        routeLayer.setNodeList(nodes);
                        routeLayer.setRouteList(routeList);
                        mapView.refresh();

                        routeNodes = new ArrayList<>();
                        for (int i : routeList) {
                            routeNodes.add(nodes.get(i));
                        }

                        moveLocation();
                    }
                });
                mapView.refresh();

            }


            @Override
            public void onMapLoadFail() {

            }

        });
    }


    private void moveLocation() {
        routePositionChanger.start(routeNodes);
    }


    private boolean isPassRouteNode(PointF point) {

        PointF routeNode = nodes.get(routeList.get(1));
        return getDistanceOfTowPoints(point, routeNode) == 0;
    }


    private double getDistanceOfTowPoints(PointF from, PointF to) {

        double distanceSquare = Math.pow(from.x - to.x, 2) + Math.pow(from.y - to.y, 2);
        return Math.sqrt(distanceSquare);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_route_layer_test, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mapView.isMapLoadFinish()) {
            switch (item.getItemId()) {
                case R.id.route_layer_tsp:
                    List<PointF> list = new ArrayList<>();
                    list.add(marks.get(39));
                    list.add(marks.get(new Random().nextInt(10)));
                    list.add(marks.get(new Random().nextInt(10) + 10));
                    list.add(marks.get(new Random().nextInt(10) + 20));
                    list.add(marks.get(new Random().nextInt(10) + 9));
                    List<Integer> routeList = MapUtils.getBestPathBetweenPoints(list, nodes,
                                                                                nodesContract);
                    routeLayer.setNodeList(nodes);
                    routeLayer.setRouteList(routeList);
                    mapView.refresh();
                    break;
                default:
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
