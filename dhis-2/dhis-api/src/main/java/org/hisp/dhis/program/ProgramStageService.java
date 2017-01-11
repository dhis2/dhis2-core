package org.hisp.dhis.program;

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


import org.hisp.dhis.dataentryform.DataEntryForm;

import java.util.List;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface ProgramStageService
{
    String ID = ProgramStageService.class.getName();

    // -------------------------------------------------------------------------
    // ProgramStage
    // -------------------------------------------------------------------------

    /**
     * Adds an {@link ProgramStage}
     *
     * @param programStage The to ProgramStage add.
     * @return A generated unique id of the added {@link ProgramStage}.
     */
    int saveProgramStage( ProgramStage programStage );

    /**
     * Deletes a {@link ProgramStage}.
     *
     * @param programStage the ProgramStage to delete.
     */
    void deleteProgramStage( ProgramStage programStage );

    /**
     * Updates an {@link ProgramStage}.
     *
     * @param programStage the ProgramStage to update.
     */
    void updateProgramStage( ProgramStage programStage );

    /**
     * Returns a {@link ProgramStage}.
     *
     * @param id the id of the ProgramStage to return.
     * @return the ProgramStage with the given id
     */
    ProgramStage getProgramStage( int id );

    /**
     * Returns the {@link ProgramStage} with the given UID.
     *
     * @param uid the UID.
     * @return the ProgramStage with the given UID, or null if no match.
     */
    ProgramStage getProgramStage( String uid );

    /**
     * Retrieve a program stage by name and a program
     *
     * @param name    Name of program stage
     * @param program Specify a {@link Program} for retrieving a program stage.
     *                The system allows the name of program stages are duplicated on
     *                different programs
     * @return ProgramStage
     */
    ProgramStage getProgramStageByName( String name, Program program );

    /**
     * Retrieve all ProgramStages associated with the given DataEntryForm.
     * @param dataEntryForm the DataEntryForm.
     * @return a list og ProgramStages.
     */
    List<ProgramStage> getProgramStagesByDataEntryForm( DataEntryForm dataEntryForm );
}
