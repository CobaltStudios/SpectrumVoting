package net.spectrumnation.spectrumvoting.commands;

import net.spectrumnation.spectrumvoting.SpectrumVoting;
import net.spectrumnation.spectrumvoting.VotingDatabaseManager;
import net.spectrumnation.utils.DatabaseUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by sirtidez on 11/1/15.
 */
public class VoteRedeemCommand implements CommandExecutor {
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        VotingDatabaseManager votingDatabaseManager = SpectrumVoting.getInstance().getVotingDatabaseManager();
        if(!(commandSender instanceof Player)) {
            return false;
        }
        List<Object> result = votingDatabaseManager.getDatabaseHandler().getValues("vote_data", new DatabaseUtil.Condition("player_uuid", ((Player)commandSender).getUniqueId().toString()));
        String vote_data = "";
        boolean canRedeem = true;
        if(result.size() < 1) {
            canRedeem = false;
        } else {
            vote_data = result.toString();
        }
        String[] strArr = vote_data.split(":");
        int[] final_votes = new int[3];
        for(int i = 0; i < 4; i++) {
            int r = Integer.parseInt(strArr[i]);
            final_votes[i] = r;
        }
        canRedeem = (final_votes[0] == 1 && final_votes[1] == 1 && final_votes[2] == 1);
        if(canRedeem) {
            //TODO: Add vote rewards here
        } else {
            commandSender.sendMessage(ChatColor.RED+"Error: You do not have the required votes in order to redeem your reward!");
        }
        return true;
    }
}
