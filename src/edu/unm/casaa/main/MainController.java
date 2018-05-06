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

import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.Utterance;
import javafx.animation.Animation;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.prefs.Preferences;

import static java.lang.String.format;


public class MainController {

    //TODO: move this if still needed

    // SESSION REPORT
    @FXML
    private ScrollPane pnReport;
    @FXML
    private Label rptScore_mico;
    @FXML
    private Label rptScore_miin;
    @FXML
    private Label rptScore_pmic;
    @FXML
    private Label rptScore_r2q;
    @FXML
    private Label rptScore_poq;
    @FXML
    private Label rptScore_pcr;
    @FXML
    private Label rptScore_ther2cli;
    @FXML
    private Label rptScore_pct;
    @FXML
    private Label rptScore_adp;
    @FXML
    private Label rptScore_adw;
    @FXML
    private Label rptScore_af;
    @FXML
    private Label rptScore_co;
    @FXML
    private Label rptScore_di;
    @FXML
    private Label rptScore_ec;
    @FXML
    private Label rptScore_gi;
    @FXML
    private Label rptScore_open;
    @FXML
    private Label rptScore_closed;
    @FXML
    private Label rptScore_rcp;
    @FXML
    private Label rptScore_rcw;
    @FXML
    private Label rptScore_simple;
    @FXML
    private Label rptScore_complex;
    @FXML
    private Label rptScore_refct;
    @FXML
    private Label rptScore_refst;
    @FXML
    private Label rptScore_st;
    @FXML
    private Label rptScore_rf;
    @FXML
    private Label rptScore_su;
    @FXML
    private Label rptScore_wa;
    @FXML
    private Label rptScore_change;
    @FXML
    private Label rptScore_sustain;


    // PLAYBACK
    @FXML
    private Label lblRate;                              // display current playback rate
    @FXML
    private Slider sldRate;                             // playback rate control
    @FXML
    private VBox vbApp;                                 // control holding non-playback controls (misc/globals)
    @FXML
    private Label lblAudioFilename;
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
    private Menu mnuCoding;
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
    private Label lblCurConfigFile;
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


    // SETTINGS KB
    @FXML
    private TextField codingActionKeyReplay;


    // UTTERANCE EDITOR
    @FXML
    private TextArea dlgAnnotation;
    @FXML
    private ListView dlgListView;




    private Preferences appPrefs;                       // User prefs persistence
    private Duration totalDuration;                     // duration of active media
    private String filenameAudio         = null;        // name of active media file. Used when switching from PLAYBACK to MISC to GLOBALS
    private File currentAudioFile        = null;        // active media file
    private SessionData sessionData      = null;        // session persistence

    private enum  GuiState {                            // available gui states
        PLAYBACK, MISC_CODING, GLOBAL_CODING, REPORT
    }
    private GuiState guiState;                          //

    private TimeLine timeLine;

    private Boolean isKeyFilterSet       = Boolean.FALSE; //





    /*****************************************************************
     * controller constructor
     *
     * called once at start. Include things here that need to run
     * once at start but doesn't need access to FXML.
     * After this, initialize() is called
     *****************************************************************/
    public MainController() {

        // initialize app persistence
        appPrefs = Preferences.userNodeForPackage(Main.class);

        // default startup state
        setGuiState(GuiState.PLAYBACK);

        // check for required config to offer generation
        verifyUserConfig();

        // load user config file to load user specific edited codes
        parseUserConfig();

    }



    /******************************************************************
     * controller initialization tasks
     *
     * Called to initialize a controller after its root element has been completely processed.
     * The initialize method is called after all @FXML annotated members have been injected.
     *
     * This is called on any FXMLLoader, NOT just when
     * app or MainController is first loaded.
     *
     * e.g.
     * loader = new FXMLLoader(getClass().getResource("MISC_CODING.fxml"), resourceStrings);
     * loader.setController(this);
     ******************************************************************/
    @FXML
    private void initialize() {
        // Use OS X standard menus no Java window menus
        if( System.getProperty("os.name","UNKNOWN").equals("Mac OS X")) {
            menuBar.setUseSystemMenuBar(true);
        }
    }


    /*
     Button event handlers
     */

