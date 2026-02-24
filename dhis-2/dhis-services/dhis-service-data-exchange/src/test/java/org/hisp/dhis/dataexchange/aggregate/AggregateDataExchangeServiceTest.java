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
package org.hisp.dhis.dataexchange.aggregate;

import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.datavalue.DataExportService;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateDataExchangeServiceTest {
  @Mock private AnalyticsService analyticsService;

  @Mock private AggregateDataExchangeStore aggregateDataExchangeStore;

  @Mock private DataQueryService dataQueryService;

  @Mock private AclService aclService;

  @Mock private DataExportService dataExportService;

  @InjectMocks private AggregateDataExchangeService service;

  @Test
  @SuppressWarnings("unchecked")
  void testToDataQueryParams() {
    when(dataQueryService.getDimension(
            eq(DATA_X_DIM_ID),
            any(),
            any(Date.class),
            nullable(List.class),
            anyBoolean(),
            nullable(DisplayProperty.class),
            nullable(IdScheme.class)))
        .thenReturn(new BaseDimensionalObject(DATA_X_DIM_ID, DimensionType.DATA_X, List.of()));
    when(dataQueryService.getDimension(
            eq(PERIOD_DIM_ID),
            any(),
            any(Date.class),
            nullable(List.class),
            anyBoolean(),
            nullable(DisplayProperty.class),
            nullable(IdScheme.class)))
        .thenReturn(new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, List.of()));
    when(dataQueryService.getDimension(
            eq(ORGUNIT_DIM_ID),
            any(),
            any(Date.class),
            nullable(List.class),
            anyBoolean(),
            nullable(DisplayProperty.class),
            nullable(IdScheme.class)))
        .thenReturn(
            new BaseDimensionalObject(ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of()));

    SourceRequest sourceRequest =
        new SourceRequest()
            .setName("SourceRequestA")
            .setDx(List.of("Vz0C3i4Wy3M", "ToaOToReol6"))
            .setPe(List.of("202101", "202102"))
            .setOu(List.of("lGgJFgRkZui", "pvINfKxtqyN", "VOyqQ54TehY"))
            .setAggregationType(AggregationType.COUNT)
            .setOutputDataElementIdScheme(IdProperty.UID)
            .setOutputOrgUnitIdScheme(IdProperty.CODE)
            .setOutputDataItemIdScheme(IdProperty.NAME)
            .setOutputIdScheme(IdProperty.CODE);

    DataQueryParams query = service.toDataQueryParams(sourceRequest, null);

    assertTrue(query.hasDimension(DATA_X_DIM_ID));
    assertTrue(query.hasDimension(PERIOD_DIM_ID));
    assertTrue(query.hasDimension(ORGUNIT_DIM_ID));
    assertEquals(
        new AnalyticsAggregationType(AggregationType.COUNT, AggregationType.COUNT),
        query.getAggregationType());
    assertEquals(IdScheme.UID, query.getOutputDataElementIdScheme());
    assertEquals(IdScheme.CODE, query.getOutputOrgUnitIdScheme());
    assertEquals(IdScheme.NAME, query.getOutputDataItemIdScheme());
    assertEquals(IdScheme.CODE, query.getOutputIdScheme());

    query = service.toDataQueryParams(sourceRequest, IdProperty.CODE);

    assertEquals(IdScheme.CODE, query.getOutputDataElementIdScheme());
    assertEquals(IdScheme.CODE, query.getOutputOrgUnitIdScheme());
    assertEquals(IdScheme.CODE, query.getOutputDataItemIdScheme());
    assertEquals(IdScheme.CODE, query.getOutputIdScheme());
  }

  @Test
  void testToAggregationType() {
    assertEquals(
        new AnalyticsAggregationType(AggregationType.COUNT, AggregationType.COUNT),
        service.toAnalyticsAggregationType(AggregationType.COUNT));
    assertNull(service.toAnalyticsAggregationType(null));
  }

  @Test
  void testGetDhis2Client() {
    Api api =
        new Api()
            .setUrl("https://play.dhis2.org/demo")
            .setUsername("admin")
            .setPassword("district");

    Target target = new Target().setType(TargetType.EXTERNAL).setApi(api);

    AggregateDataExchange exchange = new AggregateDataExchange().setTarget(target);

    Dhis2Client client = service.getDhis2Client(exchange);

    assertEquals("https://play.dhis2.org/demo", client.getUrl());
  }

  @Test
  void testGetDhis2ClientIllegalState() {
    Api api = new Api().setUrl("https://play.dhis2.org/demo");

    Target target = new Target().setType(TargetType.EXTERNAL).setApi(api);

    AggregateDataExchange exchange = new AggregateDataExchange().setTarget(target);

    assertThrows(IllegalStateException.class, () -> service.getDhis2Client(exchange));
  }

  @Test
  void testIsPersisted() {
    AggregateDataExchange adeA = new AggregateDataExchange();
    adeA.setId(1);
    adeA.setAutoFields();
    adeA.setName("DataExchangeA");

    AggregateDataExchange adeB = new AggregateDataExchange();
    adeB.setAutoFields();
    adeB.setName("DataExchangeB");

    assertTrue(service.isPersisted(adeA));
    assertFalse(service.isPersisted(adeB));
  }

  @Test
  void testGetSourceDataWithoutAccess() {
    lenient()
        .when(aclService.canDataRead(any(UserDetails.class), any(IdentifiableObject.class)))
        .thenReturn(false);
    assertThrows(
        ForbiddenException.class,
        () ->
            service.getSourceData(
                UserDetails.fromUser(new User()), "uid", new SourceDataQueryParams()));
  }

  @Test
  void testGetSourceDataValueSetsWithoutAccess() {
    lenient()
        .when(aclService.canDataRead(any(UserDetails.class), any(IdentifiableObject.class)))
        .thenReturn(false);
    assertThrows(
        ForbiddenException.class,
        () ->
            service.getSourceDataValueSets(
                UserDetails.fromUser(new User()), "uid", new SourceDataQueryParams()));
  }
}
