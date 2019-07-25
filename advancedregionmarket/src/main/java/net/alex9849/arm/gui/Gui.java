package net.alex9849.arm.gui;

import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldguard.protection.flags.*;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.ArmSettings;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.entitylimit.EntityLimit;
import net.alex9849.arm.flaggroups.FlagGroup;
import net.alex9849.arm.gui.chathandler.GuiChatInputListener;
import net.alex9849.arm.regionkind.RegionKind;
import net.alex9849.arm.util.MaterialFinder;
import net.alex9849.arm.util.Tuple;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.arm.regions.*;
import net.alex9849.arm.limitgroups.LimitGroup;
import net.alex9849.exceptions.SchematicNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.logging.Level;

public class Gui implements Listener {
    private static Material REGION_OWNER_ITEM = MaterialFinder.getGuiRegionOwnerItem();
    private static Material REGION_MEMBER_ITEM = MaterialFinder.getGuiRegionMemberItem();
    private static Material REGION_FINDER_ITEM = MaterialFinder.getGuiRegionFinderItem();
    private static Material GO_BACK_ITEM = MaterialFinder.getGuiGoBackItem();
    private static Material WARNING_YES_ITEM = MaterialFinder.getGuiWarningYesItem();
    private static Material WARNING_NO_ITEM = MaterialFinder.getGuiWarningNoItem();
    private static Material TP_ITEM = MaterialFinder.getGuiTpItem();
    private static Material SELL_REGION_ITEM = MaterialFinder.getGuiSellRegionItem();
    private static Material RESET_ITEM = MaterialFinder.getGuiResetItem();
    private static Material EXTEND_ITEM = MaterialFinder.getGuiExtendItem();
    private static Material INFO_ITEM = MaterialFinder.getGuiInfoItem();
    private static Material PROMOTE_MEMBER_TO_OWNER_ITEM = MaterialFinder.getGuiPromoteMemberToOwnerItem();
    private static Material REMOVE_MEMBER_ITEM = MaterialFinder.getGuiRemoveMemberItem();
    private static Material CONTRACT_ITEM = MaterialFinder.getGuiContractItem();
    private static Material FILL_ITEM = MaterialFinder.getGuiFillItem();
    private static Material SUBREGION_ITEM = MaterialFinder.getGuiSubregionItem();
    private static Material DELETE_ITEM = MaterialFinder.getGuiDeleteItem();
    private static Material TELEPORT_TO_SIGN_ITEM = MaterialFinder.getGuiTeleportToSignItem();
    private static Material TELEPORT_TO_REGION_ITEM = MaterialFinder.getGuiTeleportToRegionItem();
    private static Material NEXT_PAGE_ITEM = MaterialFinder.getGuiNextPageItem();
    private static Material PREV_PAGE_ITEM = MaterialFinder.getGuiPrevPageItem();
    private static Material HOTEL_SETTING_ITEM = MaterialFinder.getGuiHotelSettingItem();
    private static Material UNSELL_ITEM = MaterialFinder.getGuiUnsellItem();


