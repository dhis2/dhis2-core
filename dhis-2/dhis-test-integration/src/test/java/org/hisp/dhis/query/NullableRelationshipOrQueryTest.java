/*
 * Copyright (c) 2004-2026, University of Oslo
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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class NullableRelationshipOrQueryTest extends PostgresIntegrationTestBase {

  @Autowired private QueryService queryService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  private OrganisationUnit standalone;

  private OrganisationUnit child;

  @BeforeEach
  void setUpOrganisationUnits() {
    standalone = createOrganisationUnit('A');
    standalone.setName("Standalone");
    identifiableObjectManager.save(standalone);

    OrganisationUnit parent = createOrganisationUnit('B');
    parent.setName("Parent");
    identifiableObjectManager.save(parent);

    child = createOrganisationUnit('C', parent);
    child.setName("Child");
    identifiableObjectManager.save(child);
    clearSession();
  }

  @Test
  void displayNameOrParentNameIncludesParentlessMatchesBeforePagingAndCounting() {
    Query<OrganisationUnit> query = search(1, 10);
    assertEquals(List.of(standalone.getUid(), child.getUid()), searchUids(query));
    assertEquals(2, queryService.count(query));

    Query<OrganisationUnit> firstPage = search(1, 1);
    Query<OrganisationUnit> secondPage = search(2, 1);
    assertEquals(List.of(standalone.getUid()), searchUids(firstPage));
    assertEquals(List.of(child.getUid()), searchUids(secondPage));
    assertEquals(2, queryService.count(firstPage));
    assertEquals(2, queryService.count(secondPage));
  }

  @Test
  void mandatoryScopeStillRestrictsTheMemoryOrLeg() {
    Query<OrganisationUnit> query =
        search(1, 1)
            .addPredicateSupplier(
                new JpaPredicateSupplier() {
                  @Override
                  public <Y> Predicate getPredicate(
                      CriteriaBuilder builder, Root<Y> root, CriteriaQuery<?> criteriaQuery) {
                    return builder.equal(root.get("uid"), standalone.getUid());
                  }
                });

    assertEquals(List.of(standalone.getUid()), searchUids(query));
    assertEquals(1, queryService.count(query));
  }

  private Query<OrganisationUnit> search(int page, int pageSize) {
    return queryService.getQueryFromUrl(
        OrganisationUnit.class,
        new GetObjectListParams()
            .setRootJunction(Junction.Type.OR)
            .setFilters(List.of("displayName:ilike:Standalone", "parent.name:eq:Parent"))
            .setOrders(List.of("id:asc"))
            .setPage(page)
            .setPageSize(pageSize));
  }

  private List<String> searchUids(Query<OrganisationUnit> query) {
    return queryService.query(query).stream().map(OrganisationUnit::getUid).toList();
  }
}
