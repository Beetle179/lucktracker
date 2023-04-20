package com.lucktracker;

import com.google.inject.Provides;
import jdk.internal.org.jline.utils.Log;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.http.api.item.ItemEquipmentStats;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lucktracker.EquipmentStat.*;

@Slf4j
public class LuckTrackerUtil {

    private final LuckTrackerConfig config;
    private final Client client;
    private final ItemManager itemManager;
    private final ChatMessageManager chatMessageManager;
    private final NPCManager npcManager;

    private final static Set<Integer> imbuedSlayerHelms = new HashSet<>(Arrays.asList(
    ItemID.SLAYER_HELMET_I, ItemID.SLAYER_HELMET_I_25177, ItemID.SLAYER_HELMET_I_26674,
            ItemID.BLACK_SLAYER_HELMET_I, ItemID.BLACK_SLAYER_HELMET_I_26675, ItemID.BLACK_SLAYER_HELMET_I_25179,
            ItemID.GREEN_SLAYER_HELMET_I, ItemID.GREEN_SLAYER_HELMET_I_26676, ItemID.GREEN_SLAYER_HELMET_I_25181,
            ItemID.HYDRA_SLAYER_HELMET_I, ItemID.HYDRA_SLAYER_HELMET_I_26680, ItemID.HYDRA_SLAYER_HELMET_I_25189,
            ItemID.PURPLE_SLAYER_HELMET_I, ItemID.PURPLE_SLAYER_HELMET_I_26678, ItemID.PURPLE_SLAYER_HELMET_I_25185,
            ItemID.RED_SLAYER_HELMET_I, ItemID.RED_SLAYER_HELMET_I_26677, ItemID.RED_SLAYER_HELMET_I_25183,
            ItemID.TURQUOISE_SLAYER_HELMET_I, ItemID.TURQUOISE_SLAYER_HELMET_I_26679, ItemID.TURQUOISE_SLAYER_HELMET_I_25187,
            ItemID.TWISTED_SLAYER_HELMET_I, ItemID.TWISTED_SLAYER_HELMET_I_26681, ItemID.TWISTED_SLAYER_HELMET_I_25191,
            ItemID.TZTOK_SLAYER_HELMET_I, ItemID.TZTOK_SLAYER_HELMET_I_26682, ItemID.TZTOK_SLAYER_HELMET_I_25902,
            ItemID.TZKAL_SLAYER_HELMET_I, ItemID.TZKAL_SLAYER_HELMET_I_26684, ItemID.TZKAL_SLAYER_HELMET_I_25914,
            ItemID.VAMPYRIC_SLAYER_HELMET_I, ItemID.VAMPYRIC_SLAYER_HELMET_I_26683, ItemID.VAMPYRIC_SLAYER_HELMET_I_25908,
            ItemID.BLACK_MASK_I, ItemID.BLACK_MASK_10_I, ItemID.BLACK_MASK_9_I, ItemID.BLACK_MASK_8_I,
            ItemID.BLACK_MASK_7_I, ItemID.BLACK_MASK_6_I, ItemID.BLACK_MASK_5_I, ItemID.BLACK_MASK_4_I,
            ItemID.BLACK_MASK_3_I, ItemID.BLACK_MASK_2_I, ItemID.BLACK_MASK_1_I,
            ItemID.BLACK_MASK_I_25276, ItemID.BLACK_MASK_10_I_25266, ItemID.BLACK_MASK_9_I_25267, ItemID.BLACK_MASK_8_I_25268,
            ItemID.BLACK_MASK_7_I_25269, ItemID.BLACK_MASK_6_I_25270, ItemID.BLACK_MASK_5_I_25271, ItemID.BLACK_MASK_4_I_25272,
            ItemID.BLACK_MASK_3_I_25273, ItemID.BLACK_MASK_2_I_25274, ItemID.BLACK_MASK_1_I_25275,
            ItemID.BLACK_MASK_I_26781, ItemID.BLACK_MASK_10_I_26771, ItemID.BLACK_MASK_9_I_26772, ItemID.BLACK_MASK_8_I_26773,
            ItemID.BLACK_MASK_7_I_26774, ItemID.BLACK_MASK_6_I_26775, ItemID.BLACK_MASK_5_I_26776, ItemID.BLACK_MASK_4_I_26777,
            ItemID.BLACK_MASK_3_I_26778, ItemID.BLACK_MASK_2_I_26779, ItemID.BLACK_MASK_1_I_26780));

