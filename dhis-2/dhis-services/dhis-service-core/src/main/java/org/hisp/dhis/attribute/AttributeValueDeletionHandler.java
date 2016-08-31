package org.hisp.dhis.attribute;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public class AttributeValueDeletionHandler
    extends DeletionHandler
{
    private AttributeService attributeService;

    public void setAttributeService( AttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return AttributeValue.class.getSimpleName();
    }

    @Override
    public String allowDeleteAttribute( Attribute attribute )
    {
        String sql = "select count(*) from attributevalue where attributeid=" + attribute.getId();

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? null : ERROR;
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        removeAttributeValues( dataElement.getAttributeValues() );
    }

    @Override
    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        removeAttributeValues( dataElementGroup.getAttributeValues() );
    }

    @Override
    public void deleteIndicator( Indicator indicator )
    {
        removeAttributeValues( indicator.getAttributeValues() );
    }

    @Override
    public void deleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        removeAttributeValues( indicatorGroup.getAttributeValues() );
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        removeAttributeValues( organisationUnit.getAttributeValues() );
    }

    @Override
    public void deleteOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        removeAttributeValues( organisationUnitGroup.getAttributeValues() );
    }

    @Override
    public void deleteUser( User user )
    {
        removeAttributeValues( user.getAttributeValues() );
    }

    @Override
    public void deleteUserGroup( UserGroup userGroup )
    {
        removeAttributeValues( userGroup.getAttributeValues() );
    }

    private void removeAttributeValues( Set<AttributeValue> attributeValues )
    {
        Iterator<AttributeValue> iterator = attributeValues.iterator();

        while ( iterator.hasNext() )
        {
            AttributeValue attributeValue = iterator.next();
            iterator.remove();
            attributeService.deleteAttributeValue( attributeValue );
        }
    }
}
