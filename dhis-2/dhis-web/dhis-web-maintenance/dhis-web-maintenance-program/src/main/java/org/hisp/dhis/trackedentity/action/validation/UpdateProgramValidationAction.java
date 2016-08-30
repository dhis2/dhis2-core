package org.hisp.dhis.trackedentity.action.validation;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.program.ProgramExpression;
import org.hisp.dhis.program.ProgramValidation;
import org.hisp.dhis.program.ProgramValidationService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * @version $ UpdateProgramValidationAction.java Apr 28, 2011 11:24:43 AM $
 */
public class UpdateProgramValidationAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramValidationService programValidationService;

    public void setProgramValidationService( ProgramValidationService programValidationService )
    {
        this.programValidationService = programValidationService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String operator;

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    private String leftSideExpression;

    public void setLeftSideExpression( String leftSideExpression )
    {
        this.leftSideExpression = leftSideExpression;
    }

    private String leftSideDescription;

    public void setLeftSideDescription( String leftSideDescription )
    {
        this.leftSideDescription = leftSideDescription;
    }

    private String rightSideExpression;

    public void setRightSideExpression( String rightSideExpression )
    {
        this.rightSideExpression = rightSideExpression;
    }

    private String rightSideDescription;

    public void setRightSideDescription( String rightSideDescription )
    {
        this.rightSideDescription = rightSideDescription;
    }

    private Integer programId;

    public void setProgramId( Integer programId )
    {
        this.programId = programId;
    }

    public Integer getProgramId()
    {
        return programId;
    }

    // -------------------------------------------------------------------------
    // Implementation Action
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        ProgramValidation validation = programValidationService.getProgramValidation( id );

        ProgramExpression leftExpression = new ProgramExpression( leftSideExpression, StringUtils.trimToNull( leftSideDescription ) );
        ProgramExpression rightExpression = new ProgramExpression( rightSideExpression, StringUtils.trimToNull( rightSideDescription ) );

        validation.setName( StringUtils.trimToNull( name ) );
        validation.setOperator( Operator.valueOf( operator ) );
        validation.setLeftSide( leftExpression );
        validation.setRightSide( rightExpression );

        programValidationService.updateProgramValidation( validation );

        return SUCCESS;
    }

}
