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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.minmax.MinMaxDataElementService")
public class DefaultMinMaxDataElementService implements MinMaxDataElementService {
  private final MinMaxDataElementStore minMaxDataElementStore;

  private final DataElementService dataElementService;

  private final OrganisationUnitService organisationUnitService;

  private final CategoryService categoryService;

  // -------------------------------------------------------------------------
  // MinMaxDataElementService implementation
  // -------------------------------------------------------------------------

  @Transactional
  @Override
  public long addMinMaxDataElement(MinMaxDataElement minMaxDataElement) {
    minMaxDataElementStore.save(minMaxDataElement);

    return minMaxDataElement.getId();
  }

  @Transactional
  @Override
  public void deleteMinMaxDataElement(MinMaxDataElement minMaxDataElement) {
    minMaxDataElementStore.delete(minMaxDataElement);
  }

  @Transactional
  @Override
  public void updateMinMaxDataElement(MinMaxDataElement minMaxDataElement) {
    minMaxDataElementStore.update(minMaxDataElement);
  }

  @Override
  public MinMaxDataElement getMinMaxDataElement(
      OrganisationUnit source, DataElement dataElement, CategoryOptionCombo optionCombo) {
    return minMaxDataElementStore.get(source, dataElement, optionCombo);
  }

  @Override
  public List<MinMaxDataElement> getMinMaxDataElements(
      OrganisationUnit source, Collection<DataElement> dataElements) {
    return minMaxDataElementStore.get(source, dataElements);
  }

  @Override
  public List<MinMaxDataElement> getMinMaxDataElements(MinMaxDataElementQueryParams query) {
    return minMaxDataElementStore.query(query);
  }

  @Override
  public int countMinMaxDataElements(MinMaxDataElementQueryParams query) {
    return minMaxDataElementStore.countMinMaxDataElements(query);
  }

  @Transactional
  @Override
  public void removeMinMaxDataElements(OrganisationUnit organisationUnit) {
    minMaxDataElementStore.delete(organisationUnit);
  }

  @Transactional
  @Override
  public void removeMinMaxDataElements(DataElement dataElement) {
    minMaxDataElementStore.delete(dataElement);
  }

  @Transactional
  @Override
  public void removeMinMaxDataElements(CategoryOptionCombo optionCombo) {
    minMaxDataElementStore.delete(optionCombo);
  }

  @Transactional
  @Override
  public void removeMinMaxDataElements(
      Collection<DataElement> dataElements, OrganisationUnit parent) {
    minMaxDataElementStore.delete(dataElements, parent);
  }

  /**
   * Imports a list of min-max values from a web service request. This method processes the incoming
   * JSON request, validates the data, and stores the min-max values in the database.
   *
   * @param request the JSON request containing the min-max values to import.
   * @return the number of values processed.
   * @throws BadRequestException if any validation fails.
   */
  @Override
  @Transactional
  public int importAll(MinMaxValueUpsertRequest request) throws BadRequestException {
    if (request.values().isEmpty()) return 0;

    validate(request);

    log.info(
        "Starting min-max import: {} values for dataset={}",
        request.values().size(),
        request.dataSet());
    long startTime = System.nanoTime();

    int imported = minMaxDataElementStore.upsertValues(request.values());

    long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;

    log.info(
        "Min-max import completed: {} values processed in {} ms",
        request.values().size(),
        elapsedMillis);

    return imported;
  }

  /**
   * Deletes a list of min-max values from a web service request. This method processes the incoming
   * JSON request and removes the specified min-max values from the database.
   *
   * @param request the JSON request containing the min-max values to delete.
   * @return the number of values processed.
   */
  @Override
  @Transactional
  public int deleteAll(MinMaxValueDeleteRequest request) throws BadRequestException {
    if (request.values().isEmpty()) return 0;

    log.info("Starting min-max delete: {} values", request.values().size());
    long startTime = System.nanoTime();
    int count = minMaxDataElementStore.deleteByKeys(request.values());
    long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
    log.info(
        "Min-max delete completed: {} values processed in {} ms",
        request.values().size(),
        elapsedMillis);
    return count;
  }

  private static void validate(MinMaxValue value) throws BadRequestException {
    if (value.dataElement() == null || value.orgUnit() == null || value.optionCombo() == null)
      throw new BadRequestException(ErrorCode.E7801, value);
    if (value.minValue() == null || value.maxValue() == null)
      throw new BadRequestException(ErrorCode.E7801, value);
    if (value.minValue() >= value.maxValue()) throw new BadRequestException(ErrorCode.E7802, value);
  }

  private void validate(MinMaxValueUpsertRequest request) throws BadRequestException {
    for (MinMaxValue v : request.values()) validate(v);
    Set<String> deIds =
        Set.copyOf(minMaxDataElementStore.getDataElementsByDataSet(request.dataSet()));
    for (MinMaxValue v : request.values()) {
      if (!deIds.contains(v.dataElement().getValue()))
        throw new BadRequestException(ErrorCode.E7805, v.dataElement());
    }
  }
}
