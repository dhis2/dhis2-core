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

import static org.hisp.dhis.commons.util.TextUtils.valueOf;

import java.io.UncheckedIOException;
import lombok.AllArgsConstructor;
import org.hisp.staxwax.writer.XMLWriter;

/**
 * Write {@link DataValueSet}s as XML data.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
final class XmlDataValueSetWriter implements DataValueSetWriter {
  private final XMLWriter writer;

  @Override
  public void writeHeader() {
    writer.openDocument();
    writer.openElement("dataValueSet");
    writer.writeAttribute("xmlns", "http://dhis2.org/schema/dxf/2.0");
  }

  @Override
  public void writeHeader(
      String dataSetId, String completeDate, String isoPeriod, String orgUnitId) {
    writeHeader();
    writer.writeAttribute("dataSet", dataSetId);
    writer.writeAttribute("completeDate", completeDate);
    writer.writeAttribute("period", isoPeriod);
    writer.writeAttribute("orgUnit", orgUnitId);
  }

  @Override
  public void writeValue(DataValueEntry entry) {
    writer.openElement("dataValue");
    writer.writeAttribute("dataElement", entry.getDataElement());
    writer.writeAttribute("period", entry.getPeriod());
    writer.writeAttribute("orgUnit", entry.getOrgUnit());
    writer.writeAttribute("categoryOptionCombo", entry.getCategoryOptionCombo());
    writer.writeAttribute("attributeOptionCombo", entry.getAttributeOptionCombo());
    writer.writeAttribute("value", entry.getValue());
    writer.writeAttribute("storedBy", entry.getStoredBy());
    writer.writeAttribute("created", entry.getCreated());
    writer.writeAttribute("lastUpdated", entry.getLastUpdated());
    writer.writeAttribute("comment", entry.getComment());
    writer.writeAttribute("followUp", valueOf(entry.getFollowup()));
    writer.writeAttribute("deleted", valueOf(entry.getDeleted()));
    writer.closeElement();
  }

  @Override
  public void close() throws UncheckedIOException {
    writer.closeElement();
    writer.closeDocument();
    writer.closeWriter();
  }
}
