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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class V2_35_22__add_sms_authority_to_userauthoritygroups extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    final String sql =
        "SELECT DISTINCT(userroleid), authority FROM userroleauthorities ura WHERE authority='M_dhis-web-maintenance-mobile' AND "
            + "NOT EXISTS (SELECT * FROM userroleauthorities WHERE userroleid=ura.userroleid AND authority='M_dhis-web-sms-configuration')";

    try (final Statement stmt = context.getConnection().createStatement();
        final ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        long id = rs.getLong("userroleid");

        final String insertSql =
            "INSERT INTO userroleauthorities (userroleid, authority) VALUES (?, ?)";

        try (final PreparedStatement ps = context.getConnection().prepareStatement(insertSql)) {
          ps.setLong(1, id);
          ps.setString(2, "M_dhis-web-sms-configuration");

          ps.execute();
        }
      }
    }
  }
}
