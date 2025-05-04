package dev.cs3431.yelpapp;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class BusinessDetailsController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> categoryListA;
    @FXML private ListView<String> attributeListA;
    @FXML private TableView<Business> similarBusinesses;
    @FXML private TableColumn<Business, Integer> rankColumn;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;
    @FXML private TableColumn<Business, String> starsColumn;
    @FXML private TableColumn<Business, Double> latitudeColumn;
    @FXML private TableColumn<Business, Double> longitudeColumn;

    @FXML
    public void initialize () {
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("stars"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("longitude"));
    }

    public void initData(String businessName,
                         ObservableList<Business> similars,
                         ObservableList<String> categories,
                         ObservableList<String> attributes) {
        titleLabel.setText("Similar to: " + businessName);
        similarBusinesses.setItems(similars);
        categoryListA.setItems(categories);
        attributeListA.setItems(attributes);
    }
}
