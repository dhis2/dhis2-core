package org.hisp.dhis.dxf2.schema;

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

import org.apache.commons.validator.GenericValidator;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultSchemaValidator implements SchemaValidator
{
    @Autowired
    private SchemaService schemaService;

    @Override
    public List<ValidationViolation> validate( Object object )
    {
        return validate( object, true );
    }

    @Override
    public List<ValidationViolation> validate( Object object, boolean persisted )
    {
        if ( object == null || schemaService.getSchema( object.getClass() ) == null )
        {
            return new ArrayList<>();
        }

        Schema schema = schemaService.getSchema( object.getClass() );

        List<ValidationViolation> validationViolations = new ArrayList<>();

        for ( Property property : schema.getProperties() )
        {
            if ( persisted && !property.isPersisted() )
            {
                continue;
            }

            Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( value == null )
            {
                if ( property.isRequired() )
                {
                    validationViolations.add( new ValidationViolation( property.getName(), "Required property missing." ) );
                }

                continue;
            }

            validationViolations.addAll( validateString( value, property ) );
            validationViolations.addAll( validateCollection( value, property ) );
            validationViolations.addAll( validateInteger( value, property ) );
            validationViolations.addAll( validateFloat( value, property ) );
            validationViolations.addAll( validateDouble( value, property ) );
        }

        return validationViolations;
    }

    private Collection<? extends ValidationViolation> validateString( Object object, Property property )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        // TODO How should empty strings be handled? they are not valid color, password, url, etc of course.
        if ( !String.class.isInstance( object ) || StringUtils.isEmpty( object ) )
        {
            return validationViolations;
        }

        String value = (String) object;

        // check column max length
        if ( value.length() > property.getLength() )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Maximum length for property is "
                + property.getLength() + ", length is " + value.length(), value ) );

            return validationViolations;
        }

        if ( value.length() < property.getMin() || value.length() > property.getMax() )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Allowed range for length ["
                + property.getMin() + ", " + property.getMax() + "], length is " + value.length(), value ) );
        }

        if ( PropertyType.EMAIL == property.getPropertyType() && !GenericValidator.isEmail( value ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Not a valid email.", value ) );
        }
        else if ( PropertyType.URL == property.getPropertyType() && !isUrl( value ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Not a valid URL.", value ) );
        }
        else if ( PropertyType.PASSWORD == property.getPropertyType() && !ValidationUtils.passwordIsValid( value ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Not a valid password.", value ) );
        }
        else if ( PropertyType.COLOR == property.getPropertyType() && !ValidationUtils.isValidHexColor( value ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Not a valid hex color.", value ) );
        }

        /* TODO add proper validation for both Points and Polygons, ValidationUtils only supports points at this time
        if ( PropertyType.GEOLOCATION == property.getPropertyType() && !ValidationUtils.coordinateIsValid( value ) )
        {
            validationViolations.add( new ValidationViolation( "Value is not a valid coordinate pair [lon, lat]." ) );
        }
        */

        return validationViolations;
    }

    // Commons validator have some issues in latest version, replacing with a very simple test for now
    private boolean isUrl( String url )
    {
        return !StringUtils.isEmpty( url ) && (url.startsWith( "http://" ) || url.startsWith( "https://" ));
    }

    private Collection<? extends ValidationViolation> validateCollection( Object object, Property property )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        if ( !Collection.class.isInstance( object ) )
        {
            return validationViolations;
        }

        Collection<?> value = (Collection<?>) object;

        if ( value.size() < property.getMin() || value.size() > property.getMax() )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Invalid range for size ["
                + property.getMin() + ", " + property.getMax() + "], size is " + value.size(), value ) );
        }

        return validationViolations;
    }

    private Collection<? extends ValidationViolation> validateInteger( Object object, Property property )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        if ( !Integer.class.isInstance( object ) )
        {
            return validationViolations;
        }

        Integer value = (Integer) object;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Invalid range for value ["
                + property.getMin() + ", " + property.getMax() + "], value is " + value, value ) );
        }

        return validationViolations;
    }

    private Collection<? extends ValidationViolation> validateFloat( Object object, Property property )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        if ( !Float.class.isInstance( object ) )
        {
            return validationViolations;
        }

        Float value = (Float) object;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Invalid range for value ["
                + property.getMin() + ", " + property.getMax() + "], value is " + value, value ) );
        }

        return validationViolations;
    }

    private Collection<? extends ValidationViolation> validateDouble( Object object, Property property )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        if ( !Double.class.isInstance( object ) )
        {
            return validationViolations;
        }

        Double value = (Double) object;

        if ( !GenericValidator.isInRange( value, property.getMin(), property.getMax() ) )
        {
            validationViolations.add( new ValidationViolation( property.getName(), "Invalid range for value ["
                + property.getMin() + ", " + property.getMax() + "], value is " + value, value ) );
        }

        return validationViolations;
    }
}
