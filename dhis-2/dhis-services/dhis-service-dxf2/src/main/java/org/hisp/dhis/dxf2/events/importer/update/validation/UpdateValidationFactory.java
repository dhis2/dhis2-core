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

package org.hisp.dhis.dxf2.events.importer.update.validation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component( "trackerEventsUpdateValidationFactory" )
@Slf4j
public class UpdateValidationFactory
{
    private final Map<ImportStrategy, List<Class<? extends Checker>>> eventUpdateValidatorMap;

    public UpdateValidationFactory(
        final Map<ImportStrategy, List<Class<? extends Checker>>> eventUpdateValidatorMap )
    {
        checkNotNull( eventUpdateValidatorMap );
        this.eventUpdateValidatorMap = eventUpdateValidatorMap;
    }

    public List<ImportSummary> validateEvents( final WorkContext ctx, final List<Event> events )
    {
        final List<ImportSummary> importSummaries = new ArrayList<>();

        final ImportStrategy importStrategy = ctx.getImportOptions().getImportStrategy();

        if ( importStrategy.isCreateAndUpdate() || importStrategy.isUpdate() )
        {
            final ValidationRunner validationRunner = new ValidationRunner( eventUpdateValidatorMap.get( UPDATE ) );

            for ( final Event event : events )
            {
                importSummaries.add( validationRunner.executeValidationChain( event, ctx ) );
            }
        }

        return importSummaries;
    }

    static class ValidationRunner
    {
        private final List<Class<? extends Checker>> validators;

        public ValidationRunner( final List<Class<? extends Checker>> validators )
        {
            this.validators = validators;
        }

        public ImportSummary executeValidationChain( final Event event, final WorkContext ctx )
        {
            for ( final Class<? extends Checker> validator : validators )
            {
                try
                {
                    final Checker validationCheck = validator.newInstance();
                    final ImportSummary importSummary = validationCheck.check( new ImmutableEvent( event ), ctx );

                    if ( importSummary.isStatus( ERROR ) )
                    {
                        return importSummary;
                    }
                }
                catch ( InstantiationException | IllegalAccessException e )
                {
                    // TODO: Maikel: Ask Luciano is we should return and Import.ERROR at this point, as returning TRUE might be wrong.
                    log.error( "An error occurred during Event import validation", e );
                }
            }
            return new ImportSummary();
        }
    }
}