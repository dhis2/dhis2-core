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
package org.hisp.dhis.webapi.controller.tracker.export.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPreset;

/**
 * Abstract class that provides basic methods to transform input fields into
 * {@link FieldPath } based on {@link FieldFilterParser }.
 *
 * It will init TrackedEntityInstanceParams based on inclusion or exclusion of
 * Fields Paths see {@link #initUsingAllOrNoFields}
 *
 * It also provides reusable criteria of inclusion or exclusion (e.g
 * relationships or attributes)
 */
abstract class EntitiesFieldsSupportService
{
    protected static final String FIELD_RELATIONSHIPS = "relationships";

    protected static final String FIELD_EVENTS = "events";

    protected static final String FIELD_ATTRIBUTES = "attributes";

    /**
     * Parse the fields query parameter values to determine which resources
     * should be fetched from the DB.
     *
     * @param fields fields query parameter values
     * @return tracked entity instance parameters
     */
    abstract TrackedEntityInstanceParams getTrackedEntityInstanceParams( List<String> fields );

    protected Map<String, FieldPath> getRoots( List<String> fields )
    {
        return rootFields( getFieldPaths( fields ) );
    }

    protected List<FieldPath> getFieldPaths( List<String> fields )
    {
        return FieldFilterParser
            .parse( Collections.singleton( StringUtils.join( fields, "," ) ) );
    }

    protected Map<String, FieldPath> rootFields( List<FieldPath> fieldPaths )
    {
        Map<String, FieldPath> roots = new HashMap<>();
        for ( FieldPath p : fieldPaths )
        {
            if ( p.isRoot() && (!roots.containsKey( p.getName() ) || p.isExclude()) )
            {
                roots.put( p.getName(), p );
            }
        }
        return roots;
    }

    protected TrackedEntityInstanceParams initUsingAllOrNoFields( Map<String, FieldPath> roots )
    {
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        if ( roots.containsKey( FieldPreset.ALL ) )
        {
            FieldPath p = roots.get( FieldPreset.ALL );
            if ( p.isRoot() && !p.isExclude() )
            {
                params = TrackedEntityInstanceParams.TRUE;
            }
        }
        return params;
    }

    protected TrackedEntityInstanceParams withFieldRelationships( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_RELATIONSHIPS )
            ? params.withIncludeRelationships( !roots.get( FIELD_RELATIONSHIPS ).isExclude() )
            : params;
    }

    protected TrackedEntityInstanceParams withFieldEvents(
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_EVENTS ) ? params.withIncludeEvents( !roots.get( FIELD_EVENTS ).isExclude() )
            : params;
    }

    protected TrackedEntityInstanceParams withFieldEAttributes( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_ATTRIBUTES )
            ? params.withIncludeAttributes( !roots.get( FIELD_ATTRIBUTES ).isExclude() )
            : params;
    }
}
