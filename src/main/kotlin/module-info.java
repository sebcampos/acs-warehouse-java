module com.advancedcomponentservices.acswarehouse {
    requires javafx.controls;
    requires java.sql;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.bouncycastle.provider;
    requires kotlin.stdlib;
    requires okhttp3;

    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.advancedcomponentservices.acswarehouse to javafx.fxml;
    opens com.advancedcomponentservices.acswarehouse.google.models to com.google.gson;
    exports com.advancedcomponentservices.acswarehouse;
}