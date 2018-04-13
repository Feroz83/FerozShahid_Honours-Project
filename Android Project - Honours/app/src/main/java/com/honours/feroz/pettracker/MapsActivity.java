package com.honours.feroz.pettracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    private GoogleMap mMap;


    private String PetName;
    private  String PetType;
    double latitude, longitude;

    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;

    private Map<String,Marker> markers;


private    FirebaseDatabase database = FirebaseDatabase.getInstance();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle("MAP");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //TO DO HANDLE ALERT MESSAGE
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation();
        }


        // setup markers
        this.markers = new HashMap<String, Marker>();



        DatabaseReference GeoRef = database.getReference("Locations");
        GeoFire geoFire = new GeoFire(GeoRef);

            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latitude, longitude), 50);


            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(final String key, final GeoLocation location) {

                    DatabaseReference petDatabase = database.getReference("MissingPet").child(key);

                    petDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            PetName = dataSnapshot.child("Name").getValue(String.class);
                            PetType = dataSnapshot.child("Type").getValue(String.class);

                            LatLng petLocation = new LatLng(location.latitude, location.longitude);
                            Marker marker = mMap.addMarker(new MarkerOptions().position(petLocation).title("Missing Pet: ")
                                    .snippet("Click Here To View"));
                            marker.setTag(key);
                            markers.put(key, marker);

                            setTitle(marker);

                        }


                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });

                }

                @Override
                public void onKeyExited(String key) {

                    Marker marker = markers.get(key);
                    if (marker != null) {
                        marker.remove();
                        markers.remove(key);
                    }

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    Marker marker = markers.get(key);
                    if (marker != null) {
                        LatLng movedLocation = new LatLng(location.latitude, location.longitude);
                        marker.setPosition(movedLocation);
                    }
                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });

        }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnInfoWindowClickListener(this);

        //Checks permissions for location
        //moves the camera of the map closer to the current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {

                String selectedLoc = getIntent().getExtras().getString("selected_loc"); //gets the extra that was passed from the last screen
                LatLng currentLatLon = null;

                //if the key word 'none' was passed in when starting this activity it will use the current location the map camera
                if (selectedLoc.equals("none")) {

                    currentLatLon = new LatLng(latitude, longitude);

                } else if (!selectedLoc.equals("none")) {

                    //extracts the location from the string by splitting the comma in the latitue and logitute
                    String[] latlongVal = selectedLoc.split(",");
                    double selectedLatitude = Double.parseDouble(latlongVal[0]);
                    double selectedLongitude = Double.parseDouble(latlongVal[1]);

                    currentLatLon = new LatLng(selectedLatitude, selectedLongitude);

                    // Instantiates a new CircleOptions object and defines the center and radius
                    CircleOptions circleOptions = new CircleOptions()
                            .center(currentLatLon)
                            .strokeColor(R.color.colorPrimary)
                            .radius(1000); // In meters

// Get back the mutable Circle
                    Circle circle = mMap.addCircle(circleOptions);


                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLon, 13));
                mMap.setMyLocationEnabled(true);
            }

            }
        }



    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }


    }

    private void setTitle(Marker marker){

        if (PetName != null) {
            marker.setTitle(PetName);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final String postKey = marker.getTag().toString();
        Intent selectedPetIntent = new Intent(MapsActivity.this, SelectedMisingPet.class);
        selectedPetIntent.putExtra("post_id", postKey);
        startActivity(selectedPetIntent);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }


}
