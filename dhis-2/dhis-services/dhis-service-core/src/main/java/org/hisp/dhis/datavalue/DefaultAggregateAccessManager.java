package org.hisp.dhis.datavalue;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class DefaultAggregateAccessManager implements AggregateAccessManager
{
    private final AclService aclService;

    public DefaultAggregateAccessManager( AclService aclService )
    {
        this.aclService = aclService;
    }

    @Override
    public List<String> canWrite( User user, DataValue dataValue )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        DataElement dataElement = dataValue.getDataElement();

        if ( !aclService.canRead( user, dataElement ) )
        {
            errors.add( "User has no read access for DataElement: " + dataElement.getUid() );
        }

        Set<DataElementCategoryOption> options = new HashSet<>();

        DataElementCategoryOptionCombo categoryOptionCombo = dataValue.getCategoryOptionCombo();

        if ( categoryOptionCombo != null )
        {
            options.addAll( categoryOptionCombo.getCategoryOptions() );
        }

        DataElementCategoryOptionCombo attributeOptionCombo = dataValue.getAttributeOptionCombo();

        if ( attributeOptionCombo != null )
        {
            options.addAll( attributeOptionCombo.getCategoryOptions() );
        }

        options.forEach( option -> {
            if ( !option.isDefault() && !aclService.canDataWrite( user, option ) )
            {
                errors.add( "User has no data write access for CategoryOption: " + option.getUid() );
            }
        } );

        return errors;
    }

    @Override
    public List<String> canRead( User user, DataValue dataValue )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        DataElement dataElement = dataValue.getDataElement();

        if ( !aclService.canRead( user, dataElement ) )
        {
            errors.add( "User has no read access for DataElement: " + dataElement.getUid() );
        }

        Set<DataElementCategoryOption> options = new HashSet<>();

        DataElementCategoryOptionCombo categoryOptionCombo = dataValue.getCategoryOptionCombo();

        if ( categoryOptionCombo != null )
        {
            options.addAll( categoryOptionCombo.getCategoryOptions() );
        }

        DataElementCategoryOptionCombo attributeOptionCombo = dataValue.getAttributeOptionCombo();

        if ( attributeOptionCombo != null )
        {
            options.addAll( attributeOptionCombo.getCategoryOptions() );
        }

        options.forEach( option -> {

            if ( !option.isDefault() && !aclService.canDataRead( user, option ) )
            {
                errors.add( "User has no data read access for CategoryOption: " + option.getUid() );
            }
        } );

        return errors;
    }

    @Override
    public List<String> canWrite( User user, DataSet dataSet )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        if ( !aclService.canDataWrite( user, dataSet ) )
        {
            errors.add( "User does not have write access for DataSet: " + dataSet.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, DataSet dataSet )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        if ( !aclService.canDataRead( user, dataSet ) )
        {
            errors.add( "User does not have read access for DataSet: " + dataSet.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, DataElementCategoryOptionCombo optionCombo )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        Set<DataElementCategoryOption> options = optionCombo.getCategoryOptions();

        options.forEach( attrOption -> {
            if ( !attrOption.isDefault() && !aclService.canDataWrite( user, attrOption ) )
            {
                errors.add( "User has no data write access for CategoryOption: " + attrOption.getUid() );
            }
        } );

        return errors;
    }

    @Override
    public List<String> canRead( User user, DataElementCategoryOptionCombo optionCombo )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        Set<DataElementCategoryOption> options = optionCombo.getCategoryOptions();

        options.forEach( attrOption -> {
            if ( !aclService.canDataRead( user, attrOption ) )
            {
                errors.add( "User has no data read access for CategoryOption: " + attrOption.getUid() );
            }
        } );

        return errors;
    }

    @Override
    public List<String> canWrite( User user, DataElementOperand dataElementOperand )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        DataElement dataElement = dataElementOperand.getDataElement();

        if ( !aclService.canRead( user, dataElement ) )
        {
            errors.add( "User has no read access for DataElement: " + dataElement.getUid() );
        }

        Set<DataElementCategoryOption> options = new HashSet<>();

        DataElementCategoryOptionCombo categoryOptionCombo = dataElementOperand.getCategoryOptionCombo();

        if ( categoryOptionCombo != null )
        {
            options.addAll( categoryOptionCombo.getCategoryOptions() );
        }

        DataElementCategoryOptionCombo attributeOptionCombo = dataElementOperand.getAttributeOptionCombo();

        if ( attributeOptionCombo != null )
        {
            options.addAll( attributeOptionCombo.getCategoryOptions() );
        }

        options.forEach( option -> {
            if ( !option.isDefault() && !aclService.canDataWrite( user, option ) )
            {
                errors.add( "User has no data write access for CategoryOption: " + option.getUid() );
            }
        } );

        return errors;
    }
}