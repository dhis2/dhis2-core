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
package org.hisp.dhis.program;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;

/**
 * @author Abyot Asalefew
 */
public interface EventStore extends IdentifiableObjectStore<TrackerEvent> {

  /**
   * Merges all eventDataValues which have one of the source dataElements. The lastUpdated value is
   * used to determine which event data value is kept when merging. Any remaining source
   * eventDataValues are then deleted.
   *
   * @param sourceDataElements dataElements to determine which eventDataValues to merge
   * @param targetDataElement dataElement to use when merging source eventDataValues
   */
  void mergeEventDataValuesWithDataElement(
      @Nonnull Collection<UID> sourceDataElements, @Nonnull UID targetDataElement);

  /**
   * delete all eventDataValues which have any of the sourceDataElements
   *
   * @param sourceDataElements dataElements to determine which eventDataValues to delete
   */
  void deleteEventDataValuesWithDataElement(@Nonnull Collection<UID> sourceDataElements);

  /**
   * Updates all {@link TrackerEvent}s with references to {@link CategoryOptionCombo}s, to use the
   * coc reference.
   *
   * @param cocs {@link CategoryOptionCombo}s to update
   * @param coc {@link CategoryOptionCombo} to use as the new value
   */
  void setAttributeOptionCombo(Set<Long> cocs, long coc);
}