    private final static Set<Integer> slayerHelms = new HashSet<>(Arrays.asList(
            ItemID.SLAYER_HELMET, ItemID.BLACK_SLAYER_HELMET, ItemID.GREEN_SLAYER_HELMET, ItemID.HYDRA_SLAYER_HELMET, ItemID.PURPLE_SLAYER_HELMET, ItemID.RED_SLAYER_HELMET,  ItemID.TURQUOISE_SLAYER_HELMET,
            ItemID.TWISTED_SLAYER_HELMET, ItemID.TZTOK_SLAYER_HELMET, ItemID.TZKAL_SLAYER_HELMET, ItemID.VAMPYRIC_SLAYER_HELMET,
            ItemID.BLACK_MASK, ItemID.BLACK_MASK_1, ItemID.BLACK_MASK_2, ItemID.BLACK_MASK_3, ItemID.BLACK_MASK_4, ItemID.BLACK_MASK_5, ItemID.BLACK_MASK_6, ItemID.BLACK_MASK_7, ItemID.BLACK_MASK_8, ItemID.BLACK_MASK_9, ItemID.BLACK_MASK_10));

    private final static Set<Integer> toaRoomRegions = new HashSet<>(Arrays.asList(14160, 15698, 15700, 14162, 14164, 15186, 15188, 14674, 14676, 15184, 15696, 14672));

