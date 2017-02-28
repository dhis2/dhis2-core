package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;

import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ProgramStageObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( object ) ) return;
        ProgramStage programStage = (ProgramStage) object;

        if ( programStage.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap().get( programStage.getPeriodType().getName() );
            programStage.setPeriodType( periodType );
        }

        for ( ProgramStageDataElement programStageDataElement : programStage.getProgramStageDataElements() )
        {
            preheatService.connectReferences( programStageDataElement, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        }
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( object ) ) return;
        ProgramStage programStage = (ProgramStage) object;
        ProgramStage persistedProgramStage = (ProgramStage) persistedObject;

        if ( programStage.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap().get( programStage.getPeriodType().getName() );
            programStage.setPeriodType( periodType );
        }

        programStage.getProgramStageDataElements().clear();
        persistedProgramStage.getProgramStageDataElements().clear();

        sessionFactory.getCurrentSession().flush();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( persistedObject ) ) return;
        ProgramStage programStage = (ProgramStage) persistedObject;

        Map<String, Object> references = bundle.getObjectReferences( ProgramStage.class ).get( programStage.getUid() );
        if ( references == null ) return;

        Set<ProgramStageDataElement> programStageDataElements = (Set<ProgramStageDataElement>) references.get( "programStageDataElements" );

        if ( programStageDataElements != null && !programStageDataElements.isEmpty() )
        {
            for ( ProgramStageDataElement programStageDataElement : programStage.getProgramStageDataElements() )
            {
                preheatService.connectReferences( programStageDataElement, bundle.getPreheat(), bundle.getPreheatIdentifier() );
                programStage.getProgramStageDataElements().add( programStageDataElement );
            }
        }

        sessionFactory.getCurrentSession().update( programStage );
    }
}
