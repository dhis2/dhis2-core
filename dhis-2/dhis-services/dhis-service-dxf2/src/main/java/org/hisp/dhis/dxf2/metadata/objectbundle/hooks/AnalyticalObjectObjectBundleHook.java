package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hibernate.Session;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElementCategoryDimension;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.springframework.core.annotation.Order;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Order( 10 )
public class AnalyticalObjectObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !AnalyticalObject.class.isInstance( object ) ) return;
        BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
        Schema schema = schemaService.getDynamicSchema( analyticalObject.getClass() );
        Session session = sessionFactory.getCurrentSession();

        handleDataDimensionItems( session, schema, analyticalObject, bundle );
        handleCategoryDimensions( session, schema, analyticalObject, bundle );
        handleDataElementDimensions( session, schema, analyticalObject, bundle );
        handleAttributeDimensions( session, schema, analyticalObject, bundle );
        handleProgramIndicatorDimensions( session, schema, analyticalObject, bundle );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !AnalyticalObject.class.isInstance( object ) ) return;
        BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
        Schema schema = schemaService.getDynamicSchema( analyticalObject.getClass() );
        Session session = sessionFactory.getCurrentSession();

        handleDataDimensionItems( session, schema, analyticalObject, bundle );
        handleCategoryDimensions( session, schema, analyticalObject, bundle );
        handleDataElementDimensions( session, schema, analyticalObject, bundle );
        handleAttributeDimensions( session, schema, analyticalObject, bundle );
        handleProgramIndicatorDimensions( session, schema, analyticalObject, bundle );
    }

    private void handleDataDimensionItems( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "dataDimensionItems" ) ) return;

        for ( DataDimensionItem dataDimensionItem : analyticalObject.getDataDimensionItems() )
        {
            preheatService.connectReferences( dataDimensionItem, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            if ( dataDimensionItem.getDataElementOperand() != null )
            {
                preheatService.connectReferences( dataDimensionItem.getDataElementOperand(), bundle.getPreheat(), bundle.getPreheatIdentifier() );
                session.save( dataDimensionItem.getDataElementOperand() );
            }

            if ( dataDimensionItem.getReportingRate() != null )
            {
                preheatService.connectReferences( dataDimensionItem.getReportingRate(), bundle.getPreheat(), bundle.getPreheatIdentifier() );
            }

            preheatService.connectReferences( dataDimensionItem, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( dataDimensionItem );
        }
    }

    private void handleCategoryDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "categoryDimensions" ) ) return;

        for ( DataElementCategoryDimension categoryDimension : analyticalObject.getCategoryDimensions() )
        {
            preheatService.connectReferences( categoryDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( categoryDimension );
        }
    }

    private void handleDataElementDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "dataElementDimensions" ) ) return;

        for ( TrackedEntityDataElementDimension dataElementDimension : analyticalObject.getDataElementDimensions() )
        {
            preheatService.connectReferences( dataElementDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( dataElementDimension );
        }
    }

    private void handleAttributeDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "attributeDimensions" ) ) return;

        for ( TrackedEntityAttributeDimension attributeDimension : analyticalObject.getAttributeDimensions() )
        {
            preheatService.connectReferences( attributeDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( attributeDimension );
        }
    }

    private void handleProgramIndicatorDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "programIndicatorDimensions" ) ) return;

        for ( TrackedEntityProgramIndicatorDimension programIndicatorDimension : analyticalObject.getProgramIndicatorDimensions() )
        {
            preheatService.connectReferences( programIndicatorDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( programIndicatorDimension );
        }
    }
}
