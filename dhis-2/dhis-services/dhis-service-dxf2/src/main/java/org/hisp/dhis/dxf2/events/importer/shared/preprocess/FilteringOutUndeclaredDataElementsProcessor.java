/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.events.importer.shared.preprocess;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * Remove data elements from event and context map which are not present in the
 * program stage that is linked to it.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Component
public class FilteringOutUndeclaredDataElementsProcessor implements Processor
{
    @Override
    public void process( final Event event, final WorkContext ctx )
    {
        if ( StringUtils.isNotBlank( event.getProgramStage() ) )
        {
            updateDataValuesInEventAndContext( event, ctx );
        }
    }

    private void updateDataValuesInEventAndContext( Event event, WorkContext ctx )
    {

        Set<String> programStageDataElementUids = getDataElementUidsFromProgramStage( event.getProgramStage(),
            ctx );

        Set<EventDataValue> ctxEventDataValues = ctx.getEventDataValueMap().get( event.getUid() );

        ctx.getEventDataValueMap().put( event.getUid(), ctxEventDataValues.stream()
            .filter( eventDataValue -> programStageDataElementUids.contains( eventDataValue.getDataElement() ) )
            .collect( Collectors.toSet() ) );
    }

    public static Set<DataValue> getFilteredDataValues( Set<DataValue> dataValues,
        Set<String> programStageDataElementUids )
    {
        return Optional.ofNullable( dataValues ).orElse( Collections.emptySet() ).stream()
            .filter( dataValue -> programStageDataElementUids.contains( dataValue.getDataElement() ) )
            .collect( Collectors.toSet() );
    }

    public static Set<String> getDataElementUidsFromProgramStage( String programStageUid, WorkContext ctx )
    {
        IdScheme scheme = ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme();
        ProgramStage programStage = ctx.getProgramStage( scheme, programStageUid );
        return Optional.ofNullable( programStage )
            .map( ProgramStage::getDataElements )
            .orElse( Collections.emptySet() )
            .stream()
            .map( IdentifiableObject::getUid )
            .collect( Collectors.toSet() );
    }

}
