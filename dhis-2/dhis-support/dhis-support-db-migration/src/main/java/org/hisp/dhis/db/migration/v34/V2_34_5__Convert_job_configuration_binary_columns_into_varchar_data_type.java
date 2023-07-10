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
package org.hisp.dhis.db.migration.v34;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.scheduling.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
public class V2_34_5__Convert_job_configuration_binary_columns_into_varchar_data_type
    extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(
          V2_34_5__Convert_job_configuration_binary_columns_into_varchar_data_type.class);

  private static final String CHECK_JOB_STATUS_DATA_TYPE_SQL =
      "SELECT data_type FROM information_schema.columns WHERE "
          + "table_name = 'jobconfiguration' AND column_name = 'jobstatus';";

  private static final String CHECK_LAST_EXECUTED_STATUS_DATA_TYPE_SQL =
      "SELECT data_type FROM information_schema.columns WHERE "
          + "table_name = 'jobconfiguration' AND column_name = 'lastexecutedstatus';";

  @Override
  public void migrate(final Context context) throws Exception {
    migrateJobStatusColumn(context);
    migrateLastExecutedStatusColumn(context);
  }

  private void migrateJobStatusColumn(final Context context) throws Exception {
    // 1. Check whether migration is needed at all. Maybe it was already
    // applied. -> Achieves that script can be
    // run multiple times without worries
    boolean continueWithMigration = false;
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(CHECK_JOB_STATUS_DATA_TYPE_SQL); ) {
      if (rs.next() && rs.getString("data_type").equals("bytea")) {
        continueWithMigration = true;
      }
    }

    if (continueWithMigration) {
      // 2. Create a new JobStatus column of type VARCHAR in
      // jobconfiguration table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "ALTER TABLE jobconfiguration ADD COLUMN IF NOT EXISTS jobstatusvarchar VARCHAR(120)");
      }

      // 3. Move existing jobstatus from bytearray column into varchar
      // column
      Map<Integer, byte[]> jobStatusByteMap = new HashMap<>();
      String sql =
          "SELECT jobconfigurationid, jobstatus FROM jobconfiguration WHERE jobstatus IS NOT NULL";
      try (Statement stmt = context.getConnection().createStatement();
          ResultSet rs = stmt.executeQuery(sql); ) {
        while (rs.next()) {
          jobStatusByteMap.put(rs.getInt("jobconfigurationid"), rs.getBytes("jobstatus"));
        }
      }

      jobStatusByteMap.forEach(
          (id, jobStatusByteArray) -> {
            JobStatus jobStatus = (JobStatus) SerializationUtils.deserialize(jobStatusByteArray);
            if (jobStatus == null) {
              log.error("Flyway java migration error: Parsing JobStatus byte array failed.");
              throw new FlywayException("Parsing JobStatus byte array failed.");
            }

            try (PreparedStatement ps =
                context
                    .getConnection()
                    .prepareStatement(
                        "UPDATE jobconfiguration SET jobstatusvarchar = ? WHERE jobconfigurationid = ?")) {
              ps.setObject(1, jobStatus.name());
              ps.setInt(2, id);

              ps.execute();

            } catch (SQLException e) {
              log.error("Flyway java migration error:", e);
              throw new FlywayException(e);
            }
          });

      // 4. Delete old byte array column for JobStatus in jobconfiguration
      // table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate("ALTER TABLE jobconfiguration DROP COLUMN jobstatus");
      }

      // 5. Rename new jobstatusvarchar column to the name of the now
      // deleted column
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "ALTER TABLE jobconfiguration RENAME COLUMN jobstatusvarchar TO jobstatus");
      }
    }
  }

  private void migrateLastExecutedStatusColumn(final Context context) throws Exception {
    // 1. Check whether migration is needed at all. Maybe it was already
    // applied. -> Achieves that script can be
    // run multiple times without worries
    boolean continueWithMigration = false;
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(CHECK_LAST_EXECUTED_STATUS_DATA_TYPE_SQL); ) {
      if (rs.next() && rs.getString("data_type").equals("bytea")) {
        continueWithMigration = true;
      }
    }

    if (continueWithMigration) {
      // 2. Create a new LastExecutedStatus column of type VARCHAR in
      // jobconfiguration table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "ALTER TABLE jobconfiguration ADD COLUMN IF NOT EXISTS lastexecutedstatusvarchar VARCHAR(120)");
      }

      // 3. Move existing lastexecutedstatus from bytearray column into
      // varchar column
      Map<Integer, byte[]> lastExecutedStatusByteMap = new HashMap<>();
      String sql =
          "SELECT jobconfigurationid, lastexecutedstatus FROM jobconfiguration "
              + "WHERE lastexecutedstatus IS NOT NULL";
      try (Statement stmt = context.getConnection().createStatement();
          ResultSet rs = stmt.executeQuery(sql); ) {
        while (rs.next()) {
          lastExecutedStatusByteMap.put(
              rs.getInt("jobconfigurationid"), rs.getBytes("lastexecutedstatus"));
        }
      }

      lastExecutedStatusByteMap.forEach(
          (id, lastExecutedStatusByteArray) -> {
            JobStatus lastExecutedStatus =
                (JobStatus) SerializationUtils.deserialize(lastExecutedStatusByteArray);
            if (lastExecutedStatus == null) {
              log.error(
                  "Flyway java migration error: Parsing LastExecutedStatus byte array failed.");
              throw new FlywayException("Parsing LastExecutedStatus byte array failed.");
            }

            try (PreparedStatement ps =
                context
                    .getConnection()
                    .prepareStatement(
                        "UPDATE jobconfiguration SET lastexecutedstatusvarchar = ? WHERE jobconfigurationid = ?")) {
              ps.setObject(1, lastExecutedStatus.name());
              ps.setInt(2, id);

              ps.execute();

            } catch (SQLException e) {
              log.error("Flyway java migration error:", e);
              throw new FlywayException(e);
            }
          });

      // 4. Delete old byte array column for LastExecutedStatus in
      // jobconfiguration table
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate("ALTER TABLE jobconfiguration DROP COLUMN lastexecutedstatus");
      }

      // 5. Rename new lastexecutedstatusvarchar column to the name of the
      // now deleted column
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "ALTER TABLE jobconfiguration RENAME COLUMN lastexecutedstatusvarchar TO lastexecutedstatus");
      }

      // 6. Set default values where NULL is present
      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(
            "UPDATE jobconfiguration SET lastexecutedstatus = 'NOT_STARTED' WHERE lastexecutedstatus IS NULL");
      }
    }
  }
}
