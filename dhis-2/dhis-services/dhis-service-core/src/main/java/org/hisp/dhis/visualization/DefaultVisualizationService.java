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
package org.hisp.dhis.visualization;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.GenericAnalyticalObjectService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service("org.hisp.dhis.visualization.VisualizationService")
public class DefaultVisualizationService extends GenericAnalyticalObjectService<Visualization>
    implements VisualizationService {
  @Qualifier("org.hisp.dhis.visualization.generic.VisualizationStore")
  private final AnalyticalObjectStore<Visualization> visualizationStore;

  @Override
  protected AnalyticalObjectStore<Visualization> getAnalyticalObjectStore() {
    return visualizationStore;
  }

  @Override
  @Transactional
  public long save(Visualization visualization) {
    visualizationStore.save(visualization);

    return visualization.getId();
  }

  @Override
  @Transactional(readOnly = true)
  public Visualization getVisualization(long id) {
    return visualizationStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Visualization getVisualization(String uid) {
    return visualizationStore.getByUid(uid);
  }

  @Override
  @Transactional
  public void delete(Visualization visualization) {
    visualizationStore.delete(visualization);
  }

  @Override
  @Transactional(readOnly = true)
  public Visualization getVisualizationNoAcl(String uid) {
    return visualizationStore.getByUidNoAcl(uid);
  }
}
