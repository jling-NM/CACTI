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

import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.*;
import edu.unm.casaa.globals.*;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.w3c.dom.*;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.lang.String.format;

import javafx.collections.ObservableMap;
import javafx.collections.MapChangeListener;
import javafx.collections.FXCollections;



public class MainController {


    // PLAYBACK
    @FXML
    private Label lblRate;                              // display current playback rate
    @FXML
    private Slider sldRate;                             // playback rate control
    @FXML
    private VBox vbApp;                                 // control holding non-playback controls (misc/globals)
    @FXML
    private Label lblAudioFilename;                     //
    @FXML
    private Button btnPlayPause;
    @FXML
    private Button btnReplay;
    @FXML
    private Button btnUncode;
    @FXML
    private Button btnUncodeReplay;
    @FXML
    private Button btnRewind;
    @FXML
    private Label lblTimePos;
    @FXML
    private Label lblVolume;
    @FXML
    private Slider sldSeek;
    @FXML
    private AnchorPane apMediaCtrls;
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


    // MISC_CODING
    @FXML
    private TitledPane titlePnlCodesLeft;
    @FXML
    private TitledPane titlePnlCodesRight;
    @FXML
    private GridPane pnlCodesLeft;
    @FXML
    private GridPane pnlCodesRight;
    @FXML
    private StackPane pnTimeLine;
    @FXML
    private Label lblCurMiscFile;
    @FXML
    private Label lblCurUtrEnum;
    @FXML
    private Label lblCurUtrCode;
    @FXML
    private Label lblCurUtrStartTime;
    @FXML
    private Label lblCurUtrEndTime;
    @FXML
    private Label lblPrevUtr;


    // GLOBALS_CODING
    @FXML
    private TextArea tfGlobalsNotes;
    @FXML
    private GridPane gpGlobalControls;                  // control containing globals controls




    private Preferences appPrefs;                       // User prefs persistence

    // mediaplayer attributes
    private Duration totalDuration;                     // duration of active media
    private int numUninterruptedUncodes  = 0;           // Number of times user has uncoded without doing anything else.
    private String filenameMisc          = null;        // name of active CASAA data file.
    private String filenameGlobals       = null;        // name of active globals data file
    private String filenameAudio         = null;        // name of active media file
    private File currentAudioFile        = null;        // active media file
    private UtteranceList utteranceList  = null;        // MISC coding data
    private GlobalDataModel globalsData  = null;        // GLOBALS scoring data
    private final int utrCodeButtonWidth = 70;          // fixed width of code generated utterace buttons. Could be CSS.
    private enum  GuiState {                            // available gui states
        PLAYBACK, MISC_CODING, GLOBAL_CODING
    }
    private GuiState guiState;                          // for referencing state

    private TimeLine timeLine;


    /******************************************************************
     * controller initialization tasks
     ******************************************************************/
    @FXML
    private void initialize() {

        // Use OS X standard menus no Java window menus
        if( System.getProperty("os.name","UNKNOWN").equals("Mac OS X")) {
            menuBar.setUseSystemMenuBar(true);
        }

        //
        setGuiState(GuiState.PLAYBACK);

        // initialize app persistence
        appPrefs = Preferences.userNodeForPackage(Main.class);

        // check for required config to offer generation
        verifyUserConfig();

        // load user config file to load user specific edited codes
        parseUserConfig();

    }



    /*********************************************************
     * define lambda runnable later called by player when
     * ready with media
     *********************************************************/
    private final Runnable playerReady = () -> {

        // enable all the media controls
        apMediaCtrls.setDisable(false);

        // bind the volume slider to the mediaplayer volume
        mediaPlayer.volumeProperty().bind(sldVolume.valueProperty());
        // bind display playback volume label with volume slider id
        lblVolume.textProperty().bind(sldVolume.valueProperty().asString("%.1f"));
        // bind display playback rate with rate slider id
        lblRate.textProperty().bind(sldRate.valueProperty().asString("%.1f"));
        // set mediaplayer rate with slider id
        mediaPlayer.setRate(sldRate.getValue());

        // i'm not sure it is worth having this as private member unless it helps inside
        // the listener code to avoid making the duration call repeatedly.
        totalDuration = mediaPlayer.getTotalDuration();
        // duration label
        lblDuration.setText(Utils.formatDuration(totalDuration));

        // mediaPlayer is ready continue with user controls setup
        initializeUserControls();
    };





    /**********************************************************************
     * Button event handlers
     **********************************************************************/

