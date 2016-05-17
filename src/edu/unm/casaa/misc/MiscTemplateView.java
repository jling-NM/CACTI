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

package edu.unm.casaa.misc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

//import edu.unm.casaa.main.ActionTable;
//import edu.unm.casaa.main.MainController;
//import edu.unm.casaa.main.Style;

/**
 * This class creates the empty shell GUI for the MISC Coding interface.
 * 
 * @author UNM CASAA
 *
 */
public class MiscTemplateView extends JPanel {

	private static final long serialVersionUID = 1L;

	//====================================================================
	// Fields
	//====================================================================
	//Window Constants and Variables
	private static final int PANEL_WIDTH	= 600;
	private static final int PANEL_HEIGHT	= 450;

	//GUI Components and Constants
	private Dimension dimMainPanel			= null;

	private JPanel panelHeader				= null;
	private JPanel panelPrevText			= null;
	private JPanel panelCurrentText			= null;
	private JPanel panelControls			= null;
	private JPanel panelLeftControls	    = null;
	private JPanel panelRightControls		= null;
	private static final int BUTTON_HOR_GAP	= 5;
	private static final int BUTTON_VER_GAP	= 4;

	private JButton	buttonStart				= null;

	private Dimension dimButtonSize			= null;
	private static final int BUTTON_WIDTH	= 90;
	private static final int BUTTON_HEIGHT	= 24;

	//User Feedback Components
	private JLabel labelFile				= null;
	private JTextField textFieldOrder		= null;
	private static final int ORDER_COLS		= 9;
	private JLabel labelOrder				= null;
	private JTextField textFieldCode		= null;
	private static final int CODE_COLS		= 9;
	private JLabel labelCode				= null;
	private JTextField textFieldStartTime	= null;
	private JTextField textFieldEndTime		= null;
	private static final int TIME_COLS		= 20;
	private JLabel labelStart				= null;
	private JLabel labelEnd					= null;
	private TitledBorder borderCurrent		= null;

	private JTextField textFieldPrev		= null;
	private static final int PREV_COLS		= 60;
	private TitledBorder borderPrev			= null;

	//private ActionTable		actionTable		= null; // Communication between GUI and MainController.

	// Coding controls.
	private HashMap< Integer, JButton > buttonMiscCode	= new HashMap< Integer, JButton >();

	//====================================================================
	// Constructor and Initialization Methods
	//====================================================================

