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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultQueryPlanner}.
 *
 * @author Viet Nguyen
 */
@ExtendWith(MockitoExtension.class)
class DefaultQueryPlannerTest {

  @Mock private SchemaService schemaService;

  private DefaultQueryPlanner queryPlanner;

  @BeforeEach
  void setUp() {
    queryPlanner = new DefaultQueryPlanner(schemaService);
  }

  @Test
  @DisplayName("Should route relationship filter (parent.id) to database query")
  void testParentIdFilterResultsInDatabaseQuery() {
    // Given: A query with filter on parent.id
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter filter = new Filter("parent.id", new EqualOperator<>("ybg3MO3hcf4"));
    query.add(filter);

    // Mock schema service to return a persisted property path with alias
    Schema schema = mock(Schema.class);
    Property idProperty = new Property();
    idProperty.setName("id");
    idProperty.setFieldName("id");
    idProperty.setPersisted(true);
    idProperty.setSimple(true);

    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("parent.id")))
        .thenReturn(parentIdPath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query, not memory query
    assertEquals(1, plan.dbQuery().getFilters().size(), "Filter should be in database query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
    assertEquals(
        "parent.id",
        plan.dbQuery().getFilters().get(0).getPath(),
        "Database query should contain parent.id filter");
  }

  @Test
  @DisplayName("Should route multiple relationship filters to database query")
  void testMultipleRelationshipFiltersResultInDatabaseQuery() {
    // Given: A query with filters on parent.id and parent.name
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter parentIdFilter = new Filter("parent.id", new EqualOperator<>("ybg3MO3hcf4"));
    Filter parentNameFilter = new Filter("parent.name", new EqualOperator<>("Health Facility"));
    query.add(parentIdFilter);
    query.add(parentNameFilter);

    // Mock schema service
    Schema schema = mock(Schema.class);

    Property idProperty = new Property();
    idProperty.setName("id");
    idProperty.setFieldName("id");
    idProperty.setPersisted(true);
    idProperty.setSimple(true);

    Property nameProperty = new Property();
    nameProperty.setName("name");
    nameProperty.setFieldName("name");
    nameProperty.setPersisted(true);
    nameProperty.setSimple(true);

    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    PropertyPath parentNamePath = new PropertyPath(nameProperty, true, new String[] {"parent"});

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("parent.id")))
        .thenReturn(parentIdPath);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("parent.name")))
        .thenReturn(parentNamePath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: Both filters should be in the database query
    assertEquals(2, plan.dbQuery().getFilters().size(), "Both filters should be in database query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  @Test
  @DisplayName("Should route nested relationship filter (parent.parent.id) to database query")
  void testNestedRelationshipFilterResultsInDatabaseQuery() {
    // Given: A query with filter on parent.parent.id (grandparent)
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter filter = new Filter("parent.parent.id", new EqualOperator<>("abc123"));
    query.add(filter);

    // Mock schema service
    Schema schema = mock(Schema.class);
    Property idProperty = new Property();
    idProperty.setName("id");
    idProperty.setFieldName("id");
    idProperty.setPersisted(true);
    idProperty.setSimple(true);

    PropertyPath grandparentIdPath =
        new PropertyPath(idProperty, true, new String[] {"parent", "parent"});

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("parent.parent.id")))
        .thenReturn(grandparentIdPath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query
    assertEquals(1, plan.dbQuery().getFilters().size(), "Filter should be in database query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
    assertEquals(
        "parent.parent.id",
        plan.dbQuery().getFilters().get(0).getPath(),
        "Database query should contain parent.parent.id filter");
  }

  @Test
  @DisplayName("Should route simple property filter to database query")
  void testSimplePropertyFilterResultsInDatabaseQuery() {
    // Given: A query with filter on name (no relationship)
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter filter = new Filter("name", new EqualOperator<>("Health Center"));
    query.add(filter);

    // Mock schema service
    Schema schema = mock(Schema.class);
    Property nameProperty = new Property();
    nameProperty.setName("name");
    nameProperty.setFieldName("name");
    nameProperty.setPersisted(true);
    nameProperty.setSimple(true);

    PropertyPath namePath = new PropertyPath(nameProperty, true); // No alias

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("name")))
        .thenReturn(namePath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the database query
    assertEquals(1, plan.dbQuery().getFilters().size(), "Filter should be in database query");
    assertTrue(plan.memoryQuery().isEmpty(), "Memory query should be empty");
  }

  @Test
  @DisplayName("Should route non-persisted property filter to memory query")
  void testNonPersistedPropertyFilterResultsInMemoryQuery() {
    // Given: A query with filter on a non-persisted property
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter filter = new Filter("displayName", new EqualOperator<>("Health Center"));
    query.add(filter);

    // Mock schema service - displayName is not persisted as a column
    Schema schema = mock(Schema.class);
    Property displayNameProperty = new Property();
    displayNameProperty.setName("displayName");
    displayNameProperty.setFieldName("displayName");
    displayNameProperty.setPersisted(false); // Not persisted
    displayNameProperty.setSimple(true);

    PropertyPath displayNamePath = new PropertyPath(displayNameProperty, false);

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("displayName")))
        .thenReturn(displayNamePath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: The filter should be in the memory query, not database query
    assertEquals(1, plan.memoryQuery().getFilters().size(), "Filter should be in memory query");
    assertTrue(plan.dbQuery().getFilters().isEmpty(), "Database query should be empty");
  }

  @Test
  @DisplayName("Should split filters between database and memory queries based on persistence")
  void testMixedFiltersAreSplitCorrectly() {
    // Given: A query with both persisted and non-persisted filters
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    Filter persistedFilter = new Filter("parent.id", new EqualOperator<>("ybg3MO3hcf4"));
    Filter nonPersistedFilter = new Filter("displayName", new EqualOperator<>("Health Center"));
    query.add(persistedFilter);
    query.add(nonPersistedFilter);

    // Mock schema service
    Schema schema = mock(Schema.class);

    Property idProperty = new Property();
    idProperty.setName("id");
    idProperty.setFieldName("id");
    idProperty.setPersisted(true);
    idProperty.setSimple(true);

    Property displayNameProperty = new Property();
    displayNameProperty.setName("displayName");
    displayNameProperty.setFieldName("displayName");
    displayNameProperty.setPersisted(false);
    displayNameProperty.setSimple(true);

    PropertyPath parentIdPath = new PropertyPath(idProperty, true, new String[] {"parent"});
    PropertyPath displayNamePath = new PropertyPath(displayNameProperty, false);

    when(schemaService.getDynamicSchema(OrganisationUnit.class)).thenReturn(schema);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("parent.id")))
        .thenReturn(parentIdPath);
    when(schemaService.getPropertyPath(eq(OrganisationUnit.class), eq("displayName")))
        .thenReturn(displayNamePath);
    when(schema.hasPersistedProperty(anyString())).thenReturn(false);

    // When: Planning the query
    QueryPlan<OrganisationUnit> plan = queryPlanner.planQuery(query);

    // Then: Filters should be split correctly
    assertEquals(
        1, plan.dbQuery().getFilters().size(), "Persisted filter should be in database query");
    assertEquals(
        1,
        plan.memoryQuery().getFilters().size(),
        "Non-persisted filter should be in memory query");
    assertEquals("parent.id", plan.dbQuery().getFilters().get(0).getPath());
    assertEquals("displayName", plan.memoryQuery().getFilters().get(0).getPath());
  }
}
