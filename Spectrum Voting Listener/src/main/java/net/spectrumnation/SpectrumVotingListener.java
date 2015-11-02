package net.spectrumnation;

import com.google.common.eventbus.Subscribe;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.spectrumnation.database.DatabaseHandler;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by sirtidez on 11/1/15.
 */
public class SpectrumVotingListener extends Plugin implements Listener {

    private DatabaseHandler databaseHandler;
    private String[] services = {"", "", ""};

    @Override
    public void onEnable() {
        initDatabase();
        this.getProxy().getPluginManager().registerListener(this, this);
    }

    public void initDatabase() {
        databaseHandler = new DatabaseHandler(DatabaseUtil.DatabaseType.MYSQL, "votes_data");
        databaseHandler.init("localhost", "spectrummysqluser", "spectrummysqlpass", "spectrum_voting", "spectrummysqlport", "votes_index");
        databaseHandler.addColumn("player_uuid", DatabaseUtil.ObjectType.VARCHAR);
        databaseHandler.addColumn("votes_data", DatabaseUtil.ObjectType.VARCHAR);
    }

    @Subscribe
    public void onPlayerVote(VotifierEvent e) {
        Vote v = e.getVote();
        String serviceName = v.getServiceName();
        String username = v.getUsername();

        for(int i = 0; i < services.length; i++) {
            if(serviceName.equals(services[i])) {
                Pattern UUID_PATTERN = Pattern.compile("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)");
                HttpProfileRepository repository = new HttpProfileRepository("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
                Profile[] profile = repository.findProfilesByNames(username);
                UUID uuid = UUID.fromString(UUID_PATTERN.matcher(profile[0].getId()).replaceFirst("$1-$2-$3-$4-$5"));
                if (profile.length == 1) {
                    if(!databaseHandler.existInTable(new DatabaseUtil.Condition("player_uuid", uuid.toString()))) {
                        DatabaseUtil.DataObject[] da = new DatabaseUtil.DataObject[2];
                        da[0] = new DatabaseUtil.DataObject("player_uuid", uuid.toString());
                        da[1] = new DatabaseUtil.DataObject("vote_data", "0:0:0");
                        databaseHandler.insertOrUpdateValue(da, new DatabaseUtil.Condition("player_uuid", uuid.toString()));
                    }
                    List<Object> voteDataList = databaseHandler.getValues("vote_data", new DatabaseUtil.Condition("player_uuid", uuid.toString()));
                    String voteData = voteDataList.get(0).toString();
                    String[] strArr = voteData.split(":");
                    strArr[i] = "1";
                    voteData = strArr[0] + ":" + strArr[1] + ":" + strArr[2];
                    DatabaseUtil.DataObject[] da = new DatabaseUtil.DataObject[2];
                    da[0] = new DatabaseUtil.DataObject("player_uuid", uuid.toString());
                    da[1] = new DatabaseUtil.DataObject("vote_data", voteData);
                    databaseHandler.insertOrUpdateValue(da, new DatabaseUtil.Condition("player_uuid", uuid.toString()));
                    break;
                }
            }
        }
    }
}
