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
    when(schemaService.getPropertyPath(DataElement.class, "displayName"))
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
    when(schemaService.getPropertyPath(DataElement.class, "displayDescription"))
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
    when(schemaService.getPropertyPath(DataElement.class, "displayShortName"))
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
    when(schemaService.getPropertyPath(DataElement.class, "displayName"))
        .thenReturn(displayNamePath);
    when(schemaService.getPropertyPath(DataElement.class, "customProperty"))
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
    when(schemaService.getPropertyPath(DataElement.class, "nonPersistedProperty"))
        .thenReturn(nonPersistedPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Filter should be in memory query, not DB query
    assertEquals(0, plan.dbQuery().getFilters().size(), "DB query should have no filters");
    assertEquals(1, plan.memoryQuery().getFilters().size(), "Memory query should have 1 filter");
    assertFalse(plan.memoryQuery().isEmpty(), "Memory query should NOT be empty");
  }

  // -------------------------------------------------------------------------
  // Tests for multiple nested/aliased path filters
  // These tests verify that multiple filters on many-to-one relationships
  // are correctly planned for database vs in-memory execution.
  // -------------------------------------------------------------------------

  /**
   * Tests that multiple filters on the SAME many-to-one path (e.g., parent.id AND parent.name) go
   * to the database query.
   */
  @Test
  void testMultipleFiltersOnSameManyToOnePathGoToDb() {
    // Given: Two filters on the same nested path (parent.id and parent.name)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("parent.id", "abc123"));
    query.add(Filters.like("parent.name", "Alpha", MatchMode.ANYWHERE));

    // Mock schema for DataElement with parent relationship
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property parentProperty = mockManyToOneRelationshipProperty("parent", DataElement.class);
    when(dataElementSchema.getProperty("parent")).thenReturn(parentProperty);

    // Mock parent schema
    Schema parentSchema = mock(Schema.class);
    Property idProperty = mockSimplePersistedProperty("id");
    Property nameProperty = mockSimplePersistedProperty("name");
    when(parentSchema.getProperty("id")).thenReturn(idProperty);
    when(parentSchema.getProperty("name")).thenReturn(nameProperty);

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for "parent.id"
    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.id")).thenReturn(parentIdPath);

    // PropertyPath for "parent.name"
    PropertyPath parentNamePath = new PropertyPath(nameProperty, true, new String[] {"parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.name"))
        .thenReturn(parentNamePath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Both filters should be in the database query
    assertEquals(
        2, plan.dbQuery().getFilters().size(), "Both parent.id and parent.name should be in DB");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  /**
   * Tests that multiple filters on DIFFERENT many-to-one paths (e.g., parent.id AND createdBy.id)
   * fall back to in-memory filtering to avoid potential JPA join issues.
   *
   * <p>This is a conservative approach: when there are multiple distinct root aliases (like
   * "parent" and "createdBy"), all aliased filters are moved to in-memory to avoid potential issues
   * with multiple implicit JPA joins.
   */
  @Test
  void testMultipleFiltersOnDifferentManyToOnePathsFallBackToInMemory() {
    // Given: Two filters on different nested paths (different root aliases)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("parent.id", "abc123"));
    query.add(Filters.eq("createdBy.id", "xyz789"));

    // Mock schema for DataElement
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property parentProperty = mockManyToOneRelationshipProperty("parent", DataElement.class);
    Property createdByProperty = mockManyToOneRelationshipProperty("createdBy", Object.class);
    when(dataElementSchema.getProperty("parent")).thenReturn(parentProperty);
    when(dataElementSchema.getProperty("createdBy")).thenReturn(createdByProperty);

    // Mock id property
    Property idProperty = mockSimplePersistedProperty("id");

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for "parent.id"
    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.id")).thenReturn(parentIdPath);

    // PropertyPath for "createdBy.id"
    PropertyPath createdByIdPath = new PropertyPath(idProperty, true, new String[] {"createdBy"});
    when(schemaService.getPropertyPath(DataElement.class, "createdBy.id"))
        .thenReturn(createdByIdPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Both filters should be in the in-memory query (conservative approach)
    assertEquals(
        0,
        plan.dbQuery().getFilters().size(),
        "Aliased filters with different roots should NOT be in DB");
    assertEquals(
        2,
        plan.memoryQuery().getFilters().size(),
        "Both parent.id and createdBy.id should be in memory");
    assertFalse(plan.memoryQuery().isEmpty(), "Memory query should NOT be empty");
  }

  /**
   * Tests that deep nested path (parent.parent.id - grandparent) goes to database query when all
   * levels are many-to-one.
   */
  @Test
  void testDeepNestedManyToOnePathGoesToDb() {
    // Given: A filter on deep nested path (parent.parent.id)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("parent.parent.id", "grandparent123"));

    // Mock schema
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property parentProperty = mockManyToOneRelationshipProperty("parent", DataElement.class);
    when(dataElementSchema.getProperty("parent")).thenReturn(parentProperty);

    Property idProperty = mockSimplePersistedProperty("id");

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for "parent.parent.id"
    PropertyPath deepPath = new PropertyPath(idProperty, true, new String[] {"parent", "parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.parent.id")).thenReturn(deepPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Filter should be in the database query
    assertEquals(1, plan.dbQuery().getFilters().size(), "Deep nested path should be in DB");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  /**
   * Tests that collection paths (e.g., dataElementGroups.id) go to in-memory query even with
   * multiple filters.
   */
  @Test
  void testCollectionPathGoesToInMemory() {
    // Given: A filter on collection path
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("dataElementGroups.id", "group123"));

    // Mock schema
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property collectionProperty = mockCollectionProperty("dataElementGroups");
    when(dataElementSchema.getProperty("dataElementGroups")).thenReturn(collectionProperty);

    Property idProperty = mockSimplePersistedProperty("id");

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for collection path - marked as collection
    PropertyPath collectionPath =
        new PropertyPath(idProperty, true, new String[] {"dataElementGroups"});
    when(schemaService.getPropertyPath(DataElement.class, "dataElementGroups.id"))
        .thenReturn(collectionPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Filter should be in in-memory query (collection paths require JOINs)
    assertEquals(0, plan.dbQuery().getFilters().size(), "Collection path should NOT be in DB");
    assertEquals(1, plan.memoryQuery().getFilters().size(), "Collection path should be in memory");
  }

  /**
   * Tests mixing simple filter with single nested many-to-one filter (e.g., id:eq:X AND
   * parent.id:eq:Y). Both should go to DB because there's only one distinct root alias.
   */
  @Test
  void testMixingSimpleAndSingleNestedFilterGoToDb() {
    // Given: A simple filter and a single nested filter (one root alias)
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("id", "element123"));
    query.add(Filters.eq("parent.id", "parent123"));

    // Mock schema
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property idProperty = mockSimplePersistedProperty("id");
    when(dataElementSchema.getProperty("id")).thenReturn(idProperty);

    Property parentProperty = mockManyToOneRelationshipProperty("parent", DataElement.class);
    when(dataElementSchema.getProperty("parent")).thenReturn(parentProperty);

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for "id" (no alias)
    PropertyPath simpleIdPath = new PropertyPath(idProperty, true);
    when(schemaService.getPropertyPath(DataElement.class, "id")).thenReturn(simpleIdPath);

    // PropertyPath for "parent.id"
    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.id")).thenReturn(parentIdPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Both filters should be in the database query
    // (simple filter has no alias, and there's only one distinct root alias "parent")
    assertEquals(2, plan.dbQuery().getFilters().size(), "Both id and parent.id should be in DB");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  /**
   * Tests mixing simple filter with multiple nested filters on different roots. Simple filter goes
   * to DB, but nested filters with different roots fall back to in-memory.
   */
  @Test
  void testMixingSimpleAndMultipleNestedFiltersWithDifferentRoots() {
    // Given: A simple filter and two nested filters with different root aliases
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("id", "element123"));
    query.add(Filters.eq("parent.id", "parent123"));
    query.add(Filters.eq("createdBy.id", "user123"));

    // Mock schema
    Schema dataElementSchema = mockSchema();
    when(dataElementSchema.hasPersistedProperty("name")).thenReturn(true);
    when(dataElementSchema.hasPersistedProperty("id")).thenReturn(true);

    Property idProperty = mockSimplePersistedProperty("id");
    when(dataElementSchema.getProperty("id")).thenReturn(idProperty);

    Property parentProperty = mockManyToOneRelationshipProperty("parent", DataElement.class);
    when(dataElementSchema.getProperty("parent")).thenReturn(parentProperty);

    Property createdByProperty = mockManyToOneRelationshipProperty("createdBy", Object.class);
    when(dataElementSchema.getProperty("createdBy")).thenReturn(createdByProperty);

    when(schemaService.getDynamicSchema(DataElement.class)).thenReturn(dataElementSchema);

    // PropertyPath for "id" (no alias)
    PropertyPath simpleIdPath = new PropertyPath(idProperty, true);
    when(schemaService.getPropertyPath(DataElement.class, "id")).thenReturn(simpleIdPath);

    // PropertyPath for "parent.id"
    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    when(schemaService.getPropertyPath(DataElement.class, "parent.id")).thenReturn(parentIdPath);

    // PropertyPath for "createdBy.id"
    PropertyPath createdByIdPath = new PropertyPath(idProperty, true, new String[] {"createdBy"});
    when(schemaService.getPropertyPath(DataElement.class, "createdBy.id"))
        .thenReturn(createdByIdPath);

    // When: Query plan is created
    QueryPlan<DataElement> plan = queryPlanner.planQuery(query);

    // Then: Simple filter goes to DB, but nested filters with different roots go to in-memory
    assertEquals(1, plan.dbQuery().getFilters().size(), "Only simple id filter should be in DB");
    assertEquals(
        2,
        plan.memoryQuery().getFilters().size(),
        "Nested filters with different roots should be in memory");
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

  private Property mockSimplePersistedProperty(String name) {
    Property property = new Property();
    property.setName(name);
    property.setFieldName(name);
    property.setPersisted(true);
    property.setSimple(true);
    property.setTranslatable(false);
    return property;
  }

  private Property mockManyToOneRelationshipProperty(String name, Class<?> klass) {
    Property property = new Property();
    property.setName(name);
    property.setFieldName(name);
    property.setPersisted(true);
    property.setSimple(false);
    property.setCollection(false);
    property.setKlass(klass);
    return property;
  }

  private Property mockCollectionProperty(String name) {
    Property property = new Property();
    property.setName(name);
    property.setFieldName(name);
    property.setPersisted(true);
    property.setSimple(false);
    property.setCollection(true);
    return property;
  }
}
