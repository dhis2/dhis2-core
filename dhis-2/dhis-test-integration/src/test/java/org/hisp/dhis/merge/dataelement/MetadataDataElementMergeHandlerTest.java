/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.merge.dataelement.handler.MetadataDataElementMergeHandler;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MetadataDataElementMergeHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private MetadataDataElementMergeHandler mergeHandler;
  @Autowired private DataElementService dataElementService;
  @Autowired private DataSetService dataSetService;
  @Autowired private IdentifiableObjectManager manager;

  private DataElement deSource;
  private DataElement deTarget;
  private DataSet ds1;
  private DataSet ds2;
  private DataElementGroup deg1;
  private DataElementGroup deg2;

  @BeforeEach
  public void setUp() {
    deSource = createDataElement('s');
    deTarget = createDataElement('t');

    dataElementService.addDataElement(deSource);
    dataElementService.addDataElement(deTarget);

    ds1 = createDataSet('s');
    ds2 = createDataSet('t');

    dataSetService.addDataSet(ds1);
    dataSetService.addDataSet(ds2);

    DataSetElement dse1 = new DataSetElement(ds1, deSource);
    DataSetElement dse2 = new DataSetElement(ds2, deTarget);

    ds1.addDataSetElement(dse1);
    deSource.getDataSetElements().add(dse1);

    ds2.addDataSetElement(dse2);
    deTarget.getDataSetElements().add(dse2);

    deg1 = createDataElementGroup('s');
    deg2 = createDataElementGroup('t');

    deg1.addDataElement(deSource);
    deg2.addDataElement(deTarget);

    manager.save(List.of(deg1, deg2));
    //    deSource.addDataElementGroup(deg1);
    //    deTarget.addDataElementGroup(deg2);

    dataElementService.updateDataElement(deSource);
    dataElementService.updateDataElement(deTarget);

    dataSetService.updateDataSet(ds1);
    dataSetService.updateDataSet(ds2);
  }

  @Test
  @DisplayName("Ensure source data elements have no refs to data set elements")
  void sourceDeNoDseRefsTest() {
    // given state before merging data set elements
    assertEquals(
        1, deSource.getDataSetElements().size(), "source dataSetElements size should be 1");
    assertEquals(
        1, deTarget.getDataSetElements().size(), "target dataSetElements size should be 1");
    assertEquals(1, ds1.getDataSetElements().size(), "dataset dataSetElements size should be 1");
    assertEquals(1, ds2.getDataSetElements().size(), "dataset dataSetElements size should be 1");

    // when
    mergeHandler.handleDataSetElement(List.of(deSource), deTarget);

    // then check state after merging data set elements
    assertEquals(0, deSource.getDataSetElements().size(), "source dataSetElement size should be 0");
    assertEquals(2, deTarget.getDataSetElements().size(), "target dataSetElement size should be 2");
    assertEquals(1, ds1.getDataSetElements().size(), "dataset dataSetElements size should be 1");
    assertTrue(
        ds1.getDataSetElements().stream()
            .map(DataSetElement::getDataElement)
            .toList()
            .contains(deTarget),
        "dataset dataSetElement had target DE ref");
    assertEquals(1, ds2.getDataSetElements().size(), "dataset dataSetElements size should be 1");
  }

  @Test
  @DisplayName("Ensure source data element groups have no refs to data set elements")
  void sourceDeNoDegRefsTest() {
    // given state before merging data element groups
    assertEquals(1, deSource.getGroups().size(), "source dataElementGroups size should be 1");
    assertEquals(1, deTarget.getGroups().size(), "target dataElementGroups size should be 1");
    assertEquals(1, deg1.getMembers().size(), "dataElementGroup member size should be 1");
    assertEquals(1, deg2.getMembers().size(), "dataElementGroup member size should be 1");

    // when
    mergeHandler.handleDataElementGroup(List.of(deSource), deTarget);

    // then check state after merging data element groups
    assertEquals(0, deSource.getGroups().size(), "source dataElementGroups size should be 0");
    assertEquals(2, deTarget.getGroups().size(), "target dataElementGroups size should be 2");
    assertEquals(1, deg1.getMembers().size(), "dataElementGroup member size should be 1");
    assertEquals(1, deg2.getMembers().size(), "dataElementGroup member size should be 1");
  }
}
