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
package org.hisp.dhis.db.migration.base;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.db.migration.helper.JdbcSqlFileExecutor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Java based migration class that populates base dhis2 schema if the db is empty.
 *
 * @author Ameen Mohamed
 */
public class V2_30_0__Populate_dhis2_schema_if_empty_database extends BaseJavaMigration {

  private static final String CHECK_EMPTY_DB_QUERY =
      "SELECT EXISTS( SELECT * FROM information_schema.tables  WHERE table_name = 'organisationunit');";

  private static final String BASE_SCHEMA_SQL_LOCATION =
      "/org/hisp/dhis/db/base/dhis2_base_schema.sql";

  public void migrate(Context context) throws Exception {
    try (Statement select = context.getConnection().createStatement()) {
      try (ResultSet rows = select.executeQuery(CHECK_EMPTY_DB_QUERY)) {
        if (rows.next()) {
          boolean nonEmptyDatabase = rows.getBoolean(1);
          if (!nonEmptyDatabase) {
            Connection mConnection = context.getConnection();
            JdbcSqlFileExecutor runner = new JdbcSqlFileExecutor(mConnection, false, true);
            Resource resource = new ClassPathResource(BASE_SCHEMA_SQL_LOCATION);
            InputStream resourceInputStream = resource.getInputStream();
            runner.runScript(new BufferedReader(new InputStreamReader(resourceInputStream)));
          }
        }
      }
    }
  }
}
