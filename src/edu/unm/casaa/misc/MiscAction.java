package edu.unm.casaa.misc;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import edu.unm.casaa.main.MainController;

public class MiscAction extends AbstractAction {

    private static final long   serialVersionUID = 1L;
    private MiscCode            code;

    MiscAction( MiscCode code ) {
        super( code.name );
        this.code = code;
    }

    public void actionPerformed( ActionEvent e ) {
        //MainController.instance.handleButtonMiscCode( code ); // Pass to MainController.
        System.out.println("integrate:MiscAction: unhandled action event");
    }

}
