package org.hisp.dhis.dxf2.metadata;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class DefaultMetadataExportService implements MetadataExportService
{
    private static final Log log = LogFactory.getLog( MetadataExportService.class );

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Override
    @SuppressWarnings( "unchecked" )
    public Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> getMetadata( MetadataExportParams params )
    {
        Timer timer = new SystemTimer().start();
        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = new HashMap<>();

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        if ( params.getClasses().isEmpty() )
        {
            schemaService.getMetadataSchemas().stream().filter( Schema::isIdentifiableObject )
                .forEach( schema -> params.getClasses().add( (Class<? extends IdentifiableObject>) schema.getKlass() ) );
        }

        log.info( "(" + params.getUsername() + ") Export:Start" );

        for ( Class<? extends IdentifiableObject> klass : params.getClasses() )
        {
            Query query;

            if ( params.getQuery( klass ) != null )
            {
                query = params.getQuery( klass );
            }
            else
            {
                OrderParams orderParams = new OrderParams( Sets.newHashSet( params.getDefaultOrder() ) );
                query = queryService.getQueryFromUrl( klass, params.getDefaultFilter(), orderParams.getOrders( schemaService.getDynamicSchema( klass ) ) );
            }

            if ( query.getUser() == null )
            {
                query.setUser( params.getUser() );
            }

            query.setDefaultOrder();
            List<? extends IdentifiableObject> objects = queryService.query( query );

            if ( !objects.isEmpty() )
            {
                log.info( "(" + params.getUsername() + ") Exported " + objects.size() + " objects of type " + klass.getSimpleName() );
                metadata.put( klass, objects );
            }
        }

        log.info( "(" + params.getUsername() + ") Export:Done took " + timer.toString() );

        return metadata;
    }

    @Override
    public RootNode getMetadataAsNode( MetadataExportParams params )
    {
        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( new SimpleNode( "date", new Date(), true ) );

        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = getMetadata( params );

        for ( Class<? extends IdentifiableObject> klass : metadata.keySet() )
        {
            rootNode.addChild( fieldFilterService.filter( klass, metadata.get( klass ), params.getFields( klass ) ) );
        }

        return rootNode;
    }

    @Override
    public void validate( MetadataExportParams params )
    {

    }

    @Override
    @SuppressWarnings( "unchecked" )
    public MetadataExportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        MetadataExportParams params = new MetadataExportParams();
        Map<Class<? extends IdentifiableObject>, Map<String, List<String>>> map = new HashMap<>();

        if ( parameters.containsKey( "fields" ) )
        {
            params.setDefaultFields( parameters.get( "fields" ) );
            parameters.remove( "fields" );
        }

        if ( parameters.containsKey( "filter" ) )
        {
            params.setDefaultFilter( parameters.get( "filter" ) );
            parameters.remove( "filter" );
        }

        if ( parameters.containsKey( "order" ) )
        {
            params.setDefaultOrder( parameters.get( "order" ) );
            parameters.remove( "order" );
        }

        for ( String parameterKey : parameters.keySet() )
        {
            String[] parameter = parameterKey.split( ":" );
            Schema schema = schemaService.getSchemaByPluralName( parameter[0] );

            if ( schema == null || !schema.isIdentifiableObject() )
            {
                continue;
            }

            Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) schema.getKlass();

            // class is enabled if value = true, or fields/filter/order is present
            if ( "true".equalsIgnoreCase( parameters.get( parameterKey ).get( 0 ) ) || (parameter.length > 1 && ("fields".equalsIgnoreCase( parameter[1] )
                || "filter".equalsIgnoreCase( parameter[1] ) || "order".equalsIgnoreCase( parameter[1] ))) )
            {
                if ( !map.containsKey( klass ) ) map.put( klass, new HashMap<>() );
            }
            else
            {
                continue;
            }

            if ( parameter.length > 1 )
            {
                if ( "fields".equalsIgnoreCase( parameter[1] ) )
                {
                    if ( !map.get( klass ).containsKey( "fields" ) ) map.get( klass ).put( "fields", new ArrayList<>() );
                    map.get( klass ).get( "fields" ).addAll( parameters.get( parameterKey ) );
                }

                if ( "filter".equalsIgnoreCase( parameter[1] ) )
                {
                    if ( !map.get( klass ).containsKey( "filter" ) ) map.get( klass ).put( "filter", new ArrayList<>() );
                    map.get( klass ).get( "filter" ).addAll( parameters.get( parameterKey ) );
                }

                if ( "order".equalsIgnoreCase( parameter[1] ) )
                {
                    if ( !map.get( klass ).containsKey( "order" ) ) map.get( klass ).put( "order", new ArrayList<>() );
                    map.get( klass ).get( "order" ).addAll( parameters.get( parameterKey ) );
                }
            }
        }

        map.keySet().forEach( params::addClass );

        for ( Class<? extends IdentifiableObject> klass : map.keySet() )
        {
            Map<String, List<String>> classMap = map.get( klass );
            Schema schema = schemaService.getDynamicSchema( klass );

            if ( classMap.containsKey( "fields" ) ) params.addFields( klass, classMap.get( "fields" ) );

            if ( classMap.containsKey( "filter" ) && classMap.containsKey( "order" ) )
            {
                OrderParams orderParams = new OrderParams( Sets.newHashSet( classMap.get( "order" ) ) );
                Query query = queryService.getQueryFromUrl( klass, classMap.get( "filter" ), orderParams.getOrders( schema ) );
                query.setDefaultOrder();
                params.addQuery( query );
            }
            else if ( classMap.containsKey( "filter" ) )
            {
                Query query = queryService.getQueryFromUrl( klass, classMap.get( "filter" ), new ArrayList<>() );
                query.setDefaultOrder();
                params.addQuery( query );
            }
            else if ( classMap.containsKey( "order" ) )
            {
                OrderParams orderParams = new OrderParams();
                orderParams.setOrder( Sets.newHashSet( classMap.get( "order" ) ) );

                Query query = queryService.getQueryFromUrl( klass, new ArrayList<>(), orderParams.getOrders( schema ) );
                query.setDefaultOrder();
                params.addQuery( query );
            }
        }

        return params;
    }

    @Override
    public Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> getMetadataWithDependencies( IdentifiableObject object )
    {
        Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata = new HashMap<>();

        if ( DataSet.class.isInstance( object ) ) return handleDataSet( metadata, (DataSet) object );
        if ( Program.class.isInstance( object ) ) return handleProgram( metadata, (Program) object );

        return metadata;
    }

    @Override
    public RootNode getMetadataWithDependenciesAsNode( IdentifiableObject object )
    {
        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( new SimpleNode( "date", new Date(), true ) );

        Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata = getMetadataWithDependencies( object );

        for ( Class<? extends IdentifiableObject> klass : metadata.keySet() )
        {
            rootNode.addChild( fieldFilterService.filter( klass, Lists.newArrayList( metadata.get( klass ) ), Lists.newArrayList( ":owner" ) ) );
        }

        return rootNode;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleDataSet( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataSet dataSet )
    {
        if ( !metadata.containsKey( DataSet.class ) ) metadata.put( DataSet.class, new HashSet<>() );
        metadata.get( DataSet.class ).add( dataSet );

        dataSet.getDataElements().forEach( dataElement -> handleDataElement( metadata, dataElement ) );
        dataSet.getSections().forEach( section -> handleSection( metadata, section ) );
        dataSet.getIndicators().forEach( indicator -> handleIndicator( metadata, indicator ) );

        handleDataEntryForm( metadata, dataSet.getDataEntryForm() );
        handleLegendSet( metadata, dataSet.getLegendSet() );
        handleCategoryCombo( metadata, dataSet.getCategoryCombo() );

        dataSet.getCompulsoryDataElementOperands().forEach( dataElementOperand -> handleDataElementOperand( metadata, dataElementOperand ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleDataElementOperand( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElementOperand dataElementOperand )
    {
        if ( dataElementOperand == null ) return metadata;

        handleCategoryOptionCombo( metadata, dataElementOperand.getCategoryOptionCombo() );
        handleLegendSet( metadata, dataElementOperand.getLegendSet() );
        handleDataElement( metadata, dataElementOperand.getDataElement() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleCategoryOptionCombo( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        if ( categoryOptionCombo == null ) return metadata;
        if ( !metadata.containsKey( DataElementCategoryOptionCombo.class ) ) metadata.put( DataElementCategoryOptionCombo.class, new HashSet<>() );
        metadata.get( DataElementCategoryOptionCombo.class ).add( categoryOptionCombo );

        handleCategoryCombo( metadata, categoryOptionCombo.getCategoryCombo() );
        categoryOptionCombo.getCategoryOptions().forEach( categoryOption -> handleCategoryOption( metadata, categoryOption ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleCategoryCombo( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElementCategoryCombo categoryCombo )
    {
        if ( categoryCombo == null ) return metadata;
        if ( !metadata.containsKey( DataElementCategoryCombo.class ) ) metadata.put( DataElementCategoryCombo.class, new HashSet<>() );
        metadata.get( DataElementCategoryCombo.class ).add( categoryCombo );

        categoryCombo.getCategories().forEach( category -> handleCategory( metadata, category ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleCategory( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElementCategory category )
    {
        if ( category == null ) return metadata;
        if ( !metadata.containsKey( DataElementCategory.class ) ) metadata.put( DataElementCategory.class, new HashSet<>() );
        metadata.get( DataElementCategory.class ).add( category );

        category.getCategoryOptions().forEach( categoryOption -> handleCategoryOption( metadata, categoryOption ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleCategoryOption( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElementCategoryOption categoryOption )
    {
        if ( categoryOption == null ) return metadata;
        if ( !metadata.containsKey( DataElementCategoryOption.class ) ) metadata.put( DataElementCategoryOption.class, new HashSet<>() );
        metadata.get( DataElementCategoryOption.class ).add( categoryOption );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleLegendSet( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, LegendSet legendSet )
    {
        if ( legendSet == null ) return metadata;
        if ( !metadata.containsKey( LegendSet.class ) ) metadata.put( LegendSet.class, new HashSet<>() );
        metadata.get( LegendSet.class ).add( legendSet );

        legendSet.getLegends().forEach( legend -> handleLegend( metadata, legend ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleLegend( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, Legend legend )
    {
        if ( legend == null ) return metadata;
        if ( !metadata.containsKey( Legend.class ) ) metadata.put( Legend.class, new HashSet<>() );
        metadata.get( Legend.class ).add( legend );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleDataEntryForm( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataEntryForm dataEntryForm )
    {
        if ( dataEntryForm == null ) return metadata;
        if ( !metadata.containsKey( DataEntryForm.class ) ) metadata.put( DataEntryForm.class, new HashSet<>() );
        metadata.get( DataEntryForm.class ).add( dataEntryForm );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleDataElement( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, DataElement dataElement )
    {
        if ( dataElement == null ) return metadata;
        if ( !metadata.containsKey( DataElement.class ) ) metadata.put( DataElement.class, new HashSet<>() );
        metadata.get( DataElement.class ).add( dataElement );

        handleCategoryCombo( metadata, dataElement.getCategoryCombo() );
        handleOptionSet( metadata, dataElement.getOptionSet() );
        handleOptionSet( metadata, dataElement.getCommentOptionSet() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleOptionSet( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, OptionSet optionSet )
    {
        if ( optionSet == null ) return metadata;
        if ( !metadata.containsKey( OptionSet.class ) ) metadata.put( OptionSet.class, new HashSet<>() );
        metadata.get( OptionSet.class ).add( optionSet );

        optionSet.getOptions().forEach( o -> handleOption( metadata, o ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleOption( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, Option option )
    {
        if ( option == null ) return metadata;
        if ( !metadata.containsKey( Option.class ) ) metadata.put( Option.class, new HashSet<>() );
        metadata.get( Option.class ).add( option );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleSection( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, Section section )
    {
        if ( section == null ) return metadata;
        if ( !metadata.containsKey( Section.class ) ) metadata.put( Section.class, new HashSet<>() );
        metadata.get( Section.class ).add( section );

        section.getGreyedFields().forEach( dataElementOperand -> handleDataElementOperand( metadata, dataElementOperand ) );
        section.getIndicators().forEach( indicator -> handleIndicator( metadata, indicator ) );
        section.getDataElements().forEach( dataElement -> handleDataElement( metadata, dataElement ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleIndicator( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, Indicator indicator )
    {
        if ( indicator == null ) return metadata;
        if ( !metadata.containsKey( Indicator.class ) ) metadata.put( Indicator.class, new HashSet<>() );
        metadata.get( Indicator.class ).add( indicator );

        handleIndicatorType( metadata, indicator.getIndicatorType() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleIndicatorType( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, IndicatorType indicatorType )
    {
        if ( indicatorType == null ) return metadata;
        if ( !metadata.containsKey( IndicatorType.class ) ) metadata.put( IndicatorType.class, new HashSet<>() );
        metadata.get( IndicatorType.class ).add( indicatorType );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgram( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, Program program )
    {
        if ( program == null ) return metadata;
        if ( !metadata.containsKey( Program.class ) ) metadata.put( Program.class, new HashSet<>() );
        metadata.get( Program.class ).add( program );

        handleCategoryCombo( metadata, program.getCategoryCombo() );
        handleDataEntryForm( metadata, program.getDataEntryForm() );
        handleTrackedEntity( metadata, program.getTrackedEntity() );

        program.getProgramStages().forEach( programStage -> handleProgramStage( metadata, programStage ) );
        program.getProgramAttributes().forEach( programTrackedEntityAttribute -> handleProgramTrackedEntityAttribute( metadata, programTrackedEntityAttribute ) );
        program.getProgramIndicators().forEach( programIndicator -> handleProgramIndicator( metadata, programIndicator ) );

        List<ProgramRule> programRules = programRuleService.getProgramRule( program );
        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( program );

        programRules.forEach( programRule -> handleProgramRule( metadata, programRule ) );
        programRuleVariables.forEach( programRuleVariable -> handleProgramRuleVariable( metadata, programRuleVariable ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramRuleVariable( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramRuleVariable programRuleVariable )
    {
        if ( programRuleVariable == null ) return metadata;
        if ( !metadata.containsKey( ProgramRuleVariable.class ) ) metadata.put( ProgramRuleVariable.class, new HashSet<>() );
        metadata.get( ProgramRuleVariable.class ).add( programRuleVariable );

        handleTrackedEntityAttribute( metadata, programRuleVariable.getAttribute() );
        handleDataElement( metadata, programRuleVariable.getDataElement() );
        handleProgramStage( metadata, programRuleVariable.getProgramStage() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleTrackedEntityAttribute( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, TrackedEntityAttribute trackedEntityAttribute )
    {
        if ( trackedEntityAttribute == null ) return metadata;
        if ( !metadata.containsKey( TrackedEntityAttribute.class ) ) metadata.put( TrackedEntityAttribute.class, new HashSet<>() );
        metadata.get( TrackedEntityAttribute.class ).add( trackedEntityAttribute );

        handleOptionSet( metadata, trackedEntityAttribute.getOptionSet() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramRule( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramRule programRule )
    {
        if ( programRule == null ) return metadata;
        if ( !metadata.containsKey( ProgramRule.class ) ) metadata.put( ProgramRule.class, new HashSet<>() );
        metadata.get( ProgramRule.class ).add( programRule );

        programRule.getProgramRuleActions().forEach( programRuleAction -> handleProgramRuleAction( metadata, programRuleAction ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramRuleAction( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramRuleAction programRuleAction )
    {
        if ( programRuleAction == null ) return metadata;
        if ( !metadata.containsKey( ProgramRuleAction.class ) ) metadata.put( ProgramRuleAction.class, new HashSet<>() );
        metadata.get( ProgramRuleAction.class ).add( programRuleAction );

        handleDataElement( metadata, programRuleAction.getDataElement() );
        handleTrackedEntityAttribute( metadata, programRuleAction.getAttribute() );
        handleProgramIndicator( metadata, programRuleAction.getProgramIndicator() );
        handleProgramStageSection( metadata, programRuleAction.getProgramStageSection() );
        handleProgramStage( metadata, programRuleAction.getProgramStage() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramTrackedEntityAttribute( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramTrackedEntityAttribute programTrackedEntityAttribute )
    {
        if ( programTrackedEntityAttribute == null ) return metadata;
        if ( !metadata.containsKey( ProgramTrackedEntityAttribute.class ) ) metadata.put( ProgramTrackedEntityAttribute.class, new HashSet<>() );
        metadata.get( ProgramTrackedEntityAttribute.class ).add( programTrackedEntityAttribute );

        handleTrackedEntityAttribute( metadata, programTrackedEntityAttribute.getAttribute() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramStage( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramStage programStage )
    {
        if ( programStage == null ) return metadata;
        if ( !metadata.containsKey( ProgramStage.class ) ) metadata.put( ProgramStage.class, new HashSet<>() );
        metadata.get( ProgramStage.class ).add( programStage );

        programStage.getProgramStageDataElements().forEach( programStageDataElement -> handleProgramStageDataElement( metadata, programStageDataElement ) );
        programStage.getProgramStageSections().forEach( programStageSection -> handleProgramStageSection( metadata, programStageSection ) );
        handleDataEntryForm( metadata, programStage.getDataEntryForm() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramStageSection( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramStageSection programStageSection )
    {
        if ( programStageSection == null ) return metadata;
        if ( !metadata.containsKey( ProgramStageSection.class ) ) metadata.put( ProgramStageSection.class, new HashSet<>() );
        metadata.get( ProgramStageSection.class ).add( programStageSection );

        programStageSection.getProgramStageDataElements().forEach( programStageDataElement -> handleProgramStageDataElement( metadata, programStageDataElement ) );
        programStageSection.getProgramIndicators().forEach( programIndicator -> handleProgramIndicator( metadata, programIndicator ) );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramIndicator( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramIndicator programIndicator )
    {
        if ( programIndicator == null ) return metadata;
        if ( !metadata.containsKey( ProgramIndicator.class ) ) metadata.put( ProgramIndicator.class, new HashSet<>() );
        metadata.get( ProgramIndicator.class ).add( programIndicator );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleProgramStageDataElement( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, ProgramStageDataElement programStageDataElement )
    {
        if ( programStageDataElement == null ) return metadata;
        if ( !metadata.containsKey( ProgramStageDataElement.class ) ) metadata.put( ProgramStageDataElement.class, new HashSet<>() );
        metadata.get( ProgramStageDataElement.class ).add( programStageDataElement );

        handleDataElement( metadata, programStageDataElement.getDataElement() );

        return metadata;
    }

    private Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> handleTrackedEntity( Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> metadata, TrackedEntity trackedEntity )
    {
        if ( trackedEntity == null ) return metadata;
        if ( !metadata.containsKey( TrackedEntity.class ) ) metadata.put( TrackedEntity.class, new HashSet<>() );
        metadata.get( TrackedEntity.class ).add( trackedEntity );

        return metadata;
    }
}
