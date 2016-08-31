package org.hisp.dhis.dd.action.dataelementgroupset;

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

import com.opensymphony.xwork2.Action;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tran Thanh Tri
 */
public class UpdateDataElementGroupSetAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }
    
    private boolean compulsory;

    public void setCompulsory( boolean compulsory )
    {
        this.compulsory = compulsory;
    }

    private boolean dataDimension;

    public void setDataDimension( boolean dataDimension )
    {
        this.dataDimension = dataDimension;
    }

    private List<String> degSelected = new ArrayList<>();

    public void setDegSelected( List<String> degSelected )
    {
        this.degSelected = degSelected;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        DataElementGroupSet dataElementGroupSet = dataElementService.getDataElementGroupSet( id );

        dataElementGroupSet.setName( StringUtils.trimToNull( name ) );
        dataElementGroupSet.setCode( StringUtils.trimToNull( code ) );
        dataElementGroupSet.setDescription( StringUtils.trimToNull( description ) );
        dataElementGroupSet.setCompulsory( compulsory );
        dataElementGroupSet.setDataDimension( dataDimension );

        dataElementGroupSet.getMembers().clear();

        for ( String id : degSelected )
        {
            dataElementGroupSet.getMembers().add( dataElementService.getDataElementGroup( id ) );
        }

        dataElementService.updateDataElementGroupSet( dataElementGroupSet );

        return SUCCESS;
    }
}
