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
package org.hisp.dhis.cacheinvalidation.debezium;

import org.hibernate.HibernateException;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * HibernateFlushListener that is listening for {@link FlushEvent}s and registering it before the
 * transaction completes {@link BeforeTransactionCompletionProcess} to capture the transaction ID.
 * The captured transaction ID is put in to a hash table to enable lookup of incoming replication
 * events to see if the event/ID matches local transactions or if the transactions/replication event
 * comes from another DHIS2 server instance.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Profile({"!test", "!test-h2"})
@Conditional(value = DebeziumCacheInvalidationEnabledCondition.class)
@Component
public class HibernateFlushListener implements FlushEventListener {
  @Autowired private transient KnownTransactionsService knownTransactionsService;

  @Override
  public void onFlush(FlushEvent event) throws HibernateException {
    BeforeTransactionCompletionProcess beforeTransactionCompletionProcess =
        session -> knownTransactionsService.registerEvent(event);

    event.getSession().getActionQueue().registerProcess(beforeTransactionCompletionProcess);
  }
}
