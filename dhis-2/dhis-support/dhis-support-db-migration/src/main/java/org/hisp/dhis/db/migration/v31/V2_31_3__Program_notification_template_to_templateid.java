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
package org.hisp.dhis.db.migration.v31;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Zubair Asghar.
 */
public class V2_31_3__Program_notification_template_to_templateid extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(V2_31_3__Program_notification_template_to_templateid.class);

  @Override
  public void migrate(Context context) throws Exception {
    List<Integer> ids = new ArrayList<>();
    Map<Integer, String> templateIdUidMap = new HashMap<>();

    try (Statement stmt = context.getConnection().createStatement()) {
      ResultSet templateIds =
          stmt.executeQuery(
              "SELECT programnotificationtemplateid FROM programruleaction WHERE actiontype IN ('SENDMESSAGE', 'SCHEDULEMESSAGE')");

      while (templateIds.next()) {
        ids.add(templateIds.getInt(1));
      }
    } catch (SQLException ex) {
      log.error("Flyway java migration error", ex);
      throw new FlywayException(ex);
    }

    if (ids.size() > 0) {
      String sql_IN = StringUtils.join(ids, ",");

      try (PreparedStatement ps =
          context
              .getConnection()
              .prepareStatement(
                  "SELECT programnotificationtemplateid, uid FROM programnotificationtemplate WHERE programnotificationtemplateid IN ("
                      + sql_IN
                      + ")")) {
        ResultSet templates = null;

        templates = ps.executeQuery();

        while (templates.next()) {
          templateIdUidMap.put(templates.getInt(1), templates.getString(2));
        }
      } catch (SQLException e) {
        log.error("Flyway java migration error:", e);
        throw new FlywayException(e);
      }
    }

    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.executeUpdate("ALTER TABLE programruleaction ADD COLUMN IF NOT EXISTS templateuid text");
    }

    try (PreparedStatement preparedStmt =
        context
            .getConnection()
            .prepareStatement(
                "UPDATE programruleaction SET templateuid=? WHERE programnotificationtemplateid=?")) {
      for (Map.Entry<Integer, String> entry : templateIdUidMap.entrySet()) {
        preparedStmt.setObject(1, entry.getValue());
        preparedStmt.setObject(2, entry.getKey());
        preparedStmt.execute();
      }
    } catch (SQLException e) {
      log.error("Flyway java migration error:", e);
      throw new FlywayException(e);
    }

    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.executeUpdate(
          "ALTER TABLE programruleaction DROP COLUMN IF EXISTS programnotificationtemplateid");
    }
  }
}
