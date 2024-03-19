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
package org.hisp.dhis.predictor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests PredictionWriter.
 *
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class PredictionWriterTest extends DhisConvenienceTest {

  @Mock private DataValueService dataValueService;

  @Mock private BatchHandlerFactory batchHandlerFactory;

  @Mock BatchHandler<DataValue> dataValueBatchHandler;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private CategoryOptionCombo cocA;

  private CategoryOptionCombo cocB;

  private OrganisationUnit orgUnitA;

  private Period periodA;

  private Period periodB;

  private Period periodC;

  DataValue dataValueA;

  DataValue dataValueA0;

  DataValue dataValueADeleted;

  DataValue dataValueB;

  DataValue dataValueC;

  static final List<DataValue> NO_OLD_DATA = new ArrayList<>();

  static final List<DataValue> NO_PREDICTED_DATA = new ArrayList<>();

  private PredictionSummary predictionSummary;

  PredictionWriter writer;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  @BeforeEach
  public void initTest() {
    long id = 0;

    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');

    dataElementA.setId(++id);
    dataElementB.setId(++id);

    cocA = createCategoryOptionCombo('A');
    cocB = createCategoryOptionCombo('B');

    cocA.setId(++id);
    cocB.setId(++id);

    orgUnitA = createOrganisationUnit("A");

    orgUnitA.setId(++id);

    periodA = createPeriod("202101");
    periodB = createPeriod("202102");
    periodC = createPeriod("202103");

    periodA.setId(++id);
    periodB.setId(++id);
    periodC.setId(++id);

    dataValueA = createDataValue(dataElementA, periodA, orgUnitA, "1", cocA);
    dataValueA0 = createDataValue(dataElementA, periodA, orgUnitA, "0", cocA);
    dataValueADeleted = createDataValue(dataElementA, periodA, orgUnitA, cocA, cocA, "1", true);

    dataValueB = createDataValue(dataElementB, periodB, orgUnitA, "1", cocA);

    dataValueC = createDataValue(dataElementA, periodC, orgUnitA, "1", cocA);

    writer = new PredictionWriter(dataValueService, batchHandlerFactory);

    Set<Period> existingOutputPeriods = Sets.newHashSet(periodA);

    predictionSummary = new PredictionSummary();

    when(batchHandlerFactory.createBatchHandler(DataValueBatchHandler.class))
        .thenReturn(dataValueBatchHandler);

    when(dataValueBatchHandler.init()).thenReturn(dataValueBatchHandler);

    writer.init(existingOutputPeriods, predictionSummary);
  }

  private String writeSummary(PredictionSummary summary) {
    return "Ins "
        + summary.getInserted()
        + " Upd "
        + summary.getUpdated()
        + " Del "
        + summary.getDeleted()
        + " Unchanged "
        + summary.getUnchanged();
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------

  @Test
  void testWriteIntoExistingPeriod() {
    writer.write(Lists.newArrayList(dataValueA), NO_OLD_DATA);

    verify(dataValueBatchHandler, times(1)).addObject(dataValueA);

    assertEquals("Ins 1 Upd 0 Del 0 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWriteIntoNewPeriod() {
    writer.write(Lists.newArrayList(dataValueB), NO_OLD_DATA);

    verify(dataValueService, times(1)).addDataValue(dataValueB);

    assertEquals("Ins 1 Upd 0 Del 0 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWriteInsignificantZero() {
    writer.write(Lists.newArrayList(dataValueA0), NO_OLD_DATA);

    verify(dataValueBatchHandler, never()).addObject(any());
    verify(dataValueService, never()).addDataValue(any());

    assertEquals("Ins 0 Upd 0 Del 0 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWritePredictionUnchanged() {
    writer.write(Lists.newArrayList(dataValueA), Lists.newArrayList(dataValueA));

    verify(dataValueBatchHandler, never()).addObject(any());
    verify(dataValueService, never()).addDataValue(any());

    assertEquals("Ins 0 Upd 0 Del 0 Unchanged 1", writeSummary(predictionSummary));
  }

  @Test
  void testWriteInsignificantZeroWithOldPrediction() {
    writer.write(Lists.newArrayList(dataValueA0), Lists.newArrayList(dataValueA));

    verify(dataValueBatchHandler, times(1)).updateObject(dataValueADeleted);

    assertEquals("Ins 0 Upd 0 Del 1 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWriteInsignificantZeroWithOldDeletedPrediction() {
    writer.write(Lists.newArrayList(dataValueA0), Lists.newArrayList(dataValueADeleted));

    verify(dataValueBatchHandler, never()).addObject(any());
    verify(dataValueService, never()).addDataValue(any());

    assertEquals("Ins 0 Upd 0 Del 0 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWriteDeletingOldPrediction() {
    writer.write(NO_PREDICTED_DATA, Lists.newArrayList(dataValueA));

    verify(dataValueBatchHandler, times(1)).updateObject(dataValueADeleted);

    assertEquals("Ins 0 Upd 0 Del 1 Unchanged 0", writeSummary(predictionSummary));
  }

  @Test
  void testWriteNotDeletingOldDeletedPrediction() {
    writer.write(NO_PREDICTED_DATA, Lists.newArrayList(dataValueADeleted));

    verify(dataValueBatchHandler, never()).updateObject(any());

    assertEquals("Ins 0 Upd 0 Del 0 Unchanged 0", writeSummary(predictionSummary));
  }
}
