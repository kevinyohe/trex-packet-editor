package com.xored.javafx.packeteditor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xored.javafx.packeteditor.controllers.AppController;
import com.xored.javafx.packeteditor.guice.GuiceModule;
import com.xored.javafx.packeteditor.scapy.ConnectionException;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.ConnectionErrorDialog;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.EMBEDDED;
import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.STANDALONE;

public class TRexPacketCraftingTool extends Application {
    static Logger log = LoggerFactory.getLogger(TRexPacketCraftingTool.class);

    public static void main(String[] args) {
        TRexPacketCraftingTool.launch(args);
    }

    private Injector injector = Guice.createInjector(new GuiceModule());

    public Injector getInjector() {
        return injector;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        doStart(primaryStage, true);
    }
    
    public void startAsEmbedded(Stage stage) throws Exception {
        doStart(stage, false);
    }
    
    private void doStart(Stage stage, boolean isStandalone) throws Exception{
        ConfigurationService configurationService = injector.getInstance(ConfigurationService.class);
        ConfigurationService.ApplicationMode appMode;
        if (isStandalone) {
            appMode = STANDALONE;
        } else {
            appMode = EMBEDDED;
        }
        configurationService.setApplicationMode(appMode);

        try {
            initPacketDataService(configurationService);
        } catch (ConnectionException e) {
            if (STANDALONE.equals(appMode)) {
                log.error("Scapy server is unavailable. Critical error. Exiting now.");
                shutdown();
            } else {
                return;
            }
        }
        
        AppController appController = injector.getInstance(AppController.class);
        appController.setMainStage(stage);
        
        log.debug("Running app");
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/app.fxml"));

        Parent parent = fxmlLoader.load();

        Scene scene = new Scene(parent);

        if (System.getenv("DEBUG") == null) {
            scene.getStylesheets().add(ClassLoader.getSystemResource("styles/main-narrow.css").toExternalForm());
        } else {
            // use css from source file to utilize JavaFX css auto-reload
            String cssSource = "file://" + new File("src/main/resources/styles/main-narrow.css").getAbsolutePath();
            scene.getStylesheets().add(cssSource);
        }
        
        stage.setScene(scene);
        stage.setTitle("Packet Crafting Tool");
        stage.show();
        stage.setOnCloseRequest(e -> {
            appController.terminate();
        });
    }

    private void shutdown() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
        System.exit(0);
    }

    private void initPacketDataService(ConfigurationService configurationService) throws ConnectionException {
        PacketDataService packetDataService = injector.getInstance(PacketDataService.class);
        packetDataService.init(configurationService);
    }
}
