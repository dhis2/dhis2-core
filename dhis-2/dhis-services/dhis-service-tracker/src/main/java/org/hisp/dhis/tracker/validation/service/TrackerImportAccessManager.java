/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.validation.service;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface TrackerImportAccessManager
{
    /**
     * Same as
     * {@link OrganisationUnitService#isInUserHierarchyCached(User, OrganisationUnit)}
     * Checks the importing user has access to "capture hierarchy" of the input
     * OrganisationUnit.
     *
     * @param reporter error reporter instance
     * @param orgUnit input orgUnit to validate against
     */
    void checkOrgUnitInCaptureScope( ValidationErrorReporter reporter, OrganisationUnit orgUnit );

    /**
     * Checks the importing user has write access to the TrackedEntityType.
     *
     * @param reporter error reporter instance
     * @param trackedEntityType teiType to check importing user has write access
     *        to
     */
    void checkTeiTypeWriteAccess( ValidationErrorReporter reporter, TrackedEntityType trackedEntityType );

    /**
     * Check importing user has write access to enrollment. 1. Check user has
     * write access to program
     * <p>
     * 2 If program is registration, check : a. Check that user has read access
     * to program tei type. b. Check has access to the tei - program
     * combination.
     *
     * @param reporter error reporter instance
     * @param program program to check user has write access
     * @param trackedEntity trackedEntity to check user has write access
     * @param organisationUnit organisationUnit to check user has write access
     */
    void checkWriteEnrollmentAccess( ValidationErrorReporter reporter, Program program,
        String trackedEntity, OrganisationUnit organisationUnit );

    /**
     * Check importing user has write access to event.
     * <p>
     * Check user has access to either "search scope or capture scope" according
     * to isCreatableInSearchScope()
     * <p>
     * Check program stage is registration. If it is a registration check: 1.
     * Program stage write access 2. Program read access 3. Pprogram tei type.
     * read access 4. Tei - Program combination access
     * <p>
     * If NOT a registration check: 1. Program write access
     * <p>
     * If event has a Attribute Option Combo: 1. Check user has write access to
     * the combo.
     *
     * @param reporter error reporter instance
     * @param programStage programStage to check user has write access to
     * @param orgUnit orgUnit to check user has write access to
     * @param categoryOptionCombo categoryOptionCombo to check user has write
     *        access to
     * @param trackedEntity trackedEntity to check user has write access to
     * @param isCreatableInSearchScope flag to decide between search scope or
     *        capture scope
     */
    void checkEventWriteAccess( ValidationErrorReporter reporter, ProgramStage programStage,
        OrganisationUnit orgUnit,
        CategoryOptionCombo categoryOptionCombo,
        String trackedEntity, boolean isCreatableInSearchScope );

    /**
     * Loops trough all CategoryOptionCombo options and check that the importing
     * user has write access to all of them.
     *
     * @param reporter error reporter instance
     * @param categoryOptionCombo CategoryOptionCombo to check user has write
     *        access
     */
    void checkWriteCategoryOptionComboAccess( ValidationErrorReporter reporter,
        CategoryOptionCombo categoryOptionCombo );
}
