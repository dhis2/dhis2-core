/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalue;

import org.hisp.dhis.feedback.BadRequestException;

/**
 * In contrast to data entry as handled by {@link DataEntryService} this service is used when data
 * should be written to database more directly without the validation constraints that data entry
 * enforces.
 *
 * <p>This should only be used when data originates from a programmatic sources such as predictors.
 * Another use case is when test setup needs some data to test with without requiring a fully valid
 * metadata model to support further data entry with validation.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
public interface DataInjectionService {

  /**
   * Legacy support API to add or update one or more values.
   *
   * <p>This API uses {@link DataValue} not because it was the best choice but because this required
   * fewer changes in legacy code for the time being. At some point this should be updated to use a
   * dedicated record class.
   *
   * @param values the values to create or update
   * @return number of values written
   */
  int upsertValues(DataValue... values);

  int upsertValues(DataEntryValue... values);

  int upsertValues(DataEntryValue.Input... values) throws BadRequestException;
}
