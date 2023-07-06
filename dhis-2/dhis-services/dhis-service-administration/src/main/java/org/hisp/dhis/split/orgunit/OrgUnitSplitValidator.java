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
package org.hisp.dhis.split.orgunit;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Validation service for org unit split requests.
 *
 * @author Lars Helge Overland
 */
@Service
public class OrgUnitSplitValidator {
  @Autowired private OrganisationUnitService organisationUnitService;

  /**
   * Validates the given {@link OrgUnitSplitRequest}. Throws {@link IllegalQueryException} if
   * validation fails.
   *
   * @param request the {@link OrgUnitMergeRequest}.
   * @throws IllegalQueryException if validation failed.
   */
  public void validate(OrgUnitSplitRequest request) throws IllegalQueryException {
    ErrorMessage error = validateForErrorMessage(request);

    if (error != null) {
      throw new IllegalQueryException(error);
    }
  }

  /**
   * Validates the given {@link OrgUnitSplitRequest}.
   *
   * @param request the {@link OrgUnitSplitRequest}.
   * @return an {@link ErrorMessage} if the validation failed, or null if validation was successful.
   */
  public ErrorMessage validateForErrorMessage(OrgUnitSplitRequest request) {
    if (request.getSource() == null) {
      return new ErrorMessage(ErrorCode.E1510);
    }
    if (request.getTargets().size() < 2) {
      return new ErrorMessage(ErrorCode.E1511);
    }
    if (request.getTargets().contains(request.getSource())) {
      return new ErrorMessage(ErrorCode.E1512);
    }
    if (request.getPrimaryTarget() == null) {
      return new ErrorMessage(ErrorCode.E1513);
    }
    if (!request.getTargets().contains(request.getPrimaryTarget())) {
      return new ErrorMessage(ErrorCode.E1514);
    }
    for (OrganisationUnit target : request.getTargets()) {
      if (organisationUnitService.isDescendant(target, request.getSource())) {
        return new ErrorMessage(ErrorCode.E1516, target.getUid());
      }
    }

    return null;
  }
}
