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

import java.io.OutputStream;
import java.util.Date;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.feedback.ConflictException;

/**
 * @author Lars Helge Overland
 */
public interface DataExportService {

  Stream<DataExportValue.Output> streamValues(DataExportParams params) throws ConflictException;

  Stream<DataExportGroup.Output> streamValueGroups(DataExportParams params)
      throws ConflictException;

  void exportDataValueSetXml(DataExportParams params, OutputStream out) throws ConflictException;

  void exportDataValueSetXmlAdx(DataExportParams params, OutputStream out) throws ConflictException;

  void exportDataValueSetJson(DataExportParams params, OutputStream out) throws ConflictException;

  /**
   * Write data values as JSON.
   *
   * @param lastUpdated specifies the date to filter complete data sets last updated after
   * @param outputStream the stream to write to
   * @param idSchemes idSchemes
   */
  void exportDataValueSetJson(Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes)
      throws ConflictException;

  /**
   * Write data values as JSON.
   *
   * @param lastUpdated specifies the date to filter complete data sets last updated after
   * @param outputStream the stream to write to
   * @param idSchemes idSchemes
   * @param pageSize pageSize
   * @param page page
   */
  void exportDataValueSetJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes, int pageSize, int page)
      throws ConflictException;

  void exportDataValueSetCsv(DataExportParams params, OutputStream outputStream)
      throws ConflictException;
}
