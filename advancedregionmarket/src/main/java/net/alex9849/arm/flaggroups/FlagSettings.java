package net.alex9849.arm.flaggroups;

import com.sk89q.worldguard.protection.flags.Flag;
import net.alex9849.arm.regions.SellType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FlagSettings {
    private Flag flag;
    private boolean editable;
    private String settings;
    private Set<SellType> applyTo;
    private List<String> guidescription;

    public FlagSettings(Flag flag, boolean editable, String settings, Set<SellType> applyTo, List<String> guidescription) {
        this.flag = flag;
        this.editable = editable;
        this.settings = settings;
        this.applyTo = applyTo;
        this.guidescription = new ArrayList<>(guidescription);
    }

    public Flag getFlag() {
        return flag;
    }

    public List<String> getGuidescription() {
        return new ArrayList<>(this.guidescription);
    }

    public boolean isEditable() {
        return editable;
    }

    public Set<SellType> getApplyTo() {
        return applyTo;
    }

    public String getSettings() {
        return settings;
    }
}