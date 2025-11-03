/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.query.Filters;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link DefaultQueryPlanner}.
 *
 * @author Viet Nguyen
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultQueryPlannerTest {

  @Mock private SchemaService schemaService;

  private DefaultQueryPlanner queryPlanner;

  @BeforeEach
  void setUp() {
    queryPlanner = new DefaultQueryPlanner(schemaService);
  }

  @Test
  void testDisplayNameFilterResultsInDatabaseQuery() {
    // Given: A query with a filter on displayName (a translatable property)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.like("displayName", "Health", MatchMode.ANYWHERE));

    // Mock schema to indicate displayName is persisted and translatable
    Schema schema = mockSchema();
    Property displayNameProperty = mockTranslatableProperty("displayName", "NAME");
    when(schema.getProperty("displayName")).thenReturn(displayNameProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    PropertyPath displayNamePath = new PropertyPath(displayNameProperty, true);
    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(DataElement.class, eq("displayName")))
        .thenReturn(displayNamePath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query, not the memory query
    assertEquals(1, plan.dbQuery().getFilters().size(), "displayName filter should be in DB query");
    assertEquals(
        0,
        plan.memoryQuery().getFilters().size(),
        "displayName filter should NOT be in memory query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  @Test
  void testDisplayDescriptionFilterResultsInDatabaseQuery() {
    // Given: A query with a filter on displayDescription (a translatable property)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.like("displayDescription", "program", MatchMode.ANYWHERE));

    // Mock schema to indicate displayDescription is persisted and translatable
    Schema schema = mockSchema();
    Property displayDescriptionProperty =
        mockTranslatableProperty("displayDescription", "DESCRIPTION");
    when(schema.getProperty("displayDescription")).thenReturn(displayDescriptionProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    PropertyPath displayDescriptionPath = new PropertyPath(displayDescriptionProperty, true);
    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(DataElement.class, eq("displayDescription")))
        .thenReturn(displayDescriptionPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query
    assertEquals(
        1, plan.dbQuery().getFilters().size(), "displayDescription filter should be in DB query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  @Test
  void testDisplayShortNameFilterResultsInDatabaseQuery() {
    // Given: A query with a filter on displayShortName (a translatable property)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.like("displayShortName", "ANC", MatchMode.ANYWHERE));

    // Mock schema to indicate displayShortName is persisted and translatable
    Schema schema = mockSchema();
    Property displayShortNameProperty = mockTranslatableProperty("displayShortName", "SHORT_NAME");
    when(schema.getProperty("displayShortName")).thenReturn(displayShortNameProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    PropertyPath displayShortNamePath = new PropertyPath(displayShortNameProperty, true);
    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(DataElement.class, eq("displayShortName")))
        .thenReturn(displayShortNamePath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query
    assertEquals(
        1, plan.dbQuery().getFilters().size(), "displayShortName filter should be in DB query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  @Test
  void testDisplayNameOrderingResultsInDatabaseOrdering() {
    // Given: A query with ordering on displayName
    Query<DataElement> query = Query.of(DataElement.class);
    query.addOrder(Order.asc("displayName"));

    // Mock schema
    Schema schema = mockSchema();
    Property displayNameProperty = mockTranslatableProperty("displayName", "NAME");
    when(schema.getProperty("displayName")).thenReturn(displayNameProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Ordering should be in the database query
    assertEquals(
        1, plan.dbQuery().getOrders().size(), "displayName ordering should be in DB query");
    assertEquals(
        0,
        plan.memoryQuery().getOrders().size(),
        "displayName ordering should NOT be in memory query");
  }

  @Test
  void testMixedPersistedAndNonPersistedFilters() {
    // Given: A query with both displayName (persisted) and a non-persisted property filter
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.like("displayName", "Health", MatchMode.ANYWHERE));
    query.add(Filters.eq("customProperty", "value")); // Non-persisted property

    // Mock schema
    Schema schema = mockSchema();
    Property displayNameProperty = mockTranslatableProperty("displayName", "NAME");
    Property customProperty = mockNonPersistedProperty("customProperty");

    when(schema.getProperty("displayName")).thenReturn(displayNameProperty);
    when(schema.getProperty("customProperty")).thenReturn(customProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    PropertyPath displayNamePath = new PropertyPath(displayNameProperty, true);
    PropertyPath customPropertyPath = new PropertyPath(customProperty, false);

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(DataElement.class, eq("displayName")))
        .thenReturn(displayNamePath);
    when(schemaService.getPropertyPath(DataElement.class, eq("customProperty")))
        .thenReturn(customPropertyPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: displayName filter in DB query, customProperty filter in memory query
    assertEquals(1, plan.dbQuery().getFilters().size(), "DB query should have 1 filter");
    assertEquals(1, plan.memoryQuery().getFilters().size(), "Memory query should have 1 filter");
    assertFalse(plan.memoryQuery().isEmpty(), "Memory query should NOT be empty");
  }

  @Test
  void testNonPersistedPropertyFilterResultsInMemoryQuery() {
    // Given: A query with a non-persisted property
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("nonPersistedProperty", "value"));

    // Mock schema
    Schema schema = mockSchema();
    Property nonPersistedProperty = mockNonPersistedProperty("nonPersistedProperty");
    when(schema.getProperty("nonPersistedProperty")).thenReturn(nonPersistedProperty);
    when(schema.hasPersistedProperty("name")).thenReturn(true);
    when(schema.hasPersistedProperty("id")).thenReturn(true);

    PropertyPath nonPersistedPath = new PropertyPath(nonPersistedProperty, false);
    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(DataElement.class, eq("nonPersistedProperty")))
        .thenReturn(nonPersistedPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Filter should be in memory query, not DB query
    assertEquals(0, plan.dbQuery().getFilters().size(), "DB query should have no filters");
    assertEquals(1, plan.memoryQuery().getFilters().size(), "Memory query should have 1 filter");
    assertFalse(plan.memoryQuery().isEmpty(), "Memory query should NOT be empty");
  }

  // Helper methods to create mock objects

  private Schema mockSchema() {
    Schema schema = mock(Schema.class);
    when(schema.hasPersistedProperty(any())).thenReturn(false);
    return schema;
  }

  private Property mockTranslatableProperty(String name, String translationKey) {
    Property property = new Property();
    property.setName(name);
    property.setFieldName(name);
    property.setPersisted(true);
    property.setSimple(true);
    property.setTranslatable(true);
    property.setTranslationKey(translationKey);
    return property;
  }

  private Property mockNonPersistedProperty(String name) {
    Property property = new Property();
    property.setName(name);
    property.setFieldName(name);
    property.setPersisted(false);
    property.setSimple(true);
    property.setTranslatable(false);
    return property;
  }
}
