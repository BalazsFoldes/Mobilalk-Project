package com.example.betaprojekt;

public class Medicine {
    private String id;
    private String name;
    private String info;
    private String price;
    private String available;
    private int imageResource;
    private int cartedCount;

    public Medicine(String name, String info, String price, String available, int imageResource, int cartedCount) {
        this.name = name;
        this.info = info;
        this.price = price;
        this.available = available;
        this.imageResource = imageResource;
        this.cartedCount = cartedCount;
    }

    public Medicine() {

    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public String getPrice() {
        return price;
    }

    public String getAvailable() {
        return available;
    }

    public int getImageResource() {
        return imageResource;
    }
    public int getCartedCount(){return cartedCount;}

    public String _getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }
}