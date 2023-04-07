package com.lucktracker;
import net.runelite.api.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.http.api.item.ItemEquipmentStats;

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

    public static float getHitChance(int attRoll, int defRoll) {
        float fAttRoll = (float) attRoll;
        float fDefRoll = (float) defRoll;
        if (fAttRoll > fDefRoll) { return (1.0f - (fDefRoll + 2.0f) / (2.0f * (fAttRoll + 1.0f))); }
        else { return (fAttRoll / (2.0f * (fDefRoll + 1.0f))); }
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
        return (int) (specialBonus * ((int) (0.5 + (effRangeStrength * (rStrBonus + 64)) / 640 * gearBonus)));
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

    public int getEquipmentStyleBonus(EquipmentStat equipmentStat) { // Calculates the total bonus for a given equipment stat (e.g. ACRUSH, PRAYER, DSLASH...)

        // TODO Blowpipe; need a plugin setting to identify dart type and add it to RSTR

        Player player = client.getLocalPlayer();
        if (player == null) return -999;
        PlayerComposition playerComposition = player.getPlayerComposition();
        if (playerComposition == null) return -999;

        int[] equippedItems = playerComposition.getEquipmentIds();
        int bonus = 0;

        for (int id : equippedItems) {
            if (id < 512) continue; // Not a valid item
            id -= 512; // I know this seems weird after the last line... but convert item ID to proper value
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

    // endregion

}
