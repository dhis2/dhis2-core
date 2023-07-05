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
package org.hisp.dhis.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.planner.DefaultQueryPlanner;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class DefaultQueryServiceTest {

  private DefaultQueryService subject;

  @Mock private QueryParser queryParser;

  @Mock private JpaCriteriaQueryEngine<OrganisationUnit> criteriaQueryEngine;

  @Mock private InMemoryQueryEngine<OrganisationUnit> inMemoryQueryEngine;

  @Mock private SchemaService schemaService;

  @BeforeEach
  public void setUp() {
    QueryPlanner queryPlanner = new DefaultQueryPlanner(schemaService);
    subject =
        new DefaultQueryService(
            queryParser, queryPlanner, criteriaQueryEngine, inMemoryQueryEngine);
  }

  @Test
  void verifyQueryEngineUsesPaginationInformation() {
    Query query = Query.from(new OrganisationUnitSchemaDescriptor().getSchema());
    query.setFirstResult(100);
    query.setMaxResults(50);

    // Here we make sure that the pagination info are actually passed to the
    // Hibernate query engine
    when(criteriaQueryEngine.query(argThat(new QueryWithPagination(query))))
        .thenReturn(createOrgUnits(20));

    List<? extends IdentifiableObject> orgUnits = subject.query(query);

    assertThat(orgUnits.size(), is(20));
  }

  private List<OrganisationUnit> createOrgUnits(int size) {

    List<OrganisationUnit> result = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      result.add(createOrganisationUnit(RandomStringUtils.randomAlphabetic(1)));
    }
    return result;
  }

  static class QueryWithPagination implements ArgumentMatcher<Query> {
    int first;

    int size;

    QueryWithPagination(Query query) {
      this.first = query.getFirstResult();
      this.size = query.getMaxResults();
    }

    @Override
    public boolean matches(Query query) {
      return query.getFirstResult() == first && query.getMaxResults() == size;
    }
  }
}
