package org.hisp.dhis.trackedentity.action.validation;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.validation.ValidationCriteria;
import org.hisp.dhis.validation.ValidationCriteriaService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * @version GetValidationCriteriaAction.java Apr 29, 2010 10:45:36 AM
 */
public class GetValidationCriteriaAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    private ValidationCriteriaService validationCriteriaService;

    public void setValidationCriteriaService( ValidationCriteriaService validationCriteriaService )
    {
        this.validationCriteriaService = validationCriteriaService;
    }

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    // -------------------------------------------------------------------------
    // Input && Output
    // -------------------------------------------------------------------------

    private int id;

    private int programId;

    private List<TrackedEntityAttribute> attribute = new ArrayList<>();

    private ValidationCriteria validationCriteria;

    private Program program;

    // -------------------------------------------------------------------------
    // Getter && Setter
    // -------------------------------------------------------------------------

    public void setId( int id )
    {
        this.id = id;
    }

    public void setProgramId( int programId )
    {
        this.programId = programId;
    }

    public ValidationCriteria getValidationCriteria()
    {
        return validationCriteria;
    }

    public List<TrackedEntityAttribute> getattribute()
    {
        return attribute;
    }

    public Program getProgram()
    {
        return program;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        validationCriteria = validationCriteriaService.getValidationCriteria( id );

        program = programService.getProgram( programId );

        attribute = program.getTrackedEntityAttributes();

        Collections.sort( attribute );

        return SUCCESS;
    }

}
