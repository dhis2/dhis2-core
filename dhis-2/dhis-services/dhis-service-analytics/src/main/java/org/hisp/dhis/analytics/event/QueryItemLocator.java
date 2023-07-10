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
package org.hisp.dhis.analytics.event;

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.Program;

/**
 * This class is responsible for parsing a String containing a dimension definition and returning a
 * {@link QueryItem} containing the element matching the dimension.
 *
 * @author Luciano Fiandesio
 */
public interface QueryItemLocator {
  /**
   * This method accepts a dimension definition and transforms it into a {@link QueryItem}.
   *
   * <p>The dimension definition String can be composed of the following elements:
   *
   * <p>- Data Element [{de uid}] - Data Element + Legendset [{de uid}-{legendset uid}] - Program
   * Stage + Data Element [{ps uid}.{de uid}] - Program Stage + Data Element + Legendset [{ps
   * uid}.{de uid}-{legendset uid}] - Tracked Entity Instance [{tei uid}] - Program Indicator [{pi
   * uid}] - Relationship Type + Program Indicator [{rt uid}.{pi uid}]
   *
   * <p>If the provided dimension String is not matching any of the above elements, then a {@link
   * IllegalQueryException} is thrown
   *
   * @param dimension the dimension string.
   * @param program the {@link Program}.
   * @param type the {@link EventOutputType}.
   * @return a {@link QueryItem}.
   */
  QueryItem getQueryItemFromDimension(String dimension, Program program, EventOutputType type);
}
