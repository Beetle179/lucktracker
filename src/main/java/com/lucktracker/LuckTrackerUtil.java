package com.lucktracker;
import net.runelite.api.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.http.api.item.ItemEquipmentStats;

import java.util.ArrayList;

import static com.lucktracker.EquipmentStat.*;

public class LuckTrackerUtil {
    private final Client client;
    private final ItemManager itemManager;
    private final ChatMessageManager chatMessageManager;
    private final NPCManager npcManager;

    public LuckTrackerUtil(Client client, ItemManager itemManager, ChatMessageManager chatMessageManager, NPCManager npcManager) {
        this.client = client;
        this.itemManager = itemManager;
        this.chatMessageManager = chatMessageManager;
        this.npcManager = npcManager;
    }

    // region General Utility Functions
    public void sendChatMessage(String chatMessage) {
        final String message = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(chatMessage)
                .build();

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(message)
                        .build());
    }

    public static EquipmentStat getDefensiveStat(EquipmentStat offensiveStat) { // when attacking with offensiveStat, determine what defensive stat to roll against
        switch (offensiveStat) {
            case ACRUSH: return DCRUSH;
            case ASLASH: return DSLASH;
            case ASTAB: return DSTAB;
            case AMAGIC: return DMAGIC;
            case ARANGE: return DRANGE;
            default: return null;
        }
    } //endregion

    // region Combat Utility Functions -- Generic
    public static int calcBasicDefenceRoll(int defLvl, int styleDefBonus) { // Calculates an NPC's defensive roll.
        return (defLvl + 9) * (styleDefBonus + 64);
    }

    public static double getHitChance(int attRoll, int defRoll) {
        double fAttRoll = (double) attRoll;
        double fDefRoll = (double) defRoll;
        if (fAttRoll > fDefRoll) { return (1.0D - (fDefRoll + 2.0D) / (2.0D * (fAttRoll + 1.0D))); }
        else { return (fAttRoll / (2.0D * (fDefRoll + 1.0D))); }
    }
    // endregion

    // region Combat Utility Functions -- Melee
    public static int calcEffectiveMeleeLevel(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor) { // Calculates effective attack or strength level.
        int effLvl = ((int) (visibleLvl * prayerBonus)) + styleBonus + 8; // Internal cast necessary; int * double promotes to double
        return voidArmor ? ((int) (effLvl * 1.1D)) : effLvl;
    }

    public static int calcBasicMeleeAttackRoll(int effAttLvl, int attBonus, double tgtBonus) { // Calculate attack roll.
        return (int) (effAttLvl * (attBonus + 64) * tgtBonus);
    }

    public static int calcBasicMaxHit(int effStrLvl, int strBonus, double tgtBonus) { // Calculates max hit based on effective strength level and strength bonus (from equipment stats).
        return (int) (tgtBonus * ((effStrLvl * (strBonus + 64) + 320) / 640)); // Internal cast not necessary; int division will automatically return floored int
    } // endregion

    // region Combat Utility Functions -- Range
    public static int calcEffectiveRangeAttack(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor, boolean voidEliteArmor) {
        double voidBonus;
        if (voidEliteArmor || voidArmor) voidBonus = 1.1;
        else voidBonus = 1;
        return (int) (voidBonus * (((int) (visibleLvl * prayerBonus)) + styleBonus + 8));
    }

    public static int calcEffectiveRangeStrength(int effRangeStr, double prayerBonus, int styleBonus, boolean voidArmor, boolean voidEliteArmor) {
        double voidBonus;
        if (voidEliteArmor) voidBonus = 1.125;
        else if (voidArmor) voidBonus = 1.1;
        else voidBonus = 1;
        return (int) (voidBonus * (((int) (effRangeStr * prayerBonus)) + styleBonus + 8));
    }

    public static int calcBasicRangeAttackRoll(int effRangeAtt, int rangeAttBonus, double gearBonus) { // Calculate ranged attack roll.
        return (int) (effRangeAtt * (rangeAttBonus + 64) * gearBonus);
    }

    public static int calcRangeBasicMaxHit(int effRangeStrength, int rStrBonus, double gearBonus, double specialBonus) {
        System.out.print(String.format("EffRngStr = %d, rStrBonus = %d, gearBonus = %f, specialBonus = %f", effRangeStrength, rStrBonus, gearBonus, specialBonus));
        return (int) (specialBonus * (int) ((double) ((64 + rStrBonus) * (effRangeStrength)) / 640.0D * gearBonus + 0.5));
    } // endregion

    // region NPC Utility Functions
    private int getNpcCurrentHp(NPC npc) { // Logic from OpponentInfo plugin
        int healthRatio = npc.getHealthRatio();
        int healthScale = npc.getHealthScale();
        int maxHealth = npcManager.getHealth(npc.getId());
        int minHealth = maxHealth;
        if (healthScale > 1) {
            if (healthRatio > 1) {
                minHealth = (maxHealth * (healthRatio - 1) + healthScale - 2) / (healthScale - 1);
            }
            maxHealth = (maxHealth * healthRatio - 1) / (healthScale - 1);
        }

        int currentHp = (minHealth + maxHealth + 1) / 2;

        return currentHp;
    } // endregion

    // region Equipment & Prayer Utility Functions
    public double getActivePrayerModifiers(PrayerAttribute prayerAttribute) { // For a given PrayerAttribute -- go through all active prayers and figure out the total modifier to that PrayerAttribute
        double mod = 1.0D;
        for (Prayer prayer: Prayer.values()) {
            if (client.getVarbitValue(prayer.getVarbit()) > 0) { // Using a custom Prayer class, so we lost access to client.getPrayerActive() or whatever it is
                mod += prayer.getPrayerAttributeMod(prayerAttribute);
            }
        }
        return mod;
    }

    public int getEquipmentStyleBonus(Item[] wornItems, EquipmentStat equipmentStat) { // Calculates the total bonus for a given equipment stat (e.g. ACRUSH, PRAYER, DSLASH...)

        // TODO Blowpipe; need a plugin setting to identify dart type and add it to RSTR

        int bonus = 0;

        for (Item item : wornItems) {
            int id = item.getId();
            if (id < 0) continue; // No item in this slot
            bonus += getItemStyleBonus(id, equipmentStat);
        }
        return bonus;
    }

    public int getItemStyleBonus(int id, EquipmentStat equipmentStat) { // Takes an item ID and returns that item's bonus for the specified equipment stat.
        ItemEquipmentStats stats = itemManager.getItemStats(id, false).getEquipment();
        if (stats == null) return -999;
        switch(equipmentStat) {
            case ACRUSH: return stats.getAcrush();
            case STR: return stats.getStr();
            case MDMG: return stats.getMdmg();
            case RSTR: return stats.getRstr();
            case ASTAB: return stats.getAstab();
            case DSTAB: return stats.getDstab();
            case AMAGIC: return stats.getAmagic();
            case ARANGE: return stats.getArange();
            case ASLASH: return stats.getAslash();
            case ASPEED: return stats.getAspeed();
            case DCRUSH: return stats.getDcrush();
            case DMAGIC: return stats.getDmagic();
            case DRANGE: return stats.getDrange();
            case DSLASH: return stats.getDslash();
            case PRAYER: return stats.getPrayer();
        }
        return -999;
    }

    public boolean isWearingVoid(Item[] wornItems, Skill skill, boolean elite) { // gross
        boolean helm = false;

        ArrayList<Integer> itemIds = new ArrayList<>(wornItems.length);
        for (Item item : wornItems) itemIds.add(item.getId());

        if (itemIds.contains(11665) && (skill == Skill.ATTACK)) helm = true;
        else if (itemIds.contains(11664) && (skill == Skill.RANGED)) helm = true;
        else if (itemIds.contains(11663) && (skill == Skill.MAGIC)) helm = true;

        if (!helm) return false;

        if (itemIds.contains(8839) && itemIds.contains(8840) && itemIds.contains(8842) && (!elite)) return true; // Regular void armor
        return itemIds.contains(13072) && itemIds.contains(13073) && itemIds.contains(8842) && elite; // Elite void armor

    }
    // endregion
}
