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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.tracker.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.supplier.strategy.ClassBasedSupplierStrategy;
import org.hisp.dhis.tracker.preheat.supplier.strategy.GenericStrategy;
import org.hisp.dhis.tracker.preheat.supplier.strategy.TrackerEntityInstanceStrategy;
import org.hisp.dhis.tracker.util.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

/**
 * @author Cambi Luca
 */
@ExtendWith(MockitoExtension.class)
class ClassBasedSupplierTest {
  private ClassBasedSupplier classBasedSupplier;

  @Mock private ApplicationContext applicationContext;

  @Mock private TrackerIdentifierCollector identifierCollector;

  @Mock private GenericStrategy genericStrategy;

  @Mock private TrackerImportParams trackerImportParams;

  @Mock private TrackerEntityInstanceStrategy trackerEntityInstanceStrategy;

  @Mock private HashMap<String, String> strategiesMap;

  @BeforeEach
  public void setUp() {
    classBasedSupplier = new ClassBasedSupplier(identifierCollector, strategiesMap);
    classBasedSupplier.setApplicationContext(applicationContext);

    TrackerPreheat trackerPreheat = new TrackerPreheat();

    when(identifierCollector.collect(trackerImportParams, trackerPreheat.getDefaults()))
        .thenReturn(
            new HashMap<Class<?>, Set<String>>() {
              {
                put(TrackedEntity.class, new HashSet<>(Collections.singletonList("trackedEntity")));
              }
            });
  }

  @Test
  void verifyGenericStrategy() {
    when(strategiesMap.getOrDefault(anyString(), anyString()))
        .thenReturn(Constant.GENERIC_STRATEGY_BEAN);

    when(applicationContext.getBean(Constant.GENERIC_STRATEGY_BEAN, GenericStrategy.class))
        .thenReturn(genericStrategy);

    classBasedSupplier.preheatAdd(trackerImportParams, new TrackerPreheat());
    verify(applicationContext).getBean(Constant.GENERIC_STRATEGY_BEAN, GenericStrategy.class);
  }

  @Test
  void verifyClassBasedSupplierStrategy() {
    when(strategiesMap.getOrDefault(anyString(), anyString())).thenReturn("classbasedstrategy");

    when(applicationContext.getBean(anyString(), eq(ClassBasedSupplierStrategy.class)))
        .thenReturn(trackerEntityInstanceStrategy);

    classBasedSupplier.preheatAdd(trackerImportParams, new TrackerPreheat());

    verify(applicationContext).getBean("classbasedstrategy", ClassBasedSupplierStrategy.class);
  }
}
