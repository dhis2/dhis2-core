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
package org.hisp.dhis.webapi.controller.dataitem;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;
import static org.hisp.dhis.dataitem.query.QueryableDataItem.getEntities;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE_SIZE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.QueryExecutor;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for DataItemServiceFacade.
 *
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class DataItemServiceFacadeTest {

  @Mock private CurrentUserService currentUserService;

  @Mock private QueryExecutor queryExecutor;

  private DataItemServiceFacade dataItemServiceFacade;

  @BeforeEach
  public void setUp() {
    dataItemServiceFacade = new DataItemServiceFacade(currentUserService, queryExecutor);
  }

  @Test
  void testRetrieveDataItemEntities() {
    // Given
    final Class<? extends BaseIdentifiableObject> targetEntity = Indicator.class;
    final Set<Class<? extends BaseIdentifiableObject>> anyTargetEntities =
        new HashSet<>(asList(targetEntity));
    final List<DataItem> expectedItemsFound =
        asList(mockDataItem(INDICATOR), mockDataItem(INDICATOR));
    final Set<String> anyFilters = newHashSet("anyFilter");
    final WebOptions anyWebOptions = mockWebOptions(10, 1);
    final Set<String> anyOrdering = new HashSet<>(asList("name:desc"));
    final OrderParams anyOrderParams = new OrderParams(anyOrdering);
    final User currentUser = new User();

    // When
    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    when(queryExecutor.find(anySet(), any(MapSqlParameterSource.class)))
        .thenReturn(expectedItemsFound);
    final List<DataItem> actualDimensionalItems =
        dataItemServiceFacade.retrieveDataItemEntities(
            anyTargetEntities, anyFilters, anyWebOptions, anyOrderParams);

    // Then
    assertThat(actualDimensionalItems, hasSize(2));
    assertThat(actualDimensionalItems.get(0).getDimensionItemType(), is(INDICATOR));
    assertThat(actualDimensionalItems.get(1).getDimensionItemType(), is(INDICATOR));
  }

  @Test
  void testRetrieveDataItemEntitiesWhenTargetEntitiesIsEmpty() {
    // Given
    final Set<Class<? extends BaseIdentifiableObject>> anyTargetEntities = emptySet();
    final Set<String> anyFilters = newHashSet("anyFilter");
    final WebOptions anyWebOptions = mockWebOptions(10, 1);
    final Set<String> anyOrdering = new HashSet<>(asList("name:desc"));
    final OrderParams anyOrderParams = new OrderParams(anyOrdering);

    // When
    final List<DataItem> actualDimensionalItems =
        dataItemServiceFacade.retrieveDataItemEntities(
            anyTargetEntities, anyFilters, anyWebOptions, anyOrderParams);

    // Then
    assertThat(actualDimensionalItems, is(empty()));
  }

  @Test
  void testExtractTargetEntitiesUsingEqualsFilter() {
    // Given
    final Set<Class<? extends BaseIdentifiableObject>> expectedTargetEntities =
        new HashSet<>(asList(Indicator.class));
    final Set<String> theFilters = newHashSet("dimensionItemType:eq:INDICATOR");

    // When
    final Set<Class<? extends BaseIdentifiableObject>> actualTargetEntities =
        dataItemServiceFacade.extractTargetEntities(theFilters);

    // Then
    assertThat(actualTargetEntities, containsInAnyOrder(expectedTargetEntities.toArray()));
  }

  @Test
  void testExtractTargetEntitiesUsingInFilter() {
    // Given
    final Set<Class<? extends BaseIdentifiableObject>> expectedTargetEntities =
        new HashSet<>(asList(Indicator.class, DataSet.class));
    final Set<String> theFilters = newHashSet("dimensionItemType:in:[INDICATOR, DATA_SET]");

    // When
    final Set<Class<? extends BaseIdentifiableObject>> actualTargetEntities =
        dataItemServiceFacade.extractTargetEntities(theFilters);

    // Then
    assertThat(actualTargetEntities, containsInAnyOrder(expectedTargetEntities.toArray()));
  }

  @Test
  void testExtractTargetEntitiesWhenThereIsNoExplicitTargetSet() {
    // Given
    final Set<String> noTargetEntitiesFilters = emptySet();

    // When
    final Set<Class<? extends BaseIdentifiableObject>> actualTargetEntities =
        dataItemServiceFacade.extractTargetEntities(noTargetEntitiesFilters);

    // Then
    assertThat(actualTargetEntities, containsInAnyOrder(getEntities().toArray()));
  }

  private WebOptions mockWebOptions(final int pageSize, final int pageNumber) {
    final Map<String, String> options = new HashMap<>(0);
    options.put(PAGE_SIZE, valueOf(pageSize));
    options.put(PAGE, valueOf(pageNumber));
    options.put(PAGING, "true");

    return new WebOptions(options);
  }

  private DataItem mockDataItem(final DimensionItemType dimensionItemType) {
    final DataItem dataItem = DataItem.builder().dimensionItemType(dimensionItemType).build();

    return dataItem;
  }
}
