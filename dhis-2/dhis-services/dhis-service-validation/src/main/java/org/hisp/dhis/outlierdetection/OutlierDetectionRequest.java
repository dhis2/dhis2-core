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
package org.hisp.dhis.outlierdetection;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Encapsulation of an outlier detection request.
 *
 * @author Lars Helge Overland
 */
@Getter
public class OutlierDetectionRequest {
  private List<DataElement> dataElements = new ArrayList<>();

  private Date startDate;

  private Date endDate;

  private List<OrganisationUnit> orgUnits = new ArrayList<>();

  private OrgUnitSelection orgUnitSelection;

  private OutlierDetectionAlgorithm algorithm;

  private double threshold;

  private Date dataStartDate;

  private Date dataEndDate;

  private Order orderBy;

  private int maxResults;

  public List<Long> getDataElementIds() {
    return dataElements.stream().map(DataElement::getId).collect(Collectors.toList());
  }

  private OutlierDetectionRequest() {}

  public boolean hasDataStartEndDate() {
    return dataStartDate != null && dataEndDate != null;
  }

  public static class Builder {
    private OutlierDetectionRequest request;

    /** Initializes the {@link OutlierDetectionRequest} with default values. */
    public Builder() {
      this.request = new OutlierDetectionRequest();

      this.request.orgUnitSelection = OrgUnitSelection.DESCENDANTS;
      this.request.algorithm = OutlierDetectionAlgorithm.Z_SCORE;
      this.request.threshold = 3.0d;
      this.request.orderBy = Order.MEAN_ABS_DEV;
      this.request.maxResults = 500;
    }

    public Builder withDataElements(List<DataElement> dataElements) {
      this.request.dataElements = dataElements;
      return this;
    }

    public Builder withStartEndDate(Date startDate, Date endDate) {
      this.request.startDate = startDate;
      this.request.endDate = endDate;
      return this;
    }

    public Builder withOrgUnits(List<OrganisationUnit> orgUnits) {
      this.request.orgUnits = orgUnits;
      return this;
    }

    public Builder withAlgorithm(OutlierDetectionAlgorithm algorithm) {
      this.request.algorithm = algorithm;
      return this;
    }

    public Builder withThreshold(double threshold) {
      this.request.threshold = threshold;
      return this;
    }

    public Builder withDataStartDate(Date dataStartDate) {
      this.request.dataStartDate = dataStartDate;
      return this;
    }

    public Builder withDataEndDate(Date dataEndDate) {
      this.request.dataEndDate = dataEndDate;
      return this;
    }

    public Builder withOrderBy(Order orderBy) {
      this.request.orderBy = orderBy;
      return this;
    }

    public Builder withMaxResults(int maxResults) {
      this.request.maxResults = maxResults;
      return this;
    }

    public OutlierDetectionRequest build() {
      Preconditions.checkNotNull(this.request.orgUnitSelection);
      Preconditions.checkNotNull(this.request.algorithm);
      Preconditions.checkNotNull(this.request.orderBy);
      return this.request;
    }
  }
}
