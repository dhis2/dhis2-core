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
 * Adopter interface to read {@link DataValueSet}s from different input formats like XML, JSON and
 * CSV.
 *
 * <p>To avoid materialising a potentially very large set of {@link
 * org.hisp.dhis.dxf2.datavalue.DataValue}s with the {@link DataValueSet} the values are not
 * included in the {@link #readHeader()} value. Instead, the values are iterated/streamed using the
 * {@link #readNext()} method.
 *
 * <p>To read an input the call sequence should be the following:
 *
 * <ol>
 *   <li>call {@link #readHeader()} once (must be called)
 *   <li>call {@link #readNext()} until it returns {@code null}
 *   <li>call {@link #close()} once
 * </ol>
 *
 * All methods might throw an {@link java.io.UncheckedIOException}.
 *
 * <p>A reader that does not support actual streaming using {@link #readNext()} can include the
 * values in the {@link DataValueSet} returned by the {@link #readHeader()} and immediately return
 * {@code null} when {@link #readNext()} is called.
 *
 * @author Jan Bernitt
 * @see XmlDataValueSetReader
 * @see CsvDataValueSetReader
 * @see PdfDataValueSetReader
 * @see JsonDataValueSetReader
 */
public interface DataValueSetReader extends AutoCloseable {

  /**
   * @return The information on the {@link DataValueSet} but not including the {@link
   *     DataValueSet#getDataValues()}
   */
  DataValueSet readHeader();

  /**
   * @return the next {@link DataValueEntry} in the set or {@code null} if no more values are
   *     available
   */
  DataValueEntry readNext();

  @Override
  void close();
}
