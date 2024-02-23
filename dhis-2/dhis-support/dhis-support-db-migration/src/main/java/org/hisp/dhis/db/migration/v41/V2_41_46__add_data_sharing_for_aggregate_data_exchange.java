/*
 * Copyright (c) 2004-2024, University of Oslo
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
import org.slf4j.LoggerFactory;

/**
 * In table aggregatedataexchange, copy Metadata Write value to Data Write value in sharing.users
 * and sharing.userGroups in sharing column. Public access must not be changed.
 *
 * @author Viet Nguyen
 */
public class V2_41_46__add_data_sharing_for_aggregate_data_exchange extends BaseJavaMigration {

  private static final Logger log =
      LoggerFactory.getLogger(V2_41_46__add_data_sharing_for_aggregate_data_exchange.class);

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement statement = context.getConnection().createStatement()) {
      ResultSet results =
          statement.executeQuery(
              "select aggregatedataexchangeid, sharing from aggregatedataexchange;");
      while (results.next()) {
        updateRow(context, results.getLong(1), results.getString(2));
      }
    }
  }

  private void updateRow(Context context, long id, String sharing)
      throws SQLException, JsonProcessingException {
    String updatedSharing =
        SharingUtils.withUserAndUserGroupAccess(sharing, Sharing::copyDataWrite);
    try (PreparedStatement statement =
        context
            .getConnection()
            .prepareStatement(
                "update aggregatedataexchange set sharing = ?::json where aggregatedataexchangeid = ?")) {
      statement.setLong(2, id);
      statement.setString(1, updatedSharing);

      log.info("Executing sharing migration query: {}", statement);
      statement.executeUpdate();
    }
  }
}
