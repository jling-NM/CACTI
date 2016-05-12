package edu.unm.casaa.main;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.swing.*;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;


public class MainController {

    @FXML
    private Label lblTimePos;
    @FXML
    private Label lblVolume;
    @FXML
    private Slider sldSeek;
    @FXML
    private AnchorPane apMediaCtrls;
    @FXML
    private AnchorPane apBtnBar;
    @FXML
    private MenuBar menuBar;
    @FXML
    private MediaPlayer mediaPlayer;
    @FXML
    private Label lblDuration;
    @FXML
    public Slider sldVolume;
    @FXML
    private ImageView btnPlayImgVw;
    @FXML
    private SwingNode snCoding;


    //
    private Duration totalDuration;


    /* don't know what i want to do here yet */
    @FXML
    private void initialize() {
        System.out.println("Controller Initializing...");
    }




    // lambda runnable called when player is ready with a media loaded
    Runnable playerReady = () -> {
        System.out.println("MEDIAPLAYER: OnReady");

        // enable all the media controls; perhaps through a single pane of some sort???
        apBtnBar.setDisable(false);
        apMediaCtrls.setDisable(false);

        // bind the volume slider to the mediaplayer volume
        mediaPlayer.volumeProperty().bind(sldVolume.valueProperty());
        // bind
        lblVolume.textProperty().bind(sldVolume.valueProperty().asString("%.1f"));

        // i'm not sure it is worth having this as private member unless it helps inside
        // the listener code to avoid making the duration call repeatedly.
        totalDuration = mediaPlayer.getTotalDuration();
        // duration label
        //Duration totalDuration = mediaPlayer.getMedia().getDuration();
        lblDuration.setText(Utils.formatDuration(totalDuration));

    };


    /**********************************************************************
     * Button event handlers
     **********************************************************************/

    /**********************************************************************
     * btnActPlayPause
     * @param actionEvent
     * button event: play media
     **********************************************************************/
    public void btnActPlayPause(ActionEvent actionEvent) {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.play();
        }
    }

    /**********************************************************************
     *  button event: 5 second rewind
     *  @param actionEvent
     **********************************************************************/
    public void btnActRewind(ActionEvent actionEvent) {
        /* specific rewind button back 5 seconds */
        if( mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5.0))){
            mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5.0)));
        }
    }



    /**********************************************************************
     * Menu event handlers
     **********************************************************************/

    /**********************************************************************
     * menu selection event: About
     **********************************************************************/
    public void mniActAbout(ActionEvent actionEvent) {

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(resourceStrings.getString("wind.title.about"));
        alert.setHeaderText(null);
        alert.setContentText(resourceStrings.getString("txt.about"));
        //alert.setHeight(800); //TODO: this doesn't give proper size yet
        //alert.setWidth(600);
        //alert.setX(400);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();

    }

    /* menu selection event: Help */
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

    /* menu selection event: Exit */
    public void mniActExit(ActionEvent actionEvent) {

        // check what needs to be saved and closed

        /* save current volume in user prefs */

        /* this disabled but if we need to write more user prefs from controller, here is an example
           you'd have to pass in appPrefs to controller at that point
         */
        //System.out.println(String.format("volume:%f",sldVolume.getValue()));
        //appPrefs.putDouble("player.volume",sldVolume.getValue());

        /* Application exit */
        Platform.exit();
    }


    /* menu selection event: Open File */
    public void mniActOpenFile(ActionEvent actionEvent) {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Open audio file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"));
        File selectedFile = fc.showOpenDialog(stageTheLabelBelongs);
        if (selectedFile != null) {
            final Media media = new Media(selectedFile.toURI().toString());

            try {

                mediaPlayer = new MediaPlayer(media);

                /* Status Handler: OnReady */
                mediaPlayer.setOnReady(playerReady);

                /* Status Handler: OnPlaying - lambda runnable when mediaplayer starts playing */
                mediaPlayer.setOnPlaying(() -> btnPlayImgVw.getStyleClass().add("img-btn-pause"));

                /* Status Handler:  OnPaused */
                mediaPlayer.setOnPaused(() -> {
                    // assumes OnPlay has overlayed style class so just remove that to expose pause class
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                } );

                /* Status Handler: OnStop */
                mediaPlayer.setOnStopped(() -> {
                    System.out.println("MEDIAPLAYER: Stopped");
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });

                /* Status Handler:  lambda runnable when mediaplayer reaches end of media
                * move back to the beginning of the track */
                mediaPlayer.setOnEndOfMedia(() -> {
                    System.out.println("MEDIAPLAYER: End of Media");
                    // seek to zero otherwise it is still at the end time
                    mediaPlayer.seek(Duration.ZERO);
                    // change state
                    mediaPlayer.stop();
                });



                /* Listener: currentTime
                   responsible for updating gui components with current playback position
                   because MediaPlayer’s currentTime property is updated on a different thread than the main JavaFX application thread. Therefore we cannot bind to it directly
                 */
                mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    // label
                    lblTimePos.setText(Utils.formatDuration(newValue));
                    //slider
                    sldSeek.setValue(newValue.toMillis() / totalDuration.toMillis());
                });


                /* Listener: Update the media position if user is dragging the slider.
                 * Otherwise, do nothing. See sldSeekMousePressed() for when slider is clicked with mouse
                 * Seems odd to bind to valueProperty and check isValueChanging
                 * but when i use "valueChangingProperty" this performance is
                 * not as smooth*/
                sldSeek.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    /* if dragging slider, update media position */
                    if (sldSeek.isValueChanging()) {
                        // multiply duration by percentage calculated by slider position
                        mediaPlayer.seek(totalDuration.multiply((Double) newValue));
                    }
                });



            } catch (Exception ex) {
                System.out.println("Error with playing sound.");
                System.out.println(ex.toString());
            }



        }
    }


    /* test coding swing node */
    public void mniStartCoding(ActionEvent actionEvent) {
        System.out.println("codeView Test");
        snCoding.setContent(new JButton("Click me!"));
    }



    /* sldSeek mouse event:
       change time seek when user clicks on slid bar instead of dragging the controller
     * to change the position */
    public void sldSeekMousePressed(Event event) {
        /* if playing we first pause, seek and resume */
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            mediaPlayer.seek(totalDuration.multiply(sldSeek.getValue()));
            mediaPlayer.play();
        } else {
            /* if not playing, we can't seek stopped media so i pause it and then seek
               label then updated manually since seek an paused media doesn't.
               This method seems reasonable since user will likely play after clicking seek bar
             */
            mediaPlayer.pause();
            mediaPlayer.seek(totalDuration.multiply(sldSeek.getValue()));
            lblTimePos.setText(Utils.formatDuration(totalDuration.multiply(sldSeek.getValue())));
        }
    }

}
