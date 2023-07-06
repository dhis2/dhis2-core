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

import com.google.common.collect.Lists;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.supplier.strategy.ClassBasedSupplierStrategy;
import org.hisp.dhis.tracker.preheat.supplier.strategy.GenericStrategy;
import org.hisp.dhis.tracker.util.Constant;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * This supplier collects all the references from the Tracker Import payload and executes
 * class-based strategies, based on the type of the object in the payload.
 *
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class ClassBasedSupplier extends AbstractPreheatSupplier implements ApplicationContextAware {
  private ApplicationContext context;

  private final TrackerIdentifierCollector identifierCollector;

  /**
   * A Map correlating a Tracker class name to the preheat strategy class name to use to load the
   * data
   */
  @Qualifier("preheatStrategies")
  private final Map<String, String> classStrategies;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    /*
     * Collects all references from the payload and create a Map where key
     * is the reference type (e.g. Enrollment) and the value is a Set of
     * identifiers (e.g. a list of all Enrollment UIDs found in the payload)
     */
    Map<Class<?>, Set<String>> identifierMap = identifierCollector.collect(params);

    identifierMap.forEach(
        (key, identifiers) -> {
          List<List<String>> splitList =
              Lists.partition(new ArrayList<>(identifiers), Constant.SPLIT_LIST_PARTITION_SIZE);

          final String bean =
              classStrategies.getOrDefault(key.getSimpleName(), Constant.GENERIC_STRATEGY_BEAN);

          if (bean.equals(Constant.GENERIC_STRATEGY_BEAN)) {
            context
                .getBean(Constant.GENERIC_STRATEGY_BEAN, GenericStrategy.class)
                .add(key, splitList, preheat);
          } else {
            context
                .getBean(Introspector.decapitalize(bean), ClassBasedSupplierStrategy.class)
                .add(params, splitList, preheat);
          }
        });
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }
}
