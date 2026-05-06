package com.flying_kiwi.dyna;

import org.junit.Test;
import static org.junit.Assert.*;

public class DataCollectorTest {

    private int cstu(byte i) {
        return (int) i & 0xFF;
    }

    @Test
    public void cstu_convertsSignedByteToUnsigned() {
        assertEquals(0, cstu((byte) 0x00));
        assertEquals(127, cstu((byte) 0x7F));
        assertEquals(128, cstu((byte) 0x80));
        assertEquals(255, cstu((byte) 0xFF));
    }

    @Test
    public void cstu_handlesNegativeBytes() {
        assertEquals(255, cstu((byte) -1));
        assertEquals(128, cstu((byte) -128));
        assertEquals(129, cstu((byte) -127));
    }

    @Test
    public void weightCalculation_kgUnit() {
        byte[] testData = new byte[17];
        testData[10] = 0x03;
        testData[11] = (byte) 0xE8;
        testData[14] = 1;

        int rawWeight = (cstu(testData[10]) * 256 + cstu(testData[11]));
        float weight = rawWeight / 100f;

        assertEquals(10.0f, weight, 0.01f);
    }

    @Test
    public void weightCalculation_lbUnit() {
        byte[] testData = new byte[17];
        testData[10] = 0x01;
        testData[11] = (byte) 0xF4;
        testData[14] = 0;

        int rawWeight = (cstu(testData[10]) * 256 + cstu(testData[11]));
        float weight = rawWeight / 100f;

        assertEquals(5.0f, weight, 0.01f);
    }

    @Test
    public void weightCalculation_zeroWeight() {
        byte[] testData = new byte[17];
        testData[10] = 0x00;
        testData[11] = 0x00;
        testData[14] = 1;

        int rawWeight = (cstu(testData[10]) * 256 + cstu(testData[11]));
        float weight = rawWeight / 100f;

        assertEquals(0.0f, weight, 0.01f);
    }

    @Test
    public void weightCalculation_maxExpectedWeight() {
        byte[] testData = new byte[17];
        testData[10] = 0x0B;
        testData[11] = (byte) 0xB8;
        testData[14] = 1;

        int rawWeight = (cstu(testData[10]) * 256 + cstu(testData[11]));
        float weight = rawWeight / 100f;

        assertEquals(30.0f, weight, 0.01f);
    }
}
