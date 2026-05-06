package com.flying_kiwi.dyna.Settings;

import java.io.Serializable;

public class PresetSettings implements Serializable {
    private String name;
    private int numSets;
    private int numReps;
    private int workTime;
    private int restTime;
    private int pauseTime;
    private int countdown;
    private float targetWeight;
    private float targetMarginMin;
    private float targetMarginMax;
    private boolean sound;

    PresetSettings(int sets, int reps, int work, int rest, int pause, int cd, float target, float marginMin, float marginMax, boolean sound) {
        this.numSets = sets;
        this.numReps = reps;
        this.workTime = work;
        this.restTime = rest;
        this.pauseTime = pause;
        this.countdown = cd;
        this.targetWeight = target;
        this.targetMarginMin = marginMin;
        this.targetMarginMax = marginMax;
        this.sound = sound;
    }

    public int getNumSets() { return numSets; }
    public int getNumReps() { return numReps; }
    public int getWorkTime() { return workTime; }
    public int getRestTime() { return restTime; }
    public int getPauseTime() { return pauseTime; }
    public int getCountdown() { return countdown; }
    public float getTargetWeight() { return targetWeight; }
    public float getTargetMarginMin() { return targetMarginMin; }
    public float getTargetMarginMax() { return targetMarginMax; }
    public boolean isSound() { return sound; }

    public int getPlotMin() { return (int)(targetWeight - targetMarginMin); }
    public int getPlotMax() { return (int)(targetWeight + targetMarginMax); }
    public boolean isPlotTarget() { return targetWeight > 0; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
}
