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

	static final List<Skill> skills = Arrays.asList(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC);
	static final List<DmgAttribute> dmgAttributes = Arrays.asList(DmgAttribute.ATTACK, DmgAttribute.STRENGTH, DmgAttribute.DEFENCE, DmgAttribute.RANGED, DmgAttribute.RANGED_DEFENCE, DmgAttribute.RANGED_STRENGTH, DmgAttribute.MAGIC, DmgAttribute.MAGIC_DEFENCE, DmgAttribute.MAGIC_STRENGTH);

	private final Map<Skill, Integer> previousXpMap = new EnumMap<Skill, Integer>(Skill.class); // Create a Map of skill XPs. Can use this to calculate XP drops: XP drop = new xp - existing xp ---> set existing xp = new xp
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
		clientThread.invoke(this::initPreviousXpMap); // Initialize Map of skill XP's, used for calculating XP drops
		tickCounterUtil = new TickCounterUtil(); // Utility ripped from PVMTickCounter plugin: dictionary of (animation_ID, tick_duration) pairs, plus method to extract ambiguous animations.
		tickCounterUtil.init();
	}

	@Override
	protected void shutDown() {
		assert true;
	}

	private void initPreviousXpMap()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			previousXpMap.clear();
		}
		else
		{
			for (final Skill skill: Skill.values())
			{
				previousXpMap.put(skill, client.getSkillExperience(skill));
			}
		}
	}

	@Subscribe public void onVarbitChanged(VarbitChanged event) { return; }

	private void sendChatMessage(String chatMessage) {
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
		return;
	}

	private double getXpModifier(int npcIndex) { // Return XP modifier for a given npcIndex.
		return 1;
	}

	private int getDamageFromXpDrop(Skill skill, int xp, int npcIndex) { // Calculates Damage dealt based on XP drop to current NPC target.

		int adjustedXp = (int) (xp / getXpModifier(npcIndex)); // Some NPCs grant bonus XP (or reduced XP). Normalize the actual XP drop to an "adjusted" value as if there were no modifier.

		switch(skill) {
			case ATTACK:
			case STRENGTH:
			case DEFENCE:
				return (int) ((double) adjustedXp / 4.0D);
			case RANGED:
				if (client.getVarpValue(VarPlayer.ATTACK_STYLE) == 3) return (int) ((double) xp / 2.0D); // Defensive ranged
				return (int) ((double) xp / 4.0D);
			default:
				return -1;
		}
	}

	int getInteractingNpcIndex() { // If the player is interacting with an NPC, returns its NPC Index. If not, return -1.
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

	@Subscribe
	private void onAnimationChanged(AnimationChanged e) { // Main hook for identifying when we perform an attack: our animation changes. Will use PVMTickCounter's utility to determine
		if (!(e.getActor() instanceof Player)) return;

		Player p = (Player) e.getActor();
		if (p != client.getLocalPlayer()) return;

		if (!tickCounterUtil.isAttack(p.getAnimation())) return; // If the animation isn't an attack, stop here

		PlayerComposition pc = p.getPlayerComposition();
		if (p.getPlayerComposition() == null) return;


		// Check if we gained any XP since last

	}

	@Subscribe
	private void onFakeXpDrop(FakeXpDrop fakeXpDrop) { // If a FakeXpDrop event happens, process the XP drop
		getDamageFromXpDrop(fakeXpDrop.getSkill(), fakeXpDrop.getXp(), getInteractingNpcIndex());
	}

	@Subscribe
	private void onStatChanged(StatChanged statChanged) // If a StatChanged event happens (e.g. gained XP), process the XP drop
	{
		Skill skill = statChanged.getSkill();
		final int xpAfter = client.getSkillExperience(skill); // Get new XP
		final int xpBefore = previousXpMap.getOrDefault(skill, -1); // Get previous XP
		previousXpMap.put(skill, xpAfter); // Update XP Map with new XP

		final int xpDrop = xpAfter - xpBefore;

		sendChatMessage(String.format("Gained %d xp in %s", xpDrop, skill.getName()));

		switch(skill) {
			case ATTACK:
			case STRENGTH:
			case DEFENCE:
			case RANGED:
			case MAGIC:
				int damage = getDamageFromXpDrop(skill, xpDrop, getInteractingNpcIndex());
				break;
		}
	}
}
