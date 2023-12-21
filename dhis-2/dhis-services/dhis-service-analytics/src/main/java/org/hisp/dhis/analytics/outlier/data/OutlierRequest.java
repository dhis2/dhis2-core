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
package org.hisp.dhis.analytics.outlier.data;

import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.Z_SCORE;
import static org.hisp.dhis.analytics.outlier.Order.MEAN_ABS_DEV;
import static org.hisp.dhis.analytics.outlier.data.OutlierRequestValidator.DEFAULT_LIMIT;
import static org.hisp.dhis.common.OrganisationUnitDescendants.DESCENDANTS;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.With;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.outlier.Order;
import org.hisp.dhis.common.OrganisationUnitDescendants;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Encapsulation of an outlier detection request.
 *
 * @author Lars Helge Overland
 */
@Getter
@Builder
@With
public class OutlierRequest {

  private Date startDate;

  private Date endDate;

  private Date dataStartDate;

  private Date dataEndDate;

  private String queryKey;

  @Default private List<DataElement> dataElements = new ArrayList<>();

  @Default private List<OrganisationUnit> orgUnits = new ArrayList<>();

  @Default private Order orderBy = MEAN_ABS_DEV;

  @Default private SortOrder sortOrder = SortOrder.DESC;

  @Default private int maxResults = DEFAULT_LIMIT;

  @Default private OrganisationUnitDescendants orgUnitSelection = DESCENDANTS;

  @Default private OutlierDetectionAlgorithm algorithm = Z_SCORE;

  @Default private double threshold = 3.0d;

  private boolean analyzeOnly;

  private String explainOrderId;

  public List<Long> getDataElementIds() {
    return dataElements.stream().map(DataElement::getId).toList();
  }

  public boolean hasDataStartEndDate() {
    return dataStartDate != null && dataEndDate != null;
  }
}
