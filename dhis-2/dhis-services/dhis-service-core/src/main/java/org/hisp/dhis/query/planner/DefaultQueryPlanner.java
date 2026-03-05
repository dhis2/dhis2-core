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

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.JpaPredicateSupplier;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.InOperator;
import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.schema.Property;
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
    Schema schema = schemaService.getSchema(query.getObjectType());
    if (query.isDefaultOrders()) {
      if (schema.hasPersistedProperty("name")) query.addOrder(Order.iasc("name"));
      if (schema.hasPersistedProperty("id")) query.addOrder(Order.asc("id"));
    }
    query.setShortNamePersisted(schema.hasPersistedProperty("shortName"));
  }

  private <T extends IdentifiableObject> void categorizeAliasedFilters(
      Query<T> query,
      Filter filter,
      Query<T> dbQuery,
      List<Filter> aliasedDbFilters,
      Set<String> distinctRootAliases) {
    PropertyPath path = schemaService.getPropertyPath(query.getObjectType(), filter.getPath());
    if (path != null && path.haveAlias()) {
      aliasedDbFilters.add(filter);
      distinctRootAliases.add(path.getAlias()[0]);
    } else {
      dbQuery.add(filter);
    }
  }

  private <T extends IdentifiableObject> QueryPlan<T> split(Query<T> query) {
    Query<T> memoryQuery = Query.copyOf(query);
    memoryQuery.getFilters().clear();
    Query<T> dbQuery = Query.emptyOf(query);

    List<Filter> aliasedDbFilters = new ArrayList<>();
    Set<String> distinctRootAliases = new HashSet<>();

    for (Filter filter : query.getFilters()) {
      if (translateCollectionIdFilterToExists(query, filter, dbQuery)) {
        continue;
      }

      if (!isDbFilter(query, filter)) {
        memoryQuery.add(filter);
        continue;
      }

      if (filter.isVirtual()) {
        dbQuery.add(filter);
        continue;
      }

      categorizeAliasedFilters(query, filter, dbQuery, aliasedDbFilters, distinctRootAliases);
    }

    // Handle aliased filters: if there are multiple distinct aliases, fall back to in-memory
    // to avoid potential issues with multiple implicit JPA joins
    if (distinctRootAliases.size() > 1) {
      memoryQuery.getFilters().addAll(aliasedDbFilters);
    } else {
      dbQuery.getFilters().addAll(aliasedDbFilters);
    }

    if (query.getRootJunctionType() == Junction.Type.OR
        && !memoryQuery.getFilters().isEmpty()
        && !dbQuery.getFilters().isEmpty()) {
      // OR with some filters DB some filters in-memory => all must be in-memory
      dbQuery.getFilters().clear();
      memoryQuery.getFilters().clear();
      memoryQuery.getFilters().addAll(query.getFilters());
    }

    Schema schema = schemaService.getSchema(query.getObjectType());
    boolean dbOrdering =
        query.getOrders().stream()
            .map(Order::getProperty)
            .map(schema::getProperty)
            .allMatch(
                p ->
                    p != null
                        && (p.isPersisted() || isDisplayProperty(p.getName()))
                        && p.isSimple());
    if (dbOrdering) {
      dbQuery.addOrders(query.getOrders());
      memoryQuery.clearOrders();
    }

    return new QueryPlan<>(dbQuery, memoryQuery);
  }

  private <T extends IdentifiableObject> boolean translateCollectionIdFilterToExists(
      Query<T> query, Filter filter, Query<T> dbQuery) {
    if (query.getRootJunctionType() != Junction.Type.AND
        || filter.isVirtual()
        || filter.isAttribute()) {
      return false;
    }

    Operator<?> operator = filter.getOperator();
    if (!(operator instanceof EqualOperator<?> || operator instanceof InOperator<?>)) {
      return false;
    }

    PropertyPath path = schemaService.getPropertyPath(query.getObjectType(), filter.getPath());
    if (path == null || !path.isPersisted() || !path.haveAlias()) {
      return false;
    }

    if (!isCollectionIdPath(query.getObjectType(), filter.getPath(), path.getProperty())) {
      return false;
    }

    Collection<?> values = operator.getArgs();
    if (values.isEmpty()) {
      dbQuery.addPredicateSupplier(
          new JpaPredicateSupplier() {
            @Override
            public <T> Predicate getPredicate(
                jakarta.persistence.criteria.CriteriaBuilder builder,
                Root<T> root,
                CriteriaQuery<?> criteriaQuery) {
              return builder.disjunction();
            }
          });
      return true;
    }

    dbQuery.addPredicateSupplier(
        existsCollectionPropertyPredicate(
            query.getObjectType(), path.getAlias(), path.getPath(), values));
    return true;
  }

  private boolean isCollectionIdPath(
      Class<?> rootType, String filterPath, Property terminalProperty) {
    if (terminalProperty == null || !"id".equals(terminalProperty.getName())) {
      return false;
    }

    String[] components = filterPath.split("\\.");
    if (components.length < 2) {
      return false;
    }

    Schema schema = schemaService.getSchema(rootType);
    boolean hasCollection = false;

    for (int i = 0; i < components.length - 1; i++) {
      Property property = schema.getProperty(components[i]);
      if (property == null || property.isEmbeddedObject()) {
        return false;
      }

      if (property.isCollection()) {
        if (property.getItemKlass() == null) {
          return false;
        }
        hasCollection = true;
        schema = schemaService.getSchema(property.getItemKlass());
      } else if (!property.isSimple()) {
        if (property.getKlass() == null) {
          return false;
        }
        schema = schemaService.getSchema(property.getKlass());
      } else {
        return false;
      }
    }

    return hasCollection;
  }

  private JpaPredicateSupplier existsCollectionPropertyPredicate(
      Class<?> objectType, String[] aliases, String terminalPath, Collection<?> values) {
    String terminalField = terminalPath.substring(terminalPath.lastIndexOf('.') + 1);
    return new JpaPredicateSupplier() {
      @Override
      @SuppressWarnings({"rawtypes", "unchecked"})
      public <T> Predicate getPredicate(
          jakarta.persistence.criteria.CriteriaBuilder builder,
          Root<T> root,
          CriteriaQuery<?> criteriaQuery) {
        var subquery = criteriaQuery.subquery(Integer.class);
        Root<?> subRoot = subquery.from((Class) objectType);
        From<?, ?> joined = subRoot;
        for (String alias : aliases) {
          joined = joined.join(alias);
        }
        subquery.select(builder.literal(1));
        subquery.where(
            builder.equal(subRoot.get("id"), root.get("id")), joined.get(terminalField).in(values));
        return builder.exists(subquery);
      }
    };
  }

  private boolean isDbFilter(Query<?> query, Filter filter) {
    if (filter.isVirtual()) return filter.isIdentifiable() || filter.isQuery();
    PropertyPath path = schemaService.getPropertyPath(query.getObjectType(), filter.getPath());
    if (path == null || !path.isPersisted()) return false;
    if (Attribute.ObjectType.isValidType(path.getPath())) return false;

    if (path.haveAlias()) {
      if (pathRequiresInMemoryFiltering(query.getObjectType(), path.getAlias())) {
        return false;
      }
      return path.getProperty().isSimple();
    }
    return true;
  }

  /**
   * Checks if any property in the alias path requires in-memory filtering. This includes:
   *
   * <ul>
   *   <li>Collection paths - require JOINs which our simple get() chaining doesn't support
   *   <li>Embedded object paths - JPA navigation through embedded objects followed by relationships
   *       can be problematic
   * </ul>
   */
  private boolean pathRequiresInMemoryFiltering(Class<?> klass, String[] aliases) {
    Schema schema = schemaService.getSchema(klass);
    for (String alias : aliases) {
      var property = schema.getProperty(alias);
      if (property == null) return true; // Unknown property, fall back to in-memory
      if (property.isCollection()) return true;
      if (property.isEmbeddedObject()) return true;
      // Navigate to next schema for non-collection relationships
      schema = schemaService.getSchema(property.getKlass());
    }
    return false;
  }

  private boolean isDisplayProperty(String propertyName) {
    return propertyName != null && propertyName.startsWith("display");
  }
}
