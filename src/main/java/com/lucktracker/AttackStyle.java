package com.lucktracker;

import static com.lucktracker.EquipmentStat.*;

public enum AttackStyle // Style of attacks -- slash, stab, crush, magic, or range
{
    STAB("Stab", ASTAB),
    SLASH("Slash", ASLASH),
    CRUSH("Crush", ACRUSH),
    MAGIC("Magic", AMAGIC),
    RANGE("Range", ARANGE),
    UNKNOWN("Unknown", null);

    private final String name;
    private final EquipmentStat equipmentStat;

    AttackStyle(String name, EquipmentStat equipmentStat) {
        this.name = name;
        this.equipmentStat = equipmentStat;
    }

    public String getName() {
        return this.name;
    }

    public EquipmentStat getEquipmentStat() {
        return this.equipmentStat;
    }
}
