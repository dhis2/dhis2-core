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

import java.util.Arrays;
import javax.annotation.CheckForNull;

/**
 * The attribute option combos (AOCs) a user may read data of, as resolved by {@link
 * AccessControlStore}: the internal ids a data query filters on.
 *
 * @param ids internal ids of the readable AOCs; null when no check is needed (superuser); empty
 *     when the user may read the data of no AOC at all
 */
public record DataReadableAocs(@CheckForNull long[] ids) {

  /** No check needed: the user may read the data of all AOCs. */
  public static DataReadableAocs unrestricted() {
    return new DataReadableAocs(null);
  }

  /** True when no check is needed: queries do not filter on AOCs. */
  public boolean isUnrestricted() {
    return ids == null;
  }

  /** True when the user may read the data of no AOC: there is no data to query. */
  public boolean isNone() {
    return ids != null && ids.length == 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DataReadableAocs other && Arrays.equals(ids, other.ids);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(ids);
  }

  @Override
  public String toString() {
    return "DataReadableAocs[ids=" + Arrays.toString(ids) + "]";
  }
}
