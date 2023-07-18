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
package org.hisp.dhis.dxf2.events.importer.context;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TODO: this supplier uses Hibernate
 *
 * @author Luciano Fiandesio
 */
@Component("workContextDataElementsSupplier")
public class DataElementSupplier extends AbstractSupplier<Map<String, DataElement>> {
  private final IdentifiableObjectManager manager;

  public DataElementSupplier(
      NamedParameterJdbcTemplate jdbcTemplate, IdentifiableObjectManager manager) {
    super(jdbcTemplate);
    this.manager = manager;
  }

  @Override
  public Map<String, DataElement> get(ImportOptions importOptions, List<Event> events) {
    Map<String, DataElement> dataElementsMap;

    IdScheme dataElementIdScheme = importOptions.getIdSchemes().getDataElementIdScheme();

    // Collects all Data Elements IDs
    Set<String> allDataElements =
        events.stream()
            .map(Event::getDataValues)
            .flatMap(Collection::stream)
            .map(DataValue::getDataElement)
            .collect(Collectors.toSet());

    if (dataElementIdScheme.isNull() || dataElementIdScheme.is(IdentifiableProperty.UID)) {
      dataElementsMap =
          manager.getObjects(DataElement.class, IdentifiableProperty.UID, allDataElements).stream()
              .collect(Collectors.toMap(DataElement::getUid, d -> d));
    } else {
      // Slower, but shouldn't happen so often
      dataElementsMap =
          allDataElements.stream()
              .map(deId -> manager.getObject(DataElement.class, dataElementIdScheme, deId))
              .filter(Objects::nonNull)
              .collect(
                  Collectors.toMap(
                      dataElement ->
                          IdentifiableObjectUtils.getIdentifierBasedOnIdScheme(
                              dataElement, dataElementIdScheme),
                      d -> d));
    }
    return dataElementsMap;
  }
}
