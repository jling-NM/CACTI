package edu.unm.casaa.main;

import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.utterance.Utterance;
import edu.unm.casaa.utterance.UtteranceList;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.TimeStringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


/**
 * Created by josef on 8/22/16.
 */
public class TimeLine extends Group {

    private Duration audioDuration;                 // animation duration
    private int pixelsPerSecond	          = 50;     // sort of a framerate for the animation
    private TranslateTransition animation = null;   // animation for moving timeline
    private TimeLineMarker selectedMarker = null;   // store currently selected marker, if any

    private double height                 = 55.0;   // projected height of timeline. forces Group dimensions early
    private double thickness              = 2.0;    // thickness of line that represents time :)


    public TimeLine(Duration audioDuration, int pixelsPerSecond, double center, UtteranceList utteranceList) {

        this.audioDuration = audioDuration;
        this.pixelsPerSecond = pixelsPerSecond;

        /*
            horizontal center is beginning of timeline
            We position both cursor and start of timeline in center of screen
            Manual layout is ok as i don't want cursor to move when window is resized
            Manual layout is sort of required because the Node type is Group
        */
        // where does timeline begin; center
        this.setTranslateX(center);

        /*
        // expand Group node to full height with non-visible line
        Line v = new Line(0, 0, 0, this.height);
        //v.setStroke(Color.TRANSPARENT);
        v.setStroke(Color.INDIANRED);
        */

        /*
            the center line of the timeline
         */
        double end = audioDuration.toSeconds() * pixelsPerSecond;

        Line h = new Line(0,0,end,0);
        h.setStrokeWidth(thickness);
        h.setTranslateY( (height/2.0) + (thickness/2.0) );
        //this.getChildren().addAll(v, h);
        this.getChildren().add(h);


        // ?????
        setUtteranceList(utteranceList);


        // initialize the animation of timeline
        animation = new TranslateTransition(audioDuration, this);
        // interpolator needs to be linear like the audio playback
        animation.setInterpolator(Interpolator.LINEAR);

        // translation is negative of the entire timeline width
        double byX = -audioDuration.toSeconds() * pixelsPerSecond;
        animation.setByX(byX);
    }


    public void addMarker( Utterance newUtterance) {
        this.addMarker(newUtterance.getEnum(), newUtterance.getMiscCode().name, newUtterance.getStartTime().toSeconds(), newUtterance.getMiscCode().getSpeaker() );
    }

    /*
        Adds new utterance marker to timeline
     */
    public void addMarker(int markerIndx, String code, double posSeconds, MiscCode.Speaker speaker) {

        TimeLineMarker activeMarker = getSelectedMarker();

        /*
            if the timeline's active marker is not null
            we are in edit mode and will update code and speaker
         */
        if(activeMarker != null) {
            /* Edit active marker */

            int prevId      = activeMarker.getMarkerIndx();
            double prevPos  = activeMarker.posSeconds;

            removeMarker(prevId);

            TimeLineMarker newMarker = new TimeLineMarker(prevId, code, prevPos, speaker);
            this.getChildren().add(newMarker);

            this.setSelectedMarker(null);

        } else {
            /* add new marker */
            TimeLineMarker newMarker = new TimeLineMarker(markerIndx, code, posSeconds, speaker);
            this.getChildren().add(newMarker);

            this.setSelectedMarker(null);
        }
    }


    /*
        Call me when you want to delete an utterance marker from the timeline
     */
    public void removeMarker(int markerIndx){
        Node r = this.lookup("#"+Integer.toString(markerIndx));
        if( r != null) {
            /*
            r.setVisible(false);
            r.setDisable(true);
            */
            this.getChildren().remove(r);

            this.setSelectedMarker(null);
        }
    }

    public void setUtteranceList(UtteranceList utteranceList) {

        this.getChildren().remove(1, this.getChildren().size());


        // since utterance list is not a proper collection we old school it here
        for (int i = 0; i < utteranceList.size(); i++) {
            Utterance utterance = utteranceList.get(i);

            this.addMarker(
                    i,
                    utterance.getMiscCode().name,
                    utterance.getStartTime().toSeconds(),
                    utterance.getMiscCode().getSpeaker());

        }

    }

    public TimeLineMarker getSelectedMarker() {
        return selectedMarker;
    }

