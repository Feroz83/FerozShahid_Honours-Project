package com.honours.feroz.pettracker;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.perf.FirebasePerformance;

public class Register extends AppCompatActivity {

    private EditText mRegisterEmailField, mRegisterPasswordField, mRegisterRePasswordField
                     ,mRegisterNameField, mRegisterPhoneNumberField;

    private TextView mRegistererrorLabel;
    private Button mCreateAccountBtn;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgress;
    private FirebaseAnalytics mFirebaseAnalytics;

    //monitors how long it takes for a user to get registered with Firebase
    final Trace registeredTrace = FirebasePerformance.getInstance().newTrace("registered_trace");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(Register.this);

        mRegisterEmailField = (EditText) findViewById(R.id.registerEmailField);
        mRegisterPasswordField = (EditText) findViewById(R.id.registerPasswordField);
        mRegisterRePasswordField = (EditText) findViewById(R.id.registerRePasswordField);
        mRegistererrorLabel = (TextView) findViewById(R.id.RegisterErrorLabel);
        mCreateAccountBtn = (Button) findViewById(R.id.createAccountBtn);
        mRegisterNameField = (EditText) findViewById(R.id.registerNameField);
        mRegisterPhoneNumberField = (EditText) findViewById(R.id.registerPhoneNumberField);

        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(false);


        //If the create account button is selected the createAccount method is started
        mCreateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //shows alertbox to let users know that their data will be stored on a server
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(Register.this);
                builder.setMessage("Notice \n\n" +
                        "Any information collected such as credentials or missing pet details will be stored on a server and will compile with the data protection act." +
                        "\n\n You can delete all your data by selecting the delete account in your account settings\n\n"
                        + "Analytics will also be monitored for testing purposes\n\n"
                        + "Your Email, Name and Phone Number will be visible to other users when you report a missing pet," +
                        " this will allow you to be contacted if a missing pet was found");
                builder.setCancelable(false);
                builder.setPositiveButton("I Agree", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        createAccount();
                    }
                });
                builder.setNegativeButton("I Disagree", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(Register.this, "Note: You will not be able to use the app if you do not agree", Toast.LENGTH_LONG).show();
                    }
                });
                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();


            }
        });

    }


    private void createAccount(){

        //starts progress dialog to let the user know it is creating the account
        mProgress.setMessage("Creating a new Account ...");
        mProgress.show();

        //gets a string of the fields entered by the user
        String email = mRegisterEmailField.getText().toString();
        String password = mRegisterPasswordField.getText().toString();
        String rePassword  = mRegisterRePasswordField.getText().toString();
        final String name = mRegisterNameField.getText().toString();
        final String phoneNumber = mRegisterPhoneNumberField.getText().toString();

        //gets the length of the password
        int passLength = mRegisterPasswordField.getText().length();
        int phoneLength = mRegisterPhoneNumberField.getText().length();


        //checks to see if any of the fields entered by the user is blank
        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(rePassword) || TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)){
            mProgress.dismiss();
            //adds an error message in a Toast and displays it in a label
            //to tell the user to fill in all fields
            Toast.makeText(Register.this, "Please Fill In All Fields", Toast.LENGTH_LONG).show();
            mRegistererrorLabel.setText("Fill in all fields");
        }
        else { //next checks if both the password and re-entered password matches
            if (password.equals(rePassword) ) {

                if(isEmail(email)) { //next checks the email is valid through the isEmail function

                    if(passLength > 7) { //next checks if the password length is over 7 characters
                        //Creates a new user using the username and password chosen by the user

                        if(phoneLength == 11) { //checks that the phone number is 11 characters
                                                //which will be a valid UK number

                            //starts monitoring the time of a user started the registered process
                            registeredTrace.start();

                            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        //User registered successfully, any error labels are cleared
                                        mRegistererrorLabel.setText("");

                                        //Firebase user creation is limited towards storing
                                        //an email and password so a database will store the rest
                                        //calls method to add additional user details to a database
                                        createUserDatabase(name, phoneNumber);

                                    } else {
                                        mProgress.dismiss();
                                        //Lets User Know if their sign in was unsucesfull by adding a Toast
                                        //and sets the error label to display the exception
                                        Toast.makeText(Register.this, "Failed to create user: " + task.getException().getMessage().toString(), Toast.LENGTH_LONG).show();
                                        mRegistererrorLabel.setText("Failed to create user: " + task.getException().getMessage().toString());
                                    }
                                }
                            });
                        } else{
                            mProgress.dismiss(); //end the progress dialogue
                            //lets the user know the phonr number is not valid
                            //and sets the error label to show the problem
                            Toast.makeText(Register.this, "Phone Number is not a valid UK number (11 digits)", Toast.LENGTH_LONG).show();
                            mRegistererrorLabel.setText("Phone Number Invalid: phone number needs to be a valid UK number (11 digits)");

                        }
                    } else {
                        mProgress.dismiss(); //end the progress dialogue
                        //lets the user know the password is too short through a Toast
                        //and sets the error label to show the problem
                        Toast.makeText(Register.this, "Password is too short", Toast.LENGTH_LONG).show();
                        mRegistererrorLabel.setText("Password is too short: password needs to be longer than 7 characters");
                    }

                } else{
                    mProgress.dismiss(); //end the progress dialogue
                    //lets the user know the email is not valid through the Toast message
                    //and sets the label to show the problem
                    Toast.makeText(Register.this, "Not A Valid Email", Toast.LENGTH_LONG).show();
                    mRegistererrorLabel.setText("Enter a valid email address");
                }

            } else {
                mProgress.dismiss(); //end the progress dialogue
                //lets the user know the passwords don't match through the Toast message
                //and sets the label to show the problem
                Toast.makeText(Register.this, "Passwords Do Not Match", Toast.LENGTH_LONG).show();
                mRegistererrorLabel.setText("Passwords need to match");
            }

        }
    }

    // method that checks to see if the text entered is in the form of an email
    boolean isEmail(CharSequence email) {
        //checks that the email matches an email address pattern e.g. email@domain.com
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    //method to add additional user details to a seperate database
    private void createUserDatabase(String name, String phoneNumber){

        //gets the current user (the one that was just created)
        FirebaseUser user = mAuth.getCurrentUser();

        //adds an instance of the firebase database with the database path
        //of Users/<UserID> (user ID of the user that was just created)
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Users")
                .child(user.getUid());

        //adds the name and phone number and email to the user database
        //stored under the path of the user ID
        myRef.child("Name").setValue(name);
        myRef.child("Phone Number").setValue(phoneNumber);
        myRef.child("Email").setValue(user.getEmail());

        //Analytics collected when a user updates the location of their pet
        Bundle params = new Bundle();
        params.putString("user_id", user.getUid());

        mFirebaseAnalytics.logEvent("new_user_registered", params);

        registeredTrace.stop();

        mProgress.dismiss(); //end the progress dialogue

    }

}
