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

import java.beans.Introspector;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.preheat.PreheatException;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.supplier.PreheatSupplier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DefaultTrackerPreheatService
    implements TrackerPreheatService, ApplicationContextAware {
  private ApplicationContext ctx;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.ctx = applicationContext;
  }

  @Qualifier("preheatOrder")
  private final List<String> preheatSuppliers;

  // TODO this flag should be configurable
  private static final boolean FAIL_FAST_ON_PREHEAT_ERROR = false;

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public TrackerPreheat preheat(
      @Nonnull TrackerObjects trackerObjects, @Nonnull TrackerIdSchemeParams idSchemeParams) {
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.setIdSchemes(idSchemeParams);

    for (String supplier : preheatSuppliers) {
      final String beanName = Introspector.decapitalize(supplier);
      try {
        ctx.getBean(beanName, PreheatSupplier.class).add(trackerObjects, preheat);
      } catch (BeansException beanException) {
        processException(
            "Unable to find a preheat supplier with name "
                + beanName
                + " in the Spring context. Skipping supplier.",
            beanException,
            supplier);
      } catch (Exception e) {
        processException(
            "An error occurred while executing a preheat supplier with name " + supplier,
            e,
            supplier);
      }
    }

    return preheat;
  }

  private void processException(String message, Exception e, String supplier) {
    if (FAIL_FAST_ON_PREHEAT_ERROR) {
      throw new PreheatException(
          "An error occurred during the preheat process. Preheater with name "
              + Introspector.decapitalize(supplier)
              + "failed",
          e);
    } else {
      log.warn(message, e);
    }
  }
}
