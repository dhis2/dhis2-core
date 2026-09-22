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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Tests the {@link EnrollmentAggregateAnalyticsController}. */
@Transactional
class EnrollmentAggregateAnalyticsControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetAggregateDimensionsWithAggregationTypeAndInFilter() {
    DataElement numberDataElement =
        createDataElement(
            'A', ValueType.NUMBER, AggregationType.AVERAGE, DataElementDomain.TRACKER);
    DataElement integerDataElement =
        createDataElement('B', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER);
    DataElement textDataElement =
        createDataElement('C', ValueType.TEXT, AggregationType.SUM, DataElementDomain.TRACKER);
    manager.save(numberDataElement);
    manager.save(integerDataElement);
    manager.save(textDataElement);

    Program program = createProgram('A');
    manager.save(program);

    ProgramStage programStage = createProgramStage('A', program);
    programStage.addDataElement(numberDataElement, 0);
    programStage.addDataElement(integerDataElement, 1);
    programStage.addDataElement(textDataElement, 2);
    program.getProgramStages().add(programStage);
    manager.save(programStage);

    JsonObject response =
        GET(
                "/analytics/enrollments/aggregate/dimensions"
                    + "?programId={programId}"
                    + "&fields=id,aggregationType,valueType"
                    + "&filter=dimensionType:eq:DATA_ELEMENT"
                    + "&filter=valueType:in:[NUMBER,INTEGER]",
                program.getUid())
            .content();

    JsonArray dimensions = response.getArray("dimensions");
    assertEquals(2, dimensions.size());

    Set<String> valueTypes = new HashSet<>();
    Set<String> aggregationTypes = new HashSet<>();
    for (int i = 0; i < dimensions.size(); i++) {
      JsonObject dimension = dimensions.getObject(i);
      valueTypes.add(dimension.getString("valueType").string());
      aggregationTypes.add(dimension.getString("aggregationType").string());
    }

    assertEquals(Set.of("NUMBER", "INTEGER"), valueTypes);
    assertTrue(aggregationTypes.contains("AVERAGE"));
    assertTrue(aggregationTypes.contains("SUM"));
  }
}
