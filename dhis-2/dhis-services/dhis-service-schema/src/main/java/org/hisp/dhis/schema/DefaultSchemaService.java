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
import org.hisp.dhis.schema.descriptors.AccessSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AggregateDataExchangeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AnalyticsPeriodBoundarySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AnalyticsTableHookSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AttributeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AttributeValueSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.AxisSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionGroupSetDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionGroupSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ConstantSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DashboardItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataApprovalLevelSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataApprovalWorkflowSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementGroupSetDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementGroupSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementOperandSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataEntryFormSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataInputPeriodSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DocumentSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventChartSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventHookSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventRepetitionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventReportSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventVisualizationSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ExpressionDimensionItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ExpressionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ExternalFileResourceSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ExternalMapLayerSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.FileResourceSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorGroupSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorTypeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.InterpretationCommentSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.InterpretationSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ItemConfigSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.KeyJsonValueSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.LayoutSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.LegendDefinitionsSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.LegendSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.LegendSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.MapSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.MapViewSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.MessageConversationSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.MetadataVersionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.MinMaxDataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ObjectStyleSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionGroupSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitGroupSetDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitGroupSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitLevelSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OutlierAnalysisSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.PredictorGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.PredictorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramDataElementDimensionItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramIndicatorGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramIndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramRuleActionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramRuleSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramRuleVariableSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramSectionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageDataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageInstanceFilterSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageInstanceSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageSectionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageWorkingListSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramTrackedEntityAttributeDimensionItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramTrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.PushAnalysisSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RelationshipConstraintSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RelationshipItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RelationshipSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RelationshipTypeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ReportSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ReportingRateSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RouteSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.SectionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.SeriesKeySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.SharingSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.SmsCommandSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.SqlViewSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeValueSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityDataElementDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceFilterSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityProgramIndicatorDimensionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityTypeAttributeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityTypeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserAccessSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserCredentialsSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserGroupAccessSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserRoleSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.UserSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ValidationNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ValidationResultSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ValidationRuleGroupSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ValidationRuleSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.VisualizationSchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.CaseFormat;
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
    private static final Map<Class<?>, Class<?>> BASE_ALIAS_MAP = Map.of(
        IdentifiableObject.class, BaseIdentifiableObject.class,
        NameableObject.class, BaseNameableObject.class,
        DimensionalObject.class, BaseDimensionalObject.class,
        DimensionalItemObject.class, BaseDimensionalItemObject.class,
        AnalyticalObject.class, BaseAnalyticalObject.class );

    private final Map<Class<?>, SchemaDescriptor> descriptors = new ConcurrentHashMap<>();

    private void init()
    {
        register( new AggregateDataExchangeSchemaDescriptor() );
        register( new EventHookSchemaDescriptor() );
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
        register( new ExpressionDimensionItemSchemaDescriptor() );
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
        register( new ProgramStageWorkingListSchemaDescriptor() );
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
        register( new RouteSchemaDescriptor() );
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
        if ( schema.hasProperty( PROPERTY_SCHEMA ) )
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
