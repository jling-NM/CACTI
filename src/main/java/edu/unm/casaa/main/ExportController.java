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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;


/**
 * Export Controller
 */
public class ExportController {

    private File dstFilePath = null;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;
    @FXML
    private Label dstFilePathName;
    @FXML // fx:id="dstListView"
    private ListView<String> dstListView; // Value injected by FXMLLoader
    public static final ObservableList selectedCasaaFile = FXCollections.observableArrayList();

    // export session will have a single, shared summary file
    String dateTimeStamp;
    File summaryFilePath = null;


    /**
     * Handle button request to set destination directory
     * @param event
     */
    @FXML
    void selectDstDirectory(ActionEvent event) {

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Output path for exported files:");

        // get the destination file path
        dstFilePath = dc.showDialog(null);
        if( dstFilePath != null ) {
            if( dstFilePath.isDirectory() ) {
                // update displayed path
                dstFilePathName.setText(dstFilePath.getAbsolutePath());
                // update export session summary path for new destination directory
                setSummaryFilePath();
            }
        }
    }


    /**
     * Handle Dnd for destination directory
     * @param e DragEvent
     */
    @FXML
    private void dstDragDropped(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            dstFilePath = db.getFiles().get(0);
            if( dstFilePath.isDirectory() ) {
                // update displayed path
                dstFilePathName.setText(dstFilePath.getAbsolutePath());
                // update export session summary path for new destination directory
                setSummaryFilePath();
            }
        }

        e.setDropCompleted(success);
        e.consume();
    }


    /**
     * Reset GUI after DnD
     * @param e
     */
    @FXML
    private void dstDragExited(final DragEvent e) {
        dstFilePathName.setStyle("-fx-border-color: #C6C6C6;");
    }


    /**
     * GUI feedback for DnD
     * @param e
     */
    @FXML
    private void dstDragOver(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        final boolean isValidPayload = db.getFiles().get(0).isDirectory();
        if ( isValidPayload ) {
            // custom feedback
            dstFilePathName.setStyle("-fx-border-color: red;"
                    + "-fx-border-width: 5;"
                    + "-fx-background-color: #C6C6C6;"
                    + "-fx-border-style: solid;");
            // javafx feedback
            e.acceptTransferModes(TransferMode.COPY);
        } else {
            e.consume();
        }
    }


    /**
     * Handle DnD files to export
     * @param e
     */
    @FXML
    private void srcDragDropped(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;

            db.getFiles().forEach(file -> {
                if( file.isDirectory() ) {
                    for(File f:file.listFiles()) {
                        if(f.getName().toLowerCase().endsWith(".casaa")) {
                            exportFile(f);
                        }
                    }
                } else {
                    exportFile(file);
                }
            });
        }

        e.setDropCompleted(success);
        e.consume();
    }


    /**
     * Reset control after DnD
     * @param event
     */
    @FXML
    private void srcDragExited(DragEvent event) {
        dstListView.setStyle("-fx-border-color: #C6C6C6;");
    }

    /**
     * Provide GUI feedback for DnD
     * @param e
     */
    @FXML
    private void srcDragOver(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        final boolean isValidPayload = (dstFilePath != null) && ( db.getFiles().get(0).isDirectory() || db.getFiles().get(0).getName().toLowerCase().endsWith(".casaa") );
        if ( isValidPayload ) {
            // custom feedback
            dstListView.setStyle("-fx-border-color: red;"
                    + "-fx-border-width: 5;"
                    + "-fx-background-color: #C6C6C6;"
                    + "-fx-border-style: solid;");
            // javafx feedback
            e.acceptTransferModes(TransferMode.COPY);
        } else {
            e.consume();
        }

    }

    /**
     * Exports new casaa file format to text files
     * @param exportFile
     */
    private void exportFile( File exportFile ) {
        if(! selectedCasaaFile.contains(exportFile.getAbsolutePath())){

            Platform.runLater(() -> {
                try {
                    // load the file
                    SessionData sesData = new SessionData(exportFile.getAbsoluteFile());
                    // start export session
                    SessionData.Export dataExport = sesData.new Export(dstFilePath, summaryFilePath);
                    // dump out text version
                    dataExport.writeCodeList();
                    // append summary values to summary.csv
                    dataExport.appendSummary();

                    // if no errors add to list of successful file
                    selectedCasaaFile.add(exportFile.getAbsolutePath());
                    dstListView.setItems(selectedCasaaFile);

                } catch (IOException e) {
                    showError("Export Error", e.getMessage());
                } catch (SQLException s) {
                    showError("Export Error", s.getMessage());
                }
            });
        }
    }


    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert dstListView != null : "fx:id=\"dstListView\" was not injected: check your FXML file.";

        Date date = new Date();
        final DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        dateTimeStamp = sdf.format(date);
    }


    /**
     * Set export session summary file path
     */
    public void setSummaryFilePath() {
        this.summaryFilePath = new File( String.format("%s%scasaa_summary_export_%s.csv", this.dstFilePath, File.separator, this.dateTimeStamp) );
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
        alert.showAndWait();
    }

}
