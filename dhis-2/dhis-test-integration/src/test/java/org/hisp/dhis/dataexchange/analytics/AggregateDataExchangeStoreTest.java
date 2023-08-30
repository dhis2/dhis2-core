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
package org.hisp.dhis.dataexchange.analytics;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchangeStore;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AggregateDataExchangeStoreTest extends TransactionalIntegrationTest {
  @Autowired private AggregateDataExchangeStore store;

  @Test
  void testSaveGet() {
    AggregateDataExchange deA = getAggregateDataExchange('A');
    AggregateDataExchange deB = getAggregateDataExchange('B');

    store.save(deA);
    store.save(deB);

    assertNotNull(store.get(deA.getId()));
    assertNotNull(store.get(deB.getId()));
  }

  @Test
  void testUpdate() {
    AggregateDataExchange de = getAggregateDataExchange('A');

    store.save(de);

    assertNotNull(de.getSource().getRequests().get(0));

    de.setName("DataExchangeUpdated");
    de.getSource().getRequests().get(0).getDx().add("NhSFzklRD55");
    de.getTarget().getApi().setUrl("https://play.dhis2.org/dev");

    store.update(de);

    assertEquals("DataExchangeUpdated", de.getName());
    assertEquals(3, de.getSource().getRequests().get(0).getDx().size());
    assertContainsOnly(
        List.of("LrDpG50RAU9", "uR5HCiJhQ1w", "NhSFzklRD55"),
        de.getSource().getRequests().get(0).getDx());
    assertEquals("https://play.dhis2.org/dev", de.getTarget().getApi().getUrl());
  }
}
