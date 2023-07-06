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
package org.hisp.dhis.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class MappingServiceTest extends DhisSpringTest {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private CategoryService categoryService;

  @Autowired private MappingService mappingService;

  @Autowired private RenderService _renderService;

  private CategoryOption coA = createCategoryOption('A');

  private Category caA = createCategory('A', coA);

  private DataElement deA = createDataElement('A');

  private OrganisationUnit ouA = createOrganisationUnit('A');

  private OrganisationUnitGroup ougA = createOrganisationUnitGroup('A');

  private OrganisationUnitGroupSet ougsA = createOrganisationUnitGroupSet('A');

  @Override
  public void setUpTest() {
    renderService = _renderService;
    deA.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    ougsA.addOrganisationUnitGroup(ougA);
    idObjectManager.save(Lists.newArrayList(coA, caA, deA, ouA, ougA, ougsA));
  }

  @Test
  void testAddGet() {
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
