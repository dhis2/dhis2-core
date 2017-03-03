package org.hisp.dhis.schema.validation;

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

import org.apache.commons.validator.GenericValidator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultSchemaValidator implements SchemaValidator
{
    private Pattern BCRYPT_PATTERN = Pattern.compile( "\\A\\$2a?\\$\\d\\d\\$[./0-9A-Za-z]{53}" );

    @Autowired
    private SchemaService schemaService;

    @Override
    public List<ErrorReport> validate( Object object )
    {
        return validate( object, true );
    }

    @Override
    public List<ErrorReport> validate( Object object, boolean persisted )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null )
        {
            return errorReports;
        }

        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( schema == null )
        {
            return errorReports;
        }

        Class<?> klass = object.getClass();

        for ( Property property : schema.getProperties() )
        {
            if ( persisted && !property.isPersisted() )
            {
                continue;
            }

            Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( value == null )
            {
                if ( property.isRequired() && !Preheat.isDefaultClass( property.getKlass() ) )
                {
                    errorReports.add( new ErrorReport( klass, ErrorCode.E4000, property.getName() )
                        .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
                }

                continue;
            }

            errorReports.addAll( validateString( klass, value, property ) );
            errorReports.addAll( validateCollection( klass, value, property ) );
            errorReports.addAll( validateInteger( klass, value, property ) );
            errorReports.addAll( validateFloat( klass, value, property ) );
            errorReports.addAll( validateDouble( klass, value, property ) );
        }

        if ( User.class.isInstance( object ) )
        {
            User user = (User) object;

            if ( user.getUserCredentials() != null )
            {
                errorReports.addAll( validate( user.getUserCredentials(), persisted ) );
            }
        }

        return errorReports;
    }

    private List<? extends ErrorReport> validateString( Class<?> klass, Object propertyObject, Property property )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        // TODO How should empty strings be handled? they are not valid color, password, url, etc of course.
        if ( !String.class.isInstance( propertyObject ) || StringUtils.isEmpty( propertyObject ) )
        {
            return errorReports;
        }

        String value = (String) propertyObject;

        // check column max length
        if ( value.length() > property.getLength() )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4001, property.getName(), property.getLength(), value.length() )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
            return errorReports;
        }

        if ( value.length() < property.getMin() || value.length() > property.getMax() )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4002, property.getName(), property.getMin(), property.getMax(), value.length() )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        if ( PropertyType.EMAIL == property.getPropertyType() && !GenericValidator.isEmail( value ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4003, property.getName(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }
        else if ( PropertyType.URL == property.getPropertyType() && !isUrl( value ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4004, property.getName(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }
        else if ( !BCRYPT_PATTERN.matcher( value ).matches() && PropertyType.PASSWORD == property.getPropertyType() && !ValidationUtils.passwordIsValid( value ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4005, property.getName(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }
        else if ( PropertyType.COLOR == property.getPropertyType() && !ValidationUtils.isValidHexColor( value ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4006, property.getName(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        /* TODO add proper validation for both Points and Polygons, ValidationUtils only supports points at this time
        if ( PropertyType.GEOLOCATION == property.getPropertyType() && !ValidationUtils.coordinateIsValid( value ) )
        {
            validationViolations.add( new ValidationViolation( "Value is not a valid coordinate pair [lon, lat]." ) );
        }
        */

        return errorReports;
    }

    // Commons validator have some issues in latest version, replacing with a very simple test for now
    private boolean isUrl( String url )
    {
        return !StringUtils.isEmpty( url ) && (url.startsWith( "http://" ) || url.startsWith( "https://" ));
    }

    private List<? extends ErrorReport> validateCollection( Class<?> klass, Object propertyObject, Property property )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( !Collection.class.isInstance( propertyObject ) )
        {
            return errorReports;
        }

        Collection<?> value = (Collection<?>) propertyObject;

        if ( value.size() < property.getMin() || value.size() > property.getMax() )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4007, property.getName(), property.getMin(), property.getMax(), value.size() )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        return errorReports;
    }

    private List<? extends ErrorReport> validateInteger( Class<?> klass, Object propertyObject, Property property )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( !Integer.class.isInstance( propertyObject ) )
        {
            return errorReports;
        }

        Integer value = (Integer) propertyObject;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4008, property.getName(), property.getMin(), property.getMax(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        return errorReports;
    }

    private List<? extends ErrorReport> validateFloat( Class<?> klass, Object propertyObject, Property property )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( !Float.class.isInstance( propertyObject ) )
        {
            return errorReports;
        }

        Float value = (Float) propertyObject;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4008, property.getName(), property.getMin(), property.getMax(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        return errorReports;
    }

    private List<? extends ErrorReport> validateDouble( Class<?> klass, Object propertyObject, Property property )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( !Double.class.isInstance( propertyObject ) )
        {
            return errorReports;
        }

        Double value = (Double) propertyObject;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            errorReports.add( new ErrorReport( klass, ErrorCode.E4008, property.getName(), property.getMin(), property.getMax(), value )
                .setErrorKlass( property.getKlass() ).setErrorProperty( property.getName() ) );
        }

        return errorReports;
    }
}
