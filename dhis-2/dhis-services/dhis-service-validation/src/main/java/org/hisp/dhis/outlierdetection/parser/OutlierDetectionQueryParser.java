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
package org.hisp.dhis.outlierdetection.parser;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.springframework.stereotype.Component;

/** Parse and transform the incoming query params into the OutlierDetectionRequest. */
@Component
@AllArgsConstructor
public class OutlierDetectionQueryParser {

  private final IdentifiableObjectManager idObjectManager;

  /**
   * Creates a {@link OutlierDetectionRequest} from the given query.
   *
   * @param query the {@link OutlierDetectionQuery}.
   * @return a {@link OutlierDetectionRequest}.
   */
  public OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query) {
    OutlierDetectionRequest.Builder request = new OutlierDetectionRequest.Builder();

    List<DataSet> dataSets = idObjectManager.getByUid(DataSet.class, query.getDs());

    // Re-fetch data elements to maintain access control

    List<String> de =
        dataSets.stream()
            .map(DataSet::getDataElements)
            .flatMap(Collection::stream)
            .filter(d -> d.getValueType().isNumeric())
            .map(DataElement::getUid)
            .collect(Collectors.toList());

    de.addAll(query.getDe());

    List<DataElement> dataElements = idObjectManager.getByUid(DataElement.class, de);
    List<OrganisationUnit> orgUnits =
        idObjectManager.getByUid(OrganisationUnit.class, query.getOu());

    request
        .withDataElements(dataElements)
        .withStartEndDate(query.getStartDate(), query.getEndDate())
        .withOrgUnits(orgUnits)
        .withDataStartDate(query.getDataStartDate())
        .withDataEndDate(query.getDataEndDate());

    if (query.getAlgorithm() != null) {
      request.withAlgorithm(query.getAlgorithm());
    }

    if (query.getThreshold() != null) {
      request.withThreshold(query.getThreshold());
    }

    if (query.getOrderBy() != null) {
      request.withOrderBy(query.getOrderBy());
    }

    if (query.getMaxResults() != null) {
      request.withMaxResults(query.getMaxResults());
    }

    return request.build();
  }
}
