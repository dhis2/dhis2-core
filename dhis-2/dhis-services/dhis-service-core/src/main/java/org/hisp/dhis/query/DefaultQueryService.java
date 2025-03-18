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
package org.hisp.dhis.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Default implementation of QueryService which works with IdObjects.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DefaultQueryService implements QueryService {

  private final QueryParser queryParser;
  private final QueryPlanner queryPlanner;
  private final SchemaService schemaService;

  private final JpaCriteriaQueryEngine dbQueryEngine;
  private final InMemoryQueryEngine memoryQueryEngine;

  @Override
  public <T extends IdentifiableObject> List<T> query(Query<T> query) {
    return queryObjects(query);
  }

  @Override
  public long count(Query<?> query) {
    Query<?> count = Query.copyOf(query);

    count.clearOrders();
    count.setFirstResult(0);
    count.setMaxResults(Integer.MAX_VALUE);

    return countObjects(count);
  }

  @Override
  public <T extends IdentifiableObject> Query<T> getQueryFromUrl(
      Class<T> type, GetObjectListParams params) throws QueryParserException {
    List<String> filters = params.getFilters();
    if (filters == null) filters = List.of();
    Query<T> query = queryParser.parse(type, filters, params.getRootJunction());

    Schema schema = schemaService.getDynamicSchema(type);
    query.addOrders(QueryUtils.convertOrderStrings(params.getOrders(), schema));

    Pagination pagination = params.getPagination();
    if (pagination.hasPagination()) {
      query.setFirstResult(pagination.getFirstResult());
      query.setMaxResults(pagination.getSize());
    }

    return query;
  }

  // ---------------------------------------------------------------------------------------------
  // Helper methods
  // ---------------------------------------------------------------------------------------------

  private <T extends IdentifiableObject> long countObjects(Query<T> query) {
    List<? extends IdentifiableObject> objects;
    QueryPlan<T> plan = queryPlanner.planQuery(query);
    Query<T> dbQuery = plan.dbQuery();
    Query<T> memoryQuery = plan.memoryQuery();
    if (!memoryQuery.isEmpty()) {
      memoryQuery.setObjects(dbQueryEngine.query(dbQuery));
      objects = memoryQueryEngine.query(memoryQuery);
      return objects.size();
    }
    return dbQueryEngine.count(dbQuery);
  }

  private <T extends IdentifiableObject> List<T> queryObjects(Query<T> query) {
    List<T> objects = query.getObjects();

    if (objects != null) {
      objects = memoryQueryEngine.query(query.setObjects(objects));
      removeDefaultObject(query.getObjectType(), objects, query.getDefaults());
      return objects;
    }

    QueryPlan<T> queryPlan = queryPlanner.planQuery(query);
    Query<T> dbQuery = queryPlan.dbQuery();
    Query<T> memoryQuery = queryPlan.memoryQuery();

    objects = dbQueryEngine.query(dbQuery);

    if (!memoryQuery.isEmpty()) {
      memoryQuery.setObjects(objects);
      objects = memoryQueryEngine.query(memoryQuery);
    }

    removeDefaultObject(query.getObjectType(), objects, query.getDefaults());
    return objects;
  }

  private void removeDefaultObject(
      Class<?> klass, List<? extends IdentifiableObject> objects, Defaults defaults) {
    if (Defaults.INCLUDE == defaults || !Preheat.isDefaultClass(klass)) {
      return;
    }
    objects.removeIf(object -> "default".equals(object.getName()));
  }
}
