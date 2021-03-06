/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.kolorafa.bukkit.AuthMePremiumAutoLogin;

import uk.org.whoami.authme.api.API;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.kolorafa.premiumproxy.PremiumStatusEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.Management;

/**
 *
 * @author kolorafa
 */
public class AuthMePremiumAutoLogin extends JavaPlugin implements Listener {

    public final Logger logger = Logger.getLogger("Minecraft");
    PluginDescriptionFile pdffile;
    ArrayList<String> online;

    public String generatePassword() {
        int len = 8;
        char[] pwd = new char[len];
        int c = 'A';
        int rand = 0;
        for (int i = 0; i < len; i++) {
            rand = (int) (Math.random() * 3);
            switch (rand) {
                case 0:
                    c = '0' + (int) (Math.random() * 10);
                    break;
                case 1:
                    c = 'a' + (int) (Math.random() * 26);
                    break;
                case 2:
                    c = 'A' + (int) (Math.random() * 26);
                    break;
            }
            pwd[i] = (char) c;
        }
        return new String(pwd);
    }

    public boolean doLogin(Player p, AuthMe am) {
        Management bypass = new Management(am.database, true, am);
        bypass.performLogin(p, "dontneed");
        return true;
    }

//    public boolean doLogin(xAuthPlayer xp, xAuth plugin) {
//        int accountId = xp.getAccountId();
//        String ipAddress = xp.getIPAddress();
//        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
//
//        try {
//            // create account if one does not exist (for AuthURL only)
//            if (plugin.getConfig().getBoolean("authurl.enabled") && accountId < 1) {
//                accountId = plugin.getPlayerManager().createAccount(xp.getPlayerName(), "authURL", null, ipAddress);
//                xp.setAccountId(accountId);
//                xp.setStatus(Status.Registered);
//            }
//
//            if (plugin.getConfig().getBoolean("account.track-last-login")) {
//                plugin.getPlayerManager().updateLastLogin(accountId, ipAddress, currentTime);
//            }
//
//
//            xp.setLoginTime(currentTime);
//            xp.setStatus(Status.Authenticated);
//            plugin.getPlayerManager().unprotect(xp);
//            return true;
//        } catch (SQLException e) {
//            log("Something went wrong while auto-logging in player " + xp.getPlayerName() + ":" + e.getMessage());
//            return false;
//        }
//    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void playerpremiumchange(PremiumStatusEvent event) {
        if (event.hasPremium()) {
            online.add(event.getPlayerName());
            Player p = getServer().getPlayerExact(event.getPlayerName());
            if (p != null && p.isOnline()) {
                getServer().getScheduler().scheduleAsyncDelayedTask(this, new delayPlayerLogin(this, p), 100L);
                p.sendMessage(getConfig().getString("messages.autologinMessage").replace("{login}", p.getDisplayName()));
            }
        } else {
            online.remove(event.getPlayerName());
        }
    }

    public void check(Player p) {
        log("Sprawdzam gracza: " + p.getName());
        if (online.contains(p.getName())) {
            if (API.isAuthenticated(p)) {
                log("Already logged in: " + p.getName());
            } else {
                AuthMe am = API.hookAuthMe();
                
                if (am != null) {
                    if (am.database.getAuth(p.getName()) != null) {
                        log("Auto-login player: " + p.getName());
                        doLogin(p, am);
                        p.sendMessage(getConfig().getString("messages.autoLogged"));
                    }else{
                        log("Not registered player: " + p.getName());
                    }
                }
            }
            online.remove(p.getName());
        }
    }

//    @EventHandler(priority = EventPriority.NORMAL)
//    public void playerjoin(PlayerJoinEvent event) {
//        Player p = event.getPlayer();
//        if (p != null && p.isOnline()) {
//            check(p);
//        }
//    }
//
//    @EventHandler(priority = EventPriority.NORMAL)
//    public void playerjoin(PlayerLoginEvent event) {
//        Player p = event.getPlayer();
//        if (p != null && p.isOnline()) {
//            check(p);
//        }
//    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void playerquit(PlayerQuitEvent event) {
        online.remove(event.getPlayer().getName());
    }

    public void log(String text) {
        if (getConfig().getBoolean("debug")) {
            logger.log(Level.INFO, "[" + pdffile.getName() + "] DEBUG: " + text);
        }
    }

//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (sender instanceof ConsoleCommandSender) {
//            sender.sendMessage("This can be use only from game");
//            return true;
//        } else if (sender instanceof Player) {
//            if (online.contains(sender.getName())) {
//                xAuth a = (xAuth) getServer().getPluginManager().getPlugin("xAuth");
//                if (a != null) {
//                    xAuthPlayer player = a.getPlayerManager().getPlayer((Player) sender);
//                    if (!player.isRegistered()) {
//                        sender.sendMessage(getConfig().getString("messages.notRegistered"));
//                    } else {
//                        a.getAuthClass(player).adminChangePassword(args[0], args[0]);
//                        sender.sendMessage(getConfig().getString("messages.passwordChanged"));
//                    }
//                } else {
//                    sender.sendMessage("Somehow xAuth isn't running O_o");
//                }
//            } else {
//                sender.sendMessage(getConfig().getString("messages.notPremium"));
//            }
//        } else {
//            sender.sendMessage("Something is really wrong ! Post it to developer! " + sender);
//        }
//        return true;
//
//    }
    @Override
    public void onDisable() {
        logger.log(Level.INFO, "[" + pdffile.getName() + "] is disabled.");
    }

    @Override
    public void onEnable() {
        loadConfiguration();
        pdffile = this.getDescription();
        logger.log(Level.INFO, "[" + pdffile.getName() + "] is enabled.");
        getServer().getPluginManager().registerEvents(this, this);
        online = new ArrayList<String>();
    }

    private void loadConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
}
