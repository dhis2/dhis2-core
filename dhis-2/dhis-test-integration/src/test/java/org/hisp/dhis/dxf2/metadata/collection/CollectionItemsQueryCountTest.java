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
package org.hisp.dhis.dxf2.metadata.collection;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.event.CollectionMembershipChangedEvent;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the cost of changing an unloaded owning collection through the collection endpoints: the
 * statements against the join table and the invalidations of the owner's collection must not grow
 * with the number of items.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@RecordApplicationEvents
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class CollectionItemsQueryCountTest extends PostgresIntegrationTestBase {
  private static final String JOIN_TABLE = "orgunitgroupmembers";
  private static final String MEMBERS_ROLE = OrganisationUnitGroup.class.getName() + ".members";
  private static final String GROUPS_ROLE = OrganisationUnit.class.getName() + ".groups";
  private static final int UNITS = 6;

  @Autowired private CollectionService collectionService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ApplicationEvents events;

  private String group;
  private final List<String> units = new ArrayList<>();

  @BeforeEach
  void setUp() {
    OrganisationUnitGroup owner = createOrganisationUnitGroup('A');
    manager.save(owner);
    group = owner.getUid();
    for (int i = 0; i < UNITS; i++) {
      OrganisationUnit unit = createOrganisationUnit((char) ('A' + i));
      manager.save(unit);
      units.add(unit.getUid());
    }
  }

  @Test
  void itemCountDoesNotScaleJoinTableStatements() throws Exception {
    // one query reads the membership of all items, one JDBC batch writes all rows
    assertEquals(2, countJoinTableStatements(units, List.of()), "adding " + UNITS + " members");
    assertEquals(2, countJoinTableStatements(List.of(), units), "removing " + UNITS + " members");
  }

  @Test
  void ownerCollectionIsInvalidatedOncePerCall() throws Exception {
    TypeReport report = merge(units, List.of());

    assertEquals(UNITS, report.getStats().updated());
    Map<String, Long> invalidations =
        events.stream(CollectionMembershipChangedEvent.class)
            .collect(groupingBy(CollectionMembershipChangedEvent::role, counting()));
    // every item has its own inverse collection, all rows share the collection of the owner
    assertEquals(Map.of(MEMBERS_ROLE, 1L, GROUPS_ROLE, (long) UNITS), invalidations);
  }

  private long countJoinTableStatements(List<String> additions, List<String> deletions)
      throws Exception {
    QueryCountDataSourceProxy.clearCapturedSql();
    merge(additions, deletions);
    return QueryCountDataSourceProxy.countCapturedSqlMatching(JOIN_TABLE);
  }

  private TypeReport merge(List<String> additions, List<String> deletions) throws Exception {
    clearSession();
    OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
    IdentifiableObjects items = new IdentifiableObjects();
    items.setAdditions(
        additions.stream().map(uid -> new BaseIdentifiableObject(uid, "", "")).toList());
    items.setDeletions(
        deletions.stream().map(uid -> new BaseIdentifiableObject(uid, "", "")).toList());

    TypeReport report = collectionService.mergeCollectionItems(owner, "organisationUnits", items);

    assertFalse(Hibernate.isInitialized(owner.getMembers()), "members must stay unloaded");
    return report;
  }
}
