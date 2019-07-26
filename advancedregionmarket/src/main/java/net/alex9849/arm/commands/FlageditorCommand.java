package net.alex9849.arm.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.flaggroups.FlagSettings;
import net.alex9849.arm.gui.Gui;
import net.alex9849.exceptions.SchematicNotFoundException;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class FlageditorCommand extends BasicArmCommand {

    private final String rootCommand = "flageditor";
    private final String regex_with_args = "(?i)flageditor [^;\n ]+";
    private final String regex = "(?i)flageditor";
    private final List<String> usage = new ArrayList<>(Arrays.asList("flageditor [REGION]", "flageditor"));

    @Override
    public boolean matchesRegex(String command) {
        return command.matches(this.regex) || command.matches(this.regex_with_args);
    }

    @Override
    public String getRootCommand() {
        return this.rootCommand;
    }

    @Override
    public List<String> getUsage() {
        return this.usage;
    }

    @Override
    public boolean runCommand(CommandSender sender, Command cmd, String commandsLabel, String[] args, String allargs) throws InputException {
        if (!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }
        Player player = (Player) sender;

        if(!(sender.hasPermission(Permission.MEMBER_FLAGEDITOR) || sender.hasPermission(Permission.ADMIN_FLAGEDITOR))){
            throw new InputException(sender, Messages.NO_PERMISSION);
        }

        Region selRegion;
        if(allargs.matches(this.regex)) {
            selRegion = AdvancedRegionMarket.getRegionManager().getRegionAtPositionOrNameCommand(player, "");
        } else {
            selRegion = AdvancedRegionMarket.getRegionManager().getRegionAtPositionOrNameCommand(player, args[1]);
        }

        if(selRegion == null) {
            throw new InputException(sender, Messages.REGION_DOES_NOT_EXIST);
        }
        if(!(selRegion.getRegion().hasOwner(player.getUniqueId()) || player.hasPermission(Permission.ADMIN_FLAGEDITOR))) {
            throw new InputException(player, Messages.REGION_NOT_OWN);
        }

        Gui.openFlagEditor(player, selRegion, 0, (p) -> {
            p.closeInventory();
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> returnme = new ArrayList<>();

        if(args.length >= 1) {
            if(this.rootCommand.startsWith(args[0])) {
                if(player.hasPermission(Permission.ADMIN_FLAGEDITOR) || player.hasPermission(Permission.MEMBER_FLAGEDITOR)) {
                    if(args.length == 1) {
                        returnme.add(this.rootCommand);
                    } else if(args.length == 2 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        PlayerRegionRelationship playerRegionRelationship = null;
                        if(player.hasPermission(Permission.ADMIN_FLAGEDITOR)) {
                            playerRegionRelationship = PlayerRegionRelationship.ALL;
                        } else {
                            playerRegionRelationship = PlayerRegionRelationship.MEMBER_OR_OWNER;
                        }
                        returnme.addAll(AdvancedRegionMarket.getRegionManager().completeTabRegions(player, args[1], playerRegionRelationship, true,true));
                    }
                }
            }
        }
        return returnme;
    }
}