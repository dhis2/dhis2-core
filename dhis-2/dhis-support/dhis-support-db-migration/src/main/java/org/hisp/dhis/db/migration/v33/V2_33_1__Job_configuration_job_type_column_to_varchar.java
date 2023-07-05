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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.scheduling.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
public class V2_33_1__Job_configuration_job_type_column_to_varchar extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(V2_33_1__Job_configuration_job_type_column_to_varchar.class);

  @Override
  public void migrate(final Context context) throws Exception {
    // 1. Check whether migration is needed at all. Maybe it was already
    // applied. -> Achieves that script can be
    // run multiple times without worries
    boolean continueWithMigration = false;
    String sql =
        "SELECT data_type FROM information_schema.columns "
            + "WHERE table_name = 'jobconfiguration' AND column_name = 'jobtype';";
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql); ) {
      if (rs.next() && rs.getString("data_type").equals("bytea")) {
        continueWithMigration = true;
      }
    }

    if (continueWithMigration) {
      // 2. Create a new JobType column of type VARCHAR in
      // jobconfiguration table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "ALTER TABLE jobconfiguration ADD COLUMN IF NOT EXISTS jobtypevarchar VARCHAR(120)");
      }

      // 3. Move existing jobtype from bytearray column into varchar
      // column
      Map<Integer, byte[]> jobTypeByteMap = new HashMap<>();
      sql = "SELECT jobconfigurationid, jobtype FROM jobconfiguration WHERE jobtype IS NOT NULL";
      try (Statement stmt = context.getConnection().createStatement();
          ResultSet rs = stmt.executeQuery(sql); ) {
        while (rs.next()) {
          jobTypeByteMap.put(rs.getInt("jobconfigurationid"), rs.getBytes("jobtype"));
        }
      }

      jobTypeByteMap.forEach(
          (id, jobTypeByteArray) -> {
            JobType jobType = (JobType) SerializationUtils.deserialize(jobTypeByteArray);
            if (jobType == null) {
              log.error("Flyway java migration error: Parsing JobType byte array failed.");
              throw new FlywayException("Parsing JobType byte array failed.");
            }

            try (PreparedStatement ps =
                context
                    .getConnection()
                    .prepareStatement(
                        "UPDATE jobconfiguration SET jobtypevarchar = ? WHERE jobconfigurationid = ?")) {
              ps.setObject(1, jobType.name());
              ps.setInt(2, id);

              ps.execute();

            } catch (SQLException e) {
              log.error("Flyway java migration error:", e);
              throw new FlywayException(e);
            }
          });

      // 4. Delete old byte array column for JobType in jobconfiguration
      // table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate("ALTER TABLE jobconfiguration DROP COLUMN jobtype");
      }

      // 5. Rename new jobtypevarchar column to the name of the now
      // deleted column
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate("ALTER TABLE jobconfiguration RENAME COLUMN jobtypevarchar TO jobtype");
      }
    }
  }
}
