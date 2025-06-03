module com.advancedcomponentservices.acswarehouse {
    requires javafx.controls;
    requires java.sql;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.bouncycastle.provider;
    requires com.nimbusds.jose.jwt;
    requires kotlin.stdlib;

    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.advancedcomponentservices.acswarehouse to javafx.fxml;
    exports com.advancedcomponentservices.acswarehouse;
}