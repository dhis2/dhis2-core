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
package org.hisp.dhis.webapi.controller.dataentry;

import static org.hisp.dhis.security.Authorities.F_MINMAX_DATAELEMENT_ADD;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.minmax.MinMaxValue;
import org.hisp.dhis.minmax.MinMaxValueKey;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data-entry"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dataEntry")
public class MinMaxValueController {

  private final MinMaxDataElementService minMaxValueService;

  @RequiresAuthority(anyOf = F_MINMAX_DATAELEMENT_ADD)
  @PostMapping("/minMaxValues")
  @ResponseStatus(value = HttpStatus.OK)
  public void saveOrUpdateMinMaxValue(@RequestBody MinMaxValue value) throws BadRequestException {
    minMaxValueService.importValue(value);
  }

  @RequiresAuthority(anyOf = F_MINMAX_DATAELEMENT_ADD)
  @DeleteMapping("/minMaxValues")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void removeMinMaxValue(
      @RequestBody(required = false) MinMaxValueKey key,
      @RequestParam(required = false) @OpenApi.Param({UID.class, DataElement.class}) UID de,
      @RequestParam(required = false) @OpenApi.Param({UID.class, OrganisationUnit.class}) UID ou,
      @RequestParam(required = false) @OpenApi.Param({UID.class, CategoryOptionCombo.class}) UID co)
      throws BadRequestException, NotFoundException {
    if (key == null) key = new MinMaxValueKey(de, ou, co);
    minMaxValueService.deleteValue(key);
  }
}
