package edu.unm.casaa.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.prefs.Preferences;

/**
 * Move user config data handling out of controller
 */
public class UserConfig {

    // provide access to application preferences
    private static final Preferences appPrefs = Preferences.userNodeForPackage(Main.class);
    // default location of config file
    private static final String defaultPath = String.format("%s%s%s", System.getProperty("user.home"), System.getProperty("file.separator"), "CactiUserConfiguration.xml");


    /**
     * Writes out a minimal config file
     */
    public static void writeDefault() throws IOException {

        File config = new File(getPath());

        try (PrintWriter writer = new PrintWriter(new FileWriter(config, false))) {
            writer.println("<userConfiguration>");
            writer.println("<codes><!-- Therapist codes --><code name=\"CQ0\" value=\"11\"/><code name=\"OQ0\" value=\"14\"/></codes>");
            writer.println("<codeControls panel=\"left\" label=\"Therapist\">");
            writer.println("<row><button code=\"CQ0\"/><button code=\"OQ0\"/></row>");
            writer.println("</codeControls>");
            writer.println("<codeControls panel=\"right\" label=\"Client\">");
            writer.println("<row><button code=\"D+\"/><button code=\"D-\"/></row>");
            writer.println("</codeControls>");
            writer.println("<codes>!-- Client codes --><code name=\"D+\" value=\"34\"/><code name=\"D-\" value=\"35\"/></codes>");
            writer.println("<globals><global name=\"ACCEPTANCE\" label=\"Acceptance\" value=\"0\"/></globals>");
            writer.println("<globalControls panel=\"left\"><slider global=\"ACCEPTANCE\"/></globalControls>");
            writer.println("<globalControls panel=\"right\"><slider global=\"COLLABORATION\"/></globalControls>");
            writer.println("</userConfiguration>");

        } catch (IOException e) {
            throw e;
        }
    }


    /**
     * Check for config file
     * @return
     */
    public static boolean exists() {

        File file = new File(getPath());
        return file.canRead();

    }


    /**
     *
     * @param configPath
     */
    public static void setPath(String configPath) {
        appPrefs.put("configFilePath", configPath);
    }


    /**
     *
     * @return
     */
    public static String getPath() {

        // Don't want to send back empty path
        if( appPrefs.get("configFilePath","").isEmpty() ) {
            appPrefs.put("configFilePath", defaultPath);
        }
        return appPrefs.get("configFilePath", defaultPath );

    }

}

