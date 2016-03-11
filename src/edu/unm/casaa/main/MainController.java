package edu.unm.casaa.main;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class MainController {
    public MenuBar menuBar;
    public MenuItem mniQuit;
    public MediaView mediaView;
    public MediaPlayer mediaPlayer;
    public Slider playbackSlider;
    public MenuItem mniOpen;
    public Button btnPlayer;
    public Label lblDuration;

    public void actQuit(ActionEvent actionEvent) {
        Platform.exit();
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

    public void openFile(ActionEvent actionEvent) {

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

    public void playerAction(ActionEvent actionEvent) {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
            mediaPlayer.play();
        } else {
            mediaPlayer.stop();
        }
    }
}
