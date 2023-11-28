/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.merge.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class IndicatorTypeMergeServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private IndicatorTypeMergeService service;

  @Autowired private IdentifiableObjectManager idObjectManager;

  private IndicatorType itA;
  private IndicatorType itB;
  private IndicatorType itC;

  @Override
  public void setUpTest() {
    itA = createIndicatorType('A');
    itA.setFactor(99);
    itA.setNumber(true);
    itB = createIndicatorType('B');
    itA.setFactor(98);
    itA.setNumber(false);
    itC = createIndicatorType('C');
    idObjectManager.save(itA);
    idObjectManager.save(itB);
    idObjectManager.save(itC);
  }

  @Test
  void testGetFromQuery() {
    IndicatorTypeMergeQuery query = new IndicatorTypeMergeQuery();
    query.setSources(Lists.newArrayList(BASE_IN_TYPE_UID + 'A', BASE_IN_TYPE_UID + 'B'));
    query.setTarget(BASE_IN_TYPE_UID + 'C');
    IndicatorTypeMergeRequest request = service.getFromQuery(query);
    assertEquals(2, request.getSources().size());
    assertTrue(request.getSources().contains(itA));
    assertTrue(request.getSources().contains(itB));
    assertEquals(itC, request.getTarget());
    assertTrue(request.isDeleteSources());
  }

  @Test
  void testSourceOrgUnitNotFound() {
    IndicatorTypeMergeQuery query = new IndicatorTypeMergeQuery();
    query.setSources(Lists.newArrayList(BASE_IN_TYPE_UID + 'A', BASE_IN_TYPE_UID + 'X'));
    query.setTarget(BASE_IN_TYPE_UID + 'C');
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getFromQuery(query));
    assertEquals(ErrorCode.E1533, ex.getErrorCode());
  }

  @Test
  @DisplayName("Merge indicator types")
  void testMerge() {
    // given indicators exist and are associated with indicator types
    Indicator iA = createIndicator('A', itA);
    Indicator iB = createIndicator('B', itB);
    Indicator iC = createIndicator('C', itC);
    idObjectManager.save(iA);
    idObjectManager.save(iB);
    idObjectManager.save(iC);

    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));
    assertEquals(itA, iA.getIndicatorType());
    assertEquals(itB, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    IndicatorTypeMergeRequest request =
        new IndicatorTypeMergeRequest.Builder()
            .addSource(itA)
            .addSource(itB)
            .withTarget(itC)
            .withDeleteSources(true)
            .build();

    // when an indicator merge request is processed
    service.merge(request);

    // then
    // source indicator types are deleted
    assertNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    // and the target indicator type exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));

    // and associated source indicators are now associated to the target indicator type
    assertEquals(itC, iA.getIndicatorType());
    assertEquals(itC, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    assertEquals(100, itC.getFactor());
    assertFalse(itC.isNumber());
  }
}
