/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Component;

/** Parse and transform the incoming query params into the OutlierDetectionRequest. */
@Component
@AllArgsConstructor
public class OutlierQueryParser {
  private final IdentifiableObjectManager idObjectManager;

  /**
   * Creates a {@link OutlierRequest} from the given query.
   *
   * @param queryParams the {@link OutlierQueryParams}.
   * @return a {@link OutlierRequest}.
   */
  public OutlierRequest getFromQuery(OutlierQueryParams queryParams, boolean analyzeOnly) {
    List<DataSet> dataSets = idObjectManager.getByUid(DataSet.class, queryParams.getDs());

    // Re-fetch data elements to maintain access control.
    // Only data elements are supported for now.
    List<String> de =
        dataSets.stream()
            .map(DataSet::getDataElements)
            .flatMap(Collection::stream)
            .filter(d -> d.getValueType().isNumeric())
            .map(DataElement::getUid)
            .collect(toList());

    de.addAll(queryParams.getDx());

    List<DataElement> dataElements = idObjectManager.getByUid(DataElement.class, de);
    List<OrganisationUnit> orgUnits =
        idObjectManager.getByUid(OrganisationUnit.class, queryParams.getOu());

    OutlierRequest.OutlierRequestBuilder builder =
        OutlierRequest.builder()
            .dataElements(dataElements)
            .startDate(queryParams.getStartDate())
            .endDate(queryParams.getEndDate())
            .orgUnits(orgUnits)
            .analyzeOnly(analyzeOnly)
            .dataStartDate(queryParams.getDataStartDate())
            .dataEndDate(queryParams.getDataEndDate())
            .queryKey(queryParams.queryKey());

    if (queryParams.getAlgorithm() != null) {
      builder.algorithm(queryParams.getAlgorithm());
    }

    if (queryParams.getThreshold() != null) {
      builder.threshold(queryParams.getThreshold());
    }

    if (queryParams.getOrderBy() != null) {
      builder.orderBy(queryParams.getOrderBy());
    }

    if (queryParams.getSortOrder() != null) {
      builder.sortOrder(queryParams.getSortOrder());
    }

    if (queryParams.getMaxResults() != null) {
      builder.maxResults(queryParams.getMaxResults());
    }

    if (analyzeOnly) {
      builder.explainOrderId(UUID.randomUUID().toString());
    }

    return builder.build();
  }
}
