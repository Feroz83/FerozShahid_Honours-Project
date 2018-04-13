package com.honours.feroz.pettracker;


/**
 * Created by Feroz on 20/11/2017.
 */

public class PetCard {

    private String Name, Type, Breed , Image;

    public PetCard(){

    }

    public PetCard(String name, String type, String breed, String image) {
        Name = name;
        Type = type;
        Breed = breed;
        Image = image;

    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getType() {
        return Type;
    }

    public String getBreed() {
        return Breed;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }
}
