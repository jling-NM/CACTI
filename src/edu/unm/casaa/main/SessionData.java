package edu.unm.casaa.main;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;


public class SessionData
{

    private final String sessionFilePath;
    private final SQLiteDataSource ds;

    public SessionData(String filePath) throws SQLException {

        sessionFilePath =  filePath;

        /*
          datasource config
          - enable foreign key constraints
          - give database a version
         */
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setUserVersion(1);

        ds = new SQLiteDataSource(config);
        ds.setUrl("jdbc:sqlite:" + sessionFilePath);

        /*
          If we have existing datafile don't init db
         */
        if(!sessionFileExists())
            init();

    }


    // TODO: not sure what this needs to do yet
    // join all globals with their linked utterances
    // or just get the utterances linked to a single global
    // TBD
    public ResultSet getGlobalUtterances() throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();

            Statement statement = connection.createStatement();
            return statement.executeQuery("select utterances.*, codes.code_name from utterances inner join codes on utterances.code_id = codes.code_id");
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    // TODO: to plug into app we may want to return something else
    public ResultSet getUtterances() throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();

            Statement statement = connection.createStatement();
            return statement.executeQuery("select utterances.*, codes.code_name from utterances inner join codes on utterances.code_id = codes.code_id");
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public void setAttribute(String name, String value) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("update attributes set value = ? where name = ?");
            ps.setString(1, value);
            ps.setString(2, name);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public void unlinkUtteranceFromGlobal(int utterance_id, int global_id) throws SQLException
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


    public void linkUtteranceToGlobal(int utterance_id, int global_id) throws SQLException
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


    public void annotateUtterance(int utterance_id, String annotation) throws SQLException
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


    public void recodeUtterance(int utterance_id, int code_id) throws SQLException
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



    public void removeUtterance(int utterance_id) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("delete from utterances where utterance_id = ?");
            ps.setInt(1, utterance_id);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public int addUtterance(int code_id, String audio_file_time_marker, String annotation) throws SQLException
    {
        Connection connection = null;

        try
        {
            // create a database connection
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("insert into utterances (code_id, audio_file_time_marker, annotation) values (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, code_id);
            ps.setString(2, audio_file_time_marker);
            ps.setString(3, annotation);
            ps.executeUpdate();
            ResultSet priKeys = ps.getGeneratedKeys();

            int autoIncKey = 0;
            if (priKeys.next()) {
                autoIncKey = priKeys.getInt(1);
            }

            return autoIncKey;
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public void setGlobalResponseValue(int global_id, int response_value) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("update globals set response_value = ? where global_id = ?");
            ps.setInt(1, response_value);
            ps.setInt(2, global_id);
            ps.executeUpdate();
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public int addGlobal(String global_name, int response_value) throws SQLException
    {
        Connection connection = null;

        try
        {
            // create a database connection
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("insert into globals (global_name, response_value) values (?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, global_name);
            ps.setInt(2, response_value);
            ps.executeUpdate();
            ResultSet priKeys = ps.getGeneratedKeys();

            int autoIncKey = 0;
            if (priKeys.next()) {
                autoIncKey = priKeys.getInt(1);
            }

            return autoIncKey;
        } finally
        {
            if(connection != null)
                connection.close();
        }
    }


    public int addCode(String code_name) throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = ds.getConnection();
            PreparedStatement ps = connection.prepareStatement("insert into codes (code_name) values (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code_name);
            ps.executeUpdate();
            ResultSet priKeys = ps.getGeneratedKeys();

            int autoIncKey = 0;
            if (priKeys.next()) {
                autoIncKey = priKeys.getInt(1);
            }

            return autoIncKey;
        }
        finally
        {
            if(connection != null)
                connection.close();
        }
    }


    private void init() throws SQLException
    {
        System.out.println("INIT DB");

        Connection connection = null;

        try
        {
            // create a database connection
            connection = ds.getConnection();
            Statement statement = connection.createStatement();

            statement.executeUpdate("create table if not exists codes ( " +
                    "code_id integer primary key not null, " +
                    "code_name string not null unique" +
                    ")");
            statement.executeUpdate("create table if not exists utterances ( " +
                    "utterance_id integer primary key not null, " +
                    "code_id integer, " +
                    "audio_file_time_marker string not null unique, " +
                    "annotation string," +
                    "  foreign key (code_id) references codes (code_id)" +
                    ")");
            statement.executeUpdate("create table if not exists globals ( " +
                    "global_id integer primary key autoincrement, " +
                    "global_name string not null unique," +
                    "response_value integer not null" +
                    ")");
            statement.executeUpdate("create table if not exists utterances_globals ( " +
                    "utterance_id integer," +
                    "global_id integer, " +
                    "  foreign key (utterance_id) references utterances (utterance_id)," +
                    "  foreign key (global_id) references globals (global_id)" +
                    ")");
            statement.executeUpdate("create table if not exists attributes ( name, value )");
            // assumes data file does not exists
            statement.executeUpdate("insert into attributes ( name, value ) values ('source_audio_file_path', '')");
            statement.executeUpdate("insert into attributes ( name, value ) values ('global_notes', '')");


        } finally
        {
            if(connection != null)
                connection.close();
        }
    }

    public boolean sessionFileExists() {
        File file = new File(sessionFilePath);
        return file.canRead();
    }


}
