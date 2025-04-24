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
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
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

  @Transactional
  public void importFromJson(List<MinMaxValueDto> dtos) {
    for (MinMaxValueDto dto : dtos) {
      DataElement de = dataElementService.getDataElement(dto.getDataElement());
      OrganisationUnit ou = organisationUnitService.getOrganisationUnit(dto.getOrgUnit());
      CategoryOptionCombo coc =
          categoryService.getCategoryOptionCombo(dto.getCategoryOptionCombo());

      MinMaxDataElement existing = minMaxDataElementStore.get(ou, de, coc);

      if (existing != null) {
        existing.setMin(dto.getMinValue());
        existing.setMax(dto.getMaxValue());
        existing.setGenerated(dto.getGenerated() != null ? dto.getGenerated() : true);
        minMaxDataElementStore.update(existing);
      } else {
        MinMaxDataElement newValue =
            new MinMaxDataElement(de, ou, coc, dto.getMinValue(), dto.getMaxValue());
        newValue.setGenerated(dto.getGenerated() != null ? dto.getGenerated() : true);
        minMaxDataElementStore.save(newValue);
      }
    }
  }
}
