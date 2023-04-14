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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            case ACRUSH:
                return DCRUSH;
            case ASLASH:
                return DSLASH;
            case ASTAB:
                return DSTAB;
            case AMAGIC:
                return DMAGIC;
            case ARANGE:
                return DRANGE;
            default:
                return null;
        }
    }

    public static Pattern targetNamePattern(final String targetName) {
        return Pattern.compile("(?:\\s|^)" + targetName + "(?:\\s|$)", Pattern.CASE_INSENSITIVE);
    }
    //endregion

    // region Combat Utility Functions -- Generic
    public static int calcBasicDefenceRoll(int defLvl, int styleDefBonus) { // Calculates an NPC's defensive roll.
        return (defLvl + 9) * (styleDefBonus + 64);
    }

    public static double getHitChance(int attRoll, int defRoll) {
        double fAttRoll = (double) attRoll;
        double fDefRoll = (double) defRoll;
        if (fAttRoll > fDefRoll) {
            return (1.0D - (fDefRoll + 2.0D) / (2.0D * (fAttRoll + 1.0D)));
        } else {
            return (fAttRoll / (2.0D * (fDefRoll + 1.0D)));
        }
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
        return (int) (specialBonus * (int) ((double) ((64 + rStrBonus) * (effRangeStrength)) / 640.0D * gearBonus + 0.5));
    } // endregion

    // region NPC Utility Functions
    public int getNpcCurrentHp(NPC npc) { // Logic from OpponentInfo plugin
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
        for (Prayer prayer : Prayer.values()) {
            if (client.getVarbitValue(prayer.getVarbit()) > 0) { // Using a custom Prayer class, so we lost access to client.getPrayerActive() or whatever it is
                mod += prayer.getPrayerAttributeMod(prayerAttribute);
            }
        }
        return mod;
    }

    public int getEquipmentStyleBonus(List<Item> wornItems, EquipmentStat equipmentStat) { // Calculates the total bonus for a given equipment stat (e.g. ACRUSH, PRAYER, DSLASH...)

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
        switch (equipmentStat) {
            case ACRUSH:
                return stats.getAcrush();
            case STR:
                return stats.getStr();
            case MDMG:
                return stats.getMdmg();
            case RSTR:
                return stats.getRstr();
            case ASTAB:
                return stats.getAstab();
            case DSTAB:
                return stats.getDstab();
            case AMAGIC:
                return stats.getAmagic();
            case ARANGE:
                return stats.getArange();
            case ASLASH:
                return stats.getAslash();
            case ASPEED:
                return stats.getAspeed();
            case DCRUSH:
                return stats.getDcrush();
            case DMAGIC:
                return stats.getDmagic();
            case DRANGE:
                return stats.getDrange();
            case DSLASH:
                return stats.getDslash();
            case PRAYER:
                return stats.getPrayer();
        }
        return -999;
    }

    public static double getGearBonus(List<Item> wornItems, boolean slayerTarget, boolean salveTarget, AttackStyle attackStyle) {

        List<Integer> wornItemIds = wornItems.stream().map(Item::getId).collect(Collectors.toList());

        if (salveTarget) {
            // Salve (ei)
            if (wornItemIds.contains(ItemID.SALVE_AMULETEI) || wornItemIds.contains(ItemID.SALVE_AMULETEI_25278))
                return 1.2D;

            // Salve (e)
            if (wornItemIds.contains(ItemID.SALVE_AMULET_E)) {
                if ((attackStyle != AttackStyle.MAGIC) && (attackStyle != AttackStyle.RANGE)) return 1.2D;
            }

            // Salve (i)
            if (wornItemIds.contains(ItemID.SALVE_AMULETI) || wornItemIds.contains(ItemID.SALVE_AMULETI_26763)) {
                if (attackStyle == AttackStyle.MAGIC) return 1.15D;
                return (7.0D / 6.0D);
            }

            // Will check regular Salve AFTER checking Slayer bonuses.
        }

        // Slayer helm (i) (or black mask (i)). Outer If statement to avoid running the inner conditional if possible. (IDK if Java would stop checking after the first If anyway but I'm not gonna google it rn)
        if (slayerTarget) {
            if ((wornItemIds.contains(ItemID.SLAYER_HELMET_I) || wornItemIds.contains(ItemID.SLAYER_HELMET_I_25177) || wornItemIds.contains(ItemID.SLAYER_HELMET_I_26674))
                    || wornItemIds.contains(ItemID.BLACK_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.BLACK_SLAYER_HELMET_I_26675) || wornItemIds.contains(ItemID.BLACK_SLAYER_HELMET_I_25179)
                    || wornItemIds.contains(ItemID.GREEN_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.GREEN_SLAYER_HELMET_I_26676) || wornItemIds.contains(ItemID.GREEN_SLAYER_HELMET_I_25181)
                    || wornItemIds.contains(ItemID.HYDRA_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.HYDRA_SLAYER_HELMET_I_26680) || wornItemIds.contains(ItemID.HYDRA_SLAYER_HELMET_I_25189)
                    || wornItemIds.contains(ItemID.PURPLE_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.PURPLE_SLAYER_HELMET_I_26678) || wornItemIds.contains(ItemID.PURPLE_SLAYER_HELMET_I_25185)
                    || wornItemIds.contains(ItemID.RED_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.RED_SLAYER_HELMET_I_26677) || wornItemIds.contains(ItemID.RED_SLAYER_HELMET_I_25183)
                    || wornItemIds.contains(ItemID.TURQUOISE_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.TURQUOISE_SLAYER_HELMET_I_26679) || wornItemIds.contains(ItemID.TURQUOISE_SLAYER_HELMET_I_25187)
                    || wornItemIds.contains(ItemID.TWISTED_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.TWISTED_SLAYER_HELMET_I_26681) || wornItemIds.contains(ItemID.TWISTED_SLAYER_HELMET_I_25191)
                    || wornItemIds.contains(ItemID.TZTOK_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.TZTOK_SLAYER_HELMET_I_26682) || wornItemIds.contains(ItemID.TZTOK_SLAYER_HELMET_I_25902)
                    || wornItemIds.contains(ItemID.TZKAL_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.TZKAL_SLAYER_HELMET_I_26684) || wornItemIds.contains(ItemID.TZKAL_SLAYER_HELMET_I_25914)
                    || wornItemIds.contains(ItemID.VAMPYRIC_SLAYER_HELMET_I) || wornItemIds.contains(ItemID.VAMPYRIC_SLAYER_HELMET_I_26683) || wornItemIds.contains(ItemID.VAMPYRIC_SLAYER_HELMET_I_25908)
                    || wornItemIds.contains(ItemID.BLACK_MASK_I) || wornItemIds.contains(ItemID.BLACK_MASK_10_I) || wornItemIds.contains(ItemID.BLACK_MASK_9_I) || wornItemIds.contains(ItemID.BLACK_MASK_8_I)
                    || wornItemIds.contains(ItemID.BLACK_MASK_7_I) || wornItemIds.contains(ItemID.BLACK_MASK_6_I) || wornItemIds.contains(ItemID.BLACK_MASK_5_I) || wornItemIds.contains(ItemID.BLACK_MASK_4_I)
                    || wornItemIds.contains(ItemID.BLACK_MASK_3_I) || wornItemIds.contains(ItemID.BLACK_MASK_2_I) || wornItemIds.contains(ItemID.BLACK_MASK_1_I)
                    || wornItemIds.contains(ItemID.BLACK_MASK_I_25276) || wornItemIds.contains(ItemID.BLACK_MASK_10_I_25266) || wornItemIds.contains(ItemID.BLACK_MASK_9_I_25267) || wornItemIds.contains(ItemID.BLACK_MASK_8_I_25268)
                    || wornItemIds.contains(ItemID.BLACK_MASK_7_I_25269) || wornItemIds.contains(ItemID.BLACK_MASK_6_I_25270) || wornItemIds.contains(ItemID.BLACK_MASK_5_I_25271) || wornItemIds.contains(ItemID.BLACK_MASK_4_I_25272)
                    || wornItemIds.contains(ItemID.BLACK_MASK_3_I_25273) || wornItemIds.contains(ItemID.BLACK_MASK_2_I_25274) || wornItemIds.contains(ItemID.BLACK_MASK_1_I_25275)
                    || wornItemIds.contains(ItemID.BLACK_MASK_I_26781) || wornItemIds.contains(ItemID.BLACK_MASK_10_I_26771) || wornItemIds.contains(ItemID.BLACK_MASK_9_I_26772) || wornItemIds.contains(ItemID.BLACK_MASK_8_I_26773)
                    || wornItemIds.contains(ItemID.BLACK_MASK_7_I_26774) || wornItemIds.contains(ItemID.BLACK_MASK_6_I_26775) || wornItemIds.contains(ItemID.BLACK_MASK_5_I_26776) || wornItemIds.contains(ItemID.BLACK_MASK_4_I_26777)
                    || wornItemIds.contains(ItemID.BLACK_MASK_3_I_26778) || wornItemIds.contains(ItemID.BLACK_MASK_2_I_26779) || wornItemIds.contains(ItemID.BLACK_MASK_1_I_26780)) {
                switch (attackStyle) {
                    case MAGIC:
                    case RANGE:
                        return 1.15D;
                    default:
                        return (7.0D / 6.0D);
                }
            }
        }

        // Regular Salve
        if (salveTarget && wornItemIds.contains(ItemID.SALVE_AMULET) && ((attackStyle != AttackStyle.MAGIC) && (attackStyle != AttackStyle.RANGE)))
            return (7.0D / 6.0D);

        // Regular Slayer helm (or black mask)
        if (slayerTarget) {
            if ((attackStyle == AttackStyle.MAGIC) || (attackStyle == AttackStyle.RANGE))
                return 1.0D; // Don't need to check helmets if we're not using melee
            if ((wornItemIds.contains(ItemID.SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.BLACK_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.GREEN_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.HYDRA_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.PURPLE_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.RED_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.TURQUOISE_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.TWISTED_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.TZTOK_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.TZKAL_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.VAMPYRIC_SLAYER_HELMET)
                    || wornItemIds.contains(ItemID.BLACK_MASK)
                    || wornItemIds.contains(ItemID.BLACK_MASK_1)
                    || wornItemIds.contains(ItemID.BLACK_MASK_2)
                    || wornItemIds.contains(ItemID.BLACK_MASK_3)
                    || wornItemIds.contains(ItemID.BLACK_MASK_4)
                    || wornItemIds.contains(ItemID.BLACK_MASK_5)
                    || wornItemIds.contains(ItemID.BLACK_MASK_6)
                    || wornItemIds.contains(ItemID.BLACK_MASK_7)
                    || wornItemIds.contains(ItemID.BLACK_MASK_8)
                    || wornItemIds.contains(ItemID.BLACK_MASK_9)
                    || wornItemIds.contains(ItemID.BLACK_MASK_10))) {
                return (7.0D / 6.0D);
            }
        }

        // Default
        return 1.0D;
    }


    public boolean isWearingVoid(List<Item> wornItems, Skill skill, boolean elite) { // gross
        boolean helm = false;

        List<Integer> itemIds = wornItems.stream().map(Item::getId).collect(Collectors.toList());
        for (Item item : wornItems) itemIds.add(item.getId());

        if (itemIds.contains(11665) && (skill == Skill.ATTACK)) helm = true;
        else if (itemIds.contains(11664) && (skill == Skill.RANGED)) helm = true;
        else if (itemIds.contains(11663) && (skill == Skill.MAGIC)) helm = true;

        if (!helm) return false;

        if (itemIds.contains(8839) && itemIds.contains(8840) && itemIds.contains(8842) && (!elite))
            return true; // Regular void armor
        return itemIds.contains(13072) && itemIds.contains(13073) && itemIds.contains(8842) && elite; // Elite void armor

    }
    // endregion
}
