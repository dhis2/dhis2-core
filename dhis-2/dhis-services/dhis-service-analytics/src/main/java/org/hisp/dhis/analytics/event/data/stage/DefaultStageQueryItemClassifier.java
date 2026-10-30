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
package org.hisp.dhis.analytics.event.data.stage;

import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryItem;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link StageQueryItemClassifier}.
 *
 * <p>Classification is based on the {@link QueryItem} program stage context and the item id.
 */
@Component
public class DefaultStageQueryItemClassifier implements StageQueryItemClassifier {
  /** {@inheritDoc} */
  @Override
  public boolean isStageScoped(QueryItem item) {
    return isStageOrgUnit(item) || isStageDate(item) || isStageEventStatus(item);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageOrgUnit(QueryItem item) {
    return item != null && OrganisationUnitResolver.isStageOuDimension(item);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageDate(QueryItem item) {
    return isStageEventDate(item) || isStageScheduledDate(item);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageEventDate(QueryItem item) {
    return item != null
        && item.hasProgramStage()
        && EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(item.getItemId());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageScheduledDate(QueryItem item) {
    return item != null
        && item.hasProgramStage()
        && EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME.equals(item.getItemId());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageEventStatus(QueryItem item) {
    return item != null
        && item.hasProgramStage()
        && EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME.equals(item.getItemId());
  }
}
