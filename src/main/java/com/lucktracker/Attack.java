package com.lucktracker;

public class Attack {
    private int attRoll;
    private int maxHit;

    Attack(int attRoll, int maxHit) {
        this.attRoll = attRoll;
        this.maxHit = maxHit;
    }

    public int getAttRoll() { return this.attRoll; }
    public int getMaxHit() { return this.maxHit; }
}

