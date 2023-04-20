/*
 * Copyright (c) 2017, honeyhoney <https://github.com/honeyhoney>
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

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.lucktracker.AttackStyle.*;
import static com.lucktracker.WeaponStance.*;

@Slf4j
public enum WeaponType // Weapon types: Weapon categories (e.g. 2hander, battleaxe, powered staff)
{
    // WEAPONTYPE([WeaponStances], [AttackStyles])
    UNARMED(new WeaponStance[] {ACCURATE, AGGRESSIVE, null, DEFENSIVE},
            new AttackStyle[] {CRUSH, CRUSH, UNKNOWN, CRUSH}),
    AXE(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, CRUSH, SLASH}),
    BLUNT(new WeaponStance[] {ACCURATE, AGGRESSIVE, null, DEFENSIVE},
            new AttackStyle[] {CRUSH, CRUSH, UNKNOWN, CRUSH}),
    BOW(new WeaponStance[] {RANGE_ACCURATE, RAPID, null, RANGE_LONGRANGE},
            new AttackStyle[] {RANGE, RANGE, UNKNOWN, RANGE}),
    CLAWS(new WeaponStance[] {ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, STAB, SLASH}),
    CROSSBOW(new WeaponStance[] {RANGE_ACCURATE, RAPID, null, RANGE_LONGRANGE},
            new AttackStyle[] {RANGE, RANGE, UNKNOWN, RANGE}),								//
    SALAMANDER(new WeaponStance[] {AGGRESSIVE, RANGE_ACCURATE, CASTING, null},
            new AttackStyle[] {SLASH, RANGE, MAGIC, UNKNOWN}),								// todo verify (this looks like salamanders)
    CHINCHOMPA(new WeaponStance[] {RANGE_ACCURATE, RAPID, null, RANGE_LONGRANGE},
            new AttackStyle[] {RANGE, RANGE, UNKNOWN, RANGE}),								//
    GUN(new WeaponStance[] {OTHER, AGGRESSIVE, null, null},
            new AttackStyle[] {UNKNOWN, CRUSH, UNKNOWN, UNKNOWN}),							//
    SWORD_SLASH(new WeaponStance[] {ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, STAB, SLASH}),									//
    SWORD_2H(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, CRUSH, SLASH}),								//
    PICKAXE(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {STAB, STAB, CRUSH, STAB}),									//
    HALBERD(new WeaponStance[] {CONTROLLED, AGGRESSIVE, null, DEFENSIVE},
            new AttackStyle[] {STAB, SLASH, UNKNOWN, STAB}),								//
    POLESTAFF(new WeaponStance[] {ACCURATE, AGGRESSIVE, null, DEFENSIVE},
            new AttackStyle[] {CRUSH, CRUSH, UNKNOWN, CRUSH}),								//
    SCYTHE(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, CRUSH, SLASH}),								//
    SPEAR(new WeaponStance[] {CONTROLLED, CONTROLLED, CONTROLLED, DEFENSIVE},
            new AttackStyle[] {STAB, SLASH, CRUSH, STAB}),									//
    SPIKED(new WeaponStance[] {ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE},
            new AttackStyle[] {CRUSH, CRUSH, STAB, CRUSH}),									//
    SWORD_STAB(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {STAB, STAB, SLASH, STAB}),									//
    STAFF(new WeaponStance[] {ACCURATE, AGGRESSIVE, null, DEFENSIVE, CASTING, DEFENSIVE_CASTING},
            new AttackStyle[] {CRUSH, CRUSH, UNKNOWN, CRUSH, MAGIC, MAGIC}),				//
    THROWN(new WeaponStance[] {RANGE_ACCURATE, RAPID, null, RANGE_LONGRANGE},
            new AttackStyle[] {RANGE, RANGE, UNKNOWN, RANGE}),								//
    WHIP(new WeaponStance[] {ACCURATE, CONTROLLED, null, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, UNKNOWN, SLASH}),								//
    STAFF_BLADED(new WeaponStance[] {ACCURATE, AGGRESSIVE, null, DEFENSIVE, CASTING, DEFENSIVE_CASTING},
            new AttackStyle[] {STAB, SLASH, UNKNOWN, CRUSH, MAGIC, MAGIC}),					//
    GODSWORD(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {SLASH, SLASH, CRUSH, SLASH}),								//
    STAFF_POWERED(new WeaponStance[] {POWERED_STAFF_ACCURATE, POWERED_STAFF_ACCURATE, null, POWERED_STAFF_LONGRANGE},
            new AttackStyle[] {MAGIC, MAGIC, UNKNOWN, MAGIC}),							//
    BANNER(new WeaponStance[] {ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE},
            new AttackStyle[] {STAB, SLASH, CRUSH, STAB}),								//
    POLEARM(new WeaponStance[] {CONTROLLED, AGGRESSIVE, null, DEFENSIVE},
            new AttackStyle[] {STAB, SLASH, UNKNOWN, STAB}),								// Chally
    BLUDGEON(new WeaponStance[] {AGGRESSIVE, AGGRESSIVE, null, AGGRESSIVE},
            new AttackStyle[] {CRUSH, CRUSH, UNKNOWN, CRUSH}),							// ?
    BULWARK(new WeaponStance[] {ACCURATE, null, null, OTHER},
            new AttackStyle[] {CRUSH, UNKNOWN, UNKNOWN, UNKNOWN}),						// ?
    WAND_POWERED(new WeaponStance[] {POWERED_STAFF_ACCURATE, POWERED_STAFF_ACCURATE, POWERED_STAFF_LONGRANGE},
            new AttackStyle[] {UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN}),						// ?
    PARTISAN(new WeaponStance[] {ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE},
            new AttackStyle[] {STAB, STAB, CRUSH, STAB});						// ?

    private static final Map<Integer, WeaponType> weaponTypes;

    private final WeaponStance[] weaponStances;
    private final AttackStyle[] attackStyles;

    WeaponType(WeaponStance[] weaponStances, AttackStyle[] attackStyles)
    {
        this.weaponStances = weaponStances;
        this.attackStyles = attackStyles;
    }

    static // Construct a map of [index, WeaponType] pairs, for convenience with extraction...
    {
        ImmutableMap.Builder<Integer, WeaponType> builder = new ImmutableMap.Builder<>();
        for (WeaponType weaponTypes : values())
        {
            builder.put(weaponTypes.ordinal(), weaponTypes);
        }
        weaponTypes = builder.build();
    }

    public static WeaponStance getWeaponStance(int type, int styleId) // Weapon type comes from the EQUIPPED_WEAPON_TYPE varbit; Attack style from the ATTACK_STYLE varp
    {
        log.info(String.format("type: %d, styleId: %d", type, styleId));
        return weaponTypes.get(type).weaponStances[styleId];
    }

    public static AttackStyle getAttackStyle(int type, int styleId) // Weapon type comes from the EQUIPPED_WEAPON_TYPE varbit; Attack style from the ATTACK_STYLE varp
    {
        log.info(String.format("type: %d, styleId: %d", type, styleId));
        log.info(weaponTypes.get(type).name());
        log.info(weaponTypes.get(type).attackStyles[styleId].getName());
        log.info(weaponTypes.get(type).weaponStances[styleId].getName());
        return weaponTypes.get(type).attackStyles[styleId];
    }

}