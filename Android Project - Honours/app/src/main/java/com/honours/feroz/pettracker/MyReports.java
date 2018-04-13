package com.honours.feroz.pettracker;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import android.content.Context;


public class MyReports extends Fragment  {

    View myview;
    private RecyclerView mPetsList;

    private DatabaseReference mDatabase;

    private TextView noReportsMsg;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListner;

    Query query;


    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myview = inflater.inflate(R.layout.activity_my_reports, container, false);
        return myview;
    }

    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //sets screen to stay in portait mode
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        noReportsMsg = (TextView) getActivity().findViewById(R.id.noReportsMsg);
        noReportsMsg.setVisibility(View.GONE); //hides the message if no records found at start

        mDatabase = FirebaseDatabase.getInstance().getReference().child("MissingPets");

         query = mDatabase.orderByChild("UserID").equalTo(user.getUid());

        mPetsList = (RecyclerView) view.findViewById(R.id.MYpets_list);
        mPetsList.setHasFixedSize(true);
        mPetsList.setLayoutManager(new LinearLayoutManager(getActivity()));//vertical layout of list

    }


    @Override
    public void onStart() {
        super.onStart();

        //checks if the query returned any items if empty will show the no records found text
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    noReportsMsg.setVisibility(View.GONE);
                } else{
                    noReportsMsg.setVisibility(View.VISIBLE);
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

                viewHolder.setTitle(model.getName());
                viewHolder.setPetBreed(model.getBreed());
                viewHolder.setPetDesc("Click to Edit or Delete Record");
                viewHolder.setImage(getActivity().getApplicationContext(), model.getImage());

                //on click listener for the view
                viewHolder.mView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        Intent selectedPetIntent = new Intent(getActivity(), EditSelectedPet.class);
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

        public void setTitle(String pet_name){

            TextView list_petTitle = (TextView) mView.findViewById(R.id.list_missingTitle);
            list_petTitle.setText(pet_name + " Reported Missing");

        }

        public void setPetBreed(String pet_breed){

            TextView list_breed = (TextView) mView.findViewById(R.id.list_petBreed);
            list_breed.setText(pet_breed);

        }

        public void setPetDesc(String pet_dec){

            TextView list_petDec = (TextView) mView.findViewById(R.id.list_cardBottom);
            list_petDec.setText(pet_dec);

        }

        public void setImage(Context ctx, String image){

            ImageView pet_image = (ImageView) mView.findViewById(R.id.list_petImage);
            Picasso.with(ctx).load(image).fit().centerCrop().into(pet_image);


        }



    }

}


