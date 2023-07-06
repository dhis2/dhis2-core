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
package org.hisp.dhis.minmax;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 */
class MinMaxDataElementStoreTest extends DhisSpringTest {

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private CategoryService categoryService;

  @Autowired private MinMaxDataElementStore minMaxDataElementStore;

  @Test
  void testBasic() {
    OrganisationUnit source1 = createOrganisationUnit('A');
    OrganisationUnit source2 = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(source1);
    organisationUnitService.addOrganisationUnit(source2);
    DataElement dataElement1 = createDataElement('A');
    DataElement dataElement2 = createDataElement('B');
    DataElement dataElement3 = createDataElement('C');
    DataElement dataElement4 = createDataElement('D');
    dataElementService.addDataElement(dataElement1);
    dataElementService.addDataElement(dataElement2);
    dataElementService.addDataElement(dataElement3);
    dataElementService.addDataElement(dataElement4);
    CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(dataElement1, source1, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(dataElement2, source2, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(dataElement3, source2, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement4 =
        new MinMaxDataElement(dataElement4, source2, optionCombo, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    long mmdeid1 = minMaxDataElement1.getId();
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);
    minMaxDataElementStore.save(minMaxDataElement4);
    // ----------------------------------------------------------------------
    // Assertions
    // ----------------------------------------------------------------------
    assertNotNull(minMaxDataElementStore.get(mmdeid1));
    assertTrue(minMaxDataElementStore.get(mmdeid1).getMax() == 100);
    List<DataElement> dataElements1 = new ArrayList<>();
    dataElements1.add(dataElement1);
    List<DataElement> dataElements2 = new ArrayList<>();
    dataElements2.add(dataElement2);
    dataElements2.add(dataElement3);
    dataElements2.add(dataElement4);
    assertNotNull(minMaxDataElementStore.get(source1, dataElement1, optionCombo));
    assertNull(minMaxDataElementStore.get(source2, dataElement1, optionCombo));
    assertEquals(1, minMaxDataElementStore.get(source1, dataElements1).size());
    assertEquals(3, minMaxDataElementStore.get(source2, dataElements2).size());
    minMaxDataElementStore.delete(minMaxDataElement1);
    assertNull(minMaxDataElementStore.get(mmdeid1));
  }

  @Test
  void testQuery() {
    OrganisationUnit source1 = createOrganisationUnit('A');
    OrganisationUnit source2 = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(source1);
    organisationUnitService.addOrganisationUnit(source2);
    DataElement dataElement1 = createDataElement('A');
    DataElement dataElement2 = createDataElement('B');
    DataElement dataElement3 = createDataElement('C');
    DataElement dataElement4 = createDataElement('D');
    dataElementService.addDataElement(dataElement1);
    dataElementService.addDataElement(dataElement2);
    dataElementService.addDataElement(dataElement3);
    dataElementService.addDataElement(dataElement4);
    CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(dataElement1, source1, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(dataElement2, source2, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(dataElement3, source2, optionCombo, 0, 100, false);
    MinMaxDataElement minMaxDataElement4 =
        new MinMaxDataElement(dataElement4, source2, optionCombo, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);
    minMaxDataElementStore.save(minMaxDataElement4);
    MinMaxDataElementQueryParams params = new MinMaxDataElementQueryParams();
    List<String> filters = Lists.newArrayList();
    filters.add("dataElement.id:eq:" + dataElement1.getUid());
    params.setFilters(filters);
    List<MinMaxDataElement> result = minMaxDataElementStore.query(params);
    assertNotNull(result);
    assertEquals(1, result.size());
    params = new MinMaxDataElementQueryParams();
    filters.clear();
    filters.add("min:eq:0");
    params.setFilters(filters);
    result = minMaxDataElementStore.query(params);
    assertNotNull(result);
    assertEquals(4, result.size());
    filters.clear();
    filters.add("dataElement.id:in:[" + dataElement1.getUid() + "," + dataElement2.getUid() + "]");
    params.setFilters(filters);
    result = minMaxDataElementStore.query(params);
    assertNotNull(result);
    assertEquals(2, result.size());
  }
}
