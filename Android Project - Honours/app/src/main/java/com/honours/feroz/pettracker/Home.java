package com.honours.feroz.pettracker;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.LOCATION_SERVICE;


public class Home extends Fragment  {

    private View myview;
    private RecyclerView mPetsList;

    double currentLat, currentLon;
    private TextView titleSearchArea, noPetsFoundMsg;

    private DatabaseReference mDatabase;
    private Query query;
    private String mCountry, mPostcode;
    private LocationManager locationManager;

    private static final int REQUEST_LOCATION = 1;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;

    private FirebaseAnalytics mFirebaseAnalytics;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myview = inflater.inflate(R.layout.activity_home, container, false);
        return myview;
    }

    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //sets screen to stay in portrait mode
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        titleSearchArea = (TextView) getActivity().findViewById(R.id.titleSearchArea);
        noPetsFoundMsg = (TextView) getActivity().findViewById(R.id.noPetsFoundMsg);

        noPetsFoundMsg.setVisibility(View.GONE); //hides the message if no records found at start


        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation(); //Calls method to get current location of user
            //calls method to get the postcode from the current location
            getPostcode(currentLat, currentLon);
        }


titleSearchArea.setText("  Pets Reported In " + mPostcode);

titleSearchArea.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(getActivity());
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
        } catch (GooglePlayServicesNotAvailableException e) {
        }
    }
});


        mDatabase = FirebaseDatabase.getInstance().getReference().child("MissingPets");

        query = mDatabase.orderByChild("Postcode").equalTo(mPostcode);

        mPetsList = (RecyclerView) view.findViewById(R.id.pets_list);
        mPetsList.setHasFixedSize(true);
        mPetsList.setLayoutManager(new LinearLayoutManager(getActivity()));//vertical layout of list


    }

//Method to get the users current location
    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
            }
        }


    }

    //Method to get the first part of the postcode from the latitude and longitude
    private void getPostcode(Double latitude, Double longitude){
        //extracts postcode from users current location
        Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCountry =  addresses.get(0).getCountryCode();
        mPostcode = addresses.get(0).getPostalCode();

        if (mPostcode != null) {

            //splits spaces from the postcode to only retrieve the first part of it (before the space)
            String[] fields = mPostcode.split("\\s");

            mPostcode = mCountry + "," + fields[0];
        } else {
            mPostcode = "\n\nLocation Invalid" ;
            Toast.makeText(getActivity(), "INVALID LOCATION - NO AREA CODE FOUND (LOCATION TOO BROAD)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        //checks if the query returned any items if empty will show the no records found text
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    noPetsFoundMsg.setVisibility(View.GONE);
                } else{
                    noPetsFoundMsg.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        FirebaseRecyclerAdapter<PetCard, PetViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<PetCard, PetViewHolder>(

                PetCard.class,
                R.layout.pet_card,
                PetViewHolder.class,
                query

        ) {
            @Override
            protected void populateViewHolder(PetViewHolder viewHolder, PetCard model, int position) {

                final String postKey = getRef(position).getKey();

                viewHolder.setTitle(model.getType(), model.getName());
                viewHolder.setPetBreed(model.getBreed());
                viewHolder.setImage(getActivity().getApplicationContext(), model.getImage());


                //on click listener for the view
                viewHolder.mView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){

                        //Analytics collected when a user selects a missing pet
                        Bundle params = new Bundle();
                        params.putString("pet_id", postKey);
                        mFirebaseAnalytics.logEvent("pet_selected", params);

                        //starts the activity to view details of the missing pet selected, put extra: passes the petid in
                        Intent selectedPetIntent = new Intent(getActivity(), SelectedMisingPet.class);
                        selectedPetIntent.putExtra("post_id", postKey);
                        startActivity(selectedPetIntent);

                    }
                });
            }
        };


        mPetsList.setAdapter(firebaseRecyclerAdapter);

    }

    public static class PetViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public PetViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setTitle(String pet_type, String pet_name){

            TextView list_petTitle = (TextView) mView.findViewById(R.id.list_missingTitle);
            list_petTitle.setText("Missing " + pet_type + ": " + pet_name);

        }

        public void setPetBreed(String pet_breed){

            TextView list_breed = (TextView) mView.findViewById(R.id.list_petBreed);
            list_breed.setText(pet_breed);

        }

        public void setImage(Context ctx, String image){

            ImageView pet_image = (ImageView) mView.findViewById(R.id.list_petImage);
            Picasso.with(ctx).load(image).fit().centerCrop().into(pet_image);

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(getActivity(), data);

                Double latitude = place.getLatLng().latitude;
                Double longitude = place.getLatLng().longitude;

                //calls method to get the postcode from the selected location
                getPostcode(latitude, longitude);

                    titleSearchArea.setText("  Pets Reported In  " + mPostcode);
                    query = mDatabase.orderByChild("Postcode").equalTo(mPostcode);
                    onStart();
                }


            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }

}


