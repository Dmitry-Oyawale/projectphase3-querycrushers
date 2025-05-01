package dev.cs3431.yelpapp;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class YelpController {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String JDBC_URL = dotenv.get("JDBC_URL");
    private static final String JDBC_USER = dotenv.get("JDBC_USER");
    private static final String JDBC_PASS = dotenv.get("JDBC_PASS");
    private Connection connection;

    @FXML private Label searchText;
    @FXML private ComboBox<String> stateComboBox;
    @FXML private Button filterButton;
    @FXML private ListView<String> categoryList;
    @FXML private Button searchButton;
    @FXML private TableView<Business> businessTable;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;

    @FXML void initialize() {
        updateStates();

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));

        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryList.setItems(FXCollections.observableArrayList());
        stateComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldState, newState) -> {
                    if (newState != null) {
                        updateCategories(newState);
                    }
                });
        filterButton.setOnAction(event -> {updateCategories(stateComboBox.getSelectionModel().getSelectedItem());});
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
        try {
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

        try {  connection.close(); } catch (SQLException e) { e.printStackTrace();}
    }

    private void updateCategories(String state) {
        //String state = stateComboBox.getSelectionModel().getSelectedItem();
        if (state == null) {
            return;
        }

        ObservableList<String> categories = FXCollections.observableArrayList();
        String stateQuery = """
            SELECT DISTINCT Category.category_name
            FROM Category
            JOIN Business ON Business.business_ID = Category.business_ID
            WHERE Business.state = ?
            ORDER BY Category.category_name
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
                categories.add(rs.getString("category_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        categoryList.setItems(categories);

        try {  connection.close(); } catch (SQLException e) { e.printStackTrace();}
    }

    private void searchBusinesses() {
        String state = stateComboBox.getSelectionModel().getSelectedItem();
        List<String> cats = new ArrayList<>(categoryList.getSelectionModel().getSelectedItems());
        List<Business> results = queryBusinesses(state, cats);
        businessTable.setItems(FXCollections.observableArrayList(results));
    }

    private List<Business> queryBusinesses(String state, List<String> categories) {

        List<Business> res = new ArrayList<>();

        String businessQuery = """
            SELECT DISTINCT business_ID, name, street_address, city, latitude, longitude, stars, num_tips\s
            FROM Business
            WHERE Business.state = ? 
        """;
        // you can iterate over all selected categories as follows. You should add more conditions to your query dynamically for the selected catgories
        /*
        for (String cat: categories){
            businessQuery = businessQuery.concat(" AND ....");
        } */
        businessQuery = businessQuery.concat(" ORDER BY name");
        // to debug, you can print your query string and make sure that it is constructed correctly.
        
        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        try (PreparedStatement ps = connection.prepareStatement(businessQuery)) {
            int count =1; //count will keep track of the parameter count for the query
            ps.setString(count, state);
            count++;
            /*
            for (String cat: categories) {
                ps.setString(count, cat)
                count++;
            } */
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new Business(
                        rs.getString("business_ID"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {  connection.close(); } catch (SQLException e) { e.printStackTrace();}
        return res;
    }

    private List<Business> getSimilarBusinesses(Business selected) {

        List<Business> res = new ArrayList<>();

        String stateQuery = """
            SELECT business_ID, name, street_address, city
            FROM Business
            WHERE business_ID = ? 
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
                        rs.getString("business_ID"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {  connection.close(); } catch (SQLException e) { e.printStackTrace();}

        return res;
    }

}