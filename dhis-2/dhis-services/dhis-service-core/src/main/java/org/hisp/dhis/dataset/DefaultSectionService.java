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
package org.hisp.dhis.dataset;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tri
 */
@Service
@RequiredArgsConstructor
public class DefaultSectionService implements SectionService {
  private final SectionStore sectionStore;

  @Override
  @Transactional
  public long addSection(Section section) {
    sectionStore.save(section);

    return section.getId();
  }

  @Override
  @Transactional
  public void deleteSection(Section section) {
    sectionStore.delete(section);
  }

  @Override
  @Transactional(readOnly = true)
  public Section getSection(String uid) {
    return sectionStore.getByUid(uid);
  }

  @Override
  @Transactional
  public void updateSection(Section section) {
    sectionStore.update(section);
  }

  @Override
  public List<Section> getSectionsByDataElement(String uid) {
    return sectionStore.getSectionsByDataElement(uid);
  }

  @Override
  public List<Section> getSectionsByDataElement(Collection<DataElement> dataElements) {
    return sectionStore.getSectionsByDataElement(dataElements);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Section> getSectionsByIndicators(Collection<Indicator> indicators) {
    return sectionStore.getSectionsByIndicators(indicators);
  }
}
