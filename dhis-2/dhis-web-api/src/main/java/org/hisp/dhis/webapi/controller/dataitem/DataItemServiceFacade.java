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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dataitem.query.QueryableDataItem.getEntities;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_IN;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntitiesFromInFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntityFromEqualFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.setFilteringParams;
import static org.hisp.dhis.webapi.controller.dataitem.helper.OrderingHelper.setOrderingParams;
import static org.hisp.dhis.webapi.controller.dataitem.helper.PaginationHelper.paginate;
import static org.hisp.dhis.webapi.controller.dataitem.helper.PaginationHelper.setMaxResultsWhenPaging;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.containsFilterWithAnyOfPrefixes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.QueryExecutor;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This class is tight to the controller layer and is responsible to encapsulate logic that does not
 * belong to the controller and does not belong to the service layer either. In other words, these
 * set of methods sit between the controller and service layers. The main goal is to alleviate the
 * controller layer.
 *
 * @author maikel arabori
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DataItemServiceFacade {
  private final CurrentUserService currentUserService;

  private final QueryExecutor queryExecutor;

  /**
   * This method will iterate through the list of target entities, and query each one of them using
   * the filters and params provided. The result list will bring together the results of all target
   * entities queried.
   *
   * @param targetEntities the list of entities to be retrieved
   * @param orderParams request ordering params
   * @param filters request filters
   * @param options request options
   * @return the consolidated collection of entities found.
   */
  List<DataItem> retrieveDataItemEntities(
      final Set<Class<? extends BaseIdentifiableObject>> targetEntities,
      final Set<String> filters,
      final WebOptions options,
      final OrderParams orderParams) {
    final List<DataItem> dataItems = new ArrayList<>();

    final User currentUser = currentUserService.getCurrentUser();

    if (isNotEmpty(targetEntities)) {
      // Defining the query params map, and setting the common params.
      final MapSqlParameterSource paramsMap =
          new MapSqlParameterSource().addValue("userUid", currentUser.getUid());

      setFilteringParams(filters, options, paramsMap, currentUser);

      setOrderingParams(orderParams, paramsMap);

      setMaxResultsWhenPaging(options, paramsMap);

      dataItems.addAll(queryExecutor.find(targetEntities, paramsMap));

      // In memory pagination.
      return paginate(options, dataItems);
    }

    return dataItems;
  }

  /**
   * This method returns a set of BaseDimensionalItemObject's based on the provided filters.
   *
   * @param filters
   * @return the data items classes to be queried
   */
  Set<Class<? extends BaseIdentifiableObject>> extractTargetEntities(final Set<String> filters) {
    final Set<Class<? extends BaseIdentifiableObject>> targetedEntities = new HashSet<>(0);

    if (containsFilterWithAnyOfPrefixes(
        filters, DIMENSION_TYPE_EQUAL.getCombination(), DIMENSION_TYPE_IN.getCombination())) {
      addFilteredTargetEntities(filters, targetedEntities);
    } else {
      // If no filter is set we search for all entities.
      targetedEntities.addAll(getEntities());
    }

    return targetedEntities;
  }

  private void addFilteredTargetEntities(
      final Set<String> filters,
      final Set<Class<? extends BaseIdentifiableObject>> targetedEntities) {
    final Iterator<String> iterator = filters.iterator();

    while (iterator.hasNext()) {
      final String filter = iterator.next();
      final Class<? extends BaseIdentifiableObject> entity = extractEntityFromEqualFilter(filter);
      final Set<Class<? extends BaseIdentifiableObject>> entities =
          extractEntitiesFromInFilter(filter);

      if (entity != null || isNotEmpty(entities)) {
        if (entity != null) {
          targetedEntities.add(entity);
        }

        if (isNotEmpty(entities)) {
          targetedEntities.addAll(entities);
        }
      }
    }
  }
}
