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
package org.traccar.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

@Singleton
public class TrackJsonWriter {

    private static final String TRACK_QUERY =
            "SELECT id, deviceId, latitude, longitude, speed, course, fixTime,"
            + " valid, address, serverTime, deviceTime, altitude, accuracy, protocol,"
            + " network, geofenceIds"
            + " FROM tc_positions"
            + " WHERE deviceid=? AND fixtime BETWEEN ? AND ?"
            + " ORDER BY fixTime";

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public TrackJsonWriter(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public void writeTrack(long deviceId, Date from, Date to, OutputStream outputStream) throws IOException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(TRACK_QUERY)) {

            ps.setLong(1, deviceId);
            ps.setTimestamp(2, new Timestamp(from.getTime()));
            ps.setTimestamp(3, new Timestamp(to.getTime()));

            try (ResultSet rs = ps.executeQuery();
                 JsonGenerator gen = objectMapper.createGenerator(outputStream)) {

                gen.writeStartArray();
                while (rs.next()) {
                    gen.writeStartObject();

                    gen.writeNumberField("id", rs.getLong("id"));
                    gen.writeNumberField("deviceId", rs.getLong("deviceId"));
                    gen.writeNumberField("latitude", rs.getDouble("latitude"));
                    gen.writeNumberField("longitude", rs.getDouble("longitude"));
                    gen.writeNumberField("speed", rs.getDouble("speed"));
                    gen.writeNumberField("course", rs.getDouble("course"));
                    gen.writeBooleanField("valid", rs.getBoolean("valid"));
                    gen.writeNumberField("accuracy", rs.getDouble("accuracy"));
                    gen.writeNumberField("altitude", rs.getDouble("altitude"));

                    writeTimestamp(gen, "fixTime", rs.getTimestamp("fixTime"));
                    writeTimestamp(gen, "serverTime", rs.getTimestamp("serverTime"));
                    writeTimestamp(gen, "deviceTime", rs.getTimestamp("deviceTime"));

                    writeStringIfNotNull(gen, "address", rs.getString("address"));
                    writeStringIfNotNull(gen, "protocol", rs.getString("protocol"));

                    // Required by frontend — always present but can be empty
                    gen.writeObjectFieldStart("attributes");
                    gen.writeEndObject();

                    writeJsonRaw(gen, "geofenceIds", rs.getString("geofenceIds"), "[]");
                    writeJsonRaw(gen, "network", rs.getString("network"), "null");

                    gen.writeEndObject();
                }
                gen.writeEndArray();
            }
        } catch (Exception e) {
            throw new IOException("Failed to write track JSON", e);
        }
    }

    private static void writeTimestamp(JsonGenerator gen, String name, Timestamp ts) throws IOException {
        if (ts != null) {
            gen.writeStringField(name, ts.toInstant().toString());
        }
    }

    private static void writeStringIfNotNull(JsonGenerator gen, String name, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            gen.writeStringField(name, value);
        }
    }

    private static void writeJsonRaw(JsonGenerator gen, String name, String json, String fallback)
            throws IOException {
        gen.writeFieldName(name);
        if (json != null && !json.isEmpty()) {
            gen.writeRawValue(json);
        } else {
            gen.writeRawValue(fallback);
        }
    }
}
