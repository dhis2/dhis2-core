/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import java.util.List;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.stereotype.Component;

@Component
public class DataElementMergeValidator {

  /**
   * Method that performs property validations between source {@link DataElement} property values
   * and target {@link DataElement} property values, using {@link DataElementPropertyValidation}. If
   * there are any validation failures then an error is added to the {@link MergeReport}. Any error
   * will provide the details of the validation failures.
   *
   * @param target target {@link DataElement}
   * @param sources source {@link DataElement}s
   * @param propertyValidation {@link DataElementPropertyValidation} to check against
   * @param report {@link MergeReport}
   * @return report
   */
  public MergeReport validateProperties(
      @Nonnull DataElement target,
      @Nonnull List<DataElement> sources,
      DataElementPropertyValidation propertyValidation,
      @Nonnull MergeReport report) {
    List<DataElement> mismatches =
        sources.stream()
            .filter(source -> propertyValidation.predicate.test(target, source))
            .toList();

    if (!mismatches.isEmpty()) {
      report.addErrorMessage(
          new ErrorMessage(
              propertyValidation.errorCode,
              propertyValidation.getProperty(target),
              mismatches.stream().map(propertyValidation::getProperty).distinct().toList()));
    }
    return report;
  }

  /**
   * Enum encapsulating DataElement property validations. Each has a {@link DataElementPredicate}
   * and an {@link ErrorCode} and can also return the {@link DataElement} property being checked.
   */
  enum DataElementPropertyValidation implements DataElementProperty {
    VALUE_TYPE_VALIDATION(DataElementPredicate.VALUE_TYPE_MISMATCH, ErrorCode.E1554) {
      @Override
      public Object getProperty(@Nonnull DataElement de) {
        return de.getValueType();
      }
    },
    DOMAIN_TYPE_VALIDATION(DataElementPredicate.DOMAIN_TYPE_MISMATCH, ErrorCode.E1555) {
      @Override
      public Object getProperty(@Nonnull DataElement de) {
        return de.getDomainType();
      }
    };

    public final DataElementPredicate predicate;
    public final ErrorCode errorCode;

    DataElementPropertyValidation(DataElementPredicate predicate, ErrorCode errorCode) {
      this.predicate = predicate;
      this.errorCode = errorCode;
    }
  }

  @FunctionalInterface
  public interface DataElementProperty {
    Object getProperty(@Nonnull DataElement de);
  }

  /** Enum of {@link DataElement} {@link BiPredicate}s */
  enum DataElementPredicate implements BiPredicate<DataElement, DataElement> {
    VALUE_TYPE_MISMATCH {
      @Override
      public boolean test(@Nonnull DataElement target, @Nonnull DataElement source) {
        return target.getValueType() != source.getValueType();
      }
    },
    DOMAIN_TYPE_MISMATCH {
      @Override
      public boolean test(@Nonnull DataElement target, @Nonnull DataElement source) {
        return target.getDomainType() != source.getDomainType();
      }
    }
  }
}
