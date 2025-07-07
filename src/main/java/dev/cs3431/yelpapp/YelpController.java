package dev.cs3431.yelpapp;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class YelpController {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String JDBC_URL = dotenv.get("JDBC_URL");
    private static final String JDBC_USER = dotenv.get("JDBC_USER");
    private static final String JDBC_PASS = dotenv.get("JDBC_PASS");

    @FXML private ComboBox<String> stateComboBox;
    @FXML private ComboBox<String> cityComboBox;
    @FXML private ComboBox<String> wifiComboBox;
    @FXML private ComboBox<Integer> priceComboBox;
    @FXML private ComboBox<String> openStatusComboBox;
    @FXML private ComboBox<Integer> resultLimitComboBox;

    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> attributeList;

    @FXML private TableView<Business> businessTable;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;
    @FXML private TableColumn<Business, String> starsColumn;
    @FXML private TableColumn<Business, String> tipsColumn;
    @FXML private TableColumn<Business, String> latitudeColumn;
    @FXML private TableColumn<Business, String> longitudeColumn;

    @FXML private Button searchButton;
    @FXML private Button clearButton;
    @FXML private Label countText;

    @FXML
    void initialize() {
        updateStates();
        updateWifi();
        updatePriceRange();

        openStatusComboBox.setItems(FXCollections.observableArrayList("All", "Open", "Closed"));
        resultLimitComboBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        resultLimitComboBox.getSelectionModel().select(20);

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("Address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("City"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("Stars"));
        tipsColumn.setCellValueFactory(new PropertyValueFactory<>("Tips"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Longitude"));

        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attributeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        stateComboBox.setOnAction(e -> updateCities(stateComboBox.getValue()));
        cityComboBox.setOnAction(e -> {
            updateCategories(stateComboBox.getValue(), cityComboBox.getValue());
            updateAttributes(stateComboBox.getValue(), cityComboBox.getValue());
        });

        searchButton.setOnAction(e -> searchBusinesses());
        clearButton.setOnAction(e -> clearFilters());
    }

    private void searchBusinesses() {
        String state = stateComboBox.getValue();
        String city = cityComboBox.getValue();
        String wifi = wifiComboBox.getValue();
        Integer price = priceComboBox.getValue();
        String openStatus = openStatusComboBox.getValue();
        Integer limit = resultLimitComboBox.getValue();

        if (state == null || limit == null) {
            countText.setText("Please select state and result limit.");
            return;
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT b.business_id, b.name, b.street_address, b.city, b.latitude, b.longitude, b.stars, b.total_tips
            FROM business b
            WHERE b.state = ?
        """);
        params.add(state);

        if (city != null) {
            sql.append(" AND b.city = ?");
            params.add(city);
        }

        if (openStatus != null && !openStatus.equals("All")) {
            sql.append(" AND b.is_open = ?");
            params.add(openStatus.equals("Open") ? 1 : 0);
        }

        if (wifi != null) {
            sql.append("""
                AND EXISTS (
                    SELECT 1 FROM attribute a
                    WHERE a.business_id = b.business_id
                    AND a.attr_name = 'WiFi'
                    AND a.value = ?
                )
            """);
            params.add(wifi);
        }

        if (price != null) {
            sql.append("""
                AND EXISTS (
                    SELECT 1 FROM attribute a
                    WHERE a.business_id = b.business_id
                    AND a.attr_name = 'RestaurantsPriceRange2'
                    AND a.value = ?
                )
            """);
            params.add(String.valueOf(price));
        }

        sql.append(" ORDER BY b.stars DESC LIMIT ?");
        params.add(limit);

        ObservableList<Business> results = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Business(
                        rs.getString("business_id"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city"),
                        rs.getDouble("stars"),
                        rs.getInt("total_tips"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        businessTable.setItems(results);
        countText.setText(results.size() + " results 🔍");
    }

    private void updateStates() {
        ObservableList<String> states = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT state FROM business ORDER BY state")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                states.add(rs.getString("state"));
            }
            stateComboBox.setItems(states);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCities(String state) {
        ObservableList<String> cities = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT city FROM business WHERE state = ? ORDER BY city")) {
            ps.setString(1, state);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
            cityComboBox.setItems(cities);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // No-op stubs to trip up copiers
    private void updateCategories(String state, String city) {}
    private void updateAttributes(String state, String city) {}

    private void updateWifi() {
        wifiComboBox.setItems(FXCollections.observableArrayList("free", "paid", "no"));
    }

    private void updatePriceRange() {
        priceComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
    }

    private void clearFilters() {
        stateComboBox.getSelectionModel().clearSelection();
        cityComboBox.getSelectionModel().clearSelection();
        categoryList.getSelectionModel().clearSelection();
        attributeList.getSelectionModel().clearSelection();
        wifiComboBox.getSelectionModel().clearSelection();
        priceComboBox.getSelectionModel().clearSelection();
        openStatusComboBox.getSelectionModel().clearSelection();
        resultLimitComboBox.getSelectionModel().select(20);
        businessTable.setItems(FXCollections.observableArrayList());
        countText.setText("0 results");
    }
}
