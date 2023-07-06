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
package org.hisp.dhis.artemis.audit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Luciano Fiandesio
 */
public class QueuedAudit implements Delayed {
  private final long origin;

  private final long delay;

  private final Audit audit;

  public QueuedAudit(Audit audit, long delay) {
    checkNotNull(audit);

    this.origin = System.currentTimeMillis();
    this.audit = audit;
    this.delay = delay;
  }

  public Audit getAuditItem() {
    return audit;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed delayed) {
    if (delayed == this) {
      return 0;
    }

    if (delayed instanceof QueuedAudit) {
      long diff = delay - ((QueuedAudit) delayed).delay;
      return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
    }

    long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));

    return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;
    result = prime * result + ((audit == null) ? 0 : audit.hashCode());

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (!(obj instanceof QueuedAudit)) {
      return false;
    }

    final QueuedAudit other = (QueuedAudit) obj;

    if (audit == null) {
      return other.audit == null;
    } else {
      return audit.equals(other.audit);
    }
  }
}
