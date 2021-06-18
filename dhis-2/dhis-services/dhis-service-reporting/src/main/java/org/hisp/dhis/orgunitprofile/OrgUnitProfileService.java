/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.orgunitprofile;

import java.util.List;

import javax.annotation.Nullable;

import org.hisp.dhis.feedback.ErrorReport;

public interface OrgUnitProfileService
{
    /**
     * Save or Update {@link org.hisp.dhis.orgunitprofile.OrgUnitProfile}
     *
     * @param profile OrgUnitProfile for saving
     */
    void saveOrgUnitProfile( OrgUnitProfile profile );

    /**
     * Validate all properties of
     * {@link org.hisp.dhis.orgunitprofile.OrgUnitProfile}
     *
     * @param profile OrgUnitProfile for validating
     * @return List {@link org.hisp.dhis.feedback.ErrorReport}
     * @throws {@link org.hisp.dhis.feedback.ErrorCode#E4014} if found invalid
     *         UID
     */
    List<ErrorReport> validateOrgUnitProfile( OrgUnitProfile profile );

    /**
     * Get {@link org.hisp.dhis.orgunitprofile.OrgUnitProfile} Return empty
     * object if not found
     *
     * @return OrgUnitProfile
     */
    OrgUnitProfile getOrgUnitProfile();

    /**
     * Get {@link org.hisp.dhis.orgunitprofile.OrgUnitProfileData} for given
     * {@link org.hisp.dhis.organisationunit.OrganisationUnit} UID and ISO
     * Period
     *
     * @param orgUnit OrganisationUnit UID
     * @param isoPeriod ISO Period for getting data values, not required
     * @return OrgUnitProfileData
     */
    OrgUnitProfileData getOrgUnitProfileData( String orgUnit, @Nullable String isoPeriod );
}
