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
	}

	@Override
	protected void shutDown() {
		assert true;
	}

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

	@Subscribe
	public void onInteractingChanged(InteractingChanged event) {
		// TODO set lastInteracting here? Not sure if these fires before onGameTick / if this is upstream of onAnimationChange
	}

	// region NPC Utility Functions
	private int getNpcCurrentHp() { // Logic from OpponentInfo plugin
		NPC interactingNpc = (NPC) lastInteracting;
		int healthRatio = interactingNpc.getHealthRatio();
		int healthScale = interactingNpc.getHealthScale();
		int maxHealth = npcManager.getHealth(interactingNpc.getId());
		int minHealth = maxHealth;
		if (healthScale > 1) {
			if (healthRatio > 1) {
				minHealth = (maxHealth * (healthRatio - 1) + healthScale - 2) / (healthScale - 1);
			}
			maxHealth = (maxHealth * healthRatio - 1) / (healthScale - 1);
		}

		int currentHp = (minHealth + maxHealth + 1) / 2;

		return currentHp;
	}
	// endregion

	// region Equipment & Prayer Utility Functions

	public double getActivePrayerModifiers(PrayerAttribute prayerAttribute) { // For a given PrayerAttribute -- go through all active prayers and figure out the total modifier to that PrayerAttribute
		double mod = 1.0D;
		for (Prayer prayer: Prayer.values()) {
			if (client.getVarbitValue(prayer.getVarbit()) > 0) { // Using a custom Prayer class, so we lost access to client.getPrayerActive() or whatever it is
//				sendChatMessage(String.format("Idenfitied active prayer varbit %d", prayer.getVarbit()));
				mod += prayer.getPrayerAttributeMod(prayerAttribute);
			}
		}
//		sendChatMessage(String.format("Total prayer mod for %s is %f", prayerAttribute.getName(), mod));
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

	// region Combat Utility Functions -- Generic
	public int calcBasicDefenceRoll(int defLvl, int styleDefBonus) { // Calculates an NPC's defensive roll.
		return (defLvl + 9) * (styleDefBonus + 64);
	} // endregion

	// region Combat Utility Functions -- Melee
	public int calcEffectiveMeleeLevel(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor) { // Calculates effective attack or strength level.
		int effLvl = ((int) (visibleLvl * prayerBonus)) + styleBonus + 8; // Internal cast necessary; int * double promotes to double
		return voidArmor ? ((int) (effLvl * 1.1D)) : effLvl;
	}

	public int calcBasicMeleeAttackRoll(int effAttLvl, int attBonus, double tgtBonus) { // Calculate attack roll.
		return (int) (effAttLvl * (attBonus + 64) * tgtBonus);
	}

	public int calcBasicMaxHit(int effStrLvl, int strBonus, double tgtBonus) { // Calculates max hit based on effective strength level and strength bonus (from equipment stats).
		return (int) (tgtBonus * ((effStrLvl * (strBonus + 64) + 320) / 640)); // Internal cast not necessary; int division will automatically return floored int
	} // endregion

	// region Combat Utility Functions -- Range
	public int calcEffectiveRangeAttack(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor, boolean voidEliteArmor) {
		double voidBonus;
		if (voidEliteArmor || voidArmor) voidBonus = 1.1;
		else voidBonus = 1;
		return (int) (voidBonus * (((int) (visibleLvl * prayerBonus)) + styleBonus + 8));
	}

	public int calcEffectiveRangeStrength(int effRangeStr, double prayerBonus, int styleBonus, boolean voidArmor, boolean voidEliteArmor) {
		double voidBonus;
		if (voidEliteArmor) voidBonus = 1.125;
		else if (voidArmor) voidBonus = 1.1;
		else voidBonus = 1;
		return (int) (voidBonus * (((int) (effRangeStr * prayerBonus)) + styleBonus + 8));
	}

	public int calcBasicRangeAttackRoll(int effRangeAtt, int rangeAttBonus, double gearBonus) { // Calculate ranged attack roll.
		return (int) (effRangeAtt * (rangeAttBonus + 64) * gearBonus);
	}

	public int calcRangeBasicMaxHit(int effRangeStrength, int rStrBonus, double gearBonus, double specialBonus) {
		return (int) (specialBonus * ((int) (0.5 + (effRangeStrength * (rStrBonus + 64)) / 640 * gearBonus)));
	} // endregion

	// ************************************************** //
	// region Hit Processing Functions
	// Not pure functions: will still need to pull information from client/player
	// Pass in equipmentStat and weaponStance where possible, since those must already be calculated anyway
	// (Otherwise you wouldn't know what type of hit this was)

	private void processMeleeHit(EquipmentStat equipmentStat, WeaponStance weaponStance) {

		boolean voidArmor = false;
		double tgtBonus = 1.0; // Might need to be 2 diff target bonuses, one for str one for att?

		int effStrLvl = calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.ATTACK), getActivePrayerModifiers(PrayerAttribute.PRAY_STR), weaponStance.getInvisBonus(Skill.STRENGTH), voidArmor);
		int effAttLvl = calcEffectiveMeleeLevel(client.getBoostedSkillLevel(Skill.STRENGTH), getActivePrayerModifiers(PrayerAttribute.PRAY_ATT), weaponStance.getInvisBonus(Skill.ATTACK), voidArmor);
		int attRoll = calcBasicMeleeAttackRoll(effAttLvl, getEquipmentStyleBonus(equipmentStat), tgtBonus);
		int maxHit = calcBasicMaxHit(effStrLvl, getEquipmentStyleBonus(EquipmentStat.STR), tgtBonus);
		sendChatMessage(String.format("effAttLvl = %d / effStrLvl = %d / Attack roll = %d / Max Hit = %d", effAttLvl, effStrLvl, attRoll, maxHit));
	}

	private void processMagicSpell() {
		sendChatMessage("processMagicSpell() called");
	}

	private void processPoweredStaffHit() {
		sendChatMessage("processPoweredStaffHit() called");
	}

	private void processRangedHit(EquipmentStat equipmentStat, WeaponStance weaponStance) {

		boolean voidArmor = false;
		boolean voidEliteArmor = false;
		double gearBonus = 1.0;
		double specialBonus = 1.0;

		int effRangeAtt = calcEffectiveRangeAttack(client.getBoostedSkillLevel(Skill.RANGED), getActivePrayerModifiers(PrayerAttribute.PRAY_RATT), weaponStance.getInvisBonus(Skill.RANGED), voidArmor, voidEliteArmor);
		int effRangeStr = calcEffectiveRangeStrength(client.getBoostedSkillLevel(Skill.RANGED), getActivePrayerModifiers(PrayerAttribute.PRAY_RSTR), weaponStance.getInvisBonus(Skill.RANGED), voidArmor, voidEliteArmor);
		int attRoll = calcBasicRangeAttackRoll(effRangeAtt, getEquipmentStyleBonus(equipmentStat), gearBonus);
		int maxHit = calcRangeBasicMaxHit(effRangeStr, getEquipmentStyleBonus(EquipmentStat.RSTR), gearBonus, specialBonus);
//		sendChatMessage(String.format("RANGE HIT -- effRangeAtt = %d / effRangeStr = %d / Attack roll = %d / Max Hit = %d", effRangeAtt, effRangeStr, attRoll, maxHit));
	}

	// endregion
	// ************************************************** //

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		Player player = client.getLocalPlayer();
		if (player == null) return;

		// Update currently interacting NPC
		Actor interactingActor = player.getInteracting();
		if (!(interactingActor instanceof NPC)) return;
		lastInteracting = interactingActor;

		// DEBUG STUFF