    public LuckTrackerUtil(Client client, ItemManager itemManager, ChatMessageManager chatMessageManager, NPCManager npcManager, LuckTrackerConfig config) {
        this.client = client;
        this.itemManager = itemManager;
        this.chatMessageManager = chatMessageManager;
        this.npcManager = npcManager;
        this.config = config;
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

    public static boolean regionIsInToa(int region) {
        return toaRoomRegions.contains(region);
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

    public static int calcBasicMaxHit(int effStrLvl, int strBonus) { // Calculates max hit based on effective strength level and strength bonus (from equipment stats).
        return (effStrLvl * (strBonus + 64) + 320) / 640; // Internal cast not necessary; int division will automatically return floored int
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

    public static int calcRangeBasicMaxHit(int effRangeStrength, int rStrBonus) {
        return (int) ((double) ((64 + rStrBonus) * (effRangeStrength)) / 640.0D + 0.5);
    }

    public static int calcEffectiveMagicLevel(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor, boolean voidEliteArmor) {
        double voidBonus;
        if (voidEliteArmor || voidArmor) voidBonus = 1.45;
        else voidBonus = 1;
        return (int) (voidBonus * ((int) (visibleLvl * prayerBonus)) + styleBonus + 9);
    }

    int dartStrength(LuckTrackerConfig.BlowpipeDart dart) { // Can't set up an EnumMap for some reason
        switch (dart) {
            case DRAGON: return 35;
            case AMETHYST: return 28;
            case RUNE: return 26;
            case ADAMANT: return 17;
            case MITHRIL: return 9;
            case BLACK: return 6;
            case STEEL: return 3;
            case IRON: return 2;
            case BRONZE: return 1;
            default: return 0;
        }
    }

    // endregion

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

    public int getEquipmentStyleBonus(ItemContainer wornItemsContainer, EquipmentStat equipmentStat) { // Calculates the total bonus for a given equipment stat (e.g. ACRUSH, PRAYER, DSLASH...)

        // TODO Blowpipe; need a plugin setting to identify dart type and add it to RSTR

        Item[] wornItems = wornItemsContainer.getItems();

        int bonus = 0;

        for (Item item : wornItems) {
            int id = item.getId();
            if (id < 0) continue; // No item in this slot
            bonus += getItemStyleBonus(id, equipmentStat);
        }
        if (equipmentStat == RSTR && wornItemsContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx()).getId() == ItemID.TOXIC_BLOWPIPE) {
            bonus += dartStrength(config.equippedBlowpipeDarts());
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

    public static double calcSalveSlayerBonus(ItemContainer wornItemsContainer, boolean slayerTarget, boolean salveTarget, AttackStyle attackStyle) {
        Item[] wornItems = wornItemsContainer.getItems();
        List<Integer> wornItemIds = Arrays.stream(wornItems).map(Item::getId).collect(Collectors.toList());
        boolean equippedItemInHeadSlot = wornItemsContainer.getItem(EquipmentInventorySlot.HEAD.getSlotIdx()) != null;

        if (salveTarget) {
            // Salve (ei)
            if (wornItemIds.contains(ItemID.SALVE_AMULETEI) || wornItemIds.contains(ItemID.SALVE_AMULETEI_25278)) {
                log.info("Applying Salve (ei) bonus");
                return 1.2D;
            }

            // Salve (e)
            if (wornItemIds.contains(ItemID.SALVE_AMULET_E)) {
                if ((attackStyle != AttackStyle.MAGIC) && (attackStyle != AttackStyle.RANGE)) {
                    log.info("Applying Salve (e) bonus");
                    return 1.2D;
                }
            }

            // Salve (i)
            if (wornItemIds.contains(ItemID.SALVE_AMULETI) || wornItemIds.contains(ItemID.SALVE_AMULETI_26763)) {
                log.info("Applying Salve (i) bonus");
                if (attackStyle == AttackStyle.MAGIC) {
                    return 1.15D;
                }
                return (7.0D / 6.0D);
            }

            // Will check regular Salve AFTER checking Slayer bonuses.
        }

        // Slayer helm (i) (or black mask (i)). Outer If statement to avoid running the inner conditional if possible. (IDK if Java would stop checking after the first If anyway but I'm not gonna google it rn)
        if (slayerTarget && equippedItemInHeadSlot) {
            if (imbuedSlayerHelms.contains(wornItemsContainer.getItem(EquipmentInventorySlot.HEAD.getSlotIdx()).getId())) {
                log.info("Applying Imbued Slayer Helm/Black Mask bonus");
                switch (attackStyle) {
                    case MAGIC:
                    case RANGE: {
                        return 1.15D;
                    }
                    default: {
                        return (7.0D / 6.0D);
                    }
                }
            }
        }

        // Regular Salve
        if (salveTarget && wornItemIds.contains(ItemID.SALVE_AMULET) && ((attackStyle != AttackStyle.MAGIC) && (attackStyle != AttackStyle.RANGE))) {
            log.info("Applying Salve bonus");
            return (7.0D / 6.0D);
        }

        // Regular Slayer helm (or black mask)
        if (slayerTarget && equippedItemInHeadSlot) {
            if ((attackStyle == AttackStyle.MAGIC) || (attackStyle == AttackStyle.RANGE)) {
                return 1.0D; // Don't need to check helmets if we're not using melee
            }
            if (slayerHelms.contains(wornItemsContainer.getItem(EquipmentInventorySlot.HEAD.getSlotIdx()).getId())) {
                log.info("Applying Slayer Helm bonus");
                return (7.0D / 6.0D);
            }
        }

        // Default
        return 1.0D;
    }


    public boolean isWearingVoid(ItemContainer wornItemsContainer, Skill skill, boolean elite) { // gross
        boolean helm = false;

        Item[] wornItems = wornItemsContainer.getItems();

        List<Integer> itemIds = Arrays.stream(wornItems).map(Item::getId).collect(Collectors.toList());
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
