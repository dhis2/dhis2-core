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
package org.hisp.dhis.metadata.users;

import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FIELDS;
import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FILTER;

import java.util.Arrays;
import java.util.Collections;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataPaginationActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class UserPaginationTest extends ApiTest {
  private MetadataPaginationActions paginationActions;

  private UserActions userActions;

  private int startPage = 2;

  private int pageSize = 5;

  private int total = 50;

  @BeforeEach
  public void setUp() {
    LoginActions loginActions = new LoginActions();
    userActions = new UserActions();

    paginationActions = new MetadataPaginationActions("/users");
    loginActions.loginAsSuperUser();

    // Creates Users
    for (int i = 0; i < total; i++) {
      userActions.addUserFull(
          DataGenerator.randomString() + i,
          DataGenerator.randomString() + i,
          (DataGenerator.randomString() + i).toLowerCase(),
          DataGenerator.randomString() + "Abcd1234!" + i);
    }
  }

  @Test
  public void checkPaginationResultsForcingInMemoryPagination() {
    // this test forces the metadata query engine to execute an "in memory"
    // sorting and pagination
    // since the sort ("order") value is set to 'displayName' that is a
    // "virtual" field (that is, not a database column)
    // The metadata query engine can not execute a sql query using this
    // field, since it does not exist
    // on the table. Therefore, the engine loads the entire content of the
    // table in memory and
    // executes a sort + pagination "in memory"

    ApiResponse response = paginationActions.getPaginated(startPage, pageSize);

    response.validate().statusCode(200);

    paginationActions.assertPagination(response, total, total / pageSize, pageSize, startPage);
  }

  @Test
  public void checkPaginationResultsWithBothDatabaseAndInMemory() {
    ApiResponse response =
        paginationActions.getPaginatedWithFiltersOnly(
            Arrays.asList(DEFAULT_METADATA_FILTER.split(",")), startPage, pageSize);

    response.validate().statusCode(200);

    paginationActions.assertPagination(
        response, total, total / pageSize, pageSize, startPage, "users");
  }

  @Test
  public void checkPaginationResultsForcingDatabaseOnlyPagination() {
    // this test forces the metadata query engine to execute the query
    // (including pagination) on the database only.
    // The sort ("order") value is set to 'id' that is mapped to a DB
    // column.

    ApiResponse response =
        paginationActions.getPaginated(
            Arrays.asList(DEFAULT_METADATA_FILTER.split(",")),
            Arrays.asList(DEFAULT_METADATA_FIELDS.split(",")),
            Collections.singletonList("id:ASC"),
            startPage,
            pageSize);

    response.validate().statusCode(200);

    paginationActions.assertPagination(response, total, total / pageSize, pageSize, startPage);
  }
}
