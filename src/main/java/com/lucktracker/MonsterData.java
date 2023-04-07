package com.lucktracker;

import java.util.List;


// enum singleton: call MonsterData.getData(npcId) to return a MonsterData object; call object.getRStr() to get rStr

public class MonsterData {
    private final String name;
    private final String[] attributes; //undead, draconic, etc
    private final int size, hitpoints;
    private final int att, str, def, mage, range; //levels
    private final int attbns, arange, amagic; //offensive boosts (corresponds to player's arange, amagic, attbns = (aslash, astab, acrush))
    private final int strbns, rngbns, mbns; //more offensive boosts: strength/max hits
    private final int dstab, dslash, dcrush, dmagic, drange; //defensive bonuses

    public MonsterData(
            String name,
            String[] attributes,
            int size, int hitpoints,
            int att, int str, int def, int mage, int range,
            int attbns, int arange, int amagic,
            int strbns, int rngbns, int mbns,
            int dstab, int dslash, int dcrush, int dmagic, int drange
    )
    {
        this.name = name;
        this.attributes = attributes;
        this.size = size;
        this.hitpoints = hitpoints;
        this.att = att;
        this.str = str;
        this.def = def;
        this.mage = mage;
        this.range = range;
        this.attbns = attbns;
        this.arange = arange;
        this.amagic = amagic;
        this.strbns = strbns;
        this.rngbns = rngbns;
        this.mbns = mbns;
        this.dstab = dstab;
        this.dslash = dslash;
        this.dcrush = dcrush;
        this.dmagic = dmagic;
        this.drange = drange;
    }

    public String getName() { return this.name; }
    public String[] getAttributes() { return this.attributes; }
    public int getSize() { return this.size; }
    public int getHitpoints() { return this.hitpoints; }
    public int getAttLvl() { return this.att; }
    public int getStrLvl() { return this.str; }
    public int getDefLvl() { return this.def; }
    public int getMagicLvl() { return this.mage; }
    public int getRangeLvl() { return this.range; }
    public int getAttBonus() { return this.attbns; }
    public int getARange() { return this.arange; }
    public int getAMagic() { return this.amagic; }
    public int getStrBonus() { return this.strbns; }
    public int getRangeBonus() { return this.rngbns; }
    public int getMagicStrengthBonus() { return this.mbns; }
    public int getDStab() { return this.dstab; }
    public int getDSlash() { return this.dslash; }
    public int getDCrush() { return this.dcrush; }
    public int getDMagic() { return this.dmagic; }
    public int getDRange() { return this.drange; }
}