package net.alex9849.arm.presets.commands;

import net.alex9849.arm.Permission;
import net.alex9849.arm.exceptions.InputException;
import net.alex9849.arm.presets.presets.Preset;
import net.alex9849.arm.presets.presets.PresetType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfoCommand extends PresetOptionModifyCommand<Object> {

    public InfoCommand(PresetType presetType) {
        super("info", Arrays.asList(Permission.ADMIN_PRESET_INFO), presetType);
    }

    @Override
    protected Object getSettingsFromString(CommandSender sender, String setting) throws InputException {
        return null;
    }

    @Override
    protected void applySetting(CommandSender sender, Preset object, Object setting) throws InputException {
        object.getPresetInfo(sender);
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String settings) {
        return new ArrayList<>();
    }
}
