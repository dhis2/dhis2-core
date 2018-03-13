package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

/**
 * Created by zubair@dhis2.org on 18.08.17.
 */
public class SmsCommandObjectBundleHook extends AbstractObjectBundleHook
{
    private ImmutableMap<ParserType, Consumer<SMSCommand>> VALUE_POPULATOR = new ImmutableMap.Builder<ParserType, Consumer<SMSCommand>>()
        .put( ParserType.TRACKED_ENTITY_REGISTRATION_PARSER, sc -> { sc.setProgramStage( null ); sc.setUserGroup( null ); sc.setDataset( null ); } )
        .put( ParserType.PROGRAM_STAGE_DATAENTRY_PARSER, sc -> { sc.setDataset( null ); sc.setUserGroup( null ); } )
        .put( ParserType.KEY_VALUE_PARSER, sc -> { sc.setProgram( null ); sc.setProgramStage( null ); } )
        .put( ParserType.ALERT_PARSER, sc -> { sc.setProgram( null ); sc.setProgramStage( null ); } )
        .build();

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;


    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        if ( !SMSCommand.class.isInstance( object ) )
        {
            return;
        }

        SMSCommand command = (SMSCommand) object;

        process( command );

        getReferences( command );
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( !SMSCommand.class.isInstance( object ) )
        {
            return;
        }

        SMSCommand command = (SMSCommand) object;

        getReferences( command );
    }

    private void process(SMSCommand command )
    {
        VALUE_POPULATOR.getOrDefault( command.getParserType(), sc -> {} ).accept( command );
    }

    private void getReferences( SMSCommand command )
    {
        command.getCodes().stream()
            .filter( c -> c.hasDataElement() )
            .forEach( c -> c.setDataElement( dataElementService.getDataElement( c.getDataElement().getUid() ) ) );

        command.getCodes().stream()
            .filter( c -> c.hasTrackedEntityAttribute() )
            .forEach( c -> c.setTrackedEntityAttribute( trackedEntityAttributeService.getTrackedEntityAttribute( c.getTrackedEntityAttribute().getUid() ) ) );
    }
}
