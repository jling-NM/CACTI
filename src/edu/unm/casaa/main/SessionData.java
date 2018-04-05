package edu.unm.casaa.main;


import java.io.*;
import java.sql.*;
import java.util.*;

import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.utterance.Utterance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.util.Duration;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;




public class SessionData
{

    private File sessionFile;
    private final SQLiteDataSource ds;
    private String audioFilePath = "";

    // TODO: possible
    public UtteranceList utteranceList;
    public Ratings ratingsList;


    /**
     * Common initDB code
     */
    private SessionData () {
        /*
          datasource config
          - enable foreign key constraints
          - give database a version
         */
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setUserVersion(1);

        ds = new SQLiteDataSource(config);
    }


    /**
     * Initialize new session data
     * @param storageFile
     * @param audioFile
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
     * @param storageFile
     * @throws IOException
     */
    public SessionData(File storageFile) throws IOException {

        this();
        sessionFile = storageFile;
        ds.setUrl("jdbc:sqlite:" + sessionFile.getAbsolutePath());

        if( sessionFileExists() ) {

            /*
            load audio file path from existing db
             */
            if( isSQLiteDataFile() ) {
                try {
                    // TODO: this take a long time. WHY?
                    audioFilePath = getAttribute("source_audio_file_path");
                    utteranceList = new SessionData.UtteranceList();
                    ratingsList = new SessionData.Ratings();
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            } else {

                // can remove this or replace with file conversion dialog or automated conversion below
                throw new IOException( "File is not correct format:\n"+sessionFile.getAbsolutePath() );
            }
        } else {
            throw new IOException("File does not exist.");
        }
    }



    /**
     * TBD
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

        try ( Connection connection = ds.getConnection();
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



    private SortedMap< Integer, Utterance > getUtterances() throws SQLException
    {
        SortedMap< Integer, Utterance > utteranceTreeMap = new TreeMap<>();

        try ( Connection connection = ds.getConnection();
              Statement statement = connection.createStatement() ) {

            ResultSet rs = statement.executeQuery("select utterances.*, codes.code_name, codes.speaker_id from utterances inner join codes on utterances.code_id = codes.code_id order by utterances.utterance_id");

            while (rs.next()) {

                int utterance_id = rs.getInt("utterance_id");
                Duration startTime = Utils.parseDuration(rs.getString("audio_file_time_marker"));
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



    private void setAttribute(String name, String value) throws SQLException
    {
        String sql = "update attributes set value = ? where name = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql)  )
        {
            ps.setString(1, value);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }


    public String getAudioFilePath() {
        return this.audioFilePath;
    }


    public String getSessionFilePath() {
        return this.sessionFile.getAbsolutePath();
    }


    private String getAttribute(String name) throws SQLException
    {
        String sql = "select value from attributes where name = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setString(1, name);
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



    private void removeUtterance(int utterance_id) throws SQLException
    {
        String sql = "delete from utterances where utterance_id = ?";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setInt(1, utterance_id);
            ps.executeUpdate();
        }
    }


    private void addUtterance(int utterance_id, int code_id, String audio_file_time_marker, String annotation) throws SQLException
    {
        String sql = "insert into utterances (utterance_id, code_id, audio_file_time_marker, annotation) values (?,?,?,?)";

        try ( Connection connection = ds.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql) )
        {
            ps.setInt(1, utterance_id);
            ps.setInt(2, code_id);
            ps.setString(3, audio_file_time_marker);
            ps.setString(4, annotation);
            ps.executeUpdate();
        }
    }


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
            statement.executeUpdate("create table if not exists utterances ( " +
                    "utterance_id integer primary key not null, " +
                    "code_id integer, " +
                    "audio_file_time_marker string not null unique, " +
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
            statement.executeUpdate("insert into attributes ( name, value ) values ('source_audio_file_path', '" + this.audioFilePath + "')");
            statement.executeUpdate("insert into attributes ( name, value ) values ('rating_notes', '')");


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







    //
    //Nested Session Data classes
    //

    /**
     * A sortedMap of utterances, sorted by id which should be a string representation of start time
     */
    public class UtteranceList {

        private SortedMap< Integer, Utterance > utteranceTreeMap = new TreeMap<>();
        private ObservableMap<Integer, Utterance> observableMap;

        /**
         * @throws SQLException
         */
        private UtteranceList() throws SQLException {
            utteranceTreeMap.putAll(getUtterances());
            observableMap = FXCollections.observableMap(utteranceTreeMap);
        }

        /**
         *
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
            // update persistance
            addUtterance(utr.getID(), utr.getMiscCode().value, Utils.formatDuration(utr.getStartTime()), "");
        }


        /**
         * Remove last utterance, if list is non-empty.
         */
        public void removeLast() throws SQLException{
            if( !utteranceTreeMap.isEmpty() ) {
                // update map
                observableMap.remove(utteranceTreeMap.lastKey());
                // update persistance
                Utterance utr = utteranceTreeMap.get(utteranceTreeMap.lastKey());
                removeUtterance(utr.getID());
            }
        }

        /**
         * Remove utterance
         * @param utr
         * @throws SQLException
         */
        public void remove(Utterance utr) throws SQLException {
            observableMap.remove(utr.getID());
            removeUtterance(utr.getID());
        }

        public void remove(int ID) throws SQLException {
            observableMap.remove(ID);
            removeUtterance(ID);
        }

        public void remove(String ID) throws SQLException {
            this.remove(Integer.parseInt(ID));
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



    private class Compatibility {

        /**
         * This will load data from old, text-based format into new sql data file
         * @param MISCfile casaa file
         * @throws Exception
         */
        public void loadFromFile(File MISCfile ) throws Exception {

            SessionData.UtteranceList utteranceList = new UtteranceList();

            Scanner in;

            try {
                in = new Scanner(MISCfile);
            } catch (FileNotFoundException e) {
                throw e;
            }

            //////utteranceList.storageFile = MISCfile;

            // get the audio filename line.
            String 			filenameAudio 	= in.nextLine();
            StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

            // Eat  "Audio Filename:"
            headReader.nextToken();
            // local reference of audiofilename
            //////utteranceList.audioFilename = headReader.nextToken();

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
                        throw new Exception( String.format("The code (%s) with value (%d) in file (%s) was not found in the current user configuration file.\n\nIf you uncode (%s) you will not be able to recode it with the current config file.", codeName, codeId, MISCfile.getName(), codeName ) );
                    }
                    //st.nextToken(); // throw away the code string

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
                            throw new Exception(String.format("Code(%d) in casaa file not found in user configuration file", codeId));
                        }
                        st.nextToken(); // throw away the code string

                        utteranceList.add(item);
                    }
                }
            }

        }
    }







    public class Ratings {

        //private HashMap< Integer, Integer > ratings = new HashMap< Integer, Integer >();
        private HashMap< Integer, Integer > ratings;
        private String notes = "";

        private Ratings() throws SQLException {
            ratings = getRatings();
            notes = getAttribute("rating_notes");
        }

        public int getRating( GlobalCode code ) {
            return ratings.get(code.id);
        }

        public void	setRating( GlobalCode code, int rating ) throws SQLException {
            ratings.put( code.id, rating );
            setRatingResponseValue( code.id, rating);
        }

        public void	setRating( GlobalCode code, String rating ) throws SQLException {
            int rating_value = Integer.parseInt(rating);
            this.setRating(code, rating_value);
        }

        public void setNotes(String notes) throws SQLException {
            this.notes = notes;
            setAttribute("rating_notes", notes);
        }

        public String getNotes() {
            return notes;
        }

    }
}
