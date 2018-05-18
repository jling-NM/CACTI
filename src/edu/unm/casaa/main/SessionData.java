package edu.unm.casaa.main;


import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.Utterance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Duration;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;


/**
 * Session Data Model
 */
public class SessionData
{
    private File sessionFile;
    private final SQLiteDataSource ds;
    private String audioFilePath = "";

    /**
     * Store and manipulate list of utterances for session
     */
    public UtteranceList utteranceList;

    /**
     * Store and manipulate list of global ratings for session
     */
    public Ratings ratingsList;

    /**
     * Available session attributes
     */
    public enum SessionAttributes {
        AUDIO_FILE_PATH,
        GLOBAL_NOTES
    }


    /**
     * Common initDB code
     */
    private SessionData () {
        // database config
        SQLiteConfig config = new SQLiteConfig();
        // enable foreign key constraints
        config.enforceForeignKeys(true);
        // give database a version
        config.setUserVersion(1);
        // attach config to our datasource
        ds = new SQLiteDataSource(config);
    }


    /**
     * Initialize new session data
     * @param storageFile session data file
     * @param audioFile session audio file
     * @throws IOException
     */
    public SessionData(File storageFile, File audioFile) throws IOException {

        this();
        sessionFile = storageFile;
        audioFilePath = audioFile.getAbsolutePath();
        ds.setUrl("jdbc:sqlite:" + sessionFile.getAbsolutePath());

        try {
            initDB();
            utteranceList = new SessionData.UtteranceList();
            ratingsList = new SessionData.Ratings();
        } catch (SQLException e) {
            // constructor can fail for basic IO or SQL
            // convert SQLException as we don't need the SQL details here
            throw new IOException(e);
        }
    }


    /**
     * Initialize with existing session file
     * @param storageFile session data file
     * @throws IOException
     */
    public SessionData(File storageFile) throws IOException {

        this();
        sessionFile = storageFile;
        ds.setUrl("jdbc:sqlite:" + sessionFile.getAbsolutePath());

        // check file exists
        if( sessionFileExists() ) {
            // check file format
            if( isSQLiteDataFile() ) {
                // try loading file
                try {
                    // TODO: this take a long time. WHY?
                    audioFilePath = getAttribute(SessionAttributes.AUDIO_FILE_PATH);
                    utteranceList = new SessionData.UtteranceList();
                    ratingsList = new SessionData.Ratings();
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            } else {
                // wrong file format
                throw new FileFormatException( "File is not correct format:\n"+sessionFile.getAbsolutePath() );
            }
        } else {
            // file not found
            throw new FileNotFoundException("File not found.");
        }
    }



    /**
     * Test if current session file is a SQLite database
     */
    private boolean isSQLiteDataFile() throws IOException {

        // Test selected file format by looking for "SQLite" at beginning of file
        try ( FileReader textFileReader = new FileReader(sessionFile) ) {
            char[] buffer = new char[6];
            int numberOfCharsRead = textFileReader.read(buffer);
            //System.out.println("XX" + String.valueOf(buffer, 0, numberOfCharsRead) + "XX");
            return String.valueOf(buffer, 0, numberOfCharsRead).startsWith("SQLite");
        }
    }


    /**
     * Retrieve ratings from database
     * @return HashMap of ratings
     * @throws SQLException
     */
    private HashMap< Integer, Integer > getRatingsMap() throws SQLException {

        HashMap< Integer, Integer > ratings = new HashMap<>();

        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement() ) {

            ResultSet rs = statement.executeQuery("select rating_id, response_value from ratings");

            while (rs.next()) {
                int ratingId = rs.getInt("rating_id");
                int responseValue = rs.getInt("response_value");
                ratings.put(ratingId, responseValue);
            }

            return ratings;
        }
    }


