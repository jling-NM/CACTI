package edu.unm.casaa.main;


import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.Utterance;
import javafx.collections.FXCollections;
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
    private enum SessionAttributes {
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
    private HashMap< Integer, Integer > getRatings() throws SQLException {

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


    // TODO: not sure what this needs to do yet
    // join all globals with their linked utterances
    // or just get the utterances linked to a single global
    // TBD
    private ResultSet getGlobalUtterances() throws SQLException
    {
        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement()  ) {
            return statement.executeQuery("select utterances.*, codes.code_name from utterances inner join codes on utterances.code_id = codes.code_id");
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


    /**
     * @param sessionAttribute SessionAttribute to get
     * @return current value for session attribute
     * @throws SQLException
     */
    private String getAttribute(SessionAttributes sessionAttribute) throws SQLException
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


    private void unlinkUtteranceFromGlobal(int utterance_id, int global_id) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("delete from utterances_globals where utterance_id = ? and global_id = ?");
            ps.setInt(1, utterance_id);
            ps.setInt(2, global_id);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    private void linkUtteranceToGlobal(int utterance_id, int global_id) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("insert into utterances_globals (utterance_id, global_id) values (?,?)");
            ps.setInt(1, utterance_id);
            ps.setInt(2, global_id);
            ps.executeUpdate();

        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    private void annotateUtterance(int utterance_id, String annotation) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("update utterances set annotation = ? where utterance_id = ?");
            ps.setString(1, annotation);
            ps.setInt(2, utterance_id);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    private void recodeUtterance(int utterance_id, int code_id) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("update utterances set code_id = ? where utterance_id = ?");
            ps.setInt(1, code_id);
            ps.setInt(2, utterance_id);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    /**
     * Remove utterance from datasource
     * @param utterance_id
     * @throws SQLException
     */
    private void removeUtterance(String utterance_id) throws SQLException
    {
        String sql = "delete from utterances where utterance_id = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setString(1, utterance_id);
            ps.executeUpdate();
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
                    "utterance_id integer," +
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
            ratings = getRatings();
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