//		int npcId = ((NPC) lastInteracting).getId();
//		MonsterData npcData = monsterTable.getMonsterData(npcId);
//
//		String npcName;
//		int npcDcrush;
//		try {
//			npcName = npcData.getName();
//		}
//		catch (Exception asdf) {
//			npcName = "ERROR";
//		}
//
//		try {
//			npcDcrush = npcData.getDCrush();
//		}
//		catch (Exception asdf) {
//			npcDcrush = -999;
//		}
//
//		sendChatMessage(String.format("ID: %d || Monster: %s || Crush defense: %d", npcId, npcName, npcDcrush));
	}


	// TODO most weapon anims are 2 ticks; so 2t weapons won't get tracked properly using just onAnimationChanged
	//		Ian idea: instead of tracking onAnimationChanged, keep track of player's animation and re-set it every 2 ticks
	//		Alternative: PVMTickCounter uses an "IsBPing" flag

	@Subscribe
	private void onAnimationChanged(AnimationChanged e) { // Main hook for identifying when we perform an attack: our animation changes. Will use PVMTickCounter's utility to determine

		// ** Get handle on player and animation, ensure it's us ** //

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

		int attackStyleId = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		int weaponTypeId = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);

		WeaponStance weaponStance = WeaponType.getWeaponStance(weaponTypeId, attackStyleId); // Determine weapon stance (Controlled, Aggressive, Rapid, etc.)
		AttackStyle attackStyle = WeaponType.getAttackStyle(weaponTypeId, attackStyleId); // Determine if we're using slash, crush, stab, range, or magic based on the weapon type and current stance
		EquipmentStat equipmentStat = attackStyle.getEquipmentStat(); // Using the attackStyle, figure out which worn-equipment stat we should use

		// TODO Casting a spell will take whatever stance is currently active...
		//		Which is only accurate if autocasting.
		//		For casting spells specifically, will probably need to short circuit based on animation?

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

//		sendChatMessage(String.format("Weapon stance: %s", weaponStance.getName()));
//		sendChatMessage(String.format("Invisible ATT bonus = %d / Invisible STR bonus = %d", weaponStance.getInvisBonus(Skill.ATTACK), strStanceBonus));

		// Calculate damage distribution

	}

	@Subscribe
	private void onStatChanged(StatChanged statChanged) // If a StatChanged event happens (e.g. gained XP)
	{
		return;
	}
}
