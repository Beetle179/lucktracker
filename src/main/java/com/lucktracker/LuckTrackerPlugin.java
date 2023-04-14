/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lucktracker;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
        name = "Luck Tracker",
        description = "Stats on how lucky you are in combat",
        tags = {"combat", "rng"},
        enabledByDefault = false
)

public class LuckTrackerPlugin extends Plugin {

    private LuckTrackerPanel panel;
    private NavigationButton navButton;

    private TickCounterUtil tickCounterUtil; // Used for identifying attack animations
    private Actor lastInteracting; // Keep track of targetted NPC; will update every game tick
    private Monsters monsterTable;
    private LuckTrackerUtil UTIL;

    private List<Item> wornItems;
    private ArrayList<Pattern> slayerTargetNames = new ArrayList<>();
    private int specialAttackEnergy;
    private boolean usedSpecialAttack;

    private int totalDamage;
    private HitDist runningHitDist;

    private boolean isWearingMeleeVoid;
    private boolean isWearingRangeVoid;
    private boolean isWearingMagicVoid;
    private boolean isWearingMeleeEliteVoid;
    private boolean isWearingRangeEliteVoid;
    private boolean isWearingMagicEliteVoid;


    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private LuckTrackerConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private NPCManager npcManager;

    @Provides
    LuckTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LuckTrackerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        tickCounterUtil = new TickCounterUtil(); // Utility ripped from PVMTickCounter plugin: dictionary of (animation_ID, tick_duration) pairs. Used for ID'ing player attack animations.
        tickCounterUtil.init();
        monsterTable = new Monsters();
        UTIL = new LuckTrackerUtil(client, itemManager, chatMessageManager, npcManager);

        this.totalDamage = 0;

