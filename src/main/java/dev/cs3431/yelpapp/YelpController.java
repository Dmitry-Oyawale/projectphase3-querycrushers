package dev.cs3431.yelpapp;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class YelpController {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String JDBC_URL = dotenv.get("JDBC_URL");
    private static final String JDBC_USER = dotenv.get("JDBC_USER");
    private static final String JDBC_PASS = dotenv.get("JDBC_PASS");
    private Connection connection;
    @FXML
    private Label searchText;
    @FXML
    private ComboBox<String> stateComboBox;
    @FXML
    private ComboBox<String> cityComboBox;
    @FXML
    private Button filterButton;
    @FXML
    private ListView<String> categoryList;
    @FXML
    private ListView<String> attributeList;
    @FXML
    private Button searchButton;
    @FXML
    private TableView<Business> businessTable;
    @FXML
    private TableColumn<Business, String> nameColumn;
    @FXML
    private TableColumn<Business, String> addressColumn;
    @FXML
    private TableColumn<Business, String> cityColumn;
    @FXML
    private TableColumn<Business, String> starsColumn;
    @FXML
    private TableColumn<Business, String> tipColumn;
    @FXML
    private TableColumn<Business, String> latitudeColumn;
    @FXML
    private TableColumn<Business, String> longitudeColumn;



    @FXML
    void initialize() {
        updateStates();
        stateComboBox.setOnAction(event -> {
            String state = stateComboBox.getValue();
            if (state != null) {
                updateCities(state);
            }
        });
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("Address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("City"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("Stars"));
        tipColumn.setCellValueFactory(new PropertyValueFactory<>("Tips"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Longitude"));

        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryList.setItems(FXCollections.observableArrayList());

        attributeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attributeList.setItems(FXCollections.observableArrayList());

//        stateComboBox.getSelectionModel()
//                .selectedItemProperty()
//                .addListener((observable, oldState, newState) -> {
//                    if (newState != null) {
//                        updateCategories(newState);
//                    }
//                });
        filterButton.setOnAction(event -> {
            String selectedState = stateComboBox.getSelectionModel().getSelectedItem();
            String selectedCity = cityComboBox.getSelectionModel().getSelectedItem();
            updateCategories(selectedState, selectedCity);
            updateAttributes(selectedState, selectedCity);
        });
        searchButton.setOnAction(event -> {searchBusinesses();});
        businessTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Business selected = businessTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadBusinessPage(selected);
                }
            }
        });
    }

    private void loadBusinessPage(Business selected){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(YelpApp.class.getResource("businessDetails.fxml"));
            Parent root = fxmlLoader.load();
            BusinessDetailsController controller = fxmlLoader.getController();

            ObservableList<Business> businesses = FXCollections.observableArrayList(
                    getSimilarBusinesses(selected)
            );
            controller.initData(selected.getName(), businesses);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(businessTable.getScene().getWindow());
            dialog.setTitle("Business Details");

            Scene scene = new Scene(root, 695, 700);
            scene.getStylesheets()
                    .add(getClass().getResource("/styles/styles.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateStates() {
        ObservableList<String> states = FXCollections.observableArrayList();

        String stateQuery = """
            SELECT DISTINCT state
            FROM business
            ORDER BY state
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(stateQuery)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                states.add(rs.getString("state"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        stateComboBox.setItems(states);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCities(String state) {
        ObservableList<String> cities = FXCollections.observableArrayList();

        String stateQuery = """
            SELECT DISTINCT state, city
            FROM business
            WHERE state = ?
            ORDER BY city
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(stateQuery)) {
            ps.setString(1, state);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        cityComboBox.setItems(cities);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void searchBusinesses() {
        String state = stateComboBox.getSelectionModel().getSelectedItem();
        String city = cityComboBox.getSelectionModel().getSelectedItem();
        List<String> cats = new ArrayList<>(categoryList.getSelectionModel().getSelectedItems());
        List<Business> results = queryBusinesses(state, cats, city);
        businessTable.setItems(FXCollections.observableArrayList(results));
    }

    private void updateCategories(String state, String city) {
        // String state = stateComboBox.getSelectionModel().getSelectedItem();
        if (state == null || city == null) {
            return;
        }

        ObservableList<String> categories = FXCollections.observableArrayList();

        String categoryQuery = """
             SELECT DISTINCT category.category_name
             FROM category
             JOIN business ON business.business_id = category.business_id
             WHERE business.state = ? AND business.city = ?
             ORDER BY category.category_name
         """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(categoryQuery)) {
            ps.setString(1, state);
            ps.setString(2, city);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        categoryList.setItems(categories);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void updateAttributes(String state, String city) {
        // String state = stateComboBox.getSelectionModel().getSelectedItem();
        if (state == null || city == null) {
            return;
        }

        ObservableList<String> attributes = FXCollections.observableArrayList();

        String categoryQuery = """
             SELECT DISTINCT attribute.attribute_name
             FROM attribute
             JOIN business ON business.business_id = attribute.business_id
             WHERE business.state = ? AND business.city = ? AND attribute.value = 'True'
             ORDER BY attribute.attribute_name
         """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(categoryQuery)) {
            ps.setString(1, state);
            ps.setString(2, city);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                attributes.add(rs.getString("attribute_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        attributeList.setItems(attributes);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }




    @NotNull
    private List<Business> queryBusinesses(String state, List<String> categories, String city) {
        List<Business> res = new ArrayList<>();

        String businessQuery = """
            SELECT business_id, name, street_address, city, latitude, longitude, stars, num_tips\s
            FROM business
            WHERE business.state = ? AND business.city = ?
        """;

        businessQuery = businessQuery.concat("ORDER BY name");

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(businessQuery)) {
            //int count = 1;
            ps.setString(1, state);
            ps.setString(2, city);
            //count++;
            /*
            for(String cat: categories){
            ps.setString(count, cat);
            count++;}
             */
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new Business(
                        rs.getString("business_id"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city"),
                        rs.getDouble("stars"),
                        rs.getInt("num_tips"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    @NotNull
    private List<Business> getSimilarBusinesses(Business selected) {
        List<Business> res = new ArrayList<>();

        String stateQuery = """
            SELECT business_id, name, street_address, city, latitude, longitude, stars, num_tips\s
            FROM Business
            WHERE business_id = ?
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(stateQuery)) {
            ps.setString(1, selected.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new Business(
                        rs.getString("business_id"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city"),
                        rs.getDouble("stars"), rs.getInt("num_tips"), rs.getDouble("latitude"), rs.getDouble("longitude")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

}
