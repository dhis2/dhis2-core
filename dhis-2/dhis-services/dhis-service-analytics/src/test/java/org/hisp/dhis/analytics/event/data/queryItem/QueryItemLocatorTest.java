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
package org.hisp.dhis.analytics.event.data.queryItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createLegendSet;
import static org.hisp.dhis.DhisConvenienceTest.createOptionSet;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStageDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityAttribute;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.PROGRAMSTAGE_SEP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Date;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.DefaultQueryItemLocator;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
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

  private QueryItemLocator subject;

  private Program programA;

  private String dimension;

  private String programStageUid;

  @BeforeEach
  public void setUp() {
    programA = createProgram('A');

    dimension = CodeGenerator.generateUid();
    programStageUid = CodeGenerator.generateUid();

    subject =
        new DefaultQueryItemLocator(
            programStageService,
            dataElementService,
            attributeService,
            programIndicatorService,
            legendSetService,
            relationshipTypeService,
            dataQueryService);
  }

  @Test
  void verifyDynamicDimensionsDoesntThrowException() {
    String dimension = "dynamicDimension";

    when(dataQueryService.getDimension(
            dimension,
            Collections.emptyList(),
            (Date) null,
            Collections.emptyList(),
            null,
            true,
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
        Sets.newHashSet(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Sets.newHashSet(programStageA));

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
    // Arrange
    DataElement iBelongDataElement = createDataElement('A');
    ProgramStage programStageA = createProgramStage('A', programA);
    programStageA.setProgramStageDataElements(
        Sets.newHashSet(createProgramStageDataElement(programStageA, iBelongDataElement, 1)));
    programA.setProgramStages(Sets.newHashSet(programStageA));

    DataElement iDontBelongDataElement = createDataElement('B');
    when(dataElementService.getDataElement(dimension)).thenReturn(iDontBelongDataElement);

    // Act
    // Assert
    assertThrows(
        IllegalQueryException.class,
        () -> subject.getQueryItemFromDimension(dimension, programA, EventOutputType.EVENT));
  }

  @Test
  void verifyDimensionFailsWhenProgramStageIsMissingForEnrollmentQuery() {
    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    programStageA.setProgramStageDataElements(
        Sets.newHashSet(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Sets.newHashSet(programStageA));

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
            programStageUid + "[-1]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithStartIndexAndCount() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[-1~1]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithStartIndexAndCountAndBothDates() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[-1~1~ 2022-01-01~ 2022-01-31]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void
      verifyDimensionReturnsDataElementForEnrollmentQueryWithStartIndexAndCountAndRelativePeriod() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[1~1~LAST_3_MONTHS]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));

    assertThrows(
        IllegalQueryException.class,
        () ->
            subject.getQueryItemFromDimension(
                programStageUid + "[-1~1~ LAST_A3_MONTHS]" + PROGRAMSTAGE_SEP + dimension,
                programA,
                EventOutputType.ENROLLMENT));
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithStartBothDates() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[2022-01-01~ 2022-01-31]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithRelativePeriod() {

    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[LAST_10_YEARS]" + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(dataElementA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(programStageA));

    assertThrows(
        IllegalQueryException.class,
        () ->
            subject.getQueryItemFromDimension(
                programStageUid + "[LAST_A3_MONTHS]" + PROGRAMSTAGE_SEP + dimension,
                programA,
                EventOutputType.ENROLLMENT));
  }

  @Test
  void verifyDimensionReturnsDataElementForEnrollmentQueryWithAsterisk() {
    DataElement dataElementA = createDataElement('A');

    ProgramStage programStageA = createProgramStage('A', programA);

    configureDimensionForQueryItem(dataElementA, programStageA);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            programStageUid + "[*]" + PROGRAMSTAGE_SEP + dimension,
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
        Sets.newHashSet(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Sets.newHashSet(programStageA));

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
        Sets.newHashSet(createProgramStageDataElement(programStageA, dataElementA, 1)));

    programA.setProgramStages(Sets.newHashSet(programStageA));

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
                + PROGRAMSTAGE_SEP
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

    programA.setProgramAttributes(Lists.newArrayList(programTrackedEntityAttribute));

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

    programA.setProgramIndicators(Sets.newHashSet(programIndicatorA));
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

    programA.setProgramIndicators(Sets.newHashSet(programIndicatorA));
    when(programIndicatorService.getProgramIndicatorByUid(programIndicatorA.getUid()))
        .thenReturn(programIndicatorA);
    when(relationshipTypeService.getRelationshipType(relationshipType.getUid()))
        .thenReturn(relationshipType);

    QueryItem queryItem =
        subject.getQueryItemFromDimension(
            relationshipType.getUid() + PROGRAMSTAGE_SEP + dimension,
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
            relationshipType.getUid() + PROGRAMSTAGE_SEP + dimension,
            programA,
            EventOutputType.ENROLLMENT);

    assertThat(queryItem, is(notNullValue()));
    assertThat(queryItem.getItem(), is(programIndicatorA));
    assertThat(queryItem.getProgram(), is(programA));
    assertThat(queryItem.getProgramStage(), is(nullValue()));
    assertThat(queryItem.getLegendSet(), is(nullValue()));
    assertThat(queryItem.getRelationshipType(), is(relationshipType));
  }

  private RelationshipType createRelationshipType() {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid(CodeGenerator.generateUid());
    return relationshipType;
  }

  private void configureDimensionForQueryItem(DataElement dataElement, ProgramStage programStage) {
    programStage.setProgramStageDataElements(
        Sets.newHashSet(createProgramStageDataElement(programStage, dataElement, 1)));

    programA.setProgramStages(Sets.newHashSet(programStage));

    when(dataElementService.getDataElement(dimension)).thenReturn(dataElement);

    when(programStageService.getProgramStage(programStageUid)).thenReturn(programStage);
  }
}
