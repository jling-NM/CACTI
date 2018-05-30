/*
This source code file is part of the CASAA Treatment Coding System Utility
    Copyright (C) 2009  UNM CASAA
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package edu.unm.casaa.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Main extends Application {

    private Preferences appPrefs;           // reference for user preferences
    private Stage mainStage;
    private MainController mainController; // reference for the controller


    @Override
    public void init() {

        // get requirements
        String javaVersionStr = System.getProperty("java.specification.version","UNKNOWN");
        // if you need to enforce specfic 1.8 build like 4 or 6 use java.version
        //System.getProperties().list(System.out);

        Double javaVersionNum = Double.parseDouble(javaVersionStr);
        if( javaVersionNum < 1.8 ) {
            System.out.println("Java Version Error: " + javaVersionStr);
            System.exit(1);
        }

        // link app appPrefs
        appPrefs = Preferences.userNodeForPackage(Main.class);
    }


    @Override
    public void start(Stage primaryStage) throws Exception{

        // store reference
        mainStage = primaryStage;

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Main.fxml"), resourceStrings);
        Parent root = fxmlLoader.load();

        // get controller instance for manipulating/querying controller members
        mainController = fxmlLoader.getController();

        // main window position and size from preferences
        double windX = appPrefs.getDouble("main.wind.x", 0.0);
        double windY = appPrefs.getDouble("main.wind.y", 0.0);
        //double windH = appPrefs.getDouble("main.wind.h", 600.0);
        double windW = appPrefs.getDouble("main.wind.w", 800.0);
        mainStage.setX(windX);
        mainStage.setY(windY);
        //mainStage.setHeight(windH);
        mainStage.setWidth(windW);
        //mainStage.sizeToScene();
        mainStage.setTitle(resourceStrings.getString("wind.title.main"));

        // application window icon is set
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_16x16.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_24x24.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_32x32.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_48x48.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_64x64.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_96x96.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_128x128.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_256x256.png")));
        mainStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_512x512.png")));



        Scene mainScene = new Scene(root);
        mainStage.setScene(mainScene);

        // set volume to user prefs id
        mainController.sldVolume.adjustValue(appPrefs.getDouble("player.volume",0.5));
        mainStage.show();

        /**
         * pass command line arguments to controller
         * if the argument is a wav or casaa file it will initialize the guistate
         */
        mainController.initLaunchArgs(getParameters());

    }


    @Override
    public void stop() {

        // save user appPrefs for position and size of main window
        appPrefs.putDouble("main.wind.x",mainStage.getX());
        appPrefs.putDouble("main.wind.y",mainStage.getY());
        appPrefs.putDouble("main.wind.h",mainStage.getHeight());
        appPrefs.putDouble("main.wind.w",mainStage.getWidth());

        // save current volume preference
        appPrefs.putDouble("player.volume",mainController.sldVolume.getValue());

    }

    public static void main(String[] args) { launch(args); }

}
