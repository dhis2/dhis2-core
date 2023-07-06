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
package org.hisp.dhis.dxf2.datavalueset;

/**
 * Adapter interface to write {@link DataValueSet} data to different output formats like JSON, XML
 * and CSV.
 *
 * <p>Data is written by the following method call sequence:
 *
 * <ol>
 *   <li>{@link #writeHeader()} or {@link #writeHeader(String, String, String, String)}
 *   <li>0 or more times {@link #writeValue(DataValueEntry)}
 *   <li>{@link #close()}
 * </ol>
 *
 * All methods might throw an {@link java.io.UncheckedIOException}.
 *
 * @author Jan Bernitt
 * @see XmlDataValueSetWriter
 * @see JsonDataValueSetWriter
 * @see CsvDataValueSetWriter
 */
public interface DataValueSetWriter extends AutoCloseable {
  /**
   * Add a minimum document header to the output, so it is ready for calls of {@link
   * #writeValue(DataValueEntry)}
   */
  void writeHeader();

  /**
   * Add a header with the provided information to the output. Afterwards the output should be ready
   * for calls to {@link #writeValue(DataValueEntry)}.
   *
   * @param dataSetId ID of the written dataset
   * @param completeDate the completeDate of the set
   * @param isoPeriod the period of the set
   * @param orgUnitId the organisation unit of the set
   */
  void writeHeader(String dataSetId, String completeDate, String isoPeriod, String orgUnitId);

  void writeValue(DataValueEntry entry);

  /** Add the document footer to the output and close the document. */
  @Override
  void close();
}
