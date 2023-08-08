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
package org.hisp.dhis.split.orgunit.handler;

import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith(MockitoExtension.class)
class MetadataOrgUnitSplitHandlerTest {

  private MetadataOrgUnitSplitHandler handler;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeEach
  public void setUp() {
    handler =
        new MetadataOrgUnitSplitHandler(mock(UserService.class), mock(ConfigurationService.class));

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
  }

  @Test
  void testSplitDataSets() {
    DataSet dsA = createDataSet('A');
    dsA.addOrganisationUnit(ouA);

    DataSet dsB = createDataSet('B');
    dsB.addOrganisationUnit(ouA);

    OrgUnitSplitRequest request =
        new OrgUnitSplitRequest.Builder()
            .withSource(ouA)
            .addTarget(ouB)
            .addTarget(ouC)
            .withPrimaryTarget(ouB)
            .build();

    assertEquals(2, ouA.getDataSets().size());
    assertEquals(0, ouB.getDataSets().size());
    assertEquals(0, ouC.getDataSets().size());

    handler.splitDataSets(request);

    assertEquals(0, ouA.getDataSets().size());
    assertEquals(2, ouB.getDataSets().size());
    assertEquals(2, ouC.getDataSets().size());
  }

  @Test
  void testSplitPrograms() {
    Program prA = createProgram('A');
    prA.addOrganisationUnit(ouA);

    Program prB = createProgram('B');
    prB.addOrganisationUnit(ouA);

    OrgUnitSplitRequest request =
        new OrgUnitSplitRequest.Builder()
            .withSource(ouA)
            .addTarget(ouB)
            .addTarget(ouC)
            .withPrimaryTarget(ouB)
            .build();

    assertEquals(2, ouA.getPrograms().size());
    assertEquals(0, ouB.getPrograms().size());
    assertEquals(0, ouC.getPrograms().size());

    handler.splitPrograms(request);

    assertEquals(0, ouA.getPrograms().size());
    assertEquals(2, ouB.getPrograms().size());
    assertEquals(2, ouC.getPrograms().size());
  }
}
