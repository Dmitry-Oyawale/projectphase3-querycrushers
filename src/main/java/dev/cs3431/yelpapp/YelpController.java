package dev.cs3431.yelpapp;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class YelpController {
    private final Dotenv env = Dotenv.load();
    private final String url = env.get("JDBC_URL");
    private final String user = env.get("JDBC_USER");
    private final String pass = env.get("JDBC_PASS");

    @FXML private ComboBox<String> stateComboBox, cityComboBox, wifiComboBox, openStatusComboBox;
    @FXML private ComboBox<Integer> priceComboBox, resultLimitComboBox;
    @FXML private TableView<Business> businessTable;
    @FXML private TableColumn<Business, String> nameColumn, addressColumn, cityColumn, starsColumn, tipsColumn, latitudeColumn, longitudeColumn;
    @FXML private Button searchButton, clearButton;
    @FXML private Label countText;

    @FXML
    public void initialize() {
        setupTableColumns();
        populateComboBoxes();
        configureListeners();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("Address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("City"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("Stars"));
        tipsColumn.setCellValueFactory(new PropertyValueFactory<>("Tips"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("Longitude"));
    }

    private void populateComboBoxes() {
        stateComboBox.setItems(runQuery("SELECT DISTINCT state FROM business ORDER BY state", rs -> rs.getString("state")));
        wifiComboBox.setItems(FXCollections.observableArrayList("free", "paid", "no"));
        priceComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        openStatusComboBox.setItems(FXCollections.observableArrayList("All", "Open", "Closed"));
        resultLimitComboBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        resultLimitComboBox.getSelectionModel().select(Integer.valueOf(20));
    }

    private void configureListeners() {
        stateComboBox.setOnAction(e -> cityComboBox.setItems(
            runQuery("SELECT DISTINCT city FROM business WHERE state = ? ORDER BY city",
                rs -> rs.getString("city"), stateComboBox.getValue())));

        cityComboBox.setOnAction(e -> {
            updateCategories(stateComboBox.getValue(), cityComboBox.getValue());
            updateAttributes(stateComboBox.getValue(), cityComboBox.getValue());
        });

        searchButton.setOnAction(e -> executeSearch());
        clearButton.setOnAction(e -> resetFilters());
    }

    private void executeSearch() {
        String state = stateComboBox.getValue();
        String city = cityComboBox.getValue();
        String wifi = wifiComboBox.getValue();
        Integer price = priceComboBox.getValue();
        String status = openStatusComboBox.getValue();
        Integer limit = resultLimitComboBox.getValue();

        if (state == null || limit == null) {
            countText.setText("Missing required fields");
            return;
        }

        StringBuilder sql = new StringBuilder("""
            SELECT b.business_id, b.name, b.street_address, b.city, b.latitude, b.longitude, b.stars, b.total_tips
            FROM business b WHERE b.state = ?
        """);
        List<Object> params = new java.util.ArrayList<>(List.of(state));

        if (city != null) {
            sql.append(" AND b.city = ?");
            params.add(city);
        }
        if (status != null && !status.equals("All")) {
            sql.append(" AND b.is_open = ?");
            params.add(status.equals("Open") ? 1 : 0);
        }
        if (wifi != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM attribute WHERE attr_name = 'WiFi' AND value = ? AND business_id = b.business_id)");
            params.add(wifi);
        }
        if (price != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM attribute WHERE attr_name = 'RestaurantsPriceRange2' AND value = ? AND business_id = b.business_id)");
            params.add(price.toString());
        }

        sql.append(" ORDER BY b.stars DESC LIMIT ?");
        params.add(limit);

        ObservableList<Business> results = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
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

    private <T> ObservableList<T> runQuery(String query, SQLMapper<T> mapper, String... args) {
        ObservableList<T> list = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(query)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapper.map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void updateCategories(String state, String city) {}
    private void updateAttributes(String state, String city) {}

    private void resetFilters() {
        for (ComboBox<?> cb : Arrays.asList(stateComboBox, cityComboBox, wifiComboBox, priceComboBox, openStatusComboBox)) {
            cb.getSelectionModel().clearSelection();
        }
        resultLimitComboBox.getSelectionModel().select(Integer.valueOf(20));
        businessTable.getItems().clear();
        countText.setText("0 results");
    }

    @FunctionalInterface
    interface SQLMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
