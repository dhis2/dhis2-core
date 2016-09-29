package org.hisp.dhis.schema;

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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.SessionFactory;
import org.hisp.dhis.schema.descriptors.*;
import org.hisp.dhis.security.oauth2.OAuth2Client;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.OrderComparator;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com> descriptors
 */
public class DefaultSchemaService
    implements SchemaService
{
    private static final ImmutableMap<Class<?>, String> BEAUTIFY_OVERRIDE = ImmutableMap.<Class<?>, String>builder()
        .put( OAuth2Client.class, "OAuth2 Client" )
        .build();

    private ImmutableList<SchemaDescriptor> descriptors = new ImmutableList.Builder<SchemaDescriptor>().
        add( new MetadataVersionSchemaDescriptor() ).
        add( new AttributeSchemaDescriptor() ).
        add( new CategoryComboSchemaDescriptor() ).
        add( new CategoryOptionComboSchemaDescriptor() ).
        add( new CategoryOptionGroupSchemaDescriptor() ).
        add( new CategoryOptionGroupSetSchemaDescriptor() ).
        add( new CategoryOptionSchemaDescriptor() ).
        add( new CategorySchemaDescriptor() ).
        add( new ChartSchemaDescriptor() ).
        add( new ColorSchemaDescriptor() ).
        add( new ColorSetSchemaDescriptor() ).
        add( new ConstantSchemaDescriptor() ).
        add( new DashboardItemSchemaDescriptor() ).
        add( new DashboardSchemaDescriptor() ).
        add( new DataApprovalLevelSchemaDescriptor() ).
        add( new DataApprovalWorkflowSchemaDescriptor() ).
        add( new DataElementGroupSchemaDescriptor() ).
        add( new DataElementGroupSetSchemaDescriptor() ).
        add( new DataElementOperandSchemaDescriptor() ).
        add( new DataElementSchemaDescriptor() ).
        add( new DataEntryFormSchemaDescriptor() ).
        add( new DataSetSchemaDescriptor() ).
        add( new DataSetElementSchemaDescriptor() ).
        add( new DocumentSchemaDescriptor() ).
        add( new EventChartSchemaDescriptor() ).
        add( new EventReportSchemaDescriptor() ).
        add( new FileResourceSchemaDescriptor() ).
        add( new IndicatorGroupSchemaDescriptor() ).
        add( new IndicatorGroupSetSchemaDescriptor() ).
        add( new IndicatorSchemaDescriptor() ).
        add( new IndicatorTypeSchemaDescriptor() ).
        add( new InterpretationCommentSchemaDescriptor() ).
        add( new InterpretationSchemaDescriptor() ).
        add( new LegendSchemaDescriptor() ).
        add( new LegendSetSchemaDescriptor() ).
        add( new ExternalMapLayerSchemaDescriptor() ).
        add( new MapSchemaDescriptor() ).
        add( new MapViewSchemaDescriptor() ).
        add( new MessageConversationSchemaDescriptor() ).
        add( new OAuth2ClientSchemaDescriptor() ).
        add( new OptionSchemaDescriptor() ).
        add( new OptionSetSchemaDescriptor() ).
        add( new OrganisationUnitGroupSchemaDescriptor() ).
        add( new OrganisationUnitGroupSetSchemaDescriptor() ).
        add( new OrganisationUnitLevelSchemaDescriptor() ).
        add( new OrganisationUnitSchemaDescriptor() ).
        add( new PredictorSchemaDescriptor() ).
        add( new ProgramIndicatorSchemaDescriptor() ).
        add( new ProgramRuleActionSchemaDescriptor() ).
        add( new ProgramRuleSchemaDescriptor() ).
        add( new ProgramRuleVariableSchemaDescriptor() ).
        add( new ProgramSchemaDescriptor() ).
        add( new ProgramDataElementSchemaDescriptor() ).
        add( new ProgramStageDataElementSchemaDescriptor() ).
        add( new ProgramStageSchemaDescriptor() ).
        add( new ProgramStageSectionSchemaDescriptor() ).
        add( new ProgramTrackedEntityAttributeSchemaDescriptor() ).
        add( new ProgramNotificationTemplateSchemaDescriptor() ).
        add( new ProgramValidationSchemaDescriptor() ).
        add( new RelationshipTypeSchemaDescriptor() ).
        add( new ReportSchemaDescriptor() ).
        add( new ReportTableSchemaDescriptor() ).
        add( new SectionSchemaDescriptor() ).
        add( new SqlViewSchemaDescriptor() ).
        add( new TrackedEntityAttributeGroupSchemaDescriptor() ).
        add( new TrackedEntityAttributeSchemaDescriptor() ).
        add( new TrackedEntityInstanceSchemaDescriptor() ).
        add( new TrackedEntitySchemaDescriptor() ).
        add( new TrackedEntityDataElementDimensionSchemaDescriptor() ).
        add( new TrackedEntityProgramIndicatorDimensionSchemaDescriptor() ).
        add( new UserCredentialsSchemaDescriptor() ).
        add( new UserGroupSchemaDescriptor() ).
        add( new UserRoleSchemaDescriptor() ).
        add( new UserSchemaDescriptor() ).
        add( new ValidationCriteriaSchemaDescriptor() ).
        add( new ValidationRuleGroupSchemaDescriptor() ).
        add( new ValidationRuleSchemaDescriptor() ).
        add( new PushAnalysisSchemaDescriptor() ).
        add( new ProgramIndicatorGroupSchemaDescriptor() ).
        add( new ExternalFileResourceSchemaDescriptor() ).
        add( new OptionGroupSchemaDescriptor() ).
        add( new OptionGroupSetSchemaDescriptor()).
        build();

    private Map<Class<?>, Schema> classSchemaMap = new HashMap<>();

    private Map<String, Schema> singularSchemaMap = new HashMap<>();

    private Map<String, Schema> pluralSchemaMap = new HashMap<>();

    private Map<Class<?>, Schema> dynamicClassSchemaMap = new HashMap<>();

    @Autowired
    private PropertyIntrospectorService propertyIntrospectorService;

    @Autowired
    private SessionFactory sessionFactory;

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        for ( SchemaDescriptor descriptor : descriptors )
        {
            Schema schema = descriptor.getSchema();

            if ( sessionFactory.getClassMetadata( schema.getKlass() ) != null )
            {
                schema.setPersisted( true );
            }

            schema.setDisplayName( beautify( schema ) );

            if ( schema.getProperties().isEmpty() )
            {
                schema.setPropertyMap( Maps.newHashMap( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );
            }

            classSchemaMap.put( schema.getKlass(), schema );
            singularSchemaMap.put( schema.getSingular(), schema );
            pluralSchemaMap.put( schema.getPlural(), schema );

            updateSelf( schema );
        }
    }

    @Override
    public Schema getSchema( Class<?> klass )
    {
        if ( klass == null )
        {
            return null;
        }

        klass = ReflectionUtils.getRealClass( klass );

        if ( classSchemaMap.containsKey( klass ) )
        {
            return classSchemaMap.get( klass );
        }

        if ( dynamicClassSchemaMap.containsKey( klass ) )
        {
            return dynamicClassSchemaMap.get( klass );
        }

        return null;
    }

    @Override
    public Schema getDynamicSchema( Class<?> klass )
    {
        if ( klass == null )
        {
            return null;
        }

        Schema schema = getSchema( klass );

        if ( schema != null )
        {
            return schema;
        }

        klass = propertyIntrospectorService.getConcreteClass( ReflectionUtils.getRealClass( klass ) );

        String name = getName( klass );

        schema = new Schema( klass, name, name + "s" );
        schema.setDisplayName( beautify( schema ) );
        schema.setPropertyMap( new HashMap<>( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );
        schema.setMetadata( false );

        updateSelf( schema );

        dynamicClassSchemaMap.put( klass, schema );

        return schema;
    }

    private String getName( Class<?> klass )
    {
        if ( AnnotationUtils.isAnnotationPresent( klass, JacksonXmlRootElement.class ) )
        {
            JacksonXmlRootElement rootElement = AnnotationUtils.getAnnotation( klass, JacksonXmlRootElement.class );

            if ( !StringUtils.isEmpty( rootElement.localName() ) )
            {
                return rootElement.localName();
            }
        }

        return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, klass.getSimpleName() );
    }

    @Override
    public Schema getSchemaBySingularName( String name )
    {
        return singularSchemaMap.get( name );
    }

    @Override
    public Schema getSchemaByPluralName( String name )
    {
        return pluralSchemaMap.get( name );
    }

    @Override
    public List<Schema> getSchemas()
    {
        return Lists.newArrayList( classSchemaMap.values() );
    }

    @Override
    public List<Schema> getSortedSchemas()
    {
        List<Schema> schemas = Lists.newArrayList( classSchemaMap.values() );
        Collections.sort( schemas, OrderComparator.INSTANCE );

        return schemas;
    }

    @Override
    public List<Schema> getMetadataSchemas()
    {
        List<Schema> schemas = getSchemas();

        Iterator<Schema> iterator = schemas.iterator();

        while ( iterator.hasNext() )
        {
            Schema schema = iterator.next();

            if ( !schema.isMetadata() )
            {
                iterator.remove();
            }
        }

        Collections.sort( schemas, OrderComparator.INSTANCE );

        return schemas;
    }

    private void updateSelf( Schema schema )
    {
        if ( schema.haveProperty( "__self__" ) )
        {
            Property property = schema.getProperty( "__self__" );
            schema.setName( property.getName() );
            schema.setCollectionName( schema.getPlural() );
            schema.setNamespace( property.getNamespace() );

            schema.getPropertyMap().remove( "__self__" );
        }
    }

    private String beautify( Schema schema )
    {
        if ( BEAUTIFY_OVERRIDE.containsKey( schema.getKlass() ) )
        {
            return BEAUTIFY_OVERRIDE.get( schema.getKlass() );
        }

        String[] camelCaseWords = org.apache.commons.lang3.StringUtils.capitalize( schema.getPlural() ).split( "(?=[A-Z])" );
        return org.apache.commons.lang3.StringUtils.join( camelCaseWords, " " ).trim();
    }
}
