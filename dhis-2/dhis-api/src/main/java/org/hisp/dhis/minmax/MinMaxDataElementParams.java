/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.minmax;

import static org.hisp.dhis.jsontree.Validation.YesNo.NO;

import java.util.List;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UrlParams;
import org.hisp.dhis.jsontree.Validation;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public record MinMaxDataElementParams(
    List<String> fields,
    List<String> filters,
    Boolean skipPaging,
    @Validation(required = NO) boolean paging,
    @Validation(required = NO, minimum = 1) int page,
    @Validation(required = NO, minimum = 1, maximum = 1000) int pageSize)
    implements UrlParams {

  public static final MinMaxDataElementParams DEFAULT =
      new MinMaxDataElementParams(List.of(), List.of(), null, true, 1, 50);

  public MinMaxDataElementParams(List<String> filters) {
    this(List.of(), filters, null, true, 1, 50);
  }

  @OpenApi.Ignore
  public boolean isPaged() {
    if (skipPaging != null) return !skipPaging;
    return paging;
  }
}