    /**
     * @param utterance_id
     * @return array of global rating ids already linked to this utterance
     * @throws SQLException
     */
    public ArrayList<Integer> getUtteranceRatingIDs(String utterance_id) throws SQLException {

        ArrayList<Integer> selectedRatingIDs = new ArrayList<>();

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement("select ratings.rating_id from ratings left outer join utterances_ratings on utterances_ratings.rating_id = ratings.rating_id where utterances_ratings.utterance_id = ?;")
        ) {

            ps.setString(1, utterance_id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                selectedRatingIDs.add(rs.getInt("rating_id"));
            }

            return selectedRatingIDs;
        }
    }



    /**
     * @return
     * @throws SQLException
     */
    public ArrayList< TreeMap< String, String> > getGlobalUtterances() throws SQLException
    {
        String sql = "SELECT ratings.rating_id, ratings.rating_name, ratings.response_value, utterances.utterance_id, utterances.time_marker, utterances.annotation, codes.code_name " +
                "FROM ratings " +
                "JOIN utterances_ratings ON ratings.rating_id = utterances_ratings.rating_id " +
                "JOIN utterances ON utterances.utterance_id = utterances_ratings.utterance_id " +
                "JOIN codes ON utterances.code_id = codes.code_id " +
                "ORDER BY ratings.rating_name, utterances.time_marker";

        TreeMap< String, String> row;
        ArrayList< TreeMap< String, String> > records = new ArrayList<>();


        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement()  ) {

            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                String rating_id = rs.getString("rating_id");
                String rating_name = rs.getString("rating_name");
                String rating_value = rs.getString("response_value");
                String time_marker = rs.getString("time_marker");
                String code_name = rs.getString("code_name");
                String annotation = rs.getString("annotation");

                row = new TreeMap<>();
                row.put("rating_id", rating_id);
                row.put("rating_name", rating_name);
                row.put("rating_value", rating_value);
                row.put("time_marker", time_marker);
                row.put("code_name", code_name);
                row.put("annotation", annotation);
                records.add(row);
            }

            return records;
        }
    }


    /**
     * @return A populated map of utterances
     * @throws SQLException
     */
    private SortedMap< String, Utterance > getUtterances() throws SQLException
    {
        SortedMap< String, Utterance > utteranceTreeMap = new TreeMap<>();

        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement() ) {

            ResultSet rs = statement.executeQuery("select utterances.*, codes.code_name, codes.speaker_id from utterances inner join codes on utterances.code_id = codes.code_id order by utterances.time_marker");

            while (rs.next()) {

                String utterance_id = rs.getString("utterance_id");
                Duration startTime = Utils.parseDuration(rs.getString("time_marker"));
                int codeId = rs.getInt("code_id");
                String codeName = rs.getString("code_name");
                int speakerId = rs.getInt("speaker_id");
                MiscCode code = new MiscCode(codeId, codeName, MiscCode.Speaker.values()[speakerId]);
                MiscDataItem item = new MiscDataItem(Utils.formatID(startTime, codeId), startTime);
                item.setMiscCode(code);

                utteranceTreeMap.put(utterance_id, item);
            }

            return utteranceTreeMap;
        }
    }



    /**
     * Provide a count of codes used in all utterances
     * @return map<code_name, count>
     * @throws SQLException
     */
    private HashMap< String, Integer > getCodeCounts() throws SQLException {
        HashMap< String, Integer > mapCodeCount = new HashMap<>();

        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement() ) {

            ResultSet rs = statement.executeQuery("select codes.code_name, count(codes.code_id) as code_count from codes join utterances on codes.code_id = utterances.code_id group by codes.code_name;");

            while (rs.next()) {

                String code_name = rs.getString("code_name");
                int code_count = rs.getInt("code_count");
                mapCodeCount.put( code_name, code_count);
            }

            return mapCodeCount;
        }
    }



    public HashMap< String, Double > getCodeSummaryMap() throws SQLException {

        HashMap<String, Integer> mapCodeCount = null;
        // get counts
        mapCodeCount = getCodeCounts();


        /* create map to store summary scores*/
        HashMap< String, Double > mapCodeSummary = new HashMap<>();
        /* Simply get counts */
        mapCodeSummary.put("SUM_ADP", mapCodeCount.getOrDefault("ADP", 0).doubleValue());
        mapCodeSummary.put("SUM_ADW", mapCodeCount.getOrDefault("ADW", 0).doubleValue());
        mapCodeSummary.put("SUM_AF", mapCodeCount.getOrDefault("AF", 0).doubleValue());
        mapCodeSummary.put("SUM_CO", mapCodeCount.getOrDefault("CO", 0).doubleValue());
        mapCodeSummary.put("SUM_DI", mapCodeCount.getOrDefault("DI", 0).doubleValue());
        mapCodeSummary.put("SUM_EC", mapCodeCount.getOrDefault("EC", 0).doubleValue());
        mapCodeSummary.put("SUM_GI", mapCodeCount.getOrDefault("GI", 0).doubleValue());
        mapCodeSummary.put("SUM_RCP", mapCodeCount.getOrDefault("RCP", 0).doubleValue());
        mapCodeSummary.put("SUM_RCW", mapCodeCount.getOrDefault("RCW", 0).doubleValue());
        mapCodeSummary.put("SUM_ST", mapCodeCount.getOrDefault("ST", 0).doubleValue());
        mapCodeSummary.put("SUM_RF", mapCodeCount.getOrDefault("RF", 0).doubleValue());
        mapCodeSummary.put("SUM_SU", mapCodeCount.getOrDefault("SU", 0).doubleValue());
        mapCodeSummary.put("SUM_WA", mapCodeCount.getOrDefault("WA", 0).doubleValue());

        /* Calculate sums of different code combinations */

        // sum(C+,R+,D+,A+,N+,TS+,O+)
        mapCodeSummary.put("SUM_CHANGE_ETOH",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("C+") || e.getKey().equals("R+") || e.getKey().equals("D+") || e.getKey().equals("A+") || e.getKey().equals("N+") || e.getKey().equals("TS+") || e.getKey().equals("O+") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(C-,R-,D-,A-,N-,TS-,O-)
        mapCodeSummary.put("SUM_SUSTAIN_ETOH",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("C-") || e.getKey().equals("R-") || e.getKey().equals("D-") || e.getKey().equals("A-") || e.getKey().equals("N-") || e.getKey().equals("TS-") || e.getKey().equals("O-") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(C+_m, R+_m, D+_m, A+_m, N+_m, TS+_m, O+_m)
        mapCodeSummary.put("SUM_CHANGE_DRUG",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("C+_m") || e.getKey().equals("R+_m") || e.getKey().equals("D+_m") || e.getKey().equals("A+_m") || e.getKey().equals("N+_m") || e.getKey().equals("TS+_m") || e.getKey().equals("O+_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(C-_m, R-_m, D-_m, A-_m, N-_m, TS-_m, O-_m)
        mapCodeSummary.put("SUM_SUSTAIN_DRUG",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("C-_m") || e.getKey().equals("R-_m") || e.getKey().equals("D-_m") || e.getKey().equals("A-_m") || e.getKey().equals("N-_m") || e.getKey().equals("TS-_m") || e.getKey().equals("O-_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(ADP,AF,EC,RCP,SU)
        mapCodeSummary.put("SUM_MICO",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("ADP") || e.getKey().equals("AF") || e.getKey().equals("EC") || e.getKey().equals("RCP") || e.getKey().equals("SU") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(ADW,CO,DI,RCW,WA)
        mapCodeSummary.put("SUM_MIIN",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("ADW") || e.getKey().equals("CO") || e.getKey().equals("DI") || e.getKey().equals("RCW") || e.getKey().equals("WA") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(OQ-,OQ0,OQ+)
        mapCodeSummary.put("SUM_OQ",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("OQ-") || e.getKey().equals("OQ0") || e.getKey().equals("OQ+") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(CQ-,CQ0,CQ+)
        mapCodeSummary.put("SUM_CQ",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("CQ-") || e.getKey().equals("CQ0") || e.getKey().equals("CQ+") )
                        .mapToDouble(e -> e.getValue())
                        .sum());


        // sum(CR+,CR-,CR0,CR+/-)
        mapCodeSummary.put("SUM_CR",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("CR+") || e.getKey().equals("CR-") || e.getKey().equals("CR0") || e.getKey().equals("CR+/-") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(CQ-,CQ0,CQ+,OQ-,OQ0,OQ+)
        mapCodeSummary.put("SUM_QUESTION",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("CQ-") || e.getKey().equals("CQ0") || e.getKey().equals("CQ+") || e.getKey().equals("OQ-") || e.getKey().equals("OQ0") || e.getKey().equals("OQ+") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(SR+,SR-,SR0,SR+/-)
        mapCodeSummary.put("SUM_SIMPLE",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("SR+") || e.getKey().equals("SR-") || e.getKey().equals("SR0") || e.getKey().equals("SR+/-") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(SR+,SR-,SR0,SR+/-,CR+,CR-,CR0,CR+/-)
        mapCodeSummary.put("SUM_REFLECTION",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("SR+") || e.getKey().equals("SR-") || e.getKey().equals("SR0") || e.getKey().equals("SR+/-") || e.getKey().equals("CR+") || e.getKey().equals("CR-") || e.getKey().equals("CR0") || e.getKey().equals("CR+/-") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(Rem+, rem+_m)
        mapCodeSummary.put("SUM_REM_POS",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("Rem+") || e.getKey().equals("Rem+_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(Rem-, rem-_m)
        mapCodeSummary.put("SUM_REM_NEG",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("Rem-") || e.getKey().equals("Rem-_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(FN, rem+, rem-, rem+_m, rem-_m)
        mapCodeSummary.put("SUM_FN_RM",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("FN") || e.getKey().equals("Rem+") || e.getKey().equals("Rem-") || e.getKey().equals("Rem+_m") || e.getKey().equals("Rem-_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(SR+, CR+)
        mapCodeSummary.put("SUM_REF_CT",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("SR+") || e.getKey().equals("CR+") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // sum(SR-, CR-)
        mapCodeSummary.put("SUM_REF_ST",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("SR-") || e.getKey().equals("CR-") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // client_utt = sum( C+	R+	D+	A+	N+	TS+	O+	C-	R-	D-	A-	N-	TS-	O-	C+_m	R+_m	D+_m	A+_m	N+_m	TS+_m	O+_m	C-_m	R-_m	D-_m	A-_m	N-_m	TS-_m	O-_m	FN, Rem+, Rem-, Rem+_m, Rem-_m).
        mapCodeSummary.put("SUM_CLIENT_UTT",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("C+") || e.getKey().equals("R+") || e.getKey().equals("D+") || e.getKey().equals("A+") || e.getKey().equals("N+") || e.getKey().equals("TS+") || e.getKey().equals("O+") || e.getKey().equals("C-") || e.getKey().equals("R-") || e.getKey().equals("D-") || e.getKey().equals("A-") || e.getKey().equals("N-") || e.getKey().equals("TS-") || e.getKey().equals("O-") || e.getKey().equals("C+_m") || e.getKey().equals("R+_m") || e.getKey().equals("D+_m") || e.getKey().equals("A+_m") || e.getKey().equals("N+_m") || e.getKey().equals("TS+_m") || e.getKey().equals("O+_m") || e.getKey().equals("C-_m") || e.getKey().equals("R-_m") || e.getKey().equals("D-_m") || e.getKey().equals("A-_m") || e.getKey().equals("N-_m") || e.getKey().equals("TS-_m") || e.getKey().equals("O-_m") || e.getKey().equals("") || e.getKey().equals("FN") || e.getKey().equals("Rem+") || e.getKey().equals("Rem-") || e.getKey().equals("Rem+_m") || e.getKey().equals("Rem-_m") )
                        .mapToDouble(e -> e.getValue())
                        .sum());

        // ther_utt = sum(ADP	ADW	AF	CO	DI	EC	FA	FI	GI	CQ-	CQ0	CQ+	OQ-	OQ0	OQ+	RCP	RCW	SR+	SR-	SR0	SR+/-	CR+	CR-	CR0	CR+/-	RF	ST	SU	WA, p)
        mapCodeSummary.put("SUM_THER_UTT",
                mapCodeCount.entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals("ADP") || e.getKey().equals("ADW") || e.getKey().equals("AF") || e.getKey().equals("CO") || e.getKey().equals("DI") || e.getKey().equals("EC") || e.getKey().equals("FA") || e.getKey().equals("FI") || e.getKey().equals("GI") || e.getKey().equals("CQ-") || e.getKey().equals("CQ0") || e.getKey().equals("CQ+") || e.getKey().equals("OQ-") || e.getKey().equals("OQ0") || e.getKey().equals("OQ+") || e.getKey().equals("RCP") || e.getKey().equals("RCW") || e.getKey().equals("SR+") || e.getKey().equals("SR-") || e.getKey().equals("SR0") || e.getKey().equals("SR+/-") || e.getKey().equals("CR+") || e.getKey().equals("CR-") || e.getKey().equals("CR0") || e.getKey().equals("CR+/-") || e.getKey().equals("RF") || e.getKey().equals("ST") || e.getKey().equals("SU") || e.getKey().equals("WA") || e.getKey().equals("P") )
                        .mapToDouble(e -> e.getValue())
                        .sum());


        /* calculate sums by group */

        // sum(adp to FN, Rem+, Rem-, Rem+_m, Rem-_m, p)
        mapCodeSummary.put("SUM_TOTAL_UTT", (mapCodeSummary.get("SUM_THER_UTT") + mapCodeSummary.get("SUM_CLIENT_UTT")) );
        // Change = sum(change_etoh, change_drug)
        mapCodeSummary.put("SUM_CHANGE", (mapCodeSummary.get("SUM_CHANGE_ETOH") + mapCodeSummary.get("SUM_CHANGE_DRUG")) );
        // Sustain = sum(sustain_etoh, sustain_drug).
        mapCodeSummary.put("SUM_SUSTAIN", (mapCodeSummary.get("SUM_SUSTAIN_ETOH") + mapCodeSummary.get("SUM_SUSTAIN_DRUG")) );
        // change_rem = sum(change, Rem+, rem+_m).
        mapCodeSummary.put("SUM_CHANGE_REM", (mapCodeSummary.get("SUM_CHANGE") + mapCodeSummary.get("SUM_REM_POS")) );
        // sustain_rem = sum(sustain, Rem-, rem-_m).
        mapCodeSummary.put("SUM_SUSTAIN_REM", (mapCodeSummary.get("SUM_SUSTAIN") + mapCodeSummary.get("SUM_REM_NEG")) );

        /* calculate percentage scores */
        Double setVal = 0.0;

        // pmic = mico/sum(mico, miin).
        setVal = ((mapCodeSummary.get("SUM_MICO") / (mapCodeSummary.get("SUM_MICO") + mapCodeSummary.get("SUM_MIIN")))*100);
        mapCodeSummary.put("PCT_MIC", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // pct_etoh = change_etoh/sum(change_etoh,sustain_etoh).
        setVal = ((mapCodeSummary.get("SUM_CHANGE_ETOH") / (mapCodeSummary.get("SUM_CHANGE_ETOH") + mapCodeSummary.get("SUM_SUSTAIN_ETOH")))*100);
        mapCodeSummary.put("PCT_ETOH", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // pct_drug = change_drug/sRATIO_THER2CLIum(change_drug, sustain_drug).
        setVal = ((mapCodeSummary.get("SUM_CHANGE_DRUG") / (mapCodeSummary.get("SUM_CHANGE_DRUG") + mapCodeSummary.get("SUM_SUSTAIN_DRUG")))*100);
        mapCodeSummary.put("PCT_DRUG", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // pct = change/sum(change, sustain).
        setVal = ((mapCodeSummary.get("SUM_CHANGE") / (mapCodeSummary.get("SUM_CHANGE") + mapCodeSummary.get("SUM_SUSTAIN")))*100);
        mapCodeSummary.put("PCT", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // pcr=sum(CR+,CR_,CR0,CR+/-)/sum(SR+,SR-,SR0,SR+/-,CR+,CR-,CR0,CR+/-)
        setVal = ((mapCodeSummary.get("SUM_CR") / mapCodeSummary.get("SUM_REFLECTION")))*100;
        mapCodeSummary.put("PCT_PCR", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // poq = sum(OQ-,OQ0,OQ+)/sum(CQ-,CQ0,CQ+,OQ-,OQ0,OQ+).
        setVal = ((mapCodeSummary.get("SUM_OQ") / mapCodeSummary.get("SUM_QUESTION")))*100;
        mapCodeSummary.put("PCT_POQ", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);

        // sr2cr = sum(SR+,SR-,SR0,SR+/-)/sum(CR+,CR-,CR0,CR+/-).
        setVal = (mapCodeSummary.get("SUM_SIMPLE") / mapCodeSummary.get("SUM_CR"));
        mapCodeSummary.put("RATIO_SR2CR", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // r2q=sum(SR+,SR-,SR0,SR+/-,CR+,CR-,CR0,CR+/-)/sum(CQ-,CQ0,CQ+,OQ-,OQ0,OQ+)
        setVal = (mapCodeSummary.get("SUM_REFLECTION") / mapCodeSummary.get("SUM_QUESTION"));
        mapCodeSummary.put("RATIO_R2Q", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);
        // ther2cli = ther_utt/client_utt.
        setVal = (mapCodeSummary.get("SUM_THER_UTT") / mapCodeSummary.get("SUM_CLIENT_UTT"));
        mapCodeSummary.put("RATIO_THER2CLI", setVal.isNaN() || setVal.isInfinite() ? 0.0 : setVal);

        return mapCodeSummary;
    }



    /**
     * Reset utterance list to contents of list
     * @param utteranceList List of utterances to load
     * @throws SQLException
     */
    public void setUtteranceList(List<Utterance> utteranceList) throws SQLException {
        String sql = "insert into utterances (utterance_id, code_id, time_marker, annotation) values (?,?,?,?)";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql)  )
        {
            connection.setAutoCommit(false);
            for (Utterance utr : utteranceList) {
                ps.setString(1, utr.getID());
                ps.setInt(2, utr.getMiscCode().value);
                ps.setString(3, Utils.formatDuration(utr.getStartTime()));
                ps.setString(4, utr.getAnnotation());
                ps.addBatch();
            }

            ps.executeBatch();
            // apply changes
            connection.commit();
        }

        /* reload from now populated db */
        this.utteranceList = new SessionData.UtteranceList();

    }



    public void setRatingsList(HashMap<String, Integer> ratingsMap) throws SQLException {
        for (Map.Entry<String, Integer> entry : ratingsMap.entrySet()) {
            addRating(entry.getKey(), entry.getValue());
        }

        /* reload from now populated db */
        this.ratingsList = new SessionData.Ratings();

    }


    /**
     * @param attribute SessionAttribute to update
     * @param value new value
     * @throws SQLException
     */
    private void setAttribute(SessionAttributes attribute, String value) throws SQLException
    {
        String sql = "update attributes set value = ? where name = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql)  )
        {
            ps.setString(1, value);
            ps.setString(2, attribute.name());
            ps.executeUpdate();
        }
    }


    /**
     * Audio file path associated with this session
     * @return full file path
     */
    public String getAudioFilePath() {
        return this.audioFilePath;
    }


    /**
     * @param filePath new audio file path
     */
    public void setAudioFilePath(String filePath) throws SQLException {
        this.audioFilePath = filePath;
        setAttribute(SessionAttributes.AUDIO_FILE_PATH, this.audioFilePath);
    }


    /**
     * @return Session file path
     */
    public String getSessionFilePath() {
        return this.sessionFile.getAbsolutePath();
    }


    public String getSessionLabel() {
        return sessionFile.getName().substring(0, sessionFile.getName().lastIndexOf("."));
    }

    /**
     * @param sessionAttribute SessionAttribute to get
     * @return current value for session attribute
     * @throws SQLException
     */
    public String getAttribute(SessionAttributes sessionAttribute) throws SQLException
    {
        String sql = "select value from attributes where name = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setString(1, sessionAttribute.name());
            ResultSet rs = ps.executeQuery();

            if( rs.next() ) {
                return rs.getString("value");
            } else {
                return "";
            }
        }
    }




    public String getUtteranceAnnotationText(String utterance_id) throws SQLException {
        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement("select annotation from utterances where utterance_id = ?") ) {

            // annotation text
            ps.setString(1, utterance_id);
            ResultSet rs = ps.executeQuery();
            if( rs.next() ) {
                return rs.getString("annotation");
            } else {
                return "";
            }
        }
    }




    public void annotateUtterance(String utterance_id, String annotationText, ArrayList<GlobalCode> globalsList) throws SQLException
    {
        try ( Connection connection = ds.getConnection();
              PreparedStatement psU = connection.prepareStatement("update utterances set annotation = ? where utterance_id = ?");
              PreparedStatement psI = connection.prepareStatement("INSERT INTO utterances_ratings (utterance_id, rating_id) VALUES (?, ?)");
              Statement statement = connection.createStatement()
            ) {

            /* disable autocommit */
            connection.setAutoCommit(false);

            /* handle annotation text */
            psU.setString(1, annotationText);
            psU.setString(2, utterance_id);
            psU.executeUpdate();

            /* clear all utterance to rating links */
            statement.execute("delete from utterances_ratings where utterance_id = '"+ utterance_id + "'");

            /* add selected utterance to rating links */
            for (GlobalCode gc : globalsList ) {
                psI.setString(1, utterance_id);
                psI.setInt(2, gc.id);
                psI.addBatch();
            }
            psI.executeBatch();

            // apply changes
            connection.commit();
        }
    }



    /**
     * Remove utterance from datasource
     * @param utterance_id
     * @throws SQLException
     */
    private void removeUtterance(String utterance_id) throws SQLException
    {

        System.out.println("--- Remove utterance:" + utterance_id);

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps1 = connection.prepareStatement("delete from utterances_ratings where utterance_id = ?");
              PreparedStatement ps2 = connection.prepareStatement("delete from utterances where utterance_id = ?");
              Statement statement = connection.createStatement()
            ) {

            /* disable autocommit */
            connection.setAutoCommit(false);

            /* clear all utterance to rating links */
            ps1.setString(1, utterance_id);
            ps1.executeUpdate();

            /* delete utterance */
            ps2.setString(1, utterance_id);
            ps2.executeUpdate();

            // apply changes
            connection.commit();

            // TODO: temp
            System.out.println("--- Remaining Utterances:");
            ResultSet rs = statement.executeQuery("select utterances.*, codes.code_name, codes.speaker_id from utterances inner join codes on utterances.code_id = codes.code_id order by utterances.time_marker");

            while (rs.next()) {
                String utt_id = rs.getString("utterance_id");
                String startTime = rs.getString("time_marker");
                String codeName = rs.getString("code_name");
                System.out.println(String.format("%s - %s - %s", utt_id, startTime, codeName));
            }

        }
    }


    /**
     * Insert utterance into datasource
     * @param utterance_id
     * @param code_id
     * @param time_marker
     * @param annotation
     * @throws SQLException
     */
    private void addUtterance(String utterance_id, int code_id, String time_marker, String annotation) throws SQLException
    {

        // TODO: temp
        System.out.println( String.format("--- Add utterance: %s - %s - %s", utterance_id, time_marker, annotation) );
        // TODO: temp

        String sql = "insert into utterances (utterance_id, code_id, time_marker, annotation) values (?,?,?,?)";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setString(1, utterance_id);
            ps.setInt(2, code_id);
            ps.setString(3, time_marker);
            ps.setString(4, annotation);
            ps.executeUpdate();
        }
    }


    private void addRating(String rating_name, int response_value) throws SQLException
    {

        String sql = "select rating_id from ratings where rating_name = ?";

        int rating_id = -1;

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setString(1, rating_name);
            ResultSet rs = ps.executeQuery();

            if( rs.next() ) {
                rating_id = rs.getInt("rating_id");
            }
        }


        if( rating_id == -1 ){

            sql = "insert into ratings (rating_name, response_value) values (?,?)";

            try ( Connection connection = ds.getConnection();
                  PreparedStatement ps = connection.prepareStatement(sql) )
            {
                ps.setString(1, rating_name);
                ps.setInt(2, response_value);
                ps.executeUpdate();
            }
        } else {
          setRatingResponseValue(rating_id, response_value);
        }
    }



    /**
     * Set global rating value in datasource
     * @param rating_id
     * @param response_value
     * @throws SQLException
     */
    private void setRatingResponseValue(int rating_id, int response_value) throws SQLException
    {
        String sql = "update ratings set response_value = ? where rating_id = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql);
        )
        {
            ps.setInt(1, response_value);
            ps.setInt(2, rating_id);
            ps.executeUpdate();
        }
    }


    /**
     * Initialize datasource schema and default values
     * Codes and their defaults initialized from UserConfig
     * @throws SQLException
     */
    private void initDB() throws SQLException
    {

        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement() )
        {

            /*
            Create schema
             */
            statement.executeUpdate("create table if not exists speakers ( " +
                    "speaker_id integer primary key not null, " +
                    "speaker_name string not null unique" +
                    ")");
            statement.executeUpdate("create table if not exists codes ( " +
                    "code_id integer primary key not null, " +
                    "code_name string not null unique, " +
                    "speaker_id integer, " +
                    "  foreign key (speaker_id) references speakers (speaker_id)" +
                    ")");
            /* utterance_id needs to be 'TEXT' to preserve leading zeros used as id in node graph */
            statement.executeUpdate("create table if not exists utterances ( " +
                    "utterance_id TEXT primary key not null unique, " +
                    "time_marker string not null, " +
                    "code_id integer, " +
                    "annotation string," +
                    "  foreign key (code_id) references codes (code_id)" +
                    ")");
            statement.executeUpdate("create table if not exists ratings ( " +
                    "rating_id integer primary key not null, " +
                    "rating_name string not null unique," +
                    "response_value integer not null" +
                    ")");
            statement.executeUpdate("create table if not exists utterances_ratings ( " +
                    "utterance_id TEXT," +
                    "rating_id integer, " +
                    "  foreign key (utterance_id) references utterances (utterance_id)," +
                    "  foreign key (rating_id) references ratings (rating_id)" +
                    ")");
            statement.executeUpdate("create table if not exists attributes ( name, value )");
            // assumes data file does not exists
            statement.executeUpdate("insert into attributes ( name, value ) values ( '" + SessionAttributes.AUDIO_FILE_PATH + "', '" + this.audioFilePath + "')");
            statement.executeUpdate("insert into attributes ( name, value ) values ('" + SessionAttributes.GLOBAL_NOTES + "', '')");


            /*
            Populate speakers table
             */
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("insert into speakers (speaker_id, speaker_name) values (?,?)");
            for (MiscCode.Speaker speaker : MiscCode.Speaker.values() ) {
                ps.setInt(1, speaker.ordinal());
                ps.setString(2, speaker.name());
                ps.addBatch();
            }
            ps.executeBatch();
            // apply changes
            connection.commit();



            /*
            Populate MiscCode table
            add codes from user environment
             */
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("insert into codes (code_id, code_name, speaker_id) values (?,?,?)");
            ListIterator<MiscCode> miscCodeListIterator = MiscCode.getIterator();
            while(miscCodeListIterator.hasNext()) {
                MiscCode code = miscCodeListIterator.next();
                ps.setInt(1, code.value);
                ps.setString(2, code.name);
                ps.setInt(3, code.getSpeaker().ordinal());
                ps.addBatch();
            }
            //
            ps.executeBatch();
            // apply changes
            connection.commit();


            /*
            Populate Global Ratings table
            add codes from user environment
             */
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("insert into ratings (rating_id, rating_name, response_value) values (?,?,?)");
            ListIterator<GlobalCode> globalCodeListIterator = GlobalCode.getIterator();
            while(globalCodeListIterator.hasNext()) {
                GlobalCode rating = globalCodeListIterator.next();
                ps.setInt(1, rating.id);
                ps.setString(2, rating.name);
                ps.setInt(3, rating.defaultRating);
                ps.addBatch();
            }
            //
            ps.executeBatch();
            // apply changes
            connection.commit();

        }
    }

    public boolean sessionFileExists() {
        return sessionFile.canRead();
    }










    /**
     * Session Utterance Markers
     */
    public class UtteranceList {

        /*
         * A sortedMap of utterances, sorted by id which should be a string representation of start time
         */
        private SortedMap< String, Utterance > utteranceTreeMap = new TreeMap<>();
        private ObservableMap<String, Utterance> observableMap;

        /**
         * Constructor initializes utterance map
         * @throws SQLException
         */
        private UtteranceList() throws SQLException {
            utteranceTreeMap.putAll(getUtterances());
            observableMap = FXCollections.observableMap(utteranceTreeMap);
        }

        /**
         * @return observable version of utterance map for listeners
         */
        public ObservableMap getObservableMap() {
            return observableMap;
        }


        /**
         * Add new utterance
         * @param utr
         * @throws SQLException
         */
        public void add( Utterance utr ) throws SQLException {
            // update local map
            observableMap.put( Utils.formatID(utr.getStartTime(), utr.getMiscCode().value), utr);
            // update persistence
            addUtterance(utr.getID(), utr.getMiscCode().value, Utils.formatDuration(utr.getStartTime()), "");
        }


        /**
         * Remove last utterance, if list is non-empty.
         */
        public void removeLast() throws SQLException{
            if( !utteranceTreeMap.isEmpty() ) {
                // update map
                observableMap.remove(utteranceTreeMap.lastKey());
                // update persistence
                Utterance utr = utteranceTreeMap.get(utteranceTreeMap.lastKey());
                removeUtterance(utr.getID());
            }
        }

        /**
         * Remove utterance from list
         * @param utr
         * @throws SQLException
         */
        public void remove(Utterance utr) throws SQLException {
            observableMap.remove(utr.getID());
            removeUtterance(utr.getID());
        }


        public void remove(String ID) throws SQLException {
            observableMap.remove(ID);
            removeUtterance(ID);
        }


        /**
         * Return utterance with given id
         * @return the utterance or null
         */
        public Utterance get(String utteranceID){
            return utteranceTreeMap.get(utteranceID);
        }

        /**
         * Get last utterance (coded or not), or null if list is empty.
         */
        public Utterance last() {
            return utteranceTreeMap.isEmpty() ? null : utteranceTreeMap.get(utteranceTreeMap.lastKey());
        }

        /**
         * @return utterance map values
         */
        public Collection<Utterance> values() {
            return utteranceTreeMap.values();
        }


        public int size(){
            return utteranceTreeMap.size();
        }

        public boolean isEmpty(){
            return utteranceTreeMap.isEmpty();
        }

    }




    /**
     * Global Session Ratings
     */
    public class Ratings {

        // list for all ratings
        private HashMap< Integer, Integer > ratings;
        // notes field for ratings overall. Could just be session notes at this point.
        private String notes;


        /**
         * @throws SQLException
         */
        private Ratings() throws SQLException {
            ratings = getRatingsMap();
            notes = getAttribute(SessionAttributes.GLOBAL_NOTES);
        }


        /**
         * @param code
         * @return rating for one of the global questions
         */
        public int getRating( GlobalCode code ) {
            return ratings.get(code.id);
        }


        /**
         * @param code
         * @param rating
         * @throws SQLException
         */
        public void	setRating( GlobalCode code, int rating ) throws SQLException {
            ratings.put( code.id, rating );
            setRatingResponseValue( code.id, rating);
        }


        /**
         * @param code
         * @param rating
         * @throws SQLException
         */
        public void	setRating( GlobalCode code, String rating ) throws SQLException {
            int rating_value = Integer.parseInt(rating);
            this.setRating(code, rating_value);
        }


        /**
         * @param notes
         * @throws SQLException
         */
        public void setNotes(String notes) throws SQLException {
            this.notes = notes;
            setAttribute(SessionAttributes.GLOBAL_NOTES, notes);
        }


        /**
         * @return
         */
        public String getNotes() {
            return notes;
        }

    }


    /**
     * Grouping of static methods that support backwards compatibility in session data storage and could be removed in the future.
     * Might just be moved into SessionData
     */
    public static class Compatibility {

        /**
         * Separate function for reading audio filename from code file
         * @param casaaFileTextFormat casaa file
         * @return filenameAudio
         */
        public static String getAudioFilename( File casaaFileTextFormat ) throws IOException {

            Scanner in;
            try {
                in = new Scanner(casaaFileTextFormat);
            } catch (FileNotFoundException e) {
                throw e;
            }

            if( !in.hasNext() ){
                throw new IOException("No Audio File Listed in casaa file");
            }

            // Get the audio filename line.
            String 			filenameAudio 	= in.nextLine();
            StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

            headReader.nextToken(); // Eat line heading "Audio Filename:"
            filenameAudio = headReader.nextToken();
            if( (filenameAudio.trim()).equalsIgnoreCase("") ){
                throw new IOException("No Audio File Listed in casaa file");
            }

            return filenameAudio;
        }



        /**
         * Convert old file format to new
         * @param sessionFileTextFormat
         * @throws IOException
         */
        static public SessionData sessionDataFromPreviousFileFormat(File sessionFileTextFormat) throws IOException {

            /* move old format casaa file to same name with "*.casaa.bak" or "*.casaa.txt" */
            String backupFilePath = sessionFileTextFormat.getAbsolutePath().replace("casaa", "casaa.txt.bak");
            File backupSessionFile = new File(backupFilePath);
            /* if a backup file by that name already exists we will assume it doesn't need repeating */
            if( !backupSessionFile.exists() ) {
                Files.copy(sessionFileTextFormat.toPath(), backupSessionFile.toPath());
            }

            /* now delete old session file so a new session can be created */
            Files.delete(sessionFileTextFormat.toPath());

            /* new session file */
            File sessionFile = new File(sessionFileTextFormat.getAbsolutePath());
            /* get audioFile from old casaa file format */
            File audioFile = new File(Compatibility.getAudioFilename(backupSessionFile));

            /* initialize session data */
            SessionData sessionData = new SessionData(sessionFile, audioFile);

            /* load utterances from old text format into new session data */
            List<Utterance> utteranceList = utteranceListFromPreviousFileFormat(backupSessionFile);

            try {
                sessionData.setUtteranceList(utteranceList);
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }

            return sessionData;

        }


        /**
         * Convert old file format to new. Include globals file merge
         * @param sessionFile
         * @param globalsFile
         * @throws IOException
         */
        static public SessionData sessionDataFromPreviousFileFormat(File sessionFile, File globalsFile) throws IOException {

            SessionData sessionData = sessionDataFromPreviousFileFormat(sessionFile);

            /* import globals file */
            HashMap<String, Integer> globalsList = globalRatingsListFromPreviousFileFormat(globalsFile);

            try {
                sessionData.setRatingsList(globalsList);
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }

            String globalsNotes = globalNotesListFromPreviousFileFormat(globalsFile);
            try {
                sessionData.ratingsList.setNotes(globalsNotes);
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }

            return sessionData;
        }


        /**
         * @param sessionFileTextFormat
         * @return list of utterances loaded from the text file format
         * @throws IOException
         */
        public static List<Utterance> utteranceListFromPreviousFileFormat(File sessionFileTextFormat) throws IOException {

            List<Utterance> utteranceList = new ArrayList<>();

            Scanner in;

            try {
                in = new Scanner(sessionFileTextFormat);
            } catch (FileNotFoundException e) {
                throw e;
            }

            // get the audio filename line.
            String 			filenameAudio 	= in.nextLine();
            StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

            // Eat  "Audio Filename:"
            headReader.nextToken();

            while( in.hasNextLine() ){

                String 			nextStr 	= in.nextLine();
                StringTokenizer st 			= new StringTokenizer(nextStr, "\t");
                int 			lineSize 	= st.countTokens();

                /* new data format */
                if( lineSize == 3 ){

                    Duration startTime  = Utils.parseDuration(st.nextToken());
                    int codeId          = Integer.parseInt( st.nextToken() );
                    String codeName     = st.nextToken();
                    MiscDataItem item 	= new MiscDataItem(Utils.formatID(startTime,codeId), startTime);

                    // look up parsed code in user config codes loaded at initDB
                    try {
                        item.setMiscCodeByValue(codeId);
                    } catch (Exception e) {
                        // if lookup failed there is a possible disconnect between codes in casaa file
                        // and codes in user config file
                        throw new IOException( String.format("The code (%s) with value (%d) in file (%s) was not found in the current user configuration file.\n\nIf you uncode (%s) you will not be able to recode it with the current config file.", codeName, codeId, sessionFileTextFormat.getName(), codeName ) );
                    }

                    utteranceList.add(item);

                }
                /* read 7 to handle old data format */
                else if( lineSize == 7 ) {

                    /* throw away useless index number, start time*/
                    st.nextToken();
                    st.nextToken();
                    /* start time */
                    Duration startTime  = Utils.parseDuration(st.nextToken());

                    /* skip time zero utterances from this format */
                    if(!startTime.equals(Duration.ZERO)) {

                        /* throw away useless, byte data */
                        st.nextToken();
                        st.nextToken();

                        int codeId = Integer.parseInt(st.nextToken());
                        MiscDataItem item = new MiscDataItem(Utils.formatID(startTime, codeId), startTime);

                        // look up parsed code in user config codes loaded at initDB
                        try {
                            item.setMiscCodeByValue(codeId);
                        } catch (Exception e) {
                            // if lookup failed there is a possible disconnect between codes in casaa file
                            // and codes in user config file
                            throw new IOException(String.format("Code(%d) in casaa file not found in user configuration file", codeId));
                        }
                        st.nextToken(); // throw away the code string

                        utteranceList.add(item);
                    }
                }
            }

            return utteranceList;
        }


        /**
         * @param globalsFileTextFormat
         * @return Map of global rating items parsed from text file format
         * @throws IOException
         */
        public static HashMap<String, Integer> globalRatingsListFromPreviousFileFormat(File globalsFileTextFormat) throws IOException {

            HashMap<String, Integer> ratingList = new HashMap<>();

            Scanner in;

            try {
                in = new Scanner(globalsFileTextFormat);
            } catch (FileNotFoundException e) {
                throw e;
            }

            // get the audio filename line.
            String 			filenameAudio 	= in.nextLine();
            StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

            // Eat  "Audio Filename:"
            headReader.nextToken();
            in.nextLine();

            while( in.hasNextLine() ){

                String 			nextStr 	= in.nextLine();
                StringTokenizer st 			= new StringTokenizer(nextStr, "\t");
                int 			lineSize 	= st.countTokens();

                if( lineSize == 2 ){
                    /* global */
                    String rating_name = st.nextToken().toUpperCase().replace(":","");
                    if( !rating_name.equalsIgnoreCase("NOTES") ) {
                        int response_value = Integer.parseInt( st.nextToken() );
                        ratingList.put(rating_name, response_value);
                    }
                }
            }

            return ratingList;
        }


        /**
         * @param globalsFileTextFormat
         * @return Notes text parsed from text format file
         * @throws IOException
         */
        public static String globalNotesListFromPreviousFileFormat(File globalsFileTextFormat) throws IOException {

            String notes = "";

            Scanner in;

            try {
                in = new Scanner(globalsFileTextFormat);
            } catch (FileNotFoundException e) {
                throw e;
            }

            // get the audio filename line.
            String 			filenameAudio 	= in.nextLine();
            StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

            // Eat  "Audio Filename:"
            headReader.nextToken();
            in.nextLine();

            while( in.hasNextLine() ){

                String 			nextStr 	= in.nextLine();
                StringTokenizer st 			= new StringTokenizer(nextStr, "\t");
                int 			lineSize 	= st.countTokens();

                if( lineSize == 2 ){
                    String rating_name = st.nextToken().toUpperCase().replace(":","");
                    if( rating_name.equalsIgnoreCase("NOTES") ) {
                        notes = st.nextToken();
                    }
                }
            }
            return notes;
        }


    }
}
