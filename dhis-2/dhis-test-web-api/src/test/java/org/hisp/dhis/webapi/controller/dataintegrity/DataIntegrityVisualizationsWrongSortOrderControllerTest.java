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
package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.visualization.VisualizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test for visualizations with a gap in the sort order of their data dimension items. A gap causes
 * {@code org.hisp.dhis.webapi.controller.VisualizationController
 * #addExpressionDimensionItemElementsToDataDimensionItems} to throw a NullPointerException when the
 * visualization is opened, since Hibernate's indexed {@code <list>} mapping for {@code
 * dataDimensionItems} silently reconstructs a {@code null} element at the missing index. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/visualizations_wrong_sort_order.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityVisualizationsWrongSortOrderControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private VisualizationService visualizationService;

  @Autowired private DataElementService dataElementService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private Visualization viz;

  private static final String check = "visualizations_wrong_sort_order";

  private static final String detailsIdType = "visualizations";

  @BeforeEach
  void setUp() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);

    viz = new Visualization("myviz");
    viz.setType(VisualizationType.PIVOT_TABLE);
    viz.getDataDimensionItems().add(DataDimensionItem.create(dataElementA));
    viz.getDataDimensionItems().add(DataDimensionItem.create(dataElementB));
    viz.getDataDimensionItems().add(DataDimensionItem.create(dataElementC));

    visualizationService.save(viz);
    dbmsManager.clearSession();
  }

  @Test
  void testVisualizationWrongSortOrder() {
    /* Simulate the gap seen in production: bump the last item's sort_order from 2 to 3, so
     * the sequence becomes 0, 1, 3 instead of 0, 1, 2. */
    jdbcTemplate.update(
        "update visualization_datadimensionitems set sort_order = 3 "
            + "where visualizationid = (select visualizationid from visualization where uid = ?) "
            + "and sort_order = 2",
        viz.getUid());

    assertHasDataIntegrityIssues(detailsIdType, check, 100, viz.getUid(), "myviz", "3 != 2", true);
  }

  @Test
  void testVisualizationRightSortOrder() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }
}
