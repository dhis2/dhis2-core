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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.hisp.dhis.common.MetadataMergeMode;
import org.hisp.dhis.dxf2.metadata.merge.Simple;
import org.hisp.dhis.dxf2.metadata.merge.SimpleCollection;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.schema.MetadataMergeParams;
import org.hisp.dhis.schema.MetadataMergeService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class MetadataMergeServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private MetadataMergeService metadataMergeService;

  @Override
  public void setUpTest() {}

  @Test
  void simpleReplace() {
    Date date = new Date();
    Simple source = new Simple("string", 10, date, false, 123, 2.5f);
    Simple target = new Simple();
    metadataMergeService.merge(
        new MetadataMergeParams<>(source, target).setMergeMode(MetadataMergeMode.REPLACE));
    Assertions.assertEquals("string", target.getString());
    assertEquals(10, (int) target.getInteger());
    Assertions.assertEquals(date, target.getDate());
    Assertions.assertEquals(false, target.getBool());
    Assertions.assertEquals(123, target.getAnInt());
  }

  @Test
  void simpleMerge() {
    Date date = new Date();
    Simple source = new Simple(null, 10, date, null, 123, 2.5f);
    Simple target = new Simple("hello", 20, date, true, 123, 2.5f);
    metadataMergeService.merge(
        new MetadataMergeParams<>(source, target).setMergeMode(MetadataMergeMode.MERGE));
    Assertions.assertEquals("hello", target.getString());
    assertEquals(10, (int) target.getInteger());
    Assertions.assertEquals(date, target.getDate());
    Assertions.assertEquals(true, target.getBool());
  }

  @Test
  void simpleCollection() {
    Date date = new Date();
    SimpleCollection source = new SimpleCollection("name");
    source.getSimples().add(new Simple("simple", 10, date, false, 123, 2.5f));
    source.getSimples().add(new Simple("simple", 20, date, false, 123, 2.5f));
    source.getSimples().add(new Simple("simple", 30, date, false, 123, 2.5f));
    SimpleCollection target = new SimpleCollection("target");
    metadataMergeService.merge(
        new MetadataMergeParams<>(source, target).setMergeMode(MetadataMergeMode.MERGE));
    Assertions.assertEquals("name", target.getName());
    Assertions.assertEquals(3, target.getSimples().size());
    Assertions.assertTrue(target.getSimples().contains(source.getSimples().get(0)));
    Assertions.assertTrue(target.getSimples().contains(source.getSimples().get(1)));
    Assertions.assertTrue(target.getSimples().contains(source.getSimples().get(2)));
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
            .setMergeMode(MetadataMergeMode.REPLACE));
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
            .setMergeMode(MetadataMergeMode.REPLACE));
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
