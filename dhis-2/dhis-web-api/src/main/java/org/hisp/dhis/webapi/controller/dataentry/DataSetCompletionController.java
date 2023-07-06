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
package org.hisp.dhis.webapi.controller.dataentry;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.webapi.controller.datavalue.DataValidator;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.dataentry.DataSetCompletionDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dataEntry")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class DataSetCompletionController {
  private final CompleteDataSetRegistrationService registrationService;

  private final DataValidator dataValidator;

  @PostMapping("/dataSetCompletion")
  @ResponseStatus(value = HttpStatus.OK)
  public void saveDataSetCompletion(@RequestBody DataSetCompletionDto dto) {
    DataSet ds = dataValidator.getAndValidateDataSet(dto.getDataSet());
    Period pe = dataValidator.getAndValidatePeriod(dto.getPeriod());
    OrganisationUnit ou = dataValidator.getAndValidateOrganisationUnit(dto.getOrgUnit());
    CategoryOptionCombo aoc = dataValidator.getAndValidateAttributeOptionCombo(dto.getAttribute());
    boolean completed = ObjectUtils.firstNonNull(dto.getCompleted(), Boolean.TRUE);

    CompleteDataSetRegistration cdr =
        registrationService.getCompleteDataSetRegistration(ds, pe, ou, aoc);

    if (cdr != null) {
      cdr.setCompleted(completed);

      registrationService.updateCompleteDataSetRegistration(cdr);
    } else {
      cdr = new CompleteDataSetRegistration(ds, pe, ou, aoc, completed);

      registrationService.saveCompleteDataSetRegistration(cdr);
    }
  }
}
