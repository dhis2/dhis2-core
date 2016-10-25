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
import org.hisp.dhis.common.SetMap;
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
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.ComplexNode;
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
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private SystemService systemService;

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

        SystemInfo systemInfo = systemService.getSystemInfo();

        ComplexNode system = rootNode.addChild( new ComplexNode( "system" ) );
        system.addChild( new SimpleNode( "id", systemInfo.getSystemId() ) );
        system.addChild( new SimpleNode( "rev", systemInfo.getRevision() ) );
        system.addChild( new SimpleNode( "version", systemInfo.getVersion() ) );
        system.addChild( new SimpleNode( "date", systemInfo.getServerDate() ) );

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
    public SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> getMetadataWithDependencies( IdentifiableObject object )
    {
        SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata = new SetMap<>();

        if ( DataSet.class.isInstance( object ) ) return handleDataSet( metadata, (DataSet) object );
        if ( Program.class.isInstance( object ) ) return handleProgram( metadata, (Program) object );

        return metadata;
    }

    @Override
    public RootNode getMetadataWithDependenciesAsNode( IdentifiableObject object )
    {
        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( new SimpleNode( "date", new Date(), true ) );

        SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata = getMetadataWithDependencies( object );

        for ( Class<? extends IdentifiableObject> klass : metadata.keySet() )
        {
            rootNode.addChild( fieldFilterService.filter( klass, Lists.newArrayList( metadata.get( klass ) ), Lists.newArrayList( ":owner" ) ) );
        }

        return rootNode;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleDataSet( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataSet dataSet )
    {
        metadata.putValue( DataSet.class, dataSet );

        dataSet.getDataSetElements().forEach( dataSetElement -> handleDataSetElement( metadata, dataSetElement ) );
        dataSet.getSections().forEach( section -> handleSection( metadata, section ) );
        dataSet.getIndicators().forEach( indicator -> handleIndicator( metadata, indicator ) );

        handleDataEntryForm( metadata, dataSet.getDataEntryForm() );
        handleLegendSet( metadata, dataSet.getLegendSet() );
        handleCategoryCombo( metadata, dataSet.getCategoryCombo() );

        dataSet.getCompulsoryDataElementOperands().forEach( dataElementOperand -> handleDataElementOperand( metadata, dataElementOperand ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleDataElementOperand( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElementOperand dataElementOperand )
    {
        if ( dataElementOperand == null ) return metadata;

        handleCategoryOptionCombo( metadata, dataElementOperand.getCategoryOptionCombo() );
        handleLegendSet( metadata, dataElementOperand.getLegendSet() );
        handleDataElement( metadata, dataElementOperand.getDataElement() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleCategoryOptionCombo( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        if ( categoryOptionCombo == null ) return metadata;
        metadata.putValue( DataElementCategoryOptionCombo.class, categoryOptionCombo );

        handleCategoryCombo( metadata, categoryOptionCombo.getCategoryCombo() );
        categoryOptionCombo.getCategoryOptions().forEach( categoryOption -> handleCategoryOption( metadata, categoryOption ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleCategoryCombo( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElementCategoryCombo categoryCombo )
    {
        if ( categoryCombo == null ) return metadata;
        metadata.putValue( DataElementCategoryCombo.class, categoryCombo );

        categoryCombo.getCategories().forEach( category -> handleCategory( metadata, category ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleCategory( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElementCategory category )
    {
        if ( category == null ) return metadata;
        metadata.putValue( DataElementCategory.class, category );

        category.getCategoryOptions().forEach( categoryOption -> handleCategoryOption( metadata, categoryOption ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleCategoryOption( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElementCategoryOption categoryOption )
    {
        if ( categoryOption == null ) return metadata;
        metadata.putValue( DataElementCategoryOption.class, categoryOption );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleLegendSet( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, LegendSet legendSet )
    {
        if ( legendSet == null ) return metadata;
        metadata.putValue( LegendSet.class, legendSet );

        legendSet.getLegends().forEach( legend -> handleLegend( metadata, legend ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleLegend( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, Legend legend )
    {
        if ( legend == null ) return metadata;
        metadata.putValue( Legend.class, legend );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleDataEntryForm( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataEntryForm dataEntryForm )
    {
        if ( dataEntryForm == null ) return metadata;
        metadata.putValue( DataEntryForm.class, dataEntryForm );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleDataSetElement( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataSetElement dataSetElement )
    {
        if ( dataSetElement == null ) return metadata;
        metadata.putValue( DataSetElement.class, dataSetElement );

        handleDataElement( metadata, dataSetElement.getDataElement() );
        handleCategoryCombo( metadata, dataSetElement.getCategoryCombo() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleDataElement( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, DataElement dataElement )
    {
        if ( dataElement == null ) return metadata;
        metadata.putValue( DataElement.class, dataElement );

        handleCategoryCombo( metadata, dataElement.getDataElementCategoryCombo() );
        handleOptionSet( metadata, dataElement.getOptionSet() );
        handleOptionSet( metadata, dataElement.getCommentOptionSet() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleOptionSet( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, OptionSet optionSet )
    {
        if ( optionSet == null ) return metadata;
        metadata.putValue( OptionSet.class, optionSet );

        optionSet.getOptions().forEach( o -> handleOption( metadata, o ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleOption( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, Option option )
    {
        if ( option == null ) return metadata;
        metadata.putValue( Option.class, option );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleSection( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, Section section )
    {
        if ( section == null ) return metadata;
        metadata.putValue( Section.class, section );

        section.getGreyedFields().forEach( dataElementOperand -> handleDataElementOperand( metadata, dataElementOperand ) );
        section.getIndicators().forEach( indicator -> handleIndicator( metadata, indicator ) );
        section.getDataElements().forEach( dataElement -> handleDataElement( metadata, dataElement ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleIndicator( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, Indicator indicator )
    {
        if ( indicator == null ) return metadata;
        metadata.putValue( Indicator.class, indicator );

        handleIndicatorType( metadata, indicator.getIndicatorType() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleIndicatorType( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, IndicatorType indicatorType )
    {
        if ( indicatorType == null ) return metadata;
        metadata.putValue( IndicatorType.class, indicatorType );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgram( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, Program program )
    {
        if ( program == null ) return metadata;
        metadata.putValue( Program.class, program );

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

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramRuleVariable( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramRuleVariable programRuleVariable )
    {
        if ( programRuleVariable == null ) return metadata;
        metadata.putValue( ProgramRuleVariable.class, programRuleVariable );

        handleTrackedEntityAttribute( metadata, programRuleVariable.getAttribute() );
        handleDataElement( metadata, programRuleVariable.getDataElement() );
        handleProgramStage( metadata, programRuleVariable.getProgramStage() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleTrackedEntityAttribute( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, TrackedEntityAttribute trackedEntityAttribute )
    {
        if ( trackedEntityAttribute == null ) return metadata;
        metadata.putValue( TrackedEntityAttribute.class, trackedEntityAttribute );

        handleOptionSet( metadata, trackedEntityAttribute.getOptionSet() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramRule( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramRule programRule )
    {
        if ( programRule == null ) return metadata;
        metadata.putValue( ProgramRule.class, programRule );

        programRule.getProgramRuleActions().forEach( programRuleAction -> handleProgramRuleAction( metadata, programRuleAction ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramRuleAction( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramRuleAction programRuleAction )
    {
        if ( programRuleAction == null ) return metadata;
        metadata.putValue( ProgramRuleAction.class, programRuleAction );

        handleDataElement( metadata, programRuleAction.getDataElement() );
        handleTrackedEntityAttribute( metadata, programRuleAction.getAttribute() );
        handleProgramIndicator( metadata, programRuleAction.getProgramIndicator() );
        handleProgramStageSection( metadata, programRuleAction.getProgramStageSection() );
        handleProgramStage( metadata, programRuleAction.getProgramStage() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramTrackedEntityAttribute( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramTrackedEntityAttribute programTrackedEntityAttribute )
    {
        if ( programTrackedEntityAttribute == null ) return metadata;
        metadata.putValue( ProgramTrackedEntityAttribute.class, programTrackedEntityAttribute );

        handleTrackedEntityAttribute( metadata, programTrackedEntityAttribute.getAttribute() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramStage( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramStage programStage )
    {
        if ( programStage == null ) return metadata;
        metadata.putValue( ProgramStage.class, programStage );

        programStage.getProgramStageDataElements().forEach( programStageDataElement -> handleProgramStageDataElement( metadata, programStageDataElement ) );
        programStage.getProgramStageSections().forEach( programStageSection -> handleProgramStageSection( metadata, programStageSection ) );
        handleDataEntryForm( metadata, programStage.getDataEntryForm() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramStageSection( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramStageSection programStageSection )
    {
        if ( programStageSection == null ) return metadata;
        metadata.putValue( ProgramStageSection.class, programStageSection );

        programStageSection.getProgramStageDataElements().forEach( programStageDataElement -> handleProgramStageDataElement( metadata, programStageDataElement ) );
        programStageSection.getProgramIndicators().forEach( programIndicator -> handleProgramIndicator( metadata, programIndicator ) );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramIndicator( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramIndicator programIndicator )
    {
        if ( programIndicator == null ) return metadata;
        metadata.putValue( ProgramIndicator.class, programIndicator );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleProgramStageDataElement( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, ProgramStageDataElement programStageDataElement )
    {
        if ( programStageDataElement == null ) return metadata;
        metadata.putValue( ProgramStageDataElement.class, programStageDataElement );

        handleDataElement( metadata, programStageDataElement.getDataElement() );

        return metadata;
    }

    private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> handleTrackedEntity( SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata, TrackedEntity trackedEntity )
    {
        if ( trackedEntity == null ) return metadata;
        metadata.putValue( TrackedEntity.class, trackedEntity );

        return metadata;
    }
}
