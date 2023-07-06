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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class DefaultQueryPlannerTest {

  private DefaultQueryPlanner subject;

  @Mock private SchemaService schemaService;

  @BeforeEach
  public void setUp() {
    this.subject = new DefaultQueryPlanner(schemaService);
  }

  @Test
  void verifyPlanQueryReturnsPersistedAndNotPersistedQueries() throws Exception {
    // Create schema with attributes
    final Attribute attribute = new Attribute();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, attribute, "id", true);
    addProperty(propertyMap, attribute, "uid", true);
    Schema schema = new OrganisationUnitSchemaDescriptor().getSchema();
    schema.setPropertyMap(propertyMap);

    // Add restriction
    Query query = Query.from(schema, Junction.Type.OR);
    query.add(Restrictions.eq("id", 100L));

    // method under test
    QueryPlan queryPlan = subject.planQuery(query, true);

    Query persistedQuery = queryPlan.getPersistedQuery();

    assertTrue(persistedQuery.isPlannedQuery());
    assertEquals(persistedQuery.getCriterions().size(), 1);
    assertEquals(persistedQuery.getFirstResult().intValue(), 0);
    assertEquals(persistedQuery.getMaxResults().intValue(), Integer.MAX_VALUE);
    assertEquals(persistedQuery.getRootJunctionType(), Junction.Type.OR);

    Query nonPersistedQuery = queryPlan.getNonPersistedQuery();
    assertEquals(nonPersistedQuery.getCriterions().size(), 0);
    assertTrue(nonPersistedQuery.isPlannedQuery());
  }

  /*
   * Verifies that when adding criteria on non-persisted fields and using OR
   * junction type the planner returns a "non Persisted Query" containing all
   * the criteria - since it will execute filter on the entire dataset from
   * the target table
   */
  @Test
  void verifyPlanQueryReturnsNonPersistedQueryWithCriterion() throws Exception {
    // Create schema with attributes
    final Attribute attribute = new Attribute();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, attribute, "id", true);
    addProperty(propertyMap, attribute, "uid", true);
    // note that this is a non-persisted attribute!
    addProperty(propertyMap, attribute, "name", false);
    Schema schema = new OrganisationUnitSchemaDescriptor().getSchema();
    schema.setPropertyMap(propertyMap);

    // Add restrictions on a non persisted field
    Query query = Query.from(schema, Junction.Type.OR);
    // adding a criterion on a non-persisted attribute
    query.add(Restrictions.eq("name", "test"));
    query.add(Restrictions.eq("id", 100));

    // method under test
    QueryPlan queryPlan = subject.planQuery(query, false);

    Query persistedQuery = queryPlan.getPersistedQuery();

    assertTrue(persistedQuery.isPlannedQuery());
    assertEquals(persistedQuery.getCriterions().size(), 0);
    assertEquals(persistedQuery.getFirstResult().intValue(), 0);
    assertEquals(persistedQuery.getMaxResults().intValue(), Integer.MAX_VALUE);
    assertEquals(persistedQuery.getRootJunctionType(), Junction.Type.AND);

    Query nonPersistedQuery = queryPlan.getNonPersistedQuery();
    assertEquals(nonPersistedQuery.getCriterions().size(), 2);
    assertTrue(nonPersistedQuery.isPlannedQuery());
    assertEquals(nonPersistedQuery.getRootJunctionType(), Junction.Type.OR);
  }

  @Test
  void verifyPlanQueryReturnsNonPersistedQueryWithCriterion2() throws Exception {
    // Create schema with attributes
    final Attribute attribute = new Attribute();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, attribute, "id", true);
    addProperty(propertyMap, attribute, "uid", true);
    addProperty(propertyMap, attribute, "name", true);
    Schema schema = new OrganisationUnitSchemaDescriptor().getSchema();
    schema.setPropertyMap(propertyMap);

    // Add restrictions on a non persisted field
    Query query = Query.from(schema, Junction.Type.AND);
    query.setMaxResults(10);
    query.setFirstResult(500);

    query.add(Restrictions.eq("name", "test"));
    query.add(Restrictions.eq("id", 100));

    // method under test
    QueryPlan queryPlan = subject.planQuery(query, false);

    Query persistedQuery = queryPlan.getPersistedQuery();

    assertTrue(persistedQuery.isPlannedQuery());
    assertEquals(persistedQuery.getCriterions().size(), 2);
    assertEquals(persistedQuery.getFirstResult().intValue(), 500);
    assertEquals(persistedQuery.getMaxResults().intValue(), 10);
    assertEquals(persistedQuery.getRootJunctionType(), Junction.Type.AND);

    Query nonPersistedQuery = queryPlan.getNonPersistedQuery();
    assertEquals(nonPersistedQuery.getCriterions().size(), 0);
    assertTrue(nonPersistedQuery.isPlannedQuery());
    assertEquals(nonPersistedQuery.getRootJunctionType(), Junction.Type.AND);
  }

  @Test
  void verifyPlanQueryWithPersistedAndNotPersistedCriteria() throws Exception {
    final DataElement dataElement = new DataElement();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, dataElement, "domainType", true);
    addProperty(propertyMap, dataElement, "groups", false);
    Schema schema = new DataElementSchemaDescriptor().getSchema();
    schema.setPropertyMap(propertyMap);

    // Add restriction
    Query query = Query.from(schema, Junction.Type.AND);
    query.add(Restrictions.eq("domainType", "Aggregate"));
    query.add(Restrictions.eq("groups", "dataElementGroupId"));

    // method under test
    QueryPlan queryPlan = subject.planQuery(query, false);

    Query persistedQuery = queryPlan.getPersistedQuery();

    assertTrue(persistedQuery.isPlannedQuery());
    assertEquals(1, persistedQuery.getCriterions().size());

    Query nonPersistedQuery = queryPlan.getNonPersistedQuery();
    assertTrue(nonPersistedQuery.isPlannedQuery());
    assertEquals(1, nonPersistedQuery.getCriterions().size());
  }

  private void addProperty(
      Map<String, Property> propertyMap, Object bean, String property, boolean persisted)
      throws Exception {
    PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, property);
    Property p = new Property(pd.getPropertyType(), pd.getReadMethod(), pd.getWriteMethod());
    p.setName(pd.getName());
    p.setReadable(true);
    p.setPersisted(persisted);

    propertyMap.put(pd.getName(), p);
  }
}
