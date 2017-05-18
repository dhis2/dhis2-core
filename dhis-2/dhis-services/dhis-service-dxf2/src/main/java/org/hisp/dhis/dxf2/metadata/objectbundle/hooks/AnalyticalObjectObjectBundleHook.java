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

import org.hibernate.Session;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.CategoryDimension;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

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
            if ( dataDimensionItem == null )
            {
                continue;
            }

            dataDimensionItem.setDataElement( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getDataElement() ) );
            dataDimensionItem.setIndicator( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getIndicator() ) );
            dataDimensionItem.setProgramIndicator( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getProgramIndicator() ) );
            dataDimensionItem.setProgramAttribute( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getProgramAttribute() ) );
            dataDimensionItem.setProgramDataElement( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getProgramDataElement() ) );

            if ( dataDimensionItem.getDataElementOperand() != null )
            {
                preheatService.connectReferences( dataDimensionItem.getDataElementOperand(), bundle.getPreheat(), bundle.getPreheatIdentifier() );
                session.save( dataDimensionItem.getDataElementOperand() );
            }

            if ( dataDimensionItem.getReportingRate() != null )
            {
                dataDimensionItem.getReportingRate().setDataSet( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                    dataDimensionItem.getReportingRate().getDataSet() ) );
            }

            preheatService.connectReferences( dataDimensionItem, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( dataDimensionItem );
        }
    }

    private void handleCategoryDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "categoryDimensions" ) ) return;

        for ( CategoryDimension categoryDimension : analyticalObject.getCategoryDimensions() )
        {
            if ( categoryDimension == null )
            {
                continue;
            }

            categoryDimension.setDimension( bundle.getPreheat().get( bundle.getPreheatIdentifier(), categoryDimension.getDimension() ) );
            List<DataElementCategoryOption> categoryOptions = new ArrayList<>( categoryDimension.getItems() );
            categoryDimension.getItems().clear();

            categoryOptions.forEach( co ->
            {
                DataElementCategoryOption categoryOption = bundle.getPreheat().get( bundle.getPreheatIdentifier(), co );
                if ( categoryOption != null ) categoryDimension.getItems().add( categoryOption );
            } );

            preheatService.connectReferences( categoryDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( categoryDimension );
        }
    }

    private void handleDataElementDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "dataElementDimensions" ) ) return;

        for ( TrackedEntityDataElementDimension dataElementDimension : analyticalObject.getDataElementDimensions() )
        {
            if ( dataElementDimension == null )
            {
                continue;
            }

            dataElementDimension.setDataElement( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataElementDimension.getDataElement() ) );
            dataElementDimension.setLegendSet( bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataElementDimension.getLegendSet() ) );

            preheatService.connectReferences( dataElementDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( dataElementDimension );
        }
    }

    private void handleAttributeDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "attributeDimensions" ) ) return;

        for ( TrackedEntityAttributeDimension attributeDimension : analyticalObject.getAttributeDimensions() )
        {
            if ( attributeDimension == null )
            {
                continue;
            }

            attributeDimension.setAttribute( bundle.getPreheat().get( bundle.getPreheatIdentifier(), attributeDimension.getAttribute() ) );
            attributeDimension.setLegendSet( bundle.getPreheat().get( bundle.getPreheatIdentifier(), attributeDimension.getLegendSet() ) );

            preheatService.connectReferences( attributeDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            session.save( attributeDimension );
        }
    }

    private void handleProgramIndicatorDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "programIndicatorDimensions" ) ) return;

        for ( TrackedEntityProgramIndicatorDimension programIndicatorDimension : analyticalObject.getProgramIndicatorDimensions() )
        {
            if ( programIndicatorDimension == null )
            {
                continue;
            }

            programIndicatorDimension.setProgramIndicator( bundle.getPreheat().get( bundle.getPreheatIdentifier(), programIndicatorDimension.getProgramIndicator() ) );
            programIndicatorDimension.setLegendSet( bundle.getPreheat().get( bundle.getPreheatIdentifier(), programIndicatorDimension.getLegendSet() ) );

            preheatService.connectReferences( programIndicatorDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( programIndicatorDimension );
        }
    }
}
