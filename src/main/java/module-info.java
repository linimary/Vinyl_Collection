module com.example.vinylcollection {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires java.sql;

    opens com.example.vinylcollection to javafx.fxml;
    exports com.example.vinylcollection;
}