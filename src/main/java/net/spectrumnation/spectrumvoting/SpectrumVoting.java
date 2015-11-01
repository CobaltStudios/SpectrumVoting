package net.spectrumnation.spectrumvoting;

import net.spectrumnation.CoreController;
import net.spectrumnation.SpectrumPlugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by sirtidez on 11/1/15.
 */
public class SpectrumVoting extends JavaPlugin implements SpectrumPlugin {

    @Override
    public void onEnable() {
        CoreController.registerPlugin("SpectrumVoting", this);
    }

    @Override
    public void onDisable() {

    }

}
