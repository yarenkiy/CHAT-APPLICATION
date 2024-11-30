module com.example.proje3 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proje3 to javafx.fxml;
    exports com.example.proje3;
}