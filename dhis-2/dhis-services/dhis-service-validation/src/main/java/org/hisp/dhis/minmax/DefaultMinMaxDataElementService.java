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

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.minmax.MinMaxDataElementStore.ResolvedMinMaxDto;
import static org.hisp.dhis.minmax.MinMaxDataElementUtils.formatDtoInfo;
import static org.hisp.dhis.minmax.MinMaxDataElementUtils.validateMinMaxValues;
import static org.hisp.dhis.minmax.MinMaxDataElementUtils.validateRequiredFields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.UID;
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
  public MinMaxDataElement getMinMaxDataElement(long id) {
    return minMaxDataElementStore.get(id);
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
  @Transactional
  @Override
  public int importFromJson(MinMaxValueBatchRequest request) throws BadRequestException {
    List<MinMaxValueDto> dtos = Optional.ofNullable(request.values()).orElse(List.of());

    if (dtos.isEmpty()) {
      return 0;
    }

    log.info(
        "Starting min-max import: {} values for dataset={}, orgunit={}",
        dtos.size(),
        request.dataSet(),
        request.orgUnit());
    long startTime = System.nanoTime();
    List<ResolvedMinMaxDto> resolvedDtos = resolveAllValidDtos(dtos, true, minMaxDataElementStore);
    minMaxDataElementStore.upsert(resolvedDtos);
    long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;

    log.info(
        "Min-max import completed: {} values processed in {} ms",
        resolvedDtos.size(),
        elapsedMillis);

    return resolvedDtos.size();
  }

  /**
   * Resolves all valid DTOs and returns a list of {@link ResolvedMinMaxDto} objects. This helper
   * method is responsible for transforming incoming DTO objects into a format suitable for direct
   * database operations. Note that unresolvable DTOs will throw a BadRequestException.
   *
   * @param dtos the list of DTOs to resolve.
   * @param requireValues whether to validate min/max values.
   * @param minMaxDataElementStore the data element store.
   * @return a list of resolved DTOs.
   * @throws BadRequestException if any validation fails.
   */
  private static List<ResolvedMinMaxDto> resolveAllValidDtos(
      List<MinMaxValueDto> dtos,
      boolean requireValues,
      MinMaxDataElementStore minMaxDataElementStore)
      throws BadRequestException {

    record Uids(UID de, UID ou, UID coc) {}

    Map<MinMaxValueDto, Uids> uidMap = new HashMap<>();
    Set<UID> dataElementUids = new HashSet<>();
    Set<UID> orgUnitUids = new HashSet<>();
    Set<UID> cocUids = new HashSet<>();

    for (MinMaxValueDto dto : dtos) {
      try {
        UID de = UID.of(dto.getDataElement());
        UID ou = UID.of(dto.getOrgUnit());
        UID coc = UID.of(dto.getCategoryOptionCombo());

        uidMap.put(dto, new Uids(de, ou, coc));
        dataElementUids.add(de);
        orgUnitUids.add(ou);
        cocUids.add(coc);
      } catch (IllegalArgumentException e) {
        log.error("Invalid UID in min-max import: {}", formatDtoInfo(dto));
        throw new BadRequestException(ErrorCode.E7805, formatDtoInfo(dto));
      }
    }

    Map<UID, Long> dataElementMap = minMaxDataElementStore.getDataElementMap(dataElementUids);
    Map<UID, Long> orgUnitMap = minMaxDataElementStore.getOrgUnitMap(orgUnitUids);
    Map<UID, Long> cocMap = minMaxDataElementStore.getCategoryOptionComboMap(cocUids);

    List<ResolvedMinMaxDto> resolvedDtos = new ArrayList<>();

    for (MinMaxValueDto dto : dtos) {
      validateRequiredFields(dto);
      if (requireValues) {
        validateMinMaxValues(dto);
      }
      Uids uids = uidMap.get(dto);

      Long deId = dataElementMap.get(uids.de());
      Long ouId = orgUnitMap.get(uids.ou());
      Long cocId = cocMap.get(uids.coc());

      if (deId == null || ouId == null || cocId == null) {
        log.error("Missing required fields in min-max import: {}", formatDtoInfo(dto));
        throw new BadRequestException(ErrorCode.E7803, formatDtoInfo(dto));
      }

      resolvedDtos.add(
          new ResolvedMinMaxDto(
              deId,
              ouId,
              cocId,
              dto.getMinValue(),
              dto.getMaxValue(),
              defaultIfNull(dto.getGenerated(), true)));
    }

    return resolvedDtos;
  }

  /**
   * Deletes a list of min-max values from a web service request. This method processes the incoming
   * JSON request and removes the specified min-max values from the database.
   *
   * @param request the JSON request containing the min-max values to delete.
   * @return the number of values processed.
   */
  @Transactional
  @Override
  public int deleteFromJson(MinMaxValueBatchRequest request) throws BadRequestException {
    List<MinMaxValueDto> dtos = Optional.ofNullable(request.values()).orElse(List.of());
    if (dtos.isEmpty()) return 0;

    log.info(
        "Starting min-max delete: {} values for dataset={}, orgunit={}",
        dtos.size(),
        request.dataSet(),
        request.orgUnit());
    long startTime = System.nanoTime();
    List<ResolvedMinMaxDto> resolvedDtos = resolveAllValidDtos(dtos, false, minMaxDataElementStore);
    minMaxDataElementStore.delete(resolvedDtos);
    long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
    log.info(
        "Min-max delete completed: {} values processed in {} ms",
        resolvedDtos.size(),
        elapsedMillis);

    return resolvedDtos.size();
  }
}
