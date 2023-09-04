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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventQueryParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class TrackerEventCriteriaMapperTest {

  private static final String DE_1_UID = "OBzmpRP6YUh";

  private static final String DE_2_UID = "KSd4PejqBf9";

  private static final String TEA_1_UID = "TvjwTPToKHO";

  private static final String TEA_2_UID = "cy2oRh2sNr6";

  private static final String PROGRAM_UID = "programuid";

  @Mock private CurrentUserService currentUserService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityInstanceService entityInstanceService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private DataElementService dataElementService;

  @Mock private InputUtils inputUtils;

  @Mock private SchemaService schemaService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @InjectMocks private TrackerEventCriteriaMapper mapper;

  private Program program;

  private ProgramStage programStage;

  private TrackedEntityInstance trackedEntityInstance;

  private TrackedEntityAttribute tea1;

  private OrganisationUnit orgUnit;

  private final String orgUnitId = "orgUnitId";

  private final List<OrganisationUnit> orgUnitDescendants =
      List.of(
          createOrgUnit("orgUnit1", "uid1"),
          createOrgUnit("orgUnit2", "uid2"),
          createOrgUnit("captureScopeOrgUnit", "uid3"),
          createOrgUnit("searchScopeOrgUnit", "uid4"));

  @BeforeEach
  public void setUp() {
    User user = new User();
    OrganisationUnit ou = new OrganisationUnit();
    user.setOrganisationUnits(Set.of(ou));
    when(currentUserService.getCurrentUser()).thenReturn(user);

    program = new Program();
    program.setUid(PROGRAM_UID);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    programStage = new ProgramStage();
    programStage.setUid("programstageuid");
    when(programStageService.getProgramStage("programstageuid")).thenReturn(programStage);
    when(aclService.canDataRead(user, programStage)).thenReturn(true);

    orgUnit = createOrgUnit("orgUnit", orgUnitId);
    orgUnit.setChildren(
        Set.of(
            createOrgUnit("captureScopeChild", "captureScopeChildUid"),
            createOrgUnit("searchScopeChild", "searchScopeChildUid")));

    trackedEntityInstance = new TrackedEntityInstance();
    when(entityInstanceService.getTrackedEntityInstance("teiuid"))
        .thenReturn(trackedEntityInstance);
    tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid(TEA_2_UID);
    when(attributeService.getAllTrackedEntityAttributes()).thenReturn(List.of(tea1, tea2));
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(DE_1_UID)).thenReturn(de1);
    DataElement de2 = new DataElement();
    de2.setUid(DE_2_UID);
    when(dataElementService.getDataElement(DE_2_UID)).thenReturn(de2);

    Schema eventSchema = new Schema(Event.class, "event", "events");
    Property prop1 = new Property();
    prop1.setName("programStage");
    prop1.setSimple(true);
    eventSchema.addProperty(prop1);
    Property prop2 = new Property();
    prop2.setName("dueDate");
    prop2.setSimple(true);
    eventSchema.addProperty(prop2);
    Property prop3 = new Property();
    prop3.setName("nonSimple");
    prop3.setSimple(false);
    eventSchema.addProperty(prop3);
    when(schemaService.getDynamicSchema(Event.class)).thenReturn(eventSchema);
    mapper.setSchema();
  }

  @Test
  void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
      throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    mapper.map(criteria);

    verifyNoInteractions(programService);
    verifyNoInteractions(programStageService);
    verifyNoInteractions(organisationUnitService);
    verifyNoInteractions(entityInstanceService);
  }

  @Test
  void testMappingProgram() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgram(PROGRAM_UID);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(program, params.getProgram());
  }

  @Test
  void testMappingProgramNotFound() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgram("unknown");

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertEquals("Program is specified but does not exist: unknown", exception.getMessage());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgramStage("programstageuid");

    EventQueryParams params = mapper.map(criteria);

    assertEquals(programStage, params.getProgramStage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownProgramStage() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgramStage("unknown");

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertEquals("Program stage is specified but does not exist: unknown", exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownOrgUnit() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setOrgUnit("unknown");
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));

    assertEquals("Org unit is specified but does not exist: unknown", exception.getMessage());
  }

  @Test
  void testMappingTrackedEntity() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setTrackedEntity("teiuid");

    EventQueryParams params = mapper.map(criteria);

    assertEquals(trackedEntityInstance, params.getTrackedEntityInstance());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenTrackedEntityDoesNotExist() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setTrackedEntity("teiuid");
    when(entityInstanceService.getTrackedEntityInstance("teiuid")).thenReturn(null);

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));

    assertStartsWith(
        "Tracked entity instance is specified but does not exist: " + criteria.getTrackedEntity(),
        exception.getMessage());
  }

  @Test
  void testMappingOccurredAfterBefore() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Date occurredAfter = parseDate("2020-01-01");
    criteria.setOccurredAfter(occurredAfter);
    Date occurredBefore = parseDate("2020-09-12");
    criteria.setOccurredBefore(occurredBefore);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(occurredAfter, params.getStartDate());
    assertEquals(occurredBefore, params.getEndDate());
  }

  @Test
  void testMappingScheduledAfterBefore() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Date scheduledAfter = parseDate("2021-01-01");
    criteria.setScheduledAfter(scheduledAfter);
    Date scheduledBefore = parseDate("2021-09-12");
    criteria.setScheduledBefore(scheduledBefore);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(scheduledAfter, params.getDueDateStart());
    assertEquals(scheduledBefore, params.getDueDateEnd());
  }

  @Test
  void testMappingUpdatedDates() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Date updatedAfter = parseDate("2022-01-01");
    criteria.setUpdatedAfter(updatedAfter);
    Date updatedBefore = parseDate("2022-09-12");
    criteria.setUpdatedBefore(updatedBefore);
    String updatedWithin = "P6M";
    criteria.setUpdatedWithin(updatedWithin);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(updatedAfter, params.getLastUpdatedStartDate());
    assertEquals(updatedBefore, params.getLastUpdatedEndDate());
    assertEquals(updatedWithin, params.getLastUpdatedDuration());
  }

  @Test
  void testMappingEnrollmentEnrolledAtDates() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Date enrolledBefore = parseDate("2022-01-01");
    criteria.setEnrollmentEnrolledBefore(enrolledBefore);
    Date enrolledAfter = parseDate("2022-02-01");
    criteria.setEnrollmentEnrolledAfter(enrolledAfter);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(enrolledBefore, params.getEnrollmentEnrolledBefore());
    assertEquals(enrolledAfter, params.getEnrollmentEnrolledAfter());
  }

  @Test
  void testMappingEnrollmentOccurredAtDates() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Date enrolledBefore = parseDate("2022-01-01");
    criteria.setEnrollmentOccurredBefore(enrolledBefore);
    Date enrolledAfter = parseDate("2022-02-01");
    criteria.setEnrollmentOccurredAfter(enrolledAfter);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(enrolledBefore, params.getEnrollmentOccurredBefore());
    assertEquals(enrolledAfter, params.getEnrollmentOccurredAfter());
  }

  @Test
  void testMappingAttributeOrdering() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    OrderCriteria attributeOrder = OrderCriteria.of(TEA_1_UID, SortDirection.ASC);
    OrderCriteria unknownAttributeOrder = OrderCriteria.of("unknownAtt1", SortDirection.ASC);
    criteria.setOrder(List.of(attributeOrder, unknownAttributeOrder));

    EventQueryParams params = mapper.map(criteria);

    assertAll(
        () ->
            assertContainsOnly(
                params.getAttributeOrders(), List.of(new OrderParam(TEA_1_UID, SortDirection.ASC))),
        () -> assertContainsOnly(params.getFilterAttributes(), List.of(new QueryItem(tea1))));
  }

  @Test
  void testMappingEnrollments() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    Set<String> enrollments = Set.of("NQnuK2kLm6e");
    criteria.setEnrollments(enrollments);

    EventQueryParams params = mapper.map(criteria);

    assertEquals(enrollments, params.getProgramInstances());
  }

  @Test
  void testMappingEvents() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setEvent("XKrcfuM4Hcw;M4pNmLabtXl");

    EventQueryParams params = mapper.map(criteria);

    assertEquals(Set.of("XKrcfuM4Hcw", "M4pNmLabtXl"), params.getEvents());
  }

  @Test
  void testMappingEventsStripsInvalidUid() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setEvent("invalidUid;M4pNmLabtXl");

    EventQueryParams params = mapper.map(criteria);

    assertEquals(Set.of("M4pNmLabtXl"), params.getEvents());
  }

  @Test
  void testMappingEventIsNull() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();

    EventQueryParams params = mapper.map(criteria);

    assertIsEmpty(params.getEvents());
  }

  @Test
  void testMappingEventIsEmpty() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setEvent(" ");

    EventQueryParams params = mapper.map(criteria);

    assertIsEmpty(params.getEvents());
  }

  @Test
  void testMappingAssignedUserStripsInvalidUid() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setAssignedUser("invalidUid;M4pNmLabtXl");

    EventQueryParams params = mapper.map(criteria);

    assertEquals(Set.of("M4pNmLabtXl"), params.getAssignedUserQueryParam().getAssignedUsers());
  }

  @Test
  void testMappingAssignedUserIsEmpty() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setAssignedUser(" ");

    EventQueryParams params = mapper.map(criteria);

    assertIsEmpty(params.getAssignedUserQueryParam().getAssignedUsers());
  }

  @Test
  void testMutualExclusionOfEventsAndFilter() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilter(DE_1_UID + ":ge:1:le:2");
    criteria.setEvent(DE_1_UID + ";" + DE_2_UID);

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertEquals(
        "Event UIDs and filters can not be specified at the same time", exception.getMessage());
  }

  @Test
  void testOrderByEventSchemaProperties() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setOrder(OrderCriteria.fromOrderString("programStage:desc,dueDate:asc"));

    EventQueryParams params = mapper.map(criteria);

    assertContainsOnly(
        List.of(
            new OrderParam("programStage", SortDirection.DESC),
            new OrderParam("dueDate", SortDirection.ASC)),
        params.getOrders());
  }

  @Test
  void testOrderBySupportedPropertyNotInEventSchema()
      throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setOrder(OrderCriteria.fromOrderString("enrolledAt:asc"));

    EventQueryParams params = mapper.map(criteria);

    assertContainsOnly(
        List.of(new OrderParam("enrolledAt", SortDirection.ASC)), params.getOrders());
  }

  @Test
  void testOrderFailsForNonSimpleEventProperty() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setOrder(OrderCriteria.fromOrderString("nonSimple:desc"));

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertStartsWith("Order by property `nonSimple` is not supported", exception.getMessage());
  }

  @Test
  void testOrderFailsForUnsupportedProperty() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setOrder(
        OrderCriteria.fromOrderString(
            "unsupportedProperty1:asc,enrolledAt:asc,unsupportedProperty2:desc"));

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertAll(
        () -> assertStartsWith("Order by property `", exception.getMessage()),
        // order of properties might not always be the same; therefore using
        // contains
        () -> assertContains("unsupportedProperty1", exception.getMessage()),
        () -> assertContains("unsupportedProperty2", exception.getMessage()));
  }

  @Test
  void testFilter() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilter(DE_1_UID + ":eq:2" + "," + DE_2_UID + ":like:foo");

    EventQueryParams params = mapper.map(criteria);

    List<QueryItem> items = params.getFilters();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(DE_1_UID, DE_2_UID),
        items.stream().map(i -> i.getItem().getUid()).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    // the following block is needed because of that
    // assertion is order independent as the order of QueryItems is not
    // guaranteed
    Map<String, QueryFilter> expectedFilters =
        Map.of(
            DE_1_UID, new QueryFilter(QueryOperator.EQ, "2"),
            DE_2_UID, new QueryFilter(QueryOperator.LIKE, "foo"));
    assertAll(
        items.stream()
            .map(
                i ->
                    (Executable)
                        () -> {
                          String uid = i.getItem().getUid();
                          QueryFilter expected = expectedFilters.get(uid);
                          assertEquals(
                              expected.getOperator().getValue() + " " + expected.getFilter(),
                              i.getFiltersAsString(),
                              () -> String.format("QueryFilter mismatch for DE with UID %s", uid));
                        })
            .collect(Collectors.toList()));
  }

  @Test
  void testFilterAttributes() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilterAttributes(TEA_1_UID + ":eq:2" + "," + TEA_2_UID + ":like:foo");

    EventQueryParams params = mapper.map(criteria);

    List<QueryItem> items = params.getFilterAttributes();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(TEA_1_UID, TEA_2_UID),
        items.stream().map(i -> i.getItem().getUid()).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    // the following block is needed because of that
    // assertion is order independent as the order of QueryItems is not
    // guaranteed
    Map<String, QueryFilter> expectedFilters =
        Map.of(
            TEA_1_UID, new QueryFilter(QueryOperator.EQ, "2"),
            TEA_2_UID, new QueryFilter(QueryOperator.LIKE, "foo"));
    assertAll(
        items.stream()
            .map(
                i ->
                    (Executable)
                        () -> {
                          String uid = i.getItem().getUid();
                          QueryFilter expected = expectedFilters.get(uid);
                          assertEquals(
                              expected.getOperator().getValue() + " " + expected.getFilter(),
                              i.getFiltersAsString(),
                              () -> String.format("QueryFilter mismatch for TEA with UID %s", uid));
                        })
            .collect(Collectors.toList()));
  }

  @Test
  void testFilterWhenDEHasMultipleFilters() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilter(DE_1_UID + ":gt:10:lt:20");

    EventQueryParams params = mapper.map(criteria);

    List<QueryItem> items = params.getFilters();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(DE_1_UID),
        items.stream().map(i -> i.getItem().getUid()).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    assertContainsOnly(
        Set.of(new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")),
        items.get(0).getFilters());
  }

  @Test
  void testFilterAttributesWhenTEAHasMultipleFilters()
      throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilterAttributes(TEA_1_UID + ":gt:10:lt:20");

    EventQueryParams params = mapper.map(criteria);

    List<QueryItem> items = params.getFilterAttributes();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(TEA_1_UID),
        items.stream().map(i -> i.getItem().getUid()).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    assertContainsOnly(
        Set.of(new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")),
        items.get(0).getFilters());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenCriteriaDataElementDoesNotExist() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    String filterName = "filter";
    criteria.setFilter(filterName);
    when(dataElementService.getDataElement(filterName)).thenReturn(null);

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));

    assertEquals("Dataelement does not exist: " + filterName, exception.getMessage());
  }

  @Test
  void testFilterAttributesWhenTEAUidIsDuplicated() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilterAttributes(
        "TvjwTPToKHO:lt:20"
            + ","
            + "cy2oRh2sNr6:lt:20"
            + ","
            + "TvjwTPToKHO:gt:30"
            + ","
            + "cy2oRh2sNr6:gt:30");

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(criteria));
    assertAll(
        () ->
            assertStartsWith(
                "filterAttributes contains duplicate tracked entity attribute",
                exception.getMessage()),
        // order of TEA UIDs might not always be the same; therefore using
        // contains
        () -> assertContains(TEA_1_UID, exception.getMessage()),
        () -> assertContains(TEA_2_UID, exception.getMessage()));
  }

  @Test
  void testFilterAttributesUsingOnlyUID() throws BadRequestException, ForbiddenException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilterAttributes(TEA_1_UID);

    EventQueryParams params = mapper.map(criteria);

    assertContainsOnly(
        List.of(
            new QueryItem(
                tea1,
                null,
                tea1.getValueType(),
                tea1.getAggregationType(),
                tea1.getOptionSet(),
                tea1.isUnique())),
        params.getFilterAttributes());
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgram() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgram(program.getUid());
    User user = new User();
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(aclService.canDataRead(user, program)).thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(criteria));

    assertEquals("User has no access to program: " + program.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgramStage() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setProgramStage(programStage.getUid());
    User user = new User();
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(aclService.canDataRead(user, programStage)).thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(criteria));

    assertEquals(
        "User has no access to program stage: " + programStage.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToCategoryCombo() {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setAttributeCc("Cc");
    criteria.setAttributeCos("Cos");
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setUid("uid");
    when(inputUtils.getAttributeOptionCombo(
            criteria.getAttributeCc(), criteria.getAttributeCos(), true))
        .thenReturn(combo);
    when(aclService.canDataRead(any(User.class), any(CategoryOptionCombo.class))).thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(criteria));

    assertEquals(
        "User has no access to attribute category option combo: " + combo.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldCreateQueryFilterWhenCriteriaHasMultipleFiltersAndFilterValueWithSplitChars()
      throws ForbiddenException, BadRequestException {
    TrackerEventCriteria criteria = new TrackerEventCriteria();
    criteria.setFilterAttributes(
        TEA_1_UID + ":like:value/,with/,comma" + "," + TEA_2_UID + ":eq:value/:x");

    List<QueryFilter> actualFilters =
        mapper.map(criteria).getFilterAttributes().stream()
            .flatMap(f -> f.getFilters().stream())
            .collect(Collectors.toList());

    assertContainsOnly(
        List.of(
            new QueryFilter(QueryOperator.LIKE, "value,with,comma"),
            new QueryFilter(QueryOperator.EQ, "value:x")),
        actualFilters);
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldMapRequestedOrgUnitWhenProgramProtected(OrganisationUnitSelectionMode orgUnitMode)
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(PROTECTED);
    program.setUid(PROGRAM_UID);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid3");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);
    when(organisationUnitService.isInUserHierarchy(
            orgUnitId, user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnitId);
    eventCriteria.setOuMode(orgUnitMode);

    EventQueryParams searchParams = mapper.map(eventCriteria);

    assertEquals(orgUnitId, searchParams.getOrgUnit().getUid());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldMapSearchScopeOrgUnitWhenProgramOpen(OrganisationUnitSelectionMode orgUnitMode)
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);
    when(organisationUnitService.isInUserHierarchy(
            orgUnitId, user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    EventQueryParams searchParams = mapper.map(eventCriteria);

    assertEquals(orgUnitId, searchParams.getOrgUnit().getUid());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenProgramProtectedAndUserHasNoAccessToSearchScopeOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenProgramOpenAndUserHasNoAccessToSearchScopeOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldMapRequestedOrgUnitAsSelectedWhenOrgUnitProvidedAndNoOrgUnitModeProvided()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    User user = new User();
    user.setOrganisationUnits(Set.of(orgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(trackerAccessManager.canAccess(user, program, orgUnit)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnitId, user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    EventQueryParams searchParams = mapper.map(eventCriteria);

    assertEquals(SELECTED, searchParams.getOrgUnitSelectionMode());
    assertEquals(orgUnit, searchParams.getOrgUnit());
  }

  @Test
  void shouldMapOrgUnitModeAccessibleWhenNoOrgUnitProvidedAndNoOrgUnitModeProvided()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());

    EventQueryParams searchParams = mapper.map(eventCriteria);

    assertEquals(ACCESSIBLE, searchParams.getOrgUnitSelectionMode());
    assertNull(searchParams.getOrgUnit());
  }

  @Test
  void shouldFailWhenNoOuModeSpecifiedAndUserHasNoAccessToOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(CLOSED);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeNeedsOrgUnitAndNoOrgUnitProvided(OrganisationUnitSelectionMode mode) {
    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setOuMode(mode);

    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is required for org unit mode: " + mode, exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"CAPTURE", "ACCESSIBLE", "ALL"})
  void shouldPassWhenOuModeDoesNotNeedOrgUnitAndOrgUnitProvided(
      OrganisationUnitSelectionMode mode) {
    when(currentUserService.getCurrentUser()).thenReturn(createSearchInAllOrgUnitsUser());

    TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
    eventCriteria.setOuMode(mode);

    assertDoesNotThrow(() -> mapper.map(eventCriteria));
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }

  private User createSearchInAllOrgUnitsUser() {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(
        Set.of(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));
    user.setOrganisationUnits(Set.of(orgUnit));

    return user;
  }
}
