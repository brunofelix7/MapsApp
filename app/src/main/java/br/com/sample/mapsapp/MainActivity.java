package br.com.sample.mapsapp;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.sample.mapsapp.api.DirectionsAPI;
import br.com.sample.mapsapp.api.DirectionsAPIListener;
import br.com.sample.mapsapp.models.Route;
import br.com.sample.mapsapp.repository.ThreadedStack;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, DirectionsAPIListener {

    //  Maps
    private GoogleMap googleMap;
    private MarkerOptions markerOptions;
    private Marker marker;

    //  Maps Available
    private GoogleApiAvailability api;

    //  Geocoder
    private Geocoder geocoder;
    private List<Address> listAddress;
    private Address address;

    //  Google API Client
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    //  GPS Service
    private BroadcastReceiver broadcastReceiver;
    private static final int REQUEST_CODE = 1;
    private static final String EXTRAS_KEY = "coordinates";
    private static final String EXTRAS_KEY_LATITUDE = "latitude";
    private static final String EXTRAS_KEY_LONGITUDE = "longitude";

    //  Google API Directions
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(googlePlayServicesAvailable()){
            Toast.makeText(this, "Google Play Services Available", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_main);
            initMap();
        }else{
            //  No Google Maps Layout
        }

        if(!runtimePermission()){
            enableButtons();
        }

        sendRequest();

    }

    @Override
    public void onDirectionStart(){
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);
        if (originMarkers != null){
            for(Marker marker: originMarkers){
                marker.remove();
            }
        }

        if (destinationMarkers != null){
            for(Marker marker: destinationMarkers){
                marker.remove();
            }
        }

        if (polylinePaths != null){
            for(Polyline polyline: polylinePaths ){
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionSuccess(List<Route> routes){
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route: routes) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            Log.d("Directions", "Tempo: "     + route.duration.text);
            Log.d("Directions", "Distância: " + route.distance.text);

            /*originMarkers.add(googleMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(googleMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(route.endAddress)
                    .position(route.endLocation)));*/

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(7);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(googleMap.addPolyline(polylineOptions));
        }
    }

    private void sendRequest(){
        String origin       = "-7.17981936,-34.89062548";
        String destination  = "-7.18424222,-34.89023387";
        try{
            new DirectionsAPI(this, origin, destination).execute();
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LatLng latLng = new LatLng(intent.getExtras().getDouble(EXTRAS_KEY_LATITUDE), intent.getExtras().getDouble(EXTRAS_KEY_LONGITUDE));
                    MarkerOptions options = new MarkerOptions();
                    options.position(latLng);
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_vehicle2));
                    googleMap.addMarker(options);

                    //  ARMAZENAR EM UMA PILHA, O TOPO SEMPRE SERÁ A POCISÃO ATUAL
                    ThreadedStack stack = new ThreadedStack();
                    stack.stackAdd(intent.getExtras().getString(EXTRAS_KEY));
                    Log.d("Stack", "Top: " + stack.getTop());
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(getApplicationContext(), GpsService.class);
        stopService(intent);
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(getApplicationContext(), GpsService.class);
        stopService(intent);
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    /**
     * RUNTIME PERMISSION #1
     */
    private boolean runtimePermission() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_CODE);
            return true;
        }
        return false;
    }

    /**
     * RUNTIME PERMISSION #2
     */
    private void enableButtons() {
        //  BOTÕES PARA INICIAR E PARAR O SERVICE
        Intent intent = new Intent(getApplicationContext(), GpsService.class);
        startService(intent);
    }

    /**
     * RUNTIME PERMISSION #3
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enableButtons();
            }else{
                runtimePermission();
            }
        }
    }

    /**
     * Verifica se o Google Play Services está disponível
     */
    public boolean googlePlayServicesAvailable(){
        api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if(isAvailable == ConnectionResult.SUCCESS){
            return true;
        }else if(api.isUserResolvableError(isAvailable)){
            Dialog dialog = api.getErrorDialog(this, isAvailable, REQUEST_CODE);
            dialog.show();
        }else{
            Toast.makeText(this, "Can't connect to Google Play Services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Inicialização do Google Maps
     */
    private void initMap(){
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    /**
     * Inicializa funções e ações no mapa quando ele estiver pronto
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        //  goTolocation(-7.186524, -34.889943, 15);

        //  ADICIONA UMA JANELA PERSONALIZADA NO MAPA
        if(this.googleMap != null){
            this.googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View view = getLayoutInflater().inflate(R.layout.map_info, null);

                    TextView tv_location = (TextView) view.findViewById(R.id.tv_location);
                    TextView tv_snnipet  = (TextView) view.findViewById(R.id.tv_snnipet);
                    TextView tv_lat_lng  = (TextView) view.findViewById(R.id.tv_lat_lng);

                    LatLng latLng = marker.getPosition();
                    tv_location.setText(marker.getTitle());
                    tv_snnipet.setText(marker.getSnippet());
                    tv_lat_lng.setText(latLng.latitude + ", " + latLng.longitude);

                    return view;
                }
            });
            this.googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    marker.hideInfoWindow();
                }
            });
        }

        //  CHECAR PERMISSÃO PARA O ANDROID M
        googleMap.setMyLocationEnabled(true);
        googleApiClientConnect();

        /*//  ADICIONAR POLYLINE
        LatLng latLngUser    = new LatLng(-7.17981936, -34.89062548);
        LatLng latLngVehicle = new LatLng(-7.18424222, -34.89023387);

        this.googleMap.addPolyline(new PolylineOptions().add(
                latLngUser,
                latLngVehicle
        )
                .width(7)
                .color(Color.BLUE)
        );*/

    }

    /**
     * Move a câmera para a localização desejada
     * @param latitude
     * @param longitude
     * @param zoom
     */
    private void goTolocation(double latitude, double longitude, float zoom){
        LatLng latLng = new LatLng(latitude, longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        googleMap.animateCamera(cameraUpdate);
    }

    /**
     * Converte enderço em latidute e longitude, ou vice-versa
     * ADAPTAR MÉTODO PARA O SAMU PROJECT
     */
    /*public void geoLocate(View view) {
        EditText et_location = (EditText) findViewById(R.id.et_location);
        String location = et_location.getText().toString();

        try{
            geocoder = new Geocoder(this, Locale.getDefault());
            listAddress = geocoder.getFromLocationName(location, 1);    //  No SAMUProject - getFromLocation
            address = listAddress.get(0);

            double latitude  = address.getLatitude();
            double longitude = address.getLongitude();
            String locality  = address.getLocality();

            Toast.makeText(this, locality, Toast.LENGTH_SHORT).show();
            goTolocation(latitude, longitude, 18);

            setMarker(locality, new LatLng(latitude, longitude));

        }catch(IOException e){
            e.printStackTrace();
        }
    }*/

    /**
     * Adiciona um Marker no mapa
     * @param location
     * @param snippet
     * @param latLng
     */
    public void setMarker(String location, String snippet, LatLng latLng){
        //  EVITA TER MAIS DE 1 MARKER
        if(marker != null){
            marker.remove();
        }

        markerOptions = new MarkerOptions()
                    .title(location)
                    .position(latLng)
                    .snippet(snippet)
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_user));
        marker = this.googleMap.addMarker(markerOptions);
        marker.showInfoWindow();
    }

    /**
     * Realiza a conexão com o Google API Client
     */
    public void googleApiClientConnect(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);

        //  CHECAR PERMISSÃO PARA O ANDROID M
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //  PROBLEMA NO GPS OU TA DESLIGADO
        if(location == null){
            Toast.makeText(this, "Can't get current location", Toast.LENGTH_LONG).show();
        }else{
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);

            goTolocation(latitude, longitude, 16);

            //  COLOCAR EM UM MÉTODO
            try{
                geocoder    = new Geocoder(this, Locale.getDefault());
                listAddress = geocoder.getFromLocation(latitude, longitude, 1);    //  No SAMUProject - getFromLocation
                address     = listAddress.get(0);
                String street   = address.getAddressLine(0);
                String locality = address.getLocality();
                setMarker(street, locality, latLng);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }


}
