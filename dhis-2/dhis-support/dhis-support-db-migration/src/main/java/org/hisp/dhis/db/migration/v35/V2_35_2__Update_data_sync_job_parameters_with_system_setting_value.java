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
package org.hisp.dhis.db.migration.v35;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
public class V2_35_2__Update_data_sync_job_parameters_with_system_setting_value
    extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(
          V2_35_2__Update_data_sync_job_parameters_with_system_setting_value.class);

  private static final String DATA_VALUES_SYNC_PAGE_SIZE_KEY = "syncDataValuesPageSize";

  private final ObjectReader reader;

  private final ObjectWriter writer;

  public V2_35_2__Update_data_sync_job_parameters_with_system_setting_value() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder().allowIfBaseType(JobParameters.class).build());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    JavaType resultingJavaType = mapper.getTypeFactory().constructType(JobParameters.class);
    reader = mapper.readerFor(resultingJavaType);
    writer = mapper.writerFor(resultingJavaType);
  }

  @Override
  public void migrate(final Context context) throws Exception {
    int dataValuesPageSize = fetchDataValuesPageSizeData(context);
    updateJobParamaters(context, dataValuesPageSize);
    removeEntryFromSystemSettingTable(context);
  }

  private int fetchDataValuesPageSizeData(final Context context)
      throws SQLException, JsonProcessingException {
    int dataValuesPageSize = fetchDataValuesPageSizeFromSystemSettings(context);

    if (dataValuesPageSize == 0) {
      dataValuesPageSize = fetchDatavaluesPageSizeFromMetadataSyncJobConfiguration(context);
    }

    return dataValuesPageSize;
  }

  private int fetchDataValuesPageSizeFromSystemSettings(final Context context) throws SQLException {
    int dataValuesPageSize = 0;

    String sql =
        "SELECT value FROM systemsetting WHERE name = '" + DATA_VALUES_SYNC_PAGE_SIZE_KEY + "';";
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql); ) {
      if (rs.next()) {
        dataValuesPageSize = (Integer) SerializationUtils.deserialize(rs.getBytes("value"));
      }

      log.info("Value found in SystemSettings: dataValuePageSize: " + dataValuesPageSize);
    }

    return dataValuesPageSize;
  }

  private int fetchDatavaluesPageSizeFromMetadataSyncJobConfiguration(final Context context)
      throws SQLException, JsonProcessingException {
    int dataValuesPageSize = 0;

    String sql =
        "SELECT jobconfigurationid, jsonbjobparameters FROM jobconfiguration "
            + "WHERE jobtype = '"
            + JobType.META_DATA_SYNC.name()
            + "';";
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql); ) {
      if (rs.next()) {
        MetadataSyncJobParameters jobparams = reader.readValue(rs.getString("jsonbjobparameters"));
        dataValuesPageSize = jobparams.getDataValuesPageSize();

        log.info(
            "Value found in Metadata sync job configuration: dataValuePageSize: "
                + dataValuesPageSize);
      }
    }
    return dataValuesPageSize;
  }

  private void updateJobParamaters(final Context context, int dataValuesPageSize)
      throws JsonProcessingException, SQLException {
    if (dataValuesPageSize > 0) {
      Map<Integer, JobParameters> updatedJobParameters = new HashMap<>();

      String sql =
          "SELECT jobconfigurationid, jsonbjobparameters FROM jobconfiguration "
              + "WHERE jobtype = '"
              + JobType.DATA_SYNC.name()
              + "';";
      try (Statement stmt = context.getConnection().createStatement();
          ResultSet rs = stmt.executeQuery(sql); ) {
        while (rs.next()) {
          DataSynchronizationJobParameters jobparams =
              reader.readValue(rs.getString("jsonbjobparameters"));
          jobparams.setPageSize(dataValuesPageSize);

          updatedJobParameters.put(rs.getInt("jobconfigurationid"), jobparams);
        }
      }

      for (Map.Entry<Integer, JobParameters> jobParams : updatedJobParameters.entrySet()) {
        try (PreparedStatement ps =
            context
                .getConnection()
                .prepareStatement(
                    "UPDATE jobconfiguration SET jsonbjobparameters = ? where  jobconfigurationid = ?;")) {
          PGobject pg = new PGobject();
          pg.setType("jsonb");
          pg.setValue(writer.writeValueAsString(jobParams.getValue()));

          ps.setObject(1, pg);
          ps.setInt(2, jobParams.getKey());

          ps.execute();
        }
      }
    }
  }

  private void removeEntryFromSystemSettingTable(final Context context) throws SQLException {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.executeUpdate(
          "DELETE FROM systemsetting WHERE name = '" + DATA_VALUES_SYNC_PAGE_SIZE_KEY + "';");
    }
  }
}
