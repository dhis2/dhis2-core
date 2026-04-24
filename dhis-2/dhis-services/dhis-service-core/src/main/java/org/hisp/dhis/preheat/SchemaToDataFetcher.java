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
package org.hisp.dhis.preheat;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for fetching all the unique attributes for a {@link
 * IdentifiableObject} subclass.
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class SchemaToDataFetcher {

  /**
   * PostgreSQL's JDBC driver limits prepared statements to 65,535 bind parameters. We stay well
   * under that to leave room for other params and to match the partition size used elsewhere in the
   * preheat pipeline.
   */
  private static final int MAX_VALUES_PER_QUERY = 20_000;

  private static final String HQL_FILTERED_TEMPLATE = "SELECT :fields from :entity WHERE :filter";

  private final EntityManager entityManager;

  public SchemaToDataFetcher(EntityManager entityManager) {
    checkNotNull(entityManager);

    this.entityManager = entityManager;
  }

  /**
   * Executes a read-only query for the given Schema class and fetches only the fields marked as
   * "unique", filtered to only include records that have matching unique property values from the
   * objects being imported.
   *
   * <p>This avoids loading ALL records when only checking uniqueness for specific values.
   *
   * @param schema a {@link Schema}
   * @param objectsBeingImported the objects being imported, used to filter the query. If empty,
   *     returns empty list (no uniqueness conflicts possible with nothing being imported).
   * @return a List of objects corresponding to the "klass" of the given Schema
   */
  public List<? extends IdentifiableObject> fetch(
      Schema schema, Collection<? extends IdentifiableObject> objectsBeingImported) {
    if (schema == null) {
      return List.of();
    }

    return mapUniqueFields(schema, objectsBeingImported);
  }

  @SuppressWarnings("unchecked")
  private List<? extends IdentifiableObject> mapUniqueFields(
      Schema schema, Collection<? extends IdentifiableObject> objectsBeingImported) {
    List<Property> uniqueProperties = schema.getUniqueProperties();
    if (uniqueProperties.isEmpty()) {
      return List.of();
    }

    if (objectsBeingImported.isEmpty()) {
      return List.of();
    }

    Map<String, Set<Object>> valuesToCheck =
        extractValuesToCheck(uniqueProperties, objectsBeingImported);

    int totalValues = valuesToCheck.values().stream().mapToInt(Set::size).sum();
    if (totalValues == 0) {
      return List.of();
    }

    // Fast path: single query when under the parameter limit
    if (totalValues <= MAX_VALUES_PER_QUERY) {
      Query query = createQuery(schema, uniqueProperties, valuesToCheck);
      if (query == null) {
        return List.of();
      }
      List<Object> objects = query.getResultList();
      return uniqueProperties.size() == 1
          ? handleSingleColumn(objects, uniqueProperties, schema)
          : handleMultipleColumn((List<Object[]>) (List<?>) objects, uniqueProperties, schema);
    }

    // Batched path: run one query per unique property, partitioning each property's values
    // into chunks that stay under the parameter limit. Results are deduplicated because a row
    // matching multiple unique properties can be returned by more than one query.
    return runBatchedQueries(schema, uniqueProperties, valuesToCheck);
  }

  @SuppressWarnings("unchecked")
  private List<? extends IdentifiableObject> runBatchedQueries(
      Schema schema, List<Property> uniqueProperties, Map<String, Set<Object>> valuesToCheck) {
    String fields = extractUniqueFields(uniqueProperties);
    String entity = schema.getKlass().getSimpleName();
    boolean singleColumn = uniqueProperties.size() == 1;

    Set<Object> singleColumnResults = new LinkedHashSet<>();
    Set<List<Object>> multiColumnResults = new LinkedHashSet<>();

    for (Property property : uniqueProperties) {
      Set<Object> values = valuesToCheck.get(property.getFieldName());
      if (values == null || values.isEmpty()) {
        continue;
      }

      String hql =
          "SELECT %s FROM %s WHERE %s IN (:batch)"
              .formatted(fields, entity, property.getFieldName());

      for (List<Object> batch : Lists.partition(new ArrayList<>(values), MAX_VALUES_PER_QUERY)) {
        Query query = entityManager.createQuery(hql).setHint(QueryHints.HINT_READONLY, true);
        query.setParameter("batch", batch);
        List<Object> rows = query.getResultList();
        if (singleColumn) {
          singleColumnResults.addAll(rows);
        } else {
          for (Object row : rows) {
            multiColumnResults.add(Arrays.asList((Object[]) row));
          }
        }
      }
    }

    if (singleColumn) {
      return handleSingleColumn(new ArrayList<>(singleColumnResults), uniqueProperties, schema);
    }
    List<Object[]> dedupedRows = multiColumnResults.stream().map(List::toArray).toList();
    return handleMultipleColumn(dedupedRows, uniqueProperties, schema);
  }

  private Query createQuery(
      Schema schema, List<Property> uniqueProperties, Map<String, Set<Object>> valuesToCheck) {
    String fields = extractUniqueFields(uniqueProperties);
    String entity = schema.getKlass().getSimpleName();

    String whereClause = buildWhereClause(uniqueProperties, valuesToCheck);
    if (whereClause.isEmpty()) {
      return null; // No unique property values - no conflicts possible
    }

    String hql =
        HQL_FILTERED_TEMPLATE
            .replace(":fields", fields)
            .replace(":entity", entity)
            .replace(":filter", whereClause);
    Query query = entityManager.createQuery(hql).setHint(QueryHints.HINT_READONLY, true);
    setQueryParameters(query, uniqueProperties, valuesToCheck);
    return query;
  }

  private void setQueryParameters(
      Query query, List<Property> uniqueProperties, Map<String, Set<Object>> valuesToCheck) {
    for (Property property : uniqueProperties) {
      String fieldName = property.getFieldName();
      Set<Object> values = valuesToCheck.get(fieldName);
      if (values != null && !values.isEmpty()) {
        query.setParameter(fieldName + "Values", values);
      }
    }
  }

  /** Extracts the unique property values from the objects being imported. */
  private Map<String, Set<Object>> extractValuesToCheck(
      List<Property> uniqueProperties,
      Collection<? extends IdentifiableObject> objectsBeingImported) {
    Map<String, Set<Object>> valuesToCheck = new HashMap<>();

    for (Property property : uniqueProperties) {
      valuesToCheck.put(property.getFieldName(), new HashSet<>());
    }

    for (IdentifiableObject object : objectsBeingImported) {
      for (Property property : uniqueProperties) {
        try {
          Object value = PropertyUtils.getProperty(object, property.getFieldName());
          if (value != null) {
            valuesToCheck.get(property.getFieldName()).add(value);
          }
        } catch (Exception e) {
          log.debug(
              "Could not extract property {} from object: {}",
              property.getFieldName(),
              e.getMessage());
        }
      }
    }

    return valuesToCheck;
  }

  /**
   * Builds a WHERE clause that filters by the unique property values being imported. Returns empty
   * string if no values to check.
   */
  private String buildWhereClause(
      List<Property> uniqueProperties, Map<String, Set<Object>> valuesToCheck) {
    List<String> conditions = new ArrayList<>();

    for (Property property : uniqueProperties) {
      String fieldName = property.getFieldName();
      Set<Object> values = valuesToCheck.get(fieldName);
      if (values != null && !values.isEmpty()) {
        conditions.add(fieldName + " IN (:" + fieldName + "Values)");
      }
    }

    // Use OR because we want to find ANY existing record that conflicts with ANY unique value
    return String.join(" OR ", conditions);
  }

  private List<IdentifiableObject> handleMultipleColumn(
      List<Object[]> objects, List<Property> uniqueProperties, Schema schema) {
    List<IdentifiableObject> resultsObjects = new ArrayList<>(objects.size());

    for (Object[] uniqueValuesArray : objects) {
      Map<String, Object> valuesMap = new HashMap<>();

      for (int i = 0; i < uniqueValuesArray.length; i++) {
        valuesMap.put(uniqueProperties.get(i).getFieldName(), uniqueValuesArray[i]);
      }

      addToResult(schema, valuesMap, resultsObjects);
    }

    return resultsObjects;
  }

  private List<IdentifiableObject> handleSingleColumn(
      List<Object> objects, List<Property> uniqueProperties, Schema schema) {
    List<IdentifiableObject> resultsObjects = new ArrayList<>(objects.size());
    for (Object uniqueValue : objects) {
      Map<String, Object> valuesMap = new HashMap<>();
      valuesMap.put(uniqueProperties.get(0).getFieldName(), uniqueValue);

      addToResult(schema, valuesMap, resultsObjects);
    }

    return resultsObjects;
  }

  private void addToResult(
      Schema schema, Map<String, Object> valuesMap, List<IdentifiableObject> resultsObjects) {
    try {
      IdentifiableObject identifiableObject =
          (IdentifiableObject) schema.getKlass().getDeclaredConstructor().newInstance();
      BeanUtils.populate(identifiableObject, valuesMap);
      resultsObjects.add(identifiableObject);
    } catch (ReflectiveOperationException e) {
      log.error(
          "Error during dynamic population of object type: " + schema.getKlass().getSimpleName(),
          e);
    }
  }

  private String extractUniqueFields(List<Property> uniqueProperties) {
    return uniqueProperties.stream().map(Property::getFieldName).collect(Collectors.joining(","));
  }
}
