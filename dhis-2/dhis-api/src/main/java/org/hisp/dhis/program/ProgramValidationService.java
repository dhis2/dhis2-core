package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Collection;
import java.util.List;

/**
 * @author Chau Thu Tran
 * @version $ ProgramValidationService.java Apr 28, 2011 10:32:20 AM $
 */
public interface ProgramValidationService
{
    String ID = ProgramValidationService.class.getName();

    /**
     * Adds an {@link ProgramValidation}
     *
     * @param programValidation The to ProgramValidation add.
     * @return A generated unique id of the added {@link ProgramValidation}.
     */
    int addProgramValidation( ProgramValidation programValidation );

    /**
     * Deletes a {@link ProgramValidation}
     *
     * @param programValidation The ProgramValidation to delete.
     */
    void deleteProgramValidation( ProgramValidation programValidation );

    /**
     * Updates an {@link ProgramValidation}.
     *
     * @param programValidation The ProgramValidation to update.
     */
    void updateProgramValidation( ProgramValidation programValidation );

    /**
     * Returns a {@link ProgramValidation}.
     *
     * @param id the id of the ProgramValidation to return.
     * @return the ProgramValidation with the given id
     */
    ProgramValidation getProgramValidation( int id );

    /**
     * Returns all {@link ProgramValidation}.
     *
     * @return a collection of all ProgramValidation, or an empty collection if
     * there are no ProgramValidations.
     */
    List<ProgramValidation> getAllProgramValidation();

    /**
     * Get validation by {@link Program}
     *
     * @param program Program
     * @return ProgramValidation list
     */
    List<ProgramValidation> getProgramValidation( Program program );

    /**
     * Get validation by program stage
     *
     * @param programStage {@link ProgramStage}
     * @return ProgramValidation list
     */
    List<ProgramValidation> getProgramValidation( ProgramStage programStage );

    /**
     * Get validation violated in an event
     *
     * @param validation           ProgramValidation List
     * @param programStageInstance {@link ProgramStageInstance}
     * @return List of validation violated
     */
    List<ProgramValidationResult> validate( Collection<ProgramValidation> validation,
        ProgramStageInstance programStageInstance );
}
