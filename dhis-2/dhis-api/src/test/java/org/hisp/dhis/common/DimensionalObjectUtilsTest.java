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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.DimensionalObjectUtils.asActualDimension;
import static org.hisp.dhis.common.DimensionalObjectUtils.asBaseObjects;
import static org.hisp.dhis.common.DimensionalObjectUtils.asQualifiedDimension;
import static org.hisp.dhis.common.DimensionalObjectUtils.getQualifiedDimensions;
import static org.hisp.dhis.common.DimensionalObjectUtils.linkAssociations;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DimensionalObjectUtilsTest {

  @Test
  void testGetPrettyFilter() {
    assertEquals("< 5, = Discharged", DimensionalObjectUtils.getPrettyFilter("LT:5:EQ:Discharged"));
    assertEquals(">= 10, Female", DimensionalObjectUtils.getPrettyFilter("GE:10:LIKE:Female"));
    assertEquals(
        "> 20, Discharged, Transferred",
        DimensionalObjectUtils.getPrettyFilter("GT:20:IN:Discharged;Transferred"));
    assertEquals(null, DimensionalObjectUtils.getPrettyFilter(null));
    assertEquals(null, DimensionalObjectUtils.getPrettyFilter("uid"));
  }

  @Test
  void testIsCompositeDimensionObject() {
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.G142kJ2k3Gj"));
    assertTrue(
        DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.G142kJ2k3Gj.BoaSg2GopVn"));
    assertTrue(
        DimensionalObjectUtils.isCompositeDimensionalObject(
            "d4HjsAHkj42.G142kJ2k3Gj.BoaSg2GopVn.AGGREGATED"));
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.*.BoaSg2GopVn"));
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.G142kJ2k3Gj.*"));
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.*"));
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42.*.*"));
    assertTrue(DimensionalObjectUtils.isCompositeDimensionalObject("codeA.codeB"));
    assertFalse(DimensionalObjectUtils.isCompositeDimensionalObject("d4HjsAHkj42"));
    assertFalse(DimensionalObjectUtils.isCompositeDimensionalObject("14HjsAHkj42-G142kJ2k3Gj"));
  }

  @Test
  void testGetUidMapIsSchemeCode() {
    DataElement deA = new DataElement("NameA");
    DataElement deB = new DataElement("NameB");
    DataElement deC = new DataElement("NameC");
    deA.setUid("A123456789A");
    deB.setUid("A123456789B");
    deC.setUid("A123456789C");
    deA.setCode("CodeA");
    deB.setCode("CodeB");
    deC.setCode(null);
    List<DataElement> elements = Lists.newArrayList(deA, deB, deC);
    Map<String, String> map =
        DimensionalObjectUtils.getDimensionItemIdSchemeMap(elements, IdScheme.CODE);
    assertEquals(3, map.size());
    assertEquals("CodeA", map.get("A123456789A"));
    assertEquals("CodeB", map.get("A123456789B"));
    assertEquals(null, map.get("A123456789C"));
  }

  @Test
  void testGetUidMapIsSchemeCodeCompositeObject() {
    Program prA = new Program();
    prA.setUid("P123456789A");
    prA.setCode("PCodeA");
    DataElement deA = new DataElement("NameA");
    DataElement deB = new DataElement("NameB");
    deA.setUid("D123456789A");
    deB.setUid("D123456789B");
    deA.setCode("DCodeA");
    deB.setCode("DCodeB");
    ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem(prA, deA);
    ProgramDataElementDimensionItem pdeB = new ProgramDataElementDimensionItem(prA, deB);
    List<ProgramDataElementDimensionItem> elements = Lists.newArrayList(pdeA, pdeB);
    Map<String, String> map =
        DimensionalObjectUtils.getDimensionItemIdSchemeMap(elements, IdScheme.CODE);
    assertEquals(2, map.size());
    assertEquals("PCodeA.DCodeA", map.get("P123456789A.D123456789A"));
    assertEquals("PCodeA.DCodeB", map.get("P123456789A.D123456789B"));
  }

  @Test
  void testGetUidMapIsSchemeAttribute() {
    DataElement deA = new DataElement("DataElementA");
    DataElement deB = new DataElement("DataElementB");
    DataElement deC = new DataElement("DataElementC");
    deA.setUid("A123456789A");
    deB.setUid("A123456789B");
    deC.setUid("A123456789C");
    Attribute atA = new Attribute("AttributeA", ValueType.INTEGER);
    atA.setUid("ATTR123456A");
    deA.setAttributeValues(AttributeValues.empty().added(atA.getUid(), "AttributeValueA"));
    deB.setAttributeValues(AttributeValues.empty().added(atA.getUid(), "AttributeValueB"));
    List<DataElement> elements = Lists.newArrayList(deA, deB, deC);
    String scheme = IdScheme.ATTR_ID_SCHEME_PREFIX + atA.getUid();
    IdScheme idScheme = IdScheme.from(scheme);
    Map<String, String> map =
        DimensionalObjectUtils.getDimensionItemIdSchemeMap(elements, idScheme);
    assertEquals(3, map.size());
    assertEquals("AttributeValueA", map.get("A123456789A"));
    assertEquals("AttributeValueB", map.get("A123456789B"));
    assertEquals(null, map.get("A123456789C"));
  }

  @Test
  void testGetDataElementOperandIdSchemeCodeMap() {
    DataElement deA = new DataElement("NameA");
    DataElement deB = new DataElement("NameB");
    deA.setUid("D123456789A");
    deB.setUid("D123456789B");
    deA.setCode("DCodeA");
    deB.setCode("DCodeB");
    CategoryOptionCombo ocA = new CategoryOptionCombo();
    ocA.setUid("C123456789A");
    ocA.setCode("CCodeA");
    DataElementOperand opA = new DataElementOperand(deA, ocA);
    DataElementOperand opB = new DataElementOperand(deB, ocA);
    List<DataElementOperand> operands = Lists.newArrayList(opA, opB);
    Map<String, String> map =
        DimensionalObjectUtils.getDataElementOperandIdSchemeMap(operands, IdScheme.CODE);
    assertEquals(3, map.size());
    assertEquals("DCodeA", map.get("D123456789A"));
    assertEquals("DCodeB", map.get("D123456789B"));
    assertEquals("CCodeA", map.get("C123456789A"));
  }

  @Test
  void testGetFirstSecondIdentifier() {
    assertEquals(
        "A123456789A", DimensionalObjectUtils.getFirstIdentifier("A123456789A.P123456789A"));
    assertNull(DimensionalObjectUtils.getFirstIdentifier("A123456789A"));
  }

  @Test
  void testGetSecondIdentifier() {
    assertEquals(
        "P123456789A", DimensionalObjectUtils.getSecondIdentifier("A123456789A.P123456789A"));
    assertNull(DimensionalObjectUtils.getSecondIdentifier("A123456789A"));
  }

  @Test
  void testSortKeys() {
    Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("b1-a1-c1", 1d);
    valueMap.put("a2-c2-b2", 2d);
    valueMap.put("c3-b3-a3", 3d);
    valueMap.put("a4-b4-c4", 4d);
    Map<String, Object> sortedMap = DimensionalObjectUtils.getSortedKeysMap(valueMap);
    assertEquals(4, sortedMap.size());
    assertTrue(sortedMap.containsKey("a1-b1-c1"));
    assertTrue(sortedMap.containsKey("a2-b2-c2"));
    assertTrue(sortedMap.containsKey("a3-b3-c3"));
    assertTrue(sortedMap.containsKey("a4-b4-c4"));
    assertEquals(1d, sortedMap.get("a1-b1-c1"));
    assertEquals(2d, sortedMap.get("a2-b2-c2"));
    assertEquals(3d, sortedMap.get("a3-b3-c3"));
    assertEquals(4d, sortedMap.get("a4-b4-c4"));
    valueMap = new HashMap<>();
    valueMap.put("b1", 1d);
    valueMap.put("b2", 2d);
    sortedMap = DimensionalObjectUtils.getSortedKeysMap(valueMap);
    assertEquals(2, sortedMap.size());
    assertTrue(sortedMap.containsKey("b1"));
    assertTrue(sortedMap.containsKey("b2"));
    assertEquals(1d, sortedMap.get("b1"));
    assertEquals(2d, sortedMap.get("b2"));
    valueMap = new HashMap<>();
    valueMap.put(null, 1d);
    sortedMap = DimensionalObjectUtils.getSortedKeysMap(valueMap);
    assertEquals(0, sortedMap.size());
  }

  @Test
  void testSortKey() {
    String expected = "a-b-c";
    assertEquals(expected, DimensionalObjectUtils.sortKey("b-c-a"));
  }

  @Test
  void testGetIdentifier() {
    DataElementGroup oA = new DataElementGroup();
    DataElementGroup oB = new DataElementGroup();
    DataElementGroup oC = new DataElementGroup();
    oA.setUid("a1");
    oB.setUid("b1");
    oC.setUid("c1");
    List<DimensionalItemObject> column = new ArrayList<>();
    column.add(oC);
    column.add(oA);
    List<DimensionalItemObject> row = new ArrayList<>();
    row.add(oB);
    assertEquals("a1-b1-c1", DimensionalObjectUtils.getKey(column, row));
    assertEquals("b1", DimensionalObjectUtils.getKey(new ArrayList<>(), row));
  }

  @Test
  void testGetKey() {
    DataElement deA = new DataElement("DE NameA");
    deA.setShortName("DE ShortNameA");
    DataElement deB = new DataElement("DE NameB");
    deB.setShortName("DE ShortNameB");
    DataElement deC = new DataElement("DE NameC");
    deC.setShortName("DE ShortNameC");
    List<DimensionalItemObject> objects = Lists.newArrayList(deA, deB, deC);
    String name = DimensionalObjectUtils.getKey(objects);
    assertEquals("de_shortnamea_de_shortnameb_de_shortnamec", name);
  }

  @Test
  void testGetName() {
    DataElement deA = new DataElement("DE NameA");
    deA.setShortName("DE ShortNameA");
    DataElement deB = new DataElement("DE NameB");
    deB.setShortName("DE ShortNameB");
    DataElement deC = new DataElement("DE NameC");
    deC.setShortName("DE ShortNameC");
    List<DimensionalItemObject> objects = Lists.newArrayList(deA, deB, deC);
    String name = DimensionalObjectUtils.getName(objects);
    assertEquals("DE ShortNameA DE ShortNameB DE ShortNameC", name);
  }

  @Test
  void testConvertToDimItemValueMap() {
    DataElement deA = new DataElement("DE NameA");
    DataElement deB = new DataElement("DE NameB");
    DataElement deC = new DataElement("DE NameC");
    List<DimensionItemObjectValue> list =
        Lists.newArrayList(
            new DimensionItemObjectValue(deA, 10D),
            new DimensionItemObjectValue(deB, 20D),
            new DimensionItemObjectValue(deC, 30D));
    Map<DimensionalItemObject, Object> asMap =
        DimensionalObjectUtils.convertToDimItemValueMap(list);
    assertEquals(asMap.size(), 3);
    assertEquals(10, ((Double) asMap.get(deA)).intValue());
    assertEquals(20, ((Double) asMap.get(deB)).intValue());
    assertEquals(30, ((Double) asMap.get(deC)).intValue());
  }

  @Test
  void testLinkAssociationsSuccessfully() {
    // Given
    EventAnalyticalObject eventAnalyticalObject = stubEventAnalyticalObject();
    DimensionalObject dimensionalObject = stubDimensionalObject();
    org.hisp.dhis.eventvisualization.Attribute parent = COLUMN;

    // When
    DimensionalObject result = linkAssociations(eventAnalyticalObject, dimensionalObject, parent);

    // Then
    assertEquals(eventAnalyticalObject.getEventRepetitions().get(0), result.getEventRepetition());
  }

  @Test
  void testLinkAssociationsDoesNotFindValidAssociation() {
    // Given
    EventAnalyticalObject eventAnalyticalObject = stubEventAnalyticalObject();
    DimensionalObject dimensionalObject = stubDimensionalObject();
    dimensionalObject.setUid("nonLinkableUid");
    org.hisp.dhis.eventvisualization.Attribute parent = COLUMN;

    // When
    DimensionalObject result = linkAssociations(eventAnalyticalObject, dimensionalObject, parent);

    // Then
    assertNull(result.getEventRepetition());
  }

  @Test
  void testLinkAssociationsWithProgramAndStage() {
    // Given
    EventAnalyticalObject eventAnalyticalObject = stubEventAnalyticalObject();
    DimensionalObject dimensionalObject = stubDimensionalObject();
    org.hisp.dhis.eventvisualization.Attribute parent = COLUMN;

    // When
    DimensionalObject result = linkAssociations(eventAnalyticalObject, dimensionalObject, parent);

    // Then
    assertEquals(eventAnalyticalObject.getEventRepetitions().get(0), result.getEventRepetition());
    assertEquals(
        eventAnalyticalObject.getEventRepetitions().get(0).getParent(),
        result.getEventRepetition().getParent());
    assertEquals(dimensionalObject.getProgram(), result.getProgram());
    assertEquals(dimensionalObject.getProgramStage(), result.getProgramStage());
  }

  @Test
  void testLinkAssociationsWithProgramOnly() {
    // Given
    EventAnalyticalObject eventAnalyticalObject = stubEventAnalyticalObject();
    eventAnalyticalObject.getEventRepetitions().get(0).setProgramStage(null);
    BaseDimensionalObject dimensionalObject = stubDimensionalObject();
    dimensionalObject.setProgramStage(null);
    org.hisp.dhis.eventvisualization.Attribute parent = COLUMN;

    // When
    DimensionalObject result = linkAssociations(eventAnalyticalObject, dimensionalObject, parent);

    // Then
    assertEquals(eventAnalyticalObject.getEventRepetitions().get(0), result.getEventRepetition());
    assertEquals(
        eventAnalyticalObject.getEventRepetitions().get(0).getParent(),
        result.getEventRepetition().getParent());
    assertEquals(dimensionalObject.getProgram(), result.getProgram());
    assertNull(result.getProgramStage());
  }

  @Test
  void testLinkAssociationsWithProgramStageOnly() {
    // Given
    EventAnalyticalObject eventAnalyticalObject = stubEventAnalyticalObject();
    eventAnalyticalObject.getEventRepetitions().get(0).setProgram(null);
    BaseDimensionalObject dimensionalObject = stubDimensionalObject();
    dimensionalObject.setProgram(null);
    org.hisp.dhis.eventvisualization.Attribute parent = COLUMN;

    // When
    DimensionalObject result = linkAssociations(eventAnalyticalObject, dimensionalObject, parent);

    // Then
    assertEquals(eventAnalyticalObject.getEventRepetitions().get(0), result.getEventRepetition());
    assertEquals(
        eventAnalyticalObject.getEventRepetitions().get(0).getParent(),
        result.getEventRepetition().getParent());
    assertNull(result.getProgram());
    assertEquals(dimensionalObject.getProgramStage(), result.getProgramStage());
  }

  @Test
  void testGetQualifiedDimensionsWithFullValue() {
    // Given
    List<DimensionalObject> dimensionalObjects = List.of(stubDimensionalObject());

    // When
    List<String> results = getQualifiedDimensions(dimensionalObjects);

    // Then
    assertEquals("programUid.programStageUid.dimensionUid", results.get(0));
  }

  @Test
  void testGetQualifiedDimensionsOnlyProgram() {
    // Given
    BaseDimensionalObject dimensionalObject = stubDimensionalObject();
    dimensionalObject.setProgramStage(null);
    List<DimensionalObject> dimensionalObjects = List.of(dimensionalObject);

    // When
    List<String> results = getQualifiedDimensions(dimensionalObjects);

    // Then
    assertEquals("programUid.dimensionUid", results.get(0));
  }

  @Test
  void testGetQualifiedDimensionsOnlyDimensionItem() {
    // Given
    BaseDimensionalObject dimensionalObject = stubDimensionalObject();
    dimensionalObject.setProgram(null);
    dimensionalObject.setProgramStage(null);
    List<DimensionalObject> dimensionalObjects = List.of(dimensionalObject);

    // When
    List<String> results = getQualifiedDimensions(dimensionalObjects);

    // Then
    assertEquals("dimensionUid", results.get(0));
  }

  @Test
  void testAsQualifiedDimensionUsingAll() {
    // Given
    String programUid = "programUid";
    String programStageUid = "programStageUid";
    String dimensionUid = "dimensionUid";

    // When
    String result = asQualifiedDimension(dimensionUid, programUid, programStageUid);

    // Then
    assertEquals("programUid.programStageUid.dimensionUid", result);
  }

  @Test
  void testAsQualifiedDimensionNoProgramStage() {
    // Given
    String programUid = "programUid";
    String programStageUid = null;
    String dimensionUid = "dimensionUid";

    // When
    String result = asQualifiedDimension(dimensionUid, programUid, programStageUid);

    // Then
    assertEquals("programUid.dimensionUid", result);
  }

  @Test
  void testAsQualifiedDimensionOnlyDimension() {
    // Given
    String programUid = null;
    String programStageUid = null;
    String dimensionUid = "dimensionUid";

    // When
    String result = asQualifiedDimension(dimensionUid, programUid, programStageUid);

    // Then
    assertEquals("dimensionUid", result);
  }

  @Test
  void testAsBaseObjects() {
    // Given
    String qualifiedDim = "programUid.programStageUid.dimensionUid";

    // When
    Triple<Program, ProgramStage, DimensionalObject> result = asBaseObjects(qualifiedDim);

    // Then
    assertEquals("programUid", result.getLeft().getUid());
    assertEquals("programStageUid", result.getMiddle().getUid());
    assertEquals("dimensionUid", result.getRight().getUid());
  }

  @Test
  void testBaseObjectsNoProgramStage() {
    // Given
    String qualifiedDim = "programUid.dimensionUid";

    // When
    Triple<Program, ProgramStage, DimensionalObject> result = asBaseObjects(qualifiedDim);

    // Then
    assertEquals("programUid", result.getLeft().getUid());
    assertNull(result.getMiddle());
    assertEquals("dimensionUid", result.getRight().getUid());
  }

  @Test
  void testBaseObjectsOnlyDimensionItem() {
    // Given
    String qualifiedDim = "dimensionUid";

    // When
    Triple<Program, ProgramStage, DimensionalObject> result = asBaseObjects(qualifiedDim);

    // Then
    assertNull(result.getLeft());
    assertNull(result.getMiddle());
    assertEquals("dimensionUid", result.getRight().getUid());
  }

  @Test
  void testAsActualDimension() {
    // Given
    String qualifiedDim = "programUid.programStageUid.dimensionUid";

    // When
    String result = asActualDimension(qualifiedDim);

    // Then
    assertEquals("dimensionUid", result);
  }

  @Test
  void testAsActualDimensionNoProgramStage() {
    // Given
    String qualifiedDim = "programUid.dimensionUid";

    // When
    String result = asActualDimension(qualifiedDim);

    // Then
    assertEquals("dimensionUid", result);
  }

  @Test
  void testAsActualDimensionOnlyDimensionItem() {
    // Given
    String qualifiedDim = "dimensionUid";

    // When
    String result = asActualDimension(qualifiedDim);

    // Then
    assertEquals("dimensionUid", result);
  }

  private BaseDimensionalObject stubDimensionalObject() {
    BaseDimensionalObject baseDimensionalObject = new BaseDimensionalObject();
    baseDimensionalObject.setDimension("dimensionUid");
    baseDimensionalObject.setProgram(stubProgram());
    baseDimensionalObject.setProgramStage(stubProgramStage());

    return baseDimensionalObject;
  }

  private EventAnalyticalObject stubEventAnalyticalObject() {
    EventVisualization eventVisualization = new EventVisualization();
    eventVisualization.setType(PIVOT_TABLE);
    eventVisualization.setEventRepetitions(List.of(stubEventRepetition()));

    return eventVisualization;
  }

  private EventRepetition stubEventRepetition() {
    EventRepetition eventRepetition =
        new EventRepetition(
            COLUMN, "dimensionUid", "programUid", "programStageUid", List.of(-1, 2));
    eventRepetition.setProgram(stubProgram().getUid());
    eventRepetition.setProgramStage(stubProgramStage().getUid());

    return eventRepetition;
  }

  private Program stubProgram() {
    Program program = new Program();
    program.setUid("programUid");

    return program;
  }

  private ProgramStage stubProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("programStageUid");

    return programStage;
  }
}
