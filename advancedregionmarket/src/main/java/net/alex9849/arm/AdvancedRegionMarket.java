package net.alex9849.arm;

import net.alex9849.arm.Handler.ARMListener;
import net.alex9849.arm.Handler.CommandHandler;
import net.alex9849.arm.Handler.Scheduler;
import net.alex9849.arm.Preseter.ContractPreset;
import net.alex9849.arm.Preseter.Preset;
import net.alex9849.arm.Preseter.RentPreset;
import net.alex9849.arm.Preseter.SellPreset;
import net.alex9849.arm.regions.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.alex9849.arm.Group.LimitGroup;
import net.alex9849.arm.gui.Gui;
import net.alex9849.inter.WorldEditInterface;
import net.alex9849.inter.WorldGuardInterface;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class AdvancedRegionMarket extends JavaPlugin {
    private static Boolean faWeInstalled;
    private static Economy econ;
    private static WorldGuardPlugin worldguard;
    private static WorldGuardInterface worldGuardInterface;
    private static WorldEditPlugin worldedit;
    private static WorldEditInterface worldEditInterface;
    private static boolean enableAutoReset;
    private static boolean enableTakeOver;
    private static Statement stmt;
    private static String sqlPrefix;
    private static int autoResetAfter;
    private static int takeoverAfter;
    private static boolean teleportAfterSellRegionBought;
    private static boolean teleportAfterRentRegionBought;
    private static boolean teleportAfterRentRegionExtend;
    private static boolean teleportAfterContractRegionBought;
    private static boolean displayDefaultRegionKindInGUI;
    private static boolean displayDefaultRegionKindInLimits;
    private static boolean sendContractRegionExtendMessage;
    private static String REMAINING_TIME_TIMEFORMAT = "%date%";
    private static String DATE_TIMEFORMAT = "dd.MM.yyyy hh:mm";
    private static boolean useShortCountdown = false;
    private CommandHandler commandHandler;

    public void onEnable(){

        if(!AdvancedRegionMarket.isAllowStartup(this)){
            this.setEnabled(false);
            getLogger().log(Level.WARNING, "Plugin remotely deactivated!");
            return;
        }

        //Enable bStats
        Metrics metrics = new Metrics(this);

        AdvancedRegionMarket.faWeInstalled = setupFaWe();

        //Check if Worldguard is installed
        if (!setupWorldGuard()) {
            getLogger().log(Level.INFO, "Please install Worldguard!");
        }
        //Check if Worldedit is installed
        if (!setupWorldEdit()) {
            getLogger().log(Level.INFO, "Please install Worldedit!");
        }
        //Check if Vault and an Economy Plugin is installed
        if (!setupEconomy()) {
            getLogger().log(Level.INFO, "Please install Vault and a economy Plugin!");
        }

        File schematicdic = new File(getDataFolder() + "/schematics");
        if(!schematicdic.exists()){
            schematicdic.mkdirs();
        }

        this.updateConfigs();

        Messages.read();
        ARMListener listener = new ARMListener();
        getServer().getPluginManager().registerEvents(listener, this);
        Gui guilistener = new Gui();
        getServer().getPluginManager().registerEvents(guilistener, this);
        loadAutoPrice();
        loadRegionKind();
        loadGroups();
        loadGUI();
        if(!loadAutoReset()) {
            this.setEnabled(false);
            getLogger().log(Level.WARNING, "SQL Login wrong! Disabeling Plugin...");
            return;
        }
        loadOther();
        loadRegions();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Scheduler() , 0 ,20*getConfig().getInt("Other.SignAndResetUpdateInterval"));
        this.commandHandler = new CommandHandler();
        getCommand("arm").setTabCompleter(this.commandHandler);
        Bukkit.getLogger().log(Level.INFO, "Programmed by Alex9849");

    }

    public void onDisable(){
        AdvancedRegionMarket.econ = null;
        AdvancedRegionMarket.worldguard = null;
        AdvancedRegionMarket.worldedit = null;
        Region.Reset();
        LimitGroup.Reset();
        AutoPrice.Reset();
        RegionKind.Reset();
        SellPreset.reset();
        RentPreset.reset();
        ContractPreset.reset();
        getServer().getServicesManager().unregisterAll(this);
        SignChangeEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        BlockBreakEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        BlockPlaceEvent.getHandlerList().unregister(this);
        BlockPhysicsEvent.getHandlerList().unregister(this);
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        BlockExplodeEvent.getHandlerList().unregister(this);
        getServer().getScheduler().cancelTasks(this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        AdvancedRegionMarket.econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupFaWe(){
        Plugin plugin = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");

        if (plugin == null) {
            return false;
        }
        return true;
    }

    private boolean setupWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return false;
        }
        AdvancedRegionMarket.worldguard = (WorldGuardPlugin) plugin;
        String version = "notSupported";
        if(AdvancedRegionMarket.worldguard.getDescription().getVersion().startsWith("6.")) {
            version = "6";
        } else {
            version = "7";
        }
        try {
            final Class<?> wgClass = Class.forName("net.alex9849.adapters.WorldGuard" + version);
            if(WorldGuardInterface.class.isAssignableFrom(wgClass)) {
                AdvancedRegionMarket.worldGuardInterface = (WorldGuardInterface) wgClass.newInstance();
            }
            Bukkit.getLogger().log(Level.INFO, "[AdvancedRegionMarket] Using WorldGuard" + version + " adapter");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[AdvancedRegionMarket] Could not setup WorldGuard! (Handler could not be loaded) Compatible WorldGuard versions: 6, 7");
            e.printStackTrace();
        }

        return worldguard != null;
    }

    private boolean setupWorldEdit() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");

        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return false;
        }
        AdvancedRegionMarket.worldedit = (WorldEditPlugin) plugin;
        String version = "notSupported";

        if(AdvancedRegionMarket.worldedit.getDescription().getVersion().startsWith("6.")) {
            version = "6";
        } else {
            version = "7";
        }
        if(AdvancedRegionMarket.isFaWeInstalled()){
            version = version + "FaWe";
        }
        try {
            final Class<?> weClass = Class.forName("net.alex9849.adapters.WorldEdit" + version);
            if(WorldEditInterface.class.isAssignableFrom(weClass)) {
                AdvancedRegionMarket.worldEditInterface = (WorldEditInterface) weClass.newInstance();
            }
            Bukkit.getLogger().log(Level.INFO, "[AdvancedRegionMarket] Using WorldEdit" + version + " adapter");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[AdvancedRegionMarket] Could not setup WorldEdit! (Handler could not be loaded) Compatible WorldEdit versions: 6, 7");
            e.printStackTrace();
        }


        return worldedit != null;
    }

    public static WorldGuardPlugin getWorldGuard(){
        return AdvancedRegionMarket.worldguard;
    }

    public static WorldGuardInterface getWorldGuardInterface() {
        return  AdvancedRegionMarket.worldGuardInterface;
    }

    public static WorldEditInterface getWorldEditInterface(){
        return AdvancedRegionMarket.worldEditInterface;
    }

    private void loadRegions() {
        if(Region.getRegionsConf().get("Regions") != null) {
            LinkedList<String> worlds = new LinkedList<String>(Region.getRegionsConf().getConfigurationSection("Regions").getKeys(false));
            if(worlds != null) {
                for(int y = 0; y < worlds.size(); y++) {
                    if(Bukkit.getWorld(worlds.get(y)) != null) {
                        if(Region.getRegionsConf().get("Regions." + worlds.get(y)) != null) {
                            LinkedList<String> regions = new LinkedList<String>(Region.getRegionsConf().getConfigurationSection("Regions." + worlds.get(y)).getKeys(false));
                            if(regions != null) {
                                for(int i = 0; i < regions.size(); i++){
                                    String regionworld = worlds.get(y);
                                    String regionname = regions.get(i);
                                    int price = Region.getRegionsConf().getInt("Regions." + worlds.get(y) + "." + regions.get(i) + ".price");
                                    boolean sold = Region.getRegionsConf().getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".sold");
                                    String kind = Region.getRegionsConf().getString("Regions." + worlds.get(y) + "." + regions.get(i) + ".kind");
                                    boolean autoreset = Region.getRegionsConf().getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".autoreset");
                                    String regiontype = Region.getRegionsConf().getString("Regions." + worlds.get(y) + "." + regions.get(i) + ".regiontype");
                                    boolean allowonlynewblocks = Region.getRegionsConf().getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".isHotel");
                                    boolean doBlockReset = Region.getRegionsConf().getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".doBlockReset");
                                    long lastreset = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".lastreset");
                                    String teleportLocString = Region.getRegionsConf().getString("Regions." + worlds.get(y) + "." + regions.get(i) + ".teleportLoc");
                                    Location teleportLoc = null;
                                    if(teleportLocString != null) {
                                        String[] teleportLocarr = teleportLocString.split(";");
                                        World teleportLocWorld = Bukkit.getWorld(teleportLocarr[0]);
                                        int teleportLocBlockX = Integer.parseInt(teleportLocarr[1]);
                                        int teleportLocBlockY = Integer.parseInt(teleportLocarr[2]);
                                        int teleportLocBlockZ = Integer.parseInt(teleportLocarr[3]);
                                        float teleportLocPitch = Float.parseFloat(teleportLocarr[4]);
                                        float teleportLocYaw = Float.parseFloat(teleportLocarr[5]);
                                        teleportLoc = new Location(teleportLocWorld, teleportLocBlockX, teleportLocBlockY, teleportLocBlockZ);
                                        teleportLoc.setYaw(teleportLocYaw);
                                        teleportLoc.setPitch(teleportLocPitch);
                                    }
                                    RegionKind regionKind = RegionKind.DEFAULT;
                                    if(kind != null){
                                        RegionKind result = RegionKind.getRegionKind(kind);
                                        if(result != null){
                                            regionKind = result;
                                        }
                                    }
                                    ProtectedRegion region = AdvancedRegionMarket.getWorldGuardInterface().getRegionManager(Bukkit.getWorld(regionworld), AdvancedRegionMarket.worldguard).getRegion(regionname);

                                    if(region != null) {
                                        List<String> regionsignsloc = Region.getRegionsConf().getStringList("Regions." + worlds.get(y) + "." + regions.get(i) + ".signs");
                                        List<Sign> regionsigns = new ArrayList<>();
                                        for(int j = 0; j < regionsignsloc.size(); j++) {
                                            String[] locsplit = regionsignsloc.get(j).split(";", 4);
                                            World world = Bukkit.getWorld(locsplit[0]);
                                            Double x = Double.parseDouble(locsplit[1]);
                                            Double yy = Double.parseDouble(locsplit[2]);
                                            Double z = Double.parseDouble(locsplit[3]);
                                            Location loc = new Location(world, x, yy, z);
                                            Location locminone = new Location(world, x, yy - 1, z);

                                            if ((loc.getBlock().getType() != Material.SIGN) && (loc.getBlock().getType() != Material.WALL_SIGN)){
                                                if(locminone.getBlock().getType() == Material.AIR || locminone.getBlock().getType() == Material.LAVA || locminone.getBlock().getType() == Material.WATER
                                                        || locminone.getBlock().getType() == Material.LAVA || locminone.getBlock().getType() == Material.WATER) {
                                                    locminone.getBlock().setType(Material.STONE);
                                                }
                                                loc.getBlock().setType(Material.SIGN);

                                            }

                                            regionsigns.add((Sign) loc.getBlock().getState());
                                        }
                                        if (regiontype.equalsIgnoreCase("rentregion")){
                                            long payedtill = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".payedTill");
                                            long maxRentTime = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".maxRentTime");
                                            long rentExtendPerClick = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".rentExtendPerClick");
                                            Region armregion = new RentRegion(region, regionworld, regionsigns, price, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc,
                                                    lastreset, payedtill, maxRentTime, rentExtendPerClick,false);
                                            armregion.updateSigns();
                                            Region.getRegionList().add(armregion);
                                        } else if (regiontype.equalsIgnoreCase("sellregion")){
                                            Region armregion = new SellRegion(region, regionworld, regionsigns, price, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc, lastreset,false);
                                            armregion.updateSigns();
                                            Region.getRegionList().add(armregion);
                                        } else if (regiontype.equalsIgnoreCase("contractregion")) {
                                            long payedtill = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".payedTill");
                                            long extendTime = Region.getRegionsConf().getLong("Regions." + worlds.get(y) + "." + regions.get(i) + ".extendTime");
                                            Boolean terminated = Region.getRegionsConf().getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".terminated");
                                            Region armregion = new ContractRegion(region, regionworld, regionsigns, price, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc, lastreset,extendTime, payedtill, terminated, false);
                                            armregion.updateSigns();
                                            Region.getRegionList().add(armregion);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for(int i = 0; i < Region.getRegionList().size(); i++) {
            Region.getRegionList().get(i).writeSigns();
        }
    }

    public static AdvancedRegionMarket getARM() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AdvancedRegionMarket");
        if(plugin instanceof AdvancedRegionMarket) {
            return (AdvancedRegionMarket) plugin;
        } else {
            return null;
        }
    }

    public static boolean isUseShortCountdown() {
        return AdvancedRegionMarket.useShortCountdown;
    }

    public static Boolean isFaWeInstalled(){
        return AdvancedRegionMarket.faWeInstalled;
    }

    public static Boolean isSendContractRegionExtendMessage() {
        return AdvancedRegionMarket.sendContractRegionExtendMessage;
    }

    private void loadAutoPrice() {
        if(getConfig().get("AutoPrice") != null) {
            LinkedList<String> autoPrices = new LinkedList<>(getConfig().getConfigurationSection("AutoPrice").getKeys(false));
            if(autoPrices != null) {
                for(int i = 0; i < autoPrices.size(); i++){
                    AutoPrice.getAutoPrices().add(new AutoPrice(autoPrices.get(i), getConfig().getDouble("AutoPrice." + autoPrices.get(i))));
                }
            }
        }
    }

    private void loadRegionKind(){
        RegionKind.DEFAULT.setName(getConfig().getString("DefaultRegionKind.DisplayName"));
        RegionKind.DEFAULT.setMaterial(Material.getMaterial(getConfig().getString("DefaultRegionKind.Item")));
        List<String> defaultlore = getConfig().getStringList("DefaultRegionKind.Lore");
        for(int x = 0; x < defaultlore.size(); x++){
            defaultlore.set(x, ChatColor.translateAlternateColorCodes('&', defaultlore.get(x)));
        }
        RegionKind.DEFAULT.setLore(defaultlore);

        if(getConfig().get("RegionKinds") != null) {
            LinkedList<String> regionKinds = new LinkedList<String>(getConfig().getConfigurationSection("RegionKinds").getKeys(false));
            if(regionKinds != null) {
                for(int i = 0; i < regionKinds.size(); i++){
                    Material mat = Material.getMaterial(getConfig().getString("RegionKinds." + regionKinds.get(i) + ".item"));
                    List<String> lore = getConfig().getStringList("RegionKinds." + regionKinds.get(i) + ".lore");
                    for(int x = 0; x < lore.size(); x++){
                        lore.set(x, ChatColor.translateAlternateColorCodes('&', lore.get(x)));
                    }
                    RegionKind.getRegionKindList().add(new RegionKind(regionKinds.get(i), mat, lore));
                }
            }
        }
    }

    private void loadGUI(){
        FileConfiguration pluginConf = getConfig();
        Gui.setRegionOwnerItem(Material.getMaterial(pluginConf.getString("GUI.RegionOwnerItem")));
        Gui.setRegionMemberItem(Material.getMaterial(pluginConf.getString("GUI.RegionMemberItem")));
        Gui.setRegionFinderItem(Material.getMaterial(pluginConf.getString("GUI.RegionFinderItem")));
        Gui.setGoBackItem(Material.getMaterial(pluginConf.getString("GUI.GoBackItem")));
        Gui.setWarningYesItem(Material.getMaterial(pluginConf.getString("GUI.WarningYesItem")));
        Gui.setWarningNoItem(Material.getMaterial(pluginConf.getString("GUI.WarningNoItem")));
        Gui.setTpItem(Material.getMaterial(pluginConf.getString("GUI.TPItem")));
        Gui.setSellRegionItem(Material.getMaterial(pluginConf.getString("GUI.SellRegionItem")));
        Gui.setResetItem(Material.getMaterial(pluginConf.getString("GUI.ResetItem")));
        Gui.setExtendItem(Material.getMaterial(pluginConf.getString("GUI.ExtendItem")));
        Gui.setInfoItem(Material.getMaterial(pluginConf.getString("GUI.InfoItem")));
        Gui.setPromoteMemberToOwnerItem(Material.getMaterial(pluginConf.getString("GUI.PromoteMemberToOwnerItem")));
        Gui.setRemoveMemberItem(Material.getMaterial(pluginConf.getString("GUI.RemoveMemberItem")));
        Gui.setFillItem(Material.getMaterial(pluginConf.getString("GUI.FillItem")));
        Gui.setContractItem(Material.getMaterial(pluginConf.getString("GUI.ContractItem")));

    }

    private Boolean loadAutoReset(){
        Boolean success = true;
        AdvancedRegionMarket.enableAutoReset = getConfig().getBoolean("AutoResetAndTakeOver.enableAutoReset");
        AdvancedRegionMarket.enableTakeOver = getConfig().getBoolean("AutoResetAndTakeOver.enableTakeOver");
        if(AdvancedRegionMarket.enableAutoReset || AdvancedRegionMarket.enableTakeOver) {
            String mysqlhost = getConfig().getString("AutoResetAndTakeOver.mysql-server");
            String mysqldatabase = getConfig().getString("AutoResetAndTakeOver.mysql-database");
            String mysqlpass = getConfig().getString("AutoResetAndTakeOver.mysql-password");
            String mysqluser = getConfig().getString("AutoResetAndTakeOver.mysql-user");
            AdvancedRegionMarket.sqlPrefix = getConfig().getString("AutoResetAndTakeOver.mysql-prefix");
            AdvancedRegionMarket.autoResetAfter = getConfig().getInt("AutoResetAndTakeOver.autoresetAfter");
            AdvancedRegionMarket.takeoverAfter = getConfig().getInt("AutoResetAndTakeOver.takeoverAfter");

            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                Connection con = DriverManager.getConnection("jdbc:mysql://" + mysqlhost + "/" + mysqldatabase, mysqluser, mysqlpass);
                AdvancedRegionMarket.stmt = con.createStatement();
                AdvancedRegionMarket.checkOrCreateMySql(mysqldatabase);
                getLogger().log(Level.INFO, "SQL Login successful!");
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
                getLogger().log(Level.INFO, "SQL Login faulty!");
                success = false;
            }
        }
        return success;
    }

    private void loadGroups(){
        if(getConfig().get("Limits") != null) {
            List<String> groups = new ArrayList<>(getConfig().getConfigurationSection("Limits").getKeys(false));
            if(groups != null) {
                for(int i = 0; i < groups.size(); i++) {
                    LimitGroup.getGroupList().add(new LimitGroup(groups.get(i)));
                }
            }
        }
    }

    public static boolean isTeleportAfterContractRegionBought(){
        return AdvancedRegionMarket.teleportAfterContractRegionBought;
    }

    private void loadOther(){
        AdvancedRegionMarket.teleportAfterRentRegionBought = getConfig().getBoolean("Other.TeleportAfterRentRegionBought");
        AdvancedRegionMarket.teleportAfterRentRegionExtend = getConfig().getBoolean("Other.TeleportAfterRentRegionExtend");
        AdvancedRegionMarket.teleportAfterSellRegionBought = getConfig().getBoolean("Other.TeleportAfterSellRegionBought");
        AdvancedRegionMarket.teleportAfterContractRegionBought = getConfig().getBoolean("Other.TeleportAfterContractRegionBought");
        AdvancedRegionMarket.sendContractRegionExtendMessage = getConfig().getBoolean("Other.SendContractRegionExtendMessage");
        Region.setResetcooldown(getConfig().getInt("Other.userResetCooldown"));
        AdvancedRegionMarket.displayDefaultRegionKindInGUI = getConfig().getBoolean("DefaultRegionKind.DisplayInGUI");
        AdvancedRegionMarket.displayDefaultRegionKindInLimits = getConfig().getBoolean("DefaultRegionKind.DisplayInLimits");
        AdvancedRegionMarket.REMAINING_TIME_TIMEFORMAT = getConfig().getString("Other.RemainingTimeFormat");
        AdvancedRegionMarket.DATE_TIMEFORMAT = getConfig().getString("Other.DateTimeFormat");
        AdvancedRegionMarket.useShortCountdown = getConfig().getBoolean("Other.ShortCountdown");
        Region.setPaypackPercentage(getConfig().getDouble("Other.paypackPercentage"));
        try{
            RentRegion.setExpirationWarningTime(RentRegion.stringToTime(getConfig().getString("Other.RentRegionExpirationWarningTime")));
            RentRegion.setSendExpirationWarning(getConfig().getBoolean("Other.SendRentRegionExpirationWarning"));
        } catch (IllegalArgumentException | NullPointerException e) {
            Bukkit.getLogger().log(Level.INFO, "[AdvancedRegionMarket] Warning! Bad syntax of time format \"RentRegionExpirationWarningTime\" disabling it...");
            RentRegion.setExpirationWarningTime(0);
            RentRegion.setSendExpirationWarning(false);
        }
    }

    private static void checkOrCreateMySql(String mysqldatabase) throws SQLException {
        ResultSet rs = AdvancedRegionMarket.stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'");
        Boolean createLastlogin = true;
        while (rs.next()){
            if(rs.getString("TABLE_NAME").equals(AdvancedRegionMarket.sqlPrefix + "lastlogin")){
                createLastlogin = false;
            }
        }
        if(createLastlogin){
            AdvancedRegionMarket.stmt.executeUpdate("CREATE TABLE `" + mysqldatabase + "`.`" + AdvancedRegionMarket.sqlPrefix + "lastlogin` ( `id` INT NOT NULL AUTO_INCREMENT , `uuid` VARCHAR(40) NOT NULL , `lastlogin` TIMESTAMP NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
        }

    }

    public static Economy getEcon(){
        return AdvancedRegionMarket.econ;
    }

    public static WorldEditPlugin getWorldedit() {return AdvancedRegionMarket.worldedit;}

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandsLabel, String[] args) {
        return this.commandHandler.executeCommand(sender, cmd, commandsLabel, args);
    }

    public static String getRemainingTimeTimeformat(){
        return AdvancedRegionMarket.REMAINING_TIME_TIMEFORMAT;
    }

    public static String getDateTimeformat(){
        return AdvancedRegionMarket.DATE_TIMEFORMAT;
    }

    public static boolean isDisplayDefaultRegionKindInGUI(){
        return AdvancedRegionMarket.displayDefaultRegionKindInGUI;
    }

    public static boolean isDisplayDefaultRegionKindInLimits(){
        return AdvancedRegionMarket.displayDefaultRegionKindInLimits;
    }

    public static boolean help(CommandSender sender) {
        if(!sender.hasPermission(Permission.ARM_HELP)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "/arm setregionkind [KIND] [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm listregionkinds");
        sender.sendMessage(ChatColor.GOLD + "/arm findfreeregion [KIND]");
        sender.sendMessage(ChatColor.GOLD + "/arm resetblocks [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm reset [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm info [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm addmember [REGION] [PLAYER]");
        sender.sendMessage(ChatColor.GOLD + "/arm removemember [REGION] [PLAYER]");
        sender.sendMessage(ChatColor.GOLD + "/arm autoreset [REGION] [true/false]");
        sender.sendMessage(ChatColor.GOLD + "/arm listregions [PLAYER]");
        sender.sendMessage(ChatColor.GOLD + "/arm setowner [REGION] [PLAYER]");
        sender.sendMessage(ChatColor.GOLD + "/arm hotel [REGION] [true/false]");
        sender.sendMessage(ChatColor.GOLD + "/arm updateschematic [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm setwarp [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm unsell [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm extend [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm delete [REGION]");
        sender.sendMessage(ChatColor.GOLD + "/arm doblockreset [REGION] [true/false]");
        sender.sendMessage(ChatColor.GOLD + "/arm terminate [REGION] [true/false]");
        sender.sendMessage(ChatColor.GOLD + "/arm sellpreset [SETTING]");
        sender.sendMessage(ChatColor.GOLD + "/arm rentpreset [SETTING]");
        sender.sendMessage(ChatColor.GOLD + "/arm contractpreset [SETTING]");
        sender.sendMessage(ChatColor.GOLD + "/arm limit");
        sender.sendMessage(ChatColor.GOLD + "/arm reload");
        return true;
    }

    public static Statement getStmt() {
        return stmt;
    }

    public static String getSqlPrefix(){
        return AdvancedRegionMarket.sqlPrefix;
    }

    public static boolean getEnableAutoReset(){
        return AdvancedRegionMarket.enableAutoReset;
    }

    public static int getAutoResetAfter(){
        return AdvancedRegionMarket.autoResetAfter;
    }

    public static boolean getEnableTakeOver(){
        return AdvancedRegionMarket.enableTakeOver;
    }

    public static int getTakeoverAfter(){
        return AdvancedRegionMarket.takeoverAfter;
    }

    public static boolean isTeleportAfterRentRegionBought() {
        return teleportAfterRentRegionBought;
    }

    public static boolean isTeleportAfterSellRegionBought() {
        return teleportAfterSellRegionBought;
    }

    public static boolean isTeleportAfterRentRegionExtend() {
        return teleportAfterRentRegionExtend;
    }

    public static boolean isAllowStartup(Plugin plugin){
        Server server = Bukkit.getServer();
        String ip = server.getIp();
        int port = server.getPort();
        String hoststring = "";

        try {
            hoststring = InetAddress.getLocalHost().toString();
        } catch (Exception e) {
            hoststring = "";
        }

        Boolean allowStart = true;

        try {
            final String userAgent = "Alex9849 Plugin";
            String str=null;
            String str1=null;
            URL url = new URL("http://mcplug.alex9849.net/mcplug.php");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setInstanceFollowRedirects(true);
            con.addRequestProperty("User-Agent", userAgent);
            con.setDoOutput(true);
            PrintStream ps = new PrintStream(con.getOutputStream());

            ps.print("plugin=arm");
            ps.print("&host=" + hoststring);
            ps.print("&ip=" + ip);
            ps.print("&port=" + port);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));

            ps.close();

            str = new String();
            while ((str1 = in.readLine()) != null) {
                str = str + str1;
            }
            in.close();
            if(str.equals("1")){
                allowStart = false;
            } else {
                allowStart = true;
            }

        } catch (IOException e) {
            return allowStart;
        }
        return allowStart;
    }

    public ARMAPI getAPI(){
        return new ARMAPI();
    }

    public void generatedefaultconfig(){
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket");
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File messagesdic = new File(pluginfolder + "/config.yml");
        if(!messagesdic.exists()){
            try {
                InputStream stream = plugin.getResource("config.yml");
                byte[] buffer = new byte[stream.available()];
                stream.read(buffer);
                OutputStream output = new FileOutputStream(messagesdic);
                output.write(buffer);
                output.close();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.reloadConfig();
    }

    private void updateConfigs(){
        Region.generatedefaultConfig();
        Region.setRegionsConf();
        Messages.generatedefaultConfig();
        Preset.generatedefaultConfig();
        Preset.loadConfig();
        SellPreset.loadPresets();
        RentPreset.loadPresets();
        ContractPreset.loadPresets();
        this.generatedefaultconfig();
        FileConfiguration pluginConfig = this.getConfig();
        YamlConfiguration regionConf = Region.getRegionsConf();
        Double version = pluginConfig.getDouble("Version");
        if(version < 1.1) {
            getLogger().log(Level.WARNING, "Updating AdvancedRegionMarket config to 1.1...");
            pluginConfig.set("GUI.RegionOwnerItem", Material.ENDER_CHEST.toString());
            pluginConfig.set("GUI.RegionMemberItem", Material.CHEST.toString());
            pluginConfig.set("GUI.RegionFinderItem", Material.COMPASS.toString());
            pluginConfig.set("GUI.GoBackItem", "WOOD_DOOR");
            pluginConfig.set("GUI.WarningYesItem", "MELON_BLOCK");
            pluginConfig.set("GUI.WarningNoItem", Material.REDSTONE_BLOCK.toString());
            pluginConfig.set("GUI.TPItem", Material.ENDER_PEARL.toString());
            pluginConfig.set("GUI.SellRegionItem", Material.DIAMOND.toString());
            pluginConfig.set("GUI.ResetItem", Material.TNT.toString());
            pluginConfig.set("GUI.ExtendItem", "WATCH");
            pluginConfig.set("GUI.InfoItem", Material.BOOK.toString());
            pluginConfig.set("GUI.PromoteMemberToOwnerItem", Material.LADDER.toString());
            pluginConfig.set("GUI.RemoveMemberItem", Material.LAVA_BUCKET.toString());
            pluginConfig.set("Version", 1.1);
            saveConfig();
        }
        version = pluginConfig.getDouble("Version");
        if(version < 1.2) {
            getLogger().log(Level.WARNING, "Updating AdvancedRegionMarket config to 1.2...");
            getLogger().log(Level.WARNING, "Warning!: ARM uses a new schematic format now! You have to update all region schematics with");
            getLogger().log(Level.WARNING, "/arm updateschematic [REGION] or go back to ARM version 1.1");
            pluginConfig.set("Version", 1.2);
            saveConfig();


            LinkedList<String> worlds = new LinkedList<String>(regionConf.getConfigurationSection("Regions").getKeys(false));
            if(worlds != null) {
                for(int y = 0; y < worlds.size(); y++) {
                    LinkedList<String> regions = new LinkedList<String>(regionConf.getConfigurationSection("Regions." + worlds.get(y)).getKeys(false));
                    if(regions != null) {
                        for (int i = 0; i < regions.size(); i++) {
                            regionConf.set("Regions." + worlds.get(y) + "." + regions.get(i) + ".doBlockReset", true);
                        }
                    }
                }
            }
            Region.saveRegionsConf(regionConf);
            YamlConfiguration messagesconf = Messages.getConfig();
            messagesconf.set("Messages.RegionIsNotARentregion", "&4Region is not a rentregion!");
            messagesconf.set("Messages.RegionNotOwn", "&4You do not own this region!");
            messagesconf.set("Messages.RegionNotSold", "&4Region not sold!");
            messagesconf.set("Messages.PresetRemoved", "&aPreset removed!");
            messagesconf.set("Messages.PresetSet", "&aPreset set!");
            messagesconf.set("Messages.RegionInfoDoBlockReset", "&6DoBlockReset: ");
            messagesconf.set("Messages.PresetSaved", "&aPreset saved!");
            messagesconf.set("Messages.PresetAlreadyExists", "&4A preset with this name already exists!");
            messagesconf.set("Messages.PresetPlayerDontHasPreset", "&4You do not have a preset!");
            messagesconf.set("Messages.PresetDeleted", "&aPreset deleted!");
            messagesconf.set("Messages.PresetNotFound", "&4No preset with this name found!");
            messagesconf.set("Messages.PresetLoaded", "&aPreset loaded!");
            Messages.saveConfig();
        }
        if(version < 1.21) {
            getLogger().log(Level.WARNING, "Updating AdvancedRegionMarket config to 1.21...");
            Material mat = null;
            mat = Material.getMaterial(pluginConfig.getString("GUI.RegionOwnerItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.RegionOwnerItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.RegionMemberItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.RegionMemberItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.RegionFinderItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.RegionFinderItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.GoBackItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.GoBackItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.WarningYesItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.WarningYesItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.WarningNoItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.WarningNoItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.SellRegionItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.SellRegionItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.ResetItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.ResetItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.ExtendItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.ExtendItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.InfoItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.InfoItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.PromoteMemberToOwnerItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.PromoteMemberToOwnerItem", mat.toString());
            }
            mat = Material.getMaterial(pluginConfig.getString("GUI.RemoveMemberItem"), true);
            if(mat != null) {
                pluginConfig.set("GUI.RemoveMemberItem", mat.toString());
            }

            LinkedList<String> regionKinds = new LinkedList<String>(pluginConfig.getConfigurationSection("RegionKinds").getKeys(false));
            if(regionKinds != null) {
                for(int i = 0; i < regionKinds.size(); i++){
                    mat = Material.getMaterial(pluginConfig.getString("RegionKinds." + regionKinds.get(i) + ".item"), true);
                    if(mat != null) {
                        pluginConfig.set("RegionKinds." + regionKinds.get(i) + ".item", mat.toString());
                    }
                }
            }
            pluginConfig.set("Version", 1.21);
            saveConfig();
        }
        if(version < 1.3) {
            getLogger().log(Level.WARNING, "Updating AdvancedRegionMarket config to 1.3...");
            pluginConfig.set("DefaultRegionKind.DisplayName", "Default");
            pluginConfig.set("DefaultRegionKind.Item", Material.RED_BED.toString());
            List<String> defaultLore = new ArrayList<>();
            defaultLore.add("very default");
            pluginConfig.set("DefaultRegionKind.Lore", defaultLore);
            pluginConfig.set("DefaultRegionKind.DisplayInLimits", true);
            pluginConfig.set("DefaultRegionKind.DisplayInGUI", false);
            pluginConfig.set("Other.SendRentRegionExpirationWarning", true);
            pluginConfig.set("Other.RentRegionExpirationWarningTime", "2d");
            pluginConfig.set("Other.TeleportAfterContractRegionBought", true);
            pluginConfig.set("Other.SendContractRegionExtendMessage", true);
            pluginConfig.set("Other.SignAndResetUpdateInterval", 10);
            pluginConfig.set("Other.RemainingTimeFormat", "%countdown%");
            pluginConfig.set("Other.DateTimeFormat", "dd.MM.yyyy hh:mm");
            pluginConfig.set("Other.ShortCountdown", false);
            pluginConfig.set("Version", 1.3);
            saveConfig();

            YamlConfiguration messagesconf = Messages.getConfig();
            messagesconf.set("Messages.LimitInfoTotal", "Total");
            messagesconf.set("Messages.GUIRegionItemName", "%regionid% (%regionkind%)");
            messagesconf.set("Messages.GUIRegionFinderRegionKindName", "%regionkind%");
            messagesconf.set("Messages.RentRegionExpirationWarning", "&4[WARNING] This RentRegion(s) will expire soon: &c");
            messagesconf.set("Messages.ContractSign1", "&2Contract");
            messagesconf.set("Messages.ContractSign2", "&2available");
            messagesconf.set("Messages.ContractSign3", "%regionid%");
            messagesconf.set("Messages.ContractSign4", "%price%%currency%/%extend%");
            messagesconf.set("Messages.ContractSoldSign1", "&4Contract in use");
            messagesconf.set("Messages.ContractSoldSign2", "%regionid%/%owner%");
            messagesconf.set("Messages.ContractSoldSign3", "%price%%currency%/%extend%");
            messagesconf.set("Messages.ContractSoldSign4", "%remaining%");
            messagesconf.set("Messages.ContractRegionExtended", "&aYour contract region %regionid% has been extended for %extend%. (For %price%%currency%.)");
            messagesconf.set("Messages.GUIContractItem", "&6Manage contract");
            messagesconf.set("Messages.RegionInfoTerminated", "&6Terminated: ");
            messagesconf.set("Messages.RegionInfoAutoExtendTime", "&6Extend time: ");
            messagesconf.set("Messages.RegionInfoRemainingTime", "&6Next extend in: ");
            messagesconf.set("Messages.ContractRegionChangeTerminated", "&6The contract of &a%regionid% &6has been set to %statuslong%");
            messagesconf.set("Messages.ContractRegionStatusActiveLong", "&aActive&6! Next Extension in %remaining%");
            messagesconf.set("Messages.ContractRegionStatusActive", "&aActive");
            messagesconf.set("Messages.ContractRegionStatusTerminatedLong", "&4Terminated&6! It will be resetted in %remaining%");
            messagesconf.set("Messages.ContractRegionStatusTerminated", "&4Terminated");
            messagesconf.set("Messages.RegionIsNotAContractRegion", "&4Region is not a contractregion!");
            messagesconf.set("Messages.RegiontransferMemberNotOnline", "&4Member not online!");
            messagesconf.set("Messages.RegiontransferLimitError", "&4Transfer aborted! (Region would exceed players limit)");

            List<String> contractItemLore = new ArrayList<>();
            contractItemLore.add("&aStatus: %status%");
            contractItemLore.add("&aIf active the next extend is in:");
            contractItemLore.add("&6%remaining%");
            messagesconf.set("Messages.GUIContractItemLore", contractItemLore);
            List<String> contractItemRegionLore = new ArrayList<>();
            contractItemRegionLore.add("&aStatus: %status%");
            contractItemRegionLore.add("&aIf active the next extend is in:");
            contractItemRegionLore.add("&6%remaining%");
            messagesconf.set("Messages.GUIContractItemRegionLore", contractItemRegionLore);
            List<String> memberlistInfolore = new LinkedList<>(messagesconf.getStringList("Messages.MemberlistInfoLore"));
            String memberlistinfo = messagesconf.getString("Messages.MemberlistInfo");
            List<String> ownermemberlistInfolore = new LinkedList<>(messagesconf.getStringList("Messages.MemberlistInfoLore"));
            String ownermemberlistinfo = messagesconf.getString("Messages.MemberlistInfo");
            messagesconf.set("Messages.MemberlistInfo", null);
            messagesconf.set("Messages.MemberlistInfoLore", null);
            messagesconf.set("Messages.MemberlistInfo", memberlistinfo);
            messagesconf.set("Messages.MemberlistInfoLore", memberlistInfolore);
            messagesconf.set("Messages.OwnerMemberlistInfo", ownermemberlistinfo);
            messagesconf.set("Messages.OwnerMemberlistInfoLore", ownermemberlistInfolore);
            messagesconf.set("Messages.Seconds", "s");
            messagesconf.set("Messages.Minutes", "m");
            messagesconf.set("Messages.Hours", "h");
            messagesconf.set("Messages.Days", "d");
            messagesconf.set("Messages.SecondsForShortCountDown", "second(s)");
            messagesconf.set("Messages.MinutesForShortCountDown", "minute(s)");
            messagesconf.set("Messages.HoursForShortCountDown", "hour(s)");
            messagesconf.set("Messages.DaysForShortCountDown", "day(s)");

            Messages.saveConfig();

            LinkedList<String> worlds = new LinkedList<String>(regionConf.getConfigurationSection("Regions").getKeys(false));
            if(worlds != null) {
                for(int y = 0; y < worlds.size(); y++) {
                    LinkedList<String> regions = new LinkedList<String>(regionConf.getConfigurationSection("Regions." + worlds.get(y)).getKeys(false));
                    if(regions != null) {
                        for (int i = 0; i < regions.size(); i++) {
                            if(regionConf.getBoolean("Regions." + worlds.get(y) + "." + regions.get(i) + ".rentregion")) {
                                regionConf.set("Regions." + worlds.get(y) + "." + regions.get(i) + ".regiontype", "rentregion");
                            } else {
                                regionConf.set("Regions." + worlds.get(y) + "." + regions.get(i) + ".regiontype", "sellregion");
                            }
                            regionConf.set("Regions." + worlds.get(y) + "." + regions.get(i) + ".rentregion", null);
                            regionConf.set("Regions." + worlds.get(y) + "." + regions.get(i) + ".world", null);
                        }
                    }
                }
            }
            Region.saveRegionsConf(regionConf);
        }
        if(version < 1.4) {
            pluginConfig.set("GUI.FillItem", "GRAY_STAINED_GLASS_PANE");
            pluginConfig.set("GUI.ContractItem", "WRITABLE_BOOK");
            pluginConfig.set("GUI.DisplayRegionOwnerButton", true);
            pluginConfig.set("GUI.DisplayRegionMemberButton", true);
            pluginConfig.set("GUI.DisplayRegionFinderButton", true);
            pluginConfig.set("Version", 1.4);
            saveConfig();
            YamlConfiguration messages = Messages.getConfig();
            messages.set("Messages.UserNotAMemberOrOwner", "&4You are not a member or owner of this region!");
            Messages.saveConfig();
        }
    }
}