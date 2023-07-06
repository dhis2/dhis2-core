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
package org.hisp.dhis.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class TrackedEntityDataElementDimensionDeletionHandler extends DeletionHandler {
  private final SessionFactory sessionFactory;

  public TrackedEntityDataElementDimensionDeletionHandler(SessionFactory sessionFactory) {
    checkNotNull(sessionFactory);
    this.sessionFactory = sessionFactory;
  }

  @Override
  protected void register() {
    whenDeleting(LegendSet.class, this::deleteLegendSet);
  }

  @SuppressWarnings("unchecked")
  private void deleteLegendSet(LegendSet legendSet) {
    // TODO Move this get-method to service layer

    Query query =
        sessionFactory
            .getCurrentSession()
            .createQuery("FROM TrackedEntityDataElementDimension WHERE legendSet=:legendSet");
    query.setParameter("legendSet", legendSet);

    List<TrackedEntityDataElementDimension> dataElementDimensions = query.list();

    for (TrackedEntityDataElementDimension dataElementDimension : dataElementDimensions) {
      dataElementDimension.setLegendSet(null);
      sessionFactory.getCurrentSession().update(dataElementDimension);
    }
  }
}
