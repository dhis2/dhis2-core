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
package org.hisp.dhis.period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.CheckForNull;

/**
 * All {@link org.hisp.dhis.common.IdentifiableObject} classes that have a persisted mapping to one
 * or more periods must implement this interface returning the persisted periods.
 *
 * @author Jan Bernitt
 */
public interface UsesPeriodRelations {

  /**
   * @return null or empty list in case no period(s) are currently set
   */
  @JsonIgnore
  @CheckForNull
  List<Period> getPersistedPeriods();

  /**
   * A setter is needed because during an import each ISO period should map to a unique instance of
   * {@link Period}. This setter is used to set the original period list returned by the getter but
   * with all the unqiue instances in it.
   *
   * @param periods the periods to apply to the period relations of the target object, null or empty
   *     removes all relations
   */
  void setPersistedPeriods(@CheckForNull List<Period> periods);
}
