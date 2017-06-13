package org.hisp.dhis.mobile.caseentry.state;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 */
public interface SelectedStateManager
{
    OrganisationUnit getSelectedOrganisationUnit();

    // -------------------------------------------------------------------------
    // TrackedEntityInstance
    // -------------------------------------------------------------------------

    void setSelectedPatient( TrackedEntityInstance patient );

    TrackedEntityInstance getSelectedPatient();

    void clearSelectedPatient();

    // -------------------------------------------------------------------------
    // Program-instance
    // -------------------------------------------------------------------------

    void setSelectedProgramInstance( ProgramInstance programInstance );

    ProgramInstance getSelectedProgramInstance();

    void clearSelectedProgramInstance();

    // -------------------------------------------------------------------------
    // Program-stage-instance
    // -------------------------------------------------------------------------

    void setSelectedProgramStageInstance( ProgramStageInstance programStageInstance );

    ProgramStageInstance getSelectedProgramStageInstance();

    void clearSelectedProgramStageInstance();

    // -------------------------------------------------------------------------
    // for searching patients
    // -------------------------------------------------------------------------

    void setListAll( boolean listAll );

    boolean getListAll();

    void clearListAll();

    void setSearchingAttributeId( int searchingAttributeId );

    Integer getSearchingAttributeId();

    void clearSearchingAttributeId();

    void setSearchText( String searchText );

    String getSearchText();

    void clearSearchTest();

    // -------------------------------------------------------------------------
    // for Sorting patients - Sort by patient-attribute
    // -------------------------------------------------------------------------

    void setSortingAttributeId( int sortAttributeId );

    Integer getSortAttributeId();

    void clearSortingAttributeId();
}
