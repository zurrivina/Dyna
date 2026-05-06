package com.flying_kiwi.dyna;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class CriticalForceCalculationTest {

    private float[] calculateCFWP(List<TimestampedWeight> weights) {
        if (weights == null || weights.size() < 2) {
            return new float[]{0f, 0f};
        }

        long ending = weights.get(weights.size() - 1).getTimestamp();
        int startingIndex = weights.size() - 1;
        float maxval = 0;

        while (startingIndex > 0 && ending - weights.get(startingIndex).getTimestamp() < 60000) {
            maxval = Math.max(maxval, weights.get(startingIndex--).getWeight());
        }

        float sum = 0;
        int count = 0;
        for (int i = startingIndex; i < weights.size(); i++) {
            if (weights.get(i).getWeight() >= maxval / 5) {
                sum += weights.get(i).getWeight();
                count++;
            }
        }

        if (count == 0) return new float[]{0f, 0f};
        float CF = sum / count;

        float WP = 0;
        for (int i = 1; i < weights.size(); i++) {
            if (weights.get(i).getWeight() > CF) {
                WP += weights.get(i).getWeight() * (weights.get(i).getTimestamp() - weights.get(i - 1).getTimestamp()) / 1000;
            }
        }
        return new float[]{CF, WP};
    }

    @Test
    public void calculateCFWP_returnsValidCF_withConsistentWeights() {
        List<TimestampedWeight> weights = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            weights.add(new TimestampedWeight(10.0f, true));
        }

        float[] result = calculateCFWP(weights);
        float cf = result[0];

        assertTrue("CF should be positive", cf > 0);
        assertEquals("CF should be close to input weight", 10.0f, cf, 1.0f);
    }

    @Test
    public void calculateCFWP_handlesVaryingWeights() {
        List<TimestampedWeight> weights = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            float weight = 8.0f + (i % 5) * 0.5f;
            weights.add(new TimestampedWeight(weight, true));
        }

        float[] result = calculateCFWP(weights);
        float cf = result[0];
        float wp = result[1];

        assertTrue("CF should be positive", cf > 0);
        assertTrue("WP should be non-negative", wp >= 0);
    }

    @Test
    public void calculateCFWP_handlesEmptyList() {
        List<TimestampedWeight> weights = new ArrayList<>();
        float[] result = calculateCFWP(weights);

        assertEquals("CF should be 0 for empty list", 0f, result[0], 0.001f);
        assertEquals("WP should be 0 for empty list", 0f, result[1], 0.001f);
    }

    @Test
    public void calculateCFWP_handlesSingleWeight() {
        List<TimestampedWeight> weights = new ArrayList<>();
        weights.add(new TimestampedWeight(15.0f, true));

        float[] result = calculateCFWP(weights);

        assertEquals("CF should be 0 for single weight (no pairs)", 0f, result[0], 0.001f);
    }

    @Test
    public void calculateCFWP_filtersLowWeights() {
        List<TimestampedWeight> weights = new ArrayList<>();

        weights.add(new TimestampedWeight(20.0f, true));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        weights.add(new TimestampedWeight(1.0f, true));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        weights.add(new TimestampedWeight(18.0f, true));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        weights.add(new TimestampedWeight(2.0f, true));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        weights.add(new TimestampedWeight(19.0f, true));

        float[] result = calculateCFWP(weights);
        float cf = result[0];

        assertTrue("CF should reflect higher weights, not low outliers", cf > 10.0f);
    }

    @Test
    public void timestampedWeight_kgUnit() {
        TimestampedWeight tw = new TimestampedWeight(10.5f, true);
        assertTrue("Should be kg", tw.isKg());
        assertEquals("Weight should match", 10.5f, tw.getWeight(), 0.01f);
    }

    @Test
    public void timestampedWeight_lbUnit() {
        TimestampedWeight tw = new TimestampedWeight(22.0f, false);
        assertFalse("Should be lb", tw.isKg());
        assertEquals("Weight should match", 22.0f, tw.getWeight(), 0.01f);
    }
}
