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
package org.hisp.dhis.predictor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static org.hisp.dhis.common.OrganisationUnitDescendants.DESCENDANTS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_SKIP_TEST;
import static org.hisp.dhis.predictor.PredictionDisaggregatorUtils.createPredictionDisaggregator;
import static org.hisp.dhis.predictor.PredictionFormatter.formatPrediction;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsServiceTarget;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionInfo;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
@Slf4j
@Service( "org.hisp.dhis.predictor.PredictionService" )
@Transactional
@AllArgsConstructor
public class DefaultPredictionService
    implements PredictionService, AnalyticsServiceTarget
{
    private final PredictorService predictorService;

    private final ExpressionService expressionService;

    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final PeriodService periodService;

    private final IdentifiableObjectManager idObjectManager;

    private final BatchHandlerFactory batchHandlerFactory;

    private AnalyticsService analyticsService;

    private final CurrentUserService currentUserService;

    @Override
    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    // -------------------------------------------------------------------------
    // Prediction business logic
    // -------------------------------------------------------------------------

    @Override
    public PredictionSummary predictJob( PredictorJobParameters params, JobProgress progress )
    {
        Date startDate = DateUtils.addDays( new Date(), params.getRelativeStart() );
        Date endDate = DateUtils.addDays( new Date(), params.getRelativeEnd() );

        return predictTask( startDate, endDate, params.getPredictors(), params.getPredictorGroups(), progress );
    }

    @Override
    public PredictionSummary predictTask( Date startDate, Date endDate,
        List<String> predictors, List<String> predictorGroups, JobProgress progress )
    {
        try
        {
            return predictAll( startDate, endDate, predictors, predictorGroups, progress );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            return new PredictionSummary( PredictionStatus.ERROR,
                "Predictions failed: " + ex.getMessage() );
        }
    }

    @Override
    public PredictionSummary predictAll( Date startDate, Date endDate, List<String> predictors,
        List<String> predictorGroups, JobProgress progress )
    {
        progress.startingStage( "Fetching predictors" );
        List<Predictor> predictorList = progress.runStage( List.of(),
            () -> fetchPredictors( predictors, predictorGroups ) );

        PredictionSummary summary = new PredictionSummary();
        progress.startingStage( format( "Running predictors from %s to %s", startDate, endDate ),
            predictorList.size(), SKIP_ITEM_OUTLIER );
        progress.runStage( predictorList.stream(),
            predictor -> format( "Running predictor %s from %s to %s", predictor.getName(), startDate, endDate ),
            predictor -> predict( predictor, startDate, endDate, summary ),
            ( success, failed ) -> format( "Finished predictors from %s to %s: %s", startDate, endDate, summary ) );

        return summary;
    }

    private List<Predictor> fetchPredictors( List<String> predictors, List<String> predictorGroups )
    {
        if ( CollectionUtils.isEmpty( predictors ) && CollectionUtils.isEmpty( predictorGroups ) )
        {
            return predictorService.getAllPredictors();
        }

        List<Predictor> predictorList = new ArrayList<>();
        if ( !CollectionUtils.isEmpty( predictors ) )
        {
            predictorList.addAll( idObjectManager.getByUid( Predictor.class, predictors ) );
        }

        if ( !CollectionUtils.isEmpty( predictorGroups ) )
        {
            for ( PredictorGroup predictorGroup : idObjectManager.getByUid( PredictorGroup.class, predictorGroups ) )
            {
                predictorList.addAll( predictorGroup.getSortedMembers() );
            }
        }
        return predictorList;
    }

    @Override
    public void predict( Predictor predictor, Date startDate, Date endDate, PredictionSummary predictionSummary )
    {
        Expression generator = predictor.getGenerator();
        Expression skipTest = predictor.getSampleSkipTest();
        DataElement outputDataElement = predictor.getOutput();
        DataType expressionDataType = DataType.fromValueType( outputDataElement.getValueType() );

        ExpressionInfo exInfo = new ExpressionInfo();
        ExpressionParams baseExParams = getBaseExParams( predictor, exInfo );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        PredictionDisaggregator preDis = createPredictionDisaggregator( predictor, defaultCategoryOptionCombo,
            baseExParams.getItemMap().values() );
        Set<DimensionalItemObject> items = preDis.getDisaggregatedItems();
        DataElementOperand outputDataElementOperand = preDis.getOutputDataElementOperand();

        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        Set<Period> existingOutputPeriods = getExistingPeriods( outputPeriods );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<Period> analyticsQueryPeriods = getAnalyticsQueryPeriods( exInfo, allSamplePeriods, existingOutputPeriods );
        Set<Period> dataValueQueryPeriods = getDataValueQueryPeriods( analyticsQueryPeriods, existingOutputPeriods );
        outputPeriods = periodService.reloadPeriods( outputPeriods );

        boolean forwardReference = isForwardReference( predictor, baseExParams.getItemMap().values() );
        boolean requireData = generator.getMissingValueStrategy() != NEVER_SKIP &&
            !baseExParams.getItemMap().values().isEmpty();

        Set<OrganisationUnit> currentUserOrgUnits = new HashSet<>();
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            currentUserOrgUnits = currentUser.getOrganisationUnits();
        }

        PredictionDataConsolidator consolidator = new PredictionDataConsolidator( items,
            predictor.getOrganisationUnitDescendants().equals( DESCENDANTS ),
            new PredictionDataValueFetcher( dataValueService, categoryService ),
            new PredictionAnalyticsDataFetcher( analyticsService, categoryService ) );

        PredictionWriter predictionWriter = new PredictionWriter( dataValueService, batchHandlerFactory );

        predictionWriter.init( existingOutputPeriods, predictionSummary );

        predictionSummary.incrementPredictors();

        for ( OrganisationUnitLevel orgUnitLevel : predictor.getOrganisationUnitLevels() )
        {
            List<OrganisationUnit> orgUnits = organisationUnitService
                .getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( orgUnitLevel ), currentUserOrgUnits );

            consolidator.init( currentUserOrgUnits, orgUnitLevel.getLevel(), orgUnits,
                dataValueQueryPeriods, analyticsQueryPeriods, existingOutputPeriods, outputDataElementOperand );

            PredictionData data;

            while ( (data = consolidator.getData()) != null )
            {
                List<DataValue> predictions = new ArrayList<>();

                List<PredictionContext> contexts = PredictionContextGenerator.getContexts(
                    outputPeriods, data.getValues(), defaultCategoryOptionCombo, preDis );

                for ( PredictionContext c : contexts )
                {
                    Map<DimensionalItemObject, Object> valueMap = firstNonNull(
                        c.getPeriodValueMap().get( c.getOutputPeriod() ), new HashMap<>() );

                    List<Period> samplePeriods = new ArrayList<>( samplePeriodsMap.get( c.getOutputPeriod() ) );

                    samplePeriods.removeAll( getSkippedPeriods( allSamplePeriods, baseExParams, c.getPeriodValueMap(),
                        skipTest, data.getOrgUnit() ) );

                    if ( !isEvaluationRequired( requireData, exInfo, samplePeriods, valueMap,
                        c.getPeriodValueMap(), baseExParams.getItemMap() ) )
                    {
                        continue;
                    }

                    Object value = expressionService.getExpressionValue( baseExParams.toBuilder()
                        .expression( predictor.getGenerator().getExpression() )
                        .parseType( PREDICTOR_EXPRESSION )
                        .dataType( expressionDataType )
                        .valueMap( valueMap )
                        .days( c.getOutputPeriod().getDaysInPeriod() )
                        .missingValueStrategy( generator.getMissingValueStrategy() )
                        .orgUnit( data.getOrgUnit() )
                        .samplePeriods( samplePeriods )
                        .periodValueMap( c.getPeriodValueMap() )
                        .build() );

                    DataValue prediction = processPrediction( predictor, c, value, currentUser, data.getOrgUnit() );

                    rememberPredictedValue( prediction, predictions, contexts, forwardReference );
                }

                predictionWriter.write( predictions, data.getOldPredictions() );
            }
        }

        predictionWriter.flush();

        preDis.issueMappingWarnings();
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private DataValue processPrediction( Predictor predictor, PredictionContext c, Object value, User currentUser,
        OrganisationUnit orgUnit )
    {
        DataValue prediction = null;

        if ( value != null || predictor.getGenerator().getMissingValueStrategy() == NEVER_SKIP )
        {
            String valueString = formatPrediction( value, predictor.getOutput() );

            if ( valueString != null )
            {
                String storedBy = currentUser == null ? "system-process" : currentUser.getUsername();

                prediction = new DataValue( predictor.getOutput(),
                    c.getOutputPeriod(), orgUnit, c.getCategoryOptionCombo(),
                    c.getAttributeOptionCombo(), valueString, storedBy, new Date(), null );
            }
        }

        return prediction;
    }

    private ExpressionParams getBaseExParams( Predictor predictor, ExpressionInfo expressionInfo )
    {
        DataType expressionDataType = DataType.fromValueType( predictor.getOutput().getValueType() );

        ExpressionInfo info = expressionService.getExpressionInfo( ExpressionParams.builder()
            .expression( predictor.getGenerator().getExpression() )
            .parseType( PREDICTOR_EXPRESSION )
            .dataType( expressionDataType )
            .expressionInfo( expressionInfo )
            .build() );

        if ( predictor.getSampleSkipTest() != null )
        {
            addSampleSkipTestToExInfo( info, predictor.getSampleSkipTest().getExpression() );
        }

        return expressionService.getBaseExpressionParams( info );
    }

    private void addSampleSkipTestToExInfo( ExpressionInfo exInfo, String skipTestExpression )
    {
        Set<DimensionalItemId> savedItemIds = exInfo.getAllItemIds();

        exInfo.setItemIds( new HashSet<>() );

        expressionService.getExpressionInfo( ExpressionParams.builder()
            .expression( skipTestExpression )
            .parseType( PREDICTOR_SKIP_TEST )
            .expressionInfo( exInfo )
            .build() );

        exInfo.getSampleItemIds().addAll( exInfo.getItemIds() );

        exInfo.setItemIds( savedItemIds );
    }

    /**
     * Returns any existing periods to be used for querying analytics items (if
     * there are any). Includes sample periods if there are any sample items,
     * and includes output periods if there are any output items.
     */
    private Set<Period> getAnalyticsQueryPeriods( ExpressionInfo exInfo, Set<Period> allSamplePeriods,
        Set<Period> existingOutputPeriods )
    {
        Set<Period> analyticsQueryPeriods = new HashSet<>();

        if ( !exInfo.getSampleItemIds().isEmpty() )
        {
            analyticsQueryPeriods.addAll( getExistingPeriods( new ArrayList<>( allSamplePeriods ) ) );
        }

        if ( !exInfo.getItemIds().isEmpty() )
        {
            analyticsQueryPeriods.addAll( existingOutputPeriods );
        }

        return analyticsQueryPeriods;
    }

    /**
     * Returns any existing periods to be used to query data values. This
     * includes all existing periods to be used for querying analytics items
     * plus all existing output periods (if not already included), to find
     * existing predictor values so we know how to process predictor outputs.
     */
    private Set<Period> getDataValueQueryPeriods( Set<Period> analyticsQueryPeriods, Set<Period> existingOutputPeriods )
    {
        return Sets.union( analyticsQueryPeriods, existingOutputPeriods );
    }

    /**
     * Finds sample periods that should be skipped based on the skip test.
     */
    private Set<Period> getSkippedPeriods( Set<Period> allSamplePeriods, ExpressionParams baseExParams,
        MapMap<Period, DimensionalItemObject, Object> aocData, Expression skipTest, OrganisationUnit orgUnit )
    {
        Set<Period> skippedPeriods = new HashSet<>();

        if ( skipTest == null || StringUtils.isEmpty( skipTest.getExpression() ) )
        {
            return skippedPeriods;
        }

        for ( Period p : allSamplePeriods )
        {
            if ( aocData.get( p ) != null &&
                (Boolean) expressionService.getExpressionValue( baseExParams.toBuilder()
                    .expression( skipTest.getExpression() )
                    .parseType( PREDICTOR_SKIP_TEST )
                    .valueMap( aocData.get( p ) )
                    .days( p.getDaysInPeriod() )
                    .missingValueStrategy( skipTest.getMissingValueStrategy() )
                    .orgUnit( orgUnit )
                    .build() ) )
            {
                skippedPeriods.add( p );
            }
        }

        return skippedPeriods;
    }

    /**
     * Returns all Periods of the specified PeriodType with start date after or
     * equal the specified start date and end date before or equal the specified
     * end date. Periods are returned in ascending date order.
     * <p>
     * The periods returned do not need to be in the database.
     */
    private List<Period> getPeriodsBetweenDates( PeriodType periodType, Date startDate, Date endDate )
    {
        List<Period> periods = new ArrayList<>();

        Period period = periodType.createPeriod( startDate );

        if ( !period.getStartDate().before( startDate ) && !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
        }

        period = periodType.getNextPeriod( period );

        while ( !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
            period = periodType.getNextPeriod( period );
        }

        return periods;
    }

    /**
     * Creates a map relating each output period to a list of sample periods
     * from which the sample data is to be drawn. Sample periods returned for
     * each output period are in order from older to newer, so any prediction
     * results can be brought forward if they are to be used in later period
     * predictions.
     */
    private ListMap<Period, Period> getSamplePeriodsMap( List<Period> outputPeriods, Predictor predictor )
    {
        int sequentialCount = predictor.getSequentialSampleCount();
        int annualCount = predictor.getAnnualSampleCount();
        int skipCount = firstNonNull( predictor.getSequentialSkipCount(), 0 );
        PeriodType periodType = predictor.getPeriodType();

        ListMap<Period, Period> samplePeriodsMap = new ListMap<>();

        for ( Period outputPeriod : outputPeriods )
        {
            samplePeriodsMap.put( outputPeriod, new ArrayList<>() );

            Period p = periodType.getPreviousPeriod( outputPeriod, skipCount );

            for ( int i = skipCount; i < sequentialCount; i++ )
            {
                p = periodType.getPreviousPeriod( p );

                samplePeriodsMap.putValue( outputPeriod, p );
            }

            for ( int year = 1; year <= annualCount; year++ )
            {
                Period pPrev = periodType.getPreviousYearsPeriod( outputPeriod, year );
                Period pNext = pPrev;

                samplePeriodsMap.putValue( outputPeriod, pPrev );

                for ( int i = 0; i < sequentialCount; i++ )
                {
                    pPrev = periodType.getPreviousPeriod( pPrev );
                    pNext = periodType.getNextPeriod( pNext );

                    samplePeriodsMap.putValue( outputPeriod, pPrev );
                    samplePeriodsMap.putValue( outputPeriod, pNext );
                }
            }
        }
        return samplePeriodsMap;
    }

    /**
     * Finds periods that exist in the DB, from a list of periods.
     */
    private Set<Period> getExistingPeriods( List<Period> periods )
    {
        Set<Period> existingPeriods = new HashSet<>();

        for ( Period period : periods )
        {
            Period existingPeriod = period.getId() != 0 ? period
                : periodService.getPeriod( period.getStartDate(), period.getEndDate(), period.getPeriodType() );

            if ( existingPeriod != null )
            {
                existingPeriods.add( existingPeriod );
            }
        }
        return existingPeriods;
    }

    /**
     * Does the expression reference the output data element? If so, the value
     * predicted in one period will be carried forward in case it is used in a
     * later period.
     */
    private boolean isForwardReference( Predictor predictor, Collection<DimensionalItemObject> items )
    {
        DataElement de = predictor.getOutput();

        return items.stream().anyMatch( i -> i.equals( de ) ||
            i instanceof DataElementOperand && ((DataElementOperand) i).getDataElement().equals( de ) );
    }

    /**
     * Remember the prediction for writing out.
     * <p>
     * If the predicted value might be used in a future period prediction,
     * insert it into all contexts. This means inserting an explicit data
     * element operand with the data element and COC of the data value. If the
     * context has the same COC we are using for the prediction, we also insert
     * a data element with the value.
     */
    private void rememberPredictedValue( DataValue dv, List<DataValue> predictions,
        List<PredictionContext> contexts, boolean forwardReference )
    {
        if ( dv == null )
        {
            return;
        }

        predictions.add( dv );

        if ( !forwardReference )
        {
            return;
        }

        DataElementOperand deo = new DataElementOperand( dv.getDataElement(), dv.getCategoryOptionCombo() );

        for ( PredictionContext c : contexts )
        {
            if ( c.getAttributeOptionCombo().equals( dv.getAttributeOptionCombo() ) )
            {
                c.getPeriodValueMap().putEntry( dv.getPeriod(), deo, dv.getValue() );

                if ( c.getCategoryOptionCombo().equals( dv.getCategoryOptionCombo() ) )
                {
                    c.getPeriodValueMap().putEntry( dv.getPeriod(), dv.getDataElement(), dv.getValue() );
                }
            }
        }
    }

    /**
     * Returns true if we are required to evaluate for a prediction. This
     * happens if data is required for a prediction, or if data is present.
     * <p>
     * This allows us to save time by evaluating an expression only if there is
     * data. (Expression evaluation can take a non-trivial amount of time.)
     */
    private boolean isEvaluationRequired( boolean requireData, ExpressionInfo exInfo, List<Period> samplePeriods,
        Map<DimensionalItemObject, Object> valueMap, MapMap<Period, DimensionalItemObject, Object> periodValueMap,
        Map<DimensionalItemId, DimensionalItemObject> itemMap )
    {
        if ( !requireData || presentIn( exInfo.getItemIds(), itemMap, valueMap ) )
        {
            return true;
        }

        for ( Period p : samplePeriods )
        {
            Map<DimensionalItemObject, Object> pValueMap = periodValueMap.get( p );

            if ( pValueMap != null && presentIn( exInfo.getSampleItemIds(), itemMap, pValueMap ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if any items are present in the value map.
     */
    private boolean presentIn( Set<DimensionalItemId> items, Map<DimensionalItemId, DimensionalItemObject> itemMap,
        Map<DimensionalItemObject, Object> valueMap )
    {
        for ( DimensionalItemId item : items )
        {
            if ( valueMap.keySet().contains( itemMap.get( item ) ) )
            {
                return true;
            }
        }
        return false;
    }
}
