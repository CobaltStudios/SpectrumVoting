package net.spectrumnation;

import net.spectrumnation.CoreController;

import java.net.ConnectException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Created by sirtidez on 10/31/15.
 */
public class DatabaseUtil {

    public static class DataObject {
        private Object data;
        private String key;

        public DataObject(String key, Object data) {
            this.data = data;
            this.key = key;
        }

        public Object getData() {
            return data;
        }

        public String getKey() {
            return key;
        }
    }

    //TODO: Create Condition class to handle object insertion
    public static abstract class DatabaseCommon {

        protected Connection con = null;

        @Deprecated
        public ResultSet query(String query) throws SQLException {
            return this.con.createStatement().executeQuery(query);
        }

        public ResultSet query(String query, Condition... conditions) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(Condition c : conditions) {
                    st.setObject(i, c.key);
                    i++;
                }

                return st.executeQuery();
            } catch(SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Deprecated
        public int update(String query) throws SQLException {
            return this.con.createStatement().executeUpdate(query);
        }

        public int update(String query, Condition... conditions) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(Condition c : conditions) {
                    st.setObject(i, c.key);
                    i++;
                }

                return st.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return -1;
        }

        @SuppressWarnings("unchecked")
        public int update(String query, ArrayList<? extends Object>... objects) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(ArrayList<? extends Object> array : objects) {
                    for(Object o : array) {
                        st.setObject(i, o);
                        i++;
                    }
                }

                return st.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return -1;
        }

        public Connection getConnection() {
            return this.con;
        }

        public abstract void close();
        public abstract boolean checkTable(String table_name);
    }

    public static class MySQLUtil extends DatabaseCommon {
        private String HOST = "";
        private String USER = "";
        private String PASS = "";
        private String DATABASE = "";
        private String PORT = "";

        public MySQLUtil(String host, String user, String pass, String database, String port) {
            HOST = host;
            USER = user;
            PASS = pass;
            DATABASE = database;
            PORT = port;
        }

        public Connection open(boolean print) throws SQLException, ConnectException {
            Properties connectionProperties = new Properties();
            connectionProperties.put("user", USER);
            connectionProperties.put("password", PASS);

            if(print) CoreController.getLogger().info("Trying to connect to " + DATABASE + " with user: " + USER);
            this.con = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, connectionProperties);

            if(print) {
                if(this.con == null)
                    CoreController.getLogger().log(Level.WARNING, "Database connection failed!");
                else
                    CoreController.getLogger().info("Database connection succeeded!");
            }

            return this.con;
        }

        public boolean checkTable(String tablename) {
            try {
                ResultSet count = query("SELECT count(*) FROM information_schema.TABLES WHERE (TABLE_SCHEMA = '" + DATABASE + "') AND (TABLE_NAME = '" + tablename + "');");
                byte i = 0;
                if (count.next()) {
                    i = count.getByte(1);
                }
                count.close();
                return i == 1;
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return false;
        }

        public void close() {
            try {
                this.con.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        public boolean checkConnection() {
            if(con == null)     return false;
            try {
                ResultSet count = query("SELECT count(*) FROM information_schema.SHEMATA");
                boolean give = count.first();
                count.close();
                return give;
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    public static class SQLiteUtil extends DatabaseCommon {

        public Connection open(String path, boolean print) {
            try {
                Class.forName("org.sqlite.JDBC").newInstance();
                con = DriverManager.getConnection("jdbc:sqlite:" + path);
                if(print) {
                    if(con == null) {
                        CoreController.getLogger().log(Level.WARNING, "Database connection failed!");
                    } else {
                        CoreController.getLogger().info("Database connection succeded!");
                    }
                }
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            } catch(InstantiationException e) {
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return con;
        }

        public boolean checkTable(String tablename) {
            if(con == null)     return false;
            String command = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tablename + "'";
            byte i = 0;
            try {
                ResultSet count = query(command);
                if(count.next())
                    i = count.getByte(1);
                count.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return (i == 1);
        }

        public void close() {
            try {
                con.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Condition {
        public String key;
        public String column_key;

        public Condition(String column_key, String key) {
            this.key = key;
            this.column_key = column_key;
        }
    }

    public enum ObjectType {
        INTEGER,
        BIGINT,
        CHARACTER,
        VARCHAR,
        BOOLEAN,
        FLOAT,
        DECIMAL,
        ARRAY,
        DATE,
        TIME,
        NONE,
        TIMESTAMP;
    }

    public enum DatabaseType {
        MYSQL,
        SQLITE, DatabaseType;
    }
}
