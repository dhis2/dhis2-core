/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.dxf2.events.repository;

import java.util.*;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class TrackedEntityAttributeRepository
{

    private final SessionFactory sessionFactory;

    public TrackedEntityAttributeRepository( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Fetches all {@see TrackedEntityAttribute} linked to all
     * {@see TrackedEntityType} present in the system
     * 
     * @return a Set of {@see TrackedEntityAttribute}
     */
    @SuppressWarnings("unchecked")
    public Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes()
    {
        Query query = sessionFactory.getCurrentSession()
            .createQuery( "select tet.trackedEntityTypeAttributes from TrackedEntityType tet" );

        Set<TrackedEntityTypeAttribute> trackedEntityTypeAttributes = new HashSet<>( query.list() );

        return trackedEntityTypeAttributes.stream()
            .map( TrackedEntityTypeAttribute::getTrackedEntityAttribute )
            .collect( Collectors.toSet() );
    }


    /**
     * Fetches all {@see TrackedEntityAttribute} and groups them by {@see Program}
     *
     * @return a Map, where the key is the {@see Program} and the values is a Set of {@see TrackedEntityAttribute} associated
     * to the {@see Program} in the key
     */
    @SuppressWarnings("unchecked")
    public Map<Program, Set<TrackedEntityAttribute>> getTrackedEntityAttributesByProgram()
    {
        Map<Program, Set<TrackedEntityAttribute>> result = new HashMap<>();

        Query query = sessionFactory.getCurrentSession().createQuery( "select p.programAttributes from Program p" );

        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = (List<ProgramTrackedEntityAttribute>) query.list();

        for ( ProgramTrackedEntityAttribute programTrackedEntityAttribute : programTrackedEntityAttributes )
        {
            if ( !result.containsKey( programTrackedEntityAttribute.getProgram() ) )
            {
                result.put( programTrackedEntityAttribute.getProgram(), Sets.newHashSet( programTrackedEntityAttribute.getAttribute() ) );
            }
            else
            {
                result.get( programTrackedEntityAttribute.getProgram() ).add( programTrackedEntityAttribute.getAttribute() );
            }
        }
        return result;
    }
}
