package com.honours.feroz.pettracker;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class SelectedMisingPet extends AppCompatActivity {

    private Button mPetLocation, mContactBtn, mBackBtn;
    private TextView mAge, mPetType, mBreed, mColour, mDesc;
    private ImageView mPetImage;

    String postKey = null;
    String Lat;
    String Lon;
    private DatabaseReference mDatabasePets, mDatabaseLocations, mDatabaseUsers;

    private String OwnerID, OwnerName, OwnerEmail, OwnerNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selected_mising_pet);


        mPetType = (TextView) findViewById(R.id.SelectedPetType);
        mAge = (TextView) findViewById(R.id.selectedPetAge);
        mBreed = (TextView) findViewById(R.id.SelectedBreed);
        mColour = (TextView) findViewById(R.id.SelectedColour);
        mDesc = (TextView) findViewById(R.id.SelectedPetDesc);
        mPetImage = (ImageView) findViewById(R.id.selectedPetImage);
        mPetLocation = (Button) findViewById(R.id.SelectedPetLocation);
        mContactBtn = (Button) findViewById(R.id.ContactOwnerBtn);
        mBackBtn = (Button) findViewById(R.id.BackBtnSelected);

mDatabasePets = FirebaseDatabase.getInstance().getReference().child("MissingPets");

        postKey = getIntent().getExtras().getString("post_id");


        mDatabasePets.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //first checks if the data still exists before attempting to do anything with it
                if(dataSnapshot.exists()){
                    String Name = dataSnapshot.child("Name").getValue().toString();
                    String Age = dataSnapshot.child("Age").getValue().toString();
                    String Type = dataSnapshot.child("Type").getValue().toString();
                    String Breed = dataSnapshot.child("Breed").getValue().toString();
                    String Colour = dataSnapshot.child("Colour").getValue().toString();
                    String Desc = dataSnapshot.child("Description").getValue().toString();
                    String Image = dataSnapshot.child("Image").getValue(String.class);

                    OwnerID = dataSnapshot.child("UserID").getValue().toString();

                    getOwnerDetails();


                    Picasso.with(SelectedMisingPet.this).load(Image).fit().centerCrop().into(mPetImage);

                    mAge.setText(Age);
                    mPetType.setText(" MISSING " + Type + " : " + Name);
                    mBreed.setText(Breed);
                    mColour.setText(Colour);
                    mDesc.setText(Desc);

                } else{
                    //if the data no longer exists closes the page and lets the user know
                    Toast.makeText(SelectedMisingPet.this, "RECORD WAS JUST DELETED", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mDatabaseLocations = FirebaseDatabase.getInstance().getReference().child("Locations");


        mDatabaseLocations.child(postKey).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Lon = dataSnapshot.child("1").getValue().toString();
                    Lat = dataSnapshot.child("0").getValue().toString();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        mPetLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:<" + Lat  + ">,<" + Lon + ">?q=<" + Lat  + ">,<" + Lon + ">( Missing Pet (Last Seen)  )"));
//                startActivity(intent);

                String selectedLocation = Lat + "," + Lon;

                startActivity(new Intent(SelectedMisingPet.this, MapsActivity.class).putExtra("selected_loc",selectedLocation));


            }
        });

        mContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] options = {
                        "Email",
                        "Phone"
                };

                //needs to check if the details from the database is not null as it is asynchronously retried
                if (OwnerName != null && OwnerEmail != null && OwnerNumber != null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(SelectedMisingPet.this);
                    builder.setTitle("Contact " + OwnerName)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:

                                            Intent sendEmail = new Intent(Intent.ACTION_SEND);
                                            sendEmail.setType("text/plain");
                                            sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{OwnerEmail});
                                            sendEmail.putExtra(Intent.EXTRA_SUBJECT, "Missing Pet: UPDATE");
                                            startActivity(sendEmail);

                                            break;
                                        case 1:

                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                            intent.setData(Uri.parse("tel:" + OwnerNumber)); //TO CHANGE AND ADD PHONE NUMBER FROM DATABASE
                                            startActivity(intent);

                                            break;
                                    }

                                }
                            });

                    AlertDialog alert = builder.create();
                    alert.show();

                }
            }
        });

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    public  void getOwnerDetails(){

        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");

        mDatabaseUsers.child(OwnerID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                OwnerName = dataSnapshot.child("Name").getValue().toString();
                OwnerEmail = dataSnapshot.child("Email").getValue().toString();
                OwnerNumber = dataSnapshot.child("Phone Number").getValue().toString();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


}