        // Load the side panel
        final BufferedImage navIcon = ImageUtil.loadImageResource(this.getClass(), "/info_icon.png"); // Load the icon for the nav button (in LuckTrackerPlugin's resources subfolder)
        panel = injector.getInstance(LuckTrackerPanel.class); // Create an instance of the LuckTrackerPanel
        navButton = NavigationButton.builder() // Create a nav button that we can add to the toolbar...
                .tooltip("Luck Tracker")
                .icon(navIcon)
                .priority(10)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton); // Add the nav button to the toolbar

        clientThread.invokeLater(() -> {
            // Get worn items on startup
            final ItemContainer container = client.getItemContainer(InventoryID.EQUIPMENT);
            if (container != null) wornItems = Arrays.asList(container.getItems());

            // Get special attack energy, initialize the spec boolean
            this.usedSpecialAttack = false;
            this.specialAttackEnergy = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);

            // Master hit distribution
            this.runningHitDist = new HitDist();

            // Figure out what our slayer task is, and get a handle on all applicable target NPC names
            updateSlayerTargetNames();
        });
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        panel = null;
    }

    protected void resetStats() {
        this.runningHitDist = new HitDist();
        this.totalDamage = 0;
        panel.updatePanelStats(this.totalDamage, this.runningHitDist.getAvgDmg(), this.runningHitDist.getCdfAtDmg(this.totalDamage), this.runningHitDist.getDmgAtCdf(0.1D), this.runningHitDist.getDmgAtCdf(0.9D));
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {

        Player player = client.getLocalPlayer();
        if (player == null) return;

        // Update currently interacting NPC
        Actor interactingActor = player.getInteracting();
        if (interactingActor instanceof NPC) this.lastInteracting = interactingActor;

        // Reset the special attack bool
        clientThread.invokeLater(() -> {
            this.usedSpecialAttack = false;
        });
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Player player = client.getLocalPlayer();
        Actor actor = hitsplatApplied.getActor();
        if (!(actor instanceof NPC)) // only look for hitsplats applied to NPCs
        {
            return;
        }
        Hitsplat hitsplat = hitsplatApplied.getHitsplat(); // get a handle on the hitsplat
        if (hitsplat.isMine()) { // if it's our own hitsplat...
            this.totalDamage += hitsplat.getAmount();
            panel.updatePanelStats(this.totalDamage, this.runningHitDist.getAvgDmg(), this.runningHitDist.getCdfAtDmg(this.totalDamage), this.runningHitDist.getDmgAtCdf(0.1D), this.runningHitDist.getDmgAtCdf(0.9D));
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        if (varbitChanged.getVarpId() == VarPlayer.SPECIAL_ATTACK_PERCENT) this.specialAttackEnergy = varbitChanged.getValue();
        if (varbitChanged.getValue() < this.specialAttackEnergy) this.usedSpecialAttack = true;
        if (varbitChanged.getVarpId() == VarPlayer.SLAYER_TASK_CREATURE || varbitChanged.getVarpId() == VarPlayer.SLAYER_TASK_LOCATION)
        {
            updateSlayerTargetNames();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT)) {
            return;
        }

        this.wornItems = Arrays.asList(event.getItemContainer().getItems());

        clientThread.invokeLater(this::updateVoidStatus);
        updateSlayerTargetNames();
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged e) { // Main hook for identifying when we perform an attack: our animation changes. Will use PVMTickCounter's utility to determine
        // TODO most weapon anims are 2 ticks; so 2t weapons won't get tracked properly using just onAnimationChanged
        //		Ian idea: instead of tracking onAnimationChanged, keep track of player's animation and re-set it every 2 ticks
        //		Alternative: PVMTickCounter uses an "IsBPing" flag
        // TODO hook up salamander blaze and flare; no animation on player, but it spawns G = 952.
        //  Also potentially a lot of other attack animations.
        // 	See tickCounterUtil -> aniTM.


        // Do everything AFTER this client thread: need to allow targeted NPC to update first.
        clientThread.invokeLater(() -> {

            if (!(e.getActor() instanceof Player)) return;
            Player p = (Player) e.getActor();
            if (p != client.getLocalPlayer()) return;
            if (!tickCounterUtil.isAttack(p.getAnimation())) return; // If the animation isn't an attack, stop here
            int animationId = p.getAnimation();

            // ************************************************** //
            // Player Attack setup
            // TODO Casting a spell will take whatever stance is currently active... Which is only accurate if autocasting. For casting spells specifically, will probably need to short circuit based on animation?

            int attackStyleId = client.getVarpValue(VarPlayer.ATTACK_STYLE);
            int weaponTypeId = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);

            WeaponStance weaponStance = WeaponType.getWeaponStance(weaponTypeId, attackStyleId); // Determine weapon stance (Controlled, Aggressive, Rapid, etc.)
            AttackStyle attackStyle = WeaponType.getAttackStyle(weaponTypeId, attackStyleId); // Determine if we're using slash, crush, stab, range, or magic based on the weapon type and current stance

            // NPC Defense setup

            int npcId = ((NPC) this.lastInteracting).getId();
            MonsterData npcData = this.monsterTable.getMonsterData(npcId);
            if (npcData == null) {
                UTIL.sendChatMessage("UNKNOWN MONSTER");
                return;
            }

            HitDist hitDist = processAttack(weaponStance, attackStyle, npcData, this.usedSpecialAttack);
            UTIL.sendChatMessage("Average: " + hitDist.getAvgDmg() + " || Max: " + hitDist.getMax() + " || >0 Prob: " + hitDist.getNonZeroHitChance());

            this.runningHitDist.convolve(hitDist);
        });
    }

    private void updateVoidStatus() { // gross
        this.isWearingMeleeVoid = UTIL.isWearingVoid(this.wornItems, Skill.ATTACK, false);
        this.isWearingRangeVoid = UTIL.isWearingVoid(this.wornItems, Skill.RANGED, false);
        this.isWearingMagicVoid = UTIL.isWearingVoid(this.wornItems, Skill.MAGIC, false);

        this.isWearingMeleeEliteVoid = UTIL.isWearingVoid(this.wornItems, Skill.ATTACK, true);
        this.isWearingRangeEliteVoid = UTIL.isWearingVoid(this.wornItems, Skill.RANGED, true);
        this.isWearingMagicEliteVoid = UTIL.isWearingVoid(this.wornItems, Skill.MAGIC, true);
    }

    public void updateSlayerTargetNames() {
        this.slayerTargetNames.clear();
        int amount = client.getVarpValue(VarPlayer.SLAYER_TASK_SIZE);
        if (amount > 0) {

            String taskName;
            int taskId = client.getVarpValue(VarPlayer.SLAYER_TASK_CREATURE);

            if (taskId == 98 /* Bosses, from [proc,helper_slayer_current_assignment] */) {
                taskName = client.getEnum(EnumID.SLAYER_TASK_BOSS)
                        .getStringValue(client.getVarbitValue(Varbits.SLAYER_TASK_BOSS));
            } else {
                taskName = client.getEnum(EnumID.SLAYER_TASK_CREATURE)
                        .getStringValue(taskId);
            }

            Task task = Task.getTask(taskName);
            if (task == null) return;
            Arrays.stream(task.getTargetNames())
                    .map(LuckTrackerUtil::targetNamePattern)
                    .forEach(this.slayerTargetNames::add);
            this.slayerTargetNames.add(LuckTrackerUtil.targetNamePattern(taskName.replaceAll("s$", "")));
        }
    }

    private boolean isSlayerTarget(NPC npc) {
        if (this.slayerTargetNames.isEmpty()) {
            return false;
        }

        for (NPC nearbyNpc : client.getNpcs()) {
            NPCComposition composition = nearbyNpc.getTransformedComposition();
            String name;
            Matcher targetMatcher;
            if (composition == null) {
                continue;
            }
            name = composition.getName().replace('\u00A0', ' ').toLowerCase();

            for (Pattern target : this.slayerTargetNames) {
                targetMatcher = target.matcher(name);
                if (targetMatcher.find()
                        && (ArrayUtils.contains(composition.getActions(), "Attack")
                        || ArrayUtils.contains(composition.getActions(), "Pick"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSalveTarget(NPC npc) {
        MonsterData monsterData = monsterTable.getMonsterData(npc.getId());
        List<String> monsterAttributes = Arrays.asList(monsterData.getAttributes());
        if (monsterAttributes.contains("undead")) return true;
        return false;
    }

    HitDist processAttack(WeaponStance weaponStance, AttackStyle attackStyle, MonsterData npcData, boolean usedSpecialAttack) {

        boolean UNIQUE_CASE = false;
        double tgtBonus = 1.0D;
        double specialBonus = 1.0D;

        NPC targetedNpc = (NPC) lastInteracting;

        boolean slayerTarget = isSlayerTarget(targetedNpc);
        boolean salveTarget = isSalveTarget(targetedNpc);
        double gearBonus = LuckTrackerUtil.getGearBonus(this.wornItems, slayerTarget, salveTarget, attackStyle);

        if (slayerTarget) UTIL.sendChatMessage("Attacking Slayer Target");
        if (salveTarget) UTIL.sendChatMessage("Attacking SALVE target");
        if (gearBonus != 1.0D) UTIL.sendChatMessage("Gear bonus == " + gearBonus);

        int npcCurrentHp = UTIL.getNpcCurrentHp(targetedNpc);

        if (!UNIQUE_CASE) {
            Attack attack;
            EquipmentStat equipmentStatOffense = attackStyle.getEquipmentStat(); // Using the attackStyle, figure out which worn-equipment stat we should use
            EquipmentStat equipmentStatDefense = LuckTrackerUtil.getDefensiveStat(equipmentStatOffense); // Get the defensive stat to generate NPC's defense roll

            int defRoll = npcData.calcDefenseRoll(equipmentStatDefense);

            if (weaponStance == WeaponStance.CASTING || weaponStance == WeaponStance.DEFENSIVE_CASTING) {
                UTIL.sendChatMessage("Spell cast");
                attack = new Attack(1000, 10);
            } else if (weaponStance == WeaponStance.POWERED_STAFF_ACCURATE || weaponStance == WeaponStance.POWERED_STAFF_LONGRANGE) {
                UTIL.sendChatMessage("Powered staff");
                attack = new Attack(1000, 10);
            } else if (weaponStance == WeaponStance.RANGE_ACCURATE || weaponStance == WeaponStance.RANGE_LONGRANGE || weaponStance == WeaponStance.RAPID) {
                int effRangeAtt = LuckTrackerUtil.calcEffectiveRangeAttack(client.getBoostedSkillLevel(Skill.RANGED), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_RATT), weaponStance.getInvisBonus(Skill.RANGED), isWearingRangeVoid, isWearingRangeEliteVoid);
                int effRangeStr = LuckTrackerUtil.calcEffectiveRangeStrength(client.getBoostedSkillLevel(Skill.RANGED), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_RSTR), weaponStance.getInvisBonus(Skill.RANGED), isWearingRangeVoid, isWearingRangeEliteVoid);
                int attRoll = LuckTrackerUtil.calcBasicRangeAttackRoll(effRangeAtt, UTIL.getEquipmentStyleBonus(wornItems, equipmentStatOffense), gearBonus);
                int maxHit = LuckTrackerUtil.calcRangeBasicMaxHit(effRangeStr, UTIL.getEquipmentStyleBonus(wornItems, EquipmentStat.RSTR), gearBonus, specialBonus);
//				UTIL.sendChatMessage(String.format("RANGE HIT -- effRangeAtt = %d / effRangeStr = %d / Attack roll = %d / Max Hit = %d", effRangeAtt, effRangeStr, attRoll, maxHit));
                attack = new Attack(attRoll, maxHit);
            } else if (weaponStance == WeaponStance.ACCURATE || weaponStance == WeaponStance.AGGRESSIVE || weaponStance == WeaponStance.DEFENSIVE || weaponStance == WeaponStance.CONTROLLED) {
                int effStrLvl = LuckTrackerUtil.calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.ATTACK), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_STR), weaponStance.getInvisBonus(Skill.STRENGTH), isWearingMeleeVoid || isWearingMeleeEliteVoid);
                int effAttLvl = LuckTrackerUtil.calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.STRENGTH), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_ATT), weaponStance.getInvisBonus(Skill.ATTACK), isWearingMeleeVoid || isWearingMeleeEliteVoid);
                int attRoll = LuckTrackerUtil.calcBasicMeleeAttackRoll(effAttLvl, UTIL.getEquipmentStyleBonus(wornItems, equipmentStatOffense), tgtBonus);
                int maxHit = LuckTrackerUtil.calcBasicMaxHit(effStrLvl, UTIL.getEquipmentStyleBonus(wornItems, EquipmentStat.STR), tgtBonus);
//				UTIL.sendChatMessage(String.format("effAttLvl = %d / effStrLvl = %d / Attack roll = %d / Max Hit = %d", effAttLvl, effStrLvl, attRoll, maxHit));
                attack = new Attack(attRoll, maxHit);
            } else {
                UTIL.sendChatMessage("FAILED TO IDENTIFY ATTACK TYPE");
                attack = new Attack(1000, 10);
            }

            return new HitDist(LuckTrackerUtil.getHitChance(attack.getAttRoll(), defRoll), attack.getMaxHit(), npcCurrentHp);
        }
        return new HitDist(0.1f, 10);
    }
}
