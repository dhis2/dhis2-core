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
package org.hisp.dhis.datavalue;

import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ConflictException;

/**
 * API to export aggregate data values.
 *
 * <p>Any and all access to data to expose it in some form should always read the data using one of
 * the provided lookup methods to make sure access validation is always applied consistently.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
public interface DataExportService {

  /**
   * Export a single value.
   *
   * @apiNote Looking at the callers of this method is a good way to find places that are doing data
   *     processing wrong ;) Ideally nobody should call this (except the odd controller endpoint
   *     that is actually about 1 value). Most other callers are calling this in a loop - which
   *     really should use a bulk export instead.
   * @param key unique dimensions, null for COC + AOC are understood as default
   * @return the value if it exists or null
   * @throws ConflictException in case current user has no access to the value
   */
  @CheckForNull
  DataExportValue exportValue(@Nonnull DataEntryKey key) throws ConflictException;

  Stream<DataExportValue> exportValues(@Nonnull DataExportInputParams params)
      throws ConflictException;

  DataExportGroup.Output exportGroup(@Nonnull DataExportInputParams params, boolean sync)
      throws ConflictException;

  Stream<DataExportGroup.Output> exportInGroups(@Nonnull DataExportInputParams params)
      throws ConflictException;
}
