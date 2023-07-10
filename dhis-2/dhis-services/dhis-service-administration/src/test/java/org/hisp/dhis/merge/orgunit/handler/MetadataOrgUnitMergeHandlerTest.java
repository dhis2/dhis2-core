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
package org.hisp.dhis.merge.orgunit.handler;

import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnitGroup;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith(MockitoExtension.class)
class MetadataOrgUnitMergeHandlerTest {

  private MetadataOrgUnitMergeHandler handler;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeEach
  public void setUp() {
    handler =
        new MetadataOrgUnitMergeHandler(mock(UserService.class), mock(ConfigurationService.class));

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
  }

  @Test
  void testMergeDataSets() {
    DataSet dsA = createDataSet('A');
    dsA.addOrganisationUnit(ouA);
    dsA.addOrganisationUnit(ouB);

    DataSet dsB = createDataSet('B');
    dsB.addOrganisationUnit(ouA);

    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();

    assertEquals(2, ouA.getDataSets().size());
    assertEquals(1, ouB.getDataSets().size());
    assertEquals(0, ouC.getDataSets().size());

    handler.mergeDataSets(request);

    assertEquals(0, ouA.getDataSets().size());
    assertEquals(0, ouB.getDataSets().size());
    assertEquals(2, ouC.getDataSets().size());
  }

  @Test
  void testMergePrograms() {
    Program prA = createProgram('A');
    prA.addOrganisationUnit(ouA);
    prA.addOrganisationUnit(ouB);

    Program prB = createProgram('B');
    prB.addOrganisationUnit(ouA);

    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();

    assertEquals(2, ouA.getPrograms().size());
    assertEquals(1, ouB.getPrograms().size());
    assertEquals(0, ouC.getPrograms().size());

    handler.mergePrograms(request);

    assertEquals(0, ouA.getPrograms().size());
    assertEquals(0, ouB.getPrograms().size());
    assertEquals(2, ouC.getPrograms().size());
  }

  @Test
  void testMergeOrgUnitGroups() {
    OrganisationUnitGroup ougA = createOrganisationUnitGroup('A');
    ougA.addOrganisationUnit(ouA);
    ougA.addOrganisationUnit(ouB);

    OrganisationUnitGroup ougB = createOrganisationUnitGroup('B');
    ougB.addOrganisationUnit(ouA);

    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();

    assertEquals(2, ouA.getGroups().size());
    assertEquals(1, ouB.getGroups().size());
    assertEquals(0, ouC.getGroups().size());

    handler.mergeOrgUnitGroups(request);

    assertEquals(0, ouA.getGroups().size());
    assertEquals(0, ouB.getGroups().size());
    assertEquals(2, ouC.getGroups().size());
  }
}
