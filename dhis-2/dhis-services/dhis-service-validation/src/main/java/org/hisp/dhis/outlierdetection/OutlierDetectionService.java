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

import java.io.IOException;
import java.io.OutputStream;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Outlier detection service.
 *
 * @author Lars Helge Overland
 */
public interface OutlierDetectionService {
  /**
   * Validates the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @throws IllegalQueryException if request is invalid.
   */
  void validate(OutlierDetectionRequest request) throws IllegalQueryException;

  /**
   * Validates the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return an {@link ErrorMessage} if request is invalid, or null if valid.
   */
  ErrorMessage validateForErrorMessage(OutlierDetectionRequest request);

  /**
   * Creates a {@link OutlierDetectionRequest} from the given query.
   *
   * @param query the {@link OutlierDetectionQuery}.
   * @return a {@link OutlierDetectionRequest}.
   */
  OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query);

  /**
   * Returns outlier data values for the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return a {@link OutlierDetectionResponse}.
   * @throws IllegalQueryException if request is invalid.
   */
  OutlierDetectionResponse getOutlierValues(OutlierDetectionRequest request)
      throws IllegalQueryException;

  /**
   * Writes outlier data values for the given request as CSV to the given output stream.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @param out the {@link OutputStream} to write to.
   * @throws IllegalQueryException if request is invalid.
   */
  void getOutlierValuesAsCsv(OutlierDetectionRequest request, OutputStream out)
      throws IllegalQueryException, IOException;
}
