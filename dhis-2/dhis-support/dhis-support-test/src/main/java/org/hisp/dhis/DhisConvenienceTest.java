package org.hisp.dhis;

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

import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeGroup;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.validation.RuleType;
import org.hisp.dhis.validation.ValidationCriteria;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.joda.time.DateTime;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public abstract class DhisConvenienceTest
{
    protected static final Log log = LogFactory.getLog( DhisConvenienceTest.class );

    protected static final String BASE_UID = "abcdefghij";

    protected static final String BASE_IN_UID = "inabcdefgh";

    protected static final String BASE_DE_UID = "deabcdefgh";

    protected static final String BASE_DS_UID = "dsabcdefgh";

    protected static final String BASE_OU_UID = "ouabcdefgh";

    protected static final String BASE_USER_UID = "userabcdef";

    protected static final String BASE_USER_GROUP_UID = "ugabcdefgh";

    private static final String EXT_TEST_DIR = System.getProperty( "user.home" ) + File.separator + "dhis2_test_dir";

    private static Date date;

    protected static final double DELTA = 0.01;

    // -------------------------------------------------------------------------
    // Service references
    // -------------------------------------------------------------------------

    protected UserService userService;

    protected RenderService renderService;

    @Autowired( required = false )
    protected DataElementCategoryService _categoryService;

    protected static DataElementCategoryService categoryService;

    @PostConstruct
    protected void initStaticServices()
    {
        categoryService = _categoryService;
    }

    static
    {
        DateTime dateTime = new DateTime( 1970, 1, 1, 0, 0 );
        date = dateTime.toDate();
    }

    // -------------------------------------------------------------------------
    // Convenience methods
    // -------------------------------------------------------------------------

    /**
     * Creates a date.
     *
     * @param year  the year.
     * @param month the month.
     * @param day   the day of month.
     * @return a date.
     */
    public static Date getDate( int year, int month, int day )
    {
        DateTime dateTime = new DateTime( year, month, day, 0, 0 );
        return dateTime.toDate();
    }

    /**
     * Creates a date.
     *
     * @param s a string representation of a date
     * @return a date.
     */
    public static Date getDate( String s )
    {
        DateTime dateTime = new DateTime( s );
        return dateTime.toDate();
    }

    /**
     * Creates a date.
     *
     * @param day the day of the year.
     * @return a date.
     */
    public Date getDay( int day )
    {
        DateTime dataTime = DateTime.now();
        dataTime = dataTime.withTimeAtStartOfDay();
        dataTime = dataTime.withDayOfYear( day );

        return dataTime.toDate();
    }

    /**
     * Compares two collections for equality. This method does not check for the
     * implementation type of the collection in contrast to the native equals
     * method. This is useful for black-box testing where one will not know the
     * implementation type of the returned collection for a method.
     *
     * @param actual    the actual collection to check.
     * @param reference the reference objects to check against.
     * @return true if the collections are equal, false otherwise.
     */
    public static boolean equals( Collection<?> actual, Object... reference )
    {
        final Collection<Object> collection = new HashSet<>();

        Collections.addAll( collection, reference );

        if ( actual == collection )
        {
            return true;
        }

        if ( actual == null )
        {
            return false;
        }

        if ( actual.size() != collection.size() )
        {
            log.warn( "Actual collection has different size compared to reference collection: " + actual.size() + " / "
                + collection.size() );
            return false;
        }

        for ( Object object : actual )
        {
            if ( !collection.contains( object ) )
            {
                log.warn( "Object in actual collection not part of reference collection: " + object );
                return false;
            }
        }

        for ( Object object : collection )
        {
            if ( !actual.contains( object ) )
            {
                log.warn( "Object in reference collection not part of actual collection: " + object );
                return false;
            }
        }

        return true;
    }

    public static String message( Object expected )
    {
        return "Expected was: " + ((expected != null) ? "[" + expected.toString() + "]" : "[null]");
    }

    public static String message( Object expected, Object actual )
    {
        return message( expected ) + " Actual was: " + ((actual != null) ? "[" + actual.toString() + "]" : "[null]");
    }

    // -------------------------------------------------------------------------
    // Dependency injection methods
    // -------------------------------------------------------------------------

    /**
     * Sets a dependency on the target service. This method can be used to set
     * mock implementations of dependencies on services for testing purposes.
     * The advantage of using this method over setting the services directly is
     * that the test can still be executed against the interface type of the
     * service; making the test unaware of the implementation and thus
     * re-usable. A weakness is that the field name of the dependency must be
     * assumed.
     *
     * @param targetService the target service.
     * @param fieldName     the name of the dependency field in the target service.
     * @param dependency    the dependency.
     */
    protected void setDependency( Object targetService, String fieldName, Object dependency )
    {
        Class<?> clazz = dependency.getClass().getInterfaces()[0];

        setDependency( targetService, fieldName, dependency, clazz );
    }

    /**
     * Sets a dependency on the target service. This method can be used to set
     * mock implementations of dependencies on services for testing purposes.
     * The advantage of using this method over setting the services directly is
     * that the test can still be executed against the interface type of the
     * service; making the test unaware of the implementation and thus
     * re-usable. A weakness is that the field name of the dependency must be
     * assumed.
     *
     * @param targetService the target service.
     * @param fieldName     the name of the dependency field in the target service.
     * @param dependency    the dependency.
     * @param clazz         the interface type of the dependency.
     */
    protected void setDependency( Object targetService, String fieldName, Object dependency, Class<?> clazz )
    {
        try
        {
            targetService = getRealObject( targetService );

            String setMethodName = "set" + fieldName.substring( 0, 1 ).toUpperCase()
                + fieldName.substring( 1, fieldName.length() );

            Class<?>[] argumentClass = new Class<?>[]{ clazz };

            Method method = targetService.getClass().getMethod( setMethodName, argumentClass );

            method.invoke( targetService, dependency );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to set dependency '" + fieldName + "' on service: " + getStackTrace( ex ), ex );
        }
    }

    /**
     * If the given class is advised by Spring AOP it will return the target
     * class, i.e. the advised class. If not the given class is returned
     * unchanged.
     *
     * @param object the object.
     */
    @SuppressWarnings( "unchecked" )
    private <T> T getRealObject( T object )
        throws Exception
    {
        if ( AopUtils.isAopProxy( object ) )
        {
            return (T) ((Advised) object).getTargetSource().getTarget();
        }

        return object;
    }

    // -------------------------------------------------------------------------
    // Create object methods
    // -------------------------------------------------------------------------

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static DataElement createDataElement( char uniqueCharacter )
    {
        return createDataElement( uniqueCharacter, null );
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param categoryCombo   The category combo.
     */
    public static DataElement createDataElement( char uniqueCharacter, DataElementCategoryCombo categoryCombo )
    {
        DataElement dataElement = new DataElement();
        dataElement.setAutoFields();

        dataElement.setUid( BASE_DE_UID + uniqueCharacter );
        dataElement.setName( "DataElement" + uniqueCharacter );
        dataElement.setShortName( "DataElementShort" + uniqueCharacter );
        dataElement.setCode( "DataElementCode" + uniqueCharacter );
        dataElement.setDescription( "DataElementDescription" + uniqueCharacter );
        dataElement.setValueType( ValueType.INTEGER );
        dataElement.setDomainType( DataElementDomain.AGGREGATE );
        dataElement.setAggregationType( AggregationType.SUM );

        if ( categoryCombo != null )
        {
            dataElement.setCategoryCombo( categoryCombo );
        }
        else if ( categoryService != null )
        {
            dataElement.setCategoryCombo( categoryService.getDefaultDataElementCategoryCombo() );
        }

        return dataElement;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param valueType       The value type.
     * @param aggregationType The aggregation type.
     */
    public static DataElement createDataElement( char uniqueCharacter, ValueType valueType, AggregationType aggregationType )
    {
        DataElement dataElement = createDataElement( uniqueCharacter );
        dataElement.setValueType( valueType );
        dataElement.setAggregationType( aggregationType );

        return dataElement;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param valueType       The value type.
     * @param aggregationType The aggregation type.
     * @param domainType      The domain type.
     */
    public static DataElement createDataElement( char uniqueCharacter, ValueType valueType, AggregationType aggregationType, DataElementDomain domainType )
    {
        DataElement dataElement = createDataElement( uniqueCharacter );
        dataElement.setValueType( valueType );
        dataElement.setAggregationType( aggregationType );
        dataElement.setDomainType( domainType );

        return dataElement;
    }

    /**
     * @param categoryComboUniqueIdentifier A unique character to identify the
     *                                      category option combo.
     * @param categories                    the categories
     *                                      category options.
     * @return DataElementCategoryOptionCombo
     */
    public static DataElementCategoryCombo createCategoryCombo( char categoryComboUniqueIdentifier, DataElementCategory... categories )
    {
        DataElementCategoryCombo categoryCombo = new DataElementCategoryCombo( "CategoryCombo" + categoryComboUniqueIdentifier, DataDimensionType.DISAGGREGATION );
        categoryCombo.setAutoFields();

        for ( DataElementCategory category : categories )
        {
            categoryCombo.getCategories().add( category );
        }

        return categoryCombo;
    }

    /**
     * @param categoryComboUniqueIdentifier   A unique character to identify the
     *                                        category combo.
     * @param categoryOptionUniqueIdentifiers Unique characters to identify the
     *                                        category options.
     * @return DataElementCategoryOptionCombo
     */
    public static DataElementCategoryOptionCombo createCategoryOptionCombo( char categoryComboUniqueIdentifier,
        char... categoryOptionUniqueIdentifiers )
    {
        DataElementCategoryOptionCombo categoryOptionCombo = new DataElementCategoryOptionCombo();
        categoryOptionCombo.setAutoFields();

        categoryOptionCombo.setCategoryCombo( new DataElementCategoryCombo( "CategoryCombo"
            + categoryComboUniqueIdentifier, DataDimensionType.DISAGGREGATION ) );

        for ( char identifier : categoryOptionUniqueIdentifiers )
        {
            categoryOptionCombo.getCategoryOptions()
                .add( new DataElementCategoryOption( "CategoryOption" + identifier ) );
        }

        return categoryOptionCombo;
    }

    /**
     * @param categoryComboUniqueIdentifier A unique character to identify the
     *                                      category option combo.
     * @param dataElementCategoryCombo      The associated category combination.
     * @param categoryOptions               the category options.
     * @return DataElementCategoryOptionCombo
     */
    public static DataElementCategoryOptionCombo createCategoryOptionCombo( char categoryComboUniqueIdentifier,
        DataElementCategoryCombo dataElementCategoryCombo,
        DataElementCategoryOption... categoryOptions )
    {
        DataElementCategoryOptionCombo categoryOptionCombo = new DataElementCategoryOptionCombo();
        categoryOptionCombo.setAutoFields();

        categoryOptionCombo.setCategoryCombo( dataElementCategoryCombo );

        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            categoryOptionCombo.getCategoryOptions().add( categoryOption );

            categoryOption.getCategoryOptionCombos().add( categoryOptionCombo );
        }

        return categoryOptionCombo;
    }

    /**
     * @param categoryCombo   the category combo.
     * @param categoryOptions the category options.
     * @return DataElementCategoryOptionCombo
     */
    public static DataElementCategoryOptionCombo createCategoryOptionCombo( DataElementCategoryCombo categoryCombo,
        DataElementCategoryOption... categoryOptions )
    {
        DataElementCategoryOptionCombo categoryOptionCombo = new DataElementCategoryOptionCombo();
        categoryOptionCombo.setAutoFields();

        categoryOptionCombo.setCategoryCombo( categoryCombo );

        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            categoryOptionCombo.getCategoryOptions().add( categoryOption );
            categoryOption.getCategoryOptionCombos().add( categoryOptionCombo );
        }

        return categoryOptionCombo;
    }

    /**
     * @param categoryUniqueIdentifier A unique character to identify the
     *                                 category.
     * @param categoryOptions          the category options.
     * @return DataElementCategory
     */
    public static DataElementCategory createDataElementCategory( char categoryUniqueIdentifier,
        DataElementCategoryOption... categoryOptions )
    {
        DataElementCategory dataElementCategory = new DataElementCategory( "DataElementCategory" + categoryUniqueIdentifier, DataDimensionType.DISAGGREGATION );
        dataElementCategory.setAutoFields();

        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            dataElementCategory.addCategoryOption( categoryOption );
        }

        return dataElementCategory;
    }

    public static DataElementCategoryOption createCategoryOption( char uniqueIdentifier )
    {
        DataElementCategoryOption categoryOption = new DataElementCategoryOption( "CategoryOption" + uniqueIdentifier );
        categoryOption.setAutoFields();

        return categoryOption;
    }

    /**
     * @param uniqueIdentifier A unique character to identify the
     *                         category option group.
     * @param categoryOptions  the category options.
     * @return CategoryOptionGroup
     */
    public static CategoryOptionGroup createCategoryOptionGroup( char uniqueIdentifier,
        DataElementCategoryOption... categoryOptions )
    {
        CategoryOptionGroup categoryOptionGroup = new CategoryOptionGroup( "CategoryOptionGroup" + uniqueIdentifier );
        categoryOptionGroup.setShortName( "ShortName" + uniqueIdentifier );
        categoryOptionGroup.setAutoFields();

        categoryOptionGroup.setMembers( new HashSet<>() );

        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            categoryOptionGroup.addCategoryOption( categoryOption );
        }

        return categoryOptionGroup;
    }

    /**
     * @param categoryGroupSetUniqueIdentifier A unique character to identify the
     *                                         category option group set.
     * @param categoryOptionGroups             the category option groups.
     * @return CategoryOptionGroupSet
     */
    public static CategoryOptionGroupSet createCategoryOptionGroupSet( char categoryGroupSetUniqueIdentifier,
        CategoryOptionGroup... categoryOptionGroups )
    {
        CategoryOptionGroupSet categoryOptionGroupSet = new CategoryOptionGroupSet( "CategoryOptionGroupSet" + categoryGroupSetUniqueIdentifier );
        categoryOptionGroupSet.setAutoFields();

        // categoryOptionGroupSet.setMembers( new ArrayList<CategoryOptionGroup>() );

        for ( CategoryOptionGroup categoryOptionGroup : categoryOptionGroups )
        {
            categoryOptionGroupSet.addCategoryOptionGroup( categoryOptionGroup );

            categoryOptionGroup.setGroupSet( categoryOptionGroupSet );
        }

        return categoryOptionGroupSet;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static DataElementGroup createDataElementGroup( char uniqueCharacter )
    {
        DataElementGroup group = new DataElementGroup();
        group.setAutoFields();

        group.setUid( BASE_UID + uniqueCharacter );
        group.setName( "DataElementGroup" + uniqueCharacter );
        group.setShortName( "DataElementGroup" + uniqueCharacter );
        group.setCode( "DataElementCode" + uniqueCharacter );

        return group;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static DataElementGroupSet createDataElementGroupSet( char uniqueCharacter )
    {
        DataElementGroupSet groupSet = new DataElementGroupSet();
        groupSet.setAutoFields();

        groupSet.setUid( BASE_UID + uniqueCharacter );
        groupSet.setName( "DataElementGroupSet" + uniqueCharacter );

        return groupSet;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static IndicatorType createIndicatorType( char uniqueCharacter )
    {
        IndicatorType type = new IndicatorType();
        type.setAutoFields();

        type.setName( "IndicatorType" + uniqueCharacter );
        type.setFactor( 100 );

        return type;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param type            The type.
     */
    public static Indicator createIndicator( char uniqueCharacter, IndicatorType type )
    {
        Indicator indicator = new Indicator();
        indicator.setAutoFields();

        indicator.setUid( BASE_IN_UID + uniqueCharacter );
        indicator.setName( "Indicator" + uniqueCharacter );
        indicator.setShortName( "IndicatorShort" + uniqueCharacter );
        indicator.setCode( "IndicatorCode" + uniqueCharacter );
        indicator.setDescription( "IndicatorDescription" + uniqueCharacter );
        indicator.setAnnualized( false );
        indicator.setIndicatorType( type );
        indicator.setNumerator( "Numerator" );
        indicator.setNumeratorDescription( "NumeratorDescription" );
        indicator.setDenominator( "Denominator" );
        indicator.setDenominatorDescription( "DenominatorDescription" );

        return indicator;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static IndicatorGroup createIndicatorGroup( char uniqueCharacter )
    {
        IndicatorGroup group = new IndicatorGroup();
        group.setAutoFields();

        group.setUid( BASE_UID + uniqueCharacter );
        group.setName( "IndicatorGroup" + uniqueCharacter );

        return group;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static IndicatorGroupSet createIndicatorGroupSet( char uniqueCharacter )
    {
        IndicatorGroupSet groupSet = new IndicatorGroupSet();
        groupSet.setAutoFields();

        groupSet.setUid( BASE_UID + uniqueCharacter );
        groupSet.setName( "IndicatorGroupSet" + uniqueCharacter );

        return groupSet;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static DataSet createDataSet( char uniqueCharacter )
    {
        DataSet dataSet = createDataSet( uniqueCharacter, null );
        dataSet.setPeriodType( new MonthlyPeriodType() );
        
        return dataSet;
    }
    
    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param periodType      The period type.
     */
    public static DataSet createDataSet( char uniqueCharacter, PeriodType periodType )
    {
        return createDataSet( uniqueCharacter, periodType, null );
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param periodType      The period type.
     * @param categoryCombo   The category combo.
     */
    public static DataSet createDataSet( char uniqueCharacter, PeriodType periodType, DataElementCategoryCombo categoryCombo )
    {
        DataSet dataSet = new DataSet();
        dataSet.setAutoFields();

        dataSet.setUid( BASE_DS_UID + uniqueCharacter );
        dataSet.setName( "DataSet" + uniqueCharacter );
        dataSet.setShortName( "DataSetShort" + uniqueCharacter );
        dataSet.setCode( "DataSetCode" + uniqueCharacter );
        dataSet.setPeriodType( periodType );

        if ( categoryCombo != null )
        {
            dataSet.setCategoryCombo( categoryCombo );
        }
        else if ( categoryService != null )
        {
            dataSet.setCategoryCombo( categoryService.getDefaultDataElementCategoryCombo() );
        }

        return dataSet;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static DataEntryForm createDataEntryForm( char uniqueCharacter )
    {
        return new DataEntryForm( "DataEntryForm" + uniqueCharacter, "<p></p>" );
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param html the form HTML content.
     */
    public static DataEntryForm createDataEntryForm( char uniqueCharacter, String html )
    {
        return new DataEntryForm( "DataEntryForm" + uniqueCharacter, html );
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static OrganisationUnit createOrganisationUnit( char uniqueCharacter )
    {
        OrganisationUnit unit = new OrganisationUnit();
        unit.setAutoFields();

        unit.setUid( BASE_OU_UID + uniqueCharacter );
        unit.setName( "OrganisationUnit" + uniqueCharacter );
        unit.setShortName( "OrganisationUnitShort" + uniqueCharacter );
        unit.setCode( "OrganisationUnitCode" + uniqueCharacter );
        unit.setOpeningDate( date );
        unit.setClosedDate( date );
        unit.setComment( "Comment" + uniqueCharacter );

        return unit;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param parent          The parent.
     */
    public static OrganisationUnit createOrganisationUnit( char uniqueCharacter, OrganisationUnit parent )
    {
        OrganisationUnit unit = createOrganisationUnit( uniqueCharacter );

        unit.setParent( parent );
        parent.getChildren().add( unit );

        return unit;
    }

    /**
     * @param name The name, short name and code of the organisation unit.
     */
    public static OrganisationUnit createOrganisationUnit( String name )
    {
        OrganisationUnit unit = new OrganisationUnit();
        unit.setAutoFields();

        unit.setUid( CodeGenerator.generateCode() );
        unit.setName( name );
        unit.setShortName( name );
        unit.setCode( name );
        unit.setOpeningDate( date );
        unit.setClosedDate( date );
        unit.setComment( "Comment " + name );

        return unit;
    }

    /**
     * @param name   The name, short name and code of the organisation unit.
     * @param parent The parent.
     */
    public static OrganisationUnit createOrganisationUnit( String name, OrganisationUnit parent )
    {
        OrganisationUnit unit = createOrganisationUnit( name );

        unit.setParent( parent );
        parent.getChildren().add( unit );

        return unit;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static OrganisationUnitGroup createOrganisationUnitGroup( char uniqueCharacter )
    {
        OrganisationUnitGroup group = new OrganisationUnitGroup();
        group.setAutoFields();

        group.setUid( BASE_UID + uniqueCharacter );
        group.setName( "OrganisationUnitGroup" + uniqueCharacter );
        group.setShortName( "OrganisationUnitGroupShort" + uniqueCharacter );
        group.setCode( "OrganisationUnitGroupCode" + uniqueCharacter );

        return group;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     */
    public static OrganisationUnitGroupSet createOrganisationUnitGroupSet( char uniqueCharacter )
    {
        OrganisationUnitGroupSet groupSet = new OrganisationUnitGroupSet();
        groupSet.setAutoFields();

        groupSet.setName( "OrganisationUnitGroupSet" + uniqueCharacter );
        groupSet.setDescription( "Description" + uniqueCharacter );
        groupSet.setCompulsory( true );

        return groupSet;
    }

    /**
     * @param type      The PeriodType.
     * @param startDate The start date.
     * @param endDate   The end date.
     */
    public static Period createPeriod( PeriodType type, Date startDate )
    {
        Period period = new Period();
        period.setAutoFields();

        period.setPeriodType( type );
        period.setStartDate( startDate );

        return period;
    }

    /**
     * @param type      The PeriodType.
     * @param startDate The start date.
     * @param endDate   The end date.
     */
    public static Period createPeriod( PeriodType type, Date startDate, Date endDate )
    {
        Period period = new Period();
        period.setAutoFields();

        period.setPeriodType( type );
        period.setStartDate( startDate );
        period.setEndDate( endDate );

        return period;
    }

    /**
     * @param isoPeriod the ISO period string.
     */
    public static Period createPeriod( String isoPeriod )
    {
        return PeriodType.getPeriodFromIsoString( isoPeriod );
    }

    /**
     * @param startDate The start date.
     * @param endDate   The end date.
     */
    public static Period createPeriod( Date startDate, Date endDate )
    {
        Period period = new Period();
        period.setAutoFields();

        period.setPeriodType( new MonthlyPeriodType() );
        period.setStartDate( startDate );
        period.setEndDate( endDate );

        return period;
    }

    /**
     * Uses the given category option combo also as attribute option combo.
     *
     * @param dataElement         The data element.
     * @param period              The period.
     * @param source              The source.
     * @param value               The value.
     * @param categoryOptionCombo The category (and attribute) option combo.
     */
    public static DataValue createDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        String value, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        DataValue dataValue = new DataValue();

        dataValue.setDataElement( dataElement );
        dataValue.setPeriod( period );
        dataValue.setSource( source );
        dataValue.setCategoryOptionCombo( categoryOptionCombo );
        dataValue.setAttributeOptionCombo( categoryOptionCombo );
        dataValue.setValue( value );
        dataValue.setComment( "Comment" );
        dataValue.setStoredBy( "StoredBy" );

        return dataValue;
    }

    /**
     * @param dataElement          The data element.
     * @param period               The period.
     * @param source               The source.
     * @param value                The value.
     * @param categoryOptionCombo  The category option combo.
     * @param attributeOptionCombo The attribute option combo.
     */
    public static DataValue createDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        String value, DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        DataValue dataValue = new DataValue();

        dataValue.setDataElement( dataElement );
        dataValue.setPeriod( period );
        dataValue.setSource( source );
        dataValue.setCategoryOptionCombo( categoryOptionCombo );
        dataValue.setAttributeOptionCombo( attributeOptionCombo );
        dataValue.setValue( value );
        dataValue.setComment( "Comment" );
        dataValue.setStoredBy( "StoredBy" );

        return dataValue;
    }

    /**
     * @param dataElement          The data element.
     * @param period               The period.
     * @param source               The source.
     * @param value                The value.
     * @param lastupdated          The date.value.
     * @param categoryOptionCombo  The category option combo.
     * @param attributeOptionCombo The attribute option combo.
     */
    public static DataValue createDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        String value, Date lastupdated, DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        DataValue dataValue = new DataValue();

        dataValue.setDataElement( dataElement );
        dataValue.setPeriod( period );
        dataValue.setSource( source );
        dataValue.setCategoryOptionCombo( categoryOptionCombo );
        dataValue.setAttributeOptionCombo( attributeOptionCombo );
        dataValue.setValue( value );
        dataValue.setComment( "Comment" );
        dataValue.setStoredBy( "StoredBy" );

        return dataValue;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param operator        The operator.
     * @param leftSide        The left side expression.
     * @param rightSide       The right side expression.
     * @param periodType      The period-type.
     */
    public static ValidationRule createValidationRule( String uniqueCharacter, Operator operator, Expression leftSide,
        Expression rightSide, PeriodType periodType )
    {
        ValidationRule validationRule = new ValidationRule();
        validationRule.setAutoFields();

        validationRule.setName( "ValidationRule" + uniqueCharacter );
        validationRule.setDescription( "Description" + uniqueCharacter );
        validationRule.setOperator( operator );
        validationRule.setLeftSide( leftSide );
        validationRule.setRightSide( rightSide );
        validationRule.setPeriodType( periodType );

        return validationRule;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param operator        The operator.
     * @param leftSide        The left side expression.
     * @param rightSide       The right side expression.
     * @param periodType      The period-type.
     */
    public static ValidationRule createValidationRule( char uniqueCharacter, Operator operator, Expression leftSide,
        Expression rightSide, PeriodType periodType )
    {
        return createValidationRule( Character.toString( uniqueCharacter ), operator, leftSide, rightSide, periodType );
    }

    /**
     * Creates a ValidationRule of RULE_TYPE_MONITORING
     *
     * @param uniqueCharacter       A unique character to identify the object.
     * @param operator              The operator.
     * @param leftSide              The left side expression.
     * @param rightSide             The right side expression.
     * @param skipTest              The skiptest expression
     * @param periodType            The period-type.
     * @param organisationUnitLevel The unit level of organisations to be
     *                              evaluated by this rule.
     * @param sequentialSampleCount How many sequential past periods to sample.
     * @param annualSampleCount     How many years of past periods to sample.
     * @param sequentialSkipCount   How many periods in the current year to skip
     */
    public static ValidationRule createMonitoringRule( String uniqueCharacter, Operator operator,
        Expression leftSide, Expression rightSide, Expression skipTest,
        PeriodType periodType, int organisationUnitLevel, int sequentialSampleCount,
        int annualSampleCount, int sequentialSkipCount )
    {
        ValidationRule validationRule = new ValidationRule();
        validationRule.setAutoFields();

        validationRule.setName( "MonitoringRule" + uniqueCharacter );
        validationRule.setDescription( "Description" + uniqueCharacter );
        validationRule.setRuleType( RuleType.SURVEILLANCE );
        validationRule.setOperator( operator );
        validationRule.setLeftSide( leftSide );
        validationRule.setRightSide( rightSide );
        validationRule.setSampleSkipTest( skipTest );
        validationRule.setPeriodType( periodType );
        validationRule.setOrganisationUnitLevel( organisationUnitLevel );
        validationRule.setSequentialSampleCount( sequentialSampleCount );
        validationRule.setAnnualSampleCount( annualSampleCount );
        validationRule.setSequentialSkipCount( sequentialSkipCount );

        return validationRule;
    }

    /**
     * Creates a ValidationRule of RULE_TYPE_MONITORING
     *
     * @param uniqueCharacter       A unique character to identify the object.
     * @param operator              The operator.
     * @param leftSide              The left side expression.
     * @param rightSide             The right side expression.
     * @param periodType            The period-type.
     * @param organisationUnitLevel The unit level of organisations to be
     *                              evaluated by this rule.
     * @param sequentialSampleCount How many sequential past periods to sample.
     * @param annualSampleCount     How many years of past periods to sample.
     */
    public static ValidationRule createMonitoringRule( String uniqueCharacter,
        Operator operator, Expression leftSide, Expression rightSide,
        PeriodType periodType, int organisationUnitLevel,
        int sequentialSampleCount, int annualSampleCount )
    {
        return createMonitoringRule( uniqueCharacter, operator,
            leftSide, rightSide, null,
            periodType, organisationUnitLevel,
            sequentialSampleCount,
            annualSampleCount, 0 );
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @return ValidationRuleGroup
     */
    public static ValidationRuleGroup createValidationRuleGroup( char uniqueCharacter )
    {
        ValidationRuleGroup group = new ValidationRuleGroup();
        group.setAutoFields();

        group.setName( "ValidationRuleGroup" + uniqueCharacter );
        group.setDescription( "Description" + uniqueCharacter );

        return group;
    }

    /**
     * @param uniqueCharacter          A unique character to identify the object.
     * @param expressionString         The expression string.
     * @param dataElementsInExpression A collection of the data elements
     *                                 entering into the expression.
     */
    public static Expression createExpression( char uniqueCharacter, String expressionString,
        Set<DataElement> dataElementsInExpression, Set<DataElementCategoryOptionCombo> optionCombosInExpression )
    {
        Expression expression = new Expression();

        expression.setExpression( expressionString );
        expression.setDescription( "Description" + uniqueCharacter );
        expression.setDataElementsInExpression( dataElementsInExpression );

        return expression;
    }

    /**
     * Creates a Predictor
     *
     * @param uniqueCharacter A unique character to identify the object.
     * @param expr The right side expression.
     * @param skipTest The skiptest expression
     * @param periodType The period-type.
     * @param organisationUnitLevel The unit level of organisations to be
     *        evaluated by this rule.
     * @param sequentialSampleCount How many sequential past periods to sample.
     * @param annualSampleCount How many years of past periods to sample.
     * @param sequentialSkipCount How many periods in the current year to skip
     */
    public static Predictor createPredictor( DataElement writes, String uniqueCharacter, Expression expr,
        Expression skipTest, PeriodType periodType, OrganisationUnitLevel organisationUnitLevel, int sequentialSampleCount,
        int sequentialSkipCount, int annualSampleCount )
    {
        Predictor predictor = new Predictor();
        Set<OrganisationUnitLevel> orglevels = Sets.newHashSet( organisationUnitLevel );
        predictor.setAutoFields();

        predictor.setOutput( writes );
        predictor.setName( "Predictor" + uniqueCharacter );
        predictor.setDescription( "Description" + uniqueCharacter );
        predictor.setGenerator( expr );
        predictor.setSampleSkipTest( skipTest );
        predictor.setPeriodType( periodType );
        predictor.setOrganisationUnitLevels( orglevels );
        predictor.setSequentialSampleCount( sequentialSampleCount );
        predictor.setAnnualSampleCount( annualSampleCount );
        predictor.setSequentialSkipCount( sequentialSkipCount );

        return predictor;
    }

    public static Legend createLegend( char uniqueCharacter, Double startValue, Double endValue )
    {
        Legend legend = new Legend();
        legend.setAutoFields();

        legend.setName( "Legend" + uniqueCharacter );
        legend.setStartValue( startValue );
        legend.setEndValue( endValue );
        legend.setColor( "Color" + uniqueCharacter );

        return legend;
    }

    public static LegendSet createLegendSet( char uniqueCharacter )
    {
        LegendSet legendSet = new LegendSet();
        legendSet.setAutoFields();

        legendSet.setName( "LegendSet" + uniqueCharacter );

        return legendSet;
    }

    public static Chart createChart( char uniqueCharacter )
    {
        Chart chart = new Chart();
        chart.setAutoFields();
        chart.setName( "Chart" + uniqueCharacter );
        chart.setDescription( "Description" + uniqueCharacter );
        chart.setType( ChartType.COLUMN );

        return chart;
    }

    public static Chart createChart( char uniqueCharacter, List<Indicator> indicators, List<Period> periods,
        List<OrganisationUnit> units )
    {
        Chart chart = createChart( uniqueCharacter );

        chart.addAllDataDimensionItems( indicators );
        chart.setPeriods( periods );
        chart.setOrganisationUnits( units );
        chart.setDimensions( DimensionalObject.DATA_X_DIM_ID, DimensionalObject.PERIOD_DIM_ID,
            DimensionalObject.ORGUNIT_DIM_ID );

        return chart;
    }

    public static User createUser( char uniqueCharacter )
    {
        UserCredentials credentials = new UserCredentials();
        User user = new User();
        user.setUid( BASE_USER_UID + uniqueCharacter );

        credentials.setUserInfo( user );
        user.setUserCredentials( credentials );

        credentials.setUsername( "username" + uniqueCharacter );
        credentials.setPassword( "password" + uniqueCharacter );

        user.setFirstName( "FirstName" + uniqueCharacter );
        user.setSurname( "Surname" + uniqueCharacter );
        user.setEmail( "Email" + uniqueCharacter );
        user.setPhoneNumber( "PhoneNumber" + uniqueCharacter );
        user.setCode( "UserCode" + uniqueCharacter );
        user.setAutoFields();

        return user;
    }

    public static UserCredentials createUserCredentials( char uniqueCharacter, User user )
    {
        UserCredentials credentials = new UserCredentials();
        credentials.setName( "UserCredentials" + uniqueCharacter );
        credentials.setUsername( "Username" + uniqueCharacter );
        credentials.setPassword( "Password" + uniqueCharacter );
        credentials.setUserInfo( user );
        user.setUserCredentials( credentials );

        return credentials;
    }

    public static UserGroup createUserGroup( char uniqueCharacter, Set<User> users )
    {
        UserGroup userGroup = new UserGroup();
        userGroup.setAutoFields();

        userGroup.setUid( BASE_USER_GROUP_UID + uniqueCharacter );
        userGroup.setCode( "UserGroupCode" + uniqueCharacter );
        userGroup.setName( "UserGroup" + uniqueCharacter );
        userGroup.setMembers( users );

        return userGroup;
    }

    public static UserAuthorityGroup createUserAuthorityGroup( char uniqueCharacter )
    {
        UserAuthorityGroup role = new UserAuthorityGroup();
        role.setAutoFields();

        role.setUid( BASE_UID + uniqueCharacter );
        role.setName( "UserAuthorityGroup" + uniqueCharacter );

        return role;
    }

    public static Program createProgram( char uniqueCharacter )
    {
        return createProgram( uniqueCharacter, null, null );
    }

    public static Program createProgram( char uniqueCharacter, Set<ProgramStage> programStages,
        OrganisationUnit unit )
    {
        Set<OrganisationUnit> units = new HashSet<>();

        if ( unit != null )
        {
            units.add( unit );
        }

        return createProgram( uniqueCharacter, programStages, null, units, null );
    }

    public static Program createProgram( char uniqueCharacter, Set<ProgramStage> programStages,
        Set<TrackedEntityAttribute> attributes, Set<OrganisationUnit> organisationUnits, DataElementCategoryCombo categoryCombo )
    {
        Program program = new Program();
        program.setAutoFields();

        program.setName( "Program" + uniqueCharacter );
        program.setShortName( "ProgramShort" + uniqueCharacter  );
        program.setDescription( "Description" + uniqueCharacter );
        program.setEnrollmentDateLabel( "DateOfEnrollmentDescription" );
        program.setIncidentDateLabel( "DateOfIncidentDescription" );
        program.setProgramType( ProgramType.WITH_REGISTRATION );

        if ( programStages != null )
        {
            for ( ProgramStage programStage : programStages )
            {
                programStage.setProgram( program );
                program.getProgramStages().add( programStage );
            }
        }

        if ( attributes != null )
        {
            for ( TrackedEntityAttribute attribute : attributes )
            {
                ProgramTrackedEntityAttribute ptea = new ProgramTrackedEntityAttribute( program, attribute, false, false );
                ptea.setAutoFields();
                
                program.getProgramAttributes().add( ptea );
            }
        }

        if ( organisationUnits != null )
        {
            program.getOrganisationUnits().addAll( organisationUnits );
        }

        if ( categoryCombo != null )
        {
            program.setCategoryCombo( categoryCombo );
        }
        else if ( categoryService != null )
        {
            program.setCategoryCombo( categoryService.getDefaultDataElementCategoryCombo() );
        }
        
        return program;
    }

    public static ProgramRule createProgramRule( char uniqueCharacter, Program parentProgram )
    {
        ProgramRule programRule = new ProgramRule();
        programRule.setAutoFields();
        
        programRule.setName( "ProgramRule" + uniqueCharacter );
        programRule.setProgram( parentProgram );
        programRule.setCondition( "true" );

        return programRule;
    }

    public static ProgramRuleAction createProgramRuleAction( char uniqueCharacter )
    {
        ProgramRuleAction programRuleAction = new ProgramRuleAction();
        programRuleAction.setAutoFields();
        
        programRuleAction.setName( "ProgramRuleAction" + uniqueCharacter );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.HIDEFIELD );

        return programRuleAction;
    }

    public static ProgramRuleAction createProgramRuleAction( char uniqueCharacter, ProgramRule parentRule )
    {
        ProgramRuleAction programRuleAction = createProgramRuleAction( uniqueCharacter );
        programRuleAction.setProgramRule( parentRule );

        return programRuleAction;
    }

    public static ProgramRuleVariable createProgramRuleVariable( char uniqueCharacter, Program parentProgram )
    {
        ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
        programRuleVariable.setAutoFields();
        
        programRuleVariable.setName( "ProgramRuleVariable" + uniqueCharacter );
        programRuleVariable.setProgram( parentProgram );
        programRuleVariable.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );

        return programRuleVariable;
    }

    public static ProgramStage createProgramStage( char uniqueCharacter, Program program )
    {
        ProgramStage stage = createProgramStage( uniqueCharacter, 0, false );
        stage.setProgram( program );
        
        return stage;
    }

    public static ProgramStage createProgramStage( char uniqueCharacter, int minDays )
    {
        return createProgramStage( uniqueCharacter, minDays, false );
    }

    public static ProgramStage createProgramStage( char uniqueCharacter, int minDays, boolean repeatable )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setAutoFields();

        programStage.setName( "ProgramStage" + uniqueCharacter );
        programStage.setDescription( "description" + uniqueCharacter );
        programStage.setMinDaysFromStart( minDays );
        programStage.setRepeatable( repeatable );

        return programStage;
    }

    public static ProgramStage createProgramStage( char uniqueCharacter, Set<DataElement> dataElements )
    {
        ProgramStage programStage = createProgramStage( uniqueCharacter, 0 );

        if ( dataElements != null )
        {
            int sortOrder = 1;
            
            for ( DataElement dataElement : dataElements )
            {
                ProgramStageDataElement psd = createProgramStageDataElement( programStage, dataElement, false, sortOrder );
                psd.setAutoFields();
                
                programStage.getProgramStageDataElements().add( psd );
            }
        }

        return programStage;
    }
    
    public static ProgramStageDataElement createProgramStageDataElement( ProgramStage programStage, DataElement dataElement, 
        boolean compulsory, Integer sortOrder )
    {
        ProgramStageDataElement psde = new ProgramStageDataElement( programStage, dataElement, compulsory, sortOrder );
        psde.setAutoFields();
        
        return psde;
    }
    
    public static ProgramMessage createProgramMessage( String text, String subject, 
        ProgramMessageRecipients recipients, ProgramMessageStatus status, Set<DeliveryChannel> channels )
    {
        ProgramMessage message = new ProgramMessage();
        message.setText( text );
        message.setSubject( subject );
        message.setRecipients( recipients );
        message.setMessageStatus( status );
        message.setDeliveryChannels( channels );
        
        return message;
    }

    public static ProgramIndicator createProgramIndicator( char uniqueCharacter, Program program, String expression, String filter )
    {
        ProgramIndicator indicator = new ProgramIndicator();
        indicator.setAutoFields();
        indicator.setName( "Indicator" + uniqueCharacter );
        indicator.setShortName( "IndicatorShort" + uniqueCharacter );
        indicator.setCode( "IndicatorCode" + uniqueCharacter );
        indicator.setDescription( "IndicatorDescription" + uniqueCharacter );
        indicator.setProgram( program );
        indicator.setExpression( expression );
        indicator.setFilter( filter );

        return indicator;
    }
    
    public static ProgramStageSection createProgramStageSection( char uniqueCharacter, Integer sortOrder )
    {
        ProgramStageSection section = new ProgramStageSection();
        section.setAutoFields();        
        section.setName( "ProgramStageSection" + uniqueCharacter );
        section.setSortOrder( sortOrder );
        
        return section;
    }

    public static TrackedEntity createTrackedEntity( char uniqueChar )
    {
        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setAutoFields();
        trackedEntity.setName( "TrackedEntity" + uniqueChar );
        trackedEntity.setDescription( "TrackedEntity" + uniqueChar + " description" );

        return trackedEntity;
    }

    public static TrackedEntityInstance createTrackedEntityInstance( char uniqueChar, OrganisationUnit organisationUnit )
    {
        TrackedEntityInstance entityInstance = new TrackedEntityInstance();
        entityInstance.setAutoFields();
        entityInstance.setOrganisationUnit( organisationUnit );

        return entityInstance;
    }

    public static ProgramStageInstance createProgramStageInstance()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        return programStageInstance;
    }

    public static TrackedEntityInstance createTrackedEntityInstance( char uniqueChar, OrganisationUnit organisationUnit,
        TrackedEntityAttribute attribute )
    {
        TrackedEntityInstance entityInstance = new TrackedEntityInstance();
        entityInstance.setAutoFields();
        entityInstance.setOrganisationUnit( organisationUnit );

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
        attributeValue.setAttribute( attribute );
        attributeValue.setEntityInstance( entityInstance );
        attributeValue.setValue( "Attribute" + uniqueChar );
        entityInstance.getTrackedEntityAttributeValues().add( attributeValue );

        return entityInstance;
    }

    public static TrackedEntityAttributeValue createTrackedEntityAttributeValue( char uniqueChar, TrackedEntityInstance entityInstance,
        TrackedEntityAttribute attribute )
    {
        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
        attributeValue.setEntityInstance( entityInstance );
        attributeValue.setAttribute( attribute );
        attributeValue.setValue( "Attribute" + uniqueChar );

        return attributeValue;
    }

    /**
     * @param uniqueChar A unique character to identify the object.
     * @return TrackedEntityAttribute
     */
    public static TrackedEntityAttribute createTrackedEntityAttribute( char uniqueChar )
    {
        TrackedEntityAttribute attribute = new TrackedEntityAttribute();
        attribute.setAutoFields();

        attribute.setName( "Attribute" + uniqueChar );
        attribute.setDescription( "Attribute" + uniqueChar );
        attribute.setValueType( ValueType.TEXT );

        return attribute;
    }

    /**
     * @param uniqueChar A unique character to identify the object.
     * @return TrackedEntityAttribute
     */
    public static TrackedEntityAttribute createTrackedEntityAttribute( char uniqueChar, ValueType valueType )
    {
        TrackedEntityAttribute attribute = new TrackedEntityAttribute();
        attribute.setAutoFields();

        attribute.setName( "Attribute" + uniqueChar );
        attribute.setDescription( "Attribute" + uniqueChar );
        attribute.setValueType( valueType );

        return attribute;
    }

    /**
     * @param uniqueChar A unique character to identify the object.
     * @return TrackedEntityAttributeGroup
     */
    public static TrackedEntityAttributeGroup createTrackedEntityAttributeGroup( char uniqueChar, List<TrackedEntityAttribute> attributes )
    {
        TrackedEntityAttributeGroup attributeGroup = new TrackedEntityAttributeGroup();
        attributeGroup.setAutoFields();

        attributeGroup.setName( "TrackedEntityAttributeGroup" + uniqueChar );
        attributeGroup.setDescription( "TrackedEntityAttributeGroup" + uniqueChar );
        attributeGroup.setAttributes( attributes );

        return attributeGroup;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @return ValidationCriteria
     */
    public static ValidationCriteria createValidationCriteria( char uniqueCharacter, String property, int operator,
        String value )
    {
        ValidationCriteria validationCriteria = new ValidationCriteria();
        validationCriteria.setAutoFields();

        validationCriteria.setName( "ValidationCriteria" + uniqueCharacter );
        validationCriteria.setDescription( "Description" + uniqueCharacter );
        validationCriteria.setProperty( property );
        validationCriteria.setOperator( operator );
        validationCriteria.setValue( value );

        return validationCriteria;
    }

    /**
     * @param uniqueChar A unique character to identify the object.
     * @return RelationshipType
     */
    public static RelationshipType createRelationshipType( char uniqueChar )
    {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setAutoFields();

        relationshipType.setaIsToB( "aIsToB" );
        relationshipType.setbIsToA( "bIsToA" );
        relationshipType.setName( "RelationshipType" + uniqueChar );

        return relationshipType;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @return TrackedEntityInstanceReminder
     */
    public static TrackedEntityInstanceReminder createTrackedEntityInstanceReminder( char uniqueCharacter, 
        Integer daysAllowedSendMessage, String templateMessage, String dateToCompare,
        Integer sendTo, Integer whenToSend, Integer messageType )
    {
        TrackedEntityInstanceReminder reminder = new TrackedEntityInstanceReminder( "Reminder" + uniqueCharacter, daysAllowedSendMessage,
            templateMessage, dateToCompare, sendTo, whenToSend, messageType );
        reminder.setAutoFields();
        
        return reminder;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param sql             A query statement to retreive record/data from database.
     * @return a sqlView instance
     */
    protected static SqlView createSqlView( char uniqueCharacter, String sql )
    {
        SqlView sqlView = new SqlView();
        sqlView.setAutoFields();

        sqlView.setName( "SqlView" + uniqueCharacter );
        sqlView.setDescription( "Description" + uniqueCharacter );
        sqlView.setSqlQuery( sql );
        sqlView.setType( SqlViewType.VIEW );
        sqlView.setCacheStrategy( CacheStrategy.RESPECT_SYSTEM_SETTING );

        return sqlView;
    }

    /**
     * @param uniqueCharacter A unique character to identify the object.
     * @param value           The value for constant
     * @return a constant instance
     */
    protected static Constant createConstant( char uniqueCharacter, double value )
    {
        Constant constant = new Constant();
        constant.setAutoFields();

        constant.setName( "Constant" + uniqueCharacter );
        constant.setValue( value );

        return constant;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    protected <T extends IdentifiableObject> T fromJson( String path, Class<T> klass )
    {
        Assert.notNull( renderService, "RenderService must be injected in test" );

        try
        {
            return renderService.fromJson( new ClassPathResource( path ).getInputStream(), klass );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Injects the externalDir property of LocationManager to
     * user.home/dhis2_test_dir. LocationManager dependency must be retrieved
     * from the context up front.
     *
     * @param locationManager The LocationManager to be injected with the
     *                        external directory.
     */
    public void setExternalTestDir( LocationManager locationManager )
    {
        setDependency( locationManager, "externalDir", EXT_TEST_DIR, String.class );
    }

    /**
     * Attempts to remove the external test directory.
     */
    public void removeExternalTestDir()
    {
        deleteDir( new File( EXT_TEST_DIR ) );
    }

    private boolean deleteDir( File dir )
    {
        if ( dir.isDirectory() )
        {
            String[] children = dir.list();

            if ( children != null )
            {
                for ( String aChildren : children )
                {
                    boolean success = deleteDir( new File( dir, aChildren ) );
    
                    if ( !success )
                    {
                        return false;
                    }
                }
            }
        }

        return dir.delete();
    }

    // -------------------------------------------------------------------------
    // Allow xpath testing of DXF2
    // -------------------------------------------------------------------------

    protected String xpathTest( String xpathString, String xml )
        throws XPathExpressionException
    {
        InputSource source = new InputSource( new StringReader( xml ) );
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext( new Dxf2NamespaceResolver() );

        return xpath.evaluate( xpathString, source );
    }

    protected class Dxf2NamespaceResolver
        implements NamespaceContext
    {
        @Override
        public String getNamespaceURI( String prefix )
        {
            if ( prefix == null )
            {
                throw new IllegalArgumentException( "No prefix provided!" );
            }
            else
            {
                if ( prefix.equals( "d" ) )
                {
                    return "http://dhis2.org/schema/dxf/2.0";
                }
                else
                {
                    return XMLConstants.NULL_NS_URI;
                }
            }
        }

        @Override
        public String getPrefix( String namespaceURI )
        {
            return null;
        }

        @Override
        public Iterator<?> getPrefixes( String namespaceURI )
        {
            return null;
        }
    }

    /**
     * Creates a user and injects into the security context with username
     * "username". Requires <code>identifiableObjectManager</code> and
     * <code>userService</code> to be injected into the test.
     *
     * @param allAuth whether to grant ALL authority to user.
     * @param auths   authorities to grant to user.
     * @return the user.
     */
    protected User createUserAndInjectSecurityContext( boolean allAuth, String... auths )
    {
        return createUserAndInjectSecurityContext( null, allAuth, auths );
    }

    /**
     * Creates a user and injects into the security context with username
     * "username". Requires <code>identifiableObjectManager</code> and
     * <code>userService</code> to be injected into the test.
     *
     * @param organisationUnits the organisation units of the user.
     * @param allAuth           whether to grant the ALL authority to user.
     * @param auths             authorities to grant to user.
     * @return the user.
     */
    protected User createUserAndInjectSecurityContext( Set<OrganisationUnit> organisationUnits, boolean allAuth, String... auths )
    {
        return createUserAndInjectSecurityContext( organisationUnits, null, allAuth, auths );
    }

    /**
     * Creates a user and injects into the security context with username
     * "username". Requires <code>identifiableObjectManager</code> and
     * <code>userService</code> to be injected into the test.
     *
     * @param organisationUnits         the organisation units of the user.
     * @param dataViewOrganisationUnits user's data view organisation units.
     * @param allAuth                   whether to grant the ALL authority.
     * @param auths                     authorities to grant to user.
     * @return the user.
     */
    protected User createUserAndInjectSecurityContext( Set<OrganisationUnit> organisationUnits,
        Set<OrganisationUnit> dataViewOrganisationUnits, boolean allAuth, String... auths )
    {
        Assert.notNull( userService, "UserService must be injected in test" );

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setName( "Superuser" );

        if ( allAuth )
        {
            userAuthorityGroup.getAuthorities().add( "ALL" );
        }

        if ( auths != null )
        {
            for ( String auth : auths )
            {
                userAuthorityGroup.getAuthorities().add( auth );
            }
        }

        userService.addUserAuthorityGroup( userAuthorityGroup );

        User user = createUser( 'A' );

        if ( organisationUnits != null )
        {
            user.setOrganisationUnits( organisationUnits );
        }

        if ( dataViewOrganisationUnits != null )
        {
            user.setDataViewOrganisationUnits( dataViewOrganisationUnits );
        }

        user.getUserCredentials().getUserAuthorityGroups().add( userAuthorityGroup );
        userService.addUser( user );
        user.getUserCredentials().setUserInfo( user );
        userService.addUserCredentials( user.getUserCredentials() );

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add( new SimpleGrantedAuthority( "ALL" ) );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUserCredentials().getUsername(), user.getUserCredentials().getPassword(), authorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", authorities );
        SecurityContextHolder.getContext().setAuthentication( authentication );

        return user;
    }
    
    protected void saveAndInjectUserSecurityContext( User user )
    {
        userService.addUser( user );
        userService.addUserCredentials( user.getUserCredentials() );
        
        List<GrantedAuthority> grantedAuthorities = user.getUserCredentials().getAllAuthorities()
            .stream().map( SimpleGrantedAuthority::new ).collect( Collectors.toList() );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUserCredentials().getUsername(), user.getUserCredentials().getPassword(), grantedAuthorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", grantedAuthorities );
        SecurityContextHolder.getContext().setAuthentication( authentication );
    }

    protected User createAdminUser( String... authorities )
    {
        Assert.notNull( userService, "UserService must be injected in test" );

        String username = "admin";
        String password = "district";

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setUid( "yrB6vc5Ip3r" );
        userAuthorityGroup.setCode( "Superuser" );
        userAuthorityGroup.setName( "Superuser" );
        userAuthorityGroup.setDescription( "Superuser" );
        userAuthorityGroup.setAuthorities( Sets.newHashSet( authorities ) );

        userService.addUserAuthorityGroup( userAuthorityGroup );

        User user = new User();
        user.setUid( "M5zQapPyTZI" );
        user.setCode( "admin" );
        user.setFirstName( username );
        user.setSurname( username );

        userService.addUser( user );

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUid( "KvMx6c1eoYo" );
        userCredentials.setCode( username );
        userCredentials.setUser( user );
        userCredentials.setUserInfo( user );
        userCredentials.setUsername( username );
        userCredentials.getUserAuthorityGroups().add( userAuthorityGroup );

        userService.encodeAndSetPassword( userCredentials, password );
        userService.addUserCredentials( userCredentials );

        user.setUserCredentials( userCredentials );
        userService.updateUser( user );

        return user;
    }

    protected User createAndInjectAdminUser()
    {
        return createAndInjectAdminUser( "ALL" );
    }

    protected User createAndInjectAdminUser( String... authorities )
    {
        User user = createAdminUser( authorities );

        List<GrantedAuthority> grantedAuthorities = user.getUserCredentials().getAllAuthorities()
            .stream().map( SimpleGrantedAuthority::new ).collect( Collectors.toList() );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUserCredentials().getUsername(), user.getUserCredentials().getPassword(), grantedAuthorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", grantedAuthorities );
        SecurityContextHolder.getContext().setAuthentication( authentication );

        return user;
    }

    protected static String getStackTrace( Throwable t )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw, true );
        t.printStackTrace( pw );
        pw.flush();
        sw.flush();

        return sw.toString();
    }

}
