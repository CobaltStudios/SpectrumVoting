package net.spectrumnation.database;


import net.spectrumnation.DatabaseUtil;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sirtidez on 11/1/15.
 */
public class DatabaseHandler {
    private String path;
    private String table;
    private DatabaseUtil.DatabaseType databaseType;
    private String primaryKey;

    // MySQL
    private String HOST = "";
    private String USER = "";
    private String PASS = "";
    private String DATABASE = "";
    private String PORT = "";

    private boolean init = false;
    private int constructorUsed;

    // MySQL
    public DatabaseHandler(DatabaseUtil.DatabaseType databaseType, String tableName) {
        this.databaseType = databaseType;
        this.table = tableName;
        this.constructorUsed = 0;
    }

    // SQLite
    public DatabaseHandler(DatabaseUtil.DatabaseType databaseType, String tableName, String path) {
        this.databaseType = databaseType;
        this.table = tableName;
        this.path = path;
        this.constructorUsed = 1;
    }

    public boolean init(String host, String user, String pass, String database, String port, String primaryKey) {
        this.HOST = host;
        this.USER = user;
        this.PASS = pass;
        this.DATABASE = database;
        this.PORT = port;
        this.primaryKey = primaryKey;
        if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
            if(constructorUsed != 0) {
                new IllegalAccessError("Improper constructor used for MySQL database use!").printStackTrace();
                return false;
            }
            if(mysqlIsIinstalled()) {
                if (createTable()) {
                    if(!mysqlIsUsable()) {
                        return false;
                    } else {
                        init = true;
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
            if(constructorUsed == 1) {
                if (path == null) {
                    new NullPointerException("The path cannot be null!").printStackTrace();
                    return false;
                }
            } else {
                new IllegalAccessError("Improper constructor used for SQLite database use!").printStackTrace();
                return false;
            }

            if(sqliteIsInstalled()) {
                if(createTable()) {
                    if(!sqliteUsable()) {
                        return false;
                    } else {
                        init = true;
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
            }
        } else {
            new NullPointerException("The DatabaseType can't be null!").printStackTrace();
            return false;
        }

        return false;
    }

    public List<Object> getValues(String data, DatabaseUtil.Condition... conditions) {
        if(init) {
            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                return getValueMySQL(data, conditions);
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                return getValueSQLite(data, conditions);
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initiated yet!").printStackTrace();
            return null;
        }
        return null;
    }

    public void addColumn(String column, DatabaseUtil.ObjectType type) {
        if(init) {
            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                addColumnMySQL(column, type);
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                addColumnSQLite(column, type);
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initialized yet").printStackTrace();
        }
        return;
    }

    private String getValueCommandStringSet(ArrayList<String> keys, ArrayList<Object> values) {
        String main = "";
        if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
            main = "REPLACE INTO `" + table + "` (";
        } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
            main = "INSERT OR REPLACE INTO `" + table + "` (";
        }

        for(String key: keys) {
            if(main.endsWith("`")) {
                main = main + ", `" + key + "`";
            } else {
                main = main + "`" + key + "`";
            }
        }

        main = main + ") VALUES (";
        for(int i = 0; i < values.size(); i++) {
            if(main.endsWith("?")) {
                main = main + ", ?";
            } else {
                main = main + "?";
            }
        }

        main = main + ");";
        return main;
    }

    private List<String> getValueCommandStringUpdate(ArrayList<String> keys, ArrayList<Object> values, DatabaseUtil.Condition... conditions) {
        List<String> commands = new LinkedList<String>();
        List<Object> listObjectGet = getValues(primaryKey, conditions);
        if(listObjectGet.size() == 0) {
            commands.add(getValueCommandStringSet(keys, values));
            return commands;
        } else {
            for(Object actualPrimaryKey : listObjectGet) {
                String main = "";
                if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                    main = main + "REPLACE INTO `" + table + "` (";
                } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                    main = main + "INSERT OR REPLACE INTO `" + table + "` (";
                }
                main = main + ", `" + primaryKey + "`";

                for(String key : keys) {
                    main = main + ", `" + key + "`";
                }

                main = main + ") VALUES (" + actualPrimaryKey.toString();

                for(int i = 0; i < values.size(); i++) {
                    main = main + ", ?";
                }
                main = main + ")";
                commands.add(main);
            }
        }

        return commands;
    }

    public void insertValueForce(List<DatabaseUtil.DataObject> objects) {
        DatabaseUtil.DataObject[] array = objects.toArray(new DatabaseUtil.DataObject[objects.size()]);
        insertValueForce(array);
    }

