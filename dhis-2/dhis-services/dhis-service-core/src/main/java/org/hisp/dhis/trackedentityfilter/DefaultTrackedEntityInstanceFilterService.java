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
package org.hisp.dhis.trackedentityfilter;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@Service( "org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilterService" )
public class DefaultTrackedEntityInstanceFilterService
    implements TrackedEntityInstanceFilterService
{

    private final TrackedEntityInstanceFilterStore trackedEntityInstanceFilterStore;

    private final ProgramService programService;

    private final TrackedEntityAttributeService teaService;

    public DefaultTrackedEntityInstanceFilterService(
        TrackedEntityInstanceFilterStore trackedEntityInstanceFilterStore, ProgramService programService,
        TrackedEntityAttributeService teaService )
    {
        checkNotNull( trackedEntityInstanceFilterStore );
        checkNotNull( programService );

        this.trackedEntityInstanceFilterStore = trackedEntityInstanceFilterStore;
        this.programService = programService;
        this.teaService = teaService;
    }

    // -------------------------------------------------------------------------
    // TrackedEntityInstanceFilterService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long add( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {
        trackedEntityInstanceFilterStore.save( trackedEntityInstanceFilter );
        return trackedEntityInstanceFilter.getId();
    }

    @Override
    @Transactional
    public void delete( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {
        trackedEntityInstanceFilterStore.delete( trackedEntityInstanceFilter );
    }

    @Override
    @Transactional
    public void update( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {
        trackedEntityInstanceFilterStore.update( trackedEntityInstanceFilter );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityInstanceFilter get( long id )
    {
        return trackedEntityInstanceFilterStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityInstanceFilter> getAll()
    {
        return trackedEntityInstanceFilterStore.getAll();
    }

    @Override
    public List<String> validate( TrackedEntityInstanceFilter teiFilter )
    {
        List<String> errors = new ArrayList<>();

        if ( teiFilter.getProgram() != null && !StringUtils.isEmpty( teiFilter.getProgram().getUid() ) )
        {
            Program pr = programService.getProgram( teiFilter.getProgram().getUid() );

            if ( pr == null )
            {
                errors.add( "Program is specified but does not exist: " + teiFilter.getProgram().getUid() );
            }
        }

        EntityQueryCriteria eqc = teiFilter.getEntityQueryCriteria();

        if ( eqc == null )
        {
            return errors;
        }

        List<AttributeValueFilter> attributeValueFilters = eqc.getAttributeValueFilters();
        if ( !CollectionUtils.isEmpty( attributeValueFilters ) )
        {
            attributeValueFilters.forEach( avf -> {
                if ( StringUtils.isEmpty( avf.getAttribute() ) )
                {
                    errors.add( "Attribute Uid is missing in filter" );
                }
                else
                {
                    TrackedEntityAttribute tea = teaService.getTrackedEntityAttribute( avf.getAttribute() );
                    if ( tea == null )
                    {
                        errors.add( "No tracked entity attribute found for attribute:" + avf.getAttribute() );
                    }
                }
            } );
        }
        return errors;
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityInstanceFilter> get( Program program )
    {
        return trackedEntityInstanceFilterStore.get( program );
    }
}
