package org.hisp.dhis.webapi.view;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.PredicateUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.springframework.web.servlet.view.AbstractView;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractGridView extends AbstractView
{
    protected abstract void renderGrids( List<Grid> grids, HttpServletResponse response ) throws Exception;

    @Override
    protected void renderMergedOutputModel( Map<String, Object> model, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Object object = model.get( "model" );

        List<Grid> grids = new ArrayList<>();

        if ( WebMetaData.class.isAssignableFrom( object.getClass() ) )
        {
            WebMetaData webMetaData = (WebMetaData) object;
            Collection<Field> fields = ReflectionUtils.collectFields( WebMetaData.class, PredicateUtils.idObjectCollections );

            for ( Field field : fields )
            {
                List<IdentifiableObject> identifiableObjects = ReflectionUtils.invokeGetterMethod( field.getName(), webMetaData );

                if ( identifiableObjects.isEmpty() )
                {
                    continue;
                }

                Grid grid = new ListGrid();
                grid.setTitle( identifiableObjects.get( 0 ).getClass().getSimpleName() + "s" );

                boolean nameable = false;

                grid.addHeader( new GridHeader( "UID", false, false ) );
                grid.addHeader( new GridHeader( "Name", false, false ) );

                if ( NameableObject.class.isAssignableFrom( identifiableObjects.get( 0 ).getClass() ) )
                {
                    grid.addHeader( new GridHeader( "ShortName", false, false ) );
                    nameable = true;
                }

                grid.addHeader( new GridHeader( "Code", false, false ) );

                for ( IdentifiableObject identifiableObject : identifiableObjects )
                {
                    grid.addRow();
                    grid.addValue( identifiableObject.getUid() );
                    grid.addValue( identifiableObject.getName() );

                    if ( nameable )
                    {
                        grid.addValue( ((NameableObject) identifiableObject).getShortName() );
                    }

                    grid.addValue( identifiableObject.getCode() );
                }

                grids.add( grid );
            }
        }
        else
        {
            IdentifiableObject identifiableObject = (IdentifiableObject) object;

            Grid grid = new ListGrid();
            grid.setTitle( identifiableObject.getClass().getSimpleName() );
            grid.addEmptyHeaders( 2 );

            grid.addRow().addValue( "UID" ).addValue( identifiableObject.getUid() );
            grid.addRow().addValue( "Name" ).addValue( identifiableObject.getName() );

            if ( NameableObject.class.isAssignableFrom( identifiableObject.getClass() ) )
            {
                grid.addRow().addValue( "ShortName" ).addValue( ((NameableObject) identifiableObject).getShortName() );
                grid.addRow().addValue( "Description" ).addValue( ((NameableObject) identifiableObject).getDescription() );
            }

            grid.addRow().addValue( "Code" ).addValue( identifiableObject.getCode() );

            grids.add( grid );
        }

        renderGrids( grids, response );
    }
}
