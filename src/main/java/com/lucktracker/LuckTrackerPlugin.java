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
import net.runelite.api.ItemComposition;
import net.runelite.api.Prayer;
import net.runelite.api.kit.KitType; // For identifying equipped weapon | int weapon = playerComposition.getEquipmentId(KitType.WEAPON);
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

@PluginDescriptor(
		name = "Luck Tracker",
		description = "Stats on how lucky you are in combat",
		tags = {"combat", "rng"},
		enabledByDefault = false
)

public class LuckTrackerPlugin extends Plugin {

	private TickCounterUtil tickCounterUtil;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private LuckTrackerConfig config;
	@Inject private ItemManager itemManager;
	@Inject private ChatMessageManager chatMessageManager;

	@Provides
	LuckTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LuckTrackerConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		tickCounterUtil = new TickCounterUtil(); // Utility ripped from PVMTickCounter plugin: dictionary of (animation_ID, tick_duration) pairs, plus method to extract ambiguous animations.
		tickCounterUtil.init();
	}

	@Override
	protected void shutDown() {
		assert true;
	}

	@Subscribe public void onVarbitChanged(VarbitChanged event) { return; }

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
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
		Player player = client.getLocalPlayer(); // get a handle on the player
		Actor actor = hitsplatApplied.getActor(); // get a handle on the attacked entity
		if (!(actor instanceof NPC)) { return; } // only look for hitsplats applied to NPCs

		Hitsplat hitsplat = hitsplatApplied.getHitsplat(); // get a handle on the hitsplat

		if (hitsplat.isMine()) { // if it's our own hitsplat...
			int hit = hitsplat.getAmount();
		}
	}

	public int getInteractingNpcIndex() { // If the player is interacting with an NPC, returns its NPC Index. If not, return -1.
		Player player = client.getLocalPlayer();
		if (player == null) return -1;
		PlayerComposition playerComposition = player.getPlayerComposition();
		if (playerComposition == null) return -1;

		Actor interacted = player.getInteracting();
		if (interacted instanceof NPC) {
			NPC interactedNPC = (NPC) interacted;
			return interactedNPC.getIndex();
		}
		return -1;
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

	public void dummy() {
		client.getVar(VarPlayer.ATTACK_STYLE);
		client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
	}

	public int getEquipmentStyleBonus(EquipmentStat equipmentStat) { // Calculates the total bonus for a given equipment stat (e.g. ACRUSH, PRAYER, DSLASH...)
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

	public int calcEffectiveMeleeLevel(int visibleLvl, double prayerBonus, int styleBonus, boolean voidArmor) { // Calculates effective attack or strength level.
		int effLvl = ((int) (visibleLvl * prayerBonus)) + styleBonus + 8; // Internal cast necessary; int * double promotes to double
		return voidArmor ? ((int) (effLvl * 1.1D)) : effLvl;
	}

	public int calcBasicAttackRoll(int effAttLvl, int attBonus, double tgtBonus) { // Calculate attack roll.
		return (int) (effAttLvl * (attBonus + 64) * tgtBonus);
	}

	public int calcBasicDefenceRoll(int defLvl, int styleDefBonus) { // Calculates an NPC's defensive roll.
		return (defLvl + 9) * (styleDefBonus + 64);
	}

	public int calcBasicMaxHit(int effStrLvl, int strBonus, double tgtBonus) { // Calculates max hit based on effective strength level and strength bonus (from equipment stats).
		return (int) (tgtBonus * ((effStrLvl * (strBonus + 64) + 320) / 640)); // Internal cast not necessary; int division will automatically return floored int
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged e) { // Main hook for identifying when we perform an attack: our animation changes. Will use PVMTickCounter's utility to determine

		if (!(e.getActor() instanceof Player)) return;

		// TODO hook up salamander blaze and flare; no animation on player, but it spawns G = 952

		Player p = (Player) e.getActor();
		if (p != client.getLocalPlayer()) return;

		if (!tickCounterUtil.isAttack(p.getAnimation())) return; // If the animation isn't an attack, stop here

		PlayerComposition pc = p.getPlayerComposition();
		if (p.getPlayerComposition() == null) return;

		int attackStyleId = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		int weaponTypeId = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);

		WeaponStance weaponStance = WeaponType.getWeaponStance(weaponTypeId, attackStyleId);
		AttackStyle attackStyle = WeaponType.getAttackStyle(weaponTypeId, attackStyleId);
		EquipmentStat equipmentStat = attackStyle.getEquipmentStat();

		boolean wearingVoid = false;
		double prayerBonus = 1.0D;
		int visibleLvl = 99;

		sendChatMessage(String.format("Weapon stance: %s", weaponStance.getName()));

		int attStanceBonus = weaponStance.getInvisBonus(Skill.ATTACK);
		int strStanceBonus = weaponStance.getInvisBonus(Skill.STRENGTH);
		int attBonus = getEquipmentStyleBonus(equipmentStat);
		int effAttLvl = calcEffectiveMeleeLevel(visibleLvl, prayerBonus, 0, wearingVoid);
		int attRoll = calcBasicAttackRoll(effAttLvl, attBonus, 1.0D);

		String attackStyleString = attackStyle.getName();

		sendChatMessage(String.format("Invisible ATT bonus = %d / Invisible STR bonus = %d", attStanceBonus, strStanceBonus));
		sendChatMessage(String.format("effAttLvl = %d / Attack roll = %d", effAttLvl, attRoll));

//		sendChatMessage(String.format("--Attack style name is %s", attackStyleString));
		// sendChatMessage(String.format("--Offensive slash bonus is %d", getEquipmentStyleBonus(EquipmentStat.ASLASH)));
		// sendChatMessage(String.format("Defensive magic bonus is %d", getEquipmentStyleBonus(EquipmentStat.DMAGIC)));
		// sendChatMessage(String.format("ASPEED is %d", getEquipmentStyleBonus(EquipmentStat.ASPEED)));
		// sendChatMessage(String.format("Prayer bonus is %d", getEquipmentStyleBonus(EquipmentStat.PRAYER)));


		// Calculate damage distribution

	}

	@Subscribe
	private void onStatChanged(StatChanged statChanged) // If a StatChanged event happens (e.g. gained XP)
	{
		return;
	}
}
