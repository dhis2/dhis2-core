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

package org.hisp.dhis.dxf2.events.importer.insert.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class ProgramOrgUnitCheck implements Checker
{
    private Map<IdScheme, BiFunction<OrganisationUnit, ImmutableEvent, Boolean>> functionMap = ImmutableMap
        .<IdScheme, BiFunction<OrganisationUnit, ImmutableEvent, Boolean>> builder()
        .put( IdScheme.UID, ( ou, ev ) -> ou.getUid().equals( ev.getOrgUnit() ) )
        .put( IdScheme.CODE, ( ou, ev ) -> ou.getCode().equals( ev.getOrgUnit() ) )
        .put( IdScheme.ID, ( ou, ev ) -> String.valueOf( ou.getId() ).equals( ev.getOrgUnit() ) )
        .put( IdScheme.NAME, ( ou, ev ) -> ou.getName().equals( ev.getOrgUnit() ) )
        .build();

    private BiFunction<OrganisationUnit, ImmutableEvent, Boolean> DEFAULT_FUNCTION = ( ou, ev ) -> false;

    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        ProgramInstance programInstance = ctx.getProgramInstanceMap().get( event.getUid() );

        if ( programInstance != null )
        {
            final IdScheme orgUnitIdScheme = ctx.getImportOptions().getIdSchemes().getOrgUnitIdScheme();

            OrganisationUnit orgUnit = null;

            final Set<OrganisationUnit> organisationUnits = programInstance.getProgram().getOrganisationUnits();

            for ( OrganisationUnit ou : organisationUnits )
            {
                if ( orgUnitIdScheme.isAttribute() )
                {
                    final Optional<OrganisationUnit> ouByAttributeScheme = getByAttributeScheme( ou, event,
                        orgUnitIdScheme );
                    if ( ouByAttributeScheme.isPresent() )
                    {
                        orgUnit = ouByAttributeScheme.get();
                        break;
                    }
                }
                else
                {
                    if ( functionMap.getOrDefault( orgUnitIdScheme, DEFAULT_FUNCTION ).apply( ou, event ) )
                    {
                        orgUnit = ou;
                        break;
                    }
                }
            }

            if ( orgUnit == null )
            {
                return error( "Program is not assigned to this Organisation Unit: " + event.getOrgUnit(),
                    event.getEvent() );
            }
        }

        return success();
    }
    
    private Optional<OrganisationUnit> getByAttributeScheme( OrganisationUnit ou, ImmutableEvent event,
        IdScheme orgUnitIdScheme )
    {
        final Set<AttributeValue> attributeValues = ou.getAttributeValues();
        for ( AttributeValue attributeValue : attributeValues )
        {
            if ( orgUnitIdScheme.getAttribute().equals( attributeValue.getAttribute().getUid() ) &&
                attributeValue.getValue().equals( event.getOrgUnit() ) )
            {
                return Optional.of( ou );
            }
        }
        return Optional.empty();
    }
}
