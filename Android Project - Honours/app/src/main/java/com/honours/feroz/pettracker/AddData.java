package com.honours.feroz.pettracker;

import android.app.Fragment;
import android.app.FragmentManager;
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
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.perf.FirebasePerformance;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import static android.app.Activity.RESULT_OK;


/**
 * Created by Feroz on 06/11/2017.
 */

public class AddData extends Fragment  {


    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //gets instance of user
    private Button mUploadBtn;
    private Button mlastLocationBtn;
    private EditText mPetName;
    private EditText mPetType;
    private EditText mPetAge;
    private EditText mPetBreed;
    private EditText mPetColour;
    private EditText mPetDescription;
    private Double mlatitude, mlongitude;
    private  String mCountry, mPostcode;
    byte[] compressedImage;

    private ProgressDialog mProgress;

    private Uri mImageuri = null;
    private Uri downloadUrl;
    private ImageButton mAddImage;
    private StorageReference mStorage;

    private FirebaseAnalytics mFirebaseAnalytics;


    int PLACE_PICKER_REQUEST = 1;
    private static final int GALLERY_REQUEST = 2;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_data, container, false);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //sets screen to stay in portait mode
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //instance of components
        mUploadBtn = (Button) view.findViewById(R.id.UploadBtn);
        mlastLocationBtn = (Button) view.findViewById(R.id.last_locationBtn);
        mPetName = (EditText) view.findViewById(R.id.add_petName);
        mPetType = (EditText) view.findViewById(R.id.add_pettypeTxt);
        mPetAge = (EditText) view.findViewById(R.id.add_petAgetxt);
        mPetBreed = (EditText) view.findViewById(R.id.add_petbreedTxt);
        mPetColour = (EditText) view.findViewById(R.id.add_petcolourTxt);
        mPetDescription = (EditText) view.findViewById(R.id.add_petDescriptionTxt);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        mProgress = new ProgressDialog(getActivity());
        mProgress.setCancelable(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.reload();

        DatabaseReference userDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mAddImage = (ImageButton) view.findViewById(R.id.getImage);

        mStorage = FirebaseStorage.getInstance().getReference();

        mlastLocationBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    Intent intent = builder.build(getActivity());
                    startActivityForResult(intent, PLACE_PICKER_REQUEST); //starts the activity for the place picker
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }

            }

        });

        mAddImage.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,GALLERY_REQUEST);
            }
        });


        //upload button, calls method to upload the data the user eneters to firebase
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadData();

            }


        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(getActivity(), data);
                mlastLocationBtn.setText("Selected: " + place.getName());
                mlatitude = place.getLatLng().latitude;
                mlongitude = place.getLatLng().longitude;


                //gets postcode from latLan
                Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(mlatitude, mlongitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses != null && addresses.size()>0) {

                    mCountry =  addresses.get(0).getCountryCode();
                    mPostcode = addresses.get(0).getPostalCode();

                    //checks
                    if(mPostcode != null) {
                        //splits spaces from the postcode to only retrieve the first part of it
                        String[] fields = mPostcode.split("\\s");

                        mPostcode = mCountry + "," + fields[0];
                    } else {
                        mPostcode = null;
                        Toast.makeText(getActivity(), "CAN'T GET VALID POSTCODE CODE FROM SELECTED LOCATION", Toast.LENGTH_LONG).show();
                        mlastLocationBtn.setText("LAST SEEN LOCATION");
                    }
                } else {
                    mPostcode = null;
                    Toast.makeText(getActivity(), "CAN'T GET VALID ADDRESS OR POSTCODE", Toast.LENGTH_LONG).show();
                    mlastLocationBtn.setText("LAST SEEN LOCATION");
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
                        .start(getActivity().getApplicationContext(), this);
            }
        }

        //gets the cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mImageuri = result.getUri();
                mAddImage.setImageURI(mImageuri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    private void uploadData(){
        //starts progress dialog to let the user know it is creating the account
        mProgress.setMessage("Uploading Data ...");
        mProgress.setCancelable(false);
        mProgress.show();


        final String petName_val = mPetName.getText().toString();
        final String petType_val = mPetType.getText().toString();
        final String petAge_string = mPetAge.getText().toString();
        final String petBreed_val = mPetBreed.getText().toString();
        final String petColour_val = mPetColour.getText().toString();
        final String petDescription_val = mPetDescription.getText().toString();



        if(TextUtils.isEmpty(petType_val) || TextUtils.isEmpty(petAge_string) || TextUtils.isEmpty(petBreed_val)
                || TextUtils.isEmpty(petColour_val) || TextUtils.isEmpty(petDescription_val)
                || TextUtils.isEmpty(petName_val) ){

            Toast.makeText(getActivity(), "Make Sure The Fields Are Not Empty", Toast.LENGTH_LONG).show();
            mProgress.hide();

        } else if(mPostcode == null) {
            Toast.makeText(getActivity(), "Please Select a valid Last Location", Toast.LENGTH_LONG).show();
            mProgress.hide();
        } else if (mImageuri == null){
            Toast.makeText(getActivity(), "Please Select a Image", Toast.LENGTH_LONG).show();
            mProgress.hide();
        }
        else{
            final int petAge_val = Integer.parseInt(mPetAge.getText().toString());

            mProgress.setMessage("Uploading ...");
            mProgress.show();

            compressImage();

            //first make sure image is not large even after compression
            //prevents extremely large files being uploaded
            //if the file is still large a message will be shown to the user and the data will not upload
            final long imageSizeKB = compressedImage.length / 1024;


            if (imageSizeKB < 150) {

                //shows alertbox to let users know that their data uploaded will be visible to other users
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
                builder.setMessage("Notice \n\n" +
                        "All data entered will be visible to all other users to help find your pet including the location you have selected" +
                        "\n\nThis will be used to help users locate your missing pet\n\n"
                        + "Your Email, Name and Phone Number will be visible to other users so you can be contacted " +
                        "if a user finds your missing pet\n\n" +
                        "After upload you can delete your pet data at anytime");
                builder.setCancelable(false);
                builder.setPositiveButton("I Agree", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {



                        // Write a message to the database
                        FirebaseDatabase database = FirebaseDatabase.getInstance();

                        final DatabaseReference myRef = database.getReference("MissingPets").push();
                        final DatabaseReference GeoRef = database.getReference("Locations");

                        //records a trace to monitor how long it takes for Firebase to add data uploaded to the database
                        final Trace addTrace = FirebasePerformance.getInstance().newTrace("add_Data_trace");
                        final long startTime = System.currentTimeMillis();
                        addTrace.start();

                        //name of file in storage will be the pet reference (for a unique file name)
                        StorageReference filePath = mStorage.child("images").child(myRef.getKey().toString());
                        filePath.putBytes(compressedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                downloadUrl = taskSnapshot.getDownloadUrl();


                                GeoFire geoFire = new GeoFire(GeoRef);

                                myRef.child("Name").setValue(petName_val);
                                myRef.child("Type").setValue(petType_val);
                                myRef.child("Age").setValue(petAge_val);
                                myRef.child("Breed").setValue(petBreed_val);
                                myRef.child("Colour").setValue(petColour_val);
                                myRef.child("Description").setValue(petDescription_val);
                                myRef.child("UserID").setValue(user.getUid());
                                geoFire.setLocation(myRef.getKey(), new GeoLocation(mlatitude, mlongitude));
                                myRef.child("Image").setValue(downloadUrl.toString());
                                myRef.child("Postcode").setValue(mPostcode);


                                long timeTaken = System.currentTimeMillis() - startTime;
                                //Analytics collected when a user updates the location of their pet
                                Bundle params = new Bundle();
                                params.putString("time_taken", Long.toString(timeTaken));
                                //ads image size as parameter
                                params.putString("image_size", imageSizeKB + "KB");

                                mFirebaseAnalytics.logEvent("updated_pet_location", params);

                                addTrace.stop();

                            }
                        });


                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, new Home()).commit();


                        NavigationView navigationView1 = (NavigationView) getActivity().findViewById(R.id.nav_view);
                        navigationView1.setCheckedItem(R.id.nav_home); //highlights home from navigation drawer


                    }
                });
                builder.setNegativeButton("I Disagree", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getActivity(), "Upload Cancelled", Toast.LENGTH_LONG).show();
                    }
                });
                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();


            } else{
                Toast.makeText(getActivity(), "Can't be uploaded: File Size is too big", Toast.LENGTH_SHORT).show();
            }
            mProgress.hide();

        }
    }

    //Method to reduce the image file size before uploading it to the database
    //saves space and speeds up the application
    private void compressImage() {

        //gets the image stream from the uri
        InputStream imageStream = null;
        try {
            imageStream = getActivity().getContentResolver().openInputStream(
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