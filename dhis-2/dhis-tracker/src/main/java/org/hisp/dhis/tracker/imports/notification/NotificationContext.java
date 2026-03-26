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
package org.hisp.dhis.tracker.imports.notification;

import java.util.Map;
import java.util.Set;

/**
 * Pre-fetched notification dependencies. Built by the coordinator async task before dispatching
 * per-entity notification tasks. Contains plain data (no Hibernate entities) that safely crosses
 * thread boundaries.
 *
 * @param groupMembers user group members keyed by user group database ID. Only enabled users are
 *     included (disabled users are filtered in the SQL query).
 * @param keysToSkip repeatable flag keys (templateUid + enrollmentUid) for enrollments and tracker
 *     events that have already been sent and should not be sent again. SingleEvents are excluded
 *     (no enrollment, no repeatable check).
 */
public record NotificationContext(
    Map<Long, Set<GroupMemberInfo>> groupMembers, Set<String> keysToSkip) {

  public static final NotificationContext EMPTY = new NotificationContext(Map.of(), Set.of());

  /** Key format for the repeatable flag check: templateUid + enrollmentUid. */
  public static String repeatableKey(String templateUid, String enrollmentUid) {
    return templateUid + enrollmentUid;
  }

  /**
   * Pre-fetched user group member data for notification recipient resolution. A user with multiple
   * org units produces multiple entries (one per org unit). The hierarchy/parent filter checks all
   * entries and the results are deduplicated by userId before creating User references.
   *
   * @param userId database ID for entityManager.getReference()
   * @param orgUnitUid UID of one of the user's org units (for hierarchy/parent filter)
   */
  public record GroupMemberInfo(long userId, String orgUnitUid) {}
}
