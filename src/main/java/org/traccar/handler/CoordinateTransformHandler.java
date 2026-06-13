/*
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
package org.traccar.handler;

import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.CoordinateUtil;
import org.traccar.model.Position;

public class CoordinateTransformHandler extends BasePositionHandler {

    private final String transformType;

    @Inject
    public CoordinateTransformHandler(Config config) {
        transformType = config.getString(Keys.COORDINATE_TRANSFORM_TYPE);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (transformType != null
                && (position.getLatitude() != 0.0
                || position.getLongitude() != 0.0)) {
            CoordinateUtil.Coordinate coordinate = switch (transformType) {
                case "wgs84ToGcj02" -> CoordinateUtil.wgs84ToGcj02(
                        position.getLatitude(), position.getLongitude());
                case "gcj02ToWgs84" -> CoordinateUtil.gcj02ToWgs84(
                        position.getLatitude(), position.getLongitude());
                case "wgs84ToBd09" -> CoordinateUtil.wgs84ToBd09(
                        position.getLatitude(), position.getLongitude());
                case "bd09ToWgs84" -> CoordinateUtil.bd09ToWgs84(
                        position.getLatitude(), position.getLongitude());
                default -> null;
            };
            if (coordinate != null) {
                position.setLatitude(coordinate.latitude());
                position.setLongitude(coordinate.longitude());
            }
        }
        callback.processed(false);
    }
}
