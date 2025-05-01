package dev.cs3431.yelpapp;

public class Business {
    private final String id;
    private final String name;
    private final String address;
    private final String city;

    public Business (String id, String name, String address, String city) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }
}