	public MiscTemplateView(){
		//assert (actionTable != null);
		//this.actionTable = actionTable;
		init();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void init(){
        setMaximumSize( getDimMainPanel() );
        setMinimumSize( getDimMainPanel() );
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        parseUserControls();
        add( getPanelHeader() );
        add( getPanelPrevText() );
        add( getPanelCurrentText() );
        add( getPanelControls() );

        // Prevent control panels from expanding, as that breaks alignment.
        getPanelLeftControls().setMaximumSize( getPanelLeftControls().getMinimumSize() );
        getPanelRightControls().setMaximumSize( getPanelRightControls().getMinimumSize() );
        setVisible( true );
	}

	//====================================================================
	// Public Getter and Setter Methods
	//====================================================================

	public String toString(){
		return ("MISC");
	}
	
	public JLabel getLabelFile() {
		if( labelFile == null ) {
			labelFile = new JLabel();
		}
		return labelFile;
	}

	//====================================================================
	// Private Methods
	//====================================================================

	private JButton getButtonStart() {
		if( buttonStart == null ) {
			//buttonStart = new JButton( actionTable.get( "codeStart" ) );
			buttonStart = new JButton("Start Coding");
			//buttonStart.getActionMap().put( "pressed", buttonStart.getAction() );
			buttonStart.setPreferredSize( new Dimension(140, BUTTON_HEIGHT) ); // Larger button to fit label.
			buttonStart.setToolTipText( "Start Coding" );
		}
		return buttonStart;
	}

	private JPanel getPanelHeader(){
		if( panelHeader == null ){
			panelHeader = new JPanel();
			panelHeader.setLayout( new BorderLayout() );

			JPanel inner = new JPanel();

			inner.add( getButtonStart() );
			inner.add( getLabelFile() );
			panelHeader.add( inner, BorderLayout.LINE_START );
		}
		return panelHeader;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // MISC Code Buttons
    private JButton getButtonMiscCode( MiscCode miscCode ) {
        JButton button = buttonMiscCode.get( miscCode.value );

        // Create button if it does not yet exist.
        if( button == null ) {
            button = new JButton( new MiscAction( miscCode ) );

            button.setPreferredSize( getDimButtonSize() );

            // Prevent button from expanding or contracting, so all misc code buttons are the same size.
            button.setMinimumSize( getDimButtonSize() );
            button.setMaximumSize( getDimButtonSize() );

            button.setToolTipText( "" + miscCode.value );
            buttonMiscCode.put( miscCode.value, button );
        }
        return button;
    }

    // Parse user controls from XML file.
    private void parseUserControls() {
        //TODO: local user file path
        File    file    = new File( "/home/josef/projects/cacti/code/userConfiguration.xml" );

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
                        JPanel          panel       = null;

                        // Lookup panel.
                        if( panelName.equalsIgnoreCase( "left" ) ) {
                            panel   = getPanelLeftControls();
                        } else if( panelName.equalsIgnoreCase( "right" ) ) {
                            panel = getPanelRightControls();
                        }

                        // Parse controls, create border with given label.
                        if( panel == null ) {
                            //TODO: MainController.instance.handleUserCodesError( file, "codeControls panel unrecognized: " + panelName );
                            System.out.println("handleUserCodesError");
                        } else {
                            parseControlColumn( node, panel );
                            panel.setBorder( getBorderControls( panelLabel ) );
                        }
                    }
                }
            } catch( SAXParseException e ) {
                //TODO: MainController.instance.handleUserCodesParseException( file, e );
                System.out.println("handleUserCodesParseException");
            } catch( Exception e ) {
                //TODO: MainController.instance.handleUserCodesGenericException( file, e );
                System.out.println("handleUserCodesGenericException");
            }
        } else {
            //TODO: MainController.instance.handleUserCodesMissing( file );
            System.out.println("handleUserCodesMissing");
        }
    }

    // Parse a column of controls from given XML node.  Add buttons to given panel, and set panel layout.
	// Each child of given node is expected to be one row of controls.
	private void parseControlColumn( Node node, JPanel panel ) {
		// Count actual rows (node children), columns (max of any node child's children),
		// for use in layout.
		int numRows = 0;
		int	maxCols	= 0;

		for( Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
			if( row.getNodeName().equalsIgnoreCase( "row" ) ) {
				numRows++;
			} else {
				continue;
			}

			int	colsThisRow = 0;

			for( Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
			    String   cellName = cell.getNodeName();

			    if( cellName.equalsIgnoreCase( "button" ) ||
			        cellName.equalsIgnoreCase( "spacer" ) ||
					cellName.equalsIgnoreCase( "group") ) {
						colsThisRow++;
				}
			}
			maxCols = Math.max( maxCols, colsThisRow );
		}

		panel.setLayout( new GridLayout( numRows, maxCols, BUTTON_HOR_GAP, BUTTON_VER_GAP ) );

		// Traverse children, creating a row of buttons for each.
		for( Node row = node.getFirstChild(); row != null; row = row.getNextSibling() ) {
			if( !row.getNodeName().equalsIgnoreCase( "row" ) ) {
				continue;
			}
			int	colsThisRow = 0;

            for( Node cell = row.getFirstChild(); cell != null; cell = cell.getNextSibling() ) {
				// If cell represents a single value, add a button assigned to that code.
				// If cell represents a group of values, add a button that will open a popup.
				if( cell.getNodeName().equalsIgnoreCase( "button" ) ) {
					NamedNodeMap   map      = cell.getAttributes();
					String		   codeName = map.getNamedItem( "code" ).getTextContent();
					MiscCode	   code     = MiscCode.codeWithName( codeName );
					JButton        button   = getButtonMiscCode( code );

			        panel.add( button );

			        // Assign key binding (optional).
			        Node           keyStrokeNode = map.getNamedItem( "key" );

			        if( keyStrokeNode != null ) {
			            KeyStroke    keyBinding  = KeyStroke.getKeyStroke( keyStrokeNode.getTextContent() );

			            button.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyBinding, "pressed" );
		                button.getActionMap().put( "pressed", button.getAction() );
			        }
					colsThisRow++;
				} else if( cell.getNodeName().equalsIgnoreCase( "spacer" ) ) {
				    panel.add( Box.createRigidArea( getDimButtonSize() ) );
                    colsThisRow++;
				} else if( cell.getNodeName().equalsIgnoreCase( "group" ) ) {
					// Generate a popup menu to select one code from this group.
					NamedNodeMap 	groupMap 	= cell.getAttributes();
					String			groupLabel	= groupMap.getNamedItem( "label" ).getTextContent();
					JPopupMenu 		popup 		= new JPopupMenu();
					String			tooltipList	= "";

                    for( Node member = cell.getFirstChild(); member != null; member = member.getNextSibling() ) {
						if( !member.getNodeName().equalsIgnoreCase( "button" ) ) {
							continue;
						}

						NamedNodeMap 	memberMap 	     = member.getAttributes();
						String			memberCodeName   = memberMap.getNamedItem( "code" ).getTextContent();
						MiscCode		code		     = MiscCode.codeWithName( memberCodeName );
   						JMenuItem       item             = new JMenuItem( new MiscAction( code ) );

			            item.setToolTipText( "" + code.value );

	                    // Assign key binding (optional).
	                    Node           keyStrokeNode = memberMap.getNamedItem( "key" );

	                    if( keyStrokeNode != null ) {
	                        KeyStroke  keyBinding  = KeyStroke.getKeyStroke( keyStrokeNode.getTextContent() );
	                        String     actionName  = "pressed_" + code.name;

	                        // Map in panel, rather than menu item, so binding is available whether or not popup is open.
	                        // Use unique action name (including code name) to avoid overwriting "pressed" action.
                            getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyBinding, actionName );
                            getActionMap().put( actionName, item.getAction() );
	                    }

	                    // Add to popup.
						popup.add( item );

						// Include name in group button tooltip.
						if( tooltipList.length() > 0 )
							tooltipList += ", ";
						tooltipList += memberCodeName;
					}

                    // Add button to open popup.
					JButton 		button = new JButton( groupLabel );

					button.setPreferredSize( getDimButtonSize() );
					if( tooltipList.length() > 0 )
						button.setToolTipText( "Select from " + tooltipList );
					else
						button.setToolTipText( "Empty group" );
					button.addMouseListener( new PopupListener( popup ) );
					panel.add( button );

					colsThisRow++;
				}
			}

			// Add spacers to match max number of cells in any row.
			for( int i = colsThisRow; i < maxCols; i++ ) {
				panel.add( Box.createRigidArea( getDimButtonSize() ) );
			}
		}
	}

	private JPanel getPanelLeftControls() {
		if( panelLeftControls == null ) {
		    panelLeftControls = new JPanel();
		    panelLeftControls.setAlignmentY(Component.TOP_ALIGNMENT);
		}
		return panelLeftControls;
	}

	private JPanel getPanelRightControls(){
		if( panelRightControls == null ){
		    panelRightControls = new JPanel();
		    panelRightControls.setAlignmentY(Component.TOP_ALIGNMENT);
		}
		return panelRightControls;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelControls(){
		if( panelControls == null ){
			panelControls = new JPanel();
			panelControls.setLayout(new BoxLayout(panelControls, BoxLayout.Y_AXIS));

			JPanel panelInner = new JPanel();

			panelInner.setLayout(new BoxLayout(panelInner, BoxLayout.X_AXIS));
			panelInner.add(getPanelLeftControls());
            panelInner.add(getPanelRightControls());
            panelControls.add(panelInner);
		}
		return panelControls;
	}

	private Dimension getDimButtonSize(){
		if( dimButtonSize == null ){
			dimButtonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
		}
		return dimButtonSize;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldOrder(){
		if( textFieldOrder == null ){
			textFieldOrder = new JTextField(ORDER_COLS);
			textFieldOrder.setEditable(false);
			//Style.configureStrongText( textFieldOrder );
		}
		return textFieldOrder;
	}

	public void setTextFieldOrder(String text){
		getTextFieldOrder().setText(text);
	}

	private JLabel getLabelOrder(){
		if( labelOrder == null ){
			labelOrder = new JLabel("Enumeration");
			labelOrder.setLabelFor(getTextFieldOrder());
		}
		return labelOrder;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldCode(){
		if( textFieldCode == null ){
			textFieldCode = new JTextField(CODE_COLS);
			textFieldCode.setEditable(false);
			//Style.configureStrongText( textFieldCode );
		}
		return textFieldCode;
	}

	public void setTextFieldCode(String utteranceString){
		getTextFieldCode().setText(utteranceString);
	}

	private JLabel getLabelCode(){
		if( labelCode == null ){
			labelCode = new JLabel("MISC Code");
			labelCode.setLabelFor(getTextFieldOrder());
		}
		return labelCode;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldStartTime(){
		if( textFieldStartTime == null ){
			textFieldStartTime = new JTextField(TIME_COLS);
			textFieldStartTime.setEditable(false);
			//Style.configureStrongText( textFieldStartTime );
		}
		return textFieldStartTime;
	}

	public void setTextFieldStartTime(String utteranceString){
		getTextFieldStartTime().setText(utteranceString);
	}

	public void setTextFieldStartTimeColor(Color color){
		getTextFieldStartTime().setForeground(color);
	}

	private JLabel getLabelStart(){
		if( labelStart == null ){
			labelStart = new JLabel("Start Time");
			labelStart.setLabelFor(getTextFieldStartTime());
		}
		return labelStart;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldEndTime(){
		if( textFieldEndTime == null ){
			textFieldEndTime = new JTextField(TIME_COLS);
			textFieldEndTime.setEditable(false);
			//Style.configureStrongText( textFieldEndTime );
		}
		return textFieldEndTime;
	}

	public void setTextFieldEndTime(String utteranceString){
		getTextFieldEndTime().setText(utteranceString);
	}

	private JLabel getLabelEnd(){
		if( labelEnd == null ){
			labelEnd = new JLabel("End Time");
			labelEnd.setLabelFor(getTextFieldEndTime());
		}
		return labelEnd;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldPrev(){
		if( textFieldPrev == null ){
			textFieldPrev = new JTextField(PREV_COLS);
			textFieldPrev.setEditable(false);
			//Style.configureLightText( textFieldPrev );
		}
		return textFieldPrev;
	}

	public void setTextFieldPrev(String utteranceString){
		getTextFieldPrev().setText(utteranceString);
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelCurrentText(){
		if( panelCurrentText == null ){
			panelCurrentText = new JPanel();
			panelCurrentText.setBorder(getBorderCurrent());
			panelCurrentText.setLayout(new GridBagLayout());

			GridBagConstraints orderC = new GridBagConstraints();
			orderC.gridx = 0;
			orderC.gridy = 0;
			orderC.weightx = 1.0;
			orderC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelOrder(), orderC);

			GridBagConstraints codeC = new GridBagConstraints();
			codeC.gridx = 1;
			codeC.gridy = 0;
			codeC.weightx = 1.0;
			codeC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelCode(), codeC);

			GridBagConstraints startC = new GridBagConstraints();
			startC.gridx = 2;
			startC.gridy = 0;
			startC.weightx = 1.0;
			startC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelStart(), startC);

			GridBagConstraints endC = new GridBagConstraints();
			endC.gridx = 3;
			endC.gridy = 0;
			endC.weightx = 1.0;
			endC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelEnd(), endC);

			GridBagConstraints orderTC = new GridBagConstraints();
			orderTC.gridx = 0;
			orderTC.gridy = 1;
			orderTC.weightx = 1.0;
			orderTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldOrder(), orderTC);

			GridBagConstraints codeTC = new GridBagConstraints();
			codeTC.gridx = 1;
			codeTC.gridy = 1;
			codeTC.weightx = 1.0;
			codeTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldCode(), codeTC);

			GridBagConstraints startTC = new GridBagConstraints();
			startTC.gridx = 2;
			startTC.gridy = 1;
			startTC.weightx = 1.0;
			startTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldStartTime(), startTC);

			GridBagConstraints endTC = new GridBagConstraints();
			endTC.gridx = 3;
			endTC.gridy = 1;
			endTC.weightx = 1.0;
			endTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldEndTime(), endTC);
		}
		return panelCurrentText;
	}

	private TitledBorder getBorderCurrent(){
		if( borderCurrent == null ){
			borderCurrent = BorderFactory.createTitledBorder("Current Utterance");
			borderCurrent.setTitleJustification(TitledBorder.LEADING);
		}
		return borderCurrent;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelPrevText(){
		if( panelPrevText == null ){
			panelPrevText = new JPanel();
			panelPrevText.setBorder(getBorderPrev());
			panelPrevText.setLayout(new BorderLayout());
			panelPrevText.add(getTextFieldPrev(), BorderLayout.CENTER);
		}
		return panelPrevText;
	}

	private TitledBorder getBorderPrev(){
		if( borderPrev == null ){
			borderPrev = BorderFactory.createTitledBorder("Previous Utterance");
			borderPrev.setTitleJustification(TitledBorder.LEADING);
		}
		return borderPrev;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private TitledBorder getBorderControls( String label ){
	    TitledBorder border = BorderFactory.createTitledBorder( label );

	    border.setTitleJustification(TitledBorder.LEADING);
		return border;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Dimension getDimMainPanel(){
		if( dimMainPanel == null ){
			dimMainPanel = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
		}
		return dimMainPanel;
	}

	//===============================================================
	// PopupListener
	//===============================================================

	private class PopupListener extends MouseAdapter {
		private JPopupMenu popup = null;

		public PopupListener( JPopupMenu popup ) {
			this.popup = popup;
		}

		public void mousePressed( MouseEvent e ) {
            popup.show( e.getComponent(), e.getX(), e.getY() );
	    }
	}

}
