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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.Creates new jsonb column for jobparameters in jobconfiguration 2. Fetches jobparameters from
 * existing bytearray column and moves them into new jsonb column 3. Deletes old jobparameter column
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class V2_31_2__Job_configuration_param_to_jsonb extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(V2_31_2__Job_configuration_param_to_jsonb.class);

  private ObjectWriter writer;

  public void migrate(Context context) throws Exception {
    ObjectMapper MAPPER = new ObjectMapper();
    MAPPER.enableDefaultTyping();
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    writer = MAPPER.writerFor(JobParameters.class);

    // 1. Create new jsonb column for jobparameters in jobconfiguration
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.executeUpdate(
          "ALTER TABLE jobconfiguration ADD COLUMN IF NOT EXISTS jsonbjobparameters jsonb");
    }

    // 2. Move existing jobparameters from bytearray column into jsonb
    // column
    Map<Integer, byte[]> jobParamByteMap = new HashMap<>();
    Map<Integer, byte[]> jobTypeByteMap = new HashMap<>();
    try (Statement stmt = context.getConnection().createStatement()) {
      try (ResultSet rows =
          stmt.executeQuery(
              "select jobconfigurationid,jobparameters,jobtype from jobconfiguration where jobparameters is not null")) {
        while (rows.next()) {
          jobParamByteMap.put(rows.getInt(1), rows.getBytes(2));
          jobTypeByteMap.put(rows.getInt(1), rows.getBytes(3));
        }
      }
    }

    jobParamByteMap.forEach(
        (id, jobParamByteArray) -> {
          Object jParaB = null;
          JobType jobType = null;
          try {
            jParaB = toObject(jobParamByteArray);
            jobType = (JobType) toObject(jobTypeByteMap.get(id));
          } catch (IOException | ClassNotFoundException e) {
            log.error("Flyway java migration error:", e);
            throw new FlywayException(e);
          }

          try (PreparedStatement ps =
              context
                  .getConnection()
                  .prepareStatement(
                      "Update jobconfiguration set jsonbjobparameters =? where  jobconfigurationid = ?")) {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(convertObjectToJson(jobType.getJobParameters().cast(jParaB)));
            ps.setObject(1, pg);
            ps.setInt(2, id);

            ps.execute();
          } catch (SQLException e) {
            log.error("Flyway java migration error:", e);
            throw new FlywayException(e);
          }
        });

    // 3. Delete old byte array column for jobparameters in jobconfiguration
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.executeUpdate("ALTER TABLE jobconfiguration DROP COLUMN IF EXISTS jobparameters");
    }
  }

  private Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
    Object obj;
    ByteArrayInputStream bis = null;
    ObjectInputStream ois = null;
    try {
      bis = new ByteArrayInputStream(bytes);
      ois = new ObjectInputStream(bis);
      obj = ois.readObject();
    } finally {
      if (bis != null) {
        bis.close();
      }
      if (ois != null) {
        ois.close();
      }
    }
    return obj;
  }

  /**
   * Serializes an object to JSON.
   *
   * @param object the object to convert.
   * @return JSON content.
   */
  private String convertObjectToJson(Object object) {
    try {
      return writer.writeValueAsString(object);
    } catch (IOException e) {
      log.error("Flyway java migration error:", e);
      throw new FlywayException(e);
    }
  }
}
