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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.merge.Simple;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.schema.MetadataMergeParams;
import org.hisp.dhis.schema.MetadataMergeService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class MetadataMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private MetadataMergeService metadataMergeService;

  @Test
  void simpleReplace() {
    Date date = new Date();
    Simple source = new Simple("string", 10, date, false, 123, 2.5f);
    Simple target = new Simple();
    metadataMergeService.merge(
        new MetadataMergeParams<>(source, target).setMergeMode(MergeMode.REPLACE));
    assertEquals("string", target.getString());
    assertEquals(10, (int) target.getInteger());
    assertEquals(date, target.getDate());
    assertFalse(target.getBool());
    assertEquals(123, target.getAnInt());
  }

  @Test
  void mergeOrgUnitGroup() {
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    OrganisationUnit organisationUnitC = createOrganisationUnit('C');
    OrganisationUnit organisationUnitD = createOrganisationUnit('D');
    OrganisationUnitGroup organisationUnitGroupA = createOrganisationUnitGroup('A');
    OrganisationUnitGroup organisationUnitGroupB = createOrganisationUnitGroup('B');
    organisationUnitGroupA.getMembers().add(organisationUnitA);
    organisationUnitGroupA.getMembers().add(organisationUnitB);
    organisationUnitGroupA.getMembers().add(organisationUnitC);
    organisationUnitGroupA.getMembers().add(organisationUnitD);
    OrganisationUnitGroupSet organisationUnitGroupSetA = createOrganisationUnitGroupSet('A');
    organisationUnitGroupSetA.addOrganisationUnitGroup(organisationUnitGroupA);
    metadataMergeService.merge(
        new MetadataMergeParams<>(organisationUnitGroupA, organisationUnitGroupB)
            .setMergeMode(MergeMode.REPLACE));
    assertFalse(organisationUnitGroupB.getMembers().isEmpty());
    assertEquals(4, organisationUnitGroupB.getMembers().size());
    assertNotNull(organisationUnitGroupB.getGroupSets());
    assertFalse(organisationUnitGroupB.getGroupSets().isEmpty());
  }

  @Test
  void mergeOrgUnitGroupSet() {
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    OrganisationUnit organisationUnitC = createOrganisationUnit('C');
    OrganisationUnit organisationUnitD = createOrganisationUnit('D');
    OrganisationUnitGroup organisationUnitGroupA = createOrganisationUnitGroup('A');
    organisationUnitGroupA.getMembers().add(organisationUnitA);
    organisationUnitGroupA.getMembers().add(organisationUnitB);
    organisationUnitGroupA.getMembers().add(organisationUnitC);
    organisationUnitGroupA.getMembers().add(organisationUnitD);
    OrganisationUnitGroupSet organisationUnitGroupSetA = createOrganisationUnitGroupSet('A');
    OrganisationUnitGroupSet organisationUnitGroupSetB = createOrganisationUnitGroupSet('B');
    organisationUnitGroupSetA.addOrganisationUnitGroup(organisationUnitGroupA);
    metadataMergeService.merge(
        new MetadataMergeParams<>(organisationUnitGroupSetA, organisationUnitGroupSetB)
            .setMergeMode(MergeMode.REPLACE));
    assertFalse(organisationUnitGroupSetB.getOrganisationUnitGroups().isEmpty());
    assertEquals(organisationUnitGroupSetA.getName(), organisationUnitGroupSetB.getName());
    assertEquals(
        organisationUnitGroupSetA.getDescription(), organisationUnitGroupSetB.getDescription());
    assertEquals(
        organisationUnitGroupSetA.isCompulsory(), organisationUnitGroupSetB.isCompulsory());
    assertEquals(
        organisationUnitGroupSetA.isIncludeSubhierarchyInAnalytics(),
        organisationUnitGroupSetB.isIncludeSubhierarchyInAnalytics());
    assertEquals(1, organisationUnitGroupSetB.getOrganisationUnitGroups().size());
  }

  @Test
  void testIndicatorClone() {
    IndicatorType indicatorType = createIndicatorType('A');
    Indicator indicator = createIndicator('A', indicatorType);
    Indicator clone = metadataMergeService.clone(indicator);
    assertEquals(indicator.getName(), clone.getName());
    assertEquals(indicator.getUid(), clone.getUid());
    assertEquals(indicator.getCode(), clone.getCode());
    assertEquals(indicator.getIndicatorType(), clone.getIndicatorType());
  }
}
