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
package org.hisp.dhis.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.jfree.data.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class QueryServiceTest extends PostgresIntegrationTestBase {

  @Autowired private QueryService queryService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @BeforeEach
  void createDataElements() {
    DataElement dataElementA = createDataElement('A');
    dataElementA.setValueType(ValueType.NUMBER);
    DataElement dataElementB = createDataElement('B');
    dataElementB.setValueType(ValueType.BOOLEAN);
    DataElement dataElementC = createDataElement('C');
    dataElementC.setValueType(ValueType.INTEGER);
    DataElement dataElementD = createDataElement('D');
    dataElementD.setValueType(ValueType.NUMBER);
    DataElement dataElementE = createDataElement('E');
    dataElementE.setValueType(ValueType.BOOLEAN);
    DataElement dataElementF = createDataElement('F');
    dataElementF.setValueType(ValueType.INTEGER);
    dataElementA.setCreated(Year.parseYear("2001").getStart());
    dataElementB.setCreated(Year.parseYear("2002").getStart());
    dataElementC.setCreated(Year.parseYear("2003").getStart());
    dataElementD.setCreated(Year.parseYear("2004").getStart());
    dataElementE.setCreated(Year.parseYear("2005").getStart());
    dataElementF.setCreated(Year.parseYear("2006").getStart());
    identifiableObjectManager.save(dataElementB);
    identifiableObjectManager.save(dataElementE);
    identifiableObjectManager.save(dataElementA);
    identifiableObjectManager.save(dataElementC);
    identifiableObjectManager.save(dataElementF);
    identifiableObjectManager.save(dataElementD);
    DataElementGroup dataElementGroupA = createDataElementGroup('A');
    dataElementGroupA
        .getMembers()
        .addAll(Lists.newArrayList(dataElementA, dataElementB, dataElementC));
    DataElementGroup dataElementGroupB = createDataElementGroup('B');
    dataElementGroupB
        .getMembers()
        .addAll(Lists.newArrayList(dataElementD, dataElementE, dataElementF));
    identifiableObjectManager.save(dataElementGroupA);
    identifiableObjectManager.save(dataElementGroupB);
  }

  @Test
  void getAllQuery() {
    Query<?> query = Query.of(DataElement.class);
    assertEquals(6, queryService.query(query).size());
  }

  @Test
  void getAllQueryUrl() throws QueryParserException {
    GetObjectListParams params = new GetObjectListParams().setPaging(false);
    Query<?> query = queryService.getQueryFromUrl(DataElement.class, params);
    assertEquals(6, queryService.query(query).size());
  }

  @Test
  void getAllQueryUrlWithPaginationReturnsPageSizeElements() throws QueryParserException {
    List<? extends IdentifiableObject> resultPage1 = getResultWithPagination(1, 3);
    List<? extends IdentifiableObject> resultPage2 = getResultWithPagination(2, 3);
    String key1 =
        resultPage1.stream().map(IdentifiableObject::getUid).collect(Collectors.joining(","));
    String key2 =
        resultPage2.stream().map(IdentifiableObject::getUid).collect(Collectors.joining(","));
    assertEquals(3, resultPage1.size());
    assertEquals(3, resultPage2.size());
    // check that the actual results are different
    assertThat(key1, not(key2));
  }

  @Test
  void getAllQueryUrlWithPaginationReturnsNoDataWhenPageNumberHigherThanMaxNumberOfPages()
      throws QueryParserException {
    List<? extends IdentifiableObject> resultPage = getResultWithPagination(10, 3);
    assertEquals(0, resultPage.size());
  }

  private List<DataElement> getResultWithPagination(int page, int pageSize) {
    GetObjectListParams params = new GetObjectListParams().setPage(page).setPageSize(pageSize);
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    return queryService.query(query);
  }

  @Test
  void getMinMaxQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.setFirstResult(2);
    query.setMaxResults(10);
    assertEquals(4, queryService.query(query).size());
    query = Query.of(DataElement.class);
    query.setFirstResult(2);
    query.setMaxResults(2);
    assertEquals(2, queryService.query(query).size());
  }

  @Test
  void getEqQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("id", "deabcdefghA"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("deabcdefghA", objects.get(0).getUid());
  }

  @Test
  void getEqQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("id:eq:deabcdefghA"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("deabcdefghA", objects.get(0).getUid());
  }

  @Test
  void getIlikeQueryMatchAllLowercase() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ilike("name", "dataelementa", MatchMode.EXACT));
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("DataElementA", objects.get(0).getName());
  }

  @Test
  void getIlikeQueryMatchAllUppercase() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ilike("name", "DATAELEMENTA", MatchMode.EXACT));
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("DataElementA", objects.get(0).getName());
  }

  @Test
  void getIlikeQueryMatchMixCase() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ilike("name", "DAtAEleMEntA", MatchMode.EXACT));
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("DataElementA", objects.get(0).getName());
  }

  @Test
  void getIlikeQueryNoMatchExtraCharAtEnd() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ilike("name", "dataelementaa", MatchMode.EXACT));
    List<DataElement> objects = queryService.query(query);
    assertEquals(0, objects.size());
  }

  @Test
  void getIlikeQueryNoMatchExtraCharAtStart() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ilike("name", "ddataelementa", MatchMode.EXACT));
    List<DataElement> objects = queryService.query(query);
    assertEquals(0, objects.size());
  }

  @Test
  void getIeqQueryUrlMatch() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("name:ieq:dataelementa"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("DataElementA", objects.get(0).getName());
  }

  @Test
  void getIeqQueryUrlMatchMixedCase() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("name:ieq:dAtAeLeMeNta"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("DataElementA", objects.get(0).getName());
  }

  @Test
  void getIeqQueryUrlNoMatchExtraCharAtStart() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("name:ieq:ddataelementa"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(0, objects.size());
  }

  @Test
  void getNeQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ne("id", "deabcdefghA"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(5, objects.size());
    assertFalse(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getNeQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("id:ne:deabcdefghA"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(5, objects.size());
    assertFalse(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getLikeQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.like("name", "F", MatchMode.ANYWHERE));
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("deabcdefghF", objects.get(0).getUid());
  }

  @Test
  void getLikeQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("name:like:F"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(1, objects.size());
    assertEquals("deabcdefghF", objects.get(0).getUid());
  }

  @Test
  void getGtQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.gt("created", Year.parseYear("2003").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getGtQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("created:gt:2003"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getLtQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.lt("created", Year.parseYear("2003").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(2, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
  }

  @Test
  void getLtQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("created:lt:2003"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(2, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
  }

  @Test
  void getGeQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ge("created", Year.parseYear("2003").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(4, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getGeQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("created:ge:2003"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(4, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void getLeQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.le("created", Year.parseYear("2003").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
  }

  @Test
  void getLeQueryUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("created:le:2003"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
  }

  @Test
  void getBetweenQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(
        Filters.between(
            "created", Year.parseYear("2003").getStart(), Year.parseYear("2005").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
  }

  @Test
  void getInQuery() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.in("id", Lists.newArrayList("deabcdefghD", "deabcdefghF")));
    List<DataElement> objects = queryService.query(query);
    assertEquals(2, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void sortNameDesc() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.addOrder(new Order("name", Direction.DESCENDING));
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertEquals("deabcdefghF", objects.get(0).getUid());
    assertEquals("deabcdefghE", objects.get(1).getUid());
    assertEquals("deabcdefghD", objects.get(2).getUid());
    assertEquals("deabcdefghC", objects.get(3).getUid());
    assertEquals("deabcdefghB", objects.get(4).getUid());
    assertEquals("deabcdefghA", objects.get(5).getUid());
  }

  @Test
  void sortNameAsc() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.addOrder(new Order("name", Direction.ASCENDING));
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertEquals("deabcdefghA", objects.get(0).getUid());
    assertEquals("deabcdefghB", objects.get(1).getUid());
    assertEquals("deabcdefghC", objects.get(2).getUid());
    assertEquals("deabcdefghD", objects.get(3).getUid());
    assertEquals("deabcdefghE", objects.get(4).getUid());
    assertEquals("deabcdefghF", objects.get(5).getUid());
  }

  @Test
  void sortCreatedDesc() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.addOrder(new Order("created", Direction.DESCENDING));
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertEquals("deabcdefghF", objects.get(0).getUid());
    assertEquals("deabcdefghE", objects.get(1).getUid());
    assertEquals("deabcdefghD", objects.get(2).getUid());
    assertEquals("deabcdefghC", objects.get(3).getUid());
    assertEquals("deabcdefghB", objects.get(4).getUid());
    assertEquals("deabcdefghA", objects.get(5).getUid());
  }

  @Test
  void sortCreatedAsc() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.addOrder(new Order("created", Direction.ASCENDING));
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertEquals("deabcdefghA", objects.get(0).getUid());
    assertEquals("deabcdefghB", objects.get(1).getUid());
    assertEquals("deabcdefghC", objects.get(2).getUid());
    assertEquals("deabcdefghD", objects.get(3).getUid());
    assertEquals("deabcdefghE", objects.get(4).getUid());
    assertEquals("deabcdefghF", objects.get(5).getUid());
  }

  @Test
  void testDoubleEqConjunction() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.eq("id", "deabcdefghD"));
    query.add(Filters.eq("id", "deabcdefghF"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(0, objects.size());
  }

  @Test
  void testDoubleEqDisjunction() {
    Query<DataElement> query = Query.of(DataElement.class, Junction.Type.OR);
    query.add(Filters.eq("id", "deabcdefghD"));
    query.add(Filters.eq("id", "deabcdefghF"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(2, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void testDateRange() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.ge("created", Year.parseYear("2002").getStart()));
    query.add(Filters.le("created", Year.parseYear("2004").getStart()));
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
  }

  @Test
  void testIsNotNull() {
    Query<DataElement> query = Query.of(DataElement.class);
    query.add(Filters.isNotNull("categoryCombo"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void testIsNotNullUrl() throws QueryParserException {
    GetObjectListParams params =
        new GetObjectListParams().setPaging(false).setFilters(List.of("categoryCombo:!null"));
    Query<DataElement> query = queryService.getQueryFromUrl(DataElement.class, params);
    List<DataElement> objects = queryService.query(query);
    assertEquals(6, objects.size());
    assertTrue(collectionContainsUid(objects, "deabcdefghA"));
    assertTrue(collectionContainsUid(objects, "deabcdefghB"));
    assertTrue(collectionContainsUid(objects, "deabcdefghC"));
    assertTrue(collectionContainsUid(objects, "deabcdefghD"));
    assertTrue(collectionContainsUid(objects, "deabcdefghE"));
    assertTrue(collectionContainsUid(objects, "deabcdefghF"));
  }

  @Test
  void testCriteriaAndRootJunctionDE() {
    Query<DataElement> query = Query.of(DataElement.class, Junction.Type.AND);
    query.add(Filters.eq("id", "deabcdefghA"));
    query.add(Filters.eq("id", "deabcdefghB"));
    query.add(Filters.eq("id", "deabcdefghC"));
    List<DataElement> objects = queryService.query(query);
    assertTrue(objects.isEmpty());
  }

  @Test
  void testCriteriaOrRootJunctionDE() {
    Query<DataElement> query = Query.of(DataElement.class, Junction.Type.OR);
    query.add(Filters.eq("id", "deabcdefghA"));
    query.add(Filters.eq("id", "deabcdefghB"));
    query.add(Filters.eq("id", "deabcdefghC"));
    List<DataElement> objects = queryService.query(query);
    assertEquals(3, objects.size());
  }

  @Test
  void testCriteriaAndRootJunctionDEG() {
    Query<DataElementGroup> query = Query.of(DataElementGroup.class, Junction.Type.AND);
    query.add(Filters.eq("dataElements.id", "deabcdefghA"));
    query.add(Filters.eq("dataElements.id", "deabcdefghB"));
    query.add(Filters.eq("dataElements.id", "deabcdefghC"));
    query.add(Filters.eq("dataElements.id", "deabcdefghD"));
    query.add(Filters.eq("dataElements.id", "deabcdefghE"));
    query.add(Filters.eq("dataElements.id", "deabcdefghF"));
    List<DataElementGroup> objects = queryService.query(query);
    assertTrue(objects.isEmpty());
  }

  @Test
  void testCriteriaOrRootJunctionDEG1() {
    Query<DataElementGroup> query = Query.of(DataElementGroup.class, Junction.Type.OR);
    query.add(Filters.eq("dataElements.id", "deabcdefghA"));
    query.add(Filters.eq("dataElements.id", "deabcdefghD"));
    List<DataElementGroup> objects = queryService.query(query);
    assertEquals(2, objects.size());
  }

  @Test
  void testCriteriaOrRootJunctionDEG2() {
    Query<DataElementGroup> query = Query.of(DataElementGroup.class, Junction.Type.OR);
    query.add(Filters.eq("dataElements.id", "deabcdefghA"));
    List<DataElementGroup> objects = queryService.query(query);
    assertEquals(1, objects.size());
  }

  @Test
  void testMixedQSRootJunction1() {
    Query<DataElementGroup> query = Query.of(DataElementGroup.class, Junction.Type.OR);
    query.add(Filters.eq("id", "abcdefghijA"));
    query.add(Filters.eq("dataElements.id", "deabcdefghA"));
    query.add(Filters.eq("dataElements.id", "deabcdefghD"));
    List<DataElementGroup> objects = queryService.query(query);
    assertEquals(2, objects.size());
  }

  @Test
  void testMixedQSRootJunction2() {
    Query<DataElementGroup> query = Query.of(DataElementGroup.class, Junction.Type.OR);
    query.add(Filters.eq("id", "abcdefghijA"));
    query.add(Filters.eq("dataElements.id", "does-not-exist"));
    List<DataElementGroup> objects = queryService.query(query);
    assertEquals(1, objects.size());
  }

  /**
   * Tests filtering on embedded object paths like fromConstraint.trackedEntityType.id. These paths
   * should use in-memory filtering since JPA navigation through embedded objects followed by
   * relationships requires special handling.
   */
  @Test
  void testFilterOnEmbeddedObjectPath() {
    // Create TrackedEntityTypes
    TrackedEntityType tetA = createTrackedEntityType('X');
    TrackedEntityType tetB = createTrackedEntityType('Y');
    identifiableObjectManager.save(tetA);
    identifiableObjectManager.save(tetB);

    // Create RelationshipType A with fromConstraint pointing to tetA
    RelationshipType relTypeA = new RelationshipType();
    relTypeA.setAutoFields();
    relTypeA.setName("RelTypeA");
    relTypeA.setFromToName("from_A");
    relTypeA.setToFromName("to_A");
    RelationshipConstraint fromConstraintA = new RelationshipConstraint();
    fromConstraintA.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    fromConstraintA.setTrackedEntityType(tetA);
    fromConstraintA.setTrackerDataView(TrackerDataView.builder().build());
    RelationshipConstraint toConstraintA = new RelationshipConstraint();
    toConstraintA.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    toConstraintA.setTrackerDataView(TrackerDataView.builder().build());
    relTypeA.setFromConstraint(fromConstraintA);
    relTypeA.setToConstraint(toConstraintA);
    identifiableObjectManager.save(relTypeA);

    // Create RelationshipType B with fromConstraint pointing to tetB
    RelationshipType relTypeB = new RelationshipType();
    relTypeB.setAutoFields();
    relTypeB.setName("RelTypeB");
    relTypeB.setFromToName("from_B");
    relTypeB.setToFromName("to_B");
    RelationshipConstraint fromConstraintB = new RelationshipConstraint();
    fromConstraintB.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    fromConstraintB.setTrackedEntityType(tetB);
    fromConstraintB.setTrackerDataView(TrackerDataView.builder().build());
    RelationshipConstraint toConstraintB = new RelationshipConstraint();
    toConstraintB.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    toConstraintB.setTrackerDataView(TrackerDataView.builder().build());
    relTypeB.setFromConstraint(fromConstraintB);
    relTypeB.setToConstraint(toConstraintB);
    identifiableObjectManager.save(relTypeB);

    // Query filtering by embedded object path: fromConstraint.trackedEntityType.id
    Query<RelationshipType> query = Query.of(RelationshipType.class);
    query.add(Filters.eq("fromConstraint.trackedEntityType.id", tetA.getUid()));
    List<RelationshipType> results = queryService.query(query);

    // Should find only relTypeA
    assertEquals(1, results.size());
    assertEquals(relTypeA.getUid(), results.get(0).getUid());

    // Also test filtering by embedded object's simple property: fromConstraint.relationshipEntity
    Query<RelationshipType> query2 = Query.of(RelationshipType.class);
    query2.add(Filters.eq("fromConstraint.relationshipEntity", "TRACKED_ENTITY_INSTANCE"));
    List<RelationshipType> results2 = queryService.query(query2);

    // Should find both relationship types
    assertEquals(2, results2.size());
  }

  @Test
  void testDefaultSortOrder() {
    Date date = new Date();
    OrganisationUnit organisationUnitC = createOrganisationUnit("C");
    organisationUnitC.setUid("ccccccccccc");
    organisationUnitC.setName("orgunit");
    organisationUnitC.setCreated(date);
    organisationUnitC.setLastUpdated(date);
    OrganisationUnit organisationUnitB = createOrganisationUnit("B");
    organisationUnitB.setUid("bbbbbbbbbbb");
    organisationUnitB.setName("orgunit");
    organisationUnitB.setCreated(date);
    organisationUnitB.setLastUpdated(date);
    OrganisationUnit organisationUnitA = createOrganisationUnit("A");
    organisationUnitA.setUid("aaaaaaaaaaa");
    organisationUnitA.setName("orgunit");
    organisationUnitA.setCreated(date);
    organisationUnitA.setLastUpdated(date);
    identifiableObjectManager.save(organisationUnitC);
    identifiableObjectManager.save(organisationUnitB);
    identifiableObjectManager.save(organisationUnitA);
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    query.setDefaultOrder();
    List<OrganisationUnit> objects = queryService.query(query);
    assertEquals(3, objects.size());
    assertEquals("aaaaaaaaaaa", objects.get(0).getUid());
    assertEquals("bbbbbbbbbbb", objects.get(1).getUid());
    assertEquals("ccccccccccc", objects.get(2).getUid());
  }

  // -------------------------------------------------------------------------
  // Tests for multiple nested/aliased path filters
  // These tests verify that multiple filters on many-to-one relationships
  // work correctly with the database-level filtering optimization.
  // -------------------------------------------------------------------------

  /**
   * Tests multiple filters on DIFFERENT many-to-one paths with AND junction. This tests the
   * scenario: filter=parent.id:eq:X&filter=createdBy.id:eq:Y
   */
  @Test
  void testMultipleFiltersOnDifferentManyToOnePaths() {
    // Create org unit hierarchy: root -> child1, child2
    OrganisationUnit root = createOrganisationUnit("Root");
    root.setUid("rootrootrou");
    identifiableObjectManager.save(root);

    OrganisationUnit child1 = createOrganisationUnit("Child1");
    child1.setUid("child1child");
    child1.setParent(root);
    identifiableObjectManager.save(child1);

    OrganisationUnit child2 = createOrganisationUnit("Child2");
    child2.setUid("child2child");
    child2.setParent(root);
    identifiableObjectManager.save(child2);

    // Test: Filter by parent.id - should find both children
    Query<OrganisationUnit> query1 = Query.of(OrganisationUnit.class);
    query1.add(Filters.eq("parent.id", "rootrootrou"));
    List<OrganisationUnit> results1 = queryService.query(query1);
    assertEquals(2, results1.size());

    // Test: Filter by parent.name - should find both children
    Query<OrganisationUnit> query2 = Query.of(OrganisationUnit.class);
    query2.add(Filters.eq("parent.name", "Root"));
    List<OrganisationUnit> results2 = queryService.query(query2);
    assertEquals(2, results2.size());
  }

  /**
   * Tests multiple filters on the SAME many-to-one path with AND junction. This tests the scenario:
   * filter=parent.id:eq:X&filter=parent.name:like:Y Both conditions must apply to the same parent
   * entity.
   */
  @Test
  void testMultipleFiltersOnSameManyToOnePath() {
    // Create org unit hierarchy
    OrganisationUnit parent1 = createOrganisationUnit("ParentAlpha");
    parent1.setUid("parentalpha");
    identifiableObjectManager.save(parent1);

    OrganisationUnit parent2 = createOrganisationUnit("ParentBeta");
    parent2.setUid("parentbeta1");
    identifiableObjectManager.save(parent2);

    OrganisationUnit child1 = createOrganisationUnit("ChildOfAlpha");
    child1.setUid("childofalph");
    child1.setParent(parent1);
    identifiableObjectManager.save(child1);

    OrganisationUnit child2 = createOrganisationUnit("ChildOfBeta");
    child2.setUid("childofbeta");
    child2.setParent(parent2);
    identifiableObjectManager.save(child2);

    // Test: Filter by parent.id AND parent.name matching the same parent
    // Should find only child1
    Query<OrganisationUnit> query1 = Query.of(OrganisationUnit.class);
    query1.add(Filters.eq("parent.id", "parentalpha"));
    query1.add(Filters.like("parent.name", "Alpha", MatchMode.ANYWHERE));
    List<OrganisationUnit> results1 = queryService.query(query1);
    assertEquals(1, results1.size());
    assertEquals("childofalph", results1.get(0).getUid());

    // Test: Filter by parent.id but with non-matching parent.name
    // Should find nothing (both conditions must match the same parent)
    Query<OrganisationUnit> query2 = Query.of(OrganisationUnit.class);
    query2.add(Filters.eq("parent.id", "parentalpha"));
    query2.add(Filters.like("parent.name", "Beta", MatchMode.ANYWHERE));
    List<OrganisationUnit> results2 = queryService.query(query2);
    assertEquals(0, results2.size());
  }

  /**
   * Tests multiple filters on different many-to-one paths with OR junction. This tests the
   * scenario: filter=parent.id:eq:X&rootJunction=OR&filter=name:eq:Y With OR junction and mixed
   * DB/in-memory filters, all should go to in-memory.
   */
  @Test
  void testMultipleManyToOneFiltersWithOrJunction() {
    // Create org unit hierarchy
    OrganisationUnit parent = createOrganisationUnit("ParentOU");
    parent.setUid("parentoupar");
    identifiableObjectManager.save(parent);

    OrganisationUnit child1 = createOrganisationUnit("MatchingChild");
    child1.setUid("matchingchi");
    child1.setParent(parent);
    identifiableObjectManager.save(child1);

    OrganisationUnit child2 = createOrganisationUnit("OtherChild");
    child2.setUid("otherchild1");
    child2.setParent(parent);
    identifiableObjectManager.save(child2);

    OrganisationUnit standalone = createOrganisationUnit("MatchingChild");
    standalone.setUid("standalone1");
    identifiableObjectManager.save(standalone);

    // Test: Filter by parent.id OR name - should find children of parent AND standalone
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class, Junction.Type.OR);
    query.add(Filters.eq("parent.id", "parentoupar"));
    query.add(Filters.eq("name", "MatchingChild"));
    List<OrganisationUnit> results = queryService.query(query);
    // Should find: child1 (matches parent.id), child2 (matches parent.id), standalone (matches
    // name)
    assertEquals(3, results.size());
  }

  /**
   * Tests deep nested path filter (parent.parent.id - grandparent). This verifies that multi-level
   * navigation works correctly.
   */
  @Test
  void testDeepNestedPathFilter() {
    // Create 3-level hierarchy: grandparent -> parent -> child
    OrganisationUnit grandparent = createOrganisationUnit("Grandparent");
    grandparent.setUid("grandparent");
    identifiableObjectManager.save(grandparent);

    OrganisationUnit parent = createOrganisationUnit("ParentLevel");
    parent.setUid("parentlevel");
    parent.setParent(grandparent);
    identifiableObjectManager.save(parent);

    OrganisationUnit child = createOrganisationUnit("ChildLevel");
    child.setUid("childlevel1");
    child.setParent(parent);
    identifiableObjectManager.save(child);

    // Another branch: grandparent2 -> parent2 -> child2
    OrganisationUnit grandparent2 = createOrganisationUnit("Grandparent2");
    grandparent2.setUid("grandparen2");
    identifiableObjectManager.save(grandparent2);

    OrganisationUnit parent2 = createOrganisationUnit("ParentLevel2");
    parent2.setUid("parentleve2");
    parent2.setParent(grandparent2);
    identifiableObjectManager.save(parent2);

    OrganisationUnit child2 = createOrganisationUnit("ChildLevel2");
    child2.setUid("childlevel2");
    child2.setParent(parent2);
    identifiableObjectManager.save(child2);

    // Test: Filter by parent.parent.id (grandparent)
    // Note: This is a 2-level deep navigation
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    query.add(Filters.eq("parent.parent.id", "grandparent"));
    List<OrganisationUnit> results = queryService.query(query);

    // Should find only child (whose grandparent is "grandparent")
    assertEquals(1, results.size());
    assertEquals("childlevel1", results.get(0).getUid());
  }

  /**
   * Tests mixing simple property filter with nested many-to-one filter. This tests the scenario:
   * filter=id:eq:X&filter=parent.id:eq:Y
   */
  @Test
  void testMixingSimpleAndNestedFilters() {
    // Create org unit hierarchy
    OrganisationUnit parent = createOrganisationUnit("SimpleParent");
    parent.setUid("simpleparnt");
    identifiableObjectManager.save(parent);

    OrganisationUnit targetChild = createOrganisationUnit("TargetChild");
    targetChild.setUid("targetchild");
    targetChild.setParent(parent);
    identifiableObjectManager.save(targetChild);

    OrganisationUnit otherChild = createOrganisationUnit("OtherChild");
    otherChild.setUid("otherchild2");
    otherChild.setParent(parent);
    identifiableObjectManager.save(otherChild);

    // Test: Filter by id AND parent.id - should find exactly one
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    query.add(Filters.eq("id", "targetchild"));
    query.add(Filters.eq("parent.id", "simpleparnt"));
    List<OrganisationUnit> results = queryService.query(query);
    assertEquals(1, results.size());
    assertEquals("targetchild", results.get(0).getUid());

    // Test: Filter by id with wrong parent.id - should find nothing
    Query<OrganisationUnit> query2 = Query.of(OrganisationUnit.class);
    query2.add(Filters.eq("id", "targetchild"));
    query2.add(Filters.eq("parent.id", "wrongparent"));
    List<OrganisationUnit> results2 = queryService.query(query2);
    assertEquals(0, results2.size());
  }

  private boolean collectionContainsUid(
      Collection<? extends IdentifiableObject> collection, String uid) {
    for (IdentifiableObject identifiableObject : collection) {
      if (identifiableObject.getUid().equals(uid)) {
        return true;
      }
    }
    return false;
  }
}
