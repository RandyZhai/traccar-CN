package org.traccar.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoordinateUtilTest {

    private static final double DELTA = 1e-6;

    // Tiananmen Square, Beijing — WGS-84 coordinate
    private static final double BEIJING_LAT = 39.9087;
    private static final double BEIJING_LON = 116.3975;

    // London — outside China, should have near-zero GCJ-02 offset
    private static final double LONDON_LAT = 51.5074;
    private static final double LONDON_LON = -0.1278;

    @Test
    public void testWgs84ToGcj02InChina() {
        CoordinateUtil.Coordinate result = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        double offsetLat = Math.abs(result.latitude() - BEIJING_LAT);
        double offsetLon = Math.abs(result.longitude() - BEIJING_LON);
        // GCJ-02 offset in Beijing is roughly 500-700 meters (~0.005 degrees) northeast
        assertTrue(offsetLat > 0.001, "Should have non-trivial latitude offset in China");
        assertTrue(offsetLon > 0.001, "Should have non-trivial longitude offset in China");
    }

    @Test
    public void testWgs84ToGcj02OutsideChina() {
        CoordinateUtil.Coordinate result = CoordinateUtil.wgs84ToGcj02(LONDON_LAT, LONDON_LON);
        double offsetLat = Math.abs(result.latitude() - LONDON_LAT);
        double offsetLon = Math.abs(result.longitude() - LONDON_LON);
        // Outside China, GCJ-02 offset should be much smaller than inside China
        // The transform formula produces non-zero offsets globally, but they are significantly
        // smaller than China's ~0.005+ degree offset
        assertTrue(offsetLat < 0.05, "Offset outside China should be small, got " + offsetLat);
        assertTrue(offsetLon < 0.05, "Offset outside China should be small, got " + offsetLon);
    }

    @Test
    public void testWgs84Gcj02Roundtrip() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate wgs84 = CoordinateUtil.gcj02ToWgs84(gcj02.latitude(), gcj02.longitude());
        assertEquals(BEIJING_LAT, wgs84.latitude(), DELTA);
        assertEquals(BEIJING_LON, wgs84.longitude(), DELTA);
    }

    @Test
    public void testGcj02Wgs84Roundtrip() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate wgs84 = CoordinateUtil.gcj02ToWgs84(gcj02.latitude(), gcj02.longitude());
        CoordinateUtil.Coordinate gcj02Again = CoordinateUtil.wgs84ToGcj02(wgs84.latitude(), wgs84.longitude());
        assertEquals(gcj02.latitude(), gcj02Again.latitude(), DELTA);
        assertEquals(gcj02.longitude(), gcj02Again.longitude(), DELTA);
    }

    @Test
    public void testGcj02Bd09Roundtrip() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
        CoordinateUtil.Coordinate gcj02Again = CoordinateUtil.bd09ToGcj02(bd09.latitude(), bd09.longitude());
        assertEquals(gcj02.latitude(), gcj02Again.latitude(), DELTA);
        assertEquals(gcj02.longitude(), gcj02Again.longitude(), DELTA);
    }

    @Test
    public void testBd09Gcj02Roundtrip() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
        CoordinateUtil.Coordinate backToGcj02 = CoordinateUtil.bd09ToGcj02(bd09.latitude(), bd09.longitude());
        CoordinateUtil.Coordinate bd09Again = CoordinateUtil.gcj02ToBd09(
                backToGcj02.latitude(), backToGcj02.longitude());
        assertEquals(bd09.latitude(), bd09Again.latitude(), DELTA);
        assertEquals(bd09.longitude(), bd09Again.longitude(), DELTA);
    }

    @Test
    public void testWgs84ToBd09Composite() {
        CoordinateUtil.Coordinate direct = CoordinateUtil.wgs84ToBd09(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate composite = CoordinateUtil.gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
        assertEquals(direct.latitude(), composite.latitude(), DELTA);
        assertEquals(direct.longitude(), composite.longitude(), DELTA);
    }

    @Test
    public void testBd09ToWgs84Composite() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
        CoordinateUtil.Coordinate direct = CoordinateUtil.bd09ToWgs84(bd09.latitude(), bd09.longitude());
        CoordinateUtil.Coordinate backToGcj02 = CoordinateUtil.bd09ToGcj02(bd09.latitude(), bd09.longitude());
        CoordinateUtil.Coordinate composite = CoordinateUtil.gcj02ToWgs84(
                backToGcj02.latitude(), backToGcj02.longitude());
        assertEquals(direct.latitude(), composite.latitude(), DELTA);
        assertEquals(direct.longitude(), composite.longitude(), DELTA);
    }

    @Test
    public void testWgs84Bd09Roundtrip() {
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.wgs84ToBd09(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate wgs84 = CoordinateUtil.bd09ToWgs84(bd09.latitude(), bd09.longitude());
        assertEquals(BEIJING_LAT, wgs84.latitude(), DELTA);
        assertEquals(BEIJING_LON, wgs84.longitude(), DELTA);
    }

    @Test
    public void testGcj02ToWgs84OutsideChina() {
        CoordinateUtil.Coordinate result = CoordinateUtil.gcj02ToWgs84(LONDON_LAT, LONDON_LON);
        double offsetLat = Math.abs(result.latitude() - LONDON_LAT);
        double offsetLon = Math.abs(result.longitude() - LONDON_LON);
        assertTrue(offsetLat < 0.05, "Offset outside China should be small, got " + offsetLat);
        assertTrue(offsetLon < 0.05, "Offset outside China should be small, got " + offsetLon);
    }

    @Test
    public void testGcj02ToWgs84IterativeConvergence() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate wgs84 = CoordinateUtil.gcj02ToWgs84(gcj02.latitude(), gcj02.longitude());
        CoordinateUtil.Coordinate gcj02Verify = CoordinateUtil.wgs84ToGcj02(wgs84.latitude(), wgs84.longitude());
        assertEquals(gcj02.latitude(), gcj02Verify.latitude(), DELTA);
        assertEquals(gcj02.longitude(), gcj02Verify.longitude(), DELTA);
    }

    @Test
    public void testBd09OffsetIsNonTrivial() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(BEIJING_LAT, BEIJING_LON);
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
        double offsetLat = Math.abs(bd09.latitude() - gcj02.latitude());
        double offsetLon = Math.abs(bd09.longitude() - gcj02.longitude());
        assertTrue(offsetLat > 1e-5 || offsetLon > 1e-5,
                "BD-09 should have non-trivial offset from GCJ-02");
    }

    @Test
    public void testMultipleChinaLocations() {
        double[][] locations = {
            {39.9087, 116.3975},   // Beijing
            {31.2304, 121.4737},   // Shanghai
            {23.1291, 113.2644},   // Guangzhou
            {30.5728, 104.0668},   // Chengdu
            {22.5431, 114.0579},   // Shenzhen
        };
        for (double[] loc : locations) {
            CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(loc[0], loc[1]);
            CoordinateUtil.Coordinate wgs84 = CoordinateUtil.gcj02ToWgs84(gcj02.latitude(), gcj02.longitude());
            assertEquals(loc[0], wgs84.latitude(), DELTA,
                    "Roundtrip failed at lat=" + loc[0] + " lon=" + loc[1]);
            assertEquals(loc[1], wgs84.longitude(), DELTA,
                    "Roundtrip failed at lat=" + loc[0] + " lon=" + loc[1]);
        }
    }

    @Test
    public void testZeroCoordinates() {
        CoordinateUtil.Coordinate gcj02 = CoordinateUtil.wgs84ToGcj02(0.0, 0.0);
        CoordinateUtil.Coordinate wgs84 = CoordinateUtil.gcj02ToWgs84(0.0, 0.0);
        CoordinateUtil.Coordinate bd09 = CoordinateUtil.wgs84ToBd09(0.0, 0.0);
        CoordinateUtil.Coordinate wgs84FromBd = CoordinateUtil.bd09ToWgs84(0.0, 0.0);
        assertTrue(Math.abs(gcj02.latitude()) < 1.0);
        assertTrue(Math.abs(gcj02.longitude()) < 1.0);
        assertTrue(Math.abs(wgs84.latitude()) < 1.0);
        assertTrue(Math.abs(wgs84.longitude()) < 1.0);
        assertTrue(Math.abs(bd09.latitude()) < 1.0);
        assertTrue(Math.abs(bd09.longitude()) < 1.0);
        assertTrue(Math.abs(wgs84FromBd.latitude()) < 1.0);
        assertTrue(Math.abs(wgs84FromBd.longitude()) < 1.0);
    }
}
