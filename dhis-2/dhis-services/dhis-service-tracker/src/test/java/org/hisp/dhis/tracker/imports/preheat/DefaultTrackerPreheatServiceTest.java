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
package org.hisp.dhis.tracker.imports.preheat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.supplier.ClassBasedSupplier;
import org.hisp.dhis.tracker.imports.preheat.supplier.PreheatSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

/**
 * @author Cambi Luca
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DefaultTrackerPreheatServiceTest {
  @Mock private ClassBasedSupplier classBasedSupplier;

  @Mock private ApplicationContext applicationContext;

  @Captor private ArgumentCaptor<Class<PreheatSupplier>> preheatSupplierClassCaptor;

  @Captor private ArgumentCaptor<String> bean;

  private DefaultTrackerPreheatService preheatService;

  private final TrackerObjects preheatParams =
      TrackerObjects.builder()
          .trackedEntities(Collections.singletonList(new TrackedEntity()))
          .build();

  private final TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder().build();

  @BeforeEach
  public void setUp() {
    preheatService =
        new DefaultTrackerPreheatService(List.of(ClassBasedSupplier.class.getSimpleName()));

    preheatService.setApplicationContext(applicationContext);
  }

  @Test
  void shouldGetFromContextAndAdd() {
    when(applicationContext.getBean(bean.capture(), preheatSupplierClassCaptor.capture()))
        .thenReturn(classBasedSupplier);

    doCallRealMethod().when(classBasedSupplier).add(any(), any());

    preheatService.preheat(preheatParams, idSchemeParams);

    verify(applicationContext).getBean(bean.getValue(), preheatSupplierClassCaptor.getValue());
    verify(classBasedSupplier).add(any(), any());
    verify(classBasedSupplier).preheatAdd(any(), any());
  }

  @Test
  void shouldDoNothingWhenSupplierBeanNotFound() {
    when(applicationContext.getBean(bean.capture(), preheatSupplierClassCaptor.capture()))
        .thenThrow(new BeanCreationException("e"));

    preheatService.preheat(preheatParams, idSchemeParams);

    verify(applicationContext).getBean(bean.getValue(), preheatSupplierClassCaptor.getValue());
    verify(classBasedSupplier, times(0)).add(any(), any());
    verify(classBasedSupplier, times(0)).preheatAdd(any(), any());
  }

  @Test
  void shouldDoNothingWhenAddException() {
    when(applicationContext.getBean(bean.capture(), preheatSupplierClassCaptor.capture()))
        .thenReturn(classBasedSupplier);
    doThrow(new RuntimeException("e")).when(classBasedSupplier).add(any(), any());

    preheatService.preheat(preheatParams, idSchemeParams);

    verify(applicationContext).getBean(bean.getValue(), preheatSupplierClassCaptor.getValue());
    verify(classBasedSupplier).add(any(), any());
  }
}