    /*
     * btnActPlayPause
     * @param actionEvent
     * button event: play media
     */
    public void btnActPlayPause(@SuppressWarnings("UnusedParameters") ActionEvent actionEvent) {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING) ) {
            mediaPlayer.pause();
        } else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.play();
        }
    }


    /*
     *  button event: 5 second rewind
     */
    @SuppressWarnings("UnusedParameters")
    public void btnActRewind(ActionEvent actionEvent) { rewind(); }


    /**********************************************************************
     *  button event:
     *  Seek to beginning of current utterance.  Seek a little further back
     *  to ensure audio synchronization issues don't cause player to actually
     *  seek later than beginning of utterance.
     *  @param actionEvent not used
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActReplay(ActionEvent actionEvent) {
        gotoLastMarker();
    }


    /**
     * move player to time of last utterance
     */
    private void gotoLastMarker() {
        Utterance   utterance   = getUtteranceList().last();
        Duration    pos         = Duration.ZERO;

        if( utterance != null ) {
            // Position one second before start of utterance.
            pos = utterance.getStartTime().subtract(Duration.ONE);
        }

        setMediaPlayerPosition( pos );
    }



    /**********************************************************************
     *  button event:
     *  Remove last utterance
     **********************************************************************/
    @SuppressWarnings("UnusedParameters")
    public void btnActUncode(ActionEvent actionEvent) {
        uncode();
    }


    /**********************************************************************
     *  button event:
     *  Uncode last utterance.
     *  If more utterances exist then move to 1 second prior to the
     *  previous code. Otherwise, move to 1 second prior to that last code
     *
     **********************************************************************/
    public void btnActUncodeRewind() {
        uncodeRewind();
    }



    /**********************************************************************
     *  button event:
     *  Apply utterance code
     **********************************************************************/
    private void btnActCode(ActionEvent actionEvent) {
        Button src = (Button) actionEvent.getSource();
        MiscCode mc = MiscCode.codeWithName(src.getText());
        assert mc != null;
        insertUtterance(mc);
    }




    /*
     * Menu event handlers
     */

    /**********************************************************************
     * menu selection event: About
     **********************************************************************/
    public void mniActAbout() {

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

        Stage about = new Stage();
        Parent root = null;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("About.fxml"), resourceStrings);
        try {
            root = fxmlLoader.load();

        } catch (Exception e){
            showError("About: fxml Error in About ", format("%s\n", e.toString()));
        }
        assert root != null;
        about.setScene(new Scene(root));
        about.setTitle(resourceStrings.getString("txt.about.title"));
        about.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_16x16.png")));
        about.initModality(Modality.APPLICATION_MODAL);
        about.initStyle(StageStyle.UTILITY);
        about.showAndWait();
    }



    /**********************************************************************
     * menu selection event: Settings::Keyboard Shortcuts
     *
     * This dialog does not load from fxml which might have been better.
     * This allowed access to form fields and contained all code for the
     * dialog here rather then mixing it into main controller.
     **********************************************************************/
    public void mniActSettingsKB() {

        Locale locale = new Locale("en", "US");
        ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

        /*
         * A validation and formatting filter for textfields
         * This event filter insures that only alphanumeric keys can be used
         * for shortcuts.
         * Filtering by KEY_TYPED prevents invalid codes from appearing at
         * all in the textfield
         *
         * whole form keyevents are not thrown for dialogs??? so this
         * shared filter gets attached below
         */
        EventHandler<KeyEvent> keyEventTypedFilter = (keyEvent -> {
            if( keyEvent.getEventType().equals(KeyEvent.KEY_TYPED) ) {
                if (keyEvent.getCharacter().matches("^[\\p{Alnum}]*$")) {
                    TextField tf = (TextField) keyEvent.getTarget();
                    tf.setText(keyEvent.getCharacter().toUpperCase());
                }
                keyEvent.consume();
            }
        });


        Dialog dlgSettingsKb = new Dialog();
        dlgSettingsKb.initOwner(vbApp.getScene().getWindow());
        dlgSettingsKb.initStyle(StageStyle.UTILITY);
        dlgSettingsKb.setTitle(resourceStrings.getString("menu.title.settings.kb"));

        Label lblHeaderText = new Label(resourceStrings.getString("txt.settings.kb.instructions"));
        lblHeaderText.setPadding(new Insets(20, 20, 10, 20));
        lblHeaderText.setWrapText(true);
        dlgSettingsKb.getDialogPane().setHeader(lblHeaderText);
        Stage dlgStage = (Stage) dlgSettingsKb.getDialogPane().getScene().getWindow();
        dlgStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_16x16.png")));
        dlgSettingsKb.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        GridPane gridKb = new GridPane();
        gridKb.setPadding(new Insets(20, 20, 20, 20));
        gridKb.setHgap(30);
        gridKb.setVgap(10);
        gridKb.setGridLinesVisible(false);
        gridKb.setAlignment(Pos.CENTER);
        gridKb.add(new Label(resourceStrings.getString("txt.settings.kb.codingbtn")), 0, 0);
        gridKb.add(new Label(resourceStrings.getString("txt.settings.kb.shortcut")), 1, 0);
        gridKb.add(new Label(resourceStrings.getString("txt.settings.kb.uncode")), 0, 1);
        gridKb.add(new Label(resourceStrings.getString("txt.settings.kb.replay")), 0, 2);
        gridKb.add(new Label(resourceStrings.getString("txt.settings.kb.uncode_rewind")), 0, 3);

        Label lblShiftU = new Label("Shift +");
        lblShiftU.setPadding(new Insets(0,2,0,0));
        Label lblMatchU = new Label("[0-9,a-z]");
        lblMatchU.setPadding(new Insets(0,0,0,4));
        TextField tfU = new TextField();
        tfU.setId("codingActionKeyUncode");
        tfU.setPromptText(appPrefs.get(tfU.getId(),"U"));
        tfU.setPrefColumnCount(1);
        tfU.addEventFilter(KeyEvent.KEY_TYPED, keyEventTypedFilter);
        HBox hboxU = new HBox(lblShiftU, tfU, lblMatchU);
        hboxU.setAlignment(Pos.CENTER);
        gridKb.add(hboxU, 1, 1);

        Label lblShiftR = new Label("Shift +");
        lblShiftR.setPadding(new Insets(0,2,0,0));
        Label lblMatchR = new Label("[0-9,a-z]");
        lblMatchR.setPadding(new Insets(0,0,0,4));
        TextField tfR = new TextField();
        tfR.setId("codingActionKeyReplay");
        tfR.setPromptText(appPrefs.get(tfR.getId(),"Y"));
        tfR.setPrefColumnCount(1);
        tfR.addEventFilter(KeyEvent.KEY_TYPED, keyEventTypedFilter);
        HBox hboxR = new HBox(lblShiftR, tfR, lblMatchR);
        hboxR.setAlignment(Pos.CENTER);
        gridKb.add(hboxR, 1, 2);

        Label lblShiftUR = new Label("Shift +");
        lblShiftUR.setPadding(new Insets(0,2,0,0));
        Label lblMatchUR = new Label("[0-9,a-z]");
        lblMatchUR.setPadding(new Insets(0,0,0,6));
        TextField tfUR = new TextField();
        tfUR.setId("codingActionKeyUncodeRewind");
        tfUR.setPromptText(appPrefs.get(tfUR.getId(),"I"));
        tfUR.setPrefColumnCount(1);
        tfUR.addEventFilter(KeyEvent.KEY_TYPED, keyEventTypedFilter);
        HBox hboxUR = new HBox(lblShiftUR, tfUR, lblMatchUR);
        hboxUR.setAlignment(Pos.CENTER);
        gridKb.add(hboxUR, 1, 3);

        dlgSettingsKb.getDialogPane().setContent(gridKb);



        Optional<ButtonType> result = dlgSettingsKb.showAndWait();
        if (result.isPresent() && (result.get() == ButtonType.APPLY)){
            if( !tfU.getText().isEmpty() ){
                appPrefs.put(tfU.getId(), tfU.getText());
            }
            if( !tfR.getText().isEmpty() ){
                appPrefs.put(tfR.getId(), tfR.getText());
            }
            if( !tfUR.getText().isEmpty() ){
                appPrefs.put(tfUR.getId(), tfUR.getText());
            }
        }

    }





    /**********************************************************************
     * menu selection event: Help
     **********************************************************************/
    public void mniActOnlineHelp() {

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
    public void mniActExit() {

        // user prefs like current volume setting are saved in
        // Application.stop() when Platform.exit() is called.
        // Anything else you want to do before leaving???

        /*
            key command to quit program needs to take focus from notes field
            or any other text field.
            just make sure that focus is drawn away on exit
         */
        menuBar.requestFocus();

        /* Application exit */
        Platform.exit();
    }



    /**********************************************************************
     * menu selection event: Open Audio File to listen independently of
     * coding
     **********************************************************************/
    public void mniActOpenFile() {

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
     */
    public void mniStartCoding() {

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

        // if file is already there we drop it now
        if( miscFile.exists() ) {
            miscFile.delete();
        }

        if (audioFile.canRead()) {

            try {
                // if audio file exists proceed to initialize session data
                sessionData = new SessionData(miscFile, audioFile);
            } catch(IOException e) {
                showError("Error reading casaa file", e.getMessage());
            }

            // initialize player interface
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
     ******************************************************/
    public void mniResumeCoding() {

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

        // the rest is broken out for reuse
        resumeCoding(miscFile);
    }



    /**
     * Select a different user config file possibly with different code list
     *
     * Update code list. If currently coding, reload controls
     *
     */
    public void mniLoadConfig() {

        // if something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        /* select config file */
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CACTI Config files", "*.xml"));

        // get user selection
        File selectedFile;

        // seed path empty; select existing
        File initDir = new File(System.getProperty("user.home"));
        fc.setTitle("Open Config File");
        fc.setInitialDirectory(initDir);
        selectedFile = fc.showOpenDialog(null);

        /* if file selected */
        if( selectedFile != null ) {

            // update path
            UserConfig.setPath(selectedFile.getAbsolutePath());
            // parse the file
            parseUserConfig();

            // if we were MISC coding reload
            if (getGuiState().equals(GuiState.MISC_CODING)) {

                initializeUserControls();

                // only resume coding if casaa file exists
                File tmp = new File(sessionData.getSessionFilePath());
                if( tmp.exists() ) {
                    resumeCoding(tmp);
                }

            } else if (getGuiState().equals(GuiState.GLOBAL_CODING)) {
                initializeUserControls();
            }
        }

    }

    /**
     * Switch to coding screen or tab
     */
    public void mniCodingView() {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        if(sessionData != null) {
            setGuiState(GuiState.MISC_CODING);

            // take care of media player
            initializeMediaPlayer(currentAudioFile, playerReady);
        }
    }

    /**
     * Switch to rating screen or tab
     */
    public void mniGlobalScoringView() {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        if(sessionData != null) {
            setGuiState(GuiState.GLOBAL_CODING);

            // take care of media player
            initializeMediaPlayer(currentAudioFile, playerReady);
        }
    }


    /**
     * Display summary report of session
     */
    public void mniReportView() {

        // this something be playing, stop it
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

        if(sessionData != null) {

            Locale locale = new Locale("en", "US");
            ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

            Stage report = new Stage();
            Parent root = null;
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Report.fxml"), resourceStrings);
            // set loader so i have access to instance variables
            fxmlLoader.setController(this);
            try {
                root = fxmlLoader.load();
            } catch (Exception e){
                showError("About: fxml Error in About ", format("%s\n", e.toString()));
            }
            assert root != null;
            report.setScene(new Scene(root));
            //report.setTitle(resourceStrings.getString("txt.about.title"));
            report.setTitle("Therapist Feedback");
            report.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_16x16.png")));
            report.initModality(Modality.APPLICATION_MODAL);
            report.initStyle(StageStyle.DECORATED);


            /* get the session summary scores */
            HashMap<String, Double> mapCodeSummary = null;
            try {
                // get counts
                mapCodeSummary = sessionData.getCodeSummaryMap();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            /* attach/format summary scores to report */
            // integers: "%.0f"
            // doubles: "%.2f"
            rptScore_mico.setText(String.format("%.0f",mapCodeSummary.get("SUM_MICO")));
            rptScore_miin.setText(String.format("%.0f",mapCodeSummary.get("SUM_MIIN")));
            rptScore_pmic.setText(String.format("%.1f%%",mapCodeSummary.get("PCT_MIC")));
            rptScore_r2q.setText(String.format("%.2f",mapCodeSummary.get("RATIO_R2Q")));
            rptScore_poq.setText(String.format("%.1f%%",mapCodeSummary.get("PCT_POQ")));
            rptScore_pcr.setText(String.format("%.1f%%",mapCodeSummary.get("PCT_PCR")));
            rptScore_ther2cli.setText(String.format("%.2f",mapCodeSummary.get("RATIO_THER2CLI")));
            rptScore_pct.setText(String.format("%.1f%%",mapCodeSummary.get("PCT")));
            rptScore_adp.setText(String.format("%.0f",mapCodeSummary.get("SUM_ADP")));
            rptScore_adw.setText(String.format("%.0f",mapCodeSummary.get("SUM_ADW")));
            rptScore_af.setText(String.format("%.0f",mapCodeSummary.get("SUM_AF")));
            rptScore_co.setText(String.format("%.0f",mapCodeSummary.get("SUM_CO")));
            rptScore_di.setText(String.format("%.0f",mapCodeSummary.get("SUM_DI")));
            rptScore_ec.setText(String.format("%.0f",mapCodeSummary.get("SUM_EC")));
            rptScore_gi.setText(String.format("%.0f",mapCodeSummary.get("SUM_GI")));
            // TODO: don't know how these are defined. not in SPS syntax
            //rptScore_open.setText(String.format("%.0f",mapCodeSummary.get("")));
            //rptScore_closed.setText(String.format("%.0f",mapCodeSummary.get("")));
            rptScore_rcp.setText(String.format("%.0f",mapCodeSummary.get("SUM_RCP")));
            rptScore_rcw.setText(String.format("%.0f",mapCodeSummary.get("SUM_RCW")));
            rptScore_simple.setText(String.format("%.0f",mapCodeSummary.get("SUM_SIMPLE")));
            rptScore_complex.setText(String.format("%.0f",mapCodeSummary.get("SUM_CR")));
            rptScore_refct.setText(String.format("%.0f",mapCodeSummary.get("SUM_REF_CT")));
            rptScore_refst.setText(String.format("%.0f",mapCodeSummary.get("SUM_REF_ST")));
            rptScore_st.setText(String.format("%.0f",mapCodeSummary.get("SUM_ST")));
            rptScore_rf.setText(String.format("%.0f",mapCodeSummary.get("SUM_RF")));
            rptScore_su.setText(String.format("%.0f",mapCodeSummary.get("SUM_SU")));
            rptScore_wa.setText(String.format("%.0f",mapCodeSummary.get("SUM_WA")));
            rptScore_change.setText(String.format("%.0f",mapCodeSummary.get("SUM_CHANGE")));
            rptScore_sustain.setText(String.format("%.0f",mapCodeSummary.get("SUM_SUSTAIN")));

            report.showAndWait();
        }
    }



    /**********************************************************************
     * sldSeek mouse event:
     * change seek time when user clicks on slid bar instead of dragging the controller
     * to change the position
     **********************************************************************/
    public void sldSeekMousePressed() {
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
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files (*.wav, *.m4a)", "*.wav","*.m4a")
        );

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
     * Backwards Compatibility: Select a Globals code file for coding
     * @return Globals File object
     ************************************************************************/
    private File selectGlobalsFile(File sessionFile) {

        Stage stageTheLabelBelongs = (Stage) menuBar.getScene().getWindow();

        // set code file chooser
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLOBALS files", "*.globals"));

        // set initial directory to preferences or users home directory
        //File initDir = new File(appPrefs.get("lastGlobalsPath", System.getProperty("user.home")));
        File initDir = new File(sessionFile.getParent());
        //sessionFile.getAbsolutePath()
        // if preference path no longer exists reset to user home directory
        if( !initDir.exists() ) {
            initDir = new File(System.getProperty("user.home"));
        }
        // set chooser init directory
        fc.setInitialDirectory(initDir);

        // get user selection
        File selectedFile;
        fc.setTitle("Select GLOBALS File");
        selectedFile = fc.showOpenDialog(stageTheLabelBelongs);

        // persist path for next time
        if( selectedFile != null) {
            // update persistence
            //appPrefs.put("lastGlobalsPath", selectedFile.getParent());
            // make sure file has proper extension
            String newFile = selectedFile.getAbsolutePath();
            if( !newFile.toLowerCase().endsWith(".globals")) {
                selectedFile = new File(newFile + ".globals");
            }
        }

        return selectedFile;
    }



    /************************************************************************
     * Specify a Config file
     ************************************************************************/
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


    public void printReport(@SuppressWarnings("UnusedParameters") ActionEvent actionEvent) {
//        Printer printer = Printer.getDefaultPrinter();
//        PageLayout pageLayout = printer.createPageLayout(Paper.NA_LETTER, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
//        double scaleX = pageLayout.getPrintableWidth() / pnReport.getBoundsInParent().getWidth();
//        double scaleY = pageLayout.getPrintableHeight() / pnReport.getBoundsInParent().getHeight();
//        pnReport.getTransforms().add(new Scale(scaleX, scaleY));

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean success = job.printPage(pnReport);
            if (success) job.endJob();
        }
    }


    /**
     * Display editor for annotating an utterance and handle
     * update through data model
     * @param pcEvt Property change event we are listening for
     */
    private void openUtteranceEditor(PropertyChangeEvent pcEvt) {
        if( pcEvt.getNewValue() != null ) {

            /* grab mouse location to position editor window. not ideal but worth a test */
            Point mousePtrLoc = MouseInfo.getPointerInfo().getLocation();

            /* get data */
            String utterance_id = pcEvt.getNewValue().toString();
            String existingAnnotation = "";
            /* globals list */
            ArrayList<GlobalCode> ratingCode = null;
            try {
                existingAnnotation = sessionData.getUtteranceAnnotationText(utterance_id);
                ratingCode = sessionData.getRatingsList(utterance_id);
            } catch ( SQLException e) {
                showError("Error Annotation", e.getMessage());
            }


            /* open editor */
            Dialog<ButtonType> dlgUtteranceEditor = new Dialog<>();
            FXMLLoader dialogLoader = new FXMLLoader(getClass().getResource("UtteranceEditor.fxml"));
            try {
                dlgUtteranceEditor.setDialogPane(dialogLoader.load());
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* only basic layout is in fxml. Further dialog settings here */
            dlgUtteranceEditor.setTitle("Annotate Utterance");
            Stage dlgStage = (Stage) dlgUtteranceEditor.getDialogPane().getScene().getWindow();
            dlgStage.getIcons().add(new Image(Main.class.getResourceAsStream("/media/windows.iconset/icon_16x16.png")));
            dlgUtteranceEditor.initStyle(StageStyle.TRANSPARENT);
            Scene scene = dlgStage.getScene();
            scene.setFill(Color.TRANSPARENT);

            /* position window */
            dlgUtteranceEditor.setX(mousePtrLoc.x-20.0);
            dlgUtteranceEditor.setY(mousePtrLoc.y-60.0);


            /* populate listview of global items.
            * Since we want to reselect previous items i loop through items
            * instead of just populating with an observablelist */
            ListView<GlobalCode> dlgListView = new ListView<>();
            dlgListView.setPrefHeight(120);
            dlgListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            for(GlobalCode gc : ratingCode){
                dlgListView.getItems().add(gc);
                if((gc.label != null) && (gc.label.equals(utterance_id))) {
                    dlgListView.getSelectionModel().select(gc);
                }
            }

            /* local reference for annotation text */
            TextArea extTa = null;
            // get annotation reference and populate with existing text
            for(Node nodeOut:dlgUtteranceEditor.getDialogPane().getChildren()){
                if(nodeOut instanceof VBox) {
                    VBox vb = (VBox) nodeOut;
                    for (Node nodeIn : vb.getChildren()) {
                        if (nodeIn instanceof TextArea) {
                            extTa = (TextArea) nodeIn;
                            extTa.setText(existingAnnotation);
                        }
                    }
                    /* secondarily, add list view to VBox container */
                    vb.getChildren().add(dlgListView);
                }
            }

            // Use traditional way to get results as lambda expects 'final' variables
            Optional<ButtonType> result = dlgUtteranceEditor.showAndWait();
            if (result.isPresent() & (result.get() == ButtonType.APPLY)){
                ObservableList<Integer> selectedIndices = dlgListView.getSelectionModel().getSelectedIndices();

                ArrayList<GlobalCode> globalCodeList = new ArrayList<>();
                for(int i : selectedIndices){
                    globalCodeList.add(ratingCode.get(i));
                }

                /* save utterance annotation */
                try {
                    sessionData.annotateUtterance(utterance_id, extTa.getText(), globalCodeList);
                } catch (SQLException e) {
                    showError("Error Annotating Utterance", e.getMessage());
                }
            }

        }
    }


    /**
     * Notify user of file format issue and ask for permission to convert.
     * Guide user through selecting globals file for merging into new format.
     * Call static SessionData methods to archive casaa and globals file to *.bak.
     * If this all works out, return true
     */
    private SessionData getSessionFilePreviousFileFormat(File sessionFile) throws IOException {

        /*
            Notify user
         */
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Convert");
        alert.setHeaderText("Convert Casaa File Format?");

        Text txt = new Text("\nThe casaa file you've selected is an old format which needs to be converted for this version of the program. Once you convert the file it will only work with this and later versions of the program. However, backup versions of the old files will remain.\n\nConvert now?");

        txt.setWrappingWidth(this.vbApp.getWidth()/3.0);
        alert.getDialogPane().setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
        alert.getDialogPane().setContent(txt);

        Optional<ButtonType> result = alert.showAndWait();


        if (result.isPresent() && result.get() == ButtonType.OK) {

            /* get old globals file path, if any */
            Alert alertGlobals = new Alert(Alert.AlertType.CONFIRMATION);
            alertGlobals.setTitle("Convert");
            alertGlobals.setHeaderText("Locate Corresponding Globals File?");

            txt = new Text("\nThe casaa file you've selected may have a corresponding 'globals' file that stored the Global Ratings for this session. To merge these ratings into the new file format you will need to locate this globals file.\n\nLocate Globals file now?");

            ButtonType buttonTypeYes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType buttonTypeNo = new ButtonType("No", ButtonBar.ButtonData.NO);
            alertGlobals.getButtonTypes().setAll(buttonTypeNo, buttonTypeYes);

            txt.setWrappingWidth(this.vbApp.getWidth()/3.0);
            alertGlobals.getDialogPane().setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
            alertGlobals.getDialogPane().setContent(txt);

            Optional<ButtonType> resultGlobals = alertGlobals.showAndWait();

            /* locate globals file? */
            if (resultGlobals.isPresent() && resultGlobals.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {

                File oldGlobalsFile = selectGlobalsFile(sessionFile);
                if(oldGlobalsFile != null){
                    // call method on sessiondata with casaa file path and globals file path that will convert the files
                    return SessionData.Compatibility.sessionDataFromPreviousFileFormat(sessionFile, oldGlobalsFile);
                } else {
                    /* don't locate globals text format
                     * create seesion from old text format file
                     * attempt will throw ioexceptions returned by this method
                     */
                    return SessionData.Compatibility.sessionDataFromPreviousFileFormat(sessionFile);
                }

            } else {
                /* don't locate globals text format
                 * create session from old text format file
                 * attempt will throw ioexceptions returned by this method
                 */
                return SessionData.Compatibility.sessionDataFromPreviousFileFormat(sessionFile);
            }
        } else {
            /* user cancelled casaa file conversion */
            return null;
        }

    }



    /**
     * Break out code to resume MISC coding state
     */
    private void resumeCoding( File sessionFile ) {

        // load session data
        try {
            sessionData = new SessionData(sessionFile);
        } catch(FileFormatException e) {
            try {
                sessionData = getSessionFilePreviousFileFormat(sessionFile);
                if(sessionData == null)
                    return;
            } catch(IOException ioe) {
                showError("Casaa File Conversion", ioe.getMessage());
                return;
            }
        } catch(Exception e)  {
            showError("Error Loading Casaa File", e.getMessage());
            return;
        }

        // initialize audio file object
        File audioFile = new File(sessionData.getAudioFilePath());
        if (audioFile.canRead()) {
            // load the audio and start the player
            filenameAudio = audioFile.getAbsolutePath();
            // store reference so other states can reuse
            currentAudioFile = audioFile;
            // start media player
            initializeMediaPlayer(audioFile, playerReady);
        } else {

            /*
             show an error dialog to inform user and offer opportunity to locate the missing audio file
             */
            Locale locale = new Locale("en", "US");
            ResourceBundle resourceStrings = ResourceBundle.getBundle("strings", locale);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resourceStrings.getString("alert.missingAudioFile.title"));
            alert.setContentText(String.format("Could not load the session audio file:\n%s\nIf you know where the audio file is,\nclick the 'Locate File' button.", sessionData.getAudioFilePath()) );
            ButtonType buttonTypeLoc = new ButtonType(resourceStrings.getString("alert.missingAudioFile.btn1.text"));
            ButtonType buttonTypeCancel = new ButtonType(resourceStrings.getString("alert.missingAudioFile.btn2.text"), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeLoc, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && (result.get() == buttonTypeLoc)){
                // locate audio file
                audioFile = selectAudioFile();
                try {
                    // load the audio and start the player
                    filenameAudio = audioFile.getAbsolutePath();
                    // update session
                    sessionData.setAudioFilePath(audioFile.getAbsolutePath());
                } catch (NullPointerException e) {
                    return;
                } catch(SQLException e) {
                    showError("Error Updating Casaa File", e.getMessage());
                    return;
                }
                // start media player
                initializeMediaPlayer(audioFile, playerReady);
            } else {
                return;
            }
        }

        setGuiState(GuiState.MISC_CODING);

        setPlayerButtonState();
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
        totalDuration = mediaPlayer.getTotalDuration();
        // duration label
        lblDuration.setText(Utils.formatDuration(totalDuration));

        // this also initializes status so that display updates happen correctly at outset
        mediaPlayer.play();
        mediaPlayer.pause();
        mediaPlayer.seek(Duration.ZERO);

        // arrow keys move by half second when player has focus
        Double blockIncrement = ( ((Duration.ONE.toSeconds()/totalDuration.toSeconds())/2)*1000 );
        sldSeek.setBlockIncrement(blockIncrement);

        // mediaPlayer is ready continue with user controls setup
        initializeUserControls();
    };



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
                    lblTimePos.setText(Utils.formatDuration(mediaPlayer.getCurrentTime()));
                    btnPlayImgVw.getStyleClass().add("img-btn-pause");
                });


                //mediaPlayer.setOnPaused(playerPaused);
                /* Status Handler:  OnPaused */
                mediaPlayer.setOnPaused(() -> {
                    lblTimePos.setText(Utils.formatDuration(mediaPlayer.getCurrentTime()));
                    // assumes OnPlay has overlayed style class so just remove that to expose pause class
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });


                /* Status Handler: OnStop */
                mediaPlayer.setOnStopped(() -> {
                    lblTimePos.setText(Utils.formatDuration(mediaPlayer.getCurrentTime()));
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
                        option 2: pause at end so user can doing coding in timeline as needed
                     */
                    if(timeLine != null) {
                        timeLine.getAnimation().pause();
                    }
                    mediaPlayer.pause();
                    // assumes OnPlay has overlayed style class so just remove that to expose pause class
                    btnPlayImgVw.getStyleClass().remove("img-btn-pause");
                });



                /* Listener: Update the media position if user is dragging the slider.
                 * Otherwise, do nothing. See sldSeekMousePressed() for when slider is clicked with mouse
                 * Seems odd to bind to valueProperty and check isValueChanging
                 * but when i use "valueChangingProperty" this performance is
                 * not as smooth
                 */
                sldSeek.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    /*
                        if dragging slider, update media position
                     */
                    if(sldSeek.isValueChanging()) {
                        // multiply duration by percentage calculated by slider position
                        //mediaPlayer.seek(totalDuration.multiply(newValue.doubleValue()));
                        setMediaPlayerPosition(totalDuration.multiply(newValue.doubleValue()));
                    }
                });


                /* if dragging slider, update media playback rate */
                sldRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if (sldRate.isValueChanging()) {
                        mediaPlayer.setRate(newValue.doubleValue());
                    }
                });

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

        /*
         * determine where to put timeline overlay
         */
        double center = vbApp.getScene().getWidth()/2;
        // time position line
        Line l = new Line(0,0,0,28.0);
        l.setStrokeWidth(0.5);
        l.setTranslateX(center + l.getStrokeWidth());

        /*
          initialize timeline and add to display
         */
        timeLine = new TimeLine(totalDuration, 30, center, sessionData.utteranceList);
        pnTimeLine.getChildren().clear();
        pnTimeLine.getChildren().addAll(l, timeLine);

        /*
          Here we link to mediaplayer status to sync timeline status
          This is not done in setOnPlaying() lambda because the mediaplayer
          can be initialized without there being a timeline defined.
        */
        mediaPlayer.statusProperty().addListener( (invalidated, oldvalue, newvalue) -> {
            switch (newvalue) {
                case READY:
                    timeLine.getAnimation().pause();
                    break;
                case PLAYING:
                    timeLine.getAnimation().play();
                    break;
                case PAUSED:
                    timeLine.getAnimation().pause();
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


        /* this shouldn't happen */
        timeLine.getAnimation().setOnFinished( (e) -> timeLine.getAnimation().pause());


        /*
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


        /*
           timeline dispatches changes to annotation property that notify controller to
           edit utterance annotation.
         */
        timeLine.addPropertyChangeListener(this::openUtteranceEditor);


        /*
         * Playback rate binding
         */
        timeLine.getAnimation().rateProperty().bind(sldRate.valueProperty());

        /*
         * timeline can be paused internally by clicking on a marker
         * so this listener pauses mediaplayer in response
         */
        timeLine.getAnimation().statusProperty().addListener((invalidated, oldValue, newValue) -> {
            if (newValue.equals(Animation.Status.PAUSED)) {
                mediaPlayer.pause();
            }
        });

        /*
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
                double windW = appPrefs.getDouble("main.wind.w", 800.0);
                ourTown.setWidth(windW);

                break;



            case MISC_CODING:

                resetUserControlsContainer();

                // load new controls
                loader = new FXMLLoader(getClass().getResource("MISC_CODING.fxml"), resourceStrings);
                loader.setController(this);
                try {
                    vbApp.getChildren().add(loader.load());
                } catch (IOException ex) {
                    showError("Error", ex.toString());
                }

                // TODO: verify best place for this
                mnuCoding.setDisable(false);

                // display controls needed for coding
                setPlayerButtonState();

                // load coding buttons from userConfiguration.xml appropriate for GuiState
                parseUserControls();


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


                /*
                 * get last utterance to set player position in time
                 */
                Utterance currentUtterance = getUtteranceList().last();
                // default seek init
                onReadySeekDuration = Duration.ZERO;

                // We expect utterances in file to be coded.  For backwards compatibility,
                // tolerate uncoded utterances in file.
                if( currentUtterance != null ) {
                    // update mediaplayer position appropriately for our now active utterance
                    onReadySeekDuration = currentUtterance.getStartTime();
                }


                /*
                 * adjust player position
                 */
                mediaPlayer.seek(onReadySeekDuration);
                lblTimePos.setText(Utils.formatDuration(onReadySeekDuration));
                sldSeek.setValue(onReadySeekDuration.toMillis()/totalDuration.toMillis());

                // update the utterance data(previous/current) displayed in the gui
                updateUtteranceDisplays();


                /*
                    initialize new timeline
                    assumes mediaplayer time position is already set
                 */
                initializeTimeLine();

                // display coding file path in gui
                lblCurMiscFile.setText(sessionData.getSessionFilePath());

                // dispaly config file path in gui
                lblCurConfigFile.setText(UserConfig.getPath());

                // force last marker
                gotoLastMarker();

                // resize app window
                windW = appPrefs.getDouble("main.wind.w", 800.0);
                ourTown.setWidth(windW);
                // enforce 600 min height for coding window
                double windH = appPrefs.getDouble("main.wind.h", 600.0);
                if( windH < 600.0) {
                    ourTown.setHeight(600.0);
                } else {
                    ourTown.setHeight(windH);
                }

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
                } catch (IOException ex) {
                    showError("Error", ex.toString());
                }


                // update control state
                // load coding buttons from userConfiguration.xml appropriate for GuiState
                parseUserControls();

                // resize app window
                windW = appPrefs.getDouble("main.wind.w", 800.0);
                ourTown.setWidth(windW);
                // enforce 600 min height for coding window
                windH = appPrefs.getDouble("main.wind.h", 600.0);
                if( windH < 600.0) {
                    ourTown.setHeight(600.0);
                } else {
                    ourTown.setHeight(windH);
                }

                break;


            case REPORT:

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
                } catch (IOException ex) {
                    showError("Error", ex.toString());
                }


                // update control state
                // load coding buttons from userConfiguration.xml appropriate for GuiState
                parseUserControls();

                // resize app window
                windW = appPrefs.getDouble("main.wind.w", 800.0);
                ourTown.setWidth(windW);
                // enforce 600 min height for coding window
                windH = appPrefs.getDouble("main.wind.h", 600.0);
                if( windH < 600.0) {
                    ourTown.setHeight(600.0);
                } else {
                    ourTown.setHeight(windH);
                }

                break;

        }


        /*
          capture keypresses at scene level in the event chain
          to control some overall key functions
         */
        if(!isKeyFilterSet) {
            ourTown.addEventFilter( KeyEvent.KEY_PRESSED, ke ->  mapKeyFunctions(ke) );
            isKeyFilterSet = Boolean.TRUE;
        }

    }





    /**********************************************************************
     * Set mediaplayer position using Duration
     **********************************************************************/
    private synchronized void setMediaPlayerPosition(Duration position){

        /* pause player and timeline as needed */

        switch (mediaPlayer.getStatus()) {

            case PLAYING:

                if(timeLine != null) {
                    timeLine.getAnimation().playFrom(position);
                }
                mediaPlayer.seek(position);
                break;

            case PAUSED:

                if(timeLine != null) {
                    timeLine.getAnimation().jumpTo(position);
                }
                mediaPlayer.seek(position);
                break;

            case READY:

                mediaPlayer.pause();
                mediaPlayer.seek(position);

                if(timeLine != null) {
                    timeLine.getAnimation().pause();
                    timeLine.getAnimation().jumpTo(position);
                }
                break;
        }
    }



    /*******************************************************
     * display runtime errors
     * @param title Window title
     * @param message Window message
     *******************************************************/
    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        //alert.initStyle(StageStyle.UTILITY); // what was this for? With this, the ESC and ENTER keys don't work. //TODO: check on other OS
        alert.showAndWait();
    }


    /*******************************************************
     * display fatal errors
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
    private synchronized SessionData.UtteranceList getUtteranceList() {
        if( sessionData.utteranceList == null )
            showError("Error", "UtteranceList is null");
        return sessionData.utteranceList;
    }


    /**
        See if uncoding is currently an option
     **/
    private boolean isUncodeAvailable() {
        Utterance l = getUtteranceList().last();

        return l != null;
    }




    /**
     * New utterance
     * @param miscCode code linked to utterance
     */
    private synchronized void insertUtterance(MiscCode miscCode) {

        // get current time
        Duration position = mediaPlayer.getCurrentTime();

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
        } catch (SQLException e) {
            showError("Error writing casaa file", e.getMessage());
        }
    }


    /**********************************************************************
     *  shared method:
     *  Remove last utterance
     **********************************************************************/
    private void uncode() {
        // remove the last code
        removeLastUtterance();
        // uncoding may exhaust available codes so update button state
        setPlayerButtonState();
    }


    /**
     *  shared method
     *  Uncode last utterance.
     *  If more utterances exist then move to 1 second prior to the
     *  previous code. Otherwise, move to 1 second prior to that last code
     */
    private void uncodeRewind() {

        Utterance lastUtterance = getUtteranceList().last();
        if (lastUtterance != null) {

            // new playback position defaults to 1 second before the last code before we remove it
            Duration newPlaybackTimePos = lastUtterance.getStartTime().subtract(Duration.ONE);

            // remove the last code
            removeLastUtterance();

            // get last utterance now
            Utterance prevUtterance = getUtteranceList().last();
            if (prevUtterance != null) {
                // Position one second before start of this previous utterance.
                newPlaybackTimePos = prevUtterance.getStartTime().subtract(Duration.ONE);
            }

            // move to new position
            setMediaPlayerPosition(newPlaybackTimePos);

            // uncoding may exhaust available codes so update button state
            setPlayerButtonState();
        }
    }



    /**
     *  shared method
     *  Rewind 5 secs
     */
    private void rewind() {
        if( mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5.0))){
            setMediaPlayerPosition(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5.0)));
        }
    }




    /**
     * @param utr Utterance instance to be removed
     */
    private synchronized void removeUtterance(Utterance utr){
        try {
            sessionData.utteranceList.remove(utr);
        } catch (SQLException e) {
            showError("Error writing casaa file", e.getMessage());
        }
        // refresh last utterance display
        updateUtteranceDisplays();
    }


    /**
     * More specific remove
     */
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
    private void handleUserCodesParseException(File file, SAXParseException e) {
        // Alert and quit.
        showFatalWarning("Failed to load user codes","Parse error in " + file.getAbsolutePath() + " (line " + e.getLineNumber() + "):\n" + e.getMessage());
    }

    private void handleUserCodesGenericException(File file, Exception e) {
        showFatalWarning("Failed to load user codes","Unknown error parsing file: " + file.getAbsolutePath() + "\n" + e.toString());
    }

    private void handleUserCodesError(File file, String message) {
        showFatalWarning("Failed to load user codes", "Error loading file: " + file.getAbsolutePath() + "\n" + message);
    }




    /**
     * Parse user codes and globals from user config file
     * into vectors of MiscCode and GlobalCode
     *
     * <userConfiguration>
     *  <codes>
     *      contain code label and value
     *      goes into MiscCode
     */
    private void parseUserConfig() {

        // reset codes vector
        MiscCode.clear();

        // cheap way to check if we need to reload userconfig which we will only allow once per lifecycle
        //if( MiscCode.numCodes() == 0 ){

            // NOTE: We display parse errors to user before quiting so user knows to correct XML file.
            File file = new File(UserConfig.getPath());

            if( file.canRead() ) {
                try {
                    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = fact.newDocumentBuilder();
                    Document doc = builder.parse( file.getCanonicalPath() );
                    org.w3c.dom.Node root = doc.getDocumentElement();

                    // first, create lookup list the returns code value for code name
                    Hashtable<String, Integer> miscCodeValues = new Hashtable<>();

                    // Expected format: <userConfiguration> <codes>...</codes> <globals>...</globals> </userConfiguration>
                    for( org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling() ) {
                        if( node.getNodeName().equalsIgnoreCase( "codes" ) )

                            for( org.w3c.dom.Node n = node.getFirstChild(); n != null; n = n.getNextSibling() ) {
                                if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                                    NamedNodeMap map         = n.getAttributes();
                                    org.w3c.dom.Node            nodeValue   = map.getNamedItem( "value" );
                                    int             value       = Integer.parseInt( nodeValue.getTextContent() );
                                    String          name        = map.getNamedItem( "name" ).getTextContent();

                                    // add to lookup list
                                    miscCodeValues.put(name, value);
                                }
                            }
                        else if( node.getNodeName().equalsIgnoreCase( "globals" ) )
                            /* handle global code */
                            parseUserGlobals( file, node );
                    }


                    // now, store codes use map
                    doc.getDocumentElement().normalize();
                    // just get nodes for controls
                    org.w3c.dom.NodeList controlNodeList = doc.getElementsByTagName("codeControls");
                    // iterate each child node
                    for (int cn = 0; cn < controlNodeList.getLength(); ++cn) {
                        org.w3c.dom.Node node = controlNodeList.item(cn);

                        // Get panel name.  Must be "left" or "right".
                        NamedNodeMap map = node.getAttributes();
                        String speaker = map.getNamedItem("label").getTextContent().split(" ")[0];
                        // i added ".split(" ")[0];" because one user had other text after that and i just wanted the first word.

                        for( org.w3c.dom.Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
                            if( row.getNodeName().equalsIgnoreCase( "row" ) ) {

                                for( org.w3c.dom.Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
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
        //}
    }



    /**
     * Parse globals from user config file
     * into vector GlobalCode
     * <userConfiguration>
     *  <globals>
     *      contain code label and value
     *      goes into GlobalCode
     */
    private void parseUserGlobals( File file, org.w3c.dom.Node globals ) {

        // reset code vector
        GlobalCode.clear();


        for( org.w3c.dom.Node n = globals.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "global" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                org.w3c.dom.Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                org.w3c.dom.Node            nodeDefaultRating   = map.getNamedItem( "defaultRating" );
                org.w3c.dom.Node            nodeMinRating       = map.getNamedItem( "minRating" );
                org.w3c.dom.Node            nodeMaxRating       = map.getNamedItem( "maxRating" );
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



    /************************************************************
     * Update utterance displays (e.g. current, last, etc) in active template view
     */
    private synchronized void updateUtteranceDisplays() {
        // display full string of previous utterance
        Utterance last = getUtteranceList().last();
        lblPrevUtr.setText(last == null ? "" : last.displayCoded());
    }


    /*************************************************************
     * Parse user controls from XML file to create GUI buttons in app
     * Legacy config file had separate section <codeControls> that
     * define which code label goes to which button in the screen
     *
     * This function checks each code against MiscCode or GlobalCode
     * vectors (that is where the code value is) and then adds the
     * button to the screen in the column and row.
     *
     * <userConfiguration>
     *  <codeControls panel="left" label="Therapist">
     *      button
     *
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
                            org.w3c.dom.Node node = controlNodeList.item(cn);

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
                            org.w3c.dom.Node node = controlNodeList.item(cn);

                            NamedNodeMap map = node.getAttributes();
                            String panelSide = map.getNamedItem("panel").getTextContent();
                            if (panelSide.equalsIgnoreCase("left")) {
                                gridColIndx = 0;
                                gridRowIndx = 0;
                            } else if (panelSide.equalsIgnoreCase("right")) {
                                gridColIndx = 1;
                                gridRowIndx = 0;
                            }


                            for( org.w3c.dom.Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {

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
                                        for(int i = code.minRating; i <= code.maxRating; i++) {
                                            RadioButton rb = createRadioButton(i, tg);
                                            // set radio button selected to user response value
                                            // which would be default value if not changed by user
                                            if( i == sessionData.ratingsList.getRating(code)) {
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
                                            try {
                                                // update code in data model
                                                sessionData.ratingsList.setRating(gc, rb.getText());
                                            } catch ( SQLException e ) {
                                                showError("Error Writing Casaa File", e.getMessage());
                                            }

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

                        /*
                         * Load user notes into notes field
                         */
                        tfGlobalsNotes.setText(sessionData.ratingsList.getNotes());

                        /*
                         * listen to notes field. If focus is lost and notes are not empty, save all out to the globals file.
                         */
                        tfGlobalsNotes.focusedProperty().addListener( (ObservableValue<? extends Boolean> observable, Boolean lostFocus, Boolean hasFocus) -> {
                            if(lostFocus) {
                                try {
                                    sessionData.ratingsList.setNotes(tfGlobalsNotes.getText());
                                } catch ( SQLException e ) {
                                    showError("Error Writing Casaa File", e.getMessage());
                                }

                            }
                        });

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
    private void parseControlColumn( org.w3c.dom.Node node, GridPane panel ) {

        int activeRow = 0;
        int activeCol = 0;

        for( org.w3c.dom.Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
            if( row.getNodeName().equalsIgnoreCase( "row" ) ) {
                activeRow ++;

                for( org.w3c.dom.Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
                    if( cell.getNodeName().equalsIgnoreCase("button")) {
                        activeCol ++;
                        NamedNodeMap map = cell.getAttributes();
                        String codeName = map.getNamedItem( "code" ).getTextContent();

                        Button button = new Button(codeName);
                        // show underscores in codes; do not trigger Mnemonic parsings
                        button.setMnemonicParsing(false);
                        button.setOnAction(this::btnActCode);
                        button.getStyleClass().add("btn-dark-blue");
                        // width and height of button expands with grid resizing
                        button.prefWidthProperty().bind(panel.widthProperty());
                        button.prefHeightProperty().bind(panel.heightProperty());

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

        // do stuff before changing state

        /*
        if( guiState != null ) {

        }
        */

        // change state
        guiState = gs;
    }


    /**
     *
     * @return current gui state
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
                btnUncode.setMinWidth(96.0);
                btnUncode.setVisible(true);
                btnUncodeReplay.setMinWidth(96.0);
                btnUncodeReplay.setVisible(true);
                btnRewind.setDisable(false);
                btnReplay.getParent().autosize();
                if(!isUncodeAvailable()){
                    btnReplay.setDisable(true);
                    btnUncodeReplay.setDisable(true);
                    btnUncode.setDisable(true);
                } else {
                    btnReplay.setDisable(false);
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
            if (result.isPresent() && (result.get() == buttonTypeOne)){
                // create new using default path
                selectConfigFile(UserConfig.getPath());
                try {
                    UserConfig.writeDefault();
                } catch (IOException e) {
                    showError("File Write Error", "Could not write user config file");
                }
            } else if (result.isPresent() && (result.get() == buttonTypeTwo)) {
                // select existing by sending no path
                selectConfigFile("");
            } else {
                System.exit(0);
            }

        }
    }

    /**
     * Key operation override
     *
     * handle arrow keys for moving at small increments in audio file
     *
     */
    private void mapKeyFunctions(KeyEvent ke) {

        /*
          Handle keyevents by code
         */
        switch (ke.getCode()) {

            case LEFT:
                // move media play left

                /*
                  move by half a second unless shift key is depressed then move by 5 seconds
                 */
                if (ke.isShiftDown()) {
                    setMediaPlayerPosition(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5.0)));
                } else {
                    setMediaPlayerPosition(mediaPlayer.getCurrentTime().subtract(Duration.seconds(0.5)));
                }
                ke.consume();
                break;

            case RIGHT:
                // move media play right
                /*
                  move by half a second unless shift key is depressed then move by 5 seconds
                 */
                if (ke.isShiftDown()) {
                    setMediaPlayerPosition(mediaPlayer.getCurrentTime().add(Duration.seconds(5.0)));
                } else {
                    setMediaPlayerPosition(mediaPlayer.getCurrentTime().add(Duration.seconds(0.5)));
                }
                ke.consume();
                break;

            case UP:
                // volume up
                ke.consume();
                sldVolume.adjustValue(sldVolume.getValue() + 0.1);
                break;

            case DOWN:
                // volume down
                ke.consume();
                sldVolume.adjustValue(sldVolume.getValue() - 0.1);
                break;

            case SPACE:
                // play/pause
                if (getGuiState().equals(GuiState.GLOBAL_CODING) && ke.getTarget() instanceof javafx.scene.control.TextArea) {
                    break;
                } else {
                    if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.play();
                    }
                    ke.consume();
                }
                break;
        }


        /*
          Handle user configurable keys by string
          assuming SHIFT modifier is fixed
         */
        if (ke.isShiftDown()) {

            /*
              Only effective while MISC coding
             */
            if( getGuiState().equals(GuiState.MISC_CODING)) {

                /*
                  replay last code
                 */
                if (ke.getCode().getName().equals(appPrefs.get("codingActionKeyReplay","Y"))){
                    gotoLastMarker();
                    ke.consume();
                    return;
                }

                /*
                  uncode
                 */
                if (ke.getCode().getName().equals(appPrefs.get("codingActionKeyUncode","U"))){
                    uncode();
                    ke.consume();
                    return;
                }

                /*
                  uncode/rewind
                 */
                if (ke.getCode().getName().equals(appPrefs.get("codingActionKeyUncodeRewind","I"))){
                    uncodeRewind();
                    ke.consume();
                    return;
                }
            }

        }


    }




    /**
     * Meant to provide support for command line and open files events
     * in the future
     * @param appParams
     */
    protected void initLaunchArgs(Application.Parameters appParams) {
        /*
          loop and use the first useful argument
         */
        for (String arg : appParams.getRaw()) {

            showError("argument",arg);

            if( arg.endsWith(".wav") ) {

                File audioFile = new File(arg);

                if (audioFile.canRead()) {
                    filenameAudio = audioFile.getAbsolutePath();
                    currentAudioFile = audioFile;

                    // launch argument available
                    setGuiState(GuiState.PLAYBACK);
                    initializeMediaPlayer(currentAudioFile, playerReady);
                }

            } else if(arg.endsWith(".casaa")) {
                File miscFile = new File(arg);

                if (miscFile.canRead()) {
                    resumeCoding(miscFile);
                }
            }
        }
    }


}
