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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.function.Consumer;

import lombok.AllArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

/**
 * Created by zubair@dhis2.org on 18.08.17.
 */
@AllArgsConstructor
@Component
public class SmsCommandObjectBundleHook extends AbstractObjectBundleHook<SMSCommand>
{
    private static final ImmutableMap<ParserType, Consumer<SMSCommand>> VALUE_POPULATOR = new ImmutableMap.Builder<ParserType, Consumer<SMSCommand>>()
        .put( ParserType.TRACKED_ENTITY_REGISTRATION_PARSER, sc -> {
            sc.setProgramStage( null );
            sc.setUserGroup( null );
            sc.setDataset( null );
        } )
        .put( ParserType.PROGRAM_STAGE_DATAENTRY_PARSER, sc -> {
            sc.setDataset( null );
            sc.setUserGroup( null );
        } )
        .put( ParserType.KEY_VALUE_PARSER, sc -> {
            sc.setProgram( null );
            sc.setProgramStage( null );
        } )
        .put( ParserType.ALERT_PARSER, sc -> {
            sc.setProgram( null );
            sc.setProgramStage( null );
        } )
        .build();

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final CategoryService categoryService;

    @Override
    public void preCreate( SMSCommand command, ObjectBundle bundle )
    {
        process( command );
        getReferences( command );
    }

    @Override
    public void preUpdate( SMSCommand command, SMSCommand persistedObject, ObjectBundle bundle )
    {
        getReferences( command );
    }

    private void process( SMSCommand command )
    {
        Consumer<SMSCommand> mod = VALUE_POPULATOR.get( command.getParserType() );
        if ( mod != null )
        {
            mod.accept( command );
        }
    }

    private void getReferences( SMSCommand command )
    {
        CategoryOptionCombo defaultCoc = categoryService.getDefaultCategoryOptionCombo();

        command.getCodes().stream()
            .filter( SMSCode::hasDataElement )
            .forEach( c -> {
                c.setOptionId( c.getOptionId() == null ? defaultCoc
                    : categoryService.getCategoryOptionCombo( c.getOptionId().getUid() ) );
                c.setDataElement( dataElementService.getDataElement( c.getDataElement().getUid() ) );
            } );

        command.getCodes().stream()
            .filter( SMSCode::hasTrackedEntityAttribute )
            .forEach( c -> {
                c.setOptionId( c.getOptionId() == null ? defaultCoc
                    : categoryService.getCategoryOptionCombo( c.getOptionId().getUid() ) );
                c.setTrackedEntityAttribute(
                    trackedEntityAttributeService.getTrackedEntityAttribute( c.getTrackedEntityAttribute().getUid() ) );
            } );
    }
}
