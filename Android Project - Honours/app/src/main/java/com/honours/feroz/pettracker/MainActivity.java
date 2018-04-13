package com.honours.feroz.pettracker;

        import android.app.ProgressDialog;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.Uri;
        import android.support.annotation.NonNull;
        import android.support.v4.app.ActivityCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.text.TextUtils;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.google.android.gms.ads.AdRequest;
        import com.google.android.gms.ads.AdView;
        import com.google.android.gms.ads.MobileAds;
        import com.google.android.gms.common.GoogleApiAvailability;
        import com.google.android.gms.tasks.OnCompleteListener;
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.analytics.FirebaseAnalytics;
        import com.google.firebase.auth.AuthResult;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.perf.FirebasePerformance;
        import com.google.firebase.perf.metrics.Trace;

public class MainActivity extends AppCompatActivity {


    private EditText mEmailField, mPasswordField;
    private TextView mLoginerrorLabel;
    private Button mLoginBtn, mRegisterBtn;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListner;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mEmailField = (EditText)findViewById(R.id.emailField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);
        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mRegisterBtn = (Button) findViewById(R.id.registerBtn);
        mLoginerrorLabel = (TextView) findViewById(R.id.LoginErrorLabel);

        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(false);

        mAuthListner = new FirebaseAuth.AuthStateListener() {


            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                //checks to see first if there is a user already logged in
                //by checking that the current user is not null
                if(firebaseAuth.getCurrentUser()  != null){
                    startActivity(new Intent(MainActivity.this, NavigationBar.class));
                }

            }
        };

        //Starts the signin method if the user selects the login button
        mLoginBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                signIn();
            }
        });

        //Opens a new activity for a register page if the user selects the register button
        mRegisterBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //starts a new activity for the register page
                startActivity(new Intent(MainActivity.this, Register.class));

            }
        });

    }



    protected void onStart(){
        super.onStart();

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //if the required version of google play services is not available
                        //tells the user with a dialog and then takes them to the play store to update it
                        if (!task.isSuccessful()) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms&hl=en")));
                        } else {

                            //asks for permission for location access at the start
                            ActivityCompat.requestPermissions(MainActivity.this , new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                            mAuth.addAuthStateListener(mAuthListner); //checks for the authentication status (if user is already signed in)
                            mLoginerrorLabel.setText(""); //clears the label at the start so no login errors are still shown

                        }
                    }
                });
    }

    private void signIn(){

        //shows a progress dialog to indicate it is logging in
        mProgress.setMessage("Logging In ...");
        mProgress.show();

        //gets a string of the username and password typed in by the user
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        //checks if the fields are empty
        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
            //shows a Toast onto the screen to let the user know they haven't filled in all the fields
            Toast.makeText(MainActivity.this, "Make Sure The Fields Are Not Empty", Toast.LENGTH_LONG).show();
            mLoginerrorLabel.setText("Fill in Empty Fields");
            mProgress.dismiss(); //ends the progress dialogue

        } else {
            //signs the user in using the entered email and password through firebase
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    //checks if the login was unsuccessful
                    if (!task.isSuccessful()) {
                        //displays signin failed as a Toast message
                        Toast.makeText(MainActivity.this, "Sign In Failed", Toast.LENGTH_LONG).show();

                        //sets a label to display the failed login error along with the exception
                        mLoginerrorLabel.setText("Failed to login: " + task.getException().getMessage().toString());
                        mProgress.dismiss(); //ends the progress dialogue
                    }

                }
            });
        }


    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //handles if user tries to deny permission for location
        switch(requestCode)
        {
            case 1:
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    //shows alertbox to tell the user why location permission is needed in this app
                    //if they deny the permisssion
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("This App Requires Location Permissions\n\n" +
                            "Locations are needed to show missing pets near you\n");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            //ask again for location permission
                            ActivityCompat.requestPermissions(MainActivity.this , new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        }
                    });

                    android.support.v7.app.AlertDialog alert = builder.create();
                    alert.show();
                }

                break;
        }
    }

}