    public static void openARMGui(Player player) {
        GuiInventory menu = new GuiInventory(9, Messages.GUI_MAIN_MENU_NAME);
        FileConfiguration config = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getConfig();

        int itemcounter = 0;
        int actitem = 1;

        if(config.getBoolean("GUI.DisplayRegionOwnerButton")){
            itemcounter++;
        }
        if(config.getBoolean("GUI.DisplayRegionMemberButton")){
            itemcounter++;
        }
        if(config.getBoolean("GUI.DisplayRegionFinderButton") && player.hasPermission(Permission.MEMBER_REGIONFINDER)){
            itemcounter++;
        }


        if(config.getBoolean("GUI.DisplayRegionOwnerButton")){
            ClickItem regionMenu = new ClickItem(new ItemStack(Gui.REGION_OWNER_ITEM), Messages.GUI_MY_OWN_REGIONS).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openRegionOwnerGui(player, true);
                }
            });
            menu.addIcon(regionMenu, getPosition(actitem, itemcounter));
            actitem++;
            if(itemcounter == 1) {
                Gui.openRegionOwnerGui(player, false);
            }
        }
        if(config.getBoolean("GUI.DisplayRegionMemberButton")){
            ClickItem regionMemberMenu = new ClickItem(new ItemStack(Gui.REGION_MEMBER_ITEM), Messages.GUI_MY_MEMBER_REGIONS).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openRegionMemberGui(player, true);
                }
            });

            menu.addIcon(regionMemberMenu, getPosition(actitem, itemcounter));
            actitem++;
            if(itemcounter == 1) {
                Gui.openRegionMemberGui(player, false);
            }
        }
        if(config.getBoolean("GUI.DisplayRegionFinderButton") && player.hasPermission(Permission.MEMBER_REGIONFINDER)){
            ClickItem regionfinder = new ClickItem(new ItemStack(Gui.REGION_FINDER_ITEM), Messages.GUI_SEARCH_FREE_REGION).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openRegionFinder(player, true);
                }
            });

            menu.addIcon(regionfinder, getPosition(actitem, itemcounter));
            actitem++;
            if(itemcounter == 1) {
                Gui.openRegionFinder(player, false);
            }
        }

        menu = Gui.placeFillItems(menu);

        if(itemcounter != 1) {
            player.openInventory(menu.getInventory());
        }
    }

    public static void openRegionOwnerGui(Player player, Boolean withGoBack) {
        List<Region> regions = AdvancedRegionMarket.getRegionManager().getRegionsByOwner(player.getUniqueId());
        List<ClickItem> clickItems = new ArrayList<>();

        for (int i = 0; i < regions.size(); i++) {
            ItemStack regionItem = Gui.getRegionDisplayItem(regions.get(i), Messages.GUI_RENT_REGION_LORE, new ArrayList<>(), Messages.GUI_CONTRACT_REGION_LORE);
            int finalI = i;
            ClickItem clickItem = new ClickItem(regionItem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openRegionOwnerManager(player, regions.get(finalI));
                }
            });
            clickItems.add(clickItem);
        }

        ClickAction goBackAction = null;

        if(withGoBack) {
            goBackAction = new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openARMGui(player);
                }
            };
        }

        Gui.openInfiniteGuiList(player, clickItems, 0, Messages.GUI_OWN_REGIONS_MENU_NAME, goBackAction);
    }

    public static void openRegionOwnerManager(Player player, Region region) {
        int itemcounter = 2;
        int actitem = 1;

        if(player.hasPermission(Permission.MEMBER_TP)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_SELLBACK)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_RESETREGIONBLOCKS) && region.isUserResettable()){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_ENTITYLIMIT_CHECK)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_INFO)){
            itemcounter++;
        }
        if(Permission.hasAnySubregionPermission(player) && region.isAllowSubregions()){
            itemcounter++;
        }
        //TODO change to region flageditor permission
        if(true) {
            itemcounter++;
        }
        if(region instanceof RentRegion) {
            //Extend button
            itemcounter++;
        }
        if(region instanceof ContractRegion) {
            //Terminate Button
            itemcounter++;
        }


        GuiInventory inv = new GuiInventory(9 , region.getRegion().getId());

        ItemStack membersitem = new ItemStack(MaterialFinder.getPlayerHead(), 1, (short) 3);
        SkullMeta membersitemmeta = (SkullMeta) membersitem.getItemMeta();
        membersitemmeta.setOwner(player.getDisplayName());
        membersitemmeta.setDisplayName(Messages.GUI_MEMBERS_BUTTON);
        membersitem.setItemMeta(membersitemmeta);
        ClickItem membersicon = new ClickItem(membersitem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                Gui.openMemberList(player, region);
            }
        });
        inv.addIcon(membersicon, getPosition(actitem, itemcounter));

        actitem++;

        if(Permission.hasAnySubregionPermission(player) && region.isAllowSubregions()){
            ClickItem teleportericon = new ClickItem(new ItemStack(Gui.SUBREGION_ITEM), Messages.GUI_SUBREGION_ITEM_BUTTON, new ArrayList<>()).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openSubregionList(player, region);
                }
            });
            inv.addIcon(teleportericon, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_TP)){
            ClickItem teleportericon = new ClickItem(new ItemStack(Gui.TP_ITEM), Messages.GUI_TELEPORT_TO_REGION_BUTTON, Messages.GUI_TELEPORT_TO_REGION_BUTTON_LORE);
            teleportericon = teleportericon.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Teleporter.teleport(player, region);
                    player.closeInventory();
                }
            });
            inv.addIcon(teleportericon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_RESETREGIONBLOCKS) && region.isUserResettable()) {
            List<String> message = new ArrayList<>(Messages.GUI_RESET_REGION_BUTTON_LORE);
            for (int i = 0; i < message.size(); i++) {
                message.set(i, message.get(i).replace("%days%", Region.getResetCooldown() + ""));
            }
            ClickItem reseticon = new ClickItem(new ItemStack(Gui.RESET_ITEM), Messages.GUI_RESET_REGION_BUTTON, message);
            reseticon = reseticon.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    if(region.timeSinceLastReset() >= Region.getResetCooldown()){
                        Gui.openRegionResetWarning(player, region, true);
                    } else {
                        String message = region.getConvertedMessage(Messages.RESET_REGION_COOLDOWN_ERROR);
                        throw new InputException(player, message);
                    }
                }
            });
            inv.addIcon(reseticon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_SELLBACK)) {
            List<String> message = new ArrayList<>(Messages.GUI_USER_SELL_BUTTON_LORE);
            for (int i = 0; i < message.size(); i++) {
                message.set(i, region.getConvertedMessage(message.get(i)));
            }
            ClickItem reseticon = new ClickItem(new ItemStack(Gui.SELL_REGION_ITEM), Messages.GUI_USER_SELL_BUTTON, message).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openSellWarning(player, region, true);
                }
            });
            inv.addIcon(reseticon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(true) {
            ClickItem flagEditorItem = new ClickItem(new ItemStack(MaterialFinder.getGuiFlageditorItem()), "FlagEditor").addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Map<Flag, Tuple<String, Boolean>> flagMap = region.getFlagGroup().getFlagMapSold();
                    List<Flag> flagList = new ArrayList<>();
                    for(Flag flag : flagMap.keySet()) {
                        if(flagMap.get(flag).getValue2()) {
                            flagList.add(flag);
                        }
                    }
                    Gui.openFlagEditor(player, region, flagList, 0, (p) -> {
                        openRegionOwnerManager(player, region);
                    });
                }
            });
            inv.addIcon(flagEditorItem, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(region instanceof RentRegion) {
            List<String> extendmessage = new ArrayList<>(Messages.GUI_EXTEND_BUTTON_LORE);
            for (int i = 0; i < extendmessage.size(); i++) {
                extendmessage.set(i, region.getConvertedMessage(extendmessage.get(i)));
            }
            ClickItem extendicon = new ClickItem(new ItemStack(Gui.EXTEND_ITEM), Messages.GUI_EXTEND_BUTTON, extendmessage).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    region.buy(player);
                    Gui.openRegionOwnerManager(player, region);
                }
            });
            inv.addIcon(extendicon, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(region instanceof ContractRegion) {
            ContractRegion cregion = (ContractRegion) region;
            List<String> extendmessage = new ArrayList<>(Messages.GUI_CONTRACT_ITEM_LORE);
            for (int i = 0; i < extendmessage.size(); i++) {
                extendmessage.set(i, region.getConvertedMessage(extendmessage.get(i)));
            }
            ClickItem extendicon = new ClickItem(new ItemStack(Gui.CONTRACT_ITEM), Messages.GUI_CONTRACT_ITEM, extendmessage).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    cregion.changeTerminated(player);
                    Gui.openRegionOwnerManager(player, region);
                }
            });
            inv.addIcon(extendicon, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_ENTITYLIMIT_CHECK)) {
            ClickItem infoicon = new ClickItem(getEntityLimtGroupItem(region)).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    openRegionOwnerManager(player, region);
                    net.alex9849.arm.entitylimit.commands.InfoCommand.sendInfoToPlayer(player, region.getEntityLimitGroup());
                }
            });
            inv.addIcon(infoicon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_INFO)) {
            ClickItem infoicon = new ClickItem(new ItemStack(Gui.INFO_ITEM), Messages.GUI_SHOW_INFOS_BUTTON).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    region.regionInfo(player);
                    player.closeInventory();
                }
            });
            inv.addIcon(infoicon, getPosition(actitem, itemcounter));

            actitem++;
        }

        ClickItem gobackicon = new ClickItem(new ItemStack(Gui.GO_BACK_ITEM), Messages.GUI_GO_BACK).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                Gui.openRegionOwnerGui(player, isMainPageMultipleItems());
            }
        });
        inv.addIcon(gobackicon, getPosition(actitem, itemcounter));

        actitem++;

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openFlagEditor(Player player, Region region, List<Flag> editableFlags, int start, ClickAction goBackAction) {
        int invsize = ((editableFlags.size() * 9) - (start * 9) < 54) ? ((editableFlags.size() - start) * 9 + 9) : 54;
        //TODO incude in messages yml
        GuiInventory guiInventory = new GuiInventory(invsize, "Flag Editor");

        for(int i = start; (i - start) * 9 < (invsize - 9); i++) {
            Flag rgFlag = editableFlags.get(i);
            int invIndex = (i - start) * 9;

            class GroupFlagSetter implements ClickAction {
                private String input;
                private ClickAction afterFlagSetAction;

                GroupFlagSetter(String input, ClickAction afterFlagSetAction) {
                    this.input = input;
                    this.afterFlagSetAction = afterFlagSetAction;
                }

                @Override
                public void execute(Player player) throws InputException {
                    if(region.getRegion().getFlagSetting(rgFlag) == null) {
                        throw new InputException(player, "Flag not activated!");
                    }
                    try {
                        RegionGroup groupFlag = getParsedSettingsObject();
                        if(rgFlag.getRegionGroupFlag().getDefault() == groupFlag) {
                            region.getRegion().deleteFlags(rgFlag.getRegionGroupFlag());
                        } else {
                            region.getRegion().setFlag(rgFlag.getRegionGroupFlag(), groupFlag);
                        }
                        player.sendMessage(Messages.PREFIX + "Flag has been updated!");
                        afterFlagSetAction.execute(player);
                    } catch (InvalidFlagFormat invalidFlagFormat) {
                        //Todo
                        player.sendMessage("Could not modify flag " + rgFlag.getName() + "!");
                        Bukkit.getLogger().info("Could not modify flag " + rgFlag.getName() + " via player flageditor!");
                    }
                }

                private RegionGroup getParsedSettingsObject() throws InvalidFlagFormat {
                    return AdvancedRegionMarket.getWorldGuardInterface().parseFlagInput(rgFlag.getRegionGroupFlag(), region.getConvertedMessage(this.input));
                }

                public boolean isInputSelected() {
                    try {
                        RegionGroup settingsObj = getParsedSettingsObject();
                        Object regionFlagSetting = region.getRegion().getFlagSetting(rgFlag.getRegionGroupFlag());
                        if(region.getRegion().getFlagSetting(rgFlag) == null) {
                            return false;
                        }
                        if(regionFlagSetting == settingsObj) {
                            return true;
                        } else {
                            if(rgFlag.getRegionGroupFlag().getDefault() == settingsObj && regionFlagSetting == null) {
                                return true;
                            }
                            return false;
                        }
                    } catch (InvalidFlagFormat e) {
                        return false;
                    }
                }

            }

            ClickItem flagItem = new ClickItem(new ItemStack(Material.GRASS_BLOCK), rgFlag.getName());
            guiInventory.addIcon(flagItem, invIndex);

            ClickItem[] flagStateButtons = getFlagSettingItem(rgFlag, region, (p) -> {
                openFlagEditor(p, region, editableFlags, start, goBackAction);
            });

            if(flagStateButtons.length > 0) {
                guiInventory.addIcon(flagStateButtons[0], invIndex + 1);
            }
            if(flagStateButtons.length > 1) {
                guiInventory.addIcon(flagStateButtons[1], invIndex + 2);
            }

            //Add Materials to MaterialHandler
            ClickItem deleteButton = new ClickItem(new ItemStack(MaterialFinder.getGuiFlagRemoveItem()), "Delete flag").addClickAction((pl -> {
                region.getRegion().deleteFlags(rgFlag);
                openFlagEditor(pl, region, editableFlags, start, goBackAction);
                pl.sendMessage(Messages.PREFIX + "Flag has been deleted!");
            }));
            guiInventory.addIcon(deleteButton, invIndex + 3);

            ClickAction afterFlagSetAction = (pl) -> {
                openFlagEditor(pl, region, editableFlags, start, goBackAction);
            };

            GroupFlagSetter gfsAllButton = new GroupFlagSetter("all", afterFlagSetAction);
            ClickItem allButton = new ClickItem(gfsAllButton.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagGroupSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagGroupNotSelectedItem()), "Set for all").addClickAction(gfsAllButton);
            guiInventory.addIcon(allButton, invIndex + 4);

            GroupFlagSetter gfsMembersButton = new GroupFlagSetter("members", afterFlagSetAction);
            ClickItem membersButton = new ClickItem(gfsMembersButton.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagGroupSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagGroupNotSelectedItem()), "Set for members").addClickAction(gfsMembersButton);
            guiInventory.addIcon(membersButton, invIndex + 5);

            GroupFlagSetter gfsOwnersButton = new GroupFlagSetter("owners", afterFlagSetAction);
            ClickItem ownersButton = new ClickItem(gfsOwnersButton.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagGroupSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagGroupNotSelectedItem()), "Set for owners").addClickAction(gfsOwnersButton);
            guiInventory.addIcon(ownersButton, invIndex + 6);

            GroupFlagSetter gfsNonMembersButton = new GroupFlagSetter("non_members", afterFlagSetAction);
            ClickItem nonMembersButton = new ClickItem(gfsNonMembersButton.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagGroupSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagGroupNotSelectedItem()), "Set for non members").addClickAction(gfsNonMembersButton);
            guiInventory.addIcon(nonMembersButton, invIndex + 7);

            GroupFlagSetter gfsNonOwnersButton = new GroupFlagSetter("non_owners", afterFlagSetAction);
            ClickItem nonOwnersButton = new ClickItem(gfsNonOwnersButton.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagGroupSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagGroupNotSelectedItem()), "Set for non owners").addClickAction(gfsNonOwnersButton);
            guiInventory.addIcon(nonOwnersButton, invIndex + 8);
        }

        if(start != 0) {
            final int newstart;
            newstart = (start - 5 < 0)? 0:start - 5;
            ClickItem prevButton = new ClickItem(new ItemStack(Gui.PREV_PAGE_ITEM), Messages.GUI_PREV_PAGE).addClickAction((p) -> {
               openFlagEditor(player, region, editableFlags, newstart, goBackAction);
            });
            guiInventory.addIcon(prevButton, guiInventory.getSize() - 9);
        }

        if(goBackAction != null) {
            ClickItem goBackButton = new ClickItem(new ItemStack(Gui.GO_BACK_ITEM), Messages.GUI_GO_BACK).addClickAction(goBackAction);
            guiInventory.addIcon(goBackButton, guiInventory.getSize() - 5);
        }

        if(editableFlags.size() >= start + 5) {
            ClickItem prevButton = new ClickItem(new ItemStack(Gui.NEXT_PAGE_ITEM), Messages.GUI_NEXT_PAGE).addClickAction((p) -> {
                openFlagEditor(player, region, editableFlags, start + 5, goBackAction);
            });
            guiInventory.addIcon(prevButton, guiInventory.getSize() - 1);
        }

        guiInventory = Gui.placeFillItems(guiInventory);
        player.openInventory(guiInventory.getInventory());
    }

    public static void openRegionMemberManager(Player player, Region region) {

        int itemcounter = 1;
        int actitem = 1;

        if(player.hasPermission(Permission.MEMBER_TP)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_INFO)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_ENTITYLIMIT_CHECK)) {
            itemcounter++;
        }

        GuiInventory inv = new GuiInventory(9 , region.getRegion().getId());

        if(player.hasPermission(Permission.MEMBER_TP)){
            ItemStack teleporteritem = new ItemStack(Gui.TP_ITEM);
            ItemMeta teleporteritemmeta = teleporteritem.getItemMeta();
            teleporteritemmeta.setDisplayName(Messages.GUI_TELEPORT_TO_REGION_BUTTON);
            List<String> lore = new ArrayList<>(Messages.GUI_TELEPORT_TO_REGION_BUTTON_LORE);
            for(String lorestring : lore) {
                lorestring = region.getConvertedMessage(lorestring);
            }
            teleporteritemmeta.setLore(lore);
            teleporteritem.setItemMeta(teleporteritemmeta);
            ClickItem teleportericon = new ClickItem(teleporteritem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Teleporter.teleport(player, region);
                    player.closeInventory();
                }
            });
            inv.addIcon(teleportericon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_ENTITYLIMIT_CHECK)) {
            ClickItem infoicon = new ClickItem(getEntityLimtGroupItem(region)).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    openRegionMemberManager(player, region);
                    net.alex9849.arm.entitylimit.commands.InfoCommand.sendInfoToPlayer(player, region.getEntityLimitGroup());
                }
            });
            inv.addIcon(infoicon, getPosition(actitem, itemcounter));

            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_INFO)){
            ItemStack infoitem = new ItemStack(Gui.INFO_ITEM);
            ItemMeta infoitemmeta = infoitem.getItemMeta();
            infoitemmeta.setDisplayName(Messages.GUI_SHOW_INFOS_BUTTON);
            infoitem.setItemMeta(infoitemmeta);
            ClickItem infoicon = new ClickItem(infoitem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    region.regionInfo(player);
                    player.closeInventory();
                }
            });
            inv.addIcon(infoicon, getPosition(actitem, itemcounter));

            actitem++;
        }

        ItemStack gobackitem = new ItemStack(Gui.GO_BACK_ITEM);
        ItemMeta gobackitemmeta = gobackitem.getItemMeta();
        gobackitemmeta.setDisplayName(Messages.GUI_GO_BACK);
        gobackitem.setItemMeta(gobackitemmeta);
        ClickItem gobackicon = new ClickItem(gobackitem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                Gui.openRegionMemberGui(player, isMainPageMultipleItems());
            }
        });
        inv.addIcon(gobackicon, getPosition(actitem, itemcounter));
        actitem++;

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openSubregionList(Player player, Region region) {
        List<ClickItem> clickItems = new ArrayList<>();
        for(Region subregion : region.getSubregions()) {
            ItemStack subregionItem = Gui.getRegionDisplayItem(subregion, Messages.GUI_SUBREGION_REGION_INFO_RENT, Messages.GUI_SUBREGION_REGION_INFO_SELL, Messages.GUI_SUBREGION_REGION_INFO_CONTRACT);
            ClickItem subregionClickItem = new ClickItem(subregionItem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openSubregionManager(player, subregion, region);
                }
            });
            clickItems.add(subregionClickItem);
        }
        if(clickItems.size() == 0) {
            List<String> lore = new ArrayList<>(Messages.GUI_SUBREGION_MANAGER_NO_SUBREGION_ITEM_LORE);
            ClickItem infoItem = new ClickItem(new ItemStack(Gui.INFO_ITEM), Messages.GUI_SUBREGION_MANAGER_NO_SUBREGION_ITEM, lore);
            clickItems.add(infoItem);
        }
        Gui.openInfiniteGuiList(player, clickItems, 0, Messages.GUI_SUBREGION_LIST_MENU_NAME, new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                Gui.openRegionOwnerManager(player, region);
            }
        });
    }

    public static void openSubregionManager(Player player, Region region, Region parentRegion) {

        GuiInventory inv = new GuiInventory(9, region.getRegion().getId());

        int itemcounter = 1;
        int actitem = 1;

        if(player.hasPermission(Permission.SUBREGION_CHANGE_IS_HOTEL)) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.SUBREGION_TP)) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_INFO)) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.SUBREGION_RESETREGIONBLOCKS) && region.isUserResettable()) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.SUBREGION_UNSELL)) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.SUBREGION_DELETE_AVAILABLE) || player.hasPermission(Permission.SUBREGION_DELETE_SOLD)) {
            itemcounter++;
        }

        if(player.hasPermission(Permission.SUBREGION_CHANGE_IS_HOTEL)) {
            List<String> message = new ArrayList<>(Messages.GUI_SUBREGION_HOTEL_BUTTON_LORE);
            for(int j = 0; j < message.size(); j++) {
                message.set(j, region.getConvertedMessage(message.get(j)));
            }
            ClickItem isHotelItem = new ClickItem(new ItemStack(Gui.HOTEL_SETTING_ITEM), Messages.GUI_SUBREGION_HOTEL_BUTTON, message);
            isHotelItem= isHotelItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    region.setHotel(!region.isHotel());
                    Gui.openSubregionManager(player, region, parentRegion);
                }
            });
            inv.addIcon(isHotelItem, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(player.hasPermission(Permission.SUBREGION_TP)) {
            ClickItem tpItem = new ClickItem(new ItemStack(Gui.TP_ITEM), Messages.GUI_TELEPORT_TO_REGION_BUTTON).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Teleporter.teleport(player, region);
                    player.closeInventory();
                }
            });
            inv.addIcon(tpItem, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_INFO)) {
            ClickItem infoItem = new ClickItem(new ItemStack(Gui.INFO_ITEM), Messages.GUI_SHOW_INFOS_BUTTON).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    region.regionInfo(player);
                    player.closeInventory();
                }
            });
            inv.addIcon(infoItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if(player.hasPermission(Permission.SUBREGION_RESETREGIONBLOCKS) && region.isUserResettable()) {
            ClickItem resetItem = new ClickItem(new ItemStack(Gui.RESET_ITEM), Messages.GUI_RESET_REGION_BUTTON);
            resetItem = resetItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openWarning(player, Messages.GUI_RESET_REGION_WARNING_NAME, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            try {
                                region.resetBlocks();
                                player.sendMessage(Messages.PREFIX + Messages.COMPLETE);
                            } catch (SchematicNotFoundException e) {
                                player.sendMessage(Messages.PREFIX + Messages.SCHEMATIC_NOT_FOUND_ERROR_USER.replace("%regionid%", e.getRegion().getId()));
                                Bukkit.getLogger().log(Level.WARNING, "Could not find schematic file for region " + region.getRegion().getId() + "in world " + region.getRegionworld().getName());
                            }
                            player.closeInventory();
                        }
                    }, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openSubregionManager(player, region, parentRegion);
                        }
                    });
                }
            });
            inv.addIcon(resetItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if(player.hasPermission(Permission.SUBREGION_UNSELL)) {
            List<String> sendmsg = new ArrayList<>(Messages.UNSELL_REGION_BUTTON_LORE);
            for(int i = 0; i < sendmsg.size(); i++) {
                sendmsg.set(i, region.getConvertedMessage(sendmsg.get(i)));
            }
            ClickItem unsellItem = new ClickItem(new ItemStack(Gui.UNSELL_ITEM), Messages.UNSELL_REGION_BUTTON, sendmsg);
            unsellItem = unsellItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openWarning(player, Messages.UNSELL_REGION_WARNING_NAME, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            region.unsell();
                            player.closeInventory();
                            player.sendMessage(Messages.PREFIX + Messages.REGION_NOW_AVIABLE);
                        }
                    }, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openSubregionManager(player, region, parentRegion);
                        }
                    });
                }
            });
            inv.addIcon(unsellItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if(player.hasPermission(Permission.SUBREGION_DELETE_AVAILABLE) || player.hasPermission(Permission.SUBREGION_DELETE_SOLD)) {
            ClickItem deleteItem = new ClickItem(new ItemStack(Gui.DELETE_ITEM), Messages.GUI_SUBREGION_DELETE_REGION_BUTTON);
            deleteItem = deleteItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    if(region.isSold() && (!player.hasPermission(Permission.SUBREGION_DELETE_SOLD))) {
                        throw new InputException(player, Messages.NOT_ALLOWED_TO_REMOVE_SUB_REGION_SOLD);
                    }
                    if((!region.isSold()) && (!player.hasPermission(Permission.SUBREGION_DELETE_AVAILABLE))) {
                        throw new InputException(player, Messages.NOT_ALLOWED_TO_REMOVE_SUB_REGION_AVAILABLE);
                    }
                    Gui.openWarning(player, Messages.DELETE_REGION_WARNING_NAME, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            region.delete();
                            player.closeInventory();
                            player.sendMessage(Messages.PREFIX + Messages.REGION_DELETED);
                        }
                    }, new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openSubregionManager(player, region, parentRegion);
                        }
                    });
                }
            });
            inv.addIcon(deleteItem, getPosition(actitem, itemcounter));
            actitem++;
        }

        ClickItem goBack = new ClickItem(new ItemStack(Gui.GO_BACK_ITEM), Messages.GUI_GO_BACK).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                Gui.openSubregionList(player, parentRegion);
            }
        });
        inv.addIcon(goBack, getPosition(actitem, itemcounter));
        actitem++;


        inv = placeFillItems(inv);
        player.openInventory(inv.getInventory());
    }

    public static void openRegionFinder(Player player, Boolean withGoBack) {

        int itemcounter = 0;
        int actitem = 1;
        if(withGoBack) {
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_LIMIT)){
            itemcounter++;
        }

        if(RegionKind.DEFAULT.isDisplayInGUI()){
            itemcounter++;
        }

        if(RegionKind.SUBREGION.isDisplayInGUI() && RegionKind.hasPermission(player, RegionKind.SUBREGION)){
            itemcounter++;
        }

        for(RegionKind regionKind : AdvancedRegionMarket.getRegionKindManager()) {
            if(regionKind.isDisplayInGUI()) {
                itemcounter++;
            }
        }

        int invsize = 0;

        while (itemcounter > invsize) {
            invsize = invsize + 9;
        }

        GuiInventory inv = new GuiInventory(invsize, Messages.GUI_REGION_FINDER_MENU_NAME);
        int itempos = 0;
        if(RegionKind.DEFAULT.isDisplayInGUI()) {
            String displayName = Messages.GUI_REGIONFINDER_REGIONKIND_NAME;
            displayName = RegionKind.DEFAULT.getConvertedMessage(displayName);
            Material material = RegionKind.DEFAULT.getMaterial();
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(RegionKind.DEFAULT.getLore());
            stack.setItemMeta(meta);
            ClickItem icon = new ClickItem(stack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openRegionFinderSellTypeSelector(player, AdvancedRegionMarket.getRegionManager().getFreeRegions(RegionKind.DEFAULT), new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openRegionFinder(player, withGoBack);
                        }
                    });
                }
            });
            inv.addIcon(icon, itempos);
            itempos++;
        }

        if(RegionKind.SUBREGION.isDisplayInGUI() && RegionKind.hasPermission(player, RegionKind.SUBREGION)){
            String displayName = Messages.GUI_REGIONFINDER_REGIONKIND_NAME;
            displayName = RegionKind.SUBREGION.getConvertedMessage(displayName);
            Material material = RegionKind.SUBREGION.getMaterial();
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(RegionKind.SUBREGION.getLore());
            stack.setItemMeta(meta);
            ClickItem icon = new ClickItem(stack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openRegionFinderSellTypeSelector(player, AdvancedRegionMarket.getRegionManager().getFreeRegions(RegionKind.SUBREGION), new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openRegionFinder(player, withGoBack);
                        }
                    });
                }
            });
            inv.addIcon(icon, itempos);
            itempos++;
        }


        for(RegionKind regionKind : AdvancedRegionMarket.getRegionKindManager()) {
            if(regionKind.isDisplayInGUI()) {
                String displayName = Messages.GUI_REGIONFINDER_REGIONKIND_NAME;
                displayName = regionKind.getConvertedMessage(displayName);
                Material material = regionKind.getMaterial();
                if(RegionKind.hasPermission(player, regionKind)){
                    ItemStack stack = new ItemStack(material);
                    ItemMeta meta = stack.getItemMeta();
                    meta.setDisplayName(displayName);
                    meta.setLore(regionKind.getLore());
                    stack.setItemMeta(meta);
                    ClickItem icon = new ClickItem(stack).addClickAction(new ClickAction() {
                        @Override
                        public void execute(Player player) throws InputException {
                            Gui.openRegionFinderSellTypeSelector(player, AdvancedRegionMarket.getRegionManager().getFreeRegions(regionKind), new ClickAction() {
                                @Override
                                public void execute(Player player) throws InputException {
                                    Gui.openRegionFinder(player, withGoBack);
                                }
                            });
                        }
                    });
                    inv.addIcon(icon, itempos);
                    itempos++;
                }
            }

        }

        if(player.hasPermission(Permission.MEMBER_LIMIT)){
            ItemStack goBack = new ItemStack(Gui.INFO_ITEM);
            ItemMeta goBackMeta = goBack.getItemMeta();
            goBackMeta.setDisplayName(Messages.GUI_MY_LIMITS_BUTTON);
            goBack.setItemMeta(goBackMeta);

            ClickItem gobackButton = new ClickItem(goBack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    LimitGroup.getLimitChat(player);
                }
            });
            int pos = 1;
            if(withGoBack) {
                pos = 2;
            }
            inv.addIcon(gobackButton, (invsize - pos));
        }


        if(withGoBack) {
            ItemStack goBack = new ItemStack(Gui.GO_BACK_ITEM);
            ItemMeta goBackMeta = goBack.getItemMeta();
            goBackMeta.setDisplayName(Messages.GUI_GO_BACK);
            goBack.setItemMeta(goBackMeta);

            ClickItem gobackButton = new ClickItem(goBack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openARMGui(player);
                }
            });

            inv.addIcon(gobackButton, (invsize - 1));
        }

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());

    }

    public static void openRegionFinderSellTypeSelector(Player player, List<Region> regions, ClickAction goBackAction) throws InputException {
        GuiInventory inv = new GuiInventory(9, Messages.GUI_REGION_FINDER_MENU_NAME);
        List<ClickItem> sellRegionClickItems = new ArrayList<>();
        List<ClickItem> rentRegionClickItems = new ArrayList<>();
        List<ClickItem> contractRegionClickItems = new ArrayList<>();
        ClickItem sellRegionList = null;
        ClickItem rentRegionList = null;
        ClickItem contractRegionList = null;
        int itemcounter = 0;
        int actitem = 1;

        regions.sort(new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                if(o1.getPrice() > o2.getPrice()) {
                    return 1;
                }
                if(o1.getPrice() == o2.getPrice()) {
                    return 0;
                }
                return -1;
            }
        });


        for(Region region : regions) {
            ItemStack itemStack = getRegionDisplayItem(region, Messages.GUI_REGIONFINDER_REGION_INFO_RENT, Messages.GUI_REGIONFINDER_REGION_INFO_SELL, Messages.GUI_REGIONFINDER_REGION_INFO_CONTRACT);
            ClickItem clickItem = new ClickItem(itemStack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openRegionFinderTeleportLocationSeceltor(player, region);
                }
            });
            if(region instanceof SellRegion) {
                sellRegionClickItems.add(clickItem);
            } else if(region instanceof RentRegion) {
                rentRegionClickItems.add(clickItem);
            } else if(region instanceof ContractRegion) {
                contractRegionClickItems.add(clickItem);
            }
        }
        if(sellRegionClickItems.size() > 0) {
            itemcounter++;
        }
        if(rentRegionClickItems.size() > 0) {
            itemcounter++;
        }
        if(contractRegionClickItems.size() > 0) {
            itemcounter++;
        }
        if(sellRegionClickItems.size() > 0) {
            ClickItem clickItem = new ClickItem(new ItemStack(MaterialFinder.getBRICKS()), Messages.SELLREGION_NAME);
            clickItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openInfiniteGuiList(player, sellRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                }
            });
            if((rentRegionClickItems.size() == 0) && (contractRegionClickItems.size() == 0)) {
                Gui.openInfiniteGuiList(player, sellRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                return;
            }
            inv.addIcon(clickItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if(rentRegionClickItems.size() > 0) {
            ClickItem clickItem = new ClickItem(new ItemStack(MaterialFinder.getBRICKS()), Messages.RENTREGION_NAME);
            clickItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openInfiniteGuiList(player, rentRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                }
            });
            if((sellRegionClickItems.size() == 0) && (contractRegionClickItems.size() == 0)) {
                Gui.openInfiniteGuiList(player, rentRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                return;
            }
            inv.addIcon(clickItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if(contractRegionClickItems.size() > 0) {
            ClickItem clickItem = new ClickItem(new ItemStack(MaterialFinder.getBRICKS()), Messages.CONTRACTREGION_NAME);
            clickItem.addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openInfiniteGuiList(player, contractRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                }
            });
            if((sellRegionClickItems.size() == 0) && (rentRegionClickItems.size() == 0)) {
                Gui.openInfiniteGuiList(player, contractRegionClickItems, 0, Messages.GUI_REGION_FINDER_MENU_NAME, goBackAction);
                return;
            }
            inv.addIcon(clickItem, getPosition(actitem, itemcounter));
            actitem++;
        }
        if((sellRegionClickItems.size() == 0) && (rentRegionClickItems.size() == 0) && (contractRegionClickItems.size() == 0)) {
            throw new InputException(player, Messages.NO_FREE_REGION_WITH_THIS_KIND);
        }
        inv = Gui.placeFillItems(inv);
        player.openInventory(inv.getInventory());
    }

    public static void openRegionFinderTeleportLocationSeceltor(Player player, Region region) throws InputException {
        if(!ArmSettings.isAllowTeleportToBuySign()) {
            region.teleport(player, false);
            return;
        }

        GuiInventory inv = new GuiInventory(9, Messages.GUI_TELEPORT_TO_SIGN_OR_REGION);
        ClickItem clickSign = new ClickItem(new ItemStack(Gui.TELEPORT_TO_SIGN_ITEM), Messages.GUI_TELEPORT_TO_SIGN);
        clickSign.addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                region.teleport(player, true);
                player.closeInventory();
            }
        });
        inv.addIcon(clickSign, getPosition(1, 2));

        ClickItem clickRegion = new ClickItem(new ItemStack(Gui.TELEPORT_TO_REGION_ITEM), Messages.GUI_TELEPORT_TO_REGION);
        clickRegion.addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                region.teleport(player, false);
                player.closeInventory();
            }
        });
        inv.addIcon(clickRegion, getPosition(2, 2));
        inv = Gui.placeFillItems(inv);
        player.openInventory(inv.getInventory());
    }

    public static void openInfiniteGuiList(Player player, List<ClickItem> clickItems, int startitem, String name, ClickAction gobackAction) {

        int invsize = 0;
        int itemsize = 0;

        while (((clickItems.size() - startitem) > itemsize) && (itemsize < 45)) {
            itemsize = itemsize + 9;
        }
        invsize = itemsize;
        if(((gobackAction != null) && (clickItems.size() >= 9)) || ((startitem + 45) < (clickItems.size() - 1)) || (startitem != 0) || (itemsize == 0)) {
            invsize = itemsize + 9;
        }

        GuiInventory inv = new GuiInventory(invsize, name);

        int pos = 0;
        for(int i = startitem; ((i < startitem + itemsize) && (i < clickItems.size())); i++) {
            inv.addIcon(clickItems.get(i), pos);
            pos++;
        }
        if(startitem != 0) {
            int newStartItem = startitem - 45;
            if(newStartItem < 0) {
                newStartItem = 0;
            }
            final int finalnewStartItem = newStartItem;
            ClickItem prevPageButton = new ClickItem(new ItemStack(Gui.PREV_PAGE_ITEM), Messages.GUI_PREV_PAGE).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openInfiniteGuiList(player, clickItems, finalnewStartItem, name, gobackAction);
                }
            });
            inv.addIcon(prevPageButton, invsize - 9);
        }
        if((startitem + 45) < clickItems.size()) {
            int newStartItem = startitem + 45;
            ClickItem nextPageButton = new ClickItem(new ItemStack(Gui.NEXT_PAGE_ITEM), Messages.GUI_NEXT_PAGE).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openInfiniteGuiList(player, clickItems, newStartItem, name, gobackAction);
                }
            });
            inv.addIcon(nextPageButton, invsize - 1);
        }
        if(gobackAction != null) {
            ClickItem gobackButton = new ClickItem(new ItemStack(Gui.GO_BACK_ITEM), Messages.GUI_GO_BACK).addClickAction(gobackAction);
            if(clickItems.size() >= 9) {
                inv.addIcon(gobackButton, invsize - 5);
            } else {
                inv.addIcon(gobackButton, 8);
            }

        }

        inv = Gui.placeFillItems(inv);
        player.openInventory(inv.getInventory());
    }

    public static void openMemberList(Player player, Region region){
        ArrayList<UUID> members = region.getRegion().getMembers();

        List<ClickItem> clickItems = new ArrayList<>();

        String invname =  Messages.GUI_MEMBER_LIST_MENU_NAME.replaceAll("%regionid%", region.getRegion().getId());

        for(int i = 0; i < members.size(); i++) {
            ItemStack membersitem = new ItemStack(MaterialFinder.getPlayerHead(), 1, (short) 3);
            SkullMeta membersitemmeta = (SkullMeta) membersitem.getItemMeta();
            if(Bukkit.getOfflinePlayer(members.get(i)).getName() != null) {
                membersitemmeta.setOwner(Bukkit.getOfflinePlayer(members.get(i)).getName());
                membersitemmeta.setDisplayName(Bukkit.getOfflinePlayer(members.get(i)).getName());
            }
            membersitem.setItemMeta(membersitemmeta);
            int finalI = i;
            ClickItem membersicon = new ClickItem(membersitem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openMemberManager(player, region, Bukkit.getOfflinePlayer(members.get(finalI)));
                }
            });
            clickItems.add(membersicon);
        }

        if(members.size() == 0){
            ItemStack info = new ItemStack(Gui.INFO_ITEM);
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.setDisplayName(Messages.GUI_OWNER_MEMBER_INFO_ITEM);
            List<String> lore = new ArrayList<>(Messages.GUI_OWNER_MEMBER_INFO_LORE);
            for(int i = 0; i < lore.size(); i++) {
                lore.set(i, region.getConvertedMessage(lore.get(i)));
            }
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
            ClickItem infoButton = new ClickItem(info);
            clickItems.add(infoButton);
        }

        Gui.openInfiniteGuiList(player, clickItems, 0, invname, new ClickAction() {
            @Override
            public void execute(Player player) {
                Gui.openRegionOwnerManager(player, region);
            }
        });

    }

    public static void openMemberManager(Player player, Region region, OfflinePlayer member) {
        GuiInventory inv = new GuiInventory(9, region.getRegion().getId() + " - " + member.getName());

        int itemcounter = 1;
        int actitem = 1;

        if(player.hasPermission(Permission.MEMBER_REMOVEMEMBER)){
            itemcounter++;
        }
        if(player.hasPermission(Permission.MEMBER_PROMOTE)){
            itemcounter++;
        }

        if(player.hasPermission(Permission.MEMBER_PROMOTE)){
            ItemStack makeOwnerItem = new ItemStack(Gui.PROMOTE_MEMBER_TO_OWNER_ITEM);
            ItemMeta makeOwnerItemMeta = makeOwnerItem.getItemMeta();
            makeOwnerItemMeta.setDisplayName(Messages.GUI_MAKE_OWNER_BUTTON);
            List<String> lore = new ArrayList<>(Messages.GUI_MAKE_OWNER_BUTTON_LORE);
            for(String lorestring : lore) {
                lorestring = region.getConvertedMessage(lorestring);
            }
            makeOwnerItemMeta.setLore(lore);
            makeOwnerItem.setItemMeta(makeOwnerItemMeta);

            ClickItem makeOwnerMenu = new ClickItem(makeOwnerItem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    Gui.openMakeOwnerWarning(player, region, member, true);
                }
            });
            inv.addIcon(makeOwnerMenu, getPosition(actitem, itemcounter));
            actitem++;
        }

        if(player.hasPermission(Permission.MEMBER_REMOVEMEMBER)){
            ItemStack removeItem = new ItemStack(Gui.REMOVE_MEMBER_ITEM);
            ItemMeta removeItemMeta = removeItem.getItemMeta();
            removeItemMeta.setDisplayName(Messages.GUI_REMOVE_MEMBER_BUTTON);
            List<String> lore = new ArrayList<>(Messages.GUI_REMOVE_MEMBER_BUTTON_LORE);
            for(String lorestring : lore) {
                lorestring = region.getConvertedMessage(lorestring);
            }
            removeItemMeta.setLore(lore);
            removeItem.setItemMeta(removeItemMeta);

            ClickItem removeMenu = new ClickItem(removeItem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    region.getRegion().removeMember(member.getUniqueId());
                    player.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_REMOVED);
                    player.closeInventory();
                }
            });
            inv.addIcon(removeMenu, getPosition(actitem, itemcounter));
            actitem++;
        }


        ItemStack goBack = new ItemStack(Gui.GO_BACK_ITEM);
        ItemMeta goBackMeta = goBack.getItemMeta();
        goBackMeta.setDisplayName(Messages.GUI_GO_BACK);
        goBack.setItemMeta(goBackMeta);

        ClickItem gobackButton = new ClickItem(goBack).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                Gui.openMemberList(player, region);
            }
        });

        inv.addIcon(gobackButton, getPosition(actitem, itemcounter));
        actitem++;

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());

    }

    public static void openMakeOwnerWarning(Player player, Region region, OfflinePlayer member, Boolean goback) {
        GuiInventory inv = new GuiInventory(9, Messages.GUI_MAKE_OWNER_WARNING_NAME);

        ItemStack yesItem = new ItemStack(Gui.WARNING_YES_ITEM);
        ItemMeta yesItemMeta = yesItem.getItemMeta();
        yesItemMeta.setDisplayName(Messages.GUI_YES);
        yesItem.setItemMeta(yesItemMeta);
        Player onlinemember = Bukkit.getPlayer(member.getUniqueId());
        ClickItem yesButton = new ClickItem(yesItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                player.closeInventory();
                if(onlinemember == null) {
                    throw new InputException(player, Messages.REGION_TRANSFER_MEMBER_NOT_ONLINE);
                }
                if(LimitGroup.isCanBuyAnother(onlinemember, region)) {
                    region.setNewOwner(onlinemember);
                    player.sendMessage(Messages.PREFIX + Messages.REGION_TRANSFER_COMPLETE_MESSAGE);
                } else {
                    throw new InputException(player, Messages.REGION_TRANSFER_LIMIT_ERROR);
                }
            }
        });
        inv.addIcon(yesButton, 0);

        ItemStack noItem = new ItemStack(Gui.WARNING_NO_ITEM);
        ItemMeta noItemMeta = noItem.getItemMeta();
        noItemMeta.setDisplayName(Messages.GUI_NO);
        noItem.setItemMeta(noItemMeta);

        ClickItem noButton = new ClickItem(noItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                if(goback) {
                    Gui.openMemberManager(player, region, member);
                } else {
                    player.closeInventory();
                }

            }
        });
        inv.addIcon(noButton, 8);

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openRegionMemberGui(Player player, Boolean withGoBack) {
        List<Region> regions = AdvancedRegionMarket.getRegionManager().getRegionsByMember(player.getUniqueId());
        List<ClickItem> clickItems = new ArrayList<>();

        for (int i = 0; i < regions.size(); i++) {
            ItemStack regionItem = Gui.getRegionDisplayItem(regions.get(i), Messages.GUI_RENT_REGION_LORE, new ArrayList<>(), Messages.GUI_CONTRACT_REGION_LORE);
            int finalI = i;
            ClickItem clickItem = new ClickItem(regionItem).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openRegionMemberManager(player, regions.get(finalI));
                }
            });
            clickItems.add(clickItem);
        }

        if(regions.size() == 0){
            ItemStack info = new ItemStack(Gui.INFO_ITEM);
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.setDisplayName(Messages.GUI_MEMBER_INFO_ITEM);
            infoMeta.setLore(Messages.GUI_MEMBER_INFO_LORE);
            info.setItemMeta(infoMeta);
            ClickItem infoButton = new ClickItem(info);
            clickItems.add(infoButton);
        }


        ClickAction goBackAction = null;

        if(withGoBack) {
            goBackAction = new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    Gui.openARMGui(player);
                }
            };
        }

        Gui.openInfiniteGuiList(player, clickItems, 0, Messages.GUI_MEMBER_REGIONS_MENU_NAME, goBackAction);
    }

    public static void openOvertakeGUI(Player player, List<Region> oldRegions){

        int invsize = 0;
        while (oldRegions.size() + 1 > invsize) {
            invsize = invsize + 9;
        }

        GuiInventory inv = new GuiInventory(invsize, Messages.GUI_TAKEOVER_MENU_NAME);
        for(int i = 0; i < oldRegions.size(); i++) {
            ItemStack stack = new ItemStack(oldRegions.get(i).getLogo());
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(oldRegions.get(i).getRegion().getId());
            List<String> message = new LinkedList<>(Messages.GUI_TAKEOVER_ITEM_LORE);
            for (int j = 0; j < message.size(); j++) {
                message.set(j, message.get(j).replace("%days%", oldRegions.get(i).getRemainingDaysTillReset() + ""));
                message.set(j, oldRegions.get(i).getConvertedMessage(message.get(j)));
            }
            meta.setLore(message);
            stack.setItemMeta(meta);
            int finalI = i;
            ClickItem icon = new ClickItem(stack).addClickAction(new ClickAction() {
                @Override
                public void execute(Player player) {
                    oldRegions.get(finalI).setNewOwner(player);
                    oldRegions.remove(finalI);
                    player.sendMessage(Messages.PREFIX + Messages.REGION_TRANSFER_COMPLETE_MESSAGE);
                    Gui.openOvertakeGUI(player, oldRegions);
                    }
                });
            inv.addIcon(icon, i);
        }

        ItemStack stack = new ItemStack(Gui.GO_BACK_ITEM);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Messages.GUI_CLOSE);
        stack.setItemMeta(meta);
        ClickItem icon = new ClickItem(stack).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                player.closeInventory();
            }
        });
        inv.addIcon(icon, invsize - 1);

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openRegionResetWarning(Player player, Region region, Boolean goBack){
        GuiInventory inv = new GuiInventory(9, Messages.GUI_RESET_REGION_WARNING_NAME);

        ItemStack yesItem = new ItemStack(Gui.WARNING_YES_ITEM);
        ItemMeta yesItemMeta = yesItem.getItemMeta();
        yesItemMeta.setDisplayName(Messages.GUI_YES);
        yesItem.setItemMeta(yesItemMeta);

        ClickItem yesButton = new ClickItem(yesItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                player.closeInventory();
                region.userBlockReset(player);
            }
        });
        inv.addIcon(yesButton, 0);

        ItemStack noItem = new ItemStack(Gui.WARNING_NO_ITEM);
        ItemMeta noItemMeta = noItem.getItemMeta();
        noItemMeta.setDisplayName(Messages.GUI_NO);
        noItem.setItemMeta(noItemMeta);

        ClickItem noButton = new ClickItem(noItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                if(goBack){
                    Gui.openRegionOwnerManager(player, region);
                } else {
                    player.closeInventory();
                }

            }
        });
        inv.addIcon(noButton, 8);

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openSellWarning(Player player, Region region, Boolean goBack){
        GuiInventory inv = new GuiInventory(9, Messages.GUI_USER_SELL_WARNING);

        ItemStack yesItem = new ItemStack(Gui.WARNING_YES_ITEM);
        ItemMeta yesItemMeta = yesItem.getItemMeta();
        yesItemMeta.setDisplayName(Messages.GUI_YES);
        yesItem.setItemMeta(yesItemMeta);

        ClickItem yesButton = new ClickItem(yesItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                player.closeInventory();
                if(region.getRegion().hasOwner(player.getUniqueId())) {
                    region.userSell(player);
                } else {
                    throw new InputException(player, Messages.REGION_NOT_OWN);
                }
            }
        });
        inv.addIcon(yesButton, 0);

        ItemStack noItem = new ItemStack(Gui.WARNING_NO_ITEM);
        ItemMeta noItemMeta = noItem.getItemMeta();
        noItemMeta.setDisplayName(Messages.GUI_NO);
        noItem.setItemMeta(noItemMeta);

        ClickItem noButton = new ClickItem(noItem).addClickAction(new ClickAction() {
            @Override
            public void execute(Player player) {
                if(goBack) {
                    Gui.openRegionOwnerManager(player, region);
                } else {
                    player.closeInventory();
                }

            }
        });
        inv.addIcon(noButton, 8);

        inv = Gui.placeFillItems(inv);

        player.openInventory(inv.getInventory());
    }

    public static void openWarning(Player player, String warning, ClickAction yesAction, ClickAction noAction) {
        GuiInventory inv = new GuiInventory(9, warning);
        ClickItem yesItem = new ClickItem(new ItemStack(Gui.WARNING_YES_ITEM), Messages.GUI_YES).addClickAction(yesAction);
        inv.addIcon(yesItem, 0);
        ClickItem noItem = new ClickItem(new ItemStack(Gui.WARNING_NO_ITEM), Messages.GUI_NO).addClickAction(noAction);
        inv.addIcon(noItem, 8);
        inv = Gui.placeFillItems(inv);
        player.openInventory(inv.getInventory());
    }

    private static ItemStack getRegionDisplayItem(Region region, List<String> rentLore, List<String> sellLore, List<String> contractLore) {
        String regionDisplayName = Messages.GUI_REGION_ITEM_NAME;
        regionDisplayName = region.getConvertedMessage(regionDisplayName);
        regionDisplayName = region.getRegionKind().getConvertedMessage(regionDisplayName);

        ItemStack stack = new ItemStack(region.getRegionKind().getMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(regionDisplayName);

        List<String> regionLore = new ArrayList<>();

        if(region instanceof RentRegion) {
            regionLore = new ArrayList<>(rentLore);
        } else if (region instanceof SellRegion) {
            regionLore = new ArrayList<>(sellLore);
        } else if (region instanceof ContractRegion) {
            regionLore = new ArrayList<>(contractLore);
        }

        for (int j = 0; j < regionLore.size(); j++) {
            regionLore.set(j, region.getConvertedMessage(regionLore.get(j)));
        }
        meta.setLore(regionLore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static boolean isMainPageMultipleItems(){
        FileConfiguration config = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getConfig();

        int itemcounter = 0;

        if(config.getBoolean("GUI.DisplayRegionOwnerButton")){
            itemcounter++;
        }
        if(config.getBoolean("GUI.DisplayRegionMemberButton")){
            itemcounter++;
        }
        if(config.getBoolean("GUI.DisplayRegionFinderButton")){
            itemcounter++;
        }
        return (itemcounter > 1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiInventory) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();

                ItemStack itemStack = event.getCurrentItem();
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    return;
                }

                GuiInventory customHolder = (GuiInventory) event.getView().getTopInventory().getHolder();

                ClickItem icon = customHolder.getIcon(event.getRawSlot());
                if (icon == null) return;

                for (ClickAction clickAction : icon.getClickActions()) {
                    try {
                        clickAction.execute(player);
                    } catch (InputException inputException) {
                        inputException.sendMessages(Messages.PREFIX);
                    }
                }
            }
        }
    }

    private static GuiInventory placeFillItems(GuiInventory inv) {
        if(Gui.FILL_ITEM != Material.AIR) {
            for(int i = 0; i < inv.getInventory().getSize(); i++) {
                if(inv.getIcon(i) == null) {
                    ClickItem fillIcon = new ClickItem(new ItemStack(Gui.FILL_ITEM), " ").addClickAction(new ClickAction() {
                        @Override
                        public void execute(Player player) {
                            return;
                        }
                    });
                    inv.addIcon(fillIcon, i);
                }
            }
            return inv;
        }
        return inv;
    }

    private static int getPosition(int itemNr, int maxItems){
        if(maxItems < itemNr || maxItems > 9){
            throw new IndexOutOfBoundsException("getPosition-Method cant handle more than max. 9 Items");
        }
        if(maxItems == 0) {
            return 4;
        }
        if(maxItems == 1){
            return 4;
        }
        else if(maxItems == 2) {
            if(itemNr == 1) {
                return 2;
            } else {
                return 6;
            }
        } else if(maxItems == 3) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 4;
            } else {
                return 8;
            }
        } else if(maxItems == 4) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 2;
            } else if(itemNr == 3){
                return 6;
            } else {
                return 8;
            }
        } else if(maxItems == 5) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 2;
            } else if(itemNr == 3){
                return 4;
            } else if(itemNr == 4){
                return 6;
            } else {
                return 8;
            }
        } else if(maxItems == 6) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 1;
            } else if(itemNr == 3){
                return 3;
            } else if(itemNr == 4){
                return 5;
            } else if(itemNr == 5){
                return 7;
            } else {
                return 8;
            }
        } else if(maxItems == 7) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 1;
            } else if(itemNr == 3){
                return 3;
            } else if(itemNr == 4){
                return 4;
            } else if(itemNr == 5){
                return 5;
            } else if(itemNr == 6){
                return 7;
            } else {
                return 8;
            }
        } else if(maxItems == 8) {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 1;
            } else if(itemNr == 3){
                return 2;
            } else if(itemNr == 4){
                return 3;
            } else if(itemNr == 5){
                return 5;
            } else if(itemNr == 6){
                return 6;
            } else if(itemNr == 7){
                return 7;
            } else {
                return 8;
            }
        } else {
            if(itemNr == 1) {
                return 0;
            } else if(itemNr == 2){
                return 1;
            } else if(itemNr == 3){
                return 2;
            } else if(itemNr == 4){
                return 3;
            } else if(itemNr == 5){
                return 4;
            } else if(itemNr == 6){
                return 5;
            } else if(itemNr == 7){
                return 6;
            } else if(itemNr == 8){
                return 7;
            } else {
                return 8;
            }
        }
    }

    public static void setRegionOwnerItem(Material regionOwnerItem){
        if(regionOwnerItem == null) {
            return;
        }
        REGION_OWNER_ITEM = regionOwnerItem;
    }

    public static void setRegionMemberItem(Material regionMemberItem) {
        if(regionMemberItem == null) {
            return;
        }
        REGION_MEMBER_ITEM = regionMemberItem;
    }

    public static void setRegionFinderItem(Material regionFinderItem) {
        if(regionFinderItem == null) {
            return;
        }
        REGION_FINDER_ITEM = regionFinderItem;
    }

    public static void setFillItem(Material fillItem) {
        if(fillItem == null) {
            return;
        }
        FILL_ITEM = fillItem;
    }

    public static void setContractItem(Material contractItem) {
        if(contractItem == null) {
            return;
        }
        CONTRACT_ITEM = contractItem;
    }

    public static void setGoBackItem(Material goBackItem) {
        if(goBackItem == null) {
            return;
        }
        GO_BACK_ITEM = goBackItem;
    }

    public static void setWarningYesItem(Material warningYesItem) {
        if(warningYesItem == null) {
            return;
        }
        WARNING_YES_ITEM = warningYesItem;
    }

    public static void setWarningNoItem(Material warningNoItem) {
        if(warningNoItem == null) {
            return;
        }
        Gui.WARNING_NO_ITEM = warningNoItem;
    }

    public static void setTpItem(Material tpItem) {
        if(tpItem == null) {
            return;
        }
        TP_ITEM = tpItem;
    }

    public static void setSellRegionItem(Material sellRegionItem) {
        if(sellRegionItem == null) {
            return;
        }
        SELL_REGION_ITEM = sellRegionItem;
    }

    public static void setResetItem(Material resetRegion) {
        if(resetRegion == null) {
            return;
        }
        RESET_ITEM = resetRegion;
    }

    public static void setExtendItem(Material extendItem) {
        if(extendItem == null) {
            return;
        }
        EXTEND_ITEM = extendItem;
    }

    public static void setInfoItem(Material infoItem) {
        if(infoItem == null) {
            return;
        }
        INFO_ITEM = infoItem;
    }

    public static void setPromoteMemberToOwnerItem(Material promoteMemberToOwnerItem) {
        if(promoteMemberToOwnerItem == null) {
            return;
        }
        PROMOTE_MEMBER_TO_OWNER_ITEM = promoteMemberToOwnerItem;
    }

    public static void setRemoveMemberItem(Material removeMemberItem) {
        if(removeMemberItem == null) {
            return;
        }
        REMOVE_MEMBER_ITEM = removeMemberItem;
    }


    public static void setSubregionItem(Material subregionItem) {
        if(subregionItem == null) {
            return;
        }
        SUBREGION_ITEM = subregionItem;
    }

    public static void setDeleteItem(Material deleteItem) {
        if(deleteItem == null) {
            return;
        }
        DELETE_ITEM = deleteItem;
    }

    public static void setTeleportToSignItem(Material teleportToSignItem) {
        if(teleportToSignItem == null) {
            return;
        }
        TELEPORT_TO_SIGN_ITEM = teleportToSignItem;
    }

    public static void setTeleportToRegionItem(Material teleportToRegionItem) {
        if(teleportToRegionItem == null) {
            return;
        }
        TELEPORT_TO_REGION_ITEM = teleportToRegionItem;
    }

    public static void setNextPageItem(Material NextPageItem) {
        if(NextPageItem == null) {
            return;
        }
        NEXT_PAGE_ITEM = NextPageItem;
    }

    public static void setPrevPageItem(Material PrevPageItem) {
        if(PrevPageItem == null) {
            return;
        }
        PREV_PAGE_ITEM = PrevPageItem;
    }

    public static void setHotelSettingItem(Material HotelSettingItem) {
        if(HotelSettingItem == null) {
            return;
        }
        HOTEL_SETTING_ITEM = HotelSettingItem;
    }

    public static void setUnsellItem(Material UnsellItem) {
        if(UnsellItem == null) {
            return;
        }
        UNSELL_ITEM = UnsellItem;
    }

    public static ItemStack getEntityLimtGroupItem(Region region) {
        ItemStack itemStack = new ItemStack(MaterialFinder.getChickenSpawnEgg());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(Messages.GUI_ENTITYLIMIT_ITEM_BUTTON);

        List<Entity> entities = region.getFilteredInsideEntities(false, true, true, false, false, true, true);
        List<String> lore = new ArrayList<>(Messages.GUI_ENTITYLIMIT_ITEM_LORE);
        List<String> limitlist = new ArrayList<>();

        String totalstatus = region.getEntityLimitGroup().getConvertedMessage(Messages.GUI_ENTITYLIMIT_ITEM_INFO_PATTERN, new ArrayList<>(), region.getExtraTotalEntitys());
        if(region.getEntityLimitGroup().getSoftLimit(region.getExtraTotalEntitys()) < region.getEntityLimitGroup().getHardLimit()) {
            totalstatus = totalstatus.replace("%entityextensioninfo%", region.getEntityLimitGroup().getConvertedMessage(Messages.GUI_ENTITYLIMIT_ITEM_INFO_EXTENSION_INFO, new ArrayList<>(), region.getExtraTotalEntitys()));
        } else {
            totalstatus = totalstatus.replace("%entityextensioninfo%", "");
        }

        limitlist.add(totalstatus);

        for(EntityLimit entityLimit : region.getEntityLimitGroup().getEntityLimits()) {
            String entitystatus = entityLimit.getConvertedMessage(Messages.GUI_ENTITYLIMIT_ITEM_INFO_PATTERN, new ArrayList<>(), region.getExtraEntityAmount(entityLimit.getEntityType()));
            if((entityLimit.getSoftLimit(region.getExtraEntityAmount(entityLimit.getEntityType())) < entityLimit.getHardLimit()) && !region.isSubregion()) {
                entitystatus = entitystatus.replace("%entityextensioninfo%", entityLimit.getConvertedMessage(Messages.GUI_ENTITYLIMIT_ITEM_INFO_EXTENSION_INFO, new ArrayList<>(), region.getExtraEntityAmount(entityLimit.getEntityType())));
            } else {
                entitystatus = entitystatus.replace("%entityextensioninfo%", "");
            }
            limitlist.add(entitystatus);
        }

        for(int i = 0; i < lore.size(); i++) {
            lore.set(i, region.getConvertedMessage(lore.get(i)));
            if(lore.get(i).contains("%entityinfopattern%")) {
                lore.remove(i);
                lore.addAll(i, limitlist);
            }
        }

        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ClickItem[] getFlagSettingItem(Flag flag, Region region, ClickAction afterFlagSetAction) {
        class FlagSetter implements ClickAction {
            private String input;

            FlagSetter(String input) {
                this.input = input;
            }

            public void setInput(String s) {
                this.input = s;
            }

            public boolean isInputSelected() {
                try {
                    return region.getRegion().getFlagSetting(flag) == getParsedSettingsObject();
                } catch (InvalidFlagFormat e) {
                    return false;
                }
            }

            private Object getParsedSettingsObject() throws InvalidFlagFormat {
                return AdvancedRegionMarket.getWorldGuardInterface().parseFlagInput(flag, region.getConvertedMessage(this.input));
            }

            @Override
            public void execute(Player player) throws InputException {
                try {
                    Object flagSetting = getParsedSettingsObject();
                    if(flag.getDefault() == flagSetting) {
                        region.getRegion().deleteFlags(flag);
                    } else {
                        region.getRegion().setFlag(flag, flagSetting);
                    }
                    afterFlagSetAction.execute(player);
                    player.sendMessage(Messages.PREFIX + "Flag has been updated!");
                } catch (InvalidFlagFormat invalidFlagFormat) {
                    //Todo
                    throw new InputException(player, "Could not parse given input for flag " + flag.getName());
                }
            }
        }


        ClickItem[] clickItems;
        if(flag instanceof StateFlag) {
            clickItems = new ClickItem[2];
            //TODO change names (add to messages.yml)
            FlagSetter fs0 = new FlagSetter("allow");
            clickItems[0] = new ClickItem(fs0.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagSettingSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagSettingNotSelectedItem()), "Allow").addClickAction(new FlagSetter("allow"));
            FlagSetter fs1 = new FlagSetter("deny");
            clickItems[1] = new ClickItem(fs1.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagSettingSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagSettingNotSelectedItem()), "Deny").addClickAction(new FlagSetter("deny"));

        } else if(flag instanceof BooleanFlag) {
            clickItems = new ClickItem[2];
            //TODO change names (add to messages.yml)
            FlagSetter fs0 = new FlagSetter("true");
            clickItems[0] = new ClickItem(fs0.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagSettingSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagSettingNotSelectedItem()), "True").addClickAction(fs0);
            FlagSetter fs1 = new FlagSetter("false");
            clickItems[1] = new ClickItem(fs1.isInputSelected()? new ItemStack(MaterialFinder.getGuiFlagSettingSelectedItem()):
                    new ItemStack(MaterialFinder.getGuiFlagSettingNotSelectedItem()), "False").addClickAction(fs1);

        }
        else if (flag instanceof StringFlag) {
            clickItems = new ClickItem[1];
            final FlagSetter flagSetter = new FlagSetter("");
            clickItems[0] = new ClickItem(new ItemStack(Material.GRASS_BLOCK), "Set message").addClickAction((new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {

                    player.closeInventory();
                    player.sendMessage("Please write down a message:");
                    GuiChatInputListener gcil = new GuiChatInputListener(player, (s) -> {
                        flagSetter.setInput(s);
                        flagSetter.execute(player);
                    });
                    Bukkit.getPluginManager().registerEvents(gcil, AdvancedRegionMarket.getARM());

                }
            }));
        }
        else if (flag instanceof IntegerFlag) {

            clickItems = new ClickItem[1];
            final FlagSetter flagSetter = new FlagSetter("");
            clickItems[0] = new ClickItem(new ItemStack(Material.GRASS_BLOCK), "Write Number").addClickAction((new ClickAction() {
            @Override
            public void execute(Player player) throws InputException {
                player.closeInventory();
                player.sendMessage("Please write down a number that does not have decimals:");
                GuiChatInputListener gcil = new GuiChatInputListener(player, (s) -> {
                    flagSetter.setInput(s);
                    flagSetter.execute(player);
                });
                Bukkit.getPluginManager().registerEvents(gcil, AdvancedRegionMarket.getARM());
            }
        }));
        }
        else if (flag instanceof DoubleFlag) {

            clickItems = new ClickItem[1];
            final FlagSetter flagSetter = new FlagSetter("");
            clickItems[0] = new ClickItem(new ItemStack(Material.GRASS_BLOCK), "Write Number").addClickAction((new ClickAction() {
                @Override
                public void execute(Player player) throws InputException {
                    player.closeInventory();
                    player.sendMessage("Please write down a number:");
                    GuiChatInputListener gcil = new GuiChatInputListener(player, (s) -> {
                        flagSetter.setInput(s);
                        flagSetter.execute(player);
                    });
                    Bukkit.getPluginManager().registerEvents(gcil, AdvancedRegionMarket.getARM());
                }
            }));
        }
        else {
            return new ClickItem[0];
        }
        return clickItems;
    }
}