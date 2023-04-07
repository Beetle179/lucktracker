/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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

import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;

import java.util.Arrays;
import java.util.List;

enum PrayerAttribute {
    PRAY_ATT("Attack"),
    PRAY_STR("Strength"),
    PRAY_DEF("Defense"),
    PRAY_RATT("Ranged Attack"),
    PRAY_RSTR("Ranged Strength"),
    PRAY_RDEF("Ranged Defense"),
    PRAY_MATT("Magic Attack"),
    PRAY_MSTR("Magic Strength"),
    PRAY_MDEF("Magic Defense");

    private final String name;

    PrayerAttribute(String name) {
        this.name = name;
    }

    String getName() { return this.name; }
}

public enum Prayer
{
    THICK_SKIN(Varbits.PRAYER_THICK_SKIN, 5.0,                          new double[] {0.05D}, PrayerAttribute.PRAY_DEF),
    BURST_OF_STRENGTH(Varbits.PRAYER_BURST_OF_STRENGTH, 5.0,            new double[] {0.05D}, PrayerAttribute.PRAY_STR),
    CLARITY_OF_THOUGHT(Varbits.PRAYER_CLARITY_OF_THOUGHT, 5.0,          new double[] {0.05D}, PrayerAttribute.PRAY_ATT),
    SHARP_EYE(Varbits.PRAYER_SHARP_EYE, 5.0,                            new double[] {0.05D, 0.05D}, PrayerAttribute.PRAY_RATT, PrayerAttribute.PRAY_RSTR),
    MYSTIC_WILL(Varbits.PRAYER_MYSTIC_WILL, 5.0,                        new double[] {0.05D, 0.05D}, PrayerAttribute.PRAY_MATT, PrayerAttribute.PRAY_MDEF),
    ROCK_SKIN(Varbits.PRAYER_ROCK_SKIN, 10.0,                           new double[] {0.1D}, PrayerAttribute.PRAY_DEF),
    SUPERHUMAN_STRENGTH(Varbits.PRAYER_SUPERHUMAN_STRENGTH, 10.0,       new double[] {0.1D}, PrayerAttribute.PRAY_STR),
    IMPROVED_REFLEXES(Varbits.PRAYER_IMPROVED_REFLEXES, 10.0,           new double[] {0.1D}, PrayerAttribute.PRAY_ATT),
    RAPID_RESTORE(Varbits.PRAYER_RAPID_RESTORE, 60.0 / 36.0,            new double[] {}),
    RAPID_HEAL(Varbits.PRAYER_RAPID_HEAL, 60.0 / 18,                    new double[] {}),
    PROTECT_ITEM(Varbits.PRAYER_PROTECT_ITEM, 60.0 / 18,                new double[] {}),
    HAWK_EYE(Varbits.PRAYER_HAWK_EYE, 10.0,                             new double[] {0.1D, 0.1D}, PrayerAttribute.PRAY_RATT, PrayerAttribute.PRAY_RSTR),
    MYSTIC_LORE(Varbits.PRAYER_MYSTIC_LORE, 10.0,                       new double[] {0.1D, 0.1D}, PrayerAttribute.PRAY_MATT, PrayerAttribute.PRAY_MDEF),
    STEEL_SKIN(Varbits.PRAYER_STEEL_SKIN, 20.0,                         new double[] {0.15D}, PrayerAttribute.PRAY_DEF),
    ULTIMATE_STRENGTH(Varbits.PRAYER_ULTIMATE_STRENGTH, 20.0,           new double[] {0.15D}, PrayerAttribute.PRAY_STR),
    INCREDIBLE_REFLEXES(Varbits.PRAYER_INCREDIBLE_REFLEXES, 20.0,       new double[] {0.15D}, PrayerAttribute.PRAY_ATT),
    PROTECT_FROM_MAGIC(Varbits.PRAYER_PROTECT_FROM_MAGIC, 20.0,         new double[] {}),
    PROTECT_FROM_MISSILES(Varbits.PRAYER_PROTECT_FROM_MISSILES, 20.0,   new double[] {}),
    PROTECT_FROM_MELEE(Varbits.PRAYER_PROTECT_FROM_MELEE, 20.0,         new double[] {}),
    EAGLE_EYE(Varbits.PRAYER_EAGLE_EYE, 20.0,                           new double[] {0.15D, 0.15D}, PrayerAttribute.PRAY_RATT, PrayerAttribute.PRAY_RSTR),
    MYSTIC_MIGHT(Varbits.PRAYER_MYSTIC_MIGHT, 20.0,                     new double[] {0.15D, 0.15D}, PrayerAttribute.PRAY_MATT, PrayerAttribute.PRAY_MDEF),
    RETRIBUTION(Varbits.PRAYER_RETRIBUTION, 5.0,                        new double[] {}),
    REDEMPTION(Varbits.PRAYER_REDEMPTION, 10.0,                         new double[] {}),
    SMITE(Varbits.PRAYER_SMITE, 30.0,                                   new double[] {}),
    CHIVALRY(Varbits.PRAYER_CHIVALRY, 40.0,                             new double[] {0.15D, 0.18D, 0.2D}, PrayerAttribute.PRAY_ATT, PrayerAttribute.PRAY_STR, PrayerAttribute.PRAY_DEF),
    PIETY(Varbits.PRAYER_PIETY, 40.0,                                   new double[] {0.2D, 0.23D, 0.25D}, PrayerAttribute.PRAY_ATT, PrayerAttribute.PRAY_STR, PrayerAttribute.PRAY_DEF),
    PRESERVE(Varbits.PRAYER_PRESERVE, 60.0 / 18,                        new double[] {}),
    RIGOUR(Varbits.PRAYER_RIGOUR, 40.0,                                 new double[] {0.2D, 0.23D, 0.25D}, PrayerAttribute.PRAY_RATT, PrayerAttribute.PRAY_RSTR, PrayerAttribute.PRAY_DEF),
    AUGURY(Varbits.PRAYER_AUGURY, 40.0,                                 new double[] {0.25D, 0.25D, 0.25D}, PrayerAttribute.PRAY_MATT, PrayerAttribute.PRAY_MDEF, PrayerAttribute.PRAY_DEF);

    private final int varbit;
    private final double drainRate;
    private double[] prayerAttributeMods;
    private List<PrayerAttribute> prayerAttributes;

    Prayer(@Varbit int varbit, double drainRate, double[] prayerAttributeMods, PrayerAttribute... prayerAttributes)
    {
        this.varbit = varbit;
        this.drainRate = drainRate;
        this.prayerAttributeMods = prayerAttributeMods;
        this.prayerAttributes = Arrays.asList(prayerAttributes);
    }

    @Varbit
    public int getVarbit() { return varbit; }

    public double getDrainRate() { return drainRate; }

    public double getPrayerAttributeMod(PrayerAttribute prayerAttribute) {
        int indexOfMod = this.prayerAttributes.indexOf(prayerAttribute);
        if (indexOfMod == -1) return 0;
        return this.prayerAttributeMods[indexOfMod];
    }

    public List<PrayerAttribute> getPrayerAttributes() { return this.prayerAttributes; }

}
