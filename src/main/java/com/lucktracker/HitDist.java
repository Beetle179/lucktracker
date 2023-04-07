package com.lucktracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HitDist {
    private List<Double> pmf;

    public HitDist(double hitProb, int maxHit) {
        System.out.println("Max hit is " + maxHit);
        this.pmf = new ArrayList<Double>();
        for (int dmg = 0; dmg < maxHit + 1; dmg++) { this.pmf.add(dmg, hitProb / (maxHit + 1.0D)); }
        this.pmf.set(0, this.pmf.get(0) + (1.0D - hitProb));
    }

    public double getAvgDmg() {
        double avgDmg = 0.0D;
        for (int dmg = 0; dmg < this.pmf.size(); dmg++) {
            avgDmg += ((double) dmg) * (this.pmf.get(dmg));
//            System.out.print(String.format("DMG %d with probability %f, running avgDmg = %f", dmg, this.pmf.get(dmg), avgDmg));
        }
        return avgDmg;
    }

    // TODO reformat for ArrayList and validate
    public double[] conv1D(double[] a,double[] b) {
        // https://rarelyknows.wordpress.com/2021/09/12/1d-array-convolution-function-in-java/
        if (a.length < b.length) {
            double[] temp = a;
            a = b;
            b = temp;
        }

        double[] retArr = new double[a.length + b.length - 1];
        for (int i = 0; i < retArr.length; i++) {
            double temp = 0;
            int rem = (int) Math.floor(i / b.length);
            int jt = 0;
            int jtfin = 0;
            int r = 0;
            if (i >= a.length) {
                jtfin = a.length - 1;
            } else {
                jtfin = i;
            }
            if (i >= b.length) {
                jt = (i % b.length) + rem + ((b.length - 1) * (rem - 1));
            }
            if (i >= a.length) {
                r = i - a.length + 1;
            }

            for (int j = jt; j <= jtfin; j++) {
                temp = temp + a[j] * b[jtfin - j + r];
            }
            retArr[i] = temp;
        }
        return retArr;
    }
}
