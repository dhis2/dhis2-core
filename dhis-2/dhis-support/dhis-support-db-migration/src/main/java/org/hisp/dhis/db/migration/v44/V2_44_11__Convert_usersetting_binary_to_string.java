/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.db.migration.v44;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts all legacy binary-serialized user settings from arbitrary types (Boolean, Locale, Enum,
 * etc.) to binary-serialized Strings. After this migration, every usersetting.value row contains a
 * Java-serialized String, eliminating the deserialization attack surface for non-String types.
 *
 * <p>Idempotent: rows already containing a serialized String are left unchanged.
 *
 * @author Morten Svanaes
 */
public class V2_44_11__Convert_usersetting_binary_to_string extends BaseJavaMigration {

  private static final Logger log =
      LoggerFactory.getLogger(V2_44_11__Convert_usersetting_binary_to_string.class);

  private static final ObjectInputFilter FILTER =
      ObjectInputFilter.Config.createFilter(
          "maxdepth=5;maxrefs=20;maxarray=0;maxbytes=65536;"
              + "java.lang.String;java.lang.Boolean;"
              + "java.lang.Number;java.lang.Integer;java.lang.Long;"
              + "java.lang.Double;java.lang.Float;java.lang.Enum;"
              + "java.util.Locale;java.util.Date;"
              + "org.hisp.dhis.common.Locale;org.hisp.dhis.common.DisplayProperty;"
              + "!*");

  @Override
  public void migrate(Context context) throws Exception {
    int converted = 0;
    int skipped = 0;
    int failed = 0;

    try (Statement select = context.getConnection().createStatement();
        ResultSet rs =
            select.executeQuery(
                "SELECT userinfoid, name, value FROM usersetting WHERE value IS NOT NULL");
        PreparedStatement update =
            context
                .getConnection()
                .prepareStatement(
                    "UPDATE usersetting SET value = ? WHERE userinfoid = ? AND name = ?")) {

      while (rs.next()) {
        long userId = rs.getLong("userinfoid");
        String name = rs.getString("name");
        byte[] binary = rs.getBytes("value");
        if (binary == null || binary.length == 0) {
          skipped++;
          continue;
        }

        try {
          Object deserialized = deserialize(binary);
          if (deserialized instanceof String) {
            skipped++;
            continue;
          }

          String stringValue = toStringValue(deserialized);
          byte[] newBinary = serializeString(stringValue);

          update.setBytes(1, newBinary);
          update.setLong(2, userId);
          update.setString(3, name);
          update.addBatch();
          converted++;
        } catch (Exception e) {
          log.warn(
              "Could not convert user setting '{}' for user {}: {}", name, userId, e.getMessage());
          failed++;
        }
      }

      if (converted > 0) {
        update.executeBatch();
      }
    }

    log.info(
        "User settings migration complete: {} converted, {} already String, {} failed",
        converted,
        skipped,
        failed);
  }

  private static Object deserialize(byte[] binary) throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream(binary);
    ObjectInputStream ois = new ObjectInputStream(bis);
    ois.setObjectInputFilter(FILTER);
    return ois.readObject();
  }

  private static String toStringValue(Object value) {
    if (value == null) return "";
    if (value instanceof Date d) return String.valueOf(d.getTime());
    if (value instanceof Enum<?> e) return e.name();
    return value.toString();
  }

  private static byte[] serializeString(String value) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);
    out.writeObject(value);
    out.flush();
    return bos.toByteArray();
  }
}
