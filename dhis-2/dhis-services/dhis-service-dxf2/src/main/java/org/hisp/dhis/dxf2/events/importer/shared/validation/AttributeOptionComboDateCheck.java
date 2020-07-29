package org.hisp.dhis.dxf2.events.importer.shared.validation;

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

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.Date;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
public class AttributeOptionComboDateCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        CategoryOptionCombo attributeOptionCombo = ctx.getCategoryOptionComboMap().get( event.getUid() );

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

        Date eventDate = executionDate != null ? executionDate : dueDate;

        if ( eventDate == null )
        {
            throw new IllegalQueryException( "Event date can not be empty" );
        }

        for ( CategoryOption option : attributeOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && eventDate.compareTo( option.getStartDate() ) < 0 )
            {
                // TODO: Could we replace the exception by ImportSummary error?
//                return error( "Event date " + getMediumDateString( date ) + " is after end date "
//                    + getMediumDateString( option.getEndDate() ) + " for attributeOption '" + option.getName() + "'" )
//                        .incrementIgnored();
                throw new IllegalQueryException( "Event date " + getMediumDateString( eventDate )
                    + " is before start date " + getMediumDateString( option.getStartDate() ) + " for attributeOption '"
                    + option.getName() + "'" );
            }

            if ( option.getEndDate() != null && eventDate.compareTo( option.getEndDate() ) > 0 )
            {
                // TODO: Could we replace the exception by ImportSummary error?
//                return error( "Event date " + getMediumDateString( date ) + " is after end date "
//                    + getMediumDateString( option.getEndDate() ) + " for attributeOption '" + option.getName() + "'" )
//                        .incrementIgnored();
                throw new IllegalQueryException( "Event date " + getMediumDateString( eventDate )
                    + " is after end date " + getMediumDateString( option.getEndDate() ) + " for attributeOption '"
                    + option.getName() + "'" );
            }
        }

        return success();
    }
}
