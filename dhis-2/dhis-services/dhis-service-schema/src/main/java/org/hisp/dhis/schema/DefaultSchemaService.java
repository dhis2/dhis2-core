/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.schema;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.schema.descriptors.*;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com> descriptors
 */
@Slf4j
@Service( "org.hisp.dhis.schema.SchemaService" )
public class DefaultSchemaService
    implements SchemaService
{
    // Simple alias map for our concrete implementations of the core interfaces
    private static final ImmutableMap<Class<?>, Class<?>> BASE_ALIAS_MAP = ImmutableMap.<Class<?>, Class<?>> builder()
        .put( IdentifiableObject.class, BaseIdentifiableObject.class )
        .put( NameableObject.class, BaseNameableObject.class )
        .put( DimensionalObject.class, BaseDimensionalObject.class )
        .put( DimensionalItemObject.class, BaseDimensionalItemObject.class )
        .put( AnalyticalObject.class, BaseAnalyticalObject.class )
        .build();

    private final Map<Class<?>, SchemaDescriptor> descriptors = new ConcurrentHashMap<>();

    private void init()
    {
        register( new AggregateDataExchangeSchemaDescriptor() );
        register( new AnalyticsTableHookSchemaDescriptor() );
        register( new AttributeSchemaDescriptor() );
        register( new AttributeValueSchemaDescriptor() );
        register( new CategoryComboSchemaDescriptor() );
        register( new CategoryOptionComboSchemaDescriptor() );
        register( new CategoryOptionGroupSchemaDescriptor() );
        register( new CategoryOptionGroupSetSchemaDescriptor() );
        register( new CategoryOptionSchemaDescriptor() );
        register( new CategorySchemaDescriptor() );
        register( new ConstantSchemaDescriptor() );
        register( new DashboardItemSchemaDescriptor() );
        register( new DashboardSchemaDescriptor() );
        register( new DataApprovalLevelSchemaDescriptor() );
        register( new DataApprovalWorkflowSchemaDescriptor() );
        register( new DataElementGroupSchemaDescriptor() );
        register( new DataElementGroupSetSchemaDescriptor() );
        register( new DataElementOperandSchemaDescriptor() );
        register( new DataElementSchemaDescriptor() );
        register( new DataEntryFormSchemaDescriptor() );
        register( new DataSetSchemaDescriptor() );
        register( new DataSetElementSchemaDescriptor() );
        register( new DataSetNotificationTemplateSchemaDescriptor() );
        register( new DocumentSchemaDescriptor() );
        register( new EventChartSchemaDescriptor() );
        register( new EventReportSchemaDescriptor() );
        register( new EventVisualizationSchemaDescriptor() );
        register( new ExpressionSchemaDescriptor() );
        register( new FileResourceSchemaDescriptor() );
        register( new IconSchemaDescriptor() );
        register( new IndicatorGroupSchemaDescriptor() );
        register( new IndicatorGroupSetSchemaDescriptor() );
        register( new IndicatorSchemaDescriptor() );
        register( new IndicatorTypeSchemaDescriptor() );
        register( new InterpretationCommentSchemaDescriptor() );
        register( new InterpretationSchemaDescriptor() );
        register( new LegendSchemaDescriptor() );
        register( new LegendSetSchemaDescriptor() );
        register( new ExternalMapLayerSchemaDescriptor() );
        register( new MapSchemaDescriptor() );
        register( new MapViewSchemaDescriptor() );
        register( new MessageConversationSchemaDescriptor() );
        register( new MetadataVersionSchemaDescriptor() );
        register( new OAuth2ClientSchemaDescriptor() );
        register( new OptionSchemaDescriptor() );
        register( new OptionSetSchemaDescriptor() );
        register( new OrganisationUnitGroupSchemaDescriptor() );
        register( new OrganisationUnitGroupSetSchemaDescriptor() );
        register( new OrganisationUnitLevelSchemaDescriptor() );
        register( new OrganisationUnitSchemaDescriptor() );
        register( new PredictorSchemaDescriptor() );
        register( new PredictorGroupSchemaDescriptor() );
        register( new ProgramDataElementDimensionItemSchemaDescriptor() );
        register( new ProgramIndicatorSchemaDescriptor() );
        register( new AnalyticsPeriodBoundarySchemaDescriptor() );
        register( new ProgramRuleActionSchemaDescriptor() );
        register( new ProgramRuleSchemaDescriptor() );
        register( new ProgramRuleVariableSchemaDescriptor() );
        register( new ProgramSchemaDescriptor() );
        register( new ProgramStageDataElementSchemaDescriptor() );
        register( new ProgramStageSchemaDescriptor() );
        register( new ProgramStageSectionSchemaDescriptor() );
        register( new ProgramSectionSchemaDescriptor() );
        register( new ProgramTrackedEntityAttributeSchemaDescriptor() );
        register( new ProgramTrackedEntityAttributeDimensionItemSchemaDescriptor() );
        register( new ProgramNotificationTemplateSchemaDescriptor() );
        register( new RelationshipTypeSchemaDescriptor() );
        register( new ReportSchemaDescriptor() );
        register( new SectionSchemaDescriptor() );
        register( new SqlViewSchemaDescriptor() );
        register( new TrackedEntityAttributeSchemaDescriptor() );
        register( new TrackedEntityAttributeValueSchemaDescriptor() );
        register( new TrackedEntityInstanceSchemaDescriptor() );
        register( new TrackedEntityInstanceFilterSchemaDescriptor() );
        register( new TrackedEntityTypeSchemaDescriptor() );
        register( new TrackedEntityTypeAttributeSchemaDescriptor() );
        register( new TrackedEntityDataElementDimensionSchemaDescriptor() );
        register( new TrackedEntityProgramIndicatorDimensionSchemaDescriptor() );
        register( new UserCredentialsSchemaDescriptor() );
        register( new UserGroupSchemaDescriptor() );
        register( new UserRoleSchemaDescriptor() );
        register( new UserSchemaDescriptor() );
        register( new ValidationRuleGroupSchemaDescriptor() );
        register( new ValidationRuleSchemaDescriptor() );
        register( new ValidationNotificationTemplateSchemaDescriptor() );
        register( new PushAnalysisSchemaDescriptor() );
        register( new ProgramIndicatorGroupSchemaDescriptor() );
        register( new ExternalFileResourceSchemaDescriptor() );
        register( new OptionGroupSchemaDescriptor() );
        register( new OptionGroupSetSchemaDescriptor() );
        register( new ProgramTrackedEntityAttributeGroupSchemaDescriptor() );
        register( new DataInputPeriodSchemaDescriptor() );
        register( new ReportingRateSchemaDescriptor() );
        register( new UserAccessSchemaDescriptor() );
        register( new UserGroupAccessSchemaDescriptor() );
        register( new MinMaxDataElementSchemaDescriptor() );
        register( new ValidationResultSchemaDescriptor() );
        register( new JobConfigurationSchemaDescriptor() );
        register( new SmsCommandSchemaDescriptor() );
        register( new CategoryDimensionSchemaDescriptor() );
        register( new CategoryOptionGroupSetDimensionSchemaDescriptor() );
        register( new DataElementGroupSetDimensionSchemaDescriptor() );
        register( new OrganisationUnitGroupSetDimensionSchemaDescriptor() );
        register( new RelationshipSchemaDescriptor() );
        register( new KeyJsonValueSchemaDescriptor() );
        register( new ProgramStageInstanceSchemaDescriptor() );
        register( new ProgramInstanceSchemaDescriptor() );
        register( new ProgramStageInstanceFilterSchemaDescriptor() );
        register( new VisualizationSchemaDescriptor() );
        register( new ApiTokenSchemaDescriptor() );
        register( new AccessSchemaDescriptor() );
        register( new ObjectStyleSchemaDescriptor() );
        register( new RelationshipConstraintSchemaDescriptor() );
        register( new RelationshipItemSchemaDescriptor() );
        register( new SharingSchemaDescriptor() );
        register( new AxisSchemaDescriptor() );
        register( new EventRepetitionSchemaDescriptor() );
        register( new LegendDefinitionsSchemaDescriptor() );
        register( new SeriesKeySchemaDescriptor() );
        register( new OutlierAnalysisSchemaDescriptor() );
        register( new ItemConfigSchemaDescriptor() );
        register( new LayoutSchemaDescriptor() );
    }

    private final Map<Class<?>, Schema> classSchemaMap = new HashMap<>();

    private final Map<String, Schema> singularSchemaMap = new HashMap<>();

    private final Map<String, Schema> pluralSchemaMap = new HashMap<>();

    private final Map<Class<?>, Schema> dynamicClassSchemaMap = new HashMap<>();

    private final PropertyIntrospectorService propertyIntrospectorService;

    private final SessionFactory sessionFactory;

    @Autowired
    public DefaultSchemaService( PropertyIntrospectorService propertyIntrospectorService,
        SessionFactory sessionFactory )
    {
        checkNotNull( propertyIntrospectorService );
        checkNotNull( sessionFactory );

        this.propertyIntrospectorService = propertyIntrospectorService;
        this.sessionFactory = sessionFactory;
        init();
    }

    @Override
    public void register( SchemaDescriptor descriptor )
    {
        descriptors.putIfAbsent( descriptor.getSchema().getKlass(), descriptor );
    }

    @Override
    public Class<?> getConcreteClass( Class<?> klass )
    {
        if ( BASE_ALIAS_MAP.containsKey( klass ) )
        {
            return BASE_ALIAS_MAP.get( klass );
        }

        return klass;
    }

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        for ( SchemaDescriptor descriptor : descriptors.values() )
        {
            Schema schema = descriptor.getSchema();

            MetamodelImplementor metamodelImplementor = (MetamodelImplementor) sessionFactory.getMetamodel();

            try
            {
                EntityPersister entityPersister = metamodelImplementor.entityPersister( schema.getKlass() );

                if ( entityPersister instanceof SingleTableEntityPersister )
                {
                    schema.setTableName( ((SingleTableEntityPersister) entityPersister).getTableName() );
                }

                schema.setPersisted( true );
            }
            catch ( MappingException e )
            {
                // Class is not persisted with Hibernate
                schema.setPersisted( false );
            }

            schema.setDisplayName( TextUtils.getPrettyClassName( schema.getKlass() ) );

            if ( schema.getProperties().isEmpty() )
            {
                schema.setPropertyMap(
                    Maps.newHashMap( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );
            }

            classSchemaMap.put( schema.getKlass(), schema );
            singularSchemaMap.put( schema.getSingular(), schema );
            pluralSchemaMap.put( schema.getPlural(), schema );

            updateSelf( schema );

            schema.getPersistedProperties();
            schema.getNonPersistedProperties();
            schema.getReadableProperties();
            schema.getEmbeddedObjectProperties();
        }
    }

    @Override
    public Schema getSchema( Class<?> klass )
    {
        if ( klass == null )
        {
            log.error( "getSchema() Error, input class should not be null!" );
            return null;
        }

        if ( klass.getName().contains( "Proxy" ) )
        {
            log.error( "Error, can't use Hibernate proxy class names!!!" );
            throw new IllegalStateException( "Input class must not be Hibernate proxy class!!!" );
        }

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
            log.error( "getDynamicSchema() Error, input class should not be null!" );
            return null;
        }

        if ( klass.getName().contains( "Proxy" ) )
        {
            log.error( "Error, can't use Hibernate proxy class names!!!" );
            throw new IllegalStateException( "Input class must not be Hibernate proxy class!!!" );
        }

        Schema schema = getSchema( klass );

        if ( schema != null )
        {
            return schema;
        }

        // Lookup the implementation class of core interfaces, if the input
        // klass is a core interface
        klass = getConcreteClass( klass );

        String name = getName( klass );

        schema = new Schema( klass, name, name + "s" );
        schema.setDisplayName( beautify( schema ) );
        schema.setPropertyMap( new HashMap<>( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );

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
        schemas.sort( OrderComparator.INSTANCE );

        return schemas;
    }

    @Override
    public List<Schema> getMetadataSchemas()
    {
        List<Schema> schemas = getSchemas();

        schemas.removeIf( schema -> !schema.isMetadata() );
        schemas.sort( OrderComparator.INSTANCE );

        return schemas;
    }

    @Override
    public Set<String> collectAuthorities()
    {
        return getSchemas().stream()
            .map( Schema::getAuthorities ).flatMap( Collection::stream )
            .map( Authority::getAuthorities ).flatMap( Collection::stream )
            .collect( toSet() );
    }

    private void updateSelf( Schema schema )
    {
        if ( schema.haveProperty( PROPERTY_SCHEMA ) )
        {
            Property property = schema.getProperty( PROPERTY_SCHEMA );
            schema.setName( property.getName() );
            schema.setCollectionName( schema.getPlural() );
            schema.setNamespace( property.getNamespace() );
            schema.getPropertyMap().remove( PROPERTY_SCHEMA );
        }
    }

    private String beautify( Schema schema )
    {
        String[] camelCaseWords = org.apache.commons.lang3.StringUtils.capitalize( schema.getPlural() )
            .split( "(?=[A-Z])" );
        return org.apache.commons.lang3.StringUtils.join( camelCaseWords, " " ).trim();
    }
}
