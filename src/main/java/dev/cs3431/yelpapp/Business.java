package querycrusher.yelpapp;

public class Business{
    private final String id;
    private final String name;
    private final String address;
    private final String city;
    private final Double stars;
    private final Double numTips;
    private final Double latitude;
    private final Double longitude;


    public Business(String id, String name, String address, String city, Double stars, Double numTips, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.stars = stars;
        this.numTips = numTips;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public Double getStars() {
        return stars;
    }
    public Double getNumTips() {
        return numTips;
    }
    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
}
