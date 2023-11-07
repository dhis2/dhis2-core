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
import java.io.Writer;
import org.hisp.dhis.common.IllegalQueryException;

/**
 * Outlier detection service.
 *
 * @author Lars Helge Overland
 */
public interface OutlierDetectionService<TRs> {
  /**
   * Validates the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @throws IllegalQueryException if request is invalid.
   */
  void validate(OutlierDetectionRequest request) throws IllegalQueryException;

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
  TRs getOutlierValues(OutlierDetectionRequest request) throws IllegalQueryException;

  /**
   * Writes outlier data values for the given request as CSV to the given output stream.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @param writer the {@link Writer} to write to.
   * @throws IllegalQueryException if request is invalid.
   */
  void getOutlierValuesAsCsv(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException, IOException;

  void getOutlierValuesAsXml(OutlierDetectionRequest request, OutputStream outputStream);

  void getOutlierValuesAsXls(OutlierDetectionRequest request, OutputStream outputStream)
      throws IllegalQueryException, IOException;

  void getOutlierValuesAsHtml(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException, IOException;

  public void getOutlierValuesAsHtmlCss(OutlierDetectionRequest request, Writer writer);
}
