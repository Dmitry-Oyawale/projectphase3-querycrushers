package dev.cs3431.yelpapp;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class BusinessDetailsController {

    @FXML private Label titleLabel;
    @FXML private TableView<Business> similarBusinesses;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;

    @FXML public void initialize (){
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
    }

    public void initData (String businessName,
                          ObservableList<Business> similars) {
        titleLabel.setText("Similar to: " + businessName);
        similarBusinesses.setItems(similars);
    }
}
