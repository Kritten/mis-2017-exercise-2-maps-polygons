package com.example.micro.exercise2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLongClickListener {

    final Activity activity = this;
    private EditText editText;
    private GoogleMap mMap;
    private SharedPreferences sharedPref;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<LatLng> polygon_vertices = new ArrayList<LatLng>();
    private boolean is_polygon_mode = false;
    private Polygon polygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        editText = (EditText) findViewById(R.id.editText);

//        try to connect with the google api and call "onConnect" on success
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("CONNECTED WITH GOOGLE API");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used. call "onMapReady"
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("MAP LOADED");
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this);

        System.out.println("finished map handling");

        Set<String> set_markers = sharedPref.getStringSet("markers", new HashSet<String>());

        System.out.println("VALUES");
        for(String value: set_markers)
        {
            String[] result = value.split("__");

            LatLng latLng = new LatLng(Double.parseDouble(result[1]), Double.parseDouble(result[2]));
            MarkerOptions marker = new MarkerOptions().position(latLng).title(result[0]);
            mMap.addMarker(marker);
        }

        LatLng your_location = new LatLng(50.972997, 11.327663);
        mMap.addMarker(new MarkerOptions().position(your_location).title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(your_location, 15));
    }
    public void onCLickPolygon(View view) {
        Button button = (Button) findViewById(R.id.polygon);
        String button_text = button.getText().toString();
        if(button_text.equals("Start Polygon"))
        {
            button.setText("End Polygon");
            is_polygon_mode = true;
        } else {
            button.setText("Start Polygon");
            is_polygon_mode = false;

            PolygonOptions rectOptions = new PolygonOptions();
            for(LatLng latLng: polygon_vertices)
            {
                rectOptions.add(latLng);
            }
            rectOptions.fillColor(0x440000ff);
            Polygon polygon = mMap.addPolygon(rectOptions);
            double area = SphericalUtil.computeArea(polygon.getPoints());

            LatLng centroid = centroid(polygon_vertices);

            DecimalFormat decimalFormat = new DecimalFormat("##.##");
            String string_area = (area >= 100000) ? decimalFormat.format(area/1000000.0)+"km^2" : decimalFormat.format(area)+"m^2";

            MarkerOptions marker = new MarkerOptions().position(centroid).title(string_area);
            mMap.addMarker(marker);

            System.out.println(area);
            polygon_vertices.clear();
        }
    }

    private static LatLng centroid(ArrayList<LatLng> polygon_vertices) {
        float[] centroid = { 0, 0 };

        for (int i = 0; i < polygon_vertices.size(); i++) {
            centroid[0] += polygon_vertices.get(i).latitude;
            centroid[1] += polygon_vertices.get(i).longitude;
        }

        int totalPoints = polygon_vertices.size();
        centroid[0] = centroid[0] / totalPoints;
        centroid[1] = centroid[1] / totalPoints;

        return new LatLng(centroid[0], centroid[1]);
    }

    private double area(ArrayList<LatLng> vertices)
    {
        double area = 0.0;
        int j = vertices.size() - 1;

        ArrayList<Double> x = new ArrayList<Double>();
        ArrayList<Double> y = new ArrayList<Double>();

        for(LatLng latLng: vertices)
        {
            x.add(latLng.latitude);
            y.add(latLng.longitude);
        }
//        x.add(vertices.get(vertices.size()-1).latitude);
//        y.add(vertices.get(vertices.size()-1).longitude);

        for(int i=0; i < vertices.size(); i++)
        {
            area += (x.get(j) + x.get(i)) * (y.get(j) - y.get(i));
            j = i;
        }

        return area / 2;
    }

    public void onCLickClear(View view) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("markers", new HashSet<String>());
        editor.commit();

        mMap.clear();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println("Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        MarkerOptions marker = new MarkerOptions().position(latLng);

        if(is_polygon_mode)
        {
            polygon_vertices.add(latLng);
            mMap.addMarker(marker);
        } else {
            String text = editText.getText().toString();
            System.out.println(text);
            if(!text.isEmpty())
            {
                marker.title(text);
                mMap.addMarker(marker);

                String value = text + "__" + latLng.latitude + "__" + latLng.longitude;

                Set<String> set_markers = sharedPref.getStringSet("markers", new HashSet<String>());
                Set<String> new_set_markers = new HashSet<String>(set_markers);
                new_set_markers.add(value);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putStringSet("markers", new_set_markers);
                editor.commit();
            }
        }
    }
}
