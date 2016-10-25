package org.hisp.dhis.webapi.controller.dataelement;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.DataElementOperandSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DataElementOperandSchemaDescriptor.API_ENDPOINT )
public class DataElementOperandController
    extends AbstractCrudController<DataElementOperand>
{
    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<DataElementOperand> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        List<DataElementOperand> dataElementOperands;

        if ( options.isTrue( "persisted" ) )
        {
            dataElementOperands = Lists.newArrayList( manager.getAll( DataElementOperand.class ) );
        }
        else
        {
            boolean totals = options.isTrue( "totals" );

            String deg = CollectionUtils.popStartsWith( filters, "dataElement.dataElementGroups.id:eq:" );
            deg = deg != null ? deg.substring( "dataElement.dataElementGroups.id:eq:".length() ) : null;
            
            String ds = options.get( "dataSet" );

            if ( deg != null )
            {
                DataElementGroup dataElementGroup = manager.get( DataElementGroup.class, deg );
                dataElementOperands = dataElementCategoryService.getOperands( dataElementGroup.getMembers(), totals );
            }
            else if ( ds != null )
            {
                DataSet dataSet = manager.get( DataSet.class, ds );
                dataElementOperands = dataElementCategoryService.getOperands( dataSet, totals );
            }
            else
            {
                List<DataElement> dataElements = new ArrayList<>( manager.getAllSorted( DataElement.class ) );
                dataElementOperands = dataElementCategoryService.getOperands( dataElements, totals );
            }
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, options.getRootJunction() );
        query.setDefaultOrder();
        query.setObjects( dataElementOperands );

        dataElementOperands = (List<DataElementOperand>) queryService.query( query );

        return dataElementOperands;
    }
}
