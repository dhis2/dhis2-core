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
package org.hisp.dhis.dxf2.adx;

import static java.lang.Boolean.parseBoolean;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.datavalueset.DataValueEntry;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetReader;
import org.hisp.staxwax.reader.XMLReader;

@RequiredArgsConstructor
public class AdxDataValueSetReader implements DataValueSetReader, DataValueEntry {

  private final XMLReader adxReader;
  private final Consumer<Map<String, String>> handleGroup;
  private final Predicate<Map<String, String>> handleValue;

  private Map<String, String> groupAttributes;
  private Map<String, String> valueAttributes;

  @Override
  public DataValueSet readHeader() {
    adxReader.moveToStartElement(AdxDataService.ROOT, AdxDataService.NAMESPACE);
    return new DataValueSet();
  }

  @Override
  public DataValueEntry readNext() {
    if (groupAttributes == null) {
      if (!adxReader.moveToStartElement(AdxDataService.GROUP, AdxDataService.NAMESPACE))
        return null;
      groupAttributes = adxReader.readAttributes();
      handleGroup.accept(groupAttributes);
    }
    if (!adxReader.moveToStartElement(AdxDataService.DATAVALUE, AdxDataService.GROUP)) {
      groupAttributes = null;
      // end of the group, try next by recursively calling itself
      return readNext();
    }
    valueAttributes = adxReader.readAttributes();
    if (handleValue.test(valueAttributes)) {
      // if data element type is not numeric we need to pick out the
      // 'annotation' element
      adxReader.moveToStartElement(AdxDataService.ANNOTATION, AdxDataService.DATAVALUE);
      if (adxReader.isStartElement(AdxDataService.ANNOTATION)) {
        valueAttributes.put(AdxDataService.VALUE, adxReader.getElementValue());
      } else {
        throw new IllegalArgumentException("DataElement expects text annotation");
      }
    }
    return this;
  }

  @Override
  public void close() {
    adxReader.closeReader();
  }

  @Override
  public String getDataElement() {
    return valueAttributes.get(AdxDataService.DATAELEMENT);
  }

  @Override
  public String getPeriod() {
    return groupAttributes.get(AdxDataService.PERIOD);
  }

  @Override
  public String getOrgUnit() {
    return groupAttributes.get(AdxDataService.ORGUNIT);
  }

  @Override
  public String getCategoryOptionCombo() {
    return valueAttributes.get(AdxDataService.CATOPTCOMBO);
  }

  @Override
  public String getAttributeOptionCombo() {
    return groupAttributes.get(AdxDataService.ATTOPTCOMBO);
  }

  @Override
  public String getValue() {
    return valueAttributes.get(AdxDataService.VALUE);
  }

  @Override
  public String getStoredBy() {
    return valueAttributes.get("storedBy");
  }

  @Override
  public String getCreated() {
    return valueAttributes.get("created");
  }

  @Override
  public String getLastUpdated() {
    return valueAttributes.get("lastUpdated");
  }

  @Override
  public String getComment() {
    return valueAttributes.get("comment");
  }

  @Override
  public boolean getFollowup() {
    return parseBoolean(valueAttributes.get("followup"));
  }

  @Override
  public Boolean getDeleted() {
    String deleted = valueAttributes.get("deleted");
    return deleted == null ? null : parseBoolean(deleted);
  }
}
