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
package org.hisp.dhis.query.planner;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@RequiredArgsConstructor
public class DefaultQueryPlanner implements QueryPlanner {

  private final SchemaService schemaService;

  @Override
  public <T extends IdentifiableObject> QueryPlan<T> planQuery(Query<T> query) {
    autoFill(query);

    // if only one filter, always set to Junction.Type AND
    if (query.getFilters().size() > 1 && Junction.Type.OR == query.getRootJunctionType()) {
      return new QueryPlan<>(Query.emptyOf(query), Query.copyOf(query));
    }

    QueryPlan<T> plan = split(query);
    Query<T> memoryQuery = plan.memoryQuery();
    Query<T> dbQuery = plan.dbQuery();

    // if there are any non persisted criterions left, we leave the paging
    // to the in-memory engine
    if (!memoryQuery.isEmpty()) {
      dbQuery.setSkipPaging(true);
    } else {
      dbQuery.setFirstResult(query.getFirstResult());
      dbQuery.setMaxResults(query.getMaxResults());
    }

    return plan;
  }

  /** Modifies the query based on schema data */
  private void autoFill(Query<?> query) {
    Schema schema = schemaService.getDynamicSchema(query.getObjectType());
    if (query.isDefaultOrders()) {
      if (schema.hasPersistedProperty("name"))
        query.addOrder(Order.iasc(schema.getPersistedProperty("name")));
      if (schema.hasPersistedProperty("id"))
        query.addOrder(Order.asc(schema.getPersistedProperty("id")));
    }
    query.setShortNamePersisted(schema.hasPersistedProperty("shortName"));
  }

  private <T extends IdentifiableObject> QueryPlan<T> split(Query<T> query) {
    Query<T> memoryQuery = Query.copyOf(query);
    memoryQuery.getFilters().clear();
    Query<T> dbQuery = Query.emptyOf(query);

    for (Filter filter : query.getFilters()) {
      if (!filter.isVirtual())
        filter.setPropertyPath(
            schemaService.getPropertyPath(query.getObjectType(), filter.getPath()));

      if (isDbFilter(filter)) {
        dbQuery.add(filter);
      } else {
        memoryQuery.add(filter);
      }
    }

    if (query.ordersPersisted()) {
      dbQuery.addOrders(query.getOrders());
      memoryQuery.clearOrders();
    }

    return new QueryPlan<>(dbQuery, memoryQuery);
  }

  private static boolean isDbFilter(Filter filter) {
    if (filter.isVirtual()) return filter.isIdentifiable() || filter.isQuery();
    PropertyPath path = filter.getPropertyPath();
    return path != null
        && path.isPersisted()
        && !path.haveAlias()
        && !Attribute.ObjectType.isValidType(path.getPath());
  }
}
