package com.lucktracker;

import net.runelite.api.Skill;

import java.util.Arrays;
import java.util.List;

public enum WeaponStance // Stances: grant invisible skill bonuses to att/str/def/range/mage.
{
    // STANCE(stanceName, [skillBonuses], ...Skills)
    ACCURATE("Accurate", new int[]{3}, Skill.ATTACK),
    AGGRESSIVE("Aggressive", new int[]{3}, Skill.STRENGTH),
    DEFENSIVE("Defensive", new int[]{3}, Skill.DEFENCE),
    CONTROLLED("Controlled", new int[]{1, 1, 1}, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    RAPID("Rapid", new int[]{}),
    RANGE_ACCURATE("Range_Accurate", new int[]{3}, Skill.RANGED),
    RANGE_LONGRANGE("Range_Longrange", new int[]{0, 3}, Skill.RANGED, Skill.DEFENCE),
    POWERED_STAFF_LONGRANGE("Powered_Staff_Longrange", new int[]{1, 3}, Skill.MAGIC, Skill.DEFENCE),
    POWERED_STAFF_ACCURATE("Powered_Staff_Accurate", new int[]{3}, Skill.MAGIC),
    CASTING("Casting", new int[]{0}, Skill.MAGIC),
    DEFENSIVE_CASTING("Defensive Casting", new int[]{0, 0}, Skill.MAGIC, Skill.DEFENCE),
    OTHER("Other", new int[]{0});

    private final String name;
    private final int[] invisBonuses;
    private final List<Skill> skills;

    WeaponStance(String name, int[] invisBonuses, Skill... skills) {
        this.name = name;
        this.invisBonuses = invisBonuses;
        this.skills = Arrays.asList(skills);
    }

    public String getName() {
        return this.name;
    }

    public int getInvisBonus(Skill skill) // Get a skill's invisible bonus from a stance
    {
        int indexOfBonus = this.skills.indexOf(skill);
        if (indexOfBonus == -1) return 0;
        return this.invisBonuses[indexOfBonus];
    }

    public List<Skill> getSkills() {
        return this.skills;
    }
}
