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
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.game.NpcInfo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.http.api.item.ItemEquipmentStats;
import org.xml.sax.ErrorHandler;

import javax.inject.Inject;
import java.io.File;

@PluginDescriptor(
		name = "Luck Tracker",
		description = "Stats on how lucky you are in combat",
		tags = {"combat", "rng"},
		enabledByDefault = false
)

public class LuckTrackerPlugin extends Plugin {

	private TickCounterUtil tickCounterUtil; // Used for identifying attack animations
	private Actor lastInteracting; // Keep track of targetted NPC; will update every game tick
	private Monsters monsterTable;
	private LuckTrackerUtil UTIL;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private LuckTrackerConfig config;
	@Inject private ItemManager itemManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private NPCManager npcManager;

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
	}

	@Override
	protected void shutDown() {
		assert true;
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event) {
		// TODO set lastInteracting here? Not sure if these fires before onGameTick / if this is upstream of onAnimationChange
	}


	// ************************************************** //
	// region Hit Processing Functions
	// Not pure functions: will still need to pull information from client/player
	// Pass in equipmentStat and weaponStance where possible, since those must already be calculated anyway
	// (Otherwise you wouldn't know what type of hit this was)

	private void processMeleeHit(EquipmentStat equipmentStat, WeaponStance weaponStance) {

		boolean voidArmor = false;
		double tgtBonus = 1.0; // Might need to be 2 diff target bonuses, one for str one for att?

		int effStrLvl = LuckTrackerUtil.calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.ATTACK), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_STR), weaponStance.getInvisBonus(Skill.STRENGTH), voidArmor);
		int effAttLvl = LuckTrackerUtil.calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.STRENGTH), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_ATT), weaponStance.getInvisBonus(Skill.ATTACK), voidArmor);
		int attRoll = LuckTrackerUtil.calcBasicMeleeAttackRoll(effAttLvl, UTIL.getEquipmentStyleBonus(equipmentStat), tgtBonus);
		int maxHit = LuckTrackerUtil.calcBasicMaxHit(effStrLvl, UTIL.getEquipmentStyleBonus(EquipmentStat.STR), tgtBonus);
//		UTIL.sendChatMessage(String.format("effAttLvl = %d / effStrLvl = %d / Attack roll = %d / Max Hit = %d", effAttLvl, effStrLvl, attRoll, maxHit));
	}

	private void processMagicSpell() {
		UTIL.sendChatMessage("processMagicSpell() called");
	}

	private void processPoweredStaffHit() {
		UTIL.sendChatMessage("processPoweredStaffHit() called");
	}

	private void processRangedHit(EquipmentStat equipmentStat, WeaponStance weaponStance) {

		boolean voidArmor = false;
		boolean voidEliteArmor = false;
		double gearBonus = 1.0;
		double specialBonus = 1.0;

		int effRangeAtt = LuckTrackerUtil.calcEffectiveRangeAttack(client.getBoostedSkillLevel(Skill.RANGED), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_RATT), weaponStance.getInvisBonus(Skill.RANGED), voidArmor, voidEliteArmor);
		int effRangeStr = LuckTrackerUtil.calcEffectiveRangeStrength(client.getBoostedSkillLevel(Skill.RANGED), UTIL.getActivePrayerModifiers(PrayerAttribute.PRAY_RSTR), weaponStance.getInvisBonus(Skill.RANGED), voidArmor, voidEliteArmor);
		int attRoll = LuckTrackerUtil.calcBasicRangeAttackRoll(effRangeAtt, UTIL.getEquipmentStyleBonus(equipmentStat), gearBonus);
		int maxHit = LuckTrackerUtil.calcRangeBasicMaxHit(effRangeStr, UTIL.getEquipmentStyleBonus(EquipmentStat.RSTR), gearBonus, specialBonus);
