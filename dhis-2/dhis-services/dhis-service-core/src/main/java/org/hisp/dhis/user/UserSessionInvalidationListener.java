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
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Invalidates the sessions of users whose session principal has gone stale, so that the change
 * takes effect on the next authentication:
 *
 * <ul>
 *   <li>all members of a user role whose authorities have changed
 *   <li>all users that joined or left a user group
 * </ul>
 *
 * <p>The work runs after the updating transaction has committed, on a dedicated single-threaded
 * executor, in batches with a pause in between. This keeps a change that affects many users cheap
 * on the request thread and spreads the invalidation work out over time. If the server shuts down
 * before all batches have run, the remaining sessions simply keep the old principal until they
 * expire or the user re-authenticates.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessionInvalidationListener {

  /** Number of users whose sessions are invalidated per batch. */
  static final int BATCH_SIZE = 100;

  /** Pause between batches, to spread the work out over time. */
  static final long BATCH_DELAY_MS = 500;

  private final UserService userService;

  @Async("userSessionInvalidationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserRoleAuthoritiesChanged(UserRoleAuthoritiesChangedEvent event) {
    List<String> usernames = userService.getUsernamesByUserRole(event.userRoleUid());
    log.info(
        "Invalidating sessions of {} users after authority change of user role '{}'",
        usernames.size(),
        event.userRoleUid());
    invalidateInBatches(
        usernames, Function.identity(), "authority change of user role " + event.userRoleUid());
  }

  @Async("userSessionInvalidationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserGroupMembershipChanged(UserGroupMembershipChangedEvent event) {
    log.info(
        "Invalidating sessions of {} users after user group membership change",
        event.userIds().size());
    invalidateInBatches(
        List.copyOf(event.userIds()),
        userService::getUsernamesByIds,
        "user group membership change");
  }

  /**
   * Invalidates the sessions of the given users in batches.
   *
   * @param users the users, identified by username or any key that resolves to usernames
   * @param toUsernames resolves a batch of users to their usernames
   * @param cause what made the sessions stale, for logging
   */
  private <T> void invalidateInBatches(
      List<T> users, Function<List<T>, List<String>> toUsernames, String cause) {
    List<List<T>> batches = Lists.partition(users, BATCH_SIZE);
    for (int i = 0; i < batches.size(); i++) {
      if (i > 0 && !pauseBetweenBatches()) {
        log.warn(
            "Session invalidation after {} interrupted, {} of {} batches done",
            cause,
            i,
            batches.size());
        return;
      }
      toUsernames.apply(batches.get(i)).forEach(userService::invalidateUserSessions);
    }
  }

  private static boolean pauseBetweenBatches() {
    try {
      Thread.sleep(BATCH_DELAY_MS);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
