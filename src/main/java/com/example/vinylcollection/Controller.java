package com.example.vinylcollection;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.*;
import java.util.*;

public class Controller implements Initializable {

    @FXML
    private TextField artistTF, albumTF, yearTF, editTF, searchTabTF;

    @FXML
    private ComboBox<String> homeGenreCB, homeSizeCB, homeConditionCB, editCB, searchTabCB;

    @FXML
    private Tab homeTab, editTab, searchTab;

    @FXML
    private TableView<Object[]> homeTable, editTable, searchTable;

    private final Map<String, Integer> genreMap = new HashMap<>();
    private final Map<String, Integer> sizeMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeEditTab();

        populateGenreAndSizeMaps();

        populateConditionCB(homeConditionCB);

        initializeSearchTab();
    }

    @FXML
    void onHomeTabSelected(Event event) {
        if (homeTab.isSelected()) {
            populateHomeComboBoxes();
            populateTable(homeTable, "VINYLS");
        }
    }

    @FXML
    void onEditTabSelected(Event event) {
        if (editTab.isSelected()) {

        }
    }

    @FXML
    void onSearchTabSelected(Event event) {
        if (searchTab.isSelected()) {
//            populateTable(searchTable, "VINYLS");
        }
    }

    @FXML
    void onHomeAddBtnClicked(ActionEvent event) {
        String artistName = artistTF.getText();
        String albumTitle = albumTF.getText();
        String releaseYearText = yearTF.getText();
        String genreName = homeGenreCB.getValue();
        String sizeName = homeSizeCB.getValue();
        String condition = homeConditionCB.getValue();

        if (artistName.isEmpty() || albumTitle.isEmpty() || releaseYearText.isEmpty() ||
                genreName == null || sizeName == null || condition == null) {
            showAlert("Please fill in all fields.");
            return;
        }

        int releaseYear;
        try {
            releaseYear = Integer.parseInt(releaseYearText);
        } catch (NumberFormatException e) {
            showAlert("Invalid release year.");
            return;
        }

        int genreId = getId(genreName, genreMap);
        int sizeId = getId(sizeName, sizeMap);

        if (genreId == -1 || sizeId == -1) {
            showAlert("Please go to the Edit tab and add at least one genre and one size.");
            return;
        }

        populateGenreAndSizeMaps();

        insertIntoVinyls(artistName, albumTitle, releaseYear, genreId, sizeId, condition);
        populateTable(homeTable, "VINYLS");

        clearTextFields();
    }

    @FXML
    void onHomeModifyBtn(ActionEvent event) throws SQLException {
        Object[] selectedRow = homeTable.getSelectionModel().getSelectedItem();

        if (selectedRow != null) {
            Dialog<ButtonType> dialog = createHomeDialog(selectedRow);
            dialog.showAndWait();
        } else {
            showAlert("Please select a row.");
        }
    }

    @FXML
    void onHomeDeleteBtn(ActionEvent event) {
        Object[] selectedRow = homeTable.getSelectionModel().getSelectedItem();
        if (selectedRow != null) {
            int vinylId = Integer.parseInt(selectedRow[0].toString());
            deleteFromTable("VINYLS", vinylId);
            populateTable(homeTable, "VINYLS");
        } else {
            showAlert("Please select a row to delete.");
        }
    }

    @FXML
    void onEditAddBtn(ActionEvent event) {
        String tableName = editCB.getValue();
        String newValue = editTF.getText();

        insertIntoTable(tableName, newValue);

        populateGenreAndSizeMaps();

        editTF.clear();
    }

    @FXML
    void onEditModifyBtn(ActionEvent event) {
        String tableName = editCB.getValue();
        Object[] selectedRow = editTable.getSelectionModel().getSelectedItem();

        if (selectedRow != null) {
            Dialog<ButtonType> dialog = createEditDialog(selectedRow, tableName);
            dialog.showAndWait();
        } else {
            showAlert("Please select a row.");
        }
    }

    @FXML
    void onEditDeleteBtn(ActionEvent event) {
        String selectedTableName = editCB.getValue();
        if (selectedTableName == null) {
            showAlert("Please select a table to delete from.");
            return;
        }

        Object[] selectedRow = editTable.getSelectionModel().getSelectedItem();
        if (selectedRow != null) {
            int id = Integer.parseInt(selectedRow[0].toString());
            deleteFromTable(selectedTableName, id);
            populateTable(editTable, selectedTableName.toUpperCase());
        } else {
            showAlert("Please select a row to delete.");
        }
    }

    @FXML
    private void onEditComboBoxSelected(ActionEvent event) {
        String selectedItem = editCB.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (selectedItem.equals("Genres")) {
                populateTable(editTable, "GENRES");
            } else if (selectedItem.equals("Sizes")) {
                populateTable(editTable, "SIZES");
            }
        }
    }

    @FXML
    void onSearchBtn(ActionEvent event) {
        String selectedItem = searchTabCB.getValue();
        String searchKeyword = searchTabTF.getText();

        if (selectedItem == null) {
            showAlert("Please select a search option.");
            return;
        }

        String baseQuery = generateBaseQuery();
        String query = buildSearchQuery(selectedItem, baseQuery, searchKeyword);

        assert query != null;
        populateSearchTable(searchTable, query, searchKeyword);
    }

    // FXML Initialization Methods
    public void initializeEditTab() {
        editCB.getItems().addAll("Genres", "Sizes");
        editCB.setOnAction(this::onEditComboBoxSelected);
    }

    public void initializeSearchTab() {
        searchTabCB.getItems().addAll("Latest releases", "Old Skool Anthems", "By Genre", "By Size");
        searchTabTF.setVisible(false);
        searchTabCB.setOnAction(this::toggleTextFieldVisibility);
    }

    // Utility Methods
    private void clearTextFields() {
        artistTF.clear();
        albumTF.clear();
        yearTF.clear();
    }

    private void showAlert(String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    private int getId(String value, Map<String, Integer> map) {
        return map.getOrDefault(value, -1);
    }

    private void toggleTextFieldVisibility(ActionEvent event) {
        String selectedOption = searchTabCB.getValue();
        searchTabTF.setVisible(selectedOption != null && (selectedOption.equals("By Genre") || selectedOption.equals("By Size")));
    }

    // Database Interaction Methods
    private void populateMap(String tableName, Map<String, Integer> map) {
        String tableNameSubStr = tableName.substring(0, tableName.length() - 1);
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {
            while (resultSet.next()) {
                int id = resultSet.getInt(tableNameSubStr + "_ID");
                String value = resultSet.getString(tableNameSubStr);
                map.put(value, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateBaseQuery() {
        return "SELECT v.vinyl_id, v.artist_name, v.album_title, v.release_year, g.genre, s.size, v.condition " +
                "FROM VINYLS v " +
                "JOIN GENRES g ON v.genre_id = g.genre_id " +
                "JOIN SIZES s ON v.size_id = s.size_id ";
    }

    private String getTableName(String tableName) {
        String query;
        if (tableName.equals("VINYLS")) {
            query = generateBaseQuery();
        } else {
            query = "SELECT * FROM " + tableName;
        }
        return query;
    }

    private void generateTableColumns(TableView<Object[]> table, ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            TableColumn<Object[], Object> column = new TableColumn<>(metaData.getColumnName(i));
            final int index = i;
            column.setCellValueFactory(cellData -> {
                Object[] row = cellData.getValue();
                return new SimpleObjectProperty<>(row[index - 1]);
            });
            table.getColumns().add(column);
        }

        while (resultSet.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = resultSet.getObject(i);
            }
            table.getItems().add(row);
        }
    }

    private void insertIntoVinyls(String artistName, String albumTitle, int releaseYear, int genreId, int sizeId, String condition) {
        String insertSql = "INSERT INTO Vinyls (Artist_Name, Album_Title, Release_Year, Genre_ID, Size_ID, Condition) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, artistName);
            statement.setString(2, albumTitle);
            statement.setInt(3, releaseYear);
            statement.setInt(4, genreId);
            statement.setInt(5, sizeId);
            statement.setString(6, condition);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertIntoTable(String tableName, String value) {
        String insertSql = "INSERT INTO " + tableName + " (" + tableName.substring(0, tableName.length() - 1) + ") VALUES (?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, value);
            statement.executeUpdate();
            populateTable(editTable, tableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateVinyls(Object[] selectedRow, String artistName, String albumTitle,
                              String releaseYear, String genre, String size, String condition) {
        int genreId = getId(genre, genreMap);
        int sizeId = getId(size, sizeMap);

        String updateQuery = "UPDATE Vinyls SET Artist_Name=?, Album_Title=?, Release_Year=?, Genre_ID=?, Size_ID=?, Condition=? WHERE Vinyl_Id=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {
            statement.setString(1, artistName);
            statement.setString(2, albumTitle);
            statement.setInt(3, Integer.parseInt(releaseYear));
            statement.setInt(4, genreId);
            statement.setInt(5, sizeId);
            statement.setString(6, condition);
            statement.setInt(7, Integer.parseInt(selectedRow[0].toString()));
            statement.executeUpdate();

            populateTable(homeTable, "VINYLS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTable(Object[] selectedRow, String newValue, String tableName) {
        String columnName = tableName.substring(0, tableName.length() - 1);
        String updateQuery = "UPDATE " + tableName + " SET " + columnName + "=? WHERE " + columnName + "_ID=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {
            statement.setString(1, newValue);
            statement.setInt(2, Integer.parseInt(selectedRow[0].toString()));
            statement.executeUpdate();

            populateTable(editTable, tableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteFromTable(String tableName, int id) {
        String columnName = tableName.substring(0, tableName.length() - 1);
        String deleteSql = "DELETE FROM " + tableName + " WHERE " + columnName + "_ID=?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error deleting row from " + tableName + ": " + e.getMessage());
        }
    }

    private String buildSearchQuery(String selectedItem, String baseQuery, String searchKeyword) {
        return switch (selectedItem) {
            case "Latest releases" -> baseQuery + "WHERE Release_Year >= 2000";
            case "Old Skool Anthems" -> baseQuery + "WHERE Release_Year < 2000";
            case "By Genre" -> {
                if (searchKeyword.isEmpty()) {
                    showAlert("Please enter a genre.");
                    yield null;
                }
                yield baseQuery + "WHERE g.genre = ?";
            }
            case "By Size" -> {
                if (searchKeyword.isEmpty()) {
                    showAlert("Please enter a size.");
                    yield null;
                }
                yield baseQuery + "WHERE s.size = ?";
            }
            default -> {
                showAlert("Invalid search option.");
                yield null;
            }
        };
    }

    // UI Population Methods
    private void populateGenreAndSizeMaps() {
        populateMap("Genres", genreMap);
        populateMap("Sizes", sizeMap);
    }

    private void populateTable(TableView<Object[]> table, String tableName) {
        table.getItems().clear();
        table.getColumns().clear();

        try (Connection conn = DBConnection.getConnection()) {
            String query = getTableName(tableName);

            try (Statement stmt = conn.createStatement();
                 ResultSet resultSet = stmt.executeQuery(query)) {

                generateTableColumns(table, resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void populateHomeComboBoxes() {
        try (Connection conn = DBConnection.getConnection()) {
            populateComboBox(conn, "SELECT GENRE FROM Genres", homeGenreCB);
            populateComboBox(conn, "SELECT SIZE FROM Sizes", homeSizeCB);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateComboBox(Connection conn, String query, ComboBox<String> comboBox) throws SQLException {
        comboBox.getItems().clear();
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                comboBox.getItems().add(resultSet.getString(1));
            }
        }
    }

    public void populateConditionCB(ComboBox<String> comboBox) {
        List<String> conditions = Arrays.asList("Mint", "Near Mint", "Excellent", "Very Good", "Good", "Fair", "Poor");
        comboBox.getItems().addAll(conditions);
    }

    private void populateSearchTable(TableView<Object[]> table, String query, String searchKeyword) {
        table.getItems().clear();
        table.getColumns().clear();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (query.contains("?")) {
                stmt.setString(1, searchKeyword);
            }

            try (ResultSet resultSet = stmt.executeQuery()) {
                generateTableColumns(table, resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Dialog Creation Methods
    private Dialog<ButtonType> createHomeDialog(Object[] selectedRow) throws SQLException {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Data");
        dialog.setHeaderText("Modify the data");

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField artistNameField = new TextField(selectedRow[1].toString());
        TextField albumTitleField = new TextField(selectedRow[2].toString());
        TextField releaseYearField = new TextField(selectedRow[3].toString());

        ComboBox<String> genreComboBox = new ComboBox<>();
        populateComboBox(DBConnection.getConnection(), "SELECT GENRE FROM Genres", genreComboBox);
        genreComboBox.setValue(selectedRow[4].toString());

        ComboBox<String> sizeComboBox = new ComboBox<>();
        populateComboBox(DBConnection.getConnection(), "SELECT SIZE FROM Sizes", sizeComboBox);
        sizeComboBox.setValue(selectedRow[5].toString());

        ComboBox<String> conditionComboBox = new ComboBox<>();
        populateConditionCB(conditionComboBox);
        conditionComboBox.setValue(selectedRow[6].toString());

        grid.add(new Label("Artist Name:"), 0, 0);
        grid.add(artistNameField, 1, 0);
        grid.add(new Label("Album Title:"), 0, 1);
        grid.add(albumTitleField, 1, 1);
        grid.add(new Label("Release Year:"), 0, 2);
        grid.add(releaseYearField, 1, 2);
        grid.add(new Label("Genre:"), 0, 3);
        grid.add(genreComboBox, 1, 3);
        grid.add(new Label("Size:"), 0, 4);
        grid.add(sizeComboBox, 1, 4);
        grid.add(new Label("Condition:"), 0, 5);
        grid.add(conditionComboBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                updateVinyls(selectedRow, artistNameField.getText(), albumTitleField.getText(),
                        releaseYearField.getText(), genreComboBox.getValue(), sizeComboBox.getValue(),
                        conditionComboBox.getValue());
            }
            return null;
        });

        return dialog;
    }

    private Dialog<ButtonType> createEditDialog(Object[] selectedRow, String tableName) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Data");
        dialog.setHeaderText("Modify the data");

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField textField = new TextField(selectedRow[1].toString());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(textField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                String newValue = textField.getText();

                updateTable(selectedRow, newValue, tableName);
            }
            return null;
        });

        return dialog;
    }
}
