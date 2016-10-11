package edu.unm.casaa.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import edu.unm.casaa.utterance.Utterance;

// OldTimeline is a custom renderer for utterance data.
public class OldTimeline extends JPanel {
	private static final long serialVersionUID = 1L;

	private Dimension 		dimension 		= new Dimension( 800, 80 );
	private MainController 	control;
	private Insets			insets;
	private int				pixelsPerSecond	= 50; // Determines zoom level.

	public OldTimeline(MainController control ) {
		assert( control != null );
		this.control = control;
		setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 0) );
        //setOpaque(true);
		insets = getBorder().getBorderInsets( this );
        //dimension = getParent().getMaximumSize();
	}

	public Dimension getPreferredSize() {
		return dimension;
	}

	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		// Early-out if no audio file is loaded.
		int audioBytes = control.getAudioLength();

		if( audioBytes <= 0 ) {
			return;
		}

		// Find component bounds inside border (not affected by clip).
		int 		innerRight	= getWidth() - insets.right;
		int 		innerBottom	= getHeight() - insets.bottom;
		int			innerWidth	= innerRight - insets.left;
		int			innerHeight	= innerBottom - insets.top;

		// Adjust clip by border insets, so rendering does not overlap border.
		Rectangle 	clip 		= g.getClipBounds();
		int 		clipW		= clip.width;
		int 		clipH		= clip.height;
		int 		clipX		= clip.x;
		int 		clipY		= clip.y;

		if( clipX < insets.left )
		{
			clipW -= (insets.left - clipX);
			clipX = insets.left;
		}
		if( clipY < insets.top )
		{
			clipH -= (insets.top - clipY);
			clipY = insets.top;
		}
		clipW = Math.min( clipW, innerRight - clipX );
		clipH = Math.min( clipH, innerBottom - clipY );

		int			clipRight	= clipX + clipW;

		g.setClip( clipX, clipY, clipW, clipH );

		// OldTimeline.
		int fontAscent	= g.getFontMetrics().getAscent();
		int fontHeight	= g.getFontMetrics().getHeight();
		int centerLineY = insets.top + (innerHeight / 2);

		centerLineY += fontHeight / 2; // Shift down, since we have a line of text (time stamps) above boxes.

		g.setColor( Color.GRAY );
		g.drawLine( insets.left, centerLineY, innerRight, centerLineY );

		// Utterances.
		int bytesPerSecond 	= control.getBytesPerSecond();
		int bytesPerPixel	= (int) (bytesPerSecond / (float) pixelsPerSecond);

		assert( bytesPerPixel > 0 );

		// Scroll display to keep current time marker centered on screen
		// (except when we're at beginning/end).
		int playbackPosition	= control.getStreamPosition();
		int displayedBytes		= innerWidth * bytesPerPixel;
		int halfDisplayedBytes	= displayedBytes / 2;
		int scrollX				= 0;

		if( displayedBytes < audioBytes ) {
			if( playbackPosition > audioBytes - halfDisplayedBytes ) {
				// End.
				scrollX = audioBytes - displayedBytes;
			} else if( playbackPosition > halfDisplayedBytes ) {
				// Middle.
				scrollX = playbackPosition - halfDisplayedBytes;
			}
			scrollX /= bytesPerPixel;
		}

		int playbackX		= insets.left + (playbackPosition / bytesPerPixel) - scrollX;

		// Derive box positioning and size based on component and font dimensions.
		int	boxH			= fontHeight + 10; // Include some space around font.
		int boxY			= centerLineY - (boxH / 2);

		for( int i = 0; i < control.numUtterances(); i++ ) {
			Utterance 	u 			= control.utterance( i );

			// Draw box from start to end of current utterance.  If utterance end has not
			// been specified yet (i.e. is not yet parsed), end box at current playback time.
			int			startBytes	= u.getStartBytes();
			int			endBytes	= u.isParsed() ? u.getEndBytes() : Math.max( startBytes, playbackPosition );
			int 		boxW 		= (endBytes - startBytes) / bytesPerPixel;
			int  		boxX 		= insets.left + (startBytes / bytesPerPixel) - scrollX;

			// If box is clipped, skip.  NOTE: It is still possible for time stamp and/or label
			// to be partially visible.
			if( boxX > clipRight || (boxX + boxW) < clipX )
				continue;

			Color		borderColor	= new Color( 0.0f, 0.0f, 1.0f ); // Dark blue.
			Color		boxColor	= new Color( 0.75f, 0.75f, 1.0f ); // Light blue.

			// Highlight current utterance.
			if( u == control.getCurrentUtterance() ) {
				borderColor = new Color( 0.0f, 1.0f, 0.0f ); // Dark green.
				boxColor 	= new Color( 0.75f, 1.0f, 0.75f ); // Light green.
			}

			// Box.
			g.setColor( boxColor );
			g.fillRect( boxX, boxY, boxW, boxH );
			g.setColor( borderColor );
			g.drawRect( boxX, boxY, boxW, boxH );

			// Time stamp.
			if( u == control.getCurrentUtterance() ) {
				g.setColor( Color.BLACK );
			} else {
				g.setColor( Color.GRAY );
			}
			g.drawString( u.getStartTime(), boxX + 5, boxY - 5 );

			// Label (enumeration, MISC code label if coded).
			String	label = "" + u.getEnum() + ")";

			if( u.isCoded() ) {
				label += " " + u.getMiscCode().name;
			}
			g.drawString( label, boxX + 5, boxY + fontAscent + 5 );
		}

		// Current playback time indicator.
		g.setColor( Color.GRAY );
		g.fillRect( playbackX, insets.top, 2, innerHeight );

		// Restore original clip shape, so border is not clipped.
		g.setClip( clip );
	}
}