//		UTIL.sendChatMessage(String.format("RANGE HIT -- effRangeAtt = %d / effRangeStr = %d / Attack roll = %d / Max Hit = %d", effRangeAtt, effRangeStr, attRoll, maxHit));
	}

	// endregion
	// ************************************************** //

	@Subscribe
	public void onGameTick(GameTick gameTick) {

		Player player = client.getLocalPlayer();
		if (player == null) return;

		// Update currently interacting NPC
		Actor interactingActor = player.getInteracting();
		if (interactingActor instanceof NPC) lastInteracting = interactingActor;
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged e) { // Main hook for identifying when we perform an attack: our animation changes. Will use PVMTickCounter's utility to determine
		// TODO most weapon anims are 2 ticks; so 2t weapons won't get tracked properly using just onAnimationChanged
		//		Ian idea: instead of tracking onAnimationChanged, keep track of player's animation and re-set it every 2 ticks
		//		Alternative: PVMTickCounter uses an "IsBPing" flag
		// TODO hook up salamander blaze and flare; no animation on player, but it spawns G = 952.
		//  Also potentially a lot of other attack animations.
		// 	See tickCounterUtil -> aniTM.

		if (!(e.getActor() instanceof Player)) return;

		Player p = (Player) e.getActor();
		if (p != client.getLocalPlayer()) return;

		if (!tickCounterUtil.isAttack(p.getAnimation())) return; // If the animation isn't an attack, stop here

		PlayerComposition pc = p.getPlayerComposition();
		if (p.getPlayerComposition() == null) return;

		// ************************************************** //
		// Player Attack processing

		int attackStyleId = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		int weaponTypeId = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);

		WeaponStance weaponStance = WeaponType.getWeaponStance(weaponTypeId, attackStyleId); // Determine weapon stance (Controlled, Aggressive, Rapid, etc.)
		AttackStyle attackStyle = WeaponType.getAttackStyle(weaponTypeId, attackStyleId); // Determine if we're using slash, crush, stab, range, or magic based on the weapon type and current stance
		EquipmentStat equipmentStat = attackStyle.getEquipmentStat(); // Using the attackStyle, figure out which worn-equipment stat we should use

		// TODO Casting a spell will take whatever stance is currently active... Which is only accurate if autocasting. For casting spells specifically, will probably need to short circuit based on animation?

		if (weaponStance == WeaponStance.CASTING || weaponStance == WeaponStance.DEFENSIVE_CASTING)
		{
			processMagicSpell();
		}
		else if (weaponStance == WeaponStance.POWERED_STAFF_ACCURATE || weaponStance == WeaponStance.POWERED_STAFF_LONGRANGE)
		{
			processPoweredStaffHit();
		}
		else if (weaponStance == WeaponStance.RANGE_ACCURATE || weaponStance == WeaponStance.RANGE_LONGRANGE || weaponStance == WeaponStance.RAPID)
		{
			processRangedHit(equipmentStat, weaponStance);
		}
		else if (weaponStance == WeaponStance.ACCURATE || weaponStance == WeaponStance.AGGRESSIVE || weaponStance == WeaponStance.DEFENSIVE || weaponStance == WeaponStance.CONTROLLED)
		{
			processMeleeHit(equipmentStat, weaponStance);
		}

		// ************************************************** //
		// NPC Defense processing

		int npcId = ((NPC) lastInteracting).getId();
		MonsterData npcData = monsterTable.getMonsterData(npcId);
		EquipmentStat opponentDefenseStat = LuckTrackerUtil.getDefensiveStat(equipmentStat); // Get the defensive stat to generate NPC's defense roll

		if (npcData == null) {
			UTIL.sendChatMessage("UNKNOWN MONSTER");
			return;
		}

		String npcName = npcData.getName();
		int npcDefRoll = npcData.calcDefenseRoll(opponentDefenseStat);

		UTIL.sendChatMessage(String.format("Monster: %s || Def lvl: %d || Defense roll (%s): %d", npcName, npcData.getDefLvl(), opponentDefenseStat.name(), npcDefRoll));

		// Calculate damage distribution

	}
}
