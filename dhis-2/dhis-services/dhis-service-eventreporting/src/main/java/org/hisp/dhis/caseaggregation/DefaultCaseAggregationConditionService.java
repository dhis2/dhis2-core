package org.hisp.dhis.caseaggregation;

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

import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.OBJECT_ORGUNIT_COMPLETE_PROGRAM_STAGE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.OBJECT_PROGRAM;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.OBJECT_PROGRAM_STAGE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.OBJECT_PROGRAM_STAGE_DATAELEMENT;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.OBJECT_TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.PARAM_PERIOD_END_DATE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.PARAM_PERIOD_START_DATE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.PARAM_PERIOD_ID;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.PARAM_PERIOD_ISO_DATE;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.SEPARATOR_ID;
import static org.hisp.dhis.caseaggregation.CaseAggregationCondition.SEPARATOR_OBJECT;
import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.commons.util.ConcurrentUtils;
import org.hisp.dhis.system.util.SystemUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
public class DefaultCaseAggregationConditionService
    implements CaseAggregationConditionService
{
    private static final String INVALID_CONDITION = "Invalid condition";

    private static final String IN_CONDITION_GET_ALL = "*";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CaseAggregationConditionStore aggregationConditionStore;

    private DataElementService dataElementService;

    private ProgramStageService programStageService;

    private ProgramService programService;

    private TrackedEntityAttributeService attributeService;

    private PeriodService periodService;

    private I18nService i18nService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    @Autowired
    private Notifier notifier;
    
    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public void setAggregationConditionStore( CaseAggregationConditionStore aggregationConditionStore )
    {
        this.aggregationConditionStore = aggregationConditionStore;
    }

    public void setAttributeService( TrackedEntityAttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    // -------------------------------------------------------------------------
    // Implementation Methods
    // -------------------------------------------------------------------------

    @Override
    public int addCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition )
    {
        return aggregationConditionStore.save( caseAggregationCondition );
    }

    @Override
    public void deleteCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition )
    {
        aggregationConditionStore.delete( caseAggregationCondition );
    }

    @Override
    public Collection<CaseAggregationCondition> getAllCaseAggregationCondition()
    {
        return i18n( i18nService, aggregationConditionStore.getAll() );
    }

    @Override
    public CaseAggregationCondition getCaseAggregationCondition( int id )
    {
        return i18n( i18nService, aggregationConditionStore.get( id ) );
    }

    @Override
    public CaseAggregationCondition getCaseAggregationCondition( String name )
    {
        return i18n( i18nService, aggregationConditionStore.getByName( name ) );
    }

    @Override
    public CaseAggregationCondition getCaseAggregationConditionByUid( String uid )
    {
        return i18n( i18nService, aggregationConditionStore.getByUid( uid ) );
    }

    @Override
    public void updateCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition )
    {
        aggregationConditionStore.update( caseAggregationCondition );
    }

    @Override
    public Collection<CaseAggregationCondition> getCaseAggregationCondition( DataElement dataElement )
    {
        return i18n( i18nService, aggregationConditionStore.get( dataElement ) );
    }

    @Override
    public CaseAggregationCondition getCaseAggregationCondition( DataElement dataElement,
        DataElementCategoryOptionCombo optionCombo )
    {
        return i18n( i18nService, aggregationConditionStore.get( dataElement, optionCombo ) );
    }

    @Override
    public String getConditionDescription( String condition )
    {
        StringBuffer description = new StringBuffer();

        Pattern patternCondition = Pattern.compile( CaseAggregationCondition.regExp );

        Matcher matcher = patternCondition.matcher( condition );

        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );

            if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM_STAGE_DATAELEMENT ) )
            {
                String[] ids = info[1].split( SEPARATOR_ID );

                int programId = Integer.parseInt( ids[0] );
                Program program = programService.getProgram( programId );

                String programStageId = ids[1];
                String programStageName = "";
                if ( !programStageId.equals( IN_CONDITION_GET_ALL ) )
                {
                    ProgramStage ps = programStageService.getProgramStage( Integer.parseInt( programStageId ) );
                    if ( ps != null )
                    {
                        programStageName = ps.getName();
                    }
                }
                else
                {
                    programStageName = programStageId;
                }

                int dataElementId = Integer.parseInt( ids[2] );
                DataElement dataElement = dataElementService.getDataElement( dataElementId );

                if ( program == null || dataElement == null || programStageName.isEmpty() )
                {
                    return INVALID_CONDITION;
                }

                matcher.appendReplacement( description, "[" + program.getDisplayName() + SEPARATOR_ID
                    + programStageName + SEPARATOR_ID + dataElement.getName() + "]" );
            }
            else
            {
                String[] ids = info[1].split( SEPARATOR_ID );

                if ( info[0].equalsIgnoreCase( OBJECT_TRACKED_ENTITY_ATTRIBUTE ) )
                {
                    int objectId = Integer.parseInt( ids[0] );

                    TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( objectId );

                    if ( attribute == null )
                    {
                        return INVALID_CONDITION;
                    }

                    matcher.appendReplacement( description, "[" + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT
                        + attribute.getDisplayName() + "]" );
                }
                else if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM ) )
                {
                    int objectId = Integer.parseInt( ids[0] );

                    Program program = programService.getProgram( objectId );

                    if ( program == null )
                    {
                        return INVALID_CONDITION;
                    }

                    String programDes = OBJECT_PROGRAM + SEPARATOR_ID + program.getDisplayName();
                    if ( ids.length == 2 )
                    {
                        programDes += SEPARATOR_OBJECT + ids[1];
                    }
                    matcher.appendReplacement( description, "[" + programDes + "]" );
                }
                else if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM_STAGE )
                    || info[0].equalsIgnoreCase( OBJECT_ORGUNIT_COMPLETE_PROGRAM_STAGE ) )
                {
                    int objectId = Integer.parseInt( ids[0] );
                    ProgramStage programStage = programStageService.getProgramStage( objectId );

                    if ( programStage == null )
                    {
                        return INVALID_CONDITION;
                    }

                    String count = (ids.length == 2) ? SEPARATOR_ID + ids[1] : "";
                    matcher.appendReplacement( description,
                        "[" + info[0] + SEPARATOR_OBJECT + programStage.getDisplayName() + count + "]" );
                }
            }

        }

        matcher.appendTail( description );

        return description.toString();
    }

    @Override
    public Collection<DataElement> getDataElementsInCondition( String aggregationExpression )
    {
        String regExp = "\\[" + OBJECT_PROGRAM_STAGE_DATAELEMENT + SEPARATOR_OBJECT + "[0-9]+" + SEPARATOR_ID
            + "[0-9]+" + SEPARATOR_ID + "[0-9]+" + "\\]";

        Collection<DataElement> dataElements = new HashSet<>();

        // ---------------------------------------------------------------------
        // parse expressions
        // ---------------------------------------------------------------------

        Pattern pattern = Pattern.compile( regExp );

        Matcher matcher = pattern.matcher( aggregationExpression );

        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );
            String[] ids = info[1].split( SEPARATOR_ID );

            int dataElementId = Integer.parseInt( ids[2] );
            DataElement dataElement = dataElementService.getDataElement( dataElementId );

            dataElements.add( dataElement );
        }

        return dataElements;
    }

    @Override
    public Collection<Program> getProgramsInCondition( String aggregationExpression )
    {
        String regExp = "\\[(" + OBJECT_PROGRAM + "|" + OBJECT_PROGRAM_STAGE_DATAELEMENT + ")" + SEPARATOR_OBJECT
            + "[a-zA-Z0-9\\- ]+";

        Collection<Program> programs = new HashSet<>();

        // ---------------------------------------------------------------------
        // parse expressions
        // ---------------------------------------------------------------------

        Pattern pattern = Pattern.compile( regExp );

        Matcher matcher = pattern.matcher( aggregationExpression );

        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );
            String[] ids = info[1].split( SEPARATOR_ID );

            int programId = Integer.parseInt( ids[0] );
            Program program = programService.getProgram( programId );

            programs.add( program );
        }

        return programs;
    }

    @Override
    public Collection<TrackedEntityAttribute> getTrackedEntityAttributesInCondition( String aggregationExpression )
    {
        String regExp = "\\[" + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT + "[0-9]+\\]";

        Collection<TrackedEntityAttribute> attributes = new HashSet<>();

        // ---------------------------------------------------------------------
        // parse expressions
        // ---------------------------------------------------------------------

        Pattern pattern = Pattern.compile( regExp );

        Matcher matcher = pattern.matcher( aggregationExpression );

        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );

            int attributeId = Integer.parseInt( info[1] );
            TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( attributeId );

            attributes.add( attribute );
        }

        return attributes;
    }

    @Override
    public Collection<CaseAggregationCondition> getCaseAggregationConditions( Collection<DataElement> dataElements, String key,Integer first, Integer max )
    {
        return i18n( i18nService, aggregationConditionStore.get( dataElements, key, first, max ) );
    }

    @Override
    public int countCaseAggregationCondition( Collection<DataElement> dataElements, String key )
    {
        return aggregationConditionStore.count( dataElements, key );
    }
    
    @Override
    public void aggregate( List<CaseAggregateSchedule> caseAggregateSchedules, String taskStrategy, TaskId taskId )
    {   
        // Generate the periods
        
        for( CaseAggregateSchedule  schedule : caseAggregateSchedules ){
            List<Period> periods = new ArrayList<>(aggregationConditionStore.getPeriods( schedule.getPeriodTypeName(),
                taskStrategy ));
            periodService.reloadPeriods( periods );
        }
        
        ConcurrentLinkedQueue<CaseAggregateSchedule> queryBuilderQ = new ConcurrentLinkedQueue<>(
            caseAggregateSchedules );
        List<Future<?>> futures = new ArrayList<>();

        for ( int i = 0; i < getProcessNo(); i++ )
        { 
            Future<?> future = aggregateValueManager( queryBuilderQ, taskStrategy, taskId );
            if ( future != null )
            {
                futures.add( future );
            }
        }
        
        ConcurrentUtils.waitForCompletion( futures );
    }

    @Override
    public List<Grid> getAggregateValue( Collection<CaseAggregationCondition> caseAggregationConditions, Collection<Integer> orgunitIds,
        Collection<Period> periods, I18nFormat format, I18n i18n )
    {
        Collection<Integer> _orgunitIds = aggregationConditionStore.getServiceOrgunit();
        _orgunitIds.retainAll( orgunitIds );
        if ( _orgunitIds.size() > 0 )
        {
            int attributeOptioncomboId = categoryService.getDefaultDataElementCategoryOptionCombo().getId();
            List<Grid> grids = new ArrayList<>();
            for ( CaseAggregationCondition condition : caseAggregationConditions )
            {  
                String sql = aggregationConditionStore.parseExpressionToSql( false, condition, attributeOptioncomboId, _orgunitIds );
                for ( Period period : periods )
                {
                    period =  periodService.reloadPeriod( period );
                    String periodSQL = sql;
                    periodSQL = replacePeriodSql( periodSQL, period );
                   
                    Grid grid = aggregationConditionStore.getAggregateValue( periodSQL, format, i18n );
                    grid.setTitle( condition.getDisplayName() );
                    grid.setSubtitle( format.formatPeriod( period ) );

                    grids.add( grid );
                }
            }
            
            return grids;
        }
        
        return null;
    }
    
    @Override
    public Grid getAggregateValueDetails( CaseAggregationCondition aggregationCondition, OrganisationUnit orgunit,
        Period period, I18nFormat format, I18n i18n )
    {
        periodService.reloadPeriod( period );

        boolean nonRegistrationProgram = singleEventWithoutRegistrationProgram( aggregationCondition.getAggregationExpression() );
        return aggregationConditionStore.getAggregateValueDetails( aggregationCondition, orgunit, period, nonRegistrationProgram, format, i18n  );
    }

    @Override
    public void insertAggregateValue( Collection<CaseAggregationCondition> caseAggregationConditions,
        Collection<Integer> orgunitIds, Collection<Period> periods )
    {
        Collection<Integer> _orgunitIds = aggregationConditionStore.getServiceOrgunit();
        _orgunitIds.retainAll( orgunitIds );
        if ( _orgunitIds.size() > 0 )
        {
            DataElementCategoryOptionCombo attributeOptioncombo = categoryService.getDefaultDataElementCategoryOptionCombo();
            for( CaseAggregationCondition caseAggregationCondition : caseAggregationConditions )
            {
                String sql = aggregationConditionStore.parseExpressionToSql(true, caseAggregationCondition, attributeOptioncombo.getId(), _orgunitIds );
    
                for ( Period period : periods )
                {
                    period = periodService.reloadPeriod( period );
                    sql = replacePeriodSql( sql, period );
                    aggregationConditionStore.insertAggregateValue( sql, caseAggregationCondition.getAggregationDataElement(), 
                        caseAggregationCondition.getOptionCombo(), attributeOptioncombo, _orgunitIds, period );
                }
            }
        }
    }

    @Override
    public String parseExpressionDetailsToSql( String caseExpression, String operator, Integer orgunitId, Period period )
    {
        periodService.reloadPeriod( period );

        boolean nonRegistrationProgram = singleEventWithoutRegistrationProgram( caseExpression );
        return aggregationConditionStore.parseExpressionDetailsToSql( caseExpression, operator, orgunitId, period, nonRegistrationProgram );
    }

    @Override
    public String parseExpressionToSql( boolean isInsert, String caseExpression, String operator,
        Integer aggregateDeId, String aggregateDeName, Integer optionComboId, String optionComboName, Integer deSumId,
        Collection<Integer> orgunitIds, Period period )
    {
        periodService.reloadPeriod( period );

        int attributeOptioncomboId = categoryService.getDefaultDataElementCategoryOptionCombo().getId();
        
        String sql = aggregationConditionStore.parseExpressionToSql( isInsert, caseExpression, operator, aggregateDeId,
            aggregateDeName, optionComboId, optionComboName, attributeOptioncomboId, deSumId, orgunitIds );
        
        return replacePeriodSql( sql, period );
    }

    @Override
    public List<Integer> executeSQL( String sql )
    {
        return aggregationConditionStore.executeSQL( sql );
    }
    
    // -------------------------------------------------------------------------
    // Support Methods
    // -------------------------------------------------------------------------

    private String replacePeriodSql( String sql, Period period )
    {
        sql = sql.replaceAll( "COMBINE", "" );
        sql = sql.replaceAll( PARAM_PERIOD_START_DATE,  DateUtils.getMediumDateString( period.getStartDate() ));
        sql = sql.replaceAll( PARAM_PERIOD_END_DATE,  DateUtils.getMediumDateString( period.getEndDate() ));
        sql = sql.replaceAll( PARAM_PERIOD_ID,  period.getId() + "" );
        sql = sql.replaceAll( PARAM_PERIOD_ISO_DATE, period.getIsoDate() );

        return sql;
    }
    
    @Async
    private Future<?> aggregateValueManager( ConcurrentLinkedQueue<CaseAggregateSchedule> caseAggregateSchedule,
        String taskStrategy, TaskId taskId )
    {
        int attributeOptioncomboId = categoryService.getDefaultDataElementCategoryOptionCombo().getId();
        
        taskLoop: while ( true )
        {
            CaseAggregateSchedule condition = caseAggregateSchedule.poll();
            if ( condition == null )
            {
                break taskLoop;
            }

            Collection<Period> periods = aggregationConditionStore.getPeriods( condition.getPeriodTypeName(),
                taskStrategy );
            if( taskId != null )
            {
                notifier.notify( taskId, "Importing data for " + condition.getCaseAggregateName() );
            }
            aggregationConditionStore.runAggregate( null, condition, periods, attributeOptioncomboId );
        }

        return null;
    }

    private int getProcessNo()
    {
        return Math.max( (SystemUtils.getCpuCores() - 1), 1 );
    }

    public Integer calValue( Collection<Integer> entityInstanceIds, String operator )
    {
        return entityInstanceIds.size();
    }

    private boolean singleEventWithoutRegistrationProgram( String expression )
    {
        Pattern patternCondition = Pattern.compile( CaseAggregationCondition.regExp );

        Matcher matcher = patternCondition.matcher( expression );

        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );

            if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM_STAGE_DATAELEMENT ) )
            {
                String[] ids = info[1].split( SEPARATOR_ID );
                
                int programId = Integer.parseInt( ids[0] );
                Program program = programService.getProgram( programId );

                if ( program != null && !program.isRegistration() )
                {
                    return true;
                }
            }
            else
            {
                String[] ids = info[1].split( SEPARATOR_ID );

                if ( info[0].equalsIgnoreCase( OBJECT_TRACKED_ENTITY_ATTRIBUTE ) )
                {
                    return false;
                }
                else if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM ) )
                {
                    int objectId = Integer.parseInt( ids[0] );

                    Program program = programService.getProgram( objectId );

                    if ( program != null && !program.isRegistration() )
                    {
                        return true;
                    }
                }
                else if ( info[0].equalsIgnoreCase( OBJECT_PROGRAM_STAGE )
                    || info[0].equalsIgnoreCase( OBJECT_ORGUNIT_COMPLETE_PROGRAM_STAGE ) )
                {
                    int objectId = Integer.parseInt( ids[0] );
                    ProgramStage programStage = programStageService.getProgramStage( objectId );
                    if (programStage!=null && !programStage.getProgram().isRegistration() )
                    {
                        return true;
                    }
                }
            }

        }

        return false;
    }
}
