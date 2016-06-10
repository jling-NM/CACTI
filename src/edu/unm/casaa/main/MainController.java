package edu.unm.casaa.main;

import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.*;
import edu.unm.casaa.globals.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.w3c.dom.*;
import org.xml.sax.SAXParseException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.lang.String.format;

//import com.aquafx_project.AquaFx;

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
    private ToggleButton btnStartCoding;
    @FXML
    private SwingNode snTimeline;
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
    private int bytesPerSecond           = 0;           // legacy Cached when we load audio file.
    private int audioLength              = 0;           // legacy to be noted later

    // integrate
    private int numSaves                 = 0;           // Number of times we've saved since loading current session data.
    private int numUninterruptedUncodes  = 0;           // Number of times user has uncoded without doing anything else.
    private String filenameMisc          = null;        // name of active CASAA data file.
    private String filenameGlobals       = null;        // name of active globals data file
    private String filenameAudio         = null;        // name of active media file
    private File currentAudioFile        = null;        // active media file
    private UtteranceList utteranceList  = null;        // MISC coding data
    private GlobalDataModel globalsData  = null;        // GLOBALS scoring data

    private enum  GuiState {                            // available gui states
        PLAYBACK, MISC_CODING, GLOBAL_CODING
    }
    private GuiState guiState;                          // for referencing state




    /******************************************************************
     * controller initialization tasks
     ******************************************************************/
    @FXML
    private void initialize() {
        //
        setGuiState(GuiState.PLAYBACK);

        // initialize app persistence
        appPrefs = Preferences.userNodeForPackage(Main.class);

        // OSX CSS
        //if( System.getProperty("os.name","UNKNOWN").equals("Mac OS X")) {
        //    AquaFx.style();
        //}

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
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            // if coding, make sure to deselect "Coding..." toggle button.
            if( (getGuiState().equals(GuiState.MISC_CODING)) && (btnStartCoding != null) )  {
                btnStartCoding.setSelected(false);
            }
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
            mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5.0)));
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

        Utterance   utterance   = getCurrentUtterance();
        int         pos         = 0;

        if( utterance != null ) {
            // Position one second before start of utterance.
            pos = utterance.getStartBytes() - bytesPerSecond;
            pos = Math.max( pos, 0 ); // Clamp.
        }

        setMediaPlayerPositionByBytes( pos );
    }


    /**********************************************************************
     *  button event: Remove last utterance
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActUncode(ActionEvent actionEvent) {
        uncode();
        saveSession();
        incrementUncodeCount();
    }


    /**********************************************************************
     *  button event: Uncode last utterance and replay it, i think
     *  @param actionEvent not used
     **********************************************************************/
    public void btnActUncodeReplay(ActionEvent actionEvent) {
        uncode();
        saveSession();
        incrementUncodeCount();

        Utterance   utterance   = getCurrentUtterance();
        int         pos         = 0;

        if( utterance != null ) {
            // Position one second before start of utterance.
            pos = utterance.getStartBytes() - bytesPerSecond;
            pos = Math.max( pos, 0 ); // Clamp.
        }

        setMediaPlayerPositionByBytes( pos );
    }


    /**********************************************************************
     *  button event: Begin coding on a new utterance session
     *  @param actionEvent not used
     **********************************************************************/
    public void btnActStartCoding(ActionEvent actionEvent) {

        // Cache stream position, as it may change over repeated queries (because it advances
        // with player thread).
        int position = getStreamPosition();

        // Start/resume playback.
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {

            mediaPlayer.pause();
            sldSeek.setDisable(true);
            btnStartCoding.setText("Start Coding");
            btnUncodeReplay.setDisable(true);
            btnUncode.setDisable(true);
            btnReplay.setDisable(true);
            btnRewind.setDisable(true);

        } else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {

            mediaPlayer.play();
            sldSeek.setDisable(false);
            btnStartCoding.setText("Stop Coding");
            btnUncodeReplay.setDisable(false);
            btnUncode.setDisable(false);
            btnReplay.setDisable(false);
            btnRewind.setDisable(false);

        }

        if( getUtteranceList().size() > 0 )
            return; // Parsing starts only once.

        // Record start data.
        String startString = TimeCode.toString( position / bytesPerSecond );

        // Create first utterance.
        Utterance data = new MiscDataItem( 0, startString, position );

        getUtteranceList().add( data );

        numUninterruptedUncodes = 0;
        updateUtteranceDisplays();
    }



    /**********************************************************************
     *  button event: Apply utterance code
     **********************************************************************/
    private void btnActCode(ActionEvent actionEvent) {
        Button src = (Button) actionEvent.getSource();
        MiscCode mc = MiscCode.codeWithName(src.getText());
        handleButtonMiscCode(mc);
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

        // reset utteranceList to start fresh
        utteranceList = null;

        // Default casaa filename to match audio file, with .casaa suffix.
        String newFileName = Utils.changeSuffix( audioFile.getName(), "casaa" );
        File miscFile = selectMiscFile(newFileName);
        if( miscFile == null ) {
            return;
        }
        filenameMisc = miscFile.getAbsolutePath();

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

        // load audio file and utterancelist at same time
        // TODO: consider clarifying this with separate class
        // TODO: we should trap errors reading the codes here too
        filenameAudio = getUtteranceList().loadFromFile(miscFile);
        File audioFile = new File(filenameAudio);
        if (audioFile.canRead()) {
            initializeMediaPlayer(audioFile, playerReady);
        } else {
            showError("File Error", format("%s\n%s\n%s", "Could not load audio file:", filenameAudio, "Check that it exists and has read permissions"));
        }


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

                // legacy TODO: notes
                setAudioLength(mediaFile);
                setBytesPerSecond(mediaFile);

                /* Status Handler: OnReady */
                mediaPlayer.setOnReady(onReadyMethod);

                /* Status Handler: OnPlaying - lambda runnable when mediaplayer starts playing */
                mediaPlayer.setOnPlaying(() -> btnPlayImgVw.getStyleClass().add("img-btn-pause"));

                /* Status Handler:  OnPaused */
                mediaPlayer.setOnPaused(() -> {
                    // assumes OnPlay has overlayed style class so just remove that to expose pause class
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });

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



                /* Listener: Update the media position if user is dragging the slider.
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

                sldRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    /* if dragging slider, update media playback rate */
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


                    // activate the timeline display
                    snTimeline.setContent(new Timeline(this));

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
                    // update the timeline
                    updateTimeLineDisplay();
                });


                /***
                 * BEGIN: initialize active utterance
                 * we want to initialize the active utterance record to use it to resuming coding.
                 * we do so by taking the last utterance, using its end time and end bytes as the
                 * start time/start bytes of a new utterance.
                 ***/
                Utterance utterance = getCurrentUtterance();


                int mediaplayerSeekBytes = 0;

                // We expect utterances in file to be coded.  For backwards compatibility,
                // tolerate uncoded utterances in file.
                if( utterance != null ) {
                    if( utterance.isCoded() ) {
                        // Start new utterance.
                        int 		position		= utterance.getEndBytes();
                        String 		positionString 	= TimeCode.toString( position / getBytesPerSecond() );
                        int         order 		  	= getUtteranceList().size();

                        // create new utterance from relevant last utterance data
                        Utterance   data		 	= new MiscDataItem( order, positionString, position );

                        // add this to our utterance listing and it is our active utterance
                        getUtteranceList().add( data );
                        // update mediaplayer position appropriately for our now active utterance
                        mediaplayerSeekBytes = utterance.getEndBytes();
                    }
                    else {
                        // Tolerate uncoded final utterance.  Strip end data, so it is consistent
                        // with how we treat current utterance.  NOTE: This does not check for
                        // uncoded utterances anywhere else in file.
                        utterance.stripEndData();
                        // update mediaplayer position appropriately for our now active utterance
                        mediaplayerSeekBytes = utterance.getStartBytes();
                    }

                }

                /***
                 * END: initialize active utterance
                 ***/


                /**
                 * adjust player position
                 */
                double positionInSecs = mediaplayerSeekBytes/getBytesPerSecond();
                onReadySeekDuration = Duration.seconds(positionInSecs);
                mediaPlayer.seek(onReadySeekDuration);
                lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
                sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());

                // update the utterance data(previous/current) displayed in the gui
                updateUtteranceDisplays();

                // update timeline display as player seek doesn't update correctly on reload
                updateTimeLineDisplay();

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
     * Set mediaplayer position using bytes
     * This makes new mediaplayer compatible with legacy code
     * @param positionInBytes provide player position in bytes
     **********************************************************************/
    private synchronized void setMediaPlayerPositionByBytes(int positionInBytes){
        double positionInSecs = positionInBytes/getBytesPerSecond();

        // pause player whether playing or not which enables seek
        mediaPlayer.pause();
        mediaPlayer.seek(Duration.seconds(positionInSecs));
        mediaPlayer.play();
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
    private synchronized UtteranceList getUtteranceList() {
        if( utteranceList == null )
            utteranceList = new UtteranceList();
        return utteranceList;
    }


    // Access utterances.
    public int numUtterances() {
        return getUtteranceList().size();
    }

    public Utterance utterance(int index ) {
        return getUtteranceList().get( index );
    }

    // Get current utterance, which is always the last utterance in list.  May be null.
    public synchronized Utterance getCurrentUtterance() {
        return getUtteranceList().last();
    }


    // Get previous utterance, or null if no previous utterance exists.
    private synchronized Utterance getPreviousUtterance() {
        int count = getUtteranceList().size();

        return (count > 1) ? getUtteranceList().get( count - 2 ) : null;
    }


    /*****************************************************
     * Store length in bytes
     * Used for backward compatibility
     * @param audioFile file to inspect
     *****************************************************/
    private void setAudioLength(java.io.File audioFile) {
        try {
            javax.sound.sampled.AudioInputStream as = javax.sound.sampled.AudioSystem.getAudioInputStream(audioFile);
            audioLength = as.available();

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }


    /******************************************************
     * Store rate for audio file
     * Used for backward compatibility
     * @param audioFile file to inspect
     *****************************************************/
    private void setBytesPerSecond(java.io.File audioFile){
        try {
            javax.sound.sampled.AudioFileFormat m_audioFileFormat = AudioSystem.getAudioFileFormat(audioFile);
            bytesPerSecond = (m_audioFileFormat.getFormat().getFrameSize() * (new Float(m_audioFileFormat.getFormat().getFrameRate()).intValue()));

        } catch (UnsupportedAudioFileException | IOException e) {
            showError("Error", e.toString());
        }

    }


    /***********************************************
     * @return audio bytes per second.
     ***********************************************/
    public int getBytesPerSecond() {
        return bytesPerSecond;
    }


    /***********************************************
     * @return audio file length, in bytes.
     ***********************************************/
    public int getAudioLength() {
        return audioLength;
    }


    /***********************************************
     * @return current playback position, in bytes.
     ***********************************************/
    public int getStreamPosition() {
        return Utils.convertTimeToBytes(getBytesPerSecond(), mediaPlayer.getCurrentTime());
    }


    /********************************************************
     * Undo the actions of pressing a MISC code button.
     ********************************************************/
    private synchronized void uncode() {
        // Remove last utterance, if uncoded (utterance was
        // generated when user coded the second-to-last utterance).
        UtteranceList 	list 	= getUtteranceList();
        Utterance 		u 		= list.last();

        if( u != null && !u.isCoded() )
            list.removeLast();

        // Strip code and end data from last remaining utterance.
        u = list.last();
        if( u != null )
        {
            u.stripEndData();
            u.stripMiscCode();
        }
        updateUtteranceDisplays();
    }


    /*********************************************************
     * Save current session. Periodically also save backup copy.
     *********************************************************/
    private synchronized void saveSession() {
        // Save normal file.
        saveCurrentTextFile( false );

        // Backup every n'th save.
        if( numSaves % 10 == 0 ) {
            saveCurrentTextFile( true );
        }
        numSaves++;
    }



    private synchronized void saveCurrentTextFile( boolean asBackup ) {

        String filename;

        switch (getGuiState()) {

            case MISC_CODING:
                filename = filenameMisc;
                if( asBackup ) { filename += ".backup"; }
                getUtteranceList().writeToFile( new File( filename ), filenameAudio );
                break;

            case GLOBAL_CODING:
                filename = filenameGlobals;
                if( asBackup ) { filename += ".backup"; }
                //writeGlobalsToFile( new File( filename ), filenameAudio );
                // TODO why no save when save works
                System.out.println("Write Globals to File");
                break;
        }
    }



    private synchronized void incrementUncodeCount() {
        numUninterruptedUncodes++;
        if( numUninterruptedUncodes >= 4 ) {
            showError( "Uncode Warning", "You have uncoded 4 times in a row." );
            numUninterruptedUncodes = 0;
        }
    }



    private synchronized void handleButtonMiscCode(MiscCode miscCode) {

        assert (miscCode.isValid());

        // Assign code to current utterance, if one exists.
        Utterance utterance = getCurrentUtterance();
        if( utterance == null )
            return; // No current utterance.

        // get current player position
        int position = getStreamPosition();

        if( position <= utterance.getStartBytes() )
            return; // Ignore when playback is outside utterance.
        if( utterance.isCoded() )
            return; // Ignore if already coded.

        // assign code to utterance
        utterance.setMiscCode( miscCode );
        // number of uncoding events goes back to zero
        numUninterruptedUncodes = 0;

        // End utterance.
        assert (bytesPerSecond > 0);
        String positionString = TimeCode.toString( position / bytesPerSecond );
        //Utils.convertTimeToBytes();

        utterance.setEndTime( positionString );
        utterance.setEndBytes( position );

        // Start new utterance.
        int         order   = getUtteranceList().size();
        Utterance   data    = new MiscDataItem( order, positionString, position );

        getUtteranceList().add( data );
        updateUtteranceDisplays();
        saveSession();
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
            File file = new File( "userConfiguration.xml" );

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

    // Parse codes from given <codes> tag.
    private void parseUserCodes( File file, Node codes ) {
        for( Node n = codes.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                String          name        = map.getNamedItem( "name" ).getTextContent();

                if( !MiscCode.addCode( new MiscCode( value, name ) ) )
                    handleUserCodesError( file, "Failed to add code." );
            }
        }
    }

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
        // Reset save counter, so we backup on next save (i.e. as
        // soon as player saves changes to newly loaded data).
        numSaves = 0;
    }


    /************************************************************
     * Update utterance displays (e.g. current, last, etc) in active template view
     */
    private synchronized void updateUtteranceDisplays() {

        // display full string of previous utterance
        Utterance        prev    = getPreviousUtterance();
        if( prev == null )
            lblPrevUtr.setText( "" );
        else
            lblPrevUtr.setText( prev.toString() );


        // display individual fields of the active utterance
        Utterance        current = getCurrentUtterance();
        if( current == null ) {
            lblCurUtrEnum.setText( "" );
            lblCurUtrCode.setText( "" );
            lblCurUtrStartTime.setText( "" );
            lblCurUtrEndTime.setText( "" );
        } else {
            lblCurUtrEnum.setText( "" + current.getEnum() );
            if( current.getMiscCode().value == MiscCode.INVALID )
                lblCurUtrCode.setText( "" );
            else
                lblCurUtrCode.setText( current.getMiscCode().name );

            lblCurUtrStartTime.setText( current.getStartTime() );
            lblCurUtrEndTime.setText( current.getEndTime() );

            // Visual indication when in between utterances.
            if( getStreamPosition() < current.getStartBytes() )
                lblCurUtrStartTime.setStyle("-fx-text-fill: 'Red';");
            else
                lblCurUtrStartTime.setStyle("-fx-text-fill: 'Black';");
        }
    }


    private void updateTimeLineDisplay() {

        // TODO: this doesn't work
        //if (snTimeline.getContent().isValid()) {
        snTimeline.getContent().repaint();
        //}
        //snTimeline.getContent().repaint();
        //playerView.getTimeline().repaint();
/*        if (bytesPerSecond != 0) {
            // Handles constant bit-rates only.

            int bytes = player.getEncodedStreamPosition();
            int seconds = bytes / bytesPerSecond;

            playerView.setLabelTime("Time  " + TimeCode.toString(seconds));
        } else {
            // EXTEND: Get time based on frames rather than bytes.
            // Need a way to determine current position based on frames.
            // Something like getEncodedStreamPosition(),
            // but that returns frames. This for VBR type compressions.
        }*/
    }


    /*************************************************************
     * Parse user controls from XML file.
     *************************************************************/
    private void parseUserControls() {

        File file = new File( "userConfiguration.xml" );

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
                        button.setMinWidth(64);
                        button.setMinHeight(24);
                        button.setMaxWidth(64);
                        button.setMaxHeight(24);
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
     * TODO
     */
    private void resetUserControlsContainer() {
        // check length again before removing
        if( vbApp.getChildren().size() > 2 ) {
            // remove usercontrols content node. At some point i determined that remove add worked better than setContent()
            vbApp.getChildren().remove(2);
        }
    }


    /**
     * TODO
     */
    private void setPlayerButtonState(){

        // display controls needed for coding
        switch (getGuiState()) {

            case PLAYBACK:

                sldSeek.setDisable(false);
                btnPlayPause.setMinWidth(58.0);
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

                sldSeek.setDisable(true);
                btnPlayPause.setMinWidth(0.0);
                btnPlayPause.setMaxWidth(0.0);
                btnPlayPause.setVisible(false);
                btnPlayPause.setDisable(true);
                btnReplay.setMinWidth(58.0);
                btnReplay.setVisible(true);
                btnReplay.setDisable(true);
                btnUncode.setMinWidth(58.0);
                btnUncode.setVisible(true);
                btnUncode.setDisable(true);
                btnUncodeReplay.setMinWidth(58.0);
                btnUncodeReplay.setVisible(true);
                btnUncodeReplay.setDisable(true);
                btnRewind.setDisable(true);
                btnReplay.getParent().autosize();
                break;

            case GLOBAL_CODING:

                sldSeek.setDisable(false);
                btnPlayPause.setMinWidth(58.0);
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

}
