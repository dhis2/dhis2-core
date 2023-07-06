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
package org.hisp.dhis.dataelement;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class OperandTest {

  @Test
  void testHashCode() {
    DataElement dataElementA = new DataElement("DataElement A");
    DataElement dataElementB = new DataElement("DataElement B");
    CategoryCombo categoryComboA = new CategoryCombo("CategoryCombo A", DISAGGREGATION);
    CategoryCombo categoryComboB = new CategoryCombo("CategoryCombo B", DISAGGREGATION);
    CategoryCombo defaultCatCombo = new CategoryCombo(DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION);
    CategoryOptionCombo categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    CategoryOptionCombo categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboB.setCategoryCombo(categoryComboB);
    CategoryOptionCombo defaultCatOptionCombo = new CategoryOptionCombo();
    defaultCatOptionCombo.setCategoryCombo(defaultCatCombo);
    DataElementOperand dataElementOperandA =
        new DataElementOperand(dataElementA, categoryOptionComboA);
    DataElementOperand dataElementOperandB =
        new DataElementOperand(dataElementB, categoryOptionComboB);
    DataElementOperand dataElementOperandC =
        new DataElementOperand(dataElementA, categoryOptionComboB);
    DataElementOperand dataElementOperandD =
        new DataElementOperand(dataElementB, categoryOptionComboA);
    DataElementOperand dataElementOperandE =
        new DataElementOperand(dataElementA, categoryOptionComboA, categoryOptionComboA);
    DataElementOperand dataElementOperandF =
        new DataElementOperand(dataElementA, categoryOptionComboB, categoryOptionComboB);
    DataElementOperand dataElementOperandG =
        new DataElementOperand(dataElementA, categoryOptionComboA, categoryOptionComboB);
    DataElementOperand dataElementOperandH =
        new DataElementOperand(dataElementA, categoryOptionComboB, categoryOptionComboA);
    DataElementOperand dataElementOperandI = new DataElementOperand(dataElementA);
    DataElementOperand dataElementOperandJ =
        new DataElementOperand(dataElementA, defaultCatOptionCombo);
    Set<DataElementOperand> dataElementOperands = new HashSet<>();
    dataElementOperands.add(dataElementOperandA);
    dataElementOperands.add(dataElementOperandB);
    dataElementOperands.add(dataElementOperandC);
    dataElementOperands.add(dataElementOperandD);
    dataElementOperands.add(dataElementOperandE);
    dataElementOperands.add(dataElementOperandF);
    dataElementOperands.add(dataElementOperandG);
    dataElementOperands.add(dataElementOperandH);
    dataElementOperands.add(dataElementOperandI);
    dataElementOperands.add(dataElementOperandJ);
    assertEquals(10, dataElementOperands.size());
  }

  @Test
  void testEquals() {
    DataElement dataElementA = new DataElement("DataElement A");
    DataElement dataElementB = new DataElement("DataElement B");
    CategoryCombo categoryComboA = new CategoryCombo("CategoryCombo A", DISAGGREGATION);
    CategoryCombo categoryComboB = new CategoryCombo("CategoryCombo B", DISAGGREGATION);
    CategoryCombo defaultCatCombo = new CategoryCombo(DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION);
    CategoryOptionCombo categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    CategoryOptionCombo categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboB.setCategoryCombo(categoryComboB);
    CategoryOptionCombo defaultCatOptionCombo = new CategoryOptionCombo();
    defaultCatOptionCombo.setCategoryCombo(defaultCatCombo);
    DataElementOperand dataElementOperandA =
        new DataElementOperand(dataElementA, categoryOptionComboA);
    DataElementOperand dataElementOperandB =
        new DataElementOperand(dataElementB, categoryOptionComboB);
    DataElementOperand dataElementOperandC =
        new DataElementOperand(dataElementA, categoryOptionComboA);
    DataElementOperand dataElementOperandD =
        new DataElementOperand(dataElementB, categoryOptionComboB);
    DataElementOperand dataElementOperandE =
        new DataElementOperand(dataElementA, categoryOptionComboA, categoryOptionComboA);
    DataElementOperand dataElementOperandF =
        new DataElementOperand(dataElementA, categoryOptionComboB, categoryOptionComboB);
    DataElementOperand dataElementOperandG =
        new DataElementOperand(dataElementA, categoryOptionComboA, categoryOptionComboA);
    DataElementOperand dataElementOperandH =
        new DataElementOperand(dataElementA, categoryOptionComboB, categoryOptionComboB);
    DataElementOperand dataElementOperandI = new DataElementOperand(dataElementA);
    DataElementOperand dataElementOperandJ =
        new DataElementOperand(dataElementA, defaultCatOptionCombo);
    assertEquals(dataElementOperandA, dataElementOperandC);
    assertEquals(dataElementOperandB, dataElementOperandD);
    assertEquals(dataElementOperandE, dataElementOperandG);
    assertEquals(dataElementOperandF, dataElementOperandH);
    assertNotEquals(dataElementOperandA, dataElementOperandB);
    assertNotEquals(dataElementOperandC, dataElementOperandD);
    assertNotEquals(dataElementOperandE, dataElementOperandF);
    assertNotEquals(dataElementOperandG, dataElementOperandH);
    assertNotEquals(dataElementOperandI, dataElementOperandJ);
    assertNotEquals(dataElementOperandA, dataElementOperandE);
    assertNotEquals(dataElementOperandA, dataElementOperandI);
  }
}
