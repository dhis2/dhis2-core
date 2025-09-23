/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.db.migration.v43;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.period.PeriodType;

public class V2_43_10__Period_ISO_column extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    String query =
        """
      SELECT p.periodid, p.startdate, pt.name
        FROM period p
        JOIN periodtype pt ON p.periodtypeid = pt.periodtypeid
        WHERE iso IS NULL
        ORDER BY pt.name, p.startdate""";

    Connection conn = context.getConnection();
    try (Statement stmt = conn.createStatement()) {
      ResultSet r = stmt.executeQuery(query);

      try (PreparedStatement ps =
          conn.prepareStatement("UPDATE period SET iso = ? WHERE periodid = ?")) {

        int batchCount = 0;

        while (r.next()) {
          long periodId = r.getLong(1);
          Date start = r.getDate(2);
          String typeName = r.getString(3);

          PeriodType type = PeriodType.getPeriodTypeByName(typeName);

          String iso = type.createPeriod(start).getIsoDate();
          ps.setString(1, iso);
          ps.setLong(2, periodId);
          ps.addBatch();

          if (++batchCount >= 100) {
            ps.executeBatch();
            conn.commit();
            batchCount = 0;
          }
        }

        if (batchCount > 0) {
          ps.executeBatch();
          conn.commit();
        }
      }
    }
  }
}
