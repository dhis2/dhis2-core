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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.hisp.quick.BatchHandlerFactory;
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

  private final BatchHandlerFactory batchHandlerFactory;

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

  @Transactional
  @Override
  public void importFromJson(MinMaxValueBatchRequest request) throws BadRequestException {
    List<MinMaxValueDto> dtos = Optional.ofNullable(request.values()).orElse(List.of());
    if (dtos.isEmpty()) return;

    List<ResolvedMinMaxDto> resolvedDtos = resolveAllValidDtos(dtos, minMaxDataElementStore);
    final int CHUNK_SIZE = 500;
    for (int i = 0; i < resolvedDtos.size(); i += CHUNK_SIZE) {
      int end = Math.min(i + CHUNK_SIZE, resolvedDtos.size());
      List<ResolvedMinMaxDto> chunk = resolvedDtos.subList(i, end);
      minMaxDataElementStore.upsertResolvedDtos(chunk);
    }
  }

  private static List<ResolvedMinMaxDto> resolveAllValidDtos(
      List<MinMaxValueDto> dtos, MinMaxDataElementStore minMaxDataElementStore)
      throws BadRequestException {

    // Parse UIDs only once per DTO
    record Uids(UID de, UID ou, UID coc) {}
    Map<MinMaxValueDto, Uids> uidMap = new HashMap<>();
    for (MinMaxValueDto dto : dtos) {
      try {
        uidMap.put(
            dto,
            new Uids(
                UID.of(dto.getDataElement()),
                UID.of(dto.getOrgUnit()),
                UID.of(dto.getCategoryOptionCombo())));
      } catch (IllegalArgumentException e) {
        throw new BadRequestException(ErrorCode.E7805, formatDtoInfo(dto));
      }
    }

    Set<UID> dataElementUids =
        uidMap.values().stream().map(Uids::de).collect(Collectors.toUnmodifiableSet());
    Set<UID> orgUnitUids =
        uidMap.values().stream().map(Uids::ou).collect(Collectors.toUnmodifiableSet());
    Set<UID> cocUids =
        uidMap.values().stream().map(Uids::coc).collect(Collectors.toUnmodifiableSet());

    Map<UID, Long> dataElementMap = minMaxDataElementStore.getDataElementMap(dataElementUids);
    Map<UID, Long> orgUnitMap = minMaxDataElementStore.getOrgUnitMap(orgUnitUids);
    Map<UID, Long> cocMap = minMaxDataElementStore.getCategoryOptionComboMap(cocUids);

    List<ResolvedMinMaxDto> resolvedDtos = new ArrayList<>();

    for (MinMaxValueDto dto : dtos) {
      MinMaxDataElementUtils.validateDto(dto);
      Uids uids = uidMap.get(dto);

      Long deId = dataElementMap.get(uids.de());
      Long ouId = orgUnitMap.get(uids.ou());
      Long cocId = cocMap.get(uids.coc());

      if (deId == null || ouId == null || cocId == null) {
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

  private void validateDtoUids(MinMaxValueDto dto) throws BadRequestException {
    try {
      UID.of(dto.getDataElement());
      UID.of(dto.getOrgUnit());
      UID.of(dto.getCategoryOptionCombo());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(ErrorCode.E7804, formatDtoInfo(dto));
    }
  }

  @Transactional
  @Override
  public void deleteFromJson(MinMaxValueBatchRequest request) throws BadRequestException {
    List<MinMaxValueDto> dtos = request.values();

    for (MinMaxValueDto dto : dtos) {
      try {
        UID.of(dto.getDataElement());
        UID.of(dto.getOrgUnit());
        UID.of(dto.getCategoryOptionCombo());
      } catch (IllegalArgumentException ex) {
        throw new BadRequestException(ErrorCode.E7804, formatDtoInfo(dto));
      }
    }
    minMaxDataElementStore.deleteBulkByDtos(dtos);
  }
}
