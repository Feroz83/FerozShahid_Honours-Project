package com.honours.feroz.pettracker;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NavigationBar extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListner;
    private DatabaseReference mDatabase;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Home");

        //Sets the navigation Drawer email text to the user logged in
        NavigationView navigationView2 = (NavigationView) findViewById(R.id.nav_view);
        View v = navigationView2.getHeaderView(0);
        TextView mNav_Email_Display = (TextView ) v.findViewById(R.id.nav_drawer_email);
        final TextView mNav_Name_Display = (TextView) v.findViewById(R.id.nav_drawer_name) ;

        //gets the database path for the user that is currently logged in
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(user.getUid());

        //adds the email to the navigation bar for the current user logged in
        mNav_Email_Display.setText(user.getEmail());

        //adds the name of the current user to the navigation bar
        //retrieved from the database
        mDatabase.child("Name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue().toString();
                mNav_Name_Display.setText(name);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mAuth = FirebaseAuth.getInstance();

        NavigationView navigationView1 = (NavigationView) findViewById(R.id.nav_view);
        navigationView1.setCheckedItem(R.id.nav_home); //highlights home as selected from navigation drawer
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, new Home()).commit(); //starts the home fragment


        //checks if user is authenticated if not then sends them back to MainActivity for the login screen
        mAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if(firebaseAuth.getCurrentUser()  == null){

                    startActivity(new Intent(NavigationBar.this, MainActivity.class));

                }

            }
        };

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();



        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    //allows you to change the action Bar title
    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentManager fragmentManager = getFragmentManager();

        if (id == R.id.nav_home) {

            getSupportActionBar().setTitle("Home");

            fragmentManager.beginTransaction().replace(R.id.content_frame, new Home()).commit();

        } else if (id == R.id.nav_add_data) {

            getSupportActionBar().setTitle("Report Missing");

            fragmentManager.beginTransaction().replace(R.id.content_frame, new AddData()).commit();


        } else if (id == R.id.nav_myreports) {

            getSupportActionBar().setTitle("My Reports");

            fragmentManager.beginTransaction().replace(R.id.content_frame, new MyReports()).commit();

        } else if (id == R.id.myAccount) {

            getSupportActionBar().setTitle("My Account");

            fragmentManager.beginTransaction().replace(R.id.content_frame, new MyAccount()).commit();

        } else if (id == R.id.nav_map) {

            startActivity(new Intent(NavigationBar.this, MapsActivity.class).putExtra("selected_loc","none"));

        } else if (id == R.id.nav_signout) {

            //signs the user out through Firebase
            FirebaseAuth.getInstance().signOut();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart(){
        super.onStart();

        mAuth.addAuthStateListener(mAuthListner);
    }
}