    /**********************************************************************
     * btnActPlayPause
     * @param actionEvent
     * button event: play media
     **********************************************************************/
    public void btnActPlayPause(@SuppressWarnings("UnusedParameters") ActionEvent actionEvent) {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING) ) {
            mediaPlayer.pause();
        } else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.play();
        }
    }


    /**********************************************************************
     *  button event: 5 second rewind
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActRewind(ActionEvent actionEvent) {
        /* specific rewind button back 5 seconds */
        if( mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5.0))){
            setMediaPlayerPosition(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5.0)));
        }
    }


    /**********************************************************************
     *  button event: Seek to beginning of current utterance.  Seek a little further back
     *  to ensure audio synchronization issues don't cause player to actually
     *  seek later than beginning of utterance.
     *  @param actionEvent not used
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActReplay(ActionEvent actionEvent) {

        Utterance   utterance   = getUtteranceList().last();
        Duration    pos         = Duration.ZERO;

        if( utterance != null ) {
            // Position one second before start of utterance.
            pos = utterance.getStartTime().subtract(Duration.ONE);
        }

        setMediaPlayerPosition( pos );
    }


    /**********************************************************************
     *  button event: Remove last utterance
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActUncode(ActionEvent actionEvent) {
        // remove the last code
        removeLastUtterance();
        // update counter of how many time user uncoded
        incrementUncodeCount();
        // uncoding may exhaust available codes so update button state
        setPlayerButtonState();
    }


    /**********************************************************************
     *  button event: Uncode last utterance and replay it
     *  @param actionEvent not used
     **********************************************************************/
    public void btnActUncodeReplay(ActionEvent actionEvent) {

        Utterance utterance = getUtteranceList().last();
        if (utterance != null) {
            // Position one second before start of utterance.
            Duration pos = utterance.getStartTime().subtract(Duration.ONE);

            setMediaPlayerPosition(pos);

            // remove the last code
            removeLastUtterance();
            // update counter of how many time user uncoded
            incrementUncodeCount();

            // uncoding may exhaust available codes so update button state
            setPlayerButtonState();
        }
    }


    /**********************************************************************
     *  button event: Apply utterance code
     **********************************************************************/
    private void btnActCode(ActionEvent actionEvent) {
        Button src = (Button) actionEvent.getSource();
        MiscCode mc = MiscCode.codeWithName(src.getText());
        insertUtterance(mc);
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
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }



    /**********************************************************************
     * menu selection event: Help
     **********************************************************************/
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



    /**********************************************************************
     * menu selection event: Exit
     **********************************************************************/
    public void mniActExit(ActionEvent actionEvent) {

        // user prefs like current volume setting are saved in
        // Application.stop() when Platform.exit() is called.
        // Anything else you want to do before leaving???

        /* Application exit */
        Platform.exit();
    }



    /**********************************************************************
     * menu selection event: Open Audio File to listen independently of
     * coding
     **********************************************************************/
    public void mniActOpenFile(ActionEvent actionEvent) {

        setGuiState(GuiState.PLAYBACK);

        // if something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        File selectedFile = selectAudioFile();
        if (selectedFile != null) {
            currentAudioFile = selectedFile;
            filenameAudio = selectedFile.getAbsolutePath();

            initializeMediaPlayer(selectedFile, playerReady);
        }

    }



    /**********************************************************************
     * Begin Misc Coding
     * Load audio file and create corresponding coding output file.
     * Initialize the mediaplayer.
     * Activate the coding controls.
     * @param actionEvent event details
     */
    public void mniStartCoding(ActionEvent actionEvent) {

        setGuiState(GuiState.MISC_CODING);

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        File audioFile = currentAudioFile;
        // to we have an audio file already?
        if( audioFile == null ) {
            // Select audio file.
            audioFile = selectAudioFile();
            if( audioFile == null )
                return;
            filenameAudio = audioFile.getAbsolutePath();
        }

        // Default casaa filename to match audio file, with .casaa suffix.
        String newFileName = Utils.changeSuffix( audioFile.getName(), "casaa" );
        File miscFile = selectMiscFile(newFileName);
        if( miscFile == null ) {
            return;
        }
        filenameMisc = miscFile.getAbsolutePath();

        // reset utteranceList to start fresh
        utteranceList = new UtteranceList(miscFile, filenameAudio);

        if (audioFile.canRead()) {
            initializeMediaPlayer(audioFile, playerReady);
        } else {
            showError("File Error", format("%s\n%s\n%s", "Could not load audio file:", filenameAudio, "Check that it exists and has read permissions"));
        }

    }



    /******************************************************
     * Resume Misc Coding
     * Load coding file and corresponding audio file.
     * Initialize mediaplayer.
     * Activate timeline control updating it for utterance data
     * @param actionEvent event details
     ******************************************************/
    public void mniResumeCoding(ActionEvent actionEvent) {

        setGuiState(GuiState.MISC_CODING);

        // if something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        // user selects a casaa file or we leave
        File miscFile = selectMiscFile("");
        if( miscFile == null ) {
            //showError("File Error", "Could not open coding file");
            return;
        }
        filenameMisc = miscFile.getAbsolutePath();

        // now get utterances from code file
        try {
            utteranceList = UtteranceList.loadFromFile(miscFile);
        } catch (Exception e) {
            showError("Error Loading Casaa File", e.getMessage());
            return;
        }

        // load the audio and start the player
        File audioFile = new File(utteranceList.getAudioFilename());
        if (audioFile.canRead()) {
            initializeMediaPlayer(audioFile, playerReady);
        } else {
            showError("Error Loading Audio File", format("%s\n%s\n%s", "Could not load audio file:", filenameAudio, "Check that it exists and has read permissions"));
        }

        setPlayerButtonState();
    }



    public void mniGlobalScoring(ActionEvent actionEvent) {

        setGuiState(GuiState.GLOBAL_CODING);

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        // user selects a globals file or we leave
        File globalsFile = selectGlobalsFile();
        if( globalsFile == null ) {
            //showError("File Error", "Could not open coding file");
            return;
        }
        filenameGlobals = globalsFile.getAbsolutePath();

        // determine audio file
        File audioFile = currentAudioFile;
        // to we have an audio file already?
        if( audioFile == null ) {
            // Select audio file.
            audioFile = selectAudioFile();
            if( audioFile == null )
                return;
            filenameAudio = audioFile.getAbsolutePath();
        }

        // load audio file
        if (audioFile.canRead()) {

            // initialize globals data model
            globalsData = new GlobalDataModel(globalsFile, filenameAudio);
            // take care of media player
            initializeMediaPlayer(audioFile, playerReady);

        } else {
            showError("File Error", format("%s\n%s\n%s", "Could not load audio file:", filenameAudio, "Check that it exists and has read permissions"));
        }


    }


    /**********************************************************************
     * sldSeek mouse event:
     * change seek time when user clicks on slid bar instead of dragging the controller
     * to change the position
     * @param event details
     **********************************************************************/
    public void sldSeekMousePressed(Event event) {
        setMediaPlayerPosition(totalDuration.multiply(sldSeek.getValue()));
    }


    /************************************************************************
     * get and persist audio file
     * @return Audio File object
     ***********************************************************************/
    private File selectAudioFile() {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Open audio file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wav Files", "*.wav"));

        // set initial directory to preferences or users home directory
        File initDir = new File(appPrefs.get("lastAudioPath", System.getProperty("user.home")));
        // if preference path no longer exists reset to user home directory
        if( !initDir.exists() ) {
            initDir = new File(System.getProperty("user.home"));
        }
        // set chooser init directory
        fc.setInitialDirectory(initDir);
        // get user selection
        File selectedFile = fc.showOpenDialog(stageTheLabelBelongs);
        // persist path for next time
        if( selectedFile != null) {
            appPrefs.put("lastAudioPath", selectedFile.getParent());
            currentAudioFile = selectedFile;
        }

        return selectedFile;
    }


    /************************************************************************
     * Specify a Misc code file for coding
     * @param newFileName filename to seed filechooser
     * @return Misc Codes File object
     ************************************************************************/
    private File selectMiscFile(String newFileName) {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        // set code file chooser
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CASAA files", "*.casaa"));

        // set initial directory to preferences or users home directory
        File initDir = new File(appPrefs.get("lastCasaaPath", System.getProperty("user.home")));
        // if preference path no longer exists reset to user home directory
        if( !initDir.exists() ) {
            initDir = new File(System.getProperty("user.home"));
        }
        // set chooser init directory
        fc.setInitialDirectory(initDir);

        // get user selection
        File selectedFile;
        if (newFileName.isEmpty()) {
            fc.setTitle("Open CASAA File");
            selectedFile = fc.showOpenDialog(stageTheLabelBelongs);
        } else {
            fc.setTitle("Name New CASAA File");
            fc.setInitialFileName(newFileName);
            selectedFile = fc.showSaveDialog(stageTheLabelBelongs);
        }

        // persist path for next time
        if( selectedFile != null) {
            appPrefs.put("lastCasaaPath", selectedFile.getParent());
        }

        return selectedFile;
    }


    /************************************************************************
     * Specify a Globals code file for coding
     * @return Globals File object
     ************************************************************************/
    private File selectGlobalsFile() {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        // set code file chooser
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLOBALS files", "*.globals"));

        // set initial directory to preferences or users home directory
        File initDir = new File(appPrefs.get("lastGlobalsPath", System.getProperty("user.home")));
        // if preference path no longer exists reset to user home directory
        if( !initDir.exists() ) {
            initDir = new File(System.getProperty("user.home"));
        }
        // set chooser init directory
        fc.setInitialDirectory(initDir);

        // get user selection
        File selectedFile;
        fc.setTitle("Name New GLOBALS File");
        selectedFile = fc.showSaveDialog(stageTheLabelBelongs);
        // persist path for next time
        if( selectedFile != null) {
            // update persistence
            appPrefs.put("lastGlobalsPath", selectedFile.getParent());
            // make sure file has proper extension
            String newFile = selectedFile.getAbsolutePath();
            if( !newFile.toLowerCase().endsWith(".globals")) {
                selectedFile = new File(newFile + ".globals");
            }
        }

        return selectedFile;
    }




    private void selectConfigFile(String newFileName) {

        // set code file chooser
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CACTI Config files", "*.xml"));

        // get user selection
        File selectedFile;
        if (newFileName.isEmpty()) {
            // seed path empty; select existing
            File initDir = new File(System.getProperty("user.home"));
            fc.setTitle("Open Config File");
            fc.setInitialDirectory(initDir);
            selectedFile = fc.showOpenDialog(null);
        } else {
            // create new file
            fc.setTitle("Create New Config File");
            File initFile = new File(newFileName);
            fc.setInitialDirectory(initFile.getParentFile());
            fc.setInitialFileName(initFile.getName());
            selectedFile = fc.showSaveDialog(null);
        }

        // persist path for next time
        if( selectedFile != null) {
            UserConfig.setPath(selectedFile.getAbsolutePath());
        } else {
            System.exit(0);
        }

    }





    /**********************************************************************
     * Initialize the media player state with media file
     * @param mediaFile media object used to initialize player
     * @param onReadyMethod once player is ready which runnable will be called
     **********************************************************************/
    private void initializeMediaPlayer(File mediaFile, Runnable onReadyMethod) {

        if (mediaFile != null) {
            try {
                final Media media = new Media(mediaFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                /* Status Handler: OnReady */
                mediaPlayer.setOnReady(onReadyMethod);

                /* Status Handler: OnPlaying - lambda runnable when mediaplayer starts playing */
                mediaPlayer.setOnPlaying(() -> {
                    //timeLine.getAnimation().play();
                    //timeLine.getAnimation().playFrom(mediaPlayer.getCurrentTime());
                    btnPlayImgVw.getStyleClass().add("img-btn-pause");
                });

                /* Status Handler:  OnPaused */
                mediaPlayer.setOnPaused(() -> {
                    // assumes OnPlay has overlayed style class so just remove that to expose pause class
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });

                /* Status Handler: OnStop */
                mediaPlayer.setOnStopped(() -> {
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });

                /* Status Handler:  lambda runnable when mediaplayer reaches end of media
                * move back to the beginning of the track */
                mediaPlayer.setOnEndOfMedia(() -> {
                    /*
                     option 1: seek to zero otherwise it is still at the end time
                     However, this doesn't give user option to code that last section
                     up to the end because it goes away on reseek and stop.
                     */
                    //mediaPlayer.seek(Duration.ZERO);
                    // change state
                    //mediaPlayer.stop();


                    /*
                        option 2: pause at end so user can code
                        However, have to click play twice???
                     */
                    mediaPlayer.pause();
                    // shouldn't have to do this...
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });



                /** Listener: Update the media position if user is dragging the slider.
                 * Otherwise, do nothing. See sldSeekMousePressed() for when slider is clicked with mouse
                 * Seems odd to bind to valueProperty and check isValueChanging
                 * but when i use "valueChangingProperty" this performance is
                 * not as smooth*/
                sldSeek.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    /* if dragging slider, update media position */
                    if (sldSeek.isValueChanging()) {
                        // multiply duration by percentage calculated by slider position
                        mediaPlayer.seek(totalDuration.multiply(newValue.doubleValue()));
                    }
                });

                /** if dragging slider, update media playback rate */
                sldRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if (sldRate.isValueChanging()) {
                        mediaPlayer.setRate(newValue.doubleValue());
                    }
                });

            } catch ( MediaException mex ) {
                if(mex.getType() == MediaException.Type.MEDIA_UNSUPPORTED) {
                    showError("Error Playing Audio File", "The file you selected is not supported.\nMake sure your file is not encoded.");
                } else {
                    showError("Error Playing Audio File", mex.getType().toString());
                }
            } catch (Exception ex) {
                showError("Error Starting MediaPlayer", ex.getMessage());
            }

        }
    }


    /**
     * Timeline setup
     */
    private void initializeTimeLine() {

        /**
         * determine where to put timeline overlay
         */
        double center = vbApp.getScene().getWidth()/2;
        // time position line
        Line l = new Line(0,0,0,28.0);
        l.setStrokeWidth(0.5);
        l.setTranslateX(center + l.getStrokeWidth());

        /**
         * initialize timeline and add to display
         */
        timeLine = new TimeLine(totalDuration, 20, center, utteranceList);
        pnTimeLine.getChildren().clear();
        pnTimeLine.getChildren().addAll(l, timeLine);

        /**
         * Here we link to mediaplayer status to sync timeline status
         * This is not done in setOnPlaying() lambda because the mediaplayer
         * can be initialized without there being a timeline defined.
         */
        mediaPlayer.statusProperty().addListener( (invalidated, oldvalue, newvalue) -> {
            switch (newvalue) {
                case PLAYING:
                    timeLine.getAnimation().play();
                    break;
                case PAUSED:
                    // first pause
                    timeLine.getAnimation().pause();
                    // then, match to media position
                    //timeLine.getAnimation().jumpTo(mediaPlayer.getCurrentTime());
                    break;
                case STOPPED:
                    timeLine.getAnimation().stop();
                    break;
                case STALLED:
                    timeLine.getAnimation().stop();
                    break;
                case HALTED:
                    timeLine.getAnimation().stop();
                    break;
            }
            });


        /*
        This works for linking timeline to player when seek happens on player which is a challenge
        However, this cause timeline to jump on PLAY which is annoying so i might go about this a less elegant way.

        mediaPlayer.currentTimeProperty().addListener((invalidated, oldValue, newValue) -> {
           timeLine.getAnimation().jumpTo(mediaPlayer.getCurrentTime());
        });
        */

        /**
         * Seek slider should manipulate timeline as it does mediaplayer
         */
        sldSeek.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            /* if dragging slider, update timeline position */
            if (sldSeek.isValueChanging()) {
                timeLine.getAnimation().pause();
                // multiply duration by percentage calculated by slider position
                timeLine.getAnimation().jumpTo(totalDuration.multiply(newValue.doubleValue()));
            }
        });


        /**
         * Playback rate binding
         */
        timeLine.getAnimation().rateProperty().bind(sldRate.valueProperty());

        /**
         * timeline can be paused internally by clicking on a marker
         * so this listener pauses mediaplayer in response
         */
        timeLine.getAnimation().statusProperty().addListener((invalidated, oldValue, newValue) -> {
            if (newValue.equals(Animation.Status.PAUSED)) {
                mediaPlayer.pause();
            }
        });

        /**
         * timeline currenttime must be set to match mediaplyer's currentime
         * Following sequence seems to be best way to do that
         */
        timeLine.getAnimation().play(); // have to start before jumpto will work
        timeLine.getAnimation().pause();
        timeLine.getAnimation().jumpTo(mediaPlayer.getCurrentTime());
    }



    /**********************************************************************
     * Called by mediaplayer after initialized and ready for work
     **********************************************************************/
    private void initializeUserControls() {

        // save this window's stage for resizing new controls
        Stage ourTown = (Stage) menuBar.getScene().getWindow();

        // common control updates; file name in mediaplayer
        lblAudioFilename.setText(mediaPlayer.getMedia().getSource());

        // this will store name of loaded set of controls; defaults to PLAYBACK
        String currentUserControls = GuiState.PLAYBACK.name();

        // if we have more than 2 we have user controls
        if( vbApp.getChildren().size() > 2 ) {
            // get usercontrols content node name
            currentUserControls = vbApp.getChildren().get(2).getId();
        }

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);
        FXMLLoader loader;


        // GuiState determines action
        switch (getGuiState()) {

            case PLAYBACK:

                // clean up non-playback controls
                resetUserControlsContainer();

                /* Listener: currentTime
                   responsible for updating gui components with current playback position
                   because MediaPlayer’s currentTime property is updated on a different thread than the main JavaFX application thread. Therefore we cannot bind to it directly
                   NOTE: this version does not update a timeline */
                mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    // update display of current time
                    lblTimePos.setText(Utils.formatDuration(newValue));
                    // update the mediaplayer slider
                    sldSeek.setValue(newValue.toMillis() / totalDuration.toMillis());
                });


                // update playback controls
                Duration onReadySeekDuration = Duration.ZERO;
                mediaPlayer.seek(onReadySeekDuration);
                lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
                sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());

                // display controls needed for coding
                setPlayerButtonState();

                // resize window
                ourTown.sizeToScene();

                break;


            case MISC_CODING:

                /*
                   detect if already in MISC_CODING
                   Somethings will not have to be reloaded and make user experience snappier
                 */
                if( ! currentUserControls.equals(getGuiState().name()) ) {

                    resetUserControlsContainer();

                    // load new controls
                    loader = new FXMLLoader(getClass().getResource("MISC_CODING.fxml"), resourceStrings);
                    loader.setController(this);
                    try {
                        vbApp.getChildren().add(loader.load());
                        // NOTE: for some reason guiState is reset to BASE when i do this while other members survive just fine.
                        // if i set controller in coding.fxml then it has not reference to local members down below
                        // Therefore, i reset it.
                        setGuiState(GuiState.MISC_CODING);
                    } catch (IOException ex) {
                        showError("Error", ex.toString());
                    }

                    // display controls needed for coding
                    setPlayerButtonState();

                    // load coding buttons from userConfiguration.xml appropriate for GuiState
                    parseUserControls();

                    // resize app window
                    ourTown.sizeToScene();
                }

                /* Listener: currentTime
                   responsible for updating gui components with current playback position
                   because MediaPlayer’s currentTime property is updated on a different thread than the main JavaFX application thread. Therefore we cannot bind to it directly
                   NOTE: It is important to bind property listeners before changing utterance data
                   and adjusting the mediaplayer position below
                 */
                mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    // update display of current time
                    lblTimePos.setText(Utils.formatDuration(newValue));
                    // update the mediaplayer slider
                    sldSeek.setValue(newValue.toMillis() / totalDuration.toMillis());
                });


                /**
                 * get last utterance to set player position in time
                 **/
                Utterance currentUtterance = getUtteranceList().last();
                // default seek init
                onReadySeekDuration = Duration.ZERO;

                // We expect utterances in file to be coded.  For backwards compatibility,
                // tolerate uncoded utterances in file.
                if( currentUtterance != null ) {
                    // update mediaplayer position appropriately for our now active utterance
                    onReadySeekDuration = currentUtterance.getStartTime();
                }


                /**
                 * adjust player position
                 */
                mediaPlayer.seek(onReadySeekDuration);
                lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
                sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());

                // update the utterance data(previous/current) displayed in the gui
                updateUtteranceDisplays();


                /**
                    initialize new timeline
                    assumes mediaplayer time position is already set
                 */
                initializeTimeLine();

                // display coding file path in gui
                lblCurMiscFile.setText(filenameMisc);

                // reset some utterance accounting
                resetUtteranceCoding();

                break;


            case GLOBAL_CODING:

                // always reset for globals
                resetUserControlsContainer();

                /* Listener: currentTime
                   responsible for updating gui components with current playback position
                   because MediaPlayer’s currentTime property is updated on a different thread than the main JavaFX application thread. Therefore we cannot bind to it directly
                   NOTE: this version does not update a timeline */
                mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    // update display of current time
                    lblTimePos.setText(Utils.formatDuration(newValue));
                    // update the mediaplayer slider
                    sldSeek.setValue(newValue.toMillis() / totalDuration.toMillis());
                });

                // reset playback controls to zero
                onReadySeekDuration = Duration.ZERO;
                mediaPlayer.seek(onReadySeekDuration);
                lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
                sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());

                // hide controls needed for coding
                setPlayerButtonState();

                // enable GLOBAL coding controls
                loader = new FXMLLoader(getClass().getResource("GLOBAL_CODING.fxml"), resourceStrings);
                loader.setController(this);

                try {
                    vbApp.getChildren().add(loader.load());
                    // NOTE: for some reason guiState is reset to BASE when i do this while other members survive just fine.
                    // Therefore, i reset it.
                    setGuiState(GuiState.GLOBAL_CODING);
                } catch (IOException ex) {
                    showError("Error", ex.toString());
                }


                // update control state
                // load coding buttons from userConfiguration.xml appropriate for GuiState
                parseUserControls();

                // resize window
                ourTown.sizeToScene();

                break;


        }

    }


    /**********************************************************************
     * Set mediaplayer position using Duration
     **********************************************************************/
    private synchronized void setMediaPlayerPosition(Duration position){
        // pause player whether playing or not which enables seek. also enables timeline to detect
        //MediaPlayer.Status ls = mediaPlayer.getStatus();

        // NOTE: Does not seek when player status is READY. Only when PAUSED or PLAYING
        mediaPlayer.pause();
        mediaPlayer.seek(position);
        lblTimePos.setText(Utils.formatDuration(totalDuration.multiply(sldSeek.getValue())));

        /**
         * this is unfortunate here but works better than other linking options
         */
        if(timeLine != null) {
            timeLine.getAnimation().pause();
            timeLine.getAnimation().jumpTo(position);
        }

        /***
         *
         * can NOT get this to work reliably across platform
        // start playing again only if that is what the status
        if(ls.equals(MediaPlayer.Status.PLAYING)){

            if(timeLine != null) {
                timeLine.getAnimation().pause();
                timeLine.getAnimation().playFrom(position);
            }

            mediaPlayer.play();

        } else if(ls.equals(MediaPlayer.Status.PAUSED)) {
            if(timeLine != null) {
                timeLine.getAnimation().pause();
                timeLine.getAnimation().jumpTo(position);
            }

            mediaPlayer.pause();
        }
         */

    }


    /*******************************************************
     * Reusable method to display runtime errors
     * @param title Window title
     * @param message Window message
     *******************************************************/
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }


    /*******************************************************
     * Reusable method to display runtime errors
     * @param title Window title
     * @param message Window message
     *******************************************************/
    public static void showFatalWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();

        // just kill JVM as we don't need to stop cleanly and Platform.exit() won't work here.
        System.exit(1);
    }


    /**********************************************************
     *
     * @return list of utterances
     **********************************************************/
    //TODO: this needs to be replaced perhaps; no longer needed?
    private synchronized UtteranceList getUtteranceList() {
        if( utteranceList == null )
            showFatalWarning("Error", "UtteranceList is null");
        return utteranceList;
    }


    /**
        See if uncoding is currently an option
     **/
    private boolean isUncodeAvailable() {

        Utterance l = getUtteranceList().last();

        if( l != null ) {
            return true;
        } else return false;
    }



    private synchronized void incrementUncodeCount() {
        numUninterruptedUncodes++;
        if( numUninterruptedUncodes >= 4 ) {
            showError( "Uncode Warning", "You have uncoded 4 times in a row." );
            numUninterruptedUncodes = 0;
        }
    }



    private synchronized void insertUtterance(MiscCode miscCode) {

        // get current time
        Duration position = mediaPlayer.getCurrentTime();

        //TODO: possible?
        timeLine.getAnimation().jumpTo(position);

        // init new utterance.
        String      id   = Utils.formatID(position, miscCode.value);
        Utterance   data = new MiscDataItem(id, position);
        data.setMiscCode(miscCode);

        try {
            timeLine.add(data);
            // update display
            updateUtteranceDisplays();
            // button state different if 0 ver > 0 utterances
            setPlayerButtonState();
        } catch (IOException e) {
            showFatalWarning("File Error", e.getMessage());
        }
    }


    private synchronized void removeUtterance(Utterance utr){
        utteranceList.remove(utr);
        // refresh last utterance display
        updateUtteranceDisplays();

        try {
            utteranceList.writeToFile();
        } catch (IOException e) {
            showFatalWarning("File Error", e.getMessage());
        }
    }


    /********************************************************
     * Undo the actions of pressing a MISC code button.
     ********************************************************/
    private synchronized void removeLastUtterance(){
        // Remove last utterance
        Utterance u = getUtteranceList().last();

        if( u != null ) {
            removeUtterance(u);
        }
    }


    /*********************************************************************
     * Handle errors re: user codes XML file. We must be able to find and parse
     * this file successfully, so all of these errors are fatal.
     * @param file what was being parsed
     * @param e the Error
     *********************************************************************/
    public void handleUserCodesParseException(File file, SAXParseException e) {
        // Alert and quit.
        showFatalWarning("Failed to load user codes","Parse error in " + file.getAbsolutePath() + " (line " + e.getLineNumber() + "):\n" + e.getMessage());
    }

    public void handleUserCodesGenericException(File file, Exception e) {
        showFatalWarning("Failed to load user codes","Unknown error parsing file: " + file.getAbsolutePath() + "\n" + e.toString());
    }

    public void handleUserCodesError(File file, String message) {
        showFatalWarning("Failed to load user codes", "Error loading file: " + file.getAbsolutePath() + "\n" + message);
    }


    // Parse user codes and globals from XML.
    private void parseUserConfig() {

        // cheap way to check if we need to reload userconfig which we will only allow once per lifecycle
        if( MiscCode.numCodes() == 0 ){

            // NOTE: We display parse errors to user before quiting so user knows to correct XML file.
            File file = new File(UserConfig.getPath());

            if( file.canRead() ) {
                try {
                    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = fact.newDocumentBuilder();
                    Document doc = builder.parse( file.getCanonicalPath() );
                    Node root = doc.getDocumentElement();

                    // first, create lookup list the returns code value for code name
                    Hashtable<String, Integer> miscCodeValues = new Hashtable<String, Integer>();

                    // Expected format: <userConfiguration> <codes>...</codes> <globals>...</globals> </userConfiguration>
                    for( Node node = root.getFirstChild(); node != null; node = node.getNextSibling() ) {
                        if( node.getNodeName().equalsIgnoreCase( "codes" ) )

                            for( Node n = node.getFirstChild(); n != null; n = n.getNextSibling() ) {
                                if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                                    NamedNodeMap    map         = n.getAttributes();
                                    Node            nodeValue   = map.getNamedItem( "value" );
                                    int             value       = Integer.parseInt( nodeValue.getTextContent() );
                                    String          name        = map.getNamedItem( "name" ).getTextContent();

                                    // add to lookup list
                                    miscCodeValues.put(name, value);
                                }
                            }
                        else if( node.getNodeName().equalsIgnoreCase( "globals" ) )
                            parseUserGlobals( file, node );
                    }


                    // now, store codes use map
                    doc.getDocumentElement().normalize();
                    // just get nodes for controls
                    NodeList controlNodeList = doc.getElementsByTagName("codeControls");
                    // iterate each child node
                    for (int cn = 0; cn < controlNodeList.getLength(); ++cn) {
                        Node node = controlNodeList.item(cn);

                        // Get panel name.  Must be "left" or "right".
                        NamedNodeMap map = node.getAttributes();
                        String speaker = map.getNamedItem("label").getTextContent().split(" ")[0];
                        // i added ".split(" ")[0];" because one user had other text after that and i just wanted the first word.

                        for( Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
                            if( row.getNodeName().equalsIgnoreCase( "row" ) ) {

                                for( Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
                                    if( cell.getNodeName().equalsIgnoreCase("button")) {

                                        NamedNodeMap cellMap = cell.getAttributes();
                                        String codeName = cellMap.getNamedItem( "code" ).getTextContent();
                                        int codeValue = miscCodeValues.get(codeName);

                                        try {
                                            MiscCode.addCode( new MiscCode( codeValue, codeName, MiscCode.Speaker.valueOf(speaker) ) );
                                        } catch (Exception e) {
                                            handleUserCodesError( file, String.format("Failed to add code.\n%s", e.getMessage()) );
                                        }
                                    }
                                }

                            }
                        }

                    }





                } catch( SAXParseException e ) {
                    handleUserCodesParseException( file, e );
                } catch( Exception e ) {
                    handleUserCodesGenericException( file, e );
                }




            } else {
                // Alert and quit.
                showFatalWarning("Failed to load user codes","Failed to find required file.\n" + file.getAbsolutePath());
            }
        }
    }

    // Parse user codes and globals from XML.
