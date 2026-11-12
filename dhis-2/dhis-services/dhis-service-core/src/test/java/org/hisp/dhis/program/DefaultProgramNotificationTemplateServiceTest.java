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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParamsMapper;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class DefaultProgramNotificationTemplateServiceTest {
  private static final String UID = "PNT_UID_1";

  @Mock private ProgramNotificationTemplateStore store;

  @Mock private ProgramNotificationTemplateOperationParamsMapper paramsMapper;

  @Mock private CacheProvider cacheProvider;

  @Mock private Cache<ProgramNotificationTemplate> templateCache;

  private DefaultProgramNotificationTemplateService service;

  @BeforeEach
  void setUp() {
    doReturn(templateCache).when(cacheProvider).createNotificationTemplateCache();
    service = new DefaultProgramNotificationTemplateService(store, paramsMapper, cacheProvider);
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldEvictImmediatelyWhenNoTransactionIsActive() {
    service.invalidateCache(UID);

    verify(templateCache).invalidate(UID);
  }

  @Test
  void shouldDeferEvictionUntilAfterCommitWhenTransactionIsActive() {
    TransactionSynchronizationManager.initSynchronization();

    service.invalidateCache(UID);

    // Eviction must not happen while the transaction is still open: the write is not yet visible to
    // other connections, so evicting now would let a concurrent reader re-cache the stale template.
    verify(templateCache, never()).invalidate(UID);

    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();
    assertEquals(1, synchronizations.size());

    synchronizations.get(0).afterCommit();

    verify(templateCache).invalidate(UID);
  }

  @Test
  void shouldNotEvictWhenTransactionRollsBack() {
    TransactionSynchronizationManager.initSynchronization();

    service.invalidateCache(UID);

    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();
    synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    // The write was rolled back, so the cached template is still valid and must not be evicted.
    verify(templateCache, never()).invalidate(UID);
  }
}
