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
package org.hisp.dhis.user;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Collects the users whose user group memberships change within a transaction and publishes one
 * {@link UserGroupMembershipChangedEvent} for all of them when the transaction has committed.
 * Nothing is published when the transaction rolls back.
 *
 * <p>Memberships are written both through Hibernate (the {@code UserGroup.members} collection) and
 * directly via SQL ({@link UserGroupStore#addMember}, {@link UserGroupStore#removeMember}); both
 * report here. Collecting per transaction keeps an import that changes many groups down to one
 * event, instead of one per group.
 *
 * <p>Hibernate may flush while the transaction commits, after the {@code beforeCommit} callbacks
 * have run, so the event is published from {@code afterCompletion}. Transaction synchronization is
 * no longer active at that point, so the {@code AFTER_COMMIT} listener of the event runs right
 * away.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class UserGroupMembershipChangeCollector {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Records that the given users joined or left a user group in the current transaction.
   *
   * @param userIds the primary keys of the users
   */
  public void membershipsChanged(@Nonnull Collection<Long> userIds) {
    if (userIds.isEmpty()) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      eventPublisher.publishEvent(new UserGroupMembershipChangedEvent(Set.copyOf(userIds)));
      return;
    }
    getOrRegisterTransactionChanges().userIds.addAll(userIds);
  }

  private TransactionChanges getOrRegisterTransactionChanges() {
    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      if (synchronization instanceof TransactionChanges changes) {
        return changes;
      }
    }
    TransactionChanges changes = new TransactionChanges(eventPublisher);
    TransactionSynchronizationManager.registerSynchronization(changes);
    return changes;
  }

  /** The membership changes of one transaction. */
  private static final class TransactionChanges implements TransactionSynchronization {

    private final ApplicationEventPublisher eventPublisher;

    private final Set<Long> userIds = new HashSet<>();

    private TransactionChanges(ApplicationEventPublisher eventPublisher) {
      this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterCompletion(int status) {
      if (status == STATUS_COMMITTED) {
        eventPublisher.publishEvent(new UserGroupMembershipChangedEvent(Set.copyOf(userIds)));
      }
    }
  }
}
