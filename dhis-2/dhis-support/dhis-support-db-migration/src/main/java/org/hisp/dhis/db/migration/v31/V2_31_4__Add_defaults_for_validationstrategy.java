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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
public class V2_31_4__Add_defaults_for_validationstrategy extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {

    List<Integer> programStageIds = new ArrayList<>();

    String sql =
        "SELECT programstageid FROM programstage ps JOIN program p ON p.programid = ps.programid "
            + "WHERE p.type = 'WITHOUT_REGISTRATION'";
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql); ) {
      while (rs.next()) {
        programStageIds.add(rs.getInt("programstageid"));
      }
    }

    if (programStageIds.size() > 0) {
      String inStatement = StringUtils.join(programStageIds, ",");
      sql =
          "UPDATE programstage SET validationstrategy = 'ON_UPDATE_AND_INSERT' "
              + "WHERE programstageid IN ("
              + inStatement
              + ")";

      try (Statement stmt = context.getConnection().createStatement()) {
        stmt.executeUpdate(sql);
      }
    }
  }
}
