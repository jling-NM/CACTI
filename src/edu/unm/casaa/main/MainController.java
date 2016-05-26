package edu.unm.casaa.main;

import edu.unm.casaa.misc.MiscAction;
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
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;


public class MainController {

    @FXML
    private TitledPane titlePnlCodesLeft;
    @FXML
    private TitledPane titlePnlCodesRight;
    @FXML
    private GridPane pnlCodesLeft;
    @FXML
    private GridPane pnlCodesRight;
    @FXML
    private AnchorPane pnCoding;
    @FXML
    private Button btnReplay;
    @FXML
    private Button btnUncode;
    @FXML
    private Button btnUncodeReplay;
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



    // mediaplayer attributes
    private Duration totalDuration;
    private int bytesPerSecond           = 0; // legacy Cached when we load audio file.
    private int audioLength              = 0; // legacy to be noted later

    // integrate
    private int numSaves                 = 0;                // Number of times we've saved since loading current session data.
    private int numUninterruptedUncodes  = 0;                // Number of times user has uncoded without doing anything else.
    private String filenameMisc          = null;			 // CASAA file.
    private String filenameGlobals       = null;
    private String filenameAudio         = null;
    private UtteranceList utteranceList  = null;
    private String globalsLabel          = "Global Ratings"; // Label for global template view.



    /* handle controller initialization tasks */
    @FXML
    private void initialize() {
        System.out.println("Controller Initializing...");
        // load user config file to load user specific edited codes
        parseUserConfig();
    }



