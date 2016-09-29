package edu.unm.casaa.main;

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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;


/**
 * Created by josef on 8/22/16.
 */
public class TimeLine extends Group {

    private Duration audioDuration;                 // animation duration
    private int pixelsPerSecond	          = 50;     // sort of a framerate for the animation
    private TranslateTransition animation = null;   // animation for moving timeline
    private TimeLineMarker selectedMarker = null;   // store currently selected marker, if any


    public TimeLine(Duration audioDuration, int pixelsPerSecond, double center) {

        this.audioDuration = audioDuration;
        this.pixelsPerSecond = pixelsPerSecond;

        /*
            horizontal center is beginning of timeline
            We position both cursor and start of timeline in center of screen
            Manual layout is ok as i don't want cursor to move when window is resized
            TODO: see if better way to accomplish this
        */
        this.setTranslateX(center);

        /*
            the center line of the timeline

            It would be cool to make this with a slider instead
            and then bind all the properties together so user could drag timeline as
            well as player slider
         */
        double end = audioDuration.toSeconds() * pixelsPerSecond;
        Rectangle rect = new Rectangle(0,0,end,3);
        rect.setFill(Color.BLACK);
        this.getChildren().add(rect);

        // some temp inserts; later added by parsing of casaa file
        this.addMarker(111, "ICK", 1.0, 1);
        this.addMarker(222, "CV", 2.367, 2);
        this.addMarker(333, "SDS", 6.0, 2);
        this.addMarker(333, "HEL", 29.0, 1);

        // initialize the animation of timeline
        animation = new TranslateTransition(audioDuration, this);
        // interpolator needs to be linear like the audio playback
        animation.setInterpolator(Interpolator.LINEAR);

        // translation is negative of the entire timeline width
        double byX = -audioDuration.toSeconds() * pixelsPerSecond;
        animation.setByX(byX);
    }

    /*
        could be a convenience function
     */
    public void play() {
        animation.play();
    }

    /*
        Adds new utterance marker to timeline
     */
    private void addMarker(int someUniqueLineId, String code, double posSeconds, int speakerNum) {
        TimeLineMarker timeLineMarker = new TimeLineMarker(someUniqueLineId, code, posSeconds, speakerNum);
        this.getChildren().add(timeLineMarker);
    }


    /*
        Call me when you want to delete an utterance marker from the timeline
     */
    private void removeMarker(int someUniqueLineId){
        Node r = this.lookup("#"+Integer.toString(someUniqueLineId));
        if( r != null) {
            /*
            r.setVisible(false);
            r.setDisable(true);
            */
            this.getChildren().remove(r);
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
        private int speakerNum;             // which speaker for this utterance

        private Text markerCode;            // displays utterance code for this marker
        private Polygon indicatorShape;     // displays arrow on timeline
        private int indicatorWidth = 12;    // size of arrow


        public TimeLineMarker(int markerIndx, String code, double posSeconds, int speakerNum) {

            // Set spacing between nodes inside marker. specify spacing as CSS doesn't appear to work for this
            this.setSpacing(1.0);
            this.markerIndx = markerIndx;
            this.posSeconds = posSeconds;
            this.speakerNum = speakerNum;

            // where does marker point on timeline
            double tipPos = posSeconds * pixelsPerSecond;

            // initialize utterance code
            markerCode = new Text(code);

            // formatting of marker varies on speaker (above/below timeline)
            if( speakerNum == 1) {
                this.indicatorShape = new Polygon(tipPos,indicatorWidth, tipPos-indicatorWidth,0, tipPos+indicatorWidth,0);
                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker1");
                this.getChildren().addAll(markerCode, indicatorShape);
                this.setLayoutY(-28);
            } else {
                // speaker 2
                this.indicatorShape = new Polygon(tipPos,0, tipPos-indicatorWidth,indicatorWidth, tipPos+indicatorWidth,indicatorWidth);
                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker2");
                this.getChildren().addAll(indicatorShape, markerCode);
                this.setLayoutY(3);
            }

            // place on timeline
            this.setLayoutX(tipPos);
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

        public int getSpeakerNum() { return speakerNum; }

        public void switchSpeakerNum() {

            // assume only two speakers and just switch
            int newSpeaker = this.speakerNum == 1 ? 2 : 1;

            /* method 1 - just remove and re-add marker
            *   advantage is that it reuses instead of duplicating code
            * */
            int prevId      = this.getMarkerIndx();
            String prevCode = this.getCode();
            double prevPos  = this.posSeconds;

            removeMarker(prevId);
            addMarker(prevId, prevCode, prevPos, newSpeaker);

            /* method 2 - change properties
                this creates lots of duplicate code
            if( newSpeaker == 1) {
                this.getChildren().remove(this.indicatorShape);
                this.indicatorShape = new Polygon(this.bytePos,indicatorWidth, this.bytePos-indicatorWidth,0, this.bytePos+indicatorWidth,0);
                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker1");
                this.getChildren().add(indicatorShape);
                this.setLayoutY(-28);
            } else {
                // speaker 2
                this.getChildren().removeAll(this.indicatorShape, markerCode);
                this.indicatorShape = new Polygon(this.bytePos,0, this.bytePos-indicatorWidth,indicatorWidth, this.bytePos+indicatorWidth,indicatorWidth);
                this.indicatorShape.getStyleClass().add("unselectedMarkerShape");
                markerCode.getStyleClass().add("unselectedMarkerTextSpeaker2");
                this.getChildren().addAll(indicatorShape, markerCode);
                this.setLayoutY(3);
            }
            */

            // finally, update to new speaker number
            this.speakerNum = newSpeaker;

        }
    }

}
