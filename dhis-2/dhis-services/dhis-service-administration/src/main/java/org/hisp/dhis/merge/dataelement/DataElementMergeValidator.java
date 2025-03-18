/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import static org.hisp.dhis.feedback.ErrorCode.E1550;
import static org.hisp.dhis.feedback.ErrorCode.E1551;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.stereotype.Component;

/**
 * Class that performs property validations between source {@link DataElement} property values and
 * target {@link DataElement} property values. If there are any validation failures then an error is
 * added to the {@link MergeReport}. Any error will provide the specific details of the validation
 * failure.
 */
@Component
public class DataElementMergeValidator {

  public MergeReport validateValueType(
      @Nonnull DataElement target,
      @Nonnull List<DataElement> sources,
      @Nonnull MergeReport report) {
    List<DataElement> mismatches =
        sources.stream().filter(source -> target.getValueType() != source.getValueType()).toList();

    if (!mismatches.isEmpty()) {
      report.addErrorMessage(
          new ErrorMessage(
              E1550,
              target.getValueType(),
              mismatches.stream().map(DataElement::getValueType).distinct().toList()));
    }
    return report;
  }

  public MergeReport validateDomainType(
      @Nonnull DataElement target,
      @Nonnull List<DataElement> sources,
      @Nonnull MergeReport report) {
    List<DataElement> mismatches =
        sources.stream()
            .filter(source -> target.getDomainType() != source.getDomainType())
            .toList();

    if (!mismatches.isEmpty()) {
      report.addErrorMessage(
          new ErrorMessage(
              E1551,
              target.getDomainType(),
              mismatches.stream().map(DataElement::getDomainType).distinct().toList()));
    }
    return report;
  }
}
