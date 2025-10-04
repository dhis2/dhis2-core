/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.webapi.fields.FieldsConverter.PRESETS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.jsontree.JsonDiff.Mode;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsPropertyFilter;
import org.hisp.dhis.tracker.export.fieldfiltering.SchemaFieldsPresets;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.controller.tracker.view.RelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * This test ensures that simple POJOs like Tracker view classes can be serialized and field
 * filtered to JSON by Jackson. This test makes sure the filters Spring/Jackson configuration works
 * and that the tracker field filtering is backwards compatible with the current {@link
 * FieldFilterParser} and {@link FieldFilterService}.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FieldFilterSerializationTest extends H2ControllerIntegrationTestBase {
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final Instant DATE = Instant.parse("2023-03-15T14:30:45Z");

  @Autowired private FieldFilterService fieldFilterService;

  // use primary ObjectMapper from JacksonObjectMapperConfig to serialize the current ObjectNode to
  // a JSON string
  @Autowired private ObjectMapper objectMapper;

  // use the filter ObjectMapper from FieldsConfig to serialize, filter and transform an Object to a
  // JSON string
  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper filterMapper;

  @Autowired private SchemaService schemaService;
  @Autowired private SchemaFieldsPresets schemaFieldsPresets;

  private List<OrganisationUnit> organisationUnits;
  private Schema organisationUnitSchema;

  private List<Event> events;
  private Schema eventSchema;

  @BeforeAll
  void setUp() {
    organisationUnits = createOrganisationUnits(2);
    organisationUnitSchema = schemaService.getDynamicSchema(organisationUnits.get(0).getClass());

    events = createEvents(2);
    eventSchema = schemaService.getDynamicSchema(events.get(0).getClass());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        ":all",
        "!*",
        "!:all",
        ":simple",
        ":identifiable",
        ":nameable",
        ":owner",
        ":persisted",

        // Basic reference expansion tests
        "dataSets",
        "users",
        "groups",
        "dataSets,users",
        "dataSets,users,groups",

        // Explicit specification tests (should NOT expand)
        "dataSets[*]",
        "dataSets[id]",
        "dataSets[id,name]",
        "users[*]",
        "dataSets[id],users",

        // Exclusion + Expansion interactions
        "*,!dataSets",
        "*,!users",
        "dataSets[!id]",
        "users[!id],dataSets",
        "*,dataSets[!id]",

        // Execution order tests (preset + expansion)
        ":identifiable,dataSets",
        ":owner,users",
        ":simple,dataSets[id]",
        ":all,!dataSets",
        "dataSets,:identifiable",

        // Double-expansion prevention
        "dataSets[*],dataSets[id]",
        "dataSets,dataSets[id]",
        "dataSets[id],dataSets[id]",

        // Order dependency verification
        "*,!dataSets,dataSets[id]",

        // Nested child expansion tests
        "dataSets[organisationUnits]",
        "users[organisationUnits]",
        "groups[organisationUnits]",
        "dataSets[organisationUnits],users[organisationUnits]"
      })
  void trackerFilterShouldMatchCurrentFilterOnMetadata(String fields)
      throws JsonProcessingException {
    String actualCurrent = serializeUsingCurrentFilter(organisationUnits, fields);
    String actualTracker =
        serializeUsingTrackerFilter(organisationUnits, fields, organisationUnitSchema);

    assertEquals(actualCurrent, actualTracker);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        ":all",
        "!*",
        "!:all", // should this be invalid? the exclusion is ignored and the preset is applied
        ":simple",
        "!event",
        "event,dataValues",
        "event,!dataValues",
        "!dataValues",
        "!dataValues[]", // behaves like !dataValues
        // "!dataValues[value]", // bug in current field filter NPE Cannot invoke
        // "org.hisp.dhis.fieldfiltering.FieldPath.getPath()" because "fieldPath" is null
        // behaves like !dataValues in tracker filter
        "dataValues,!dataValues",
        "!dataValues,dataValues",
        // this is opening a block without a field is a 400 in tracker filter but ignored in the
        // current one
        // http://localhost:8080/api/organisationUnits?pageSize=1&fields=[id]
        // "[value]",
        "event,!dataValues,*",
        "dataValues[!value]",
        "dataValues, ,dataValues[!value], ",
        "dataValues,dataValues[!value,value)",
        "dataValues,dataValues[value]",
        "*,dataValues[value]",
        "dataValues[value]",
        "dataValues[value",
        "dataValues[dataElement,!value]",
        "dataValues::rename(values)[dataElement,!value]",
        "event,*,dataValues[!value]",
        "event,dataValues[dataElement,value]",
        "event,dataValues[*,!storedBy]",
        "event,dataValues[:all,!storedBy]",
        "event,dataValues[!*,!storedBy]",
        "event,dataValues[:simple,!storedBy]",
        "event,dataValues[:simple,!storedBy,:all]",
        "event,dataValues[!:all,!storedBy]",
        "*,!enrollment",
        "relationships,relationships[from]",
        "relationships[!from]",
        "relationships[  ]",
        "relationships[relationship,unknownfield]",
        // The current filter returns all if that field while the tracker filter returns {} as the
        // field
        // does not exist. The current behavior makes no sense as this suggests to a user that their
        // fields
        // input was correct.
        // "relationships[unknownfield]",
        "relationships[!unknownfield]",
        "relationships[f rom[trackedEntity[ org Unit ]",
        "relationships[from[trackedEntity[ :simple ]",

        // Nested child expansion tests - ensure child paths get expanded recursively
        "relationships[from]",
        "relationships[to]",
        "relationships[from[trackedEntity]]",
        "relationships[to[event]]",
        "relationships[from[trackedEntity],to[enrollment]]",
        // transformations
        "dataValues~isEmpty",
        "dataValues|isEmpty",
        "dataValues::isEmpty",
        "notes::isEmpty",
        "event::isEmpty",
        "dataValues::isNotEmpty",
        "notes::isNotEmpty",
        "event::isNotEmpty",
        "dataValues|rename(hasDataValues)~isEmpty", // rename must be applied last
        "notes[:all,value~rename(text)]",
        "notes::size",
        "event::size",
        "relationships[bidirectional::size]",
        "dataValues::pluck",
        "dataValues::pluck(value)",
        "event::pluck",
        // tracker does not have the field id which the key defaults to so this leads to {}
        "dataValues~keyBy[dataElement,value]",
        "dataValues~keyBy(dataElement)[dataElement,value]",
        "dataValues~keyBy(dataElement)[!value,:all]",
        // filtering is done before the transformation so this leads to {} as the key is filtered
        // out
        "dataValues~keyBy(dataElement)[!dataElement]",
        "event::keyBy",
      })
  void trackerFilterShouldMatchCurrentFilterOnSimplePojo(String fields)
      throws JsonProcessingException {
    String actualCurrent = serializeUsingCurrentFilter(events, fields);
    String actualTracker = serializeUsingTrackerFilter(events, fields, eventSchema);

    assertEquals(actualCurrent, actualTracker);
  }

  /**
   * Test rename transformations where tracker filter maintains natural field order while current
   * filter ({@link FieldFilterService}) does not.
   */
  @ParameterizedTest
  @ValueSource(strings = {"*,event::rename(nonEvent)", "event::rename(nonEvent)"})
  void trackerFilterShouldMatchCurrentFilterIgnoringFieldOrder(String fields)
      throws JsonProcessingException {
    String actualCurrent = serializeUsingCurrentFilter(events, fields);
    String actualTracker = serializeUsingTrackerFilter(events, fields, eventSchema);

    // Use JsonDiff with LENIENT mode to ignore field order differences
    assertNoDiff(actualCurrent, actualTracker, Mode.LENIENT);
  }

  private <T> String serializeUsingCurrentFilter(List<T> objects, String fields)
      throws JsonProcessingException {
    List<FieldPath> filter = FieldFilterParser.parse(fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(objects, filter);
    return objectMapper.writeValueAsString(objectNodes);
  }

  private <T> String serializeUsingTrackerFilter(List<T> objects, String fieldsInput, Schema schema)
      throws JsonProcessingException {
    Fields fields =
        FieldsParser.parse(fieldsInput, schema, schemaFieldsPresets::getSchema, PRESETS);
    return filterMapper
        .writer()
        .withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, fields)
        .writeValueAsString(objects);
  }

  static List<OrganisationUnit> createOrganisationUnits(int n) {
    List<OrganisationUnit> orgUnits = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      orgUnits.add(createOrganisationUnit((char) ('A' + i)));
    }
    return orgUnits;
  }

  public static OrganisationUnit createOrganisationUnit(char uniqueCharacter) {
    OrganisationUnit unit = new OrganisationUnit();
    unit.setAutoFields();
    unit.setUid(UID.generate().getValue());
    unit.setName("OrganisationUnit" + uniqueCharacter);
    unit.setShortName("OrganisationUnitShort" + uniqueCharacter);
    unit.setCode("OrganisationUnitCode" + uniqueCharacter);
    unit.setOpeningDate(java.util.Date.from(DATE));
    unit.setComment("Comment" + uniqueCharacter);
    unit.setGeometry(GEOMETRY_FACTORY.createPoint(new Coordinate(4, 12)));
    unit.setDescription("Description for OrganisationUnit " + uniqueCharacter);
    unit.setEmail("orgunit" + uniqueCharacter + "@example.com");
    unit.setPhoneNumber("+123456789" + uniqueCharacter);
    unit.setAddress("Address " + uniqueCharacter);
    unit.setContactPerson("Contact Person " + uniqueCharacter);
    unit.setUrl("https://example.com/orgunit" + uniqueCharacter);

    // Add reference/complex objects for nested expansion testing
    DataSet dataSet1 = new DataSet();
    dataSet1.setAutoFields();
    dataSet1.setUid(UID.generate().getValue());
    dataSet1.setName("DataSet" + uniqueCharacter + "1");
    dataSet1.setCode("DS" + uniqueCharacter + "1");

    DataSet dataSet2 = new DataSet();
    dataSet2.setAutoFields();
    dataSet2.setUid(UID.generate().getValue());
    dataSet2.setName("DataSet" + uniqueCharacter + "2");
    dataSet2.setCode("DS" + uniqueCharacter + "2");

    unit.setDataSets(Set.of(dataSet1, dataSet2));

    org.hisp.dhis.user.User user1 = new org.hisp.dhis.user.User();
    user1.setAutoFields();
    user1.setUid(UID.generate().getValue());
    user1.setUsername("user" + uniqueCharacter + "1");
    user1.setFirstName("User" + uniqueCharacter + "1");

    org.hisp.dhis.user.User user2 = new org.hisp.dhis.user.User();
    user2.setAutoFields();
    user2.setUid(UID.generate().getValue());
    user2.setUsername("user" + uniqueCharacter + "2");
    user2.setFirstName("User" + uniqueCharacter + "2");

    unit.setUsers(Set.of(user1, user2));

    OrganisationUnitGroup group1 = new OrganisationUnitGroup();
    group1.setAutoFields();
    group1.setUid(UID.generate().getValue());
    group1.setName("Group" + uniqueCharacter + "1");
    group1.setCode("GRP" + uniqueCharacter + "1");

    OrganisationUnitGroup group2 = new OrganisationUnitGroup();
    group2.setAutoFields();
    group2.setUid(UID.generate().getValue());
    group2.setName("Group" + uniqueCharacter + "2");
    group2.setCode("GRP" + uniqueCharacter + "2");

    unit.setGroups(Set.of(group1, group2));

    unit.updatePath();
    return unit;
  }

  static List<Event> createEvents(int n) {
    List<Event> events = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      events.add(createEvent());
    }
    return events;
  }

  private static Event createEvent() {
    return Event.builder()
        .event(UID.generate())
        .program(UID.generate().getValue())
        .programStage(UID.generate().getValue())
        .enrollment(UID.generate())
        .trackedEntity(UID.generate())
        .orgUnit(UID.generate().getValue())
        .relationships(
            List.of(
                Relationship.builder()
                    .relationship(UID.generate())
                    .relationshipName("Mother-Child")
                    .relationshipType(UID.generate().getValue())
                    .createdAt(DATE)
                    .bidirectional(false)
                    .from(
                        RelationshipItem.builder()
                            .trackedEntity(
                                RelationshipItem.TrackedEntity.builder()
                                    .trackedEntity(UID.generate())
                                    .trackedEntityType(UID.generate().getValue())
                                    .createdAt(DATE)
                                    .orgUnit(UID.generate().getValue())
                                    .build())
                            .build())
                    .to(
                        RelationshipItem.builder()
                            .trackedEntity(
                                RelationshipItem.TrackedEntity.builder()
                                    .trackedEntity(UID.generate())
                                    .trackedEntityType(UID.generate().getValue())
                                    .createdAt(DATE)
                                    .orgUnit(UID.generate().getValue())
                                    .build())
                            .build())
                    .build(),
                Relationship.builder()
                    .relationship(UID.generate())
                    .relationshipName("Sibling")
                    .relationshipType(UID.generate().getValue())
                    .createdAt(DATE)
                    .bidirectional(true)
                    .from(
                        RelationshipItem.builder()
                            .event(
                                RelationshipItem.Event.builder()
                                    .event(UID.generate())
                                    .program(UID.generate().getValue())
                                    .programStage(UID.generate().getValue())
                                    .orgUnit(UID.generate().getValue())
                                    .occurredAt(DATE)
                                    .createdAt(DATE)
                                    .build())
                            .build())
                    .to(
                        RelationshipItem.builder()
                            .enrollment(
                                RelationshipItem.Enrollment.builder()
                                    .enrollment(UID.generate())
                                    .program(UID.generate().getValue())
                                    .orgUnit(UID.generate().getValue())
                                    .enrolledAt(DATE)
                                    .createdAt(DATE)
                                    .build())
                            .build())
                    .build()))
        .scheduledAt(DATE)
        .storedBy("fred")
        .followUp(true)
        .createdAt(DATE)
        .attributeOptionCombo(UID.generate().getValue())
        .attributeCategoryOptions(UID.generate().getValue())
        .geometry(GEOMETRY_FACTORY.createPoint(new Coordinate(4, 12)))
        .createdBy(
            User.builder()
                .uid(UID.generate().getValue())
                .username("fred")
                .displayName("Freddy")
                .build())
        .dataValues(
            Set.of(
                DataValue.builder()
                    .dataElement(UID.generate().getValue())
                    .value("14")
                    .storedBy("alice")
                    .build(),
                DataValue.builder()
                    .dataElement(UID.generate().getValue())
                    .value("78")
                    .storedBy("bob")
                    .build()))
        .notes(List.of(Note.builder().note(UID.generate()).value("lovely note").build()))
        .build();
  }
}
