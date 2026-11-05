/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.security.acl;

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.user.UserDetails;

/**
 * Database lookups of what a user may access, organised by access question rather than by table, so
 * that every feature (data value export, tracker export, ...) asks the same question the same way.
 *
 * <p>Lookups run within the caller's transaction. They resolve access up front, so that data
 * queries filter on a known, small list instead of joining a sharing-filtered set of metadata the
 * planner cannot estimate.
 */
public interface AccessControlStore {

  /**
   * The attribute option combos (AOCs) the user may read data of: the default combo (no sharing
   * guards on default), and the option combos of {@code ATTRIBUTE} category combos for which the
   * user can read data of every category option.
   *
   * @param user the user to check access for
   * @param limitTo checks only these option combos; null or empty checks all of them
   * @return internal ids of the readable AOCs; null when the user is not restricted (superuser), so
   *     no AOC filter applies; empty when the user may read no AOC at all, so no data is readable
   */
  @CheckForNull
  long[] getDataReadableAttributeOptionComboIds(
      @Nonnull UserDetails user, @CheckForNull Collection<UID> limitTo);
}
