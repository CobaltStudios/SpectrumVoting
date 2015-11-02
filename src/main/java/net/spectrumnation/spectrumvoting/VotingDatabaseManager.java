package net.spectrumnation.spectrumvoting;


import net.spectrumnation.utils.DatabaseUtil;
import net.spectrumnation.utils.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by sirtidez on 11/1/15.
 */
public class VotingDatabaseManager implements Listener {
    private DatabaseHandler databaseHandler;

    public VotingDatabaseManager() {
        databaseHandler = new DatabaseHandler(DatabaseUtil.DatabaseType.MYSQL, "votes_data");
        databaseHandler.init("localhost", "spectrummysqluser", "spectrummysqlpass", "spectrum_voting", "spectrummysqlport", "votes_index");
        databaseHandler.addColumn("player_uuid", DatabaseUtil.ObjectType.VARCHAR);
        databaseHandler.addColumn("votes_data", DatabaseUtil.ObjectType.VARCHAR);

        for(OfflinePlayer p : Bukkit.getServer().getOfflinePlayers()) {
            DatabaseUtil.Condition c = new DatabaseUtil.Condition("player_uuid", p.getUniqueId().toString());
            DatabaseUtil.DataObject[] da = new DatabaseUtil.DataObject[2];
            da[0] = new DatabaseUtil.DataObject("player_uuid", p.getUniqueId().toString());
            String votes_data = "0:0:0";
            da[1] = new DatabaseUtil.DataObject("votes_data", votes_data);
            if(!databaseHandler.existInTable(c)) {
                databaseHandler.insertOrUpdateValue(da, c);
            }
        }

        for(Player p : Bukkit.getServer().getOnlinePlayers()) {
            DatabaseUtil.Condition c = new DatabaseUtil.Condition("player_uuid", p.getUniqueId().toString());
            if(!databaseHandler.existInTable(c)) {
                DatabaseUtil.DataObject[] da = new DatabaseUtil.DataObject[2];
                da[0] = new DatabaseUtil.DataObject("player_uuid", p.getUniqueId().toString());
                String votes_data = "0:0:0";
                da[1] = new DatabaseUtil.DataObject("votes_data", votes_data);
                databaseHandler.insertOrUpdateValue(da, c);
            }
        }
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        DatabaseUtil.Condition c = new DatabaseUtil.Condition("player_uuid", p.getUniqueId().toString());
        if(!databaseHandler.existInTable(c)) {
            DatabaseUtil.DataObject[] da = new DatabaseUtil.DataObject[2];
            da[0] = new DatabaseUtil.DataObject("player_uuid", p.getUniqueId().toString());
            da[1] = new DatabaseUtil.DataObject("votes_data", "0:0:0");
        }
    }
}
