package org.hisp.dhis.web.ohie.fred.webapi.v1.validation.constraint;

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

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.web.ohie.fred.webapi.v1.validation.constraint.annotation.ValidProperties;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PropertiesValidator implements ConstraintValidator<ValidProperties, Map<String, Object>>
{
    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Override
    public void initialize( ValidProperties constraintAnnotation )
    {
    }

    @Override
    public boolean isValid( Map<String, Object> values, ConstraintValidatorContext context )
    {
        boolean returnValue = true;

        boolean validParent = validateParent( values );

        if ( !validParent )
        {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate( String.format( "parent does not point to a valid facility." ) )
                .addNode( "parent" )
                .addConstraintViolation();

            returnValue = false;
        }

        boolean validDataSets = validateDataSets( values );

        if ( !validDataSets )
        {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate( String.format( "dataSets has one or more non-matching IDs." ) )
                .addNode( "dataSets" )
                .addConstraintViolation();

            returnValue = false;
        }

        // TODO should check for code also, but we don't know if its an update, or a creation.. split validator into create/update?

        return returnValue;
    }

    private boolean validateParent( Map<String, Object> values )
    {
        String parentId = (String) values.get( "parent" );

        if ( parentId == null )
        {
            return true;
        }

        OrganisationUnit organisationUnit = identifiableObjectManager.get( OrganisationUnit.class, parentId );

        if ( organisationUnit != null )
        {
            return true;
        }

        organisationUnit = organisationUnitService.getOrganisationUnitByUuid( parentId );

        if ( organisationUnit != null )
        {
            return true;
        }

        return false;
    }

    @SuppressWarnings( "unchecked" )
    private boolean validateDataSets( Map<String, Object> values )
    {
        Collection<String> dataSetIds = (Collection<String>) values.get( "dataSets" );

        if ( dataSetIds != null )
        {
            for ( String dataSetId : dataSetIds )
            {
                DataSet dataSet = identifiableObjectManager.get( DataSet.class, dataSetId );

                if ( dataSet == null )
                {
                    return false;
                }
            }
        }

        return true;
    }
}
