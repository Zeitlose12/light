package com.skymaxx.tiki.boa;

import com.skymaxx.tiki.boa.BoaPrize;
import java.util.Random;

public final class BoaOdds {
    public static final int MIN_BULLETS = 5;
    public static final int MAX_BULLETS = 8;
    public static final int CHAMBERS = 8;
    private static final double[] BULLET_ODDS;
    private static final double[] GIVEN_FOURTH;
    private static final double[] GIVEN_HEAD;
    private static final double[] HIT;
    private static final double THRASH = 0.7;
    private static final double COMMON_RATE = 0.02;
    private static final double RARE_RATE = 0.00312;
    private static final int MAX_SHOTS = 24;
    private static final double[][][][] REACH;
    private static final double[][][] SPARE;
    private static final double[][] GIVEN;

    private BoaOdds() {
    }

    private static double chance(int stage, boolean thrashing) {
        double base = HIT[Math.max(0, Math.min(HIT.length - 1, stage))];
        return thrashing ? base * 0.7 : base;
    }

    public static double bulletOdds(int bullets) {
        int i = bullets - 5;
        return i < 0 || i > 3 ? 0.0 : BULLET_ODDS[i];
    }

    public static double bulletOdds(int bullets, BoaPrize prize) {
        int i = bullets - 5;
        return i < 0 || i > 3 ? 0.0 : GIVEN[BoaOdds.row(prize)][i];
    }

    public static double hitChance(int stage, boolean thrashing) {
        return BoaOdds.chance(stage, thrashing);
    }

    public static double reach(int target, int hits, int shots, boolean thrashing) {
        if (hits >= target) {
            return 1.0;
        }
        if (shots <= 0) {
            return 0.0;
        }
        return REACH[target][hits][Math.min(24, shots)][thrashing ? 1 : 0];
    }

    public static double spare(int hits, int shots, boolean thrashing) {
        if (hits >= 5) {
            return 0.0;
        }
        if (shots <= 0) {
            return 1.0;
        }
        return SPARE[hits][Math.min(24, shots)][thrashing ? 1 : 0];
    }

    private static int row(BoaPrize prize) {
        if (prize == null || !prize.dropped()) {
            return 0;
        }
        return prize.inHead() ? 2 : 1;
    }

    public static int rollBullets(Random rng, BoaPrize prize) {
        double[] odds = GIVEN[BoaOdds.row(prize)];
        double roll = rng.nextDouble();
        for (int i = 0; i < 3; ++i) {
            if (!((roll -= odds[i]) < 0.0)) continue;
            return 5 + i;
        }
        return 8;
    }

    public static double nextHit(int target, int hits, int shots, boolean thrashing) {
        if (shots <= 0) {
            return 0.0;
        }
        double q = BoaOdds.chance(hits, thrashing);
        if (target <= 0) {
            if (hits >= 4) {
                return 0.0;
            }
            double now = BoaOdds.spare(hits, shots, thrashing);
            return now <= 0.0 ? 0.0 : q * BoaOdds.spare(hits + 1, shots - 1, true) / now;
        }
        if (hits >= target) {
            return 0.0;
        }
        double now = BoaOdds.reach(target, hits, shots, thrashing);
        if (now <= 0.0) {
            return 1.0;
        }
        return q * BoaOdds.reach(target, hits + 1, shots - 1, true) / now;
    }

    static {
        int i;
        BULLET_ODDS = new double[]{0.4, 0.3, 0.2, 0.1};
        GIVEN_FOURTH = new double[]{0.12, 0.22, 0.32, 0.34};
        GIVEN_HEAD = new double[]{0.06, 0.16, 0.34, 0.44};
        HIT = new double[]{0.82, 0.72, 0.46, 0.32, 0.3};
        REACH = new double[6][][][];
        SPARE = new double[6][25][2];
        GIVEN = new double[3][];
        for (int target = 1; target <= 5; ++target) {
            double[][][] table = new double[target + 1][25][2];
            for (int n = 0; n <= 24; ++n) {
                table[target][n][0] = 1.0;
                table[target][n][1] = 1.0;
                for (int h = target - 1; h >= 0; --h) {
                    for (int last = 0; last < 2; ++last) {
                        double q = BoaOdds.chance(h, last == 1);
                        table[h][n][last] = n == 0 ? 0.0 : q * table[h + 1][n - 1][1] + (1.0 - q) * table[h][n - 1][0];
                    }
                }
            }
            BoaOdds.REACH[target] = table;
        }
        for (int n = 0; n <= 24; ++n) {
            for (int h = 4; h >= 0; --h) {
                for (int last = 0; last < 2; ++last) {
                    if (n == 0) {
                        BoaOdds.SPARE[h][n][last] = 1.0;
                        continue;
                    }
                    double q = BoaOdds.chance(h, last == 1);
                    double onHit = h + 1 >= 5 ? 0.0 : SPARE[h + 1][n - 1][1];
                    BoaOdds.SPARE[h][n][last] = q * onHit + (1.0 - q) * SPARE[h][n - 1][0];
                }
            }
        }
        BoaOdds.GIVEN[1] = (double[])GIVEN_FOURTH.clone();
        BoaOdds.GIVEN[2] = (double[])GIVEN_HEAD.clone();
        double[] none = new double[4];
        double total = 0.0;
        for (i = 0; i < 4; ++i) {
            double left = BULLET_ODDS[i] - 0.02 * GIVEN[1][i] - 0.00312 * GIVEN[2][i];
            none[i] = Math.max(1.0E-6, left / 0.97688);
            total += none[i];
        }
        i = 0;
        while (i < 4) {
            int n = i++;
            none[n] = none[n] / total;
        }
        BoaOdds.GIVEN[0] = none;
    }
}