    // define lambda runnable later called by player when ready with a media loaded
    Runnable playerReady = () -> {
        System.out.println("playerReady: MEDIAPLAYER: OnReady");

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
        lblDuration.setText(Utils.formatDuration(totalDuration));

        // TODO: clear or disable or hide coding stuff check all this is here
        Duration onReadySeekDuration = Duration.ZERO;
        mediaPlayer.seek(onReadySeekDuration);
        lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
        sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());
        // update the utterance data(previous/current) displayed in the gui
        updateUtteranceDisplays();
    };




    // define lambda runnable later called by player when ready with a media loaded
    Runnable playerReadyToCode = () -> {
        System.out.println("MEDIAPLAYER: Ready for Coding");

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
        lblDuration.setText(Utils.formatDuration(totalDuration));


        /***
         * BEGIN: initialize active utterance
         * we want to initialize the active utterance record to use it to resuming coding.
         * we do so by taking the last utterance, using its end time and end bytes as the
         * start time/start bytes of a new utterance.         *
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


        double positionInSecs = mediaplayerSeekBytes/getBytesPerSecond();
        Duration onReadySeekDuration = Duration.seconds(positionInSecs);
        mediaPlayer.seek(onReadySeekDuration);
        lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
        sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());
        // update the utterance data(previous/current) displayed in the gui
        updateUtteranceDisplays();
        // update timeline display as player seek doesn't update correctly on reload
        updateTimeLineDisplay();

        // temp button generation
        parseUserControls();
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

            updateUtteranceDisplays();
        }
    }


    /**********************************************************************
     *  button event: Seek to beginning of current utterance.  Seek a little further back
     *  to ensure audio synchronization issues don't cause player to actually
     *  seek later than beginning of utterance.
     *  @param actionEvent
     **********************************************************************/
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
     *  @param actionEvent
     **********************************************************************/
    public void btnActUncode(ActionEvent actionEvent) {
        uncode();
        saveSession();
        incrementUncodeCount();
    }

    /**********************************************************************
     *  button event: Uncode last utterance and replay it, i think
     *  @param actionEvent
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
     *  button event: Begin utterance coding
     *  @param actionEvent
     **********************************************************************/
    public void btnActStartCoding(ActionEvent actionEvent) {

        // Cache stream position, as it may change over repeated queries (because it advances
        // with player thread).
        int position = streamPosition();

        // Start/resume playback.
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.play();
        }

        if( getUtteranceList().size() > 0 )
            return; // Parsing starts only once.

        // Record start data.
        if (bytesPerSecond > 0) {
            return;
        }
        String startString = TimeCode.toString( position / bytesPerSecond );

        // Create first utterance.
        Utterance data = new MiscDataItem( 0, startString, position );

        getUtteranceList().add( data );

        numUninterruptedUncodes = 0;
        updateUtteranceDisplays();
    }


    /**********************************************************************
     *  button event: Apply utterance code
     *  @param actionEvent
     **********************************************************************/
    public void btnActCode(ActionEvent actionEvent) {
        Button src = (Button) actionEvent.getSource();
        MiscCode mc = MiscCode.codeWithName(src.getText());
        handleButtonMiscCode(mc);
    }





    /**********************************************************************
     * Menu event handlers
     **********************************************************************/

    /**********************************************************************
     * menu selection event: About
     * @param actionEvent
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



    /**********************************************************************
     * menu selection event: Help
     * @param actionEvent
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
     * @param actionEvent
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
     * @param actionEvent
     **********************************************************************/
    public void mniActOpenFile(ActionEvent actionEvent) {
        File selectedFile = selectAudioFile();
        if (selectedFile != null) {
            initializeMediaPlayer(selectedFile, playerReady);
        }
        // hide controls needed for coding
        setMiscCodingControlVisibility(false);
    }



    /**********************************************************************
     * Begin Misc Coding
     * Load audio file and create corresponding coding output file.
     * Initialize the mediaplayer.
     * Activate the coding controls.
     * @param actionEvent
     */
    public void mniStartCoding(ActionEvent actionEvent) {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        // Select audio file.
        File audioFile = selectAudioFile();
        if( audioFile == null )
            return;
        filenameAudio = audioFile.getAbsolutePath();

        // Default casaa filename to match audio file, with .casaa suffix.
        String newFileName = changeSuffix( audioFile.getName(), "casaa" );
        File miscFile = selectMiscFile(newFileName);
        filenameMisc = miscFile.getAbsolutePath();

        // display path in gui
        lblCurMiscFile.setText(miscFile.getAbsolutePath());
        //
        initializeMediaPlayer(audioFile, playerReadyToCode);

        // activate the timeline display
        snTimeline.setContent(new Timeline(this));

        // temp button generation
        // in callback now
        //parseUserControls();

        // display controls needed for coding
        setMiscCodingControlVisibility(true);
    }



    /******************************************************
     * Resume Misc Coding
     * Load coding file and corresponding audio file.
     * Initialize mediaplayer.
     * Activate timeline control updating it for utterance data
     * @param actionEvent
     ******************************************************/
    public void mniResumeCoding(ActionEvent actionEvent) {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        // user selects a casaa file or we leave
        File miscFile = selectMiscFile("");
        if( miscFile == null ) {
            showError("File Error", "Could not open coding file");
            return;
        }
        filenameMisc = miscFile.getAbsolutePath();


        // load audio file and utterancelist at same time
        // TODO: consider clarifying this with separate class
        // TODO: we should trap errors reading the codes here too
        filenameAudio = getUtteranceList().loadFromFile(miscFile);
        File audioFile = new File(filenameAudio);
        if (audioFile.canRead()) {
            initializeMediaPlayer(audioFile, playerReadyToCode);
        } else {
            showError("File Error", String.format("%s\n%s\n%s", "Could not load audio file:", filenameAudio, "Check that it exists and has read permissions"));
            return;
        }


        // display coding file path in gui
        lblCurMiscFile.setText(miscFile.getAbsolutePath());

        // reset some utterance accounting
        resetUtteranceCoding();


        // activate the timeline display
        snTimeline.setContent(new Timeline(this));

        // display controls needed for coding
        setMiscCodingControlVisibility(true);
    }



    /**********************************************************************
     * sldSeek mouse event:
     * change seek time when user clicks on slid bar instead of dragging the controller
     * to change the position
     * @param event
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

        double sec = totalDuration.multiply(sldSeek.getValue()).toSeconds();
        System.out.println("time:"+sec);
        System.out.println( "bytes:" + (int)(sec * getBytesPerSecond()) );
    }



    /************************************************************************
     *
     * @return Audio File object
     ***********************************************************************/
    private File selectAudioFile() {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Open audio file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav"));
        File selectedFile = fc.showOpenDialog(stageTheLabelBelongs);

        return selectedFile;
    }



    /************************************************************************
     * Specify a Misc code file for coding
     * @param newFileName
     * @return Misc Codes File object
     ************************************************************************/
    private File selectMiscFile(String newFileName) {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();

        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CASAA files", "*.casaa"));

        File selectedFile;
        if (newFileName.isEmpty()) {
            fc.setTitle("Open CASAA File");
            selectedFile = fc.showOpenDialog(stageTheLabelBelongs);
        } else {
            fc.setTitle("Name New CASAA File");
            fc.setInitialFileName(newFileName);
            selectedFile = fc.showSaveDialog(stageTheLabelBelongs);
        }
        return selectedFile;
    }



    /**********************************************************************
     * Initialize the media player state with media file
     * @param mediaFile
     **********************************************************************/
    public void initializeMediaPlayer(File mediaFile, Runnable onReadyMethod) {

        if (mediaFile != null) {
            final Media media = new Media(mediaFile.toURI().toString());

            try {

                // legacy TODO: notes
                setAudioLength(mediaFile);
                setBytesPerSecond(mediaFile);

                mediaPlayer = new MediaPlayer(media);

                /* Status Handler: OnReady */
                mediaPlayer.setOnReady(onReadyMethod);

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
                    // update display of current time
                    lblTimePos.setText(Utils.formatDuration(newValue));
                    // update the mediaplayer slider
                    sldSeek.setValue(newValue.toMillis() / totalDuration.toMillis());
                    // update the timeline
                    updateTimeLineDisplay();
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
                showError("Error Starting MediaPlayer", ex.getMessage());
            }

        }
    }


    /**********************************************************************
     * Set mediaplayer position using bytes
     * This makes new mediaplayer compatible with legacy code
     * @param positionInBytes
     **********************************************************************/
    private synchronized void setMediaPlayerPositionByBytes(int positionInBytes){
        double positionInSecs = positionInBytes/getBytesPerSecond();

        // pause player whether playing or not which enables seek
        mediaPlayer.pause();
        mediaPlayer.seek(Duration.seconds(positionInSecs));
        mediaPlayer.play();
    }



    // ******************************
    // Begin Integrate
    // ******************************

    /*******************************************************
     * Reusable method to display runtime errors
     * @param title
     * @param message
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
     * @param title
     * @param message
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
     * @return
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

    // TODO: determine if this is necessary or can just use "updateTimeLineDisplay() on its own"
    private void utteranceListChanged() {
        updateTimeLineDisplay();
    }
    // Get previous utterance, or null if no previous utterance exists.
    private synchronized Utterance getPreviousUtterance() {
        int count = getUtteranceList().size();

        return (count > 1) ? getUtteranceList().get( count - 2 ) : null;
    }




    private void setAudioLength(java.io.File audioFile) {
        try {
            javax.sound.sampled.AudioInputStream as = javax.sound.sampled.AudioSystem.getAudioInputStream(audioFile);
            audioLength = as.available();

        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setBytesPerSecond(java.io.File audioFile){
        try {
            javax.sound.sampled.AudioFileFormat m_audioFileFormat = AudioSystem.getAudioFileFormat(audioFile);
            bytesPerSecond = (m_audioFileFormat.getFormat().getFrameSize() * (new Float(m_audioFileFormat.getFormat().getFrameRate()).intValue()));

        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
    public int streamPosition() {

        int position = Utils.convertTimeToBytes(getBytesPerSecond(), mediaPlayer.getCurrentTime());
        return position;

    }


    // Undo the actions of pressing a MISC code button.
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


    // Save current session. Periodically also save backup copy.
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
       // if( templateView instanceof MiscTemplateView && filenameMisc != null ) {
            String filename = filenameMisc;

            if( asBackup ) {
                filename += ".backup";
                getUtteranceList().writeToFile( new File( filename ), filenameAudio );
            }

        // TODO: determine how coding type will be stored and complete this
//        } else if( templateView instanceof GlobalTemplateView ) {
//            String filename = filenameGlobals;
//
//            if( asBackup )
//                filename += ".backup";
//            ((GlobalTemplateUiService) templateUI).writeGlobalsToFile( new File( filename ), filenameAudio );
//        }
    }



    private synchronized void incrementUncodeCount() {
        numUninterruptedUncodes++;
        if( numUninterruptedUncodes >= 4 ) {
            showError( "Uncode Warning", "You have uncoded 4 times in a row." );
            numUninterruptedUncodes = 0;
        }
    }


    public synchronized void handleButtonMiscCode( MiscCode miscCode ) {

        assert (miscCode.isValid());

        // Assign code to current utterance, if one exists.
        Utterance utterance = getCurrentUtterance();

        if( utterance == null )
            return; // No current utterance.

        int position = streamPosition();

        if( position <= utterance.getStartBytes() )
            return; // Ignore when playback is outside utterance.
        if( utterance.isCoded() )
            return; // Ignore if already coded.

        utterance.setMiscCode( miscCode );
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



    // Handle errors re: user codes XML file. We must be able to find and parse
    // this file
    // successfully, so all of these errors are fatal.
    public void handleUserCodesParseException(File file, SAXParseException e) {
        // Alert and quit.
        this.showFatalWarning("Failed to load user codes","Parse error in " + file.getAbsolutePath() + " (line " + e.getLineNumber() + "):\n" + e.getMessage());
    }

    public void handleUserCodesGenericException(File file, Exception e) {
        this.showFatalWarning("Failed to load user codes","Unknown error parsing file: " + file.getAbsolutePath() + "\n" + e.toString());
    }

    public void handleUserCodesError(File file, String message) {
        this.showFatalWarning("Failed to load user codes", "Error loading file: " + file.getAbsolutePath() + "\n" + message);
    }


    // Parse user codes and globals from XML.
    private void parseUserConfig() {

        // cheap way to check if we need to reload userconfig which we will only allow once per lifecycle
        if( MiscCode.numCodes() == 0 ){

            // NOTE: We display parse errors to user before quiting so user knows to correct XML file.
            File file = new File( "userConfiguration.mac.xml" );

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
                        else if( node.getNodeName().equalsIgnoreCase( "globalsBorder" ) )
                            parseUserGlobalsBorder( file, node );
                    }
                } catch( SAXParseException e ) {
                    handleUserCodesParseException( file, e );
                } catch( Exception e ) {
                    handleUserCodesGenericException( file, e );
                }
            } else {
                // Alert and quit.
                this.showFatalWarning("Failed to load user codes","Failed to find required file.\n" + file.getAbsolutePath());
            }
        }
    }

    // Parse codes from given <codes> tag.
    private void parseUserCodes( File file, Node codes ) {
        for( Node n = codes.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                NamedNodeMap map         = n.getAttributes();
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

    // Parse globalsLabel from given <globalsBorder> tag.
    private void parseUserGlobalsBorder( File file, Node node ) {
        NamedNodeMap    map = node.getAttributes();

        globalsLabel = map.getNamedItem( "label" ).getTextContent();
    }

    /***********************************************************
     *
     * @param filename
     * @param newSuffix Suffixes should be specified without leading period.
     * @return copy of filename with oldSuffix (if present) removed, and newSuffix added.
     */
    private String changeSuffix( String filename, String newSuffix ) {
        String	result 	= filename;
        int		index 	= filename.lastIndexOf( '.' );

        if( index > 0 ) {
            result = result.substring( 0, index );
        }
        return result + "." + newSuffix;
    }


    private void resetUtteranceCoding() {
        utteranceList = null;
        numUninterruptedUncodes = 0;
        // Reset save counter, so we backup on next save (i.e. as
        // soon as player saves changes to newly loaded data).
        numSaves = 0;
    }


    /************************************************************
     * Update utterance displays (e.g. current, last, etc) in active template view
     */
    private synchronized void updateUtteranceDisplays() {

        // TODO: temporarily mark if we are misc coding. later see if necessary in globals mode
        if( 1 ==1 ) {

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
                if( streamPosition() < current.getStartBytes() )
                    lblCurUtrStartTime.setStyle("-fx-text-fill: 'Red';");
                else
                    lblCurUtrStartTime.setStyle("-fx-text-fill: 'Black';");
            }
        }
    }

    private void updateTimeLineDisplay() {


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


    private void setMiscCodingControlVisibility(boolean controlVisibility) {
        btnReplay.setVisible(controlVisibility);
        btnUncode.setVisible(controlVisibility);
        btnUncodeReplay.setVisible(controlVisibility);
        pnCoding.setVisible(controlVisibility);
    }



    /*************************************************************
     * Parse user controls from XML file.
     */
    private void parseUserControls() {
        File file = new File( "userConfiguration.xml" );

        if( file.exists() ) {
            try {
                DocumentBuilderFactory  fact    = DocumentBuilderFactory.newInstance();
                DocumentBuilder         builder = fact.newDocumentBuilder();
                Document                doc     = builder.parse( file.getCanonicalFile());
                Node                    root    = doc.getDocumentElement();

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
                for( Node node = root.getFirstChild(); node != null; node = node.getNextSibling() ) {
                    if( node.getNodeName().equalsIgnoreCase( "codeControls" ) ) {
                        // Get panel name.  Must be "left" or "right".
                        NamedNodeMap    map         = node.getAttributes();
                        String          panelName   = map.getNamedItem( "panel" ).getTextContent();
                        String          panelLabel  = map.getNamedItem( "label" ).getTextContent();
                        GridPane        gridpane    = null;
                        TitledPane      titledpane   = null;

                        // Lookup panel.
                        if( panelName.equalsIgnoreCase( "left" ) ) {
                            gridpane = pnlCodesLeft;
                            titledpane = titlePnlCodesLeft;
                        } else if( panelName.equalsIgnoreCase( "right" ) ) {
                            gridpane = pnlCodesRight;
                            titledpane = titlePnlCodesRight;
                        }

                        // Parse controls, create border with given label.
                        if( gridpane == null ) {
                            handleUserCodesError( file, "codeControls panel unrecognized: " + panelName );
                        } else {
                            parseControlColumn( node, gridpane );
                            titledpane.setText(panelLabel);
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
            this.showFatalWarning("Failed to load user codes","Failed to find required file.\n" + file.getAbsolutePath());
        }
    }



    /*******************************************************************
     * Parse a column of controls from given XML node.  Add buttons to given panel, and set panel layout.
     * Each child of given node is expected to be one row of controls.
     * @param node
     * @param panel
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
                        button.setMinHeight(28);
                        button.setMaxWidth(64);
                        button.setMaxHeight(28);
                        //button.setStyle("btn-misc");
                        panel.add(button, activeCol, activeRow, 1, 1);
                    }
                }

                activeCol = 0;

            } else {
                continue;
            }
        }
    }

}
