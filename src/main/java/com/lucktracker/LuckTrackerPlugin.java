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
	private LuckTrackerUtil UTIL;
	private Item[] wornItems;
	private int specialAttackEnergy;
	private boolean usedSpecialAttack;

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

		clientThread.invokeLater(() -> {
			// Get worn items on startup
			final ItemContainer container = client.getItemContainer(InventoryID.EQUIPMENT);
			if (container != null) wornItems = container.getItems();

			// Get special attack energy, initialize the spec boolean
			this.usedSpecialAttack = false;
			this.specialAttackEnergy = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
		});
	}

	@Override
	protected void shutDown() {
		System.out.println("Dummy statement");
		assert true;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {

		Player player = client.getLocalPlayer();
		if (player == null) return;

		// Update currently interacting NPC
		Actor interactingActor = player.getInteracting();
		if (interactingActor instanceof NPC) this.lastInteracting = interactingActor;

		// Reset the special attack bool
		clientThread.invokeLater(() -> {this.usedSpecialAttack = false;});
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (event.getVarpId() != VarPlayer.SPECIAL_ATTACK_PERCENT) return;
		if (event.getValue() < this.specialAttackEnergy) this.usedSpecialAttack = true;
		this.specialAttackEnergy = event.getValue();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT)) {
			return;
		}

		this.wornItems = event.getItemContainer().getItems();

		clientThread.invokeLater(this::updateVoidStatus);
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
			if (npcData == null) { UTIL.sendChatMessage("UNKNOWN MONSTER"); return; }


			HitDist hitDist = processAttack(weaponStance, attackStyle, npcData, this.usedSpecialAttack);

//			HitDist hitDist = new HitDist(LuckTrackerUtil.getHitChance(attack.getAttRoll(), npcDefRoll), attack.getMaxHit());

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

	HitDist processAttack(WeaponStance weaponStance, AttackStyle attackStyle, MonsterData npcData, boolean usedSpecialAttack) {

		boolean UNIQUE_CASE = false;

		if (!UNIQUE_CASE) {
			EquipmentStat equipmentStatOffense = attackStyle.getEquipmentStat(); // Using the attackStyle, figure out which worn-equipment stat we should use
			EquipmentStat equipmentStatDefense = LuckTrackerUtil.getDefensiveStat(equipmentStatOffense); // Get the defensive stat to generate NPC's defense roll
		}



		return new HitDist(0.1f, 10);
	}

}
