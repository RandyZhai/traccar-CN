/*
 * Copyright 2024 容均致 (harryrong@rushanio.com)
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

/**
 * Coordinate system conversion related toolkits, mainstream coordinate systems include:
 * WGS84 coordinate system: colloquially Earth coordinate system, used by Maps outside China.
 * GCJ02 coordinate system: colloquially Mars coordinate system, used by Gaode, Tencent, Ali and so on.
 *
 * Reference：<a href="https://en.wikipedia.org/wiki/Restrictions_on_geographic_data_in_China">...</a>
 * Reference: <a href="https://github.com/dromara/hutool">...</a>
 */
public final class CoordinateUtil {

    private CoordinateUtil() {
    }

    private static final double RADIUS = 6378245.0;
    private static final double CORRECTION_PARAM = 0.00669342162296594323;
    private static final double X_PI = Math.PI * 3000.0 / 180.0;

    public record Coordinate(double latitude, double longitude) {
    }

    public static Coordinate wgs84ToGcj02(double latitude, double longitude) {
        Coordinate offset = offset(latitude, longitude);
        return new Coordinate(latitude + offset.latitude(), longitude + offset.longitude());
    }

    /**
     * Convert GCJ-02 (Mars coordinates) to WGS-84 using iterative approximation.
     * Typically converges within 2-3 iterations to sub-meter accuracy.
     */
    public static Coordinate gcj02ToWgs84(double latitude, double longitude) {
        double wgsLat = latitude;
        double wgsLon = longitude;
        for (int i = 0; i < 6; i++) {
            Coordinate test = wgs84ToGcj02(wgsLat, wgsLon);
            double deltaLat = test.latitude() - latitude;
            double deltaLon = test.longitude() - longitude;
            if (Math.abs(deltaLat) < 1e-7 && Math.abs(deltaLon) < 1e-7) {
                break;
            }
            wgsLat -= deltaLat;
            wgsLon -= deltaLon;
        }
        return new Coordinate(wgsLat, wgsLon);
    }

    /**
     * Convert GCJ-02 to BD-09 (Baidu coordinate system).
     */
    public static Coordinate gcj02ToBd09(double latitude, double longitude) {
        double x = longitude;
        double y = latitude;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);
        return new Coordinate(z * Math.sin(theta) + 0.006, z * Math.cos(theta) + 0.0065);
    }

    /**
     * Convert BD-09 to GCJ-02.
     */
    public static Coordinate bd09ToGcj02(double latitude, double longitude) {
        double x = longitude - 0.0065;
        double y = latitude - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        return new Coordinate(z * Math.sin(theta), z * Math.cos(theta));
    }

    /**
     * Convert WGS-84 to BD-09 (WGS-84 → GCJ-02 → BD-09).
     */
    public static Coordinate wgs84ToBd09(double latitude, double longitude) {
        Coordinate gcj02 = wgs84ToGcj02(latitude, longitude);
        return gcj02ToBd09(gcj02.latitude(), gcj02.longitude());
    }

    /**
     * Convert BD-09 to WGS-84 (BD-09 → GCJ-02 → WGS-84).
     */
    public static Coordinate bd09ToWgs84(double latitude, double longitude) {
        Coordinate gcj02 = bd09ToGcj02(latitude, longitude);
        return gcj02ToWgs84(gcj02.latitude(), gcj02.longitude());
    }

    private static Coordinate offset(double latitude, double longitude) {
        double latitudeOffset = transformLatitude(latitude - 35.0, longitude - 105.0);
        double longitudeOffset = transformLongitude(latitude - 35.0, longitude - 105.0);

        double magic = Math.sin(latitude / 180.0 * Math.PI);
        magic = 1 - CORRECTION_PARAM * magic * magic;
        double sqrtMagic = Math.sqrt(magic);

        latitudeOffset = (latitudeOffset * 180.0)
                / ((RADIUS * (1 - CORRECTION_PARAM)) / (magic * sqrtMagic) * Math.PI);
        longitudeOffset = (longitudeOffset * 180.0)
                / (RADIUS / sqrtMagic * Math.cos(latitude / 180.0 * Math.PI) * Math.PI);

        return new Coordinate(latitudeOffset, longitudeOffset);
    }

    private static double transformLongitude(double latitude, double longitude) {
        double offset = 300.0 + longitude + 2.0 * latitude
                + 0.1 * longitude * longitude + 0.1 * longitude * latitude
                + 0.1 * Math.sqrt(Math.abs(longitude));
        offset += (20.0 * Math.sin(6.0 * longitude * Math.PI)
                + 20.0 * Math.sin(2.0 * longitude * Math.PI)) * 2.0 / 3.0;
        offset += (20.0 * Math.sin(longitude * Math.PI)
                + 40.0 * Math.sin(longitude / 3.0 * Math.PI)) * 2.0 / 3.0;
        offset += (150.0 * Math.sin(longitude / 12.0 * Math.PI)
                + 300.0 * Math.sin(longitude / 30.0 * Math.PI)) * 2.0 / 3.0;
        return offset;
    }

    private static double transformLatitude(double latitude, double longitude) {
        double offset = -100.0 + 2.0 * longitude + 3.0 * latitude
                + 0.2 * latitude * latitude + 0.1 * longitude * latitude
                + 0.2 * Math.sqrt(Math.abs(longitude));
        offset += (20.0 * Math.sin(6.0 * longitude * Math.PI)
                + 20.0 * Math.sin(2.0 * longitude * Math.PI)) * 2.0 / 3.0;
        offset += (20.0 * Math.sin(latitude * Math.PI)
                + 40.0 * Math.sin(latitude / 3.0 * Math.PI)) * 2.0 / 3.0;
        offset += (160.0 * Math.sin(latitude / 12.0 * Math.PI)
                + 320 * Math.sin(latitude * Math.PI / 30.0)) * 2.0 / 3.0;
        return offset;
    }

}