/*
    private void parseUserConfig() {

        // cheap way to check if we need to reload userconfig which we will only allow once per lifecycle
        if( MiscCode.numCodes() == 0 ){

            // NOTE: We display parse errors to user before quiting so user knows to correct XML file.
            File file = new File(UserConfig.getPath());

            if( file.canRead() ) {
                try {
                    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = fact.newDocumentBuilder();
                    Document doc = builder.parse( file.getCanonicalPath() );
                    Node root = doc.getDocumentElement();

                    // Expected format: <userConfiguration> <codes>...</codes> <globals>...</globals> </userConfiguration>
                    for( Node node = root.getFirstChild(); node != null; node = node.getNextSibling() ) {
                        if( node.getNodeName().equalsIgnoreCase( "codes" ) )
                            parseUserCodes( file, node );
                        else if( node.getNodeName().equalsIgnoreCase( "globals" ) )
                            parseUserGlobals( file, node );
                    }
                } catch( SAXParseException e ) {
                    handleUserCodesParseException( file, e );
                } catch( Exception e ) {
                    handleUserCodesGenericException( file, e );
                }
            } else {
                // Alert and quit.
                showFatalWarning("Failed to load user codes","Failed to find required file.\n" + file.getAbsolutePath());
            }
        }
    }
*/

