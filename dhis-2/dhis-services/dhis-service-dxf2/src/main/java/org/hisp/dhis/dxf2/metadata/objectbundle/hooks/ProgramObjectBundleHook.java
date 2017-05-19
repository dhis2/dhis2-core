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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;

import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ProgramObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        if ( !Program.class.isInstance( object ) ) return;
        Program program = (Program) object;

        for ( ProgramTrackedEntityAttribute programTrackedEntityAttribute : program.getProgramAttributes() )
        {
            preheatService.connectReferences( programTrackedEntityAttribute, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        }
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( !Program.class.isInstance( object ) ) return;
        Program program = (Program) object;
        Program persistedProgram = (Program) persistedObject;

        program.getProgramAttributes().clear();
        persistedProgram.getProgramAttributes().clear();

        sessionFactory.getCurrentSession().flush();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( !Program.class.isInstance( persistedObject ) ) return;
        if ( !bundle.getObjectReferences().containsKey( Program.class ) ) return;
        Program program = (Program) persistedObject;

        Map<String, Object> references = bundle.getObjectReferences( Program.class ).get( program.getUid() );
        if ( references == null ) return;

        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = (List<ProgramTrackedEntityAttribute>) references.get( "programTrackedEntityAttributes" );

        if ( programTrackedEntityAttributes != null && !programTrackedEntityAttributes.isEmpty() )
        {
            for ( ProgramTrackedEntityAttribute programTrackedEntityAttribute : programTrackedEntityAttributes )
            {
                preheatService.connectReferences( programTrackedEntityAttribute, bundle.getPreheat(), bundle.getPreheatIdentifier() );
                program.getProgramAttributes().add( programTrackedEntityAttribute );
            }
        }

        sessionFactory.getCurrentSession().update( program );
    }
}