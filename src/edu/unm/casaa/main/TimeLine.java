package edu.unm.casaa.main;

import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.utterance.Utterance;
import edu.unm.casaa.utterance.UtteranceList;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
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
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.IOException;


/**
 * Created by josef on 8/22/16.
 */
public class TimeLine extends Group {

    private int pixelsPerSecond	          = 50;     // sort of a framerate for the animation
    private TranslateTransition animation = null;   // animation for moving timeline
    private TimeLineMarker selectedMarker = null;   // store currently selected marker, if any
    private UtteranceList utteranceList   = null;   //
    private double height                 = 55.0;   // projected height of timeline. forces Group dimensions early
    private double thickness              = 2.0;    // thickness of line that represents time :)


    public TimeLine(Duration audioDuration, int pixelsPerSecond, double center, UtteranceList utteranceList) {

        this.pixelsPerSecond = pixelsPerSecond;
        this.utteranceList = utteranceList;

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

        renderUtterances();


        // initialize the animation of timeline
        animation = new TranslateTransition(audioDuration, this);
        // interpolator needs to be linear like the audio playback
        animation.setInterpolator(Interpolator.LINEAR);

        // translation is negative of the entire timeline width
        double byX = -audioDuration.toSeconds() * pixelsPerSecond;
        animation.setByX(byX);

        /**
         * Define listeners for change to utterance list
         * This links timeline markers to changes in utterance list
        */
        ObservableMap<String, Utterance> observableMap = utteranceList.getObservableMap();
        observableMap.addListener(new MapChangeListener() {
            public void onChanged(MapChangeListener.Change change) {
                if(change.wasAdded()){
                    Utterance utr = (Utterance) change.getValueAdded();
                    System.out.println("Add timeline marker:" + utr.toString());
                    addMarker(utr);

                } else if(change.wasRemoved()) {
                    Utterance utr = (Utterance) change.getValueRemoved();
                    System.out.println("Remove timeline marker:" + utr.toString());
                    removeMarker(utr);
                }
            }
        });

        this.getAnimation().setOnFinished((x) -> {
            getAnimation().pause();
            getAnimation().jumpTo(Duration.ZERO);
        });
    }



    /**
     * Add or update utterance on timeline and model
     *
     * @param newUtterance
     */
    public void addMarker(Utterance newUtterance) {
        // sync timeline with player time to marker appears lined up.
        this.getAnimation().jumpTo(newUtterance.getStartTime());

        // insert updated marker
        TimeLineMarker newMarker = new TimeLineMarker(newUtterance.getID(), newUtterance.getMiscCode().name, newUtterance.getStartTime().toSeconds(), newUtterance.getMiscCode().getSpeaker());
        this.getChildren().add(newMarker);
    }

    /**
     * Add or update utterance on timeline and model
     *
     * @param newUtterance
     */
    public void add(Utterance newUtterance) throws IOException {
        /* check for selected timeline marker */
        TimeLineMarker activeMarker = getSelectedMarker();

        /*
            if the timeline's active marker is not null
            we are in edit mode and will update code and speaker
         */
        if(activeMarker != null) {
            /* Edit active marker */
            String prevId  = activeMarker.getMarkerID();
            double prevPos = activeMarker.posSeconds;

            /* remove marker from timeline and model */
            utteranceList.remove(prevId);

            /* create new id with previous time and new code value */
            String newID = Utils.formatID(Duration.seconds(prevPos), newUtterance.getMiscCode().value);

            /* prevent flipping onto existing marker */
            Node r = this.lookup("#"+newUtterance.getID());
            if( r == null) {
                /*
                  update model, id and time = PREVIOUS, the other members can be updated
                 */
                newUtterance.setID(newID);
                newUtterance.setStartTime(prevPos);
                utteranceList.add(newUtterance);
            }

            // write data
            utteranceList.writeToFile();

        } else {
            /*  check if exact utterance is already in list.
                This to prevent timeline from adding duplicates.
             */
            Node r = this.lookup("#"+newUtterance.getID());
            if( r == null) {
                // new utterance in storage
                utteranceList.add(newUtterance);
                // write data
                utteranceList.writeToFile();
            }

        }

    }



    /*
      Call me when you want to delete an utterance marker from the timeline and model
    */
    public void remove(String markerID) throws IOException {
        Node r = this.lookup("#"+markerID);
        if( r != null) {
            utteranceList.remove(markerID);
            utteranceList.writeToFile();
        }
    }

    /*
        Call me when you want to delete an utterance marker from the timeline and model
    */
    public void removeMarker(Utterance utr) {
        Node r = this.lookup("#"+(utr.getID()));
        if( r != null) {
            this.getChildren().remove(r);
            this.setSelectedMarker(null);
        }
    }

    public void renderUtterances() {
        this.getChildren().remove(1, this.getChildren().size());
        utteranceList.values().forEach(this::addMarker);
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




    /*
        Represents a marker on the timeline.
        Inline so that it has access to TimeLine properties
     */
    public class TimeLineMarker extends VBox {

        private String markerID;            // how to find record in data
        private double posSeconds;          // start positon in bytes
        private MiscCode.Speaker speaker;   //
        private Text markerCode;            // displays utterance code for this marker
        private Polygon indicatorShape;     // displays arrow on timeline
        private int indicatorWidth = 12;    // size of arrow


        public TimeLineMarker(String markerID, String code, double posSeconds, MiscCode.Speaker speaker) {

            // Set spacing between nodes inside marker. specify spacing as CSS doesn't appear to work for this
            this.setSpacing(1.0);
            this.markerID = markerID;
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

            // set parent container to id that will be used to pull utterance;
            this.setId(markerID);
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
                try {
                    remove(getSelectedMarker().getMarkerID());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
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

        public String getMarkerID() {
            return markerID;
        }

        public void setMarkerID(String markerID) {
            this.markerID = markerID;
        }

        public String getCode() { return markerCode.getText(); }

        public void setCode(String code) { markerCode.setText(code); }

        public MiscCode.Speaker getSpeaker() { return speaker; }

        public void setSpeaker(MiscCode.Speaker newSpeaker) { this.speaker = newSpeaker; }

    }

}
