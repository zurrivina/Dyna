package com.flying_kiwi.dyna;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManagerTest {

    @Test
    public void session_serializationPreservesData() {
        Session original = new Session(SessionType.LIVE_DATA);
        original.setName("TestSession");

        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            original.addWeight(new TimestampedWeight(10.0f + i, true));
        }

        assertNotNull("Session name should not be null", original.getName());
        assertEquals("Session name should match", "TestSession", original.getName());
        assertEquals("Should have 10 weights", 10, original.getWeights().size());
    }

    @Test
    public void session_maxWeightCalculation() {
        Session session = new Session(SessionType.PEAK_LOAD);

        session.addWeight(new TimestampedWeight(5.0f, true));
        session.addWeight(new TimestampedWeight(10.0f, true));
        session.addWeight(new TimestampedWeight(7.5f, true));
        session.addWeight(new TimestampedWeight(12.0f, true));

        TimestampedWeight max = session.getSessionMax();
        assertNotNull("Session max should not be null", max);
        assertEquals("Max weight should be 12.0", 12.0f, max.getWeight(), 0.01f);
    }

    @Test
    public void session_averageCalculation() {
        Session session = new Session(SessionType.LIVE_DATA);

        session.addWeight(new TimestampedWeight(10.0f, true));
        session.addWeight(new TimestampedWeight(20.0f, true));
        session.addWeight(new TimestampedWeight(30.0f, true));

        double avg = session.getCurrentAvg();
        assertEquals("Average should be 20.0", 20.0, avg, 0.01);
    }

    @Test
    public void session_emptyWeightsReturnsZeroMax() {
        Session session = new Session(SessionType.LIVE_DATA);

        TimestampedWeight max = session.getSessionMax();
        assertNotNull("Max should return default object", max);
        assertEquals("Max weight should be 0 for empty session", 0f, max.getWeight(), 0.01f);
    }

    @Test
    public void profile_serialization() {
        Profile profile = new Profile("TestUser");

        assertNotNull("Profile name should not be null", profile.getName());
        assertEquals("Profile name should match", "TestUser", profile.getName());
    }

    @Test
    public void timestampedWeight_serialization() {
        long timestamp = System.currentTimeMillis();
        TimestampedWeight tw = new TimestampedWeight(15.5f, true);

        assertEquals("Weight should match", 15.5f, tw.getWeight(), 0.01f);
        assertEquals("Timestamp should match", timestamp, tw.getTimestamp());
        assertTrue("Should be kg", tw.isKg());
    }

    @Test
    public void timestampedWeight_toString() {
        TimestampedWeight tw = new TimestampedWeight(10.0f, true);
        String str = tw.toString();

        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain weight", str.contains("10"));
    }
}
