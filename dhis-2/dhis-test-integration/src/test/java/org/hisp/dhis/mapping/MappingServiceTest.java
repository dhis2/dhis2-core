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
package org.hisp.dhis.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
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
class MappingServiceTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private CategoryService categoryService;

  @Autowired private MappingService mappingService;

  @Autowired private RenderService _renderService;

  @BeforeAll
  void setUp() {
    renderService = _renderService;
  }

  @Test
  void testAddGet() {
    CategoryOption coA = createCategoryOption('A');
    Category caA = createCategory('A', coA);
    DataElement deA = createDataElement('A');
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnitGroup ougA = createOrganisationUnitGroup('A');
    OrganisationUnitGroupSet ougsA = createOrganisationUnitGroupSet('A');
    deA.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    ougsA.addOrganisationUnitGroup(ougA);
    idObjectManager.save(Lists.newArrayList(coA, caA, deA, ouA, ougA, ougsA));

    MapView mvA = new MapView("thematic");
    CategoryDimension cadA = new CategoryDimension();
    cadA.setDimension(caA);
    cadA.setItems(Lists.newArrayList(coA));
    OrganisationUnitGroupSetDimension ougsdA = new OrganisationUnitGroupSetDimension();
    ougsdA.setDimension(ougsA);
    ougsdA.setItems(Lists.newArrayList(ougA));
    mvA.addCategoryDimension(cadA);
    mvA.addDataDimensionItem(deA);
    mvA.getOrganisationUnits().add(ouA);
    mvA.addOrganisationUnitGroupSetDimension(ougsdA);
    Map mpA = new Map("MapA", null, 0d, 0d, 0);
    mpA.getMapViews().add(mvA);
    long id = mappingService.addMap(mpA);
    Map map = mappingService.getMap(id);
    assertNotNull(map);
    assertEquals(1, map.getMapViews().size());
    MapView mapView = map.getMapViews().get(0);
    assertNotNull(mapView);
    assertEquals(1, mapView.getCategoryDimensions().size());
    assertEquals(1, mapView.getDataElements().size());
    assertEquals(1, mapView.getOrganisationUnits().size());
    assertEquals(1, mapView.getOrganisationUnitGroupSetDimensions().size());
    assertEquals(cadA, mapView.getCategoryDimensions().get(0));
    assertEquals(deA, mapView.getDataElements().get(0));
    assertEquals(ouA, mapView.getOrganisationUnits().get(0));
    assertEquals(ougsdA, mapView.getOrganisationUnitGroupSetDimensions().get(0));
  }
}
