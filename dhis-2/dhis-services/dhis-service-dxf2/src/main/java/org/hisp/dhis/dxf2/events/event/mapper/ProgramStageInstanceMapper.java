package org.hisp.dhis.dxf2.events.event.mapper;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.Date;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.util.DateUtils;
import org.springframework.util.StringUtils;

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageInstanceMapper
{

    private ValidationContext validationContext;

    public ProgramStageInstanceMapper( ValidationContext ctx )
    {
        this.validationContext = ctx;
    }

    public ProgramStageInstance convert( Event event )
    {
        ImportOptions importOptions = validationContext.getImportOptions();
        ProgramStageInstance psi = new ProgramStageInstance();

        if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.CODE ) )
        {
            psi.setCode( event.getUid() );
        }

        // FKs
        psi.setProgramInstance( this.validationContext.getProgramInstanceMap().get( event.getUid() ) );
        psi.setProgramStage( this.validationContext.getProgramStage( event.getProgramStage() ) );
        psi.setOrganisationUnit( this.validationContext.getOrganisationUnitMap().get( event.getUid() ) );

        // EXECUTION + DUE DATE
        Date executionDate = null;

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        psi.setDueDate( dueDate );
        psi.setExecutionDate( executionDate );

        // STORED + COMPLETED BY + COMPLETED TIMESTAMP
        String fallBackUserName = importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]";

        psi.setStoredBy( getValidUsername( event.getStoredBy(), fallBackUserName ) );
        psi.setCompletedBy( getValidUsername( event.getCompletedBy(), fallBackUserName ) );

        if ( psi.isCompleted() )
        {
            Date completedDate = new Date();
            if ( event.getCompletedDate() != null )
            {
                completedDate = DateUtils.parseDate( event.getCompletedDate() );
            }
            psi.setCompletedDate( completedDate );
        }

        // STATUS

        psi.setStatus( EventStatus.fromInt( event.getStatus().getValue() ) );

        // ATTRIBUTE OPTION COMBO

        psi.setAttributeOptionCombo( this.validationContext.getCategoryOptionComboMap().get( event.getUid() ) );

        // GEOMETRY
        
        psi.setGeometry( event.getGeometry() );

        // CREATED AT CLIENT + UPDATED AT CLIENT

        psi.setCreatedAtClient( DateUtils.parseDate( event.getCreatedAtClient() ) );
        psi.setLastUpdatedAtClient( DateUtils.parseDate( event.getLastUpdatedAtClient() ) );

        if ( psi.getProgramStage().isEnableUserAssignment() )
        {
            psi.setAssignedUser( this.validationContext.getAssignedUserMap().get( event.getUid() ) );
        }

        return psi;

    }

    public static String getValidUsername( String userName, String fallbackUsername )
    {
        String validUsername = userName;

        if ( StringUtils.isEmpty( validUsername ) )
        {
            validUsername = User.getSafeUsername( fallbackUsername );
        }
        else if ( validUsername.length() > UserCredentials.USERNAME_MAX_LENGTH )
        {

            validUsername = User.getSafeUsername( fallbackUsername );
        }

        return validUsername;
    }

}
