package edu.unm.casaa.main;

import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.misc.MiscTemplateView;
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
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


    // mediaplayer attributes
    private Duration totalDuration;
    private int bytesPerSecond           = 0; // legacy Cached when we load audio file.
    private int audioLength                               = 0; // legacy to be noted later

    // integrate
    private int numSaves                 = 0;                // Number of times we've saved since loading current session data.
    private int numUninterruptedUncodes  = 0;                // Number of times user has uncoded without doing anything else.
    private String               filenameMisc             = null;				// CASAA file.
    private String               filenameGlobals          = null;
    private String               filenameAudio            = null;
    private UtteranceList        utteranceList            = null;
    private String               globalsLabel             = "Global Ratings";   // Label for global template view.



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

    /**********************************************************************
     * menu selection event: Open File
     * @param actionEvent
     **********************************************************************/
    public void mniActOpenFile(ActionEvent actionEvent) {

        File selectedFile = selectAudioFile();
        if (selectedFile != null) {
            final Media media = new Media(selectedFile.toURI().toString());

            try {

                // legacy TODO: notes
                setAudioLength(selectedFile);
                setBytesPerSecond(selectedFile);

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

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }


        // Select audio file.
        File audioFile = selectAudioFile();
        if( audioFile == null )
            return;

        // Default casaa filename to match audio file, with .casaa suffix.
        String newFileName = changeSuffix( audioFile.getName(), "casaa" );
        File miscFile = selectMiscFile(newFileName);
        //
        cleanupMode();
        //
        filenameMisc = miscFile.getName();
        //
        utteranceListChanged();
        //setMode( Mode.CODE );
        //
        snCoding.setContent(new MiscTemplateView());
    }

    /******************************************************
     * Resume Misc Coding
     * @param actionEvent
     */
    public void mniResumeCoding(ActionEvent actionEvent) {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        File miscFile = selectMiscFile("");
        if( miscFile == null )
            return;

        cleanupMode();
        filenameMisc = miscFile.getAbsolutePath();
        filenameAudio = getUtteranceList().loadFromFile(miscFile);
        utteranceListChanged();
        Utterance utterance = getCurrentUtterance();


        // We expect utterances in file to be coded.  For backwards compatibility,
        // tolerate uncoded utterances in file.
        if( utterance.isCoded() ) {
            // Start new utterance.
            int 		position		= utterance.getEndBytes();
            String 		positionString 	= TimeCode.toString( position / getBytesPerSecond() );
            int         order 		  	= getUtteranceList().size();
            Utterance   data		 	= new MiscDataItem( order, positionString, position );

            getUtteranceList().add( data );
            // TODO: how to handle position update after medidplayer ready status
            // playerSeek( position );
        }
        else {
            // Tolerate uncoded final utterance.  Strip end data, so it is consistent
            // with how we treat current utterance.  NOTE: This does not check for
            // uncoded utterances anywhere else in file.
            utterance.stripEndData();
            // TODO: see above
            // playerSeek( utterance.getStartBytes() );
        }


        // TODO: this will need to be handled
        // updateUtteranceDisplays();

        numSaves = 0; // Reset save counter, so we backup on next save (i.e. as
        // soon as player saves changes to newly loaded data).


        snCoding.setContent(new MiscTemplateView());
    }


    /**********************************************************************
     * sldSeek mouse event:
     * change time seek when user clicks on slid bar instead of dragging the controller
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



    private File selectAudioFile() {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Open audio file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"));
        File selectedFile = fc.showOpenDialog(stageTheLabelBelongs);

        return selectedFile;
    }


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

    // ******************************
    // Begin Integrate
    // ******************************


    /*******************************************************
     * Reusable method to display runtime errors
     * @param title
     * @param message
     *******************************************************/
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
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

    private void utteranceListChanged() {
        //playerView.getTimeline().repaint();
        System.out.println("Unhandled method: utteranceListChanged()");
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
/*        int position = player.getEncodedStreamPosition();

        // If playback has reached end of file, position will be -1.
        // In that case, use length - 1.
        if( position < 0 ) {
            int length = player.getEncodedLength() - 1;

            position = (length > 0) ? (length - 1) : 0;
        }
        return position;*/
        return 0;
    }



    // Handle errors re: user codes XML file. We must be able to find and parse
    // this file
    // successfully, so all of these errors are fatal.
    public void handleUserCodesParseException(File file, SAXParseException e) {
        // Alert and quit.
        this.showWarning("Failed to load user codes","Parse error in " + file.getAbsolutePath() + " (line " + e.getLineNumber() + "):\n" + e.getMessage());
        Platform.exit();
    }

    public void handleUserCodesGenericException(File file, Exception e) {
        this.showWarning("Failed to load user codes","Unknown error parsing file: " + file.getAbsolutePath() + "\n" + e.toString());
        Platform.exit();
    }

    public void handleUserCodesError(File file, String message) {
        this.showWarning("Failed to load user codes", "Error loading file: " + file.getAbsolutePath() + "\n" + message);
        Platform.exit();
    }

    public void handleUserCodesMissing(File file) {
        // Alert and quit.
        this.showWarning("Failed to load user codes","Failed to find required file." + file.getAbsolutePath());
        Platform.exit();
    }

    // Parse user codes and globals from XML.
    private void parseUserConfig() {
        // NOTE: We display parse errors to user, so user can correct XML file, then quit.
        File file = new File( "userConfiguration.xml" );

        if( file.exists() ) {
            try {
                DocumentBuilderFactory fact    = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = fact.newDocumentBuilder();
                Document doc     = builder.parse( file.getCanonicalFile() );
                Node root    = doc.getDocumentElement();

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
            handleUserCodesMissing( file );
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

    private synchronized void resetUncodeCount() {
        numUninterruptedUncodes = 0;
    }
    private void cleanupMode() {
        utteranceList = null;
        resetUncodeCount();
    }

}
