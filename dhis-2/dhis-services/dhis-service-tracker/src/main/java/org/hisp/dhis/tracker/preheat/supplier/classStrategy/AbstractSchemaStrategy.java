package org.hisp.dhis.tracker.preheat.supplier.classStrategy;

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

import java.util.List;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;

/**
 * Abstract Tracker Preheat strategy that applies to strategies that employ the
 * generic {@link QueryService} to fetch data
 * 
 * @author Luciano Fiandesio
 */
public abstract class AbstractSchemaStrategy implements ClassBasedSupplierStrategy
{
    protected final SchemaService schemaService;

    private final QueryService queryService;

    private final IdentifiableObjectManager manager;

    public AbstractSchemaStrategy( SchemaService schemaService, QueryService queryService,
        IdentifiableObjectManager manager )
    {
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.manager = manager;
    }

    @Override
    public void add( TrackerPreheatParams params, List<List<String>> splitList, TrackerPreheat preheat )
    {
        TrackerIdentifier identifier = params.getIdentifiers().getByClass( getSchemaClass() );
        Schema schema = schemaService.getDynamicSchema( getSchemaClass() );

        queryForIdentifiableObjects( preheat, schema, identifier, splitList );
    }

    protected Class<?> getSchemaClass()
    {
        return getClass().getAnnotation( StrategyFor.class ).value();
    }

    @SuppressWarnings( "unchecked" )
    protected void queryForIdentifiableObjects( TrackerPreheat preheat, Schema schema, TrackerIdentifier identifier,
        List<List<String>> splitList )
    {

        TrackerIdScheme idScheme = identifier.getIdScheme();
        for ( List<String> ids : splitList )
        {
            List<? extends IdentifiableObject> objects;

            if ( TrackerIdScheme.ATTRIBUTE.equals( idScheme ) )
            {
                Attribute attribute = new Attribute();
                attribute.setUid( identifier.getValue() );
                objects = manager.getAllByAttributeAndValues(
                    (Class<? extends IdentifiableObject>) schema.getKlass(), attribute, ids );
            }
            else
            {
                Query query = Query.from( schema );
                query.setUser( preheat.getUser() );
                query.add( generateRestrictionFromIdentifiers( idScheme, ids ) );
                query.setDefaults( Defaults.INCLUDE );
                objects = queryService.query( query );
            }

            preheat.put( identifier, objects );
        }
    }

    private Restriction generateRestrictionFromIdentifiers( TrackerIdScheme idScheme, List<String> ids )
    {
        if ( TrackerIdScheme.CODE.equals( idScheme ) )
        {
            return Restrictions.in( "code", ids );
        }
        else
        {
            return Restrictions.in( "id", ids );
        }
    }

}