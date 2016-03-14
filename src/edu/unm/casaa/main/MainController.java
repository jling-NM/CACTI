package edu.unm.casaa.main;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;


public class MainController {
    @FXML
    private MenuBar menuBar;
    @FXML
    private MediaView mediaView;
    @FXML
    private MediaPlayer mediaPlayer;
    @FXML
    private Slider playbackSlider;
    @FXML
    private Button btnPlayer;
    @FXML
    private Label lblDuration;
    @FXML
    private MenuItem mniLoad;
    @FXML
    private MenuItem mniPrefs;
    @FXML
    private MenuItem mniExit;
    @FXML
    private Menu mnuCodeUterrances;
    @FXML
    private MenuItem mniCodeStart;
    @FXML
    private MenuItem mniCodeResume;
    @FXML
    private Menu mnuFile;
    @FXML
    private Menu mnuGlobalRatings;
    @FXML
    private MenuItem mniGlobalScore;
    @FXML
    private MenuItem mniOnlineHelp;
    @FXML
    private Button btnPlayPause;
    @FXML
    private Button btnRewind;
    @FXML
    public Slider sldVolume;


    //TODO: where to get strings resource bundle ONCE for controller. Some contructor or initialzer should do it

    @FXML
    private void initialize() {
        System.out.println("Controller Initializing...");

        // i'm thinking this lambda gets used to set mediaplayer volume only; is there a way to bind the two?
        // i will not use this to set volume app prefs because i only need to do that at the end of some action
        // not everytime the slider value changes.
        sldVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Slider Value Changed (newValue: " + newValue.intValue() + ")");
            // change player volume; mediaplayer must be initiated
            // mediaPlayer.setVolume(sldVolume.getValue());
        });
    }


    public void playMedia(ActionEvent actionEvent) {
        try {
            final Media media = new Media(Main.class.getResource("/media/sample_tone.wav").toURI().toURL().toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();

        } catch (URISyntaxException ex) {
            System.out.println("Error with playing sound.");
            System.out.println(ex.toString());
        } catch(MalformedURLException ex) {

            System.out.println("Error with playing sound.");
            System.out.println(ex.toString());
        }
    }



    public void playerAction(ActionEvent actionEvent) {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            mediaPlayer.play();
        } else {
            mediaPlayer.pause();
        }
    }

    public void btnActRewind(ActionEvent actionEvent) {
    }

    public void mniActAbout(ActionEvent actionEvent) {

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(resourceStrings.getString("wind.title.about"));
        alert.setHeaderText(null);
        alert.setContentText(resourceStrings.getString("txt.about"));
        alert.setHeight(800); //TODO: this doesn't give proper size yet
        alert.setWidth(600);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();

    }


    public void mniActOnlineHelp(ActionEvent actionEvent) {

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(resourceStrings.getString("wind.title.help"));
        alert.setHeaderText(null);
        alert.setContentText(resourceStrings.getString("txt.help.message"));
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();

    }


    public void mniActExit(ActionEvent actionEvent) {

        // check what needs to be saved and closed

        // exit
        Platform.exit();
    }


    public void mniActOpenFile(ActionEvent actionEvent) {
        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Open audio file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"));
        File selectedFile = fc.showOpenDialog(stageTheLabelBelongs);
        if (selectedFile != null) {
            final Media media = new Media(selectedFile.toURI().toString());

            // just trying to see if duration is in metadata. nope.
            ObservableMap<String, Object> metadata =media.getMetadata();
            for(String key : metadata.keySet()) {
                System.out.println(key + " = " + metadata.get(key));
            }

            mediaPlayer = new MediaPlayer(media);
            btnPlayer.setDisable(false);
            //final Duration totalDuration = mediaPlayer.getTotalDuration();
            System.out.println(mediaPlayer.currentTimeProperty().toString());

            //lblDuration.setText(mediaPlayer.totalDurationProperty().toString());
        }
    }

    public void sldActVolume(Event event) {
        System.out.println(String.format("volume:%f",sldVolume.getValue()));
    }

}
