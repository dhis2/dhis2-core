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

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.feedback.ConflictException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility to convert between internal data records {@link DataExportGroup.Output} and external text
 * formats.
 *
 * @implNote While being a layer above {@link org.springframework.stereotype.Service} this class
 *     needs to use {@link Transactional} because the values streamed from database have to be
 *     consumed within the transaction scope. But IO formatting is not a service level concern so
 *     instead this layer is responsible for transactions.
 * @author Jan Bernitt
 * @since 2.43
 */
@Component
@RequiredArgsConstructor
public class DataExportPipeline {

  private final DataExportService service;

  @Transactional(readOnly = true)
  public <T> List<T> exportAsList(DataExportInputParams params, Function<DataExportValue, T> f)
      throws ConflictException {
    // it might appear silly to just have this bit of code in here
    // limiting what can be done with the Stream, but we have to process the stream
    // within the transaction boundaries - that is why it is inside of this method
    return service.exportValues(params).map(f).toList();
  }

  @Transactional(readOnly = true)
  public void exportToConsumer(DataExportInputParams params, Consumer<DataExportValue> f)
      throws ConflictException {
    // it might appear silly to just have this bit of code in here
    // limiting what can be done with the Stream, but we have to process the stream
    // within the transaction boundaries - that is why it is inside of this method
    service.exportValues(params).forEach(f);
  }

  @Transactional(readOnly = true)
  public void exportAsJson(DataExportInputParams params, OutputStream out) throws ConflictException {
    DataExportOutput.toJson(service.exportGroup(params, false), out);
  }

  @Transactional(readOnly = true)
  public void exportAsJsonSync(DataExportInputParams params, OutputStream out) throws ConflictException {
    DataExportOutput.toJson(service.exportGroup(params, true), out);
  }

  @Transactional(readOnly = true)
  public void exportAsCsv(DataExportInputParams params, OutputStream out) throws ConflictException {
    DataExportOutput.toCsv(service.exportGroup(params, false), out);
  }

  @Transactional(readOnly = true)
  public void exportAsXml(DataExportInputParams params, OutputStream out) throws ConflictException {
    DataExportOutput.toXml(service.exportGroup(params, false), out);
  }

  @Transactional(readOnly = true)
  public void exportAsXmlGroups(DataExportInputParams params, OutputStream out)
      throws ConflictException {
    //ADX special handling of decoding and encoding
    Boolean outputCodeFallback = params.getInputUseCodeFallback();
    if (outputCodeFallback == null) params.setInputUseCodeFallback(true);
    IdSchemes encodeTo = params.getOutputIdSchemes();
    encodeTo.setDefaultIdScheme(IdScheme.CODE);
    DataExportOutput.toXml(service.exportInGroups(params), out);
  }
}
