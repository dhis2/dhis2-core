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

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Marks the users whose session principal has gone stale as changed in the {@link
 * UserDetailsChangeTracker}, so that their sessions refresh the principal on their next request:
 *
 * <ul>
 *   <li>all members of a user role whose authorities or restrictions have changed
 *   <li>all users whose {@link UserDetails} changed in a transaction, see {@link
 *       UserDetailsChangeCollector}
 * </ul>
 *
 * <p>The work runs after the updating transaction has committed, on a dedicated single-threaded
 * executor, in batches. This keeps a change that affects many users cheap on the request thread. If
 * the server shuts down before all batches have run, the remaining sessions simply keep the old
 * principal until they expire or the user re-authenticates.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDetailsChangeListener {

  /** Number of users whose usernames are resolved and marked per batch. */
  static final int BATCH_SIZE = 100;

  private final UserService userService;

  private final UserDetailsChangeTracker userDetailsChangeTracker;

  @Async("userDetailsChangeTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserRoleAuthoritiesChanged(UserRoleAuthoritiesChangedEvent event) {
    List<String> usernames = userService.getUsernamesByUserRole(event.userRoleUid());
    log.info(
        "Marking {} users as changed after authority change of user role '{}'",
        usernames.size(),
        event.userRoleUid());
    Lists.partition(usernames, BATCH_SIZE).forEach(userDetailsChangeTracker::markChanged);
  }

  @Async("userDetailsChangeTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserDetailsChanged(UserDetailsChangedEvent event) {
    log.info("Marking {} users as changed", event.userIds().size());
    for (List<Long> batch : Lists.partition(List.copyOf(event.userIds()), BATCH_SIZE)) {
      userDetailsChangeTracker.markChanged(userService.getUsernamesByIds(batch));
    }
  }
}
