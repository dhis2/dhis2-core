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
package org.hisp.dhis.analytics.event.data.queryItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionConstants.ITEM_SEP;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.hisp.dhis.test.TestBase.createOptionSet;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramIndicator;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.hisp.dhis.test.TestBase.createProgramStageDataElement;
import static org.hisp.dhis.test.TestBase.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.test.TestBase.createTrackedEntityAttribute;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.data.DefaultQueryItemLocator;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class QueryItemLocatorTest {
  @Mock private ProgramStageService programStageService;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private DataQueryService dataQueryService;

  @Mock private LegendSetService legendSetService;

  @Mock private RelationshipTypeService relationshipTypeService;

  @InjectMocks private DefaultQueryItemLocator subject;

  private Program programA;

  private String dimension;

  private String programStageUid;

  @BeforeEach
  public void setUp() {
    programA = createProgram('A');

    dimension = CodeGenerator.generateUid();
    programStageUid = CodeGenerator.generateUid();
  }

  @Test
  void verifyDynamicDimensionsDoesNotThrowException() {
    String dimension = "dynamicDimension";

    when(dataQueryService.getDimension(
            dimension,
            Collections.emptyList(),
            (Date) null,
            Collections.emptyList(),
            true,
            null,
            IdScheme.UID))
        .thenReturn(new BaseDimensionalObject(dimension));

    assertDoesNotThrow(() -> subject.getQueryItemFromDimension(dimension, programA, null));
  }

  @Test
  void verifyExceptionOnEmptyDimension() {
    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension("", programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyExceptionOnEmptyProgram() {
    assertThrows(
        NullPointerException.class,
        () -> subject.getQueryItemFromDimension(dimension, null, EventOutputType.ENROLLMENT),
        "Program can not be null");
  }

  @Test
  void verifyDimensionReturnsDataElementForEventQuery() {
    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Set.of(programStageA));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.EVENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
  }

  @Test
  void getQueryItemFromDimensionThrowsRightExceptionWhenElementDoesNotBelongToProgram() {
    DataElement iBelongDataElement = createDataElement('A');
    ProgramStage programStageA = createProgramStage('A', programA);
    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, iBelongDataElement, 1)));
    programA.setProgramStages(Set.of(programStageA));

    DataElement iDontBelongDataElement = createDataElement('B');
    when(dataElementService.getDataElement(dimension)).thenReturn(iDontBelongDataElement);

    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension(dimension, programA, EventOutputType.EVENT));
  }

  @Test
  void verifyDimensionFailsWhenProgramStageIsMissingForEnrollmentQuery() {
    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Set.of(programStageA));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);

    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT),
        "Program stage is mandatory for data element dimensions in enrollment analytics queries");
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithStartIndex() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[-1]" + DIMENSION_IDENTIFIER_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionWithLegendSetReturnsDataElement() {
    String legendSetUid = CodeGenerator.generateUid();

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Set.of(programStageA));

    LegendSet legendSetA = createLegendSet('A');

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);
    when(legendSetService.getLegendSet(legendSetUid)).thenReturn(legendSetA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            dimension + ITEM_SEP + legendSetUid, programA, EventOutputType.EVENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(legendSetA));
  }

  @Test
  void verifyDimensionWithLegendSetAndProgramStageReturnsDataElement() {
    String legendSetUid = CodeGenerator.generateUid();

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Set.of(programStageA));

    LegendSet legendSetA = createLegendSet('A');

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);
    when(legendSetService.getLegendSet(legendSetUid)).thenReturn(legendSetA);
    when(programStageService.getProgramStage(programStageUid)).thenReturn(programStageA);

    // programStageUid.dimensionUid-legendSetUid
    int stageOffset = -1256;
    String withStageOffset = "[" + stageOffset + "]";
    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid
                + withStageOffset
                + DIMENSION_IDENTIFIER_SEP
                + dimension
                + ITEM_SEP
                + legendSetUid,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
    assertThat(queryItem.getLegendSet(), is(legendSetA));
    assertThat(queryItem.getProgramStageOffset(), is(stageOffset));

    verifyNoMoreInteractions(attributeService);
    verifyNoMoreInteractions(programIndicatorService);
  }

  @Test
  void verifyDimensionReturnsTrackedEntityAttribute() {
    OptionSet optionSetA = createOptionSet('A');

    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setUid(dimension);
    trackedEntityAttribute.setOptionSet(optionSetA);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(attributeService.getTrackedEntityAttribute(dimension)).thenReturn(trackedEntityAttribute);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(trackedEntityAttribute));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(nullValue()));
    assertThat(queryItem.getOptionSet(), is(optionSetA));
    verifyNoMoreInteractions(programIndicatorService);
  }

  @Test
  void verifyDimensionReturnsProgramIndicator() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    programA.setProgramIndicators(Set.of(programIndicatorA));
    when(programIndicatorService.getProgramIndicatorByUid(programIndicatorA.getUid()))
        .thenReturn(programIndicatorA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(nullValue()));
  }

  @Test
  void verifyDimensionReturnsProgramIndicatorWithRelationship() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    RelationshipType relationshipType = createRelationshipType();

    programA.setProgramIndicators(Set.of(programIndicatorA));
    when(programIndicatorService.getProgramIndicatorByUid(programIndicatorA.getUid()))
        .thenReturn(programIndicatorA);
    when(relationshipTypeService.getRelationshipType(relationshipType.getUid()))
        .thenReturn(relationshipType);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            relationshipType.getUid() + DIMENSION_IDENTIFIER_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(nullValue()));
    assertThat(queryItem.getRelationshipType(), is(relationshipType));
  }

  @Test
  void verifyForeignProgramIndicatorWithoutRelationshipIsNotAccepted() {

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    when(programIndicatorService.getProgramIndicatorByUid(programIndicatorA.getUid()))
        .thenReturn(programIndicatorA);

    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyForeignProgramIndicatorWithRelationshipIsAccepted() {

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    RelationshipType relationshipType = createRelationshipType();
    when(programIndicatorService.getProgramIndicatorByUid(programIndicatorA.getUid()))
        .thenReturn(programIndicatorA);
    when(relationshipTypeService.getRelationshipType(relationshipType.getUid()))
        .thenReturn(relationshipType);
    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            relationshipType.getUid() + DIMENSION_IDENTIFIER_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(nullValue()));
    assertThat(queryItem.getRelationshipType(), is(relationshipType));
  }

  @Test
  void verifyDimensionReturnsEventDateQueryItem() {
    ProgramStage programStageA = createProgramStage('A', programA);
    programA.setProgramStages(Set.of(programStageA));
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageA.getUid() + DIMENSION_IDENTIFIER_SEP + "EVENT_DATE",
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItemId(), is(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    assertThat(queryItem.getValueType(), is(ValueType.DATE));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsScheduledDateQueryItem() {
    ProgramStage programStageA = createProgramStage('A', programA);
    programA.setProgramStages(Set.of(programStageA));
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageA.getUid() + DIMENSION_IDENTIFIER_SEP + "SCHEDULED_DATE",
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItemId(), is(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME));
    assertThat(queryItem.getValueType(), is(ValueType.DATE));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsEventStatusQueryItem() {
    ProgramStage programStageA = createProgramStage('A', programA);
    programA.setProgramStages(Set.of(programStageA));
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageA.getUid() + DIMENSION_IDENTIFIER_SEP + "EVENT_STATUS",
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItemId(), is(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));
    assertThat(queryItem.getValueType(), is(ValueType.TEXT));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsProgramStageOrgUnitQueryItem() {
    ProgramStage programStageA = createProgramStage('A', programA);
    programA.setProgramStages(Set.of(programStageA));
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageA.getUid() + DIMENSION_IDENTIFIER_SEP + "ou",
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItemId(), is(EventAnalyticsColumnName.OU_COLUMN_NAME));
    assertThat(queryItem.getValueType(), is(ValueType.ORGANISATION_UNIT));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyEventDateWithoutProgramStageThrows() {
    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension("EVENT_DATE", programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyScheduledDateWithoutProgramStageThrows() {
    assertThrows(
        IllegalQueryException.class,
        () ->
            subject.getQueryItemFromDimension(
                "SCHEDULED_DATE", programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyEventStatusWithoutProgramStageThrows() {
    assertThrows(
        IllegalQueryException.class,
        () ->
            subject.getQueryItemFromDimension("EVENT_STATUS", programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyOuWithoutProgramStageThrows() {
    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension("ou", programA, EventOutputType.ENROLLMENT),
        "Item identifier does not reference any data element, attribute or indicator part of the program");
  }

  @Test
  void verifyDimensionReturnsTrackedEntityAttributeWithLegendSet() {
    String legendSetUid = CodeGenerator.generateUid();

    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setUid(dimension);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    LegendSet legendSetA = createLegendSet('A');

    when(attributeService.getTrackedEntityAttribute(dimension)).thenReturn(trackedEntityAttribute);
    when(legendSetService.getLegendSet(legendSetUid)).thenReturn(legendSetA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            dimension + ITEM_SEP + legendSetUid, programA, EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(trackedEntityAttribute));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getLegendSet(), is(legendSetA));
    assertThat(queryItem.getValueType(), is(ValueType.TEXT));
  }

  @Test
  void verifyDimensionReturnsTrackedEntityAttributeWithProgramStage() {
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setUid(dimension);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    ProgramStage programStageA = createProgramStage('A', programA);
    programA.setProgramStages(Set.of(programStageA));

    when(attributeService.getTrackedEntityAttribute(dimension)).thenReturn(trackedEntityAttribute);
    when(programStageService.getProgramStage(programStageUid)).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + DIMENSION_IDENTIFIER_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(trackedEntityAttribute));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDataElementTakesPrecedenceOverTrackedEntityAttribute() {
    // When both a DataElement and TrackedEntityAttribute exist with the same dimension,
    // DataElement should be resolved first
    DataElement dataElementA = createDataElement('A');
    dataElementA.setUid(dimension);

    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setUid(dimension);

    ProgramStage programStageA = createProgramStage('A', programA);
    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);

    programA.setProgramStages(Set.of(programStageA));
    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);
    // attributeService should not be called since DataElement is found first

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.EVENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));

    verifyNoMoreInteractions(attributeService);
    verifyNoMoreInteractions(programIndicatorService);
  }

  @Test
  void verifyTrackedEntityAttributeTakesPrecedenceOverProgramIndicator() {
    // When both a TrackedEntityAttribute and ProgramIndicator exist,
    // TrackedEntityAttribute should be resolved first
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setUid(dimension);

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));
    programA.setProgramIndicators(Set.of(programIndicatorA));

    when(attributeService.getTrackedEntityAttribute(dimension)).thenReturn(trackedEntityAttribute);
    // programIndicatorService should not be called since TEA is found first

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(trackedEntityAttribute));

    verifyNoMoreInteractions(programIndicatorService);
  }

  @Test
  void verifyProgramIndicatorTakesPrecedenceOverEventDate() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    programA.setProgramIndicators(Set.of(programIndicatorA));

    when(programIndicatorService.getProgramIndicatorByUid(dimension)).thenReturn(programIndicatorA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
  }

  @Test
  void verifyProgramIndicatorWithProgramStage() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");
    programIndicatorA.setUid(dimension);

    ProgramStage programStageA = createProgramStage('A', programA);

    programA.setProgramIndicators(Set.of(programIndicatorA));
    programA.setProgramStages(Set.of(programStageA));

    when(programIndicatorService.getProgramIndicatorByUid(dimension)).thenReturn(programIndicatorA);
    when(programStageService.getProgramStage(programStageUid)).thenReturn(programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + DIMENSION_IDENTIFIER_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyInvalidProgramStageDimensionThrows() {
    String invalidProgramStageUid = CodeGenerator.generateUid();

    assertThrows(
        IllegalQueryException.class,
        () ->
            subject.getQueryItemFromDimension(
                invalidProgramStageUid + DIMENSION_IDENTIFIER_SEP + dimension,
                programA,
                EventOutputType.ENROLLMENT));
  }

  @Test
  void verifyDataElementWithOptionSet() {
    DataElement dataElementA = createDataElement('A');
    OptionSet optionSetA = createOptionSet('A');
    dataElementA.setOptionSet(optionSetA);

    ProgramStage programStageA = createProgramStage('A', programA);
    programStageA.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Set.of(programStageA));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElementA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(dimension, programA, EventOutputType.EVENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getOptionSet(), is(optionSetA));
  }

  private RelationshipType createRelationshipType() {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid(CodeGenerator.generateUid());
    return relationshipType;
  }

  private void configureDimensionForQueryItem(DataElement dataElement, ProgramStage programStage) {
    programStage.setProgramStageDataElements(
        Set.of(createProgramStageDataElement(programStage, dataElement, 1)));

    programA.setProgramStages(Set.of(programStage));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElement);

    when(programStageService.getProgramStage(programStageUid)).thenReturn(programStage);
  }
}
