/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataValueStoreTest extends SingleSetupIntegrationTestBase {
  private @PersistenceContext EntityManager manager;
  @Autowired private DataValueStore dataValueStore;

  @Test
  void testGetSoftDeletedDataValue() {
    DataValue dataValue = createDataValue();
    dataValueStore.addDataValue(dataValue);
    dataValue.setDeleted(true);
    dataValueStore.updateDataValue(dataValue);
    DataValue deletedDataValue = dataValueStore.getSoftDeletedDataValue(dataValue);
    assertEquals(dataValue.getDataElement().getId(), deletedDataValue.getDataElement().getId());
    assertEquals(dataValue.getSource().getId(), deletedDataValue.getSource().getId());
    assertEquals(
        dataValue.getCategoryOptionCombo().getId(),
        deletedDataValue.getCategoryOptionCombo().getId());
    assertEquals(
        dataValue.getAttributeOptionCombo().getId(),
        deletedDataValue.getAttributeOptionCombo().getId());
    assertEquals(dataValue.getValue(), deletedDataValue.getValue());
  }

  private DataValue createDataValue() {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.TEXT);
    CategoryOptionCombo defaultCategoryOptionCombo = createCategoryOptionCombo('D');
    defaultCategoryOptionCombo.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    Period period = createPeriod(new Date(), new Date());
    period.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.persist(dataElement);
    manager.persist(organisationUnitA);
    manager.persist(period);
    manager.persist(defaultCategoryOptionCombo);
    CategoryOption categoryOption = createCategoryOption('A');
    categoryOption.setCategoryOptionCombos(Sets.newHashSet(defaultCategoryOptionCombo));
    manager.persist(categoryOption);
    defaultCategoryOptionCombo.getCategoryOptions().add(categoryOption);
    return createDataValue(
        dataElement, period, organisationUnitA, "test", defaultCategoryOptionCombo);
  }
}
