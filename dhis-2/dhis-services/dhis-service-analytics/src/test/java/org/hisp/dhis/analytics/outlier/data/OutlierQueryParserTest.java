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
package org.hisp.dhis.analytics.outlier.data;

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createDataSet;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutlierQueryParserTest {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private DimensionalObjectProvider dimensionalObjectProducer;

  @Mock private UserService userService;

  @InjectMocks private OutlierQueryParser subject;

  @BeforeEach
  void setup() {
    DataSet dataSet = createDataSet('A');
    when(idObjectManager.getByUid(eq(DataSet.class), anyCollection())).thenReturn(List.of(dataSet));

    DataElement dataElement = createDataElement('B');
    when(idObjectManager.getByUid(eq(DataElement.class), anyCollection()))
        .thenReturn(List.of(dataElement));

    OrganisationUnit organisationUnit = createOrganisationUnit('O');
    DimensionalObject baseDimensionalObject = new BaseDimensionalObject();
    baseDimensionalObject.setItems(List.of(organisationUnit));
    lenient()
        .when(
            dimensionalObjectProducer.getOrgUnitDimension(
                anyList(), eq(DisplayProperty.NAME), anyList(), eq(IdScheme.UID)))
        .thenReturn(baseDimensionalObject);

    User user = new User();
    user.setUsername("test");
    user.setDataViewOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContextNoSettings(UserDetails.fromUser(user));
    when(userService.getUserByUsername(anyString())).thenReturn(user);
  }

  @Test
  void testGetFromQueryDataElement() {
    // given
    OutlierQueryParams params = new OutlierQueryParams();
    params.setOu(Set.of("ou"));
    // when
    OutlierRequest request = subject.getFromQuery(params, false);
    // then
    assertEquals(1, (long) request.getDataDimensions().size());
    assertEquals("deabcdefghB", request.getDataDimensions().get(0).getDataElement().getUid());
  }

  @Test
  void testGetFromQueryOrgUnit() {
    // given
    OutlierQueryParams params = new OutlierQueryParams();
    params.setOu(Set.of("ou"));
    // when
    OutlierRequest request = subject.getFromQuery(params, false);
    // then
    assertEquals(1, (long) request.getOrgUnits().size());
    assertEquals("ouabcdefghO", request.getOrgUnits().get(0).getUid());
  }

  @Test
  void testGetFromQueryNoOrgUnit() {
    // given
    OutlierQueryParams params = new OutlierQueryParams();
    // when
    OutlierRequest request = subject.getFromQuery(params, false);
    // then
    assertEquals(1, (long) request.getOrgUnits().size());
  }
}
