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
package org.hisp.dhis.split.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class AnalyticalObjectOrgUnitSplitHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private AnalyticalObjectOrgUnitSplitHandler handler;

  private DataElement deA;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeAll
  void setUp() {
    deA = createDataElement('A');
    idObjectManager.save(deA);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
  }

  @Test
  void testSplitVisualizations() {
    Visualization vA = createVisualization('A');
    vA.addDataDimensionItem(deA);
    vA.getOrganisationUnits().add(ouA);
    Visualization vB = createVisualization('B');
    vB.addDataDimensionItem(deA);
    vB.getOrganisationUnits().add(ouA);
    idObjectManager.save(vA);
    idObjectManager.save(vB);
    assertEquals(2, getVisualizationCount(ouA));
    assertEquals(0, getVisualizationCount(ouB));
    assertEquals(0, getVisualizationCount(ouC));
    OrgUnitSplitRequest request =
        new OrgUnitSplitRequest.Builder()
            .withSource(ouA)
            .addTarget(ouB)
            .addTarget(ouC)
            .withPrimaryTarget(ouB)
            .build();
    handler.splitAnalyticalObjects(request);
    idObjectManager.update(ouC);
    assertEquals(0, getVisualizationCount(ouA));
    assertEquals(2, getVisualizationCount(ouB));
    assertEquals(2, getVisualizationCount(ouC));
  }

  /**
   * Test migrate HQL update statement with an HQL select statement to ensure the updated rows are
   * visible by the current transaction.
   *
   * @param target the {@link OrganisationUnit}
   * @return the count of interpretations.
   */
  private long getVisualizationCount(OrganisationUnit target) {
    return (Long)
        entityManager
            .createQuery(
                "select count(distinct v) from Visualization v where :target in elements(v.organisationUnits)")
            .setParameter("target", target)
            .getSingleResult();
  }
}
