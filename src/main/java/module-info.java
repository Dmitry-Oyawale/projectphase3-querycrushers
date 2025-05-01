module dev.cs3431.yelpapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.dotenv;
    requires java.sql;


    opens dev.cs3431.yelpapp to javafx.fxml;
    exports dev.cs3431.yelpapp;
}