    public void setSelectedMarker(TimeLineMarker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    public TranslateTransition getAnimation() {
        return animation;
    }

    public void setAnimation(TranslateTransition animation) {
        this.animation = animation;
    }





    /*
        Represents a marker on the timeline.
        Inline so that it has access to TimeLine properties
     */
    public class TimeLineMarker extends VBox {

        private int markerIndx;             // how to find record in data
        private double posSeconds;          // start positon in bytes
        private MiscCode.Speaker speaker;   //
        private Text markerCode;            // displays utterance code for this marker
        private Polygon indicatorShape;     // displays arrow on timeline
        private int indicatorWidth = 12;    // size of arrow


        public TimeLineMarker(int markerIndx, String code, double posSeconds, MiscCode.Speaker speaker) {

            // Set spacing between nodes inside marker. specify spacing as CSS doesn't appear to work for this
            this.setSpacing(1.0);
            this.markerIndx = markerIndx;
            this.posSeconds = posSeconds;
            this.speaker = speaker;

            // where does marker point on timeline
            double tipPos = (posSeconds * pixelsPerSecond);

            // initialize utterance code
            markerCode = new Text(code);

            // formatting of marker varies on speaker (above/below timeline)
            if( speaker.equals(MiscCode.Speaker.Therapist) ) {
                this.indicatorShape = new Polygon(0,-indicatorWidth, indicatorWidth,0, indicatorWidth*2,-indicatorWidth);
                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker1");
                this.getChildren().addAll(markerCode, indicatorShape);
            } else {
                // speaker 2
                this.indicatorShape = new Polygon(0,indicatorWidth, indicatorWidth,0, indicatorWidth*2,indicatorWidth);

                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker2");
                this.getChildren().addAll(indicatorShape, markerCode);
                //
                this.setTranslateY( (height/2.0)+thickness );
            }

            // place on timeline
            this.setTranslateX(tipPos-indicatorWidth);
            this.setAlignment(Pos.TOP_CENTER);



            /*
                Filter the TimeLineMarker MouseEvent.MOUSE_CLICKED to control event order.
                If i simply assign setOnContextMenuRequested() this gets handled before the
                MouseEvent.MOUSE_CLICKED. This interferes with setting the selected marker
                prior to wanting to use it in context menu.
             */
            this.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {

                // Stop the timeline animation while user works with utterance code
                if( animation.getStatus() == Animation.Status.RUNNING ) {
                    animation.pause();
                }

                // get marker clicked on
                TimeLineMarker timeLineMarker = (TimeLineMarker) e.getSource();

                // select or deselect that marker
                if( timeLineMarker != null) {

                    if( timeLineMarker.equals(getSelectedMarker()) ) {
                        // selected marker clicked again. If left-click, delect this marker, if right-click, don't.

                        if( e.getButton().equals(MouseButton.PRIMARY)) {
                            timeLineMarker.getIndicatorShape().setFill(Color.TRANSPARENT);
                            setSelectedMarker(null);
                        }
                    } else {

                        // different marker selected, deselect any previous and select this one regardles of mouse button
                        if( getSelectedMarker() != null ) {
                            getSelectedMarker().getIndicatorShape().setFill(Color.TRANSPARENT);
                        }

                        // SELECT
                        timeLineMarker.getIndicatorShape().setFill(Color.INDIANRED);

                        // set active
                        setSelectedMarker(timeLineMarker);
                    }
                }

                // finished with accounting. Now, provide a popup menu on right-click that will use selectedMarker
                // "|| e.isControlDown()" is for OSX Ctrl+Click provided in addition to two finger click
                if( e.getButton().equals(MouseButton.SECONDARY) || e.isControlDown() ){
                    getContextMenu().show(this, Side.BOTTOM, 0, 0);
                }

            });

            // set parent container to id that will be used to pull utterance; startbyes or id in new scheme
            this.setId(Integer.toString(markerIndx));
        }


        /*
            generate right-click pop-up menu
         */
        private ContextMenu getContextMenu() {

            // context menu to return
            ContextMenu contextMenu = new ContextMenu();

            // menu item for deleting marker
            MenuItem mniRemoveMarker = new MenuItem("Remove Marker");
            mniRemoveMarker.setOnAction( e -> {
                removeMarker(getSelectedMarker().getMarkerIndx());
            });

            contextMenu.getItems().addAll(mniRemoveMarker);
            contextMenu.getStyleClass().add("contextMenu");

            return contextMenu;
        }



        public Polygon getIndicatorShape() {
            return indicatorShape;
        }

        public void setIndicatorShape(Polygon indicatorShape) {
            this.indicatorShape = indicatorShape;
        }

        public int getMarkerIndx() {
            return markerIndx;
        }

        public void setMarkerIndx(int markerIndx) {
            this.markerIndx = markerIndx;
        }

        public String getCode() { return markerCode.getText(); }

        public void setCode(String code) { markerCode.setText(code); }

        public MiscCode.Speaker getSpeaker() { return speaker; }

        public void setSpeaker(MiscCode.Speaker newSpeaker) { this.speaker = newSpeaker; }

    }

}
