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
package org.hisp.dhis.dataset;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;

public interface SectionService {
  /**
   * Adds a {@link Section}.
   *
   * @param section the {@link Section} to add.
   * @return the generated identifier.
   */
  long addSection(Section section);

  /**
   * Updates a {@link Section}.
   *
   * @param section the {@link Section} to update.
   */
  void updateSection(Section section);

  /**
   * Deletes a {@link Section}.
   *
   * @param section the {@link Section} to delete.
   */
  void deleteSection(Section section);

  /**
   * Retrieves the {@link Section} with the given UID.
   *
   * @param uid the identifier of the {@link Section} to retrieve.
   * @return the {@link Section}.
   */
  Section getSection(String uid);

  /**
   * Retrieves sections associated with the data element with the given UID.
   *
   * @param uid the data element UID.
   * @return a list of {@link Section}.
   */
  List<Section> getSectionsByDataElement(String uid);

  /**
   * Retrieves sections associated with the given data elements.
   *
   * @param dataElements the list of {@link DataElement}.
   * @return a list of {@link Section}.
   */
  List<Section> getSectionsByDataElement(Collection<DataElement> dataElements);

  /**
   * Retrieves sections associated with the given indicators.
   *
   * @param indicators the list of {@link Indicator}.
   * @return a list of {@link Section}.
   */
  List<Section> getSectionsByIndicators(Collection<Indicator> indicators);
}
