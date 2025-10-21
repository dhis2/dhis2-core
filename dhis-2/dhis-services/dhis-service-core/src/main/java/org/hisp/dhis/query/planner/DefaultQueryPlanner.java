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

    QueryPlan<T> plan = split(query);
    Query<T> memoryQuery = plan.memoryQuery();
    Query<T> dbQuery = plan.dbQuery();

    // if there are any non persisted filters, leave the paging to the in-memory engine
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
      if (schema.hasPersistedProperty("name")) query.addOrder(Order.iasc("name"));
      if (schema.hasPersistedProperty("id")) query.addOrder(Order.asc("id"));
    }
    query.setShortNamePersisted(schema.hasPersistedProperty("shortName"));
  }

  private <T extends IdentifiableObject> QueryPlan<T> split(Query<T> query) {
    Query<T> memoryQuery = Query.copyOf(query);
    memoryQuery.getFilters().clear();
    Query<T> dbQuery = Query.emptyOf(query);

    for (Filter filter : query.getFilters()) {
      if (isDbFilter(query, filter)) {
        dbQuery.add(filter);
      } else {
        memoryQuery.add(filter);
      }
    }
    if (query.getRootJunctionType() == Junction.Type.OR
        && !memoryQuery.getFilters().isEmpty()
        && !dbQuery.getFilters().isEmpty()) {
      // OR with some filters DB some filters in-memory => all must be in-memory
      dbQuery.getFilters().clear();
      memoryQuery.getFilters().clear();
      memoryQuery.getFilters().addAll(query.getFilters());
    }

    Schema schema = schemaService.getDynamicSchema(query.getObjectType());
    boolean dbOrdering =
        query.getOrders().stream()
            .map(Order::getProperty)
            .map(schema::getProperty)
            .allMatch(p -> p != null && (p.isPersisted() || isDisplayProperty(p.getName())) && p.isSimple());
    if (dbOrdering) {
      dbQuery.addOrders(query.getOrders());
      memoryQuery.clearOrders();
    }

    return new QueryPlan<>(dbQuery, memoryQuery);
  }

  private boolean isDbFilter(Query<?> query, Filter filter) {
    if (filter.isVirtual()) return filter.isIdentifiable() || filter.isQuery();
    PropertyPath path = schemaService.getPropertyPath(query.getObjectType(), filter.getPath());
    path = overridePersistedFlag(path, filter);
    return path != null
        && path.isPersisted()
        && !path.haveAlias()
        && !Attribute.ObjectType.isValidType(path.getPath());
  }

  /**
   * Overrides the persisted flag for certain properties based on the filter.
   */
  private PropertyPath overridePersistedFlag(PropertyPath path, Filter filter) {
    if (isTranslationFilter(filter)) {
      return new PropertyPath(path.getProperty(), true, path.getAlias());
    }
    return path;
  }


  private boolean isTranslationFilter(Filter filter) {
    String path = filter.getPath();
    return path.startsWith("display");
  }

  private boolean isDisplayProperty(String propertyName) {
    return propertyName != null && propertyName.startsWith("display");
  }
}
