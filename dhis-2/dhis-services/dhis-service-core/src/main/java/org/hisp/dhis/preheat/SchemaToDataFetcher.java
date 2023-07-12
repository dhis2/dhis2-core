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
package org.hisp.dhis.preheat;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.SessionFactory;
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
  @PersistenceContext private EntityManager entityManager;

  private SessionFactory sessionFactory;

  public SchemaToDataFetcher(SessionFactory sessionFactory) {
    checkNotNull(sessionFactory);

    this.sessionFactory = sessionFactory;
  }

  /**
   * Executes a read-only query for the given Schema class and fetches only the fields marked as
   * "unique".
   *
   * @param schema a {@link Schema}
   * @return a List of objects corresponding to the "klass" of the given Schema
   */
  public List<? extends IdentifiableObject> fetch(Schema schema) {
    if (schema == null) {
      return Collections.emptyList();
    }

    return mapUniqueFields(schema);
  }

  @SuppressWarnings("unchecked")
  private List<? extends IdentifiableObject> mapUniqueFields(Schema schema) {
    List<Property> uniqueProperties = schema.getUniqueProperties();

    List objects = new ArrayList();

    if (!uniqueProperties.isEmpty()) {
      final String fields = extractUniqueFields(uniqueProperties);

      objects =
          entityManager
              .createQuery("SELECT " + fields + " from " + schema.getKlass().getSimpleName())
              .setHint(QueryHints.HINT_READONLY, true)
              .getResultList();
    }

    // Hibernate returns a List containing an array of Objects if multiple
    // columns are used in the query
    // or a "simple" List if only one columns is used in the query
    return uniqueProperties.size() == 1
        ? handleSingleColumn(objects, uniqueProperties, schema)
        : handleMultipleColumn(objects, uniqueProperties, schema);
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
      IdentifiableObject identifiableObject = (IdentifiableObject) schema.getKlass().newInstance();
      BeanUtils.populate(identifiableObject, valuesMap);
      resultsObjects.add(identifiableObject);
    } catch (Exception e) {
      log.error(
          "Error during dynamic population of object type: " + schema.getKlass().getSimpleName(),
          e);
    }
  }

  private String extractUniqueFields(List<Property> uniqueProperties) {
    return uniqueProperties.stream().map(Property::getFieldName).collect(Collectors.joining(","));
  }
}
