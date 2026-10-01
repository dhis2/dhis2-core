/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.audit;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes tracked entity audit records on a separate thread.
 *
 * <p>This deliberately accepts only fully-resolved, immutable {@link TrackedEntityAudit} values.
 * Passing a managed or proxied entity across the {@code @Async} boundary is a thread-safety bug:
 * the caller's Hibernate session is not thread-safe and is usually already closed by the time the
 * async thread runs, so dereferencing a lazy association here throws {@code
 * LazyInitializationException} (silently, since the exception is swallowed by the async
 * uncaught-exception handler) and can corrupt the caller's connection state. Resolve everything on
 * the caller thread; hand values here.
 *
 * <p>Split into its own bean rather than an {@code @Async} method on the service because Spring
 * applies {@code @Async} through a proxy, so a self-invocation would not be asynchronous.
 *
 * <p>The insert uses {@code REQUIRES_NEW}: on a fresh async thread there is no transaction to join,
 * so this is identical to the default today, but it pins the intended contract. If this task ever
 * runs on the submitting thread instead (for example a future bounded executor whose rejection
 * policy runs tasks inline), the default {@code REQUIRED} would join the caller's read-only
 * transaction, the insert would fail, and the failure would mark the caller's transaction
 * rollback-only, breaking the user's request. The audit insert must always run in its own write
 * transaction, wherever it executes.
 *
 * @author Morten Svanæs
 */
@Component
@RequiredArgsConstructor
public class AsyncTrackedEntityAuditWriter {

  private final TrackedEntityAuditStore trackedEntityAuditStore;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addTrackedEntityAudits(@Nonnull List<TrackedEntityAudit> audits) {
    if (audits.isEmpty()) {
      return;
    }
    if (audits.size() == 1) {
      trackedEntityAuditStore.addTrackedEntityAudit(audits.get(0));
    } else {
      trackedEntityAuditStore.addTrackedEntityAudit(audits);
    }
  }
}
