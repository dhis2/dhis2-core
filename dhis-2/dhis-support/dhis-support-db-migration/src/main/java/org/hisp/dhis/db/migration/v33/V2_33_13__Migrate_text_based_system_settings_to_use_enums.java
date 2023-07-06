/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.db.migration.v33;

import java.sql.ResultSet;
import java.sql.Statement;
import javax.annotation.Nonnull;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author Volker Schmidt
 */
public class V2_33_13__Migrate_text_based_system_settings_to_use_enums extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(V2_33_13__Migrate_text_based_system_settings_to_use_enums.class);

  @Override
  public void migrate(final Context context) throws Exception {
    update(context, "systemsetting", "systemsettingid", "keyCacheStrategy", CacheStrategy.class);
    update(
        context,
        "systemsetting",
        "systemsettingid",
        "keyAnalysisDisplayProperty",
        DisplayProperty.class);
    update(
        context,
        "systemsetting",
        "systemsettingid",
        "keyAnalysisRelativePeriod",
        RelativePeriodEnum.class);
    update(
        context, "usersetting", "userinfoid", "keyAnalysisDisplayProperty", DisplayProperty.class);
  }

  @SuppressWarnings("unchecked")
  protected void update(
      @Nonnull Context context,
      @Nonnull String tableName,
      @Nonnull String primaryKeyColumnName,
      @Nonnull String settingName,
      @Nonnull Class<? extends Enum> enumClass)
      throws Exception {
    try (final Statement stmt =
        context
            .getConnection()
            .createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
      final String query =
          "SELECT "
              + primaryKeyColumnName
              + ",name,value FROM "
              + tableName
              + " WHERE name='"
              + settingName
              + "'";
      try (final ResultSet rs = stmt.executeQuery(query)) {
        while (rs.next()) {
          final String id = rs.getString(1);
          final String name = rs.getString(2);
          final byte[] value = rs.getBytes(3);
          Object valueObject;
          Enum<?> convertedValue;

          if (value == null) {
            continue;
          }

          try {
            valueObject = SerializationUtils.deserialize(value);
          } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Setting contains invalid value and will be ignored: " + id, e);

            continue;
          }

          try {
            convertedValue =
                Enum.valueOf(
                    enumClass, valueObject.toString().trim().replace(' ', '_').toUpperCase());
          } catch (IllegalArgumentException e) {
            convertedValue = null;

            rs.deleteRow();
            log.error(
                "Could not convert setting '"
                    + valueObject
                    + "' of id '"
                    + id
                    + " (record deleted).");
          }

          if (convertedValue != null) {
            rs.updateBytes(3, SerializationUtils.serialize(convertedValue));
            rs.updateRow();
            log.info(
                "Migrated "
                    + tableName
                    + " value of type "
                    + name
                    + " (id="
                    + id
                    + ") from '"
                    + valueObject
                    + "' to '"
                    + convertedValue
                    + "'");
          }
        }
      }
    }
  }
}
