package me.peridot.premiumlogintest;

import me.peridot.premiumlogintest.handlers.PremiumLoginHandler;
import me.peridot.premiumlogintest.storage.PremiumNicknameCache;
import org.bukkit.plugin.java.JavaPlugin;

public class PremiumLoginTest extends JavaPlugin {

    private PremiumNicknameCache premiumNicknameCache;

    @Override
    public void onEnable() {
        premiumNicknameCache = new PremiumNicknameCache();
        new PremiumLoginHandler(this);
    }

    public PremiumNicknameCache getPremiumNicknameCache() {
        return premiumNicknameCache;
    }

}
