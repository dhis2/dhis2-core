/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.LikeOperator;
import org.hisp.dhis.query.operators.NullOperator;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class QueryParserTest extends PostgresIntegrationTestBase {

  private QueryParser queryParser;

  @Autowired private SchemaService schemaService;

  @Autowired private OrganisationUnitStore organisationUnitStore;

  @BeforeEach
  void setUp() {
    OrganisationUnit orgUnitA = createOrganisationUnit('A');
    organisationUnitStore.save(orgUnitA);

    User user = createAndAddUser("A");
    user.addOrganisationUnit(orgUnitA);
    userService.updateUser(user);

    injectSecurityContextUser(user);

    queryParser = new DefaultQueryParser(schemaService);
  }

  @Test
  void failedFilters() {
    assertThrows(
        QueryParserException.class,
        () -> queryParser.parse(DataElement.class, Arrays.asList("id", "name")));
  }

  @Test
  void eqOperator() throws QueryParserException {
    Query<DataElement> query =
        queryParser.parse(DataElement.class, Arrays.asList("id:eq:1", "id:eq:2"));
    assertEquals(2, query.getFilters().size());
    Filter filter = (Filter) query.getFilters().get(0);
    assertEquals("id", filter.getPath());
    assertEquals("1", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof EqualOperator);
    filter = (Filter) query.getFilters().get(1);
    assertEquals("id", filter.getPath());
    assertEquals("2", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof EqualOperator);
  }

  @Test
  void ieqOperator() throws QueryParserException {
    Query<DataElement> query =
        queryParser.parse(DataElement.class, Arrays.asList("name:ieq:Test1", "name:ieq:test2"));
    assertEquals(2, query.getFilters().size());
    Filter filter = (Filter) query.getFilters().get(0);
    assertEquals("name", filter.getPath());
    assertEquals("Test1", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof LikeOperator<?>);
    filter = (Filter) query.getFilters().get(1);
    assertEquals("name", filter.getPath());
    assertEquals("test2", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof LikeOperator<?>);
  }

  @Test
  void eqOperatorDeepPath1() throws QueryParserException {
    Query<DataElement> query =
        queryParser.parse(
            DataElement.class,
            Arrays.asList("dataElementGroups.id:eq:1", "dataElementGroups.id:eq:2"));
    assertEquals(2, query.getFilters().size());
    Filter filter = (Filter) query.getFilters().get(0);
    assertEquals("dataElementGroups.id", filter.getPath());
    assertEquals("1", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof EqualOperator);
    filter = (Filter) query.getFilters().get(1);
    assertEquals("dataElementGroups.id", filter.getPath());
    assertEquals("2", filter.getOperator().getArgs().get(0));
    assertTrue(filter.getOperator() instanceof EqualOperator);
  }

  @Test
  void eqOperatorDeepPathFail() {
    assertThrows(
        QueryParserException.class,
        () ->
            queryParser.parse(
                DataElement.class,
                Arrays.asList("dataElementGroups.id.name:eq:1", "dataElementGroups.id.abc:eq:2")));
  }

  @Test
  void nullOperator() throws QueryParserException {
    Query<DataElement> query =
        queryParser.parse(DataElement.class, Arrays.asList("id:null", "name:null"));
    assertEquals(2, query.getFilters().size());
    Filter filter = (Filter) query.getFilters().get(0);
    assertEquals("id", filter.getPath());
    assertTrue(filter.getOperator() instanceof NullOperator);
    filter = (Filter) query.getFilters().get(1);
    assertEquals("name", filter.getPath());
    assertTrue(filter.getOperator() instanceof NullOperator);
  }
}
