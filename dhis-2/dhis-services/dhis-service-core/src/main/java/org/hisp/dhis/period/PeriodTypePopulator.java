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
package org.hisp.dhis.period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;

/**
 * @author Torgeir Lorange Ostby
 */
@Slf4j
@RequiredArgsConstructor
public class PeriodTypePopulator extends TransactionContextStartupRoutine {
  private final PeriodStore periodStore;

  private final SessionFactory sessionFactory;

  // -------------------------------------------------------------------------
  // Execute
  // -------------------------------------------------------------------------

  @Override
  public void executeInTransaction() {
    List<PeriodType> types = new ArrayList<>(PeriodType.getAvailablePeriodTypes());

    Collection<PeriodType> storedTypes = periodStore.getAllPeriodTypes();

    types.removeAll(storedTypes);

    // ---------------------------------------------------------------------
    // Populate missing
    // ---------------------------------------------------------------------

    StatelessSession session = sessionFactory.openStatelessSession();
    session.beginTransaction();
    try {
      types.forEach(
          type -> {
            session.insert(type);
            log.debug("Added PeriodType: " + type.getName());
          });
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      DbmsUtils.closeStatelessSession(session);
    }

    types.forEach(type -> periodStore.reloadPeriodType(type));
  }
}
