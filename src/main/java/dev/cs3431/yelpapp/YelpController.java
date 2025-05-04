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
    private Label countText;
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
    private TableColumn<Business, String> tipsColumn;
    @FXML
    private TableColumn<Business, String> latitudeColumn;
    @FXML
    private TableColumn<Business, String> longitudeColumn;
    @FXML
    private ComboBox<String> wifiComboBox;
    @FXML
    private ComboBox<Integer> priceComboBox;



    @FXML
    void initialize() {
        updateStates();
        stateComboBox.setOnAction(event -> {
            String state = stateComboBox.getValue();
            if (state != null) {
                updateCities(state);
            }
        });
        updateWifi();
        updatePriceRange();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("Address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("City"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("Stars"));
        tipsColumn.setCellValueFactory(new PropertyValueFactory<>("Tips"));
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

            ObservableList<Business> businesses = FXCollections.observableArrayList(getSimilarBusinesses(selected));
            ObservableList<String> categories = fetchCategoriesForBusiness(selected.getId());
            ObservableList<String> attributes = fetchAttributesForBusiness(selected.getId());

            controller.initData(selected.getName(), businesses, categories, attributes);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(businessTable.getScene().getWindow());
            dialog.setTitle("Business Details");

            Scene scene = new Scene(root, 850, 850);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
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
        List<String> categories = new ArrayList<>(categoryList.getSelectionModel().getSelectedItems());
        List<String> attributes = new ArrayList<>(attributeList.getSelectionModel().getSelectedItems());
        Integer price = priceComboBox.getSelectionModel().getSelectedItem();
        String wifi = wifiComboBox.getSelectionModel().getSelectedItem();
        List<Business> results = queryBusinesses(state, categories, attributes, city, wifi, price);
        businessTable.setItems(FXCollections.observableArrayList(results));
        countText.setText(results.size() + " results");

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
    private List<Business> queryBusinesses(String state, List<String> categories, List<String> attributes, String city, String wifi, Integer price) {
        List<Business> res = new ArrayList<>();
        if (state == null || city == null) return res;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);

            StringBuilder sql = new StringBuilder("""
            SELECT b.business_id, b.name, b.street_address, b.city, b.latitude, b.longitude, b.stars, b.num_tips
            FROM business b
        """);

            boolean hasCats = categories != null && !categories.isEmpty();
            boolean hasAttrs = attributes != null && !attributes.isEmpty();
            boolean hasWifi = wifi != null && !wifi.isEmpty();
            boolean hasPrice = price != null;

            if (hasCats) sql.append("JOIN category c ON b.business_id = c.business_id\n");

            if (hasAttrs) sql.append("JOIN attribute a ON b.business_id = a.business_id\n");

            if (hasWifi) {
                sql.append("""
                JOIN attribute aw ON b.business_id = aw.business_id
                """);
            }
            if (hasPrice) {
                sql.append("""
                JOIN attribute ap ON b.business_id = ap.business_id
                """);
            }

            sql.append("WHERE b.state = ? AND b.city = ?\n");

            List<Object> params = new ArrayList<>();
            params.add(state);
            params.add(city);

            if (hasCats) {
                sql.append("AND c.category_name IN (").append("?,".repeat(categories.size()));
                sql.setLength(sql.length() - 1);
                sql.append(")\n");
                params.addAll(categories);
            }

            if (hasAttrs) {
                sql.append("AND a.attribute_name IN (").append("?,".repeat(attributes.size()));
                sql.setLength(sql.length() - 1);
                sql.append(") AND a.value = 'True'\n");
                params.addAll(attributes);
            }

            if (hasWifi) {
                sql.append("AND aw.attribute_name = 'WiFi' AND aw.value = ?\n");
                params.add(wifi);
            }

            if (hasPrice) {
                sql.append("AND ap.attribute_name = 'RestaurantsPriceRange2' AND ap.value = ?\n");
                params.add(String.valueOf(price));
            }

            sql.append("""
            GROUP BY b.business_id, b.name, b.street_address, b.city, b.latitude, b.longitude, b.stars, b.num_tips
        """);

            if (hasCats) {
                sql.append("HAVING COUNT(DISTINCT c.category_name) = ").append(categories.size()).append("\n");
            }
            if (hasAttrs) {
                if (hasCats) {
                    sql.append("AND ");
                } else {
                    sql.append("HAVING ");
                }
                sql.append("COUNT(DISTINCT a.attribute_name) = ").append(attributes.size()).append("\n");
            }

            sql.append("ORDER BY b.name");

            PreparedStatement ps = connection.prepareStatement(sql.toString());

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

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

            ps.close();
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return res;
    }

    private void updateWifi() {

        ObservableList<String> wifi = FXCollections.observableArrayList();

        String categoryQuery = """
             SELECT DISTINCT attribute.value
             FROM attribute
             WHERE attribute.attribute_name = 'WiFi'
             ORDER BY attribute.value
         """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(categoryQuery)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                wifi.add(rs.getString("value"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        wifiComboBox.setItems(wifi);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @NotNull
    private void updatePriceRange() {

        ObservableList<Integer> prices = FXCollections.observableArrayList();

        String priceQuery = """
             SELECT DISTINCT attribute.value
             FROM attribute
             WHERE attribute.attribute_name = 'RestaurantsPriceRange2'
             ORDER BY attribute.value
         """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(priceQuery)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                prices.add(rs.getInt("value"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        priceComboBox.setItems(prices);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @NotNull
    private List<Business> getSimilarBusinesses(Business selected) {
        List<Business> res = new ArrayList<>();

        String sql = """
        SELECT Bus2.business_ID,
               Bus2.name,
               Bus2.street_address,
               Bus2.city,
               Bus2.stars,
               Bus2.num_tips,
               Bus2.latitude,
               Bus2.longitude,
               count_categories(Bus1.business_ID, Bus2.business_ID) AS rank
        FROM Business Bus1, Business Bus2
        WHERE Bus1.business_ID = ?
          AND Bus2.business_ID != Bus1.business_ID
          AND Bus2.zip_code = Bus1.zip_code
          AND geodistance(Bus1.latitude, Bus1.longitude, Bus2.latitude, Bus2.longitude) <= 20
          AND EXISTS (
              SELECT 1
              FROM Category Cat1
              JOIN Category Cat2 ON Cat1.category_name = Cat2.category_name
              WHERE Cat1.business_ID = Bus1.business_ID
                AND Cat2.business_ID = Bus2.business_ID
          )
        ORDER BY rank DESC
        LIMIT 20;
    """;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, selected.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Business b = new Business(
                        rs.getString("business_ID"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city"),
                        rs.getDouble("stars"),
                        rs.getInt("num_tips"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                );
                b.setRank(rs.getInt("rank")); 
                res.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }


    public static ObservableList<String> fetchCategoriesForBusiness(String businessId) {
        ObservableList<String> categories = FXCollections.observableArrayList();
        String sql = "SELECT category_name FROM category WHERE business_id = ? ORDER BY category_name";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, businessId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
    }

    public static ObservableList<String> fetchAttributesForBusiness(String businessId) {
        ObservableList<String> attributes = FXCollections.observableArrayList();
        String sql = """
        SELECT attribute_name, value
        FROM attribute
        WHERE business_id = ? AND value != 'False'
        ORDER BY attribute_name
    """;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, businessId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                attributes.add(rs.getString("attribute_name") + "(" + rs.getString("value")+ ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return attributes;
    }


}
