/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.visualization.Icon.IconType.DATA_ITEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests for {@link DefaultVisualizationService}. */
@Transactional
class DefaultVisualizationServiceTest extends PostgresIntegrationTestBase {
  @Autowired private VisualizationService visualizationService;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testPostWithIconsObject() {
    // Given
    Icon icon = new Icon();
    icon.setType(DATA_ITEM);

    Visualization aVisWithIcons = createVisualization("any");
    aVisWithIcons.setIcons(Set.of(icon));

    // When
    long uid = visualizationService.save(aVisWithIcons);

    // Then
    Visualization saved = visualizationService.getVisualization(uid);
    assertIterableEquals(Set.of(icon), saved.getIcons());
    assertEquals("any", saved.getName());
  }

  private Visualization createVisualization(String name) {
    Visualization visualization = createVisualization('X');
    visualization.setName(name);
    return visualization;
  }

  // -------------------------------------------------------------------------
  // JPA migration verification (DataElementGroupSetDimension HBM -> annotations)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "JPA: DataElementGroupSetDimension (dimension + ordered items) round-trips through the "
          + "still-HBM-mapped Visualization owner")
  void testJpaDataElementGroupSetDimensionRoundTrip() {
    DataElementGroup degA = createDataElementGroup('A');
    DataElementGroup degB = createDataElementGroup('B');
    DataElementGroup degC = createDataElementGroup('C');
    manager.save(degA);
    manager.save(degB);
    manager.save(degC);

    DataElementGroupSet degs = createDataElementGroupSet('S');
    manager.save(degs);

    DataElementGroupSetDimension dimension = new DataElementGroupSetDimension();
    dimension.setDimension(degs);
    dimension.setItems(List.of(degC, degA, degB));

    Visualization visualization = createVisualization('V');
    visualization.addDataElementGroupSetDimension(dimension);

    long id = visualizationService.save(visualization);

    clearSession(); // force reload from DB

    Visualization reloaded = visualizationService.getVisualization(id);
    assertNotNull(reloaded);
    assertEquals(1, reloaded.getDataElementGroupSetDimensions().size());

    DataElementGroupSetDimension reloadedDimension =
        reloaded.getDataElementGroupSetDimensions().get(0);
    assertEquals(degs.getUid(), reloadedDimension.getDimension().getUid());

    // @OrderColumn must preserve insertion order, not just membership.
    List<String> itemUids =
        reloadedDimension.getItems().stream().map(DataElementGroup::getUid).toList();
    assertEquals(List.of(degC.getUid(), degA.getUid(), degB.getUid()), itemUids);
  }
}
