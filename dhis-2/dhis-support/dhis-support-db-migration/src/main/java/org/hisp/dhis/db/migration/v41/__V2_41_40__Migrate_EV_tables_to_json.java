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
package org.hisp.dhis.db.migration.v41;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.util.SharingUtils;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author maikel arabori
 */
public class __V2_41_40__Migrate_EV_tables_to_json extends BaseJavaMigration {
  private static final Logger log = getLogger(__V2_41_40__Migrate_EV_tables_to_json.class);

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement statement = context.getConnection().createStatement()) {
      // Select all event visualization ids.
      ResultSet eventVisualizations = statement.executeQuery("select eventvisualizationid from eventvisualization");
      while (eventVisualizations.next()) {

        // Moving columns.
        try (PreparedStatement ps = context.getConnection().prepareStatement("select eventvisualizationid, dimension from eventvisualization_columns where eventvisualizationid = ? order by sort_order asc")) {
          ps.setLong(1, eventVisualizations.getLong(1));

          ResultSet rs = ps.executeQuery();
          while (rs.next()) {
            /*
            {
              "program": "uid",
              "programStage": "uid",
[
    {
      "dimensionType": "PERIOD",
      "items": [
        {
          "id": "THIS_YEAR"
        }
      ],
      "dimension": "pe"
    },
    {
      "dimensionType": "ORGANISATION_UNIT",
      "items": [
        {
          "id": "O6uvpzGd5pu"
        }
      ],
      "dimension": "ou"
    },
    {
      "dimensionType": "PROGRAM_DATA_ELEMENT",
      "items": [],
      "valueType": "DATE",
      "dimension": "eMyVanycQSC"
    },
    {
      "dimensionType": "PROGRAM_DATA_ELEMENT",
      "items": [],
      "legendSet": {
        "id": "Yf6UHoPkdS6"
      },
      "valueType": "INTEGER",
      "dimension": "qrur9Dvnyt5"
    },
    {
      "dimensionType": "PROGRAM_INDICATOR",
      "items": [],
      "dimension": "tUdBD1JDxpn"
    },
            }
             */
          }

        }
      }
    }
  }

  private void updateRow(Context context, long sqlviewid, String sharing)
      throws SQLException, JsonProcessingException {
    String updatedSharing = SharingUtils.withAccess(sharing, Sharing::copyMetadataToData);
    try (PreparedStatement statement =
        context
            .getConnection()
            .prepareStatement("update sqlview set sharing = ?::json where sqlviewid = ?")) {
      statement.setLong(2, sqlviewid);
      statement.setString(1, updatedSharing);

      log.info("Executing sharing migration query: [" + statement + "]");
      statement.executeUpdate();
    } catch (SQLException e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
