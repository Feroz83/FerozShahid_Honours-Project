package com.honours.feroz.pettracker;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MyAccount extends Fragment {


    private TextView mAccountName, mAccountEmail, mAccountPhoneNumber;
    private Button mdeleteAccount;
    private DatabaseReference mDatabase;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_my_account, container, false);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //sets screen to stay in portait mode
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mAccountName = (TextView) view.findViewById(R.id.myAccountName);
        mAccountEmail = (TextView) view.findViewById(R.id.myAccountEmail);
        mAccountPhoneNumber = (TextView) view.findViewById(R.id.myAccountPhoneNumber);
        mdeleteAccount = (Button) view.findViewById(R.id.deleteAccount);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        mAccountEmail.setText(user.getEmail());

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");


//gets the user details from the Firebase database
        mDatabase.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()) {
                    String name = dataSnapshot.child("Name").getValue().toString();
                    String phoneNumber = dataSnapshot.child("Phone Number").getValue().toString();

                    mAccountName.setText(name);
                    mAccountPhoneNumber.setText(phoneNumber);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mdeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Creates Alert dialog to first confirm if the user really wants to delete
                //your account and all data associated with it
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Are you sure you want to delete your account?\n\n"
                + "This will delete all data associated with your account,\n\n"
                + "any missing pets reported will also be deleted");
                builder.setCancelable(false);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                deleteAccount();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        Toast.makeText(getActivity(), "Canceled", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

    }

    private void deleteAccount(){

        //monitors how long it takes for a user to get registered with Firebase
        final Trace deleteAcountTrace = FirebasePerformance.getInstance().newTrace("account_deleted_trace");

        //starts monitoring the time of a user started the delete account process
        deleteAcountTrace.start();

        //starts progress bar to let the user know that the deleting process
        //is being carried out
       final ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setCancelable(false);

        progress.setMessage("Deleting Account ...");

        progress.show();

        //gets current user logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //gets database pets that match the current users pets that were reported
        final DatabaseReference  petDatabase = FirebaseDatabase.getInstance().getReference().child("MissingPets");
        Query petQuery = petDatabase.orderByChild("UserID").equalTo(user.getUid());

        final DatabaseReference  locationDatabase = FirebaseDatabase.getInstance().getReference().child("Locations");

        petQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child: dataSnapshot.getChildren()) {

                    petDatabase.child(child.getKey()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {

                                String Image = dataSnapshot.child("Image").getValue().toString();

                                StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Image);
                                storageRef.delete(); //deletes the image related to the missing value
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    //deletes the databases stored for the selected missing animal
                    //and the location details of the pet
                    locationDatabase.child(child.getKey()).removeValue();
                    petDatabase.child(child.getKey()).removeValue();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //finds database of user details
        DatabaseReference  userDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(user.getUid());

        //removes the user from Firebase Authentication
        userDatabase.removeValue();

   user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
       @Override
       public void onSuccess(Void aVoid) {

           progress.hide();

           //stops recording the length of the delete trace
           deleteAcountTrace.stop();

           //takes the user back t the login activity once their account was deleted
           Intent intent = new Intent(getActivity(), MainActivity.class);
           startActivity(intent);
       }
   });



    }
}
