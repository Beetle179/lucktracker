package com.lucktracker;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class HitDist {
    private ArrayList<Double> pmf;
    private ArrayList<Double> cdf;

    public HitDist() {
        this.pmf = new ArrayList<>(1);
        this.pmf.add(1.0D);
        this.cdf = constructCdf(this.pmf);
    }

    public HitDist(ArrayList<Double> pmf) { // If something need special logic, e.g. Osmumten's Fang
        this.pmf = pmf;
        this.cdf = constructCdf(this.pmf);
    }

    public HitDist(double hitProb, int maxHit) { // No clamps
        this.pmf = new ArrayList<>();
        for (int dmg = 0; dmg < maxHit + 1; dmg++) { this.pmf.add(hitProb / (maxHit + 1.0D)); }
        this.pmf.set(0, this.pmf.get(0) + (1.0D - hitProb));
        this.cdf = constructCdf(this.pmf);
    }

    public HitDist(double hitProb, int maxHit, int maxClamp) { // Using an upper clamp -- e.g. if max hit is 17, but NPC has 8 hp, then roll all 9+ hits into the probability of hitting 8
        this.pmf = new ArrayList<>(maxHit);
        double incrementalProb = hitProb / (maxHit + 1.0D);
        for (int dmg = 0; dmg < maxHit + 1; dmg++) {
            if (dmg < maxClamp + 1) this.pmf.add(incrementalProb);
            else this.pmf.set(maxClamp, this.pmf.get(maxClamp) + incrementalProb);
        }
        this.pmf.set(0, this.pmf.get(0) + (1.0D - hitProb));
        this.cdf = constructCdf(this.pmf);
    }

    public double getAvgDmg() {
        double avgDmg = 0.0D;
        for (int dmg = 0; dmg < this.pmf.size(); dmg++) {
            avgDmg += ((double) dmg) * (this.pmf.get(dmg));
        }
        return avgDmg;
    }

    public int getMax() { // Only applicable for single-hit distributions
        return this.pmf.size() - 1;
    }

    public double getNonZeroHitChance() { // Only applicable for single-hit distributions
        if (this.pmf.size() == 0) return 0.0D;
        return 1 - this.pmf.get(0);
    }

    private static ArrayList<Double> constructCdf(ArrayList<Double> _pmf) {
        ArrayList<Double> _cdf = new ArrayList<>(_pmf.size());
        Double sum = 0.0D;
        for (Double incProb : _pmf) {
            sum += incProb;
            _cdf.add(sum);
        }
        return _cdf;
    }

    public double getCdfAtDmg(int val) {
        if (val > this.cdf.size()) {
            log.info("*** Encountered total damage which exceeded expected maximum total damage ***");
            return this.cdf.get(this.cdf.size() - 1);
        }
        return this.cdf.get(val);
    }

    public int getDmgAtCdf(double cdf) {
        if (cdf >= 1.0) { return this.cdf.size() - 1; }
        for (int i = 0; i < this.cdf.size(); i++) {
            if (this.cdf.get(i) > cdf) { return i; }
        }
        return -1;
    }

    public void convolve(HitDist hitDist) {
        // https://rarelyknows.wordpress.com/2021/09/12/1d-array-convolution-function-in-java/
        ArrayList<Double> b = hitDist.pmf;
        if (this.pmf.size() < b.size()) {
            ArrayList<Double> temp = this.pmf;
            this.pmf = b;
            b = temp;
        }

        ArrayList<Double> retArr = new ArrayList<>(b.size() - 1);
        for (int i = 0; i < this.pmf.size() + b.size() - 1; i++) {
            double temp = 0;
            int rem = i / b.size();
            int jt = 0;
            int jtfin = 0;
            int r = 0;
            if (i >= this.pmf.size()) {
                jtfin = this.pmf.size() - 1;
            } else {
                jtfin = i;
            }
            if (i >= b.size()) {
                jt = (i % b.size()) + rem + ((b.size() - 1) * (rem - 1));
            }
            if (i >= this.pmf.size()) {
                r = i - this.pmf.size() + 1;
            }

            for (int j = jt; j <= jtfin; j++) {
                temp = temp + this.pmf.get(j) * b.get(jtfin - j + r);
            }
            retArr.add(temp);
        }
        this.pmf = retArr;
        this.cdf = constructCdf(this.pmf);
    }
}
