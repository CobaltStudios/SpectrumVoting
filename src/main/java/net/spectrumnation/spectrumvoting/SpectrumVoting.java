package net.spectrumnation.spectrumvoting;

import net.spectrumnation.CoreController;
import net.spectrumnation.SpectrumPlugin;
import net.spectrumnation.spectrumvoting.commands.VoteRedeemCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by sirtidez on 11/1/15.
 */
public class SpectrumVoting extends JavaPlugin implements SpectrumPlugin {
    private static SpectrumVoting instance;
    private VotingDatabaseManager vdm;

    @Override
    public void onEnable() {
        instance = this;
        CoreController.registerPlugin("SpectrumVoting", this);
        CoreController.getLogger().info("Starting SpectrumVoting...");
        vdm = new VotingDatabaseManager();
        Bukkit.getServer().getPluginManager().registerEvents(vdm, this);
        this.getCommand("votesredeem").setExecutor(new VoteRedeemCommand());
    }

    @Override
    public void onDisable() {

    }

    public VotingDatabaseManager getVotingDatabaseManager() {
        return vdm;
    }

    public static SpectrumVoting getInstance() {
        return instance;
    }

}