/*    // Parse codes from given <codes> tag.
    private void parseUserCodes( File file, Node codes ) {
        for( Node n = codes.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                String          name        = map.getNamedItem( "name" ).getTextContent();


                try {
                    MiscCode.addCode( new MiscCode( value, name ) );
                } catch (Exception e) {
                    handleUserCodesError( file, String.format("Failed to add code.\n%s", e.getMessage()) );
                }
            }
        }
    }*/

    // Parse globals from given <globals> tag.
    private void parseUserGlobals( File file, Node globals ) {
        for( Node n = globals.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "global" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                Node            nodeDefaultRating   = map.getNamedItem( "defaultRating" );
                Node            nodeMinRating       = map.getNamedItem( "minRating" );
                Node            nodeMaxRating       = map.getNamedItem( "maxRating" );
                String          name        = map.getNamedItem( "name" ).getTextContent();
                String          label       = map.getNamedItem( "label" ).getTextContent();
                GlobalCode      code        = new GlobalCode( value, name, label );

                if( nodeDefaultRating != null )
                    code.defaultRating = Integer.parseInt( nodeDefaultRating.getTextContent() );
                if( nodeMinRating != null )
                    code.minRating = Integer.parseInt( nodeMinRating.getTextContent() );
                if( nodeMaxRating != null )
                    code.maxRating = Integer.parseInt( nodeMaxRating.getTextContent() );

                if( code.defaultRating < code.minRating ||
                        code.defaultRating > code.maxRating ||
                        code.maxRating < code.minRating ) {
                    handleUserCodesError( file, "Invalid range for global code: " + code.name +
                            ", minRating: " + code.minRating +
                            ", maxRating: " + code.maxRating +
                            ", defaultRating: " + code.defaultRating );
                }

                if( !GlobalCode.addCode( code ) )
                    handleUserCodesError( file, "Failed to add global code." );
            }
        }
    }




    private void resetUtteranceCoding() {
        numUninterruptedUncodes = 0;
    }


    /************************************************************
     * Update utterance displays (e.g. current, last, etc) in active template view
     */
    private synchronized void updateUtteranceDisplays() {
        // display full string of previous utterance
        Utterance last = getUtteranceList().last();
        lblPrevUtr.setText(last == null ? "" : last.displayCoded());
    }


    /*************************************************************
     * Parse user controls from XML file.
     *************************************************************/
    private void parseUserControls() {

        File file = new File(UserConfig.getPath());

        if( file.exists() ) {
            try {

                /* Expected format:
                 * <userConfiguration>
                 *   <codes>
                 *    ...
                 *   </codes>
                 *   <codeControls panel="left" label="Therapist">
                 *     ...
                 *   </codeControls>
                 *   <codeControls panel="right" label="Client">
                 *     ...
                 *   </codeControls>
                 * </userConfiguration>
                 */

                DocumentBuilderFactory  fact    = DocumentBuilderFactory.newInstance();
                DocumentBuilder         builder = fact.newDocumentBuilder();
                Document                doc     = builder.parse(file);
                doc.getDocumentElement().normalize();

                switch (getGuiState()) {

                    case MISC_CODING:

                        // just get nodes for controls
                        NodeList controlNodeList = doc.getElementsByTagName("codeControls");
                        // iterate each child node
                        for (int cn = 0; cn < controlNodeList.getLength(); ++cn) {
                            Node node = controlNodeList.item(cn);

                            // Get panel name.  Must be "left" or "right".
                            NamedNodeMap map = node.getAttributes();
                            String panelName = map.getNamedItem("panel").getTextContent();
                            String panelLabel = map.getNamedItem("label").getTextContent();
                            GridPane gridpane = null;
                            TitledPane titledpane = null;

                            // Lookup panel.
                            if (panelName.equalsIgnoreCase("left")) {
                                gridpane = pnlCodesLeft;
                                titledpane = titlePnlCodesLeft;
                            } else if (panelName.equalsIgnoreCase("right")) {
                                gridpane = pnlCodesRight;
                                titledpane = titlePnlCodesRight;
                            }

                            // Parse controls, create border with given label.
                            if (gridpane == null) {
                                handleUserCodesError(file, "codeControls panel unrecognized: " + panelName);
                            } else {
                                parseControlColumn(node, gridpane);
                                titledpane.setText(panelLabel);
                            }
                        }

                        break;


                    case GLOBAL_CODING:

                        // track if left or right side
                        int gridColIndx = 0;
                        int gridRowIndx = 0;

                        // just get nodes for controls
                        controlNodeList = doc.getElementsByTagName("globalControls");
                        // iterate each child node ("left" or "right")
                        for (int cn = 0; cn < controlNodeList.getLength(); ++cn) {
                            Node node = controlNodeList.item(cn);

                            NamedNodeMap map = node.getAttributes();
                            String panelSide = map.getNamedItem("panel").getTextContent();
                            if (panelSide.equalsIgnoreCase("left")) {
                                gridColIndx = 0;
                                gridRowIndx = 0;
                            } else if (panelSide.equalsIgnoreCase("right")) {
                                gridColIndx = 1;
                                gridRowIndx = 0;
                            }


                            for( Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {

                                if( row.getNodeName().equals("slider")){
                                    NamedNodeMap rowMap = row.getAttributes();
                                    String globalName = rowMap.getNamedItem("global").getNodeValue();
                                    // get code by name
                                    GlobalCode code = GlobalCode.codeWithName( globalName );

                                    if( code != null ) {

                                        // use HBox node for spacing
                                        HBox hb = new HBox(6.0);
                                        // create togglegroup
                                        ToggleGroup tg = new ToggleGroup();
                                        tg.setUserData(code.name);

                                        // create radio buttons with values and defaults
                                        // do this after radio buttons added
                                        // here i'm just using this to fire a save of globals data
                                        // right away instead of as previously where we waited until
                                        // user left scene
                                        for(int i = code.minRating; i <= code.maxRating; i++) {
                                            RadioButton rb = createRadioButton(i, tg);
                                            // set selected id
                                            if( i == code.defaultRating ) {
                                                tg.selectToggle(rb);
                                            }
                                            hb.getChildren().add(rb);
                                        }

                                        // handle toggle id changes
                                        tg.selectedToggleProperty().addListener( (ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {

                                            // get code name stored in toggle group userdata
                                            String codeName = newValue.getToggleGroup().getUserData().toString();
                                            // get code by that name from globals data model
                                            GlobalCode gc = GlobalCode.codeWithName(codeName);
                                            // cast Toggle to source class to get selected id
                                            RadioButton rb = (RadioButton) newValue;
                                            // update code with new id
                                            //gc.id = Integer.getInteger(rb.getText());
                                            // update code in data model
                                            //globalsData.setRating(gc, Integer.getInteger(rb.getText(), gc.defaultRating));
                                            globalsData.setRating(gc, Integer.valueOf(rb.getText()));
                                            // TODO: duplicate code alert!!! see setGuiState()
                                            // TODO: use savetextfile or what???
                                            if( !tfGlobalsNotes.getText().isEmpty() ) {
                                                globalsData.setNotes(tfGlobalsNotes.getText());
                                            }
                                            globalsData.writeToFile();
                                        });

                                        // set toggle group label
                                        Label lbl1 = new Label(code.label);
                                        // use vbox node for label and controls
                                        VBox vb1 = new VBox(4.0, lbl1, hb);
                                        // drop VBox into GridPane node
                                        gpGlobalControls.add(vb1, gridColIndx, gridRowIndx);

                                        gridRowIndx++;

                                    }
                                } else if( row.getNodeName().equals("spacer")) {
                                    gridRowIndx++;
                                }

                            }

                        }

                        break;
                }


            } catch( SAXParseException e ) {
                handleUserCodesParseException( file, e );
            } catch( Exception e ) {
                handleUserCodesGenericException( file, e );
            }
        } else {
            // Alert and quit.
            showFatalWarning("Failed to load user codes","Failed to find required file.\n" + file.getAbsolutePath());
        }
    }


    /*******************************************************************
     * Parse a column of controls from given XML node.
     * Add buttons to given panel, and set panel layout.
     * Each child of given node is expected to be one row of controls.
     * @param node xml node
     * @param panel parent fxml node
     *******************************************************************/
    private void parseControlColumn( Node node, GridPane panel ) {

        int activeRow = 0;
        int activeCol = 0;

        for( Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
            if( row.getNodeName().equalsIgnoreCase( "row" ) ) {
                activeRow ++;

                for( Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
                    if( cell.getNodeName().equalsIgnoreCase("button")) {
                        activeCol ++;
                        NamedNodeMap map = cell.getAttributes();
                        String codeName = map.getNamedItem( "code" ).getTextContent();

                        Button button = new Button(codeName);
                        button.setOnAction(this::btnActCode);
                        // TODO: make varible or class for this button widths

                        button.setMinWidth(utrCodeButtonWidth);
                        button.setMinHeight(22);
                        button.setMaxWidth(utrCodeButtonWidth);
                        button.setMaxHeight(22);
                        button.getStyleClass().add("btn-dark-blue");
                        panel.add(button, activeCol, activeRow, 1, 1);
                    }
                }

                activeCol = 0;

            }
        }
    }

    private RadioButton createRadioButton( int buttonValue, ToggleGroup tg) {
        RadioButton rb = new RadioButton(Integer.toString(buttonValue));
        rb.setToggleGroup(tg);
        return rb;
    }



    private void setGuiState( GuiState gs) {

        if( guiState != null ) {

            // do stuff before changing state
            if (getGuiState().equals(GuiState.GLOBAL_CODING)) {
                if( globalsData != null ) {
                    if( !tfGlobalsNotes.getText().isEmpty() ) {
                        globalsData.setNotes(tfGlobalsNotes.getText());
                    }
                    globalsData.writeToFile();
                }
            }

        }

        // change state
        guiState = gs;
    }


    /**
     *
     * @return
     */
    private GuiState getGuiState() {
        return guiState;
    }


    /**
     * This clears coding controls leaving only the playback device
     */
    private void resetUserControlsContainer() {
        // check length again before removing
        if( vbApp.getChildren().size() > 2 ) {
            // remove usercontrols content node. At some point i determined that remove add worked better than setContent()
            vbApp.getChildren().remove(2);
        }
    }


    /**
     * Set player buttons to correct general state for GUI
     */
    private void setPlayerButtonState(){

        // display controls needed for coding
        switch (getGuiState()) {

            case PLAYBACK:

                sldSeek.setDisable(false);
                btnPlayPause.setMinWidth(96.0);
                btnPlayPause.setVisible(true);
                btnPlayPause.setDisable(false);
                btnReplay.setMinWidth(0.0);
                btnReplay.setVisible(false);
                btnReplay.setDisable(true);
                btnUncode.setMinWidth(0.0);
                btnUncode.setVisible(false);
                btnUncode.setDisable(true);
                btnUncodeReplay.setMinWidth(0.0);
                btnUncodeReplay.setVisible(false);
                btnUncodeReplay.setDisable(true);
                btnRewind.setDisable(false);
                break;

            case MISC_CODING:

                sldSeek.setDisable(false);
                btnPlayPause.setMinWidth(96.0);
                btnPlayPause.setVisible(true);
                btnPlayPause.setDisable(false);
                btnReplay.setMinWidth(96.0);
                btnReplay.setVisible(true);
                btnReplay.setDisable(false);
                btnUncode.setMinWidth(96.0);
                btnUncode.setVisible(true);
                btnUncodeReplay.setMinWidth(96.0);
                btnUncodeReplay.setVisible(true);
                btnRewind.setDisable(false);
                btnReplay.getParent().autosize();
                if(!isUncodeAvailable()){
                    btnUncodeReplay.setDisable(true);
                    btnUncode.setDisable(true);
                } else {
                    btnUncodeReplay.setDisable(false);
                    btnUncode.setDisable(false);
                }
                break;

            case GLOBAL_CODING:

                sldSeek.setDisable(false);
                btnPlayPause.setMinWidth(96.0);
                btnPlayPause.setVisible(true);
                btnPlayPause.setDisable(false);
                btnReplay.setMinWidth(0.0);
                btnReplay.setMaxWidth(0.0);
                btnReplay.setVisible(false);
                btnUncode.setMinWidth(0.0);
                btnUncode.setMaxWidth(0.0);
                btnUncode.setVisible(false);
                btnUncodeReplay.setMinWidth(0.0);
                btnUncodeReplay.setMaxWidth(0.0);
                btnUncodeReplay.setVisible(false);
                btnRewind.setDisable(false);
                break;
        }
    }


    private void verifyUserConfig() {

        if( ! UserConfig.exists() ) {

            Locale locale = new Locale("en", "US");
            ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(resourceStrings.getString("alert.config.title"));
            alert.setContentText(resourceStrings.getString("alert.config.text"));

            ButtonType buttonTypeOne = new ButtonType(resourceStrings.getString("alert.config.btn1.text"));
            ButtonType buttonTypeTwo = new ButtonType(resourceStrings.getString("alert.config.btn2.text"));
            ButtonType buttonTypeCancel = new ButtonType(resourceStrings.getString("alert.config.btn3.text"), ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonTypeOne){
                // create new using default path
                selectConfigFile(UserConfig.getPath());
                try {
                    UserConfig.writeDefault();
                } catch (IOException e) {
                    showError("File Write Error", "Could not write user config file");
                }
            } else if (result.get() == buttonTypeTwo) {
                // select existing by sending no path
                selectConfigFile("");
            } else {
                System.exit(0);
            }

        }
    }


}
