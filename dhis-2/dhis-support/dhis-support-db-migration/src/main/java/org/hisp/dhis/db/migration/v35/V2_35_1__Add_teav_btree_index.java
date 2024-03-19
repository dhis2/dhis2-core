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

import static org.hisp.dhis.trackedentity.TrackedEntityAttributeService.TEA_VALUE_MAX_LENGTH;

import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Java migration changes the data type of the 'value' column in trackedentityattributevalue
 * table to varchar(1200). Additionally we put a btree index on the same table, to improve
 * performance of lookup. This upograde will fail is any value in the table exceeds 1200 characters.
 * Due to this, we catch any SQLException and write a custom message, including a link to a
 * community.dhis2.org post explaining this upgrade, and how to deal with any upgrade failures.
 *
 * <p>By setting canExecuteInTransaction to false, we let this upgrade run outside the main flow of
 * flyway, allowing us to ignore that the upgrade potentially fails.
 *
 * @author Stian
 */
public class V2_35_1__Add_teav_btree_index extends BaseJavaMigration {
  private static final String COP_POST_URL =
      "https://community.dhis2.org/t/important-database-upgrade-for-tracker-performance/38766";

  private static final Logger log = LoggerFactory.getLogger(V2_35_1__Add_teav_btree_index.class);

  @Override
  public void migrate(Context context) {

    try (Statement statement = context.getConnection().createStatement()) {
      statement.execute(
          "alter table trackedentityattributevalue alter column value set data type varchar(1200)");
      statement.execute(
          "create index in_trackedentity_attribute_value on trackedentityattributevalue using btree (trackedentityattributeid, lower(value)) ");
    } catch (SQLException e) {
      String message =
          "Could not perform upgrade of table 'trackedentityattributevalue'. "
              + String.format(
                  "Column 'value' should be altered to data type varchar(%s) and receive a new index. ",
                  TEA_VALUE_MAX_LENGTH)
              + String.format(
                  "For more information, please see the following post: '%s'. ", COP_POST_URL)
              + String.format("Error message was: %s", e.getMessage());

      log.warn(message);
    }
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }
}
