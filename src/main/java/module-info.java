module edu.unm.casaa.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires javafx.media;
    requires java.desktop;
    requires java.sql;
    requires java.prefs;
    requires sqlite.jdbc;

    opens edu.unm.casaa.main to javafx.fxml;
    exports edu.unm.casaa.main;
}
