package org.hisp.dhis.dxf2.events.importer.insert.validation;

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

import java.util.Set;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;

public class DataValueCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        final Set<DataValue> dataValues = event.getDataValues();
        final ImportSummary importSummary = new ImportSummary();
        final User user = ctx.getImportOptions().getUser();
        for ( DataValue dataValue : dataValues )
        {
            DataElement dataElement = ctx.getDataElementMap().get( dataValue.getDataElement() );

            if ( dataElement == null )
            {
                // This can happen if a wrong data element identifier is provided
                importSummary.getConflicts().add(
                    new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
            }
            else
            {
                final String status = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

                if ( status != null )
                {
                    importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
                }
            }
            if ( doValidationOfMandatoryAttributes( user ) )
            {
                ValidationStrategy validationStrategy = getValidationStrategy( ctx, event );

            }

        }

        return importSummary;
    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null
            || !user.isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    private ValidationStrategy getValidationStrategy( WorkContext ctx, ImmutableEvent event )
    {
        return ctx
            .getProgramStage( ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme(), event.getProgramStage() )
            .getValidationStrategy();
    }

    @Override
    public boolean isFinal()
    {
        return true;
    }
}
