package org.hisp.dhis.dd.action.dataelement;

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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.user.UserSettingService.KEY_CURRENT_DOMAIN_TYPE;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.UserSettingService;

/**
 * @author Torgeir Lorange Ostby
 */
public class GetDataElementListAction
    extends ActionPagingSupport<DataElement>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<DataElement> dataElements;

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String domainType;

    public String getDomainType()
    {
        return domainType;
    }

    public void setDomainType( String domainType )
    {
        this.domainType = domainType;
    }

    private String key;

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        if ( domainType == null ) // None, get current domain type
        {
            domainType = (String) userSettingService.getUserSetting( KEY_CURRENT_DOMAIN_TYPE );
        }
        else if ( "all".equals( domainType ) ) // All, reset current domain type
        {
            userSettingService.saveUserSetting( KEY_CURRENT_DOMAIN_TYPE, null );

            domainType = null;
        }
        else  // Specified, set current domain type
        {
            userSettingService.saveUserSetting( KEY_CURRENT_DOMAIN_TYPE, domainType );
        }

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        if ( isNotBlank( key ) ) // Filter on key only if set
        {
            this.paging = createPaging( dataElementService.getDataElementCountByName( key ) );

            dataElements = dataElementService.getDataElementsBetweenByName( key, paging.getStartPos(), paging.getPageSize() );
        }
        else if ( domainType != null )
        {
            DataElementDomain deDomainType = DataElementDomain.fromValue( domainType );
            
            this.paging = createPaging( dataElementService.getDataElementCountByDomainType( deDomainType ) );

            dataElements = dataElementService.getDataElementsByDomainType( deDomainType, paging.getStartPos(), paging.getPageSize() );
        }
        else
        {
            this.paging = createPaging( dataElementService.getDataElementCount() );

            dataElements = dataElementService.getDataElementsBetween( paging.getStartPos(), paging.getPageSize() );
        }

        Collections.sort( dataElements, new IdentifiableObjectNameComparator() );

        return SUCCESS;
    }
}