    @SuppressWarnings("unchecked")
    public void insertValueForce(DatabaseUtil.DataObject[] objects) {
        if(init) {
            ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
            ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
            for(DatabaseUtil.DataObject d : objects) {
                keys.add(d.getKey());
                values.add(d.getData());
            }

            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                try {
                    db.open(false);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
                try {
                    String command = getValueCommandStringSet(keys, values);
                    db.open(path, false);
                    db.update(command, values);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else {
                new NullPointerException().printStackTrace();
                return;
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
        }

        return;
    }

    public void insertOrUpdateValue(List<DatabaseUtil.DataObject> objects, DatabaseUtil.Condition... conditions) {
        DatabaseUtil.DataObject[] array = objects.toArray(new DatabaseUtil.DataObject[objects.size()]);
        insertOrUpdateValue(array, conditions);
    }

    public void insertOrUpdateValue(DatabaseUtil.DataObject[] objects, DatabaseUtil.Condition... conditions) {
        if(init) {
            ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
            ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
            for(DatabaseUtil.DataObject d : objects) {
                keys.add(d.getKey());
                values.add(d.getData());
            }

            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                try {
                    db.open(false);
                    List<String> allCommands = getValueCommandStringUpdate(keys, values, conditions);
                    for(String command : allCommands) {
                        db.update(command, values);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ConnectException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
                try {
                    List<String> allCommands = getValueCommandStringUpdate(keys, values, conditions);
                    db.open(path, false);
                    for(String command : allCommands) {
                        db.update(command, values);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else {
                new NullPointerException("The database type must not be null!").printStackTrace();
                return;
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
            return;
        }
    }

    public boolean existInTable(DatabaseUtil.Condition... conditions) {
        return (numberObjectsInTable(conditions) > 0);
    }

    public int numberObjectsInTable(DatabaseUtil.Condition... conditions) {
        if(init) {
            String command = null;
            for(DatabaseUtil.Condition c : conditions) {
                if(command == null) {
                    command = "SELECT count(*) FROM" + table + "WHERE " + c.column_key + "=? ";
                } else {
                    command = command + "AND " +c.column_key + "=? ";
                }
            }

            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                try {
                    db.open(false);
                    ResultSet result = db.query(command, conditions);
                    byte i = 0;
                    if(result.next()) {
                        i = result.getByte(1);
                    }
                    result.close();
                    return i;
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ConnectException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                DatabaseUtil.SQLiteUtil db  = new DatabaseUtil.SQLiteUtil();
                try {
                    db.open(path, false);
                    ResultSet result = db.query(command, conditions);
                    byte i = 0;
                    if(result.next()) {
                        i = result.getByte(1);
                    }
                    result.close();
                    return i;
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else {
                new NullPointerException().printStackTrace();
                return 0;
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
            return 0;
        }

        return 0;
    }

    private boolean createTable() {
        if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
            return createTableMySQL();
        } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
            return createTableSQLite();
        } else {
            new NullPointerException().printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public void clearTable() {
        if(init) {
            String command_clear = "DELETE FROM " + table;
            String command_reset;
            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                command_reset = "ALTER TABLE " + table + " AUTO_INCREMENT = 1";
            } else if(databaseType.equals((DatabaseUtil.DatabaseType.SQLITE))) {
                command_reset = "DELETE FROM sqlite_sequence WHERE name='" + table + "'";
            } else {
                return;
            }

            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                try {
                    db.open(false);
                    db.update(command_clear);
                    db.update(command_reset);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ConnectException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
                try {
                    db.open(path, false);
                    db.update(command_clear);
                    db.update(command_reset);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else {
                new NullPointerException().printStackTrace();
                return;
            }
        } else {
            new IllegalStateException("The DatabaseHandler wan't initiated!").printStackTrace();
            return;
        }
    }

    private String getValueCommandStringDelete(DatabaseUtil.Condition... conditions) {
        String result = "DELETE FROM `" + table + "` WHERE ";
        for(int i = 0; i < conditions.length; i++) {
            DatabaseUtil.Condition c = conditions[i];
            if(conditions.length > (i + 1)) {
                result = result + c.column_key + "=? AND ";
            } else {
                result = result + c.column_key + "=?";
            }
        }

        return result;
    }

    public void deleteObject(DatabaseUtil.Condition... conditions) {
        if(init) {
            if(databaseType.equals(DatabaseUtil.DatabaseType.MYSQL)) {
                DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                try {
                    db.open(false);
                    String command = getValueCommandStringDelete(conditions);
                    db.update(command, conditions);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ConnectException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else if(databaseType.equals(DatabaseUtil.DatabaseType.SQLITE)) {
                DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
                try {
                    String command = getValueCommandStringDelete(conditions);
                    db.open(path, false);
                    db.update(command, conditions);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
            } else {
                new NullPointerException().printStackTrace();
                return;
            }
        } else {
            new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
            return;
        }
    }

    private boolean mysqlIsIinstalled() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean mysqlIsUsable() {
        DatabaseUtil.MySQLUtil db = null;
        try {
            db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
            db.open(true);
            if(db.checkTable(table)) {
                db.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return false;
    }

    private boolean sqliteIsInstalled() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean sqliteUsable() {
        DatabaseUtil.SQLiteUtil db = null;
        try {
            db = new DatabaseUtil.SQLiteUtil();
            db.open(path, false);
            if(db.checkTable(table)) {
                db.close();
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean createTableSQLite() {
        DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
        File f = new File(path);
        try {
            if(!f.getParentFile().exists()) {
                if(f.getParentFile().getParentFile().canWrite())  {
                    f.mkdirs();
                    if(!f.getParentFile().exists()) {
                        new NullPointerException("The folder where the database should be written doesn't exist!").printStackTrace();
                        return false;
                    }
                }
            }

            if(!f.exists()) {
                if(f.getParentFile().canWrite()) {
                    new File(path).createNewFile();
                } else {
                    new AccessDeniedException(f.getName()).printStackTrace();
                    return false;
                }
            }

            db.open(path, true);
            String command = "CREATE TABLE IF NOT EXISTS " + table + " ( " + primaryKey + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT);";
            db.update(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return true;
    }

    private List<Object> getValueSQLite(String data, DatabaseUtil.Condition... conditions) {
        DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
        List<Object> ret = new LinkedList<Object>();
        try {
            db.open(path, false);
            String command = "SELECT * FROM `" + table + "` WHERE ";
            for(int i = 0; i < conditions.length; i++) {
                DatabaseUtil.Condition con = conditions[i];
                if(i == 0) command = command + con.column_key + "=? ";
                else command = command + "AND " + con.column_key + "=? ";
            }
            command = command + ";";
            ResultSet result = db.query(command, conditions);
            while(result.next()) {
                ret.add(result.getObject(data));
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return ret;
    }

    @SuppressWarnings("deprecation")
    private boolean columnExistSQLite(String column) {
        DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
        try {
            db.open(path, false);
            if (db.checkTable(table))
                db.query("SELECT " + column + " FROM " + table + ";");
        } catch (SQLException e) {
            return false;
        } finally {
            db.close();
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private void addColumnSQLite(String column, DatabaseUtil.ObjectType type) {
        DatabaseUtil.SQLiteUtil db = new DatabaseUtil.SQLiteUtil();
        try {
            db.open(path, false);
            if(db.checkTable(table)) {
                if(!columnExistSQLite(column)) {
                    String command = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;";
                    db.update(command);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean createTableMySQL() {
        DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
        try {
            db.open(false);
            if(db.checkConnection()) {
                if(!db.checkTable(table)) {
                    String command = "CREATE TABLE IF NOT EXISTS `" + table + "` ( `" + primaryKey + " INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY (`" + primaryKey + "`)) ENGINE=InnoDB;";
                    db.update(command);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (ConnectException e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
        return true;
    }

    private List<Object> getValueMySQL(String data, DatabaseUtil.Condition... conditions) {
        DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
        List<Object> ret = new LinkedList<Object>();
        try {
            db.open(false);
            String command = "SELECT * FROM `" + table + "` WHERE ";
            for(int i = 0; i < conditions.length; i++) {
                DatabaseUtil.Condition con = conditions[i];
                if(i == 0) command = command + con.column_key + "=? ";
                else command = command + "AND " + con.column_key + "=? ";
            }
            command = command + ";";
            ResultSet result = db.query(command, conditions);
            while(result.next()) {
                ret.add(result.getObject(data));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return ret;
    }

    @SuppressWarnings("deprecation")
    private boolean columnExistMySQL(String column) {
        DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
        try {
            db.open(false);
            if(db.checkTable(table)) {
                db.query("SELECT " + column + " FROM " + table);
            }
        } catch (SQLException e) {
            return false;
        } catch (ConnectException e) {
            return false;
        } finally {
            db.close();
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private void addColumnMySQL(String column, DatabaseUtil.ObjectType type) {
        DatabaseUtil.MySQLUtil db = new DatabaseUtil.MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
        try {
            db.open(false);
            if(db.checkConnection()) {
                if(db.checkTable(table)) {
                    if(!columnExistMySQL(column)) {
                        db.update("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
