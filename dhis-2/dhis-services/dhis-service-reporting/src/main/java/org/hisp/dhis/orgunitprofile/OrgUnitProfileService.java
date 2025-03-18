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
package org.hisp.dhis.orgunitprofile;

import java.util.List;
import javax.annotation.Nullable;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;

/**
 * Main interface for org unit profile management.
 *
 * @author Lars Helge Overland
 */
public interface OrgUnitProfileService {
  /**
   * Saves or updates the {@link OrgUnitProfile}.
   *
   * @param profile the {@link OrgUnitProfile}.
   */
  void saveOrgUnitProfile(OrgUnitProfile profile) throws ForbiddenException;

  /**
   * Validates the {@link OrgUnitProfile}.
   *
   * @param profile the {@link OrgUnitProfile}.
   * @return a list of {@link ErrorReport}.
   */
  List<ErrorReport> validateOrgUnitProfile(OrgUnitProfile profile);

  /**
   * Retrieves the current {@link OrgUnitProfile}. If no profile is set, an empty profile object is
   * returned.
   *
   * @return the {@link OrgUnitProfile}, never null.
   */
  OrgUnitProfile getOrgUnitProfile() throws ForbiddenException;

  /**
   * Retrieves data for the current {@link OrgUnitProfile}.
   *
   * @param orgUnit org unit identifier.
   * @param isoPeriod the ISO period, optional.
   * @return the {@link OrgUnitProfileData}.
   */
  OrgUnitProfileData getOrgUnitProfileData(String orgUnit, @Nullable String isoPeriod)
      throws ForbiddenException;
}
