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

  /**
   * Lifts eligible collection identifier filters (e.g. {@code organisationUnits.id:in:[...]} or
   * {@code ...:eq:...}) out of the generic filter list and translates them to SQL-level EXISTS
   * predicates. This prevents in-memory fallback and preserves database pagination for these
   * filters.
   *
   * @return {@code true} when the filter was translated and should not be processed further
   */
  private <T extends IdentifiableObject> boolean translateCollectionIdFilterToExists(
      Query<T> query, Filter filter, Query<T> dbQuery) {
    PropertyPath path = resolveTranslatableCollectionIdPath(query, filter);
    if (path == null) return false;

    Collection<?> values = filter.getOperator().getArgs();
    if (values.isEmpty()) {
      return addAlwaysFalsePredicate(dbQuery);
    }

    dbQuery.addPredicateSupplier(
        new CollectionIdExistsPredicateSupplier(
            query.getObjectType(), path.getAlias(), path.getPath(), values));
    return true;
  }

  /**
   * Determines if the filter is an eligible collection identifier filter and resolves its property
   * path.
   *
   * @param query the query containing the filter
   * @param filter the filter to evaluate
   * @return the resolved property path if the filter is an eligible collection identifier filter,
   *     or {@code null} otherwise
   * @param <T> the type of identifiable objects being queried
   */
  private <T extends IdentifiableObject> PropertyPath resolveTranslatableCollectionIdPath(
      Query<T> query, Filter filter) {
    if (!isEligibleCollectionIdFilter(query, filter)) return null;

    PropertyPath path = schemaService.getPropertyPath(query.getObjectType(), filter.getPath());
    if (path == null || !path.isPersisted() || !path.haveAlias()) return null;

    return isCollectionIdPath(query.getObjectType(), filter.getPath(), path.getProperty())
        ? path
        : null;
  }

  /**
   * Checks if the filter is a non-virtual, non-attribute filter on an "id" property of a collection
   * path, with a supported operator.
   *
   * @param query the query containing the filter
   * @param filter the filter to evaluate
   * @return {@code true} if the filter is an eligible collection identifier filter, {@code false}
   *     otherwise
   */
  private boolean isEligibleCollectionIdFilter(Query<?> query, Filter filter) {
    if (query.getRootJunctionType() != Junction.Type.AND) return false;
    if (filter.isVirtual() || filter.isAttribute()) return false;
    return isCollectionIdSupportedOperator(filter.getOperator());
  }

  /**
   * Checks if the operator is supported for collection identifier filters. Currently only supports
   * equality and IN operators.
   *
   * @param operator the operator to evaluate
   * @return {@code true} if the operator is supported for collection identifier filters, {@code
   *     false} otherwise
   */
  private boolean isCollectionIdSupportedOperator(Operator<?> operator) {
    return operator instanceof EqualOperator<?> || operator instanceof InOperator<?>;
  }

  /**
   * Adds a predicate that always evaluates to false to the query. This is used to handle cases
   * where a collection identifier filter has an empty value set, which should result in no matches.
   *
   * @param dbQuery the query to which the predicate should be added
   * @return {@code true} to indicate that the filter was handled and no further processing is
   *     needed
   * @param <T> the type of identifiable objects being queried
   */
  private <T extends IdentifiableObject> boolean addAlwaysFalsePredicate(Query<T> dbQuery) {
    dbQuery.addPredicateSupplier(
        new JpaPredicateSupplier() {
          @Override
          public <R> Predicate getPredicate(
              jakarta.persistence.criteria.CriteriaBuilder builder,
              Root<R> root,
              jakarta.persistence.criteria.CriteriaQuery<?> criteriaQuery) {
            return builder.disjunction();
          }
        });
    return true;
  }

  /*
    * Checks if the filter path corresponds to an "id" property of a collection path. This is done by
    * verifying that the terminal property is named "id", that the path has at least one traversal, and
    that at least one traversal in the path is a collection. This ensures that we only translate
    filters that target identifiers of collection relationships, which can be safely translated to
    * EXISTS predicates without risking unintended consequences on non-collection relationships or
    * simple properties.
    *
    * @param rootType the root type of the query
    * @param filterPath the filter path to evaluate
    * @param terminalProperty the terminal property of the filter path
    * @return {@code true} if the filter path corresponds to an "id" property of a collection
    * path {@code false} otherwise
   */
  private boolean isCollectionIdPath(
      Class<?> rootType, String filterPath, Property terminalProperty) {
    if (!isTerminalIdProperty(terminalProperty)) return false;

    String[] components = filterPath.split("\\.");
    if (!hasPathTraversal(components)) return false;

    return hasCollectionTraversal(rootType, components);
  }

  /**
   * Returns whether the terminal property is `id`, which is required for this
   * collection identifier translation pattern (for example
   * `collectionProperty.id:in:[...]`).
   *
   * Other terminal properties are intentionally not translated and continue
   * through the existing planner path.
   * @param terminalProperty the terminal property of the filter path
   * @return {@code true} if the terminal property is named "id", {@code false} otherwise
   */
  private boolean isTerminalIdProperty(Property terminalProperty) {
    return terminalProperty != null && "id".equals(terminalProperty.getName());
  }

  /**
   * Checks if the filter path has at least one traversal (i.e. contains at least one dot). This is
   * required for this collection identifier translation pattern, as we are only targeting filters on
   * collection relationships (e.g. `collectionProperty.id`), and we want to exclude simple properties
   * named "id" on the root object (e.g. `id`
   * @param components the components of the filter path, split by dot
   * @return {@code true} if the filter path has at least one traversal, {@code false} otherwise
   */
  private boolean hasPathTraversal(String[] components) {
    return components.length >= 2;
  }

  /**
   * Returns whether the non-terminal path contains at least one collection traversal.
   *
   * <p>While traversing schema properties, invalid segments, embedded objects, or
   * non-traversable/simple segments return {@code false}. A path is eligible only
   * if traversal succeeds and at least one collection property is encountered.
   *
   * @param rootType root query type
   * @param components filter path split by `.`
   * @return {@code true} when the path traverses at least one collection
   */
  private boolean hasCollectionTraversal(Class<?> rootType, String[] components) {
    Schema schema = schemaService.getSchema(rootType);
    boolean hasCollection = false;

    for (int i = 0; i < components.length - 1; i++) {
      Property property = schema.getProperty(components[i]);
      if (property == null || property.isEmbeddedObject()) return false;

      if (property.isCollection()) {
        if (property.getItemKlass() == null) return false;
        hasCollection = true;
        schema = schemaService.getSchema(property.getItemKlass());
        continue;
      }

      if (property.isSimple() || property.getKlass() == null) return false;
      schema = schemaService.getSchema(property.getKlass());
    }

    return hasCollection;
  }

  /**
   * Returns whether a filter can be executed in the DB query plan.
   *
   * <p>Virtual filters are DB-eligible only for `identifiable` and `query`. For
   * regular filters, the path must resolve to a persisted property and not require
   * in-memory alias traversal (for example collection/embedded paths handled by
   * {@code pathRequiresInMemoryFiltering(...)}).
   *
   * @param query query containing the filter
   * @param filter filter to evaluate
   * @return {@code true} when the filter is DB-eligible, {@code false} otherwise
   */
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
   * Returns whether an aliased path should remain in in-memory filtering.
   *
   * <p>This includes:
   * <ul>
   *   <li>Collection paths - require JOINs which our simple get() chaining doesn't support
   *   <li>Embedded object paths - JPA navigation through embedded objects followed by relationships
   *       can be problematic
   * </ul>
   *
   * <p>This planner step is conservative: collection and embedded-object traversals
   * are treated as in-memory here. Collection identifier filters that are eligible
   * for SQL translation are handled earlier by
   * {@code translateCollectionIdFilterToExists(...)} and therefore do not reach this method.
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
