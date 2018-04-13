package com.honours.feroz.pettracker;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.perf.FirebasePerformance;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class EditSelectedPet extends AppCompatActivity {

    private Button mDeletePetBtn, mUpdatePetBtn, mUpdateLocation;
    private ImageButton mUpdateImage;
    private EditText mUpdateDescription;
    private DatabaseReference mDatabasePets, mDatabaseLocations;
    private String Image;
    private Uri mImageuri = null;
    private StorageReference mStorage;
    int PLACE_PICKER_REQUEST = 1;
    private static final int GALLERY_REQUEST = 2;
    private Double mNewlatitude, mNewlongitude;
    private String mCountry, mPostcode;
    private Double mlatitude, mlongitude;
    private String postKey = null;
    private byte[] compressedImage;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_selected_pet);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Details");

        //sets screen to stay in portait mode
        EditSelectedPet.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(EditSelectedPet.this);

        mDeletePetBtn = (Button) findViewById(R.id.tempDelete);
        mUpdatePetBtn = (Button) findViewById(R.id.UpdateBtn);
        mUpdateLocation = (Button) findViewById(R.id.update_locationBtn);
        mUpdateDescription = (EditText) findViewById(R.id.update_petDescriptionTxt);
        mUpdateImage = (ImageButton) findViewById(R.id.updateImage);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.reload();


        //Had issue where keyboard keep opening on start of activity, this prevents the keyboard
        //from opening and makes sure it is hidden at the start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mStorage = FirebaseStorage.getInstance().getReference();

        mUpdateImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK);

                intent.setType("image/*");

                startActivityForResult(intent, GALLERY_REQUEST);

            }
        });

        //Firebase References for both MissingPets & Locations databases
        mDatabasePets = FirebaseDatabase.getInstance().getReference().child("MissingPets");
        mDatabaseLocations = FirebaseDatabase.getInstance().getReference().child("Locations");

        postKey = getIntent().getExtras().getString("post_id");

        mDatabasePets.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //first checks if the data exists
                if (dataSnapshot.exists()) {
                    Image = dataSnapshot.child("Image").getValue().toString(); //gets the image URL
                    String Description = dataSnapshot.child("Description").getValue().toString();

                    Picasso.with(EditSelectedPet.this).load(Image).fit().centerCrop().into(mUpdateImage);
                    mUpdateDescription.setText(Description);
                } else {
                    //if the data no longer exists closes the page and lets the user know
                    Toast.makeText(EditSelectedPet.this, "RECORD WAS JUST DELETED", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });


        mDatabaseLocations.child(postKey).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    mlongitude = Double.parseDouble(dataSnapshot.child("1").getValue().toString());
                    mlatitude = Double.parseDouble(dataSnapshot.child("0").getValue().toString());
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUpdatePetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                //first delete the old image
//                StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Image);
//                storageRef.delete(); //deletes the image related to the missing value

                String striDesc = mUpdateDescription.getText().toString();


                //checks to make sure the description is not updated to no value so checks for empty fields before trying to update
                if (striDesc.isEmpty()) {
                    Toast.makeText(EditSelectedPet.this, "Can't Have An Empty Description", Toast.LENGTH_LONG).show();
                } else if (!striDesc.isEmpty()) {

                    //records a trace to monitor how long it takes for Firebase to update text in the description
                    final Trace textDescTrace = FirebasePerformance.getInstance().newTrace("update_desc_text_trace");
                    textDescTrace.start();

                    mDatabasePets.child(postKey).child("Description").setValue(striDesc);

                    Toast.makeText(EditSelectedPet.this, "Data Has Been Updated", Toast.LENGTH_LONG).show();

                    textDescTrace.stop();

                    //first checks if the there was a new location updated before trying to update the location
                    if (mPostcode != null) {

                        //records a trace to monitor how long it takes for Firebase & geofire to a update the location
                        final Trace locationTrace = FirebasePerformance.getInstance().newTrace("update_location_trace");
                        locationTrace.start();

                        long startTime = System.currentTimeMillis();

                        mDatabaseLocations.child(postKey).child("l").child("1").setValue(mNewlongitude);
                        mDatabaseLocations.child(postKey).child("l").child("0").setValue(mNewlatitude);

                        mDatabasePets.child(postKey).child("Postcode").setValue(mPostcode);

                        long timeTaken = System.currentTimeMillis() - startTime;

                        //Analytics collected when a user updates the location of their pet
                        Bundle params = new Bundle();
                        params.putString("time_taken", Long.toString(timeTaken));

                        mFirebaseAnalytics.logEvent("updated_pet_location", params);

                        locationTrace.stop();
                    }


                    //checks the the image is not null i.e if a new image was selected
                    if (mImageuri != null) {


                        final ProgressDialog progress = new ProgressDialog(EditSelectedPet.this);
                        progress.setCancelable(false);

                        progress.setMessage("Compressing Image ...");

                        progress.show();

                        compressImage();



                        //first make sure image is not large even after compression
                        //prevents extremely large files being uploaded
                        //if the file is still large a message will be shown to the user and the data will not upload
                        final long imageSizeKB = compressedImage.length / 1024;

                        if (imageSizeKB < 150) {

                            //records a trace to monitor how long it takes for Firebase & geofire to a update an image
                            final Trace imageTrace = FirebasePerformance.getInstance().newTrace("update_image_trace");
                            imageTrace.start();

                            final long startTime = System.currentTimeMillis();

                            //Deletes the old image from storage
                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Image);
                            //gets the name of the file
                            String Filename = storageRef.getName();

                            storageRef.delete(); //deletes the image related to the missing value

                            //uploads the new image to the storage
                            StorageReference filePath = mStorage.child("images").child(Filename);
                            filePath.putBytes(compressedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                    //updates the missing pet database with the url of the new image
                                    mDatabasePets.child(postKey).child("Image").setValue(downloadUrl.toString());

                                    long timeTaken = System.currentTimeMillis() - startTime;
                                    //Analytics collected when a user updates the location of their pet
                                    Bundle params = new Bundle();
                                    params.putString("time_taken", Long.toString(timeTaken));
                                    //ads image size as parameter
                                    params.putString("image_size", imageSizeKB + "KB");

                                    mFirebaseAnalytics.logEvent("updated_pet_location", params);

                                    imageTrace.stop();

                                }
                            });

                        } else {
                            Toast.makeText(EditSelectedPet.this, "Can't be updated: File Size is too big", Toast.LENGTH_SHORT).show();
                        }

                        progress.hide();

                    }
                }

            }

        });


        mUpdateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Sets the Place Picker at the location of the missing pet from the database
                LatLngBounds PET_LOCATION = new LatLngBounds(
                        new LatLng(mlatitude, mlongitude), new LatLng(mlatitude, mlongitude));

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder().setLatLngBounds(PET_LOCATION);

                try {
                    Intent intent = builder.build(EditSelectedPet.this);
                    startActivityForResult(intent, PLACE_PICKER_REQUEST); //starts the activity for the place picker
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        mDeletePetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                //Creates Alert dialog to first confirm if the user really wants to delete
                //the missing pet details
                AlertDialog.Builder builder = new AlertDialog.Builder(EditSelectedPet.this);
                builder.setMessage("Are you sure you want to delete?");
                builder.setCancelable(false);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //records a trace to monitor how long it takes for Firebase to delete a record, this includes deleting the
                        //missing pet database, the location database and image associated with the record
                        final Trace deleteTrace = FirebasePerformance.getInstance().newTrace("delete_data_trace");
                        deleteTrace.start();

                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Image);
                        storageRef.delete(); //deletes the image related to the missing value

                        //deletes the databases stored for the selected missing animal
                        mDatabaseLocations.child(postKey).removeValue();
                        mDatabasePets.child(postKey).removeValue();

                        deleteTrace.stop();

                        onBackPressed(); //exits the activity once the item was deleted
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(), "Canceled", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();


            }
        });


    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(EditSelectedPet.this, data);
                mUpdateLocation.setText("UPDATED: " + place.getName());
                mNewlatitude = place.getLatLng().latitude;
                mNewlongitude = place.getLatLng().longitude;


                //gets postcode from latLan
                Geocoder geocoder = new Geocoder(EditSelectedPet.this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(mNewlatitude, mNewlongitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses != null && addresses.size()>0) {

                    mCountry = addresses.get(0).getCountryCode();
                    mPostcode = addresses.get(0).getPostalCode();

                    //checks
                    if (mPostcode != null) {

                        //splits spaces from the postcode to only retrieve the first part of it
                        String[] fields = mPostcode.split("\\s");

                        mPostcode = mCountry + "," + fields[0];
                    } else {
                        mPostcode = null;
                        Toast.makeText(EditSelectedPet.this, "CAN'T GET VALID POSTCODE FROM SELECTED LOCATION", Toast.LENGTH_LONG).show();
                        mUpdateLocation.setText("LAST SEEN LOCATION");
                    }
                } else {
                    mPostcode = null;
                    Toast.makeText(EditSelectedPet.this, "CAN'T GET VALID ADDRESS OR POSTCODE", Toast.LENGTH_LONG).show();
                    mUpdateLocation.setText("LAST SEEN LOCATION");
                }

            }
        }

        //gets the result of the selected image from the user
        if(requestCode == GALLERY_REQUEST){
            if (resultCode == RESULT_OK){

                Uri img = data.getData();
                // start picker to get image to crop
                CropImage.activity(img)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(3,2)
                        .start(this);
            }
        }

        //gets the cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mImageuri = result.getUri();
                mUpdateImage.setImageURI(mImageuri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    //Method to reduce the image file size before uploading it to the database
    //saves space and speeds up the application
    private void compressImage() {

        //gets the image stream from the uri
        InputStream imageStream = null;
        try {
            imageStream = this.getContentResolver().openInputStream(
                    mImageuri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //gets the image as a bitmap
        Bitmap bmp = BitmapFactory.decodeStream(imageStream);


        //new byte array to store the compressed image
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //scales the image size but maintains its aspect ratio by using .getheight()
        int nh = (int) ( bmp.getHeight() * (512.0 / bmp.getWidth()) );
        Bitmap scaled = Bitmap.createScaledBitmap(bmp, 512, nh, true);

        //gets the stream and compresses it to a JPEG, reduces the quality to 80%
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream);

        //stores the compressed image byte array
        compressedImage = stream.toByteArray();

        try {
            stream.close();
            stream = null;
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

}