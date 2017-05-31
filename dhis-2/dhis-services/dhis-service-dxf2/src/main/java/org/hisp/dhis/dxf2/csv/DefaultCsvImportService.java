package org.hisp.dhis.dxf2.csv;

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

import com.csvreader.CsvReader;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationRule;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.system.util.DateUtils.getMediumDate;

/**
 * TODO Unit testing
 *
 * @author Lars Helge Overland
 */
public class DefaultCsvImportService
    implements CsvImportService
{
    @Autowired
    private DataElementCategoryService categoryService;

    // -------------------------------------------------------------------------
    // CsvImportService implementation
    // -------------------------------------------------------------------------

    @Override
    public Metadata fromCsv( InputStream input, Class<? extends IdentifiableObject> clazz )
        throws IOException
    {
        CsvReader reader = new CsvReader( input, Charset.forName( "UTF-8" ) );
        reader.readRecord(); // Ignore first row

        Metadata metadata = new Metadata();

        if ( DataElement.class.equals( clazz ) )
        {
            metadata.setDataElements( dataElementsFromCsv( reader ) );
        }
        else if ( DataElementGroup.class.equals( clazz ) )
        {
            metadata.setDataElementGroups( dataElementGroupsFromCsv( reader ) );
        }
        else if ( DataElementCategoryOption.class.equals( clazz ) )
        {
            metadata.setCategoryOptions( categoryOptionsFromCsv( reader ) );
        }
        else if ( CategoryOptionGroup.class.equals( clazz ) )
        {
            metadata.setCategoryOptionGroups( categoryOptionGroupsFromCsv( reader ) );
        }
        else if ( OrganisationUnit.class.equals( clazz ) )
        {
            metadata.setOrganisationUnits( organisationUnitsFromCsv( reader ) );
        }
        else if ( OrganisationUnitGroup.class.equals( clazz ) )
        {
            metadata.setOrganisationUnitGroups( organisationUnitGroupsFromCsv( reader ) );
        }
        else if ( ValidationRule.class.equals( clazz ) )
        {
            metadata.setValidationRules( validationRulesFromCsv( reader ) );
        }
        else if ( OptionSet.class.equals( clazz ) )
        {
            setOptionSetsFromCsv( reader, metadata );
        }

        return metadata;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<DataElementCategoryOption> categoryOptionsFromCsv( CsvReader reader )
        throws IOException
    {
        List<DataElementCategoryOption> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                DataElementCategoryOption object = new DataElementCategoryOption();
                setIdentifiableObject( object, values );
                object.setShortName( getSafe( values, 3, object.getName(), 50 ) );
                list.add( object );
            }
        }

        return list;
    }

    private List<CategoryOptionGroup> categoryOptionGroupsFromCsv( CsvReader reader )
        throws IOException
    {
        List<CategoryOptionGroup> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                CategoryOptionGroup object = new CategoryOptionGroup();
                setIdentifiableObject( object, values );
                object.setShortName( getSafe( values, 3, object.getName(), 50 ) );
                list.add( object );
            }
        }

        return list;
    }

    private List<DataElement> dataElementsFromCsv( CsvReader reader )
        throws IOException
    {
        DataElementCategoryCombo categoryCombo = categoryService.getDefaultDataElementCategoryCombo();

        List<DataElement> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                DataElement object = new DataElement();
                setIdentifiableObject( object, values );
                object.setShortName( getSafe( values, 3, object.getName(), 50 ) );
                object.setDescription( getSafe( values, 4, null, null ) );
                object.setFormName( getSafe( values, 5, null, 230 ) );

                String domainType = getSafe( values, 6, DataElementDomain.AGGREGATE.getValue(), 16 );
                object.setDomainType( DataElementDomain.fromValue( domainType ) );
                object.setValueType( ValueType.valueOf( getSafe( values, 7, ValueType.INTEGER.toString(), 50 ) ) );

                object.setAggregationType( AggregationType.valueOf( getSafe( values, 8, AggregationType.SUM.toString(), 50 ) ) );
                String categoryComboUid = getSafe( values, 9, null, 11 );
                object.setUrl( getSafe( values, 10, null, 255 ) );
                object.setZeroIsSignificant( Boolean.valueOf( getSafe( values, 11, "false", null ) ) );
                String optionSetUid = getSafe( values, 12, null, 11 );
                String commentOptionSetUid = getSafe( values, 13, null, 11 );
                object.setAutoFields();

                DataElementCategoryCombo cc = new DataElementCategoryCombo();
                cc.setUid( categoryComboUid );
                cc.setAutoFields();

                if ( categoryComboUid == null )
                {
                    cc.setUid( categoryCombo.getUid() );
                }

                object.setDataElementCategoryCombo( cc );

                if ( optionSetUid != null )
                {
                    OptionSet optionSet = new OptionSet();
                    optionSet.setUid( optionSetUid );
                    optionSet.setAutoFields();
                    object.setOptionSet( optionSet );
                }

                if ( commentOptionSetUid != null )
                {
                    OptionSet optionSet = new OptionSet();
                    optionSet.setUid( commentOptionSetUid );
                    optionSet.setAutoFields();
                    object.setCommentOptionSet( optionSet );
                }

                list.add( object );
            }
        }

        return list;
    }

    private List<DataElementGroup> dataElementGroupsFromCsv( CsvReader reader )
        throws IOException
    {
        List<DataElementGroup> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                DataElementGroup object = new DataElementGroup();
                setIdentifiableObject( object, values );
                object.setShortName( getSafe( values, 3, object.getName(), 50 ) );
                object.setAutoFields();
                list.add( object );
            }
        }

        return list;
    }

    private List<ValidationRule> validationRulesFromCsv( CsvReader reader )
        throws IOException
    {
        List<ValidationRule> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                Expression leftSide = new Expression();
                Expression rightSide = new Expression();

                ValidationRule object = new ValidationRule();
                setIdentifiableObject( object, values );
                object.setDescription( getSafe( values, 3, null, 255 ) );
                object.setInstruction( getSafe( values, 4, null, 255 ) );
                object.setImportance( Importance.valueOf( getSafe( values, 5, Importance.MEDIUM.toString(), 255 ) ) );
                // Left here so nobody wonders what field 6 is for
                // object.setRuleType( RuleType.valueOf( getSafe( values, 6, RuleType.VALIDATION.toString(), 255 ) ) );
                object.setOperator( Operator.safeValueOf( getSafe( values, 7, Operator.equal_to.toString(), 255 ) ) );
                object.setPeriodType( PeriodType.getByNameIgnoreCase( getSafe( values, 8, MonthlyPeriodType.NAME, 255 ) ) );

                leftSide.setExpression( getSafe( values, 9, null, 255 ) );
                leftSide.setDescription( getSafe( values, 10, null, 255 ) );
                leftSide.setMissingValueStrategy( MissingValueStrategy.safeValueOf( getSafe( values, 11, MissingValueStrategy.NEVER_SKIP.toString(), 50 ) ) );

                rightSide.setExpression( getSafe( values, 12, null, 255 ) );
                rightSide.setDescription( getSafe( values, 13, null, 255 ) );
                rightSide.setMissingValueStrategy( MissingValueStrategy.safeValueOf( getSafe( values, 14, MissingValueStrategy.NEVER_SKIP.toString(), 50 ) ) );

                object.setLeftSide( leftSide );
                object.setRightSide( rightSide );
                object.setAutoFields();

                list.add( object );
            }
        }

        return list;
    }

    private List<OrganisationUnit> organisationUnitsFromCsv( CsvReader reader )
        throws IOException
    {
        List<OrganisationUnit> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                OrganisationUnit object = new OrganisationUnit();
                setIdentifiableObject( object, values );
                String parentUid = getSafe( values, 3, null, 230 ); // Could be UID, code, name
                object.setShortName( getSafe( values, 4, object.getName(), 50 ) );
                object.setDescription( getSafe( values, 5, null, null ) );
                object.setOpeningDate( getMediumDate( getSafe( values, 6, "1970-01-01", null ) ) );
                object.setClosedDate( getMediumDate( getSafe( values, 7, null, null ) ) );
                object.setComment( getSafe( values, 8, null, null ) );
                object.setFeatureType( FeatureType.valueOf( getSafe( values, 9, "NONE", 50 ) ) );
                object.setCoordinates( getSafe( values, 10, null, null ) );
                object.setUrl( getSafe( values, 11, null, 255 ) );
                object.setContactPerson( getSafe( values, 12, null, 255 ) );
                object.setAddress( getSafe( values, 13, null, 255 ) );
                object.setEmail( getSafe( values, 14, null, 150 ) );
                object.setPhoneNumber( getSafe( values, 15, null, 150 ) );
                object.setAutoFields();

                if ( parentUid != null )
                {
                    OrganisationUnit parent = new OrganisationUnit();
                    parent.setUid( parentUid );
                    object.setParent( parent );
                }

                list.add( object );
            }
        }

        return list;
    }

    private List<OrganisationUnitGroup> organisationUnitGroupsFromCsv( CsvReader reader )
        throws IOException
    {
        List<OrganisationUnitGroup> list = new ArrayList<>();

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                OrganisationUnitGroup object = new OrganisationUnitGroup();
                setIdentifiableObject( object, values );
                object.setAutoFields();
                object.setShortName( getSafe( values, 3, object.getName(), 50 ) );
                list.add( object );
            }
        }

        return list;
    }

    /**
     * Option set format:
     * <p>
     * <ul>
     * <li>option set name</li>
     * <li>option set uid</li>
     * <li>option set code</li>
     * <li>option name</li>
     * <li>option uid</li>
     * <li>option code</li>
     * </ul>
     */
    private void setOptionSetsFromCsv( CsvReader reader, Metadata metadata )
        throws IOException
    {
        ListMap<String, Option> nameOptionMap = new ListMap<>();
        Map<String, OptionSet> nameOptionSetMap = new HashMap<>();

        // Read option sets and options and put in maps

        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();

            if ( values != null && values.length > 0 )
            {
                OptionSet optionSet = new OptionSet();
                setIdentifiableObject( optionSet, values );
                optionSet.setAutoFields();
                optionSet.setValueType( ValueType.TEXT );

                Option option = new Option();
                option.setName( getSafe( values, 3, null, 230 ) );
                option.setUid( getSafe( values, 4, CodeGenerator.generateUid(), 11 ) );
                option.setCode( getSafe( values, 5, null, 50 ) );
                option.setAutoFields();

                if ( optionSet.getName() == null || option.getCode() == null )
                {
                    continue;
                }

                nameOptionSetMap.put( optionSet.getName(), optionSet );

                nameOptionMap.putValue( optionSet.getName(), option );

                metadata.getOptions().add( option );
            }
        }

        // Read option sets from map and set in meta data

        for ( String optionSetName : nameOptionSetMap.keySet() )
        {
            OptionSet optionSet = nameOptionSetMap.get( optionSetName );

            List<Option> options = new ArrayList<>( nameOptionMap.get( optionSetName ) );

            optionSet.setOptions( options );

            metadata.getOptionSets().add( optionSet );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Sets the name, uid and code properties on the given object.
     *
     * @param object the object to set identifiable properties.
     * @param values the array of property values.
     */
    private static void setIdentifiableObject( BaseIdentifiableObject object, String[] values )
    {
        object.setName( getSafe( values, 0, null, 230 ) );
        object.setUid( getSafe( values, 1, CodeGenerator.generateUid(), 11 ) );
        object.setCode( getSafe( values, 2, null, 50 ) );
    }

    /**
     * Returns a string from the given array avoiding exceptions.
     *
     * @param values       the string array.
     * @param index        the array index of the string to get, zero-based.
     * @param defaultValue the default value in case index is out of bounds.
     * @param maxChars     the max number of characters to return for the string.
     */
    private static String getSafe( String[] values, int index, String defaultValue, Integer maxChars )
    {
        String string = null;

        if ( values == null || index < 0 || index >= values.length )
        {
            string = defaultValue;
        }
        else
        {
            string = values[index];
        }

        string = StringUtils.defaultIfBlank( string, defaultValue );

        if ( string != null )
        {
            return maxChars != null ? StringUtils.substring( string, 0, maxChars ) : string;
        }

        return null;
    }
}
