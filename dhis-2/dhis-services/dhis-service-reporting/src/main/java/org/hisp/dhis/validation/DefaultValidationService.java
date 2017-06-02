package org.hisp.dhis.validation;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_ESCAPED_SEP;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;

/**
 * @author Jim Grace
 * @author Stian Sandvold
 */
@Transactional
public class DefaultValidationService
    implements ValidationService
{
    private static final Log log = LogFactory.getLog( DefaultValidationService.class );

    private static final ImmutableSet<DimensionItemType> EVENT_DIM_ITEM_TYPES = ImmutableSet.of(
        PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR );

    @Autowired
    private PeriodService periodService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ValidationNotificationService notificationService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private ApplicationContext applicationContext;

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public Collection<ValidationResult> startInteractiveValidationAnalysis( Date startDate, Date endDate,
        List<OrganisationUnit> orgUnits,
        DataElementCategoryOptionCombo attributeOptionCombo, ValidationRuleGroup group, boolean sendNotifications,
        I18nFormat format )
    {
        Clock clock = new Clock( log ).startClock().logTime( "Starting interactive validation run." );

        Collection<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );

        Collection<ValidationRule> rules =
            group != null ? group.getMembers() : validationRuleService.getAllValidationRules();

        ValidationRunContext context = getValidationContext( orgUnits, periods, rules )
            .withAttributeCombo( attributeOptionCombo )
            .withMaxResults( MAX_INTERACTIVE_ALERTS )
            .withSendNotifications( sendNotifications )
            .build();

        clock.logTime( "Initialized interactive validation run." );

        Collection<ValidationResult> validationResults = startValidationAnalysis( context );

        clock.logTime( "Finished interactive validation run." );

        return validationResults;
    }

    @Override
    public Collection<ValidationResult> startInteractiveValidationAnalysis( DataSet dataSet, Period period,
        OrganisationUnit orgUnit,
        DataElementCategoryOptionCombo attributeOptionCombo )
    {
        Collection<ValidationRule> rules = validationRuleService
            .getValidationRulesForDataElements( dataSet.getDataElements() );

        List<OrganisationUnit> orgUnits = Lists.newArrayList( orgUnit );

        Collection<Period> periods = Sets.newHashSet( period );

        ValidationRunContext context = getValidationContext( orgUnits, periods, rules )
            .withAttributeCombo( attributeOptionCombo )
            .withMaxResults( MAX_INTERACTIVE_ALERTS )
            .build();

        Collection<ValidationResult> validationResults = startValidationAnalysis( context );

        return validationResults;
    }

    @Override
    public void startScheduledValidationAnalysis()
    {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();

        // Find all rules which might generate notifications
        Set<ValidationRule> rules = getValidationRulesWithNotificationTemplates();

        Set<Period> periods = extractNotificationPeriods( rules );

        Clock clock = new Clock( log ).startClock().logTime( "Starting scheduled validation run, orgUnits: "
            + orgUnits.size() + ", periods: " + periods.size() + ", rules:" + rules.size() );

        // TODO: We are not actively using LAST_MONITORING_RUN anymore, remove when sure we don't need it.
        systemSettingManager.saveSystemSetting( SettingKey.LAST_MONITORING_RUN, new Date() );

        ValidationRunContext context = getValidationContext( orgUnits, periods, rules )
            .withPersistResults( true )
            .withMaxResults( MAX_SCHEDULED_ALERTS )
            .withSendNotifications( false )
            .build();

        clock.logTime( "Initialized scheduled validation run." );

        startValidationAnalysis( context );

        clock.logTime( "Finished scheduled validation run." );
    }

    private Collection<ValidationResult> startValidationAnalysis( ValidationRunContext context )
    {
        Collection<ValidationResult> results = Validator.validate( context, applicationContext );

        log.info( "Send Notifications: " + context.isSendNotifications() );
        log.info( "Violations: " + context.getValidationResults().size() );

        if ( context.isSendNotifications() )
        {
            notificationService.sendNotifications( Sets.newHashSet( results ) );
        }

        return results;
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( DataElementCategoryOptionCombo co : de.getCategoryOptionCombos() )
                {
                    DataValue dv = dataValueService
                        .getDataValue( de, period, organisationUnit, co, attributeOptionCombo );

                    boolean missingValue = dv == null || StringUtils.trimToNull( dv.getValue() ) == null;
                    boolean missingComment = dv == null || StringUtils.trimToNull( dv.getComment() ) == null;

                    if ( missingValue && missingComment )
                    {
                        violations.add( new DataElementOperand( de, co ) );
                    }
                }
            }
        }

        return violations;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<ValidationRule> getValidationRulesWithNotificationTemplates()
    {
        return Sets.newHashSet( validationRuleService.getValidationRulesWithNotificationTemplates() );
    }

    /**
     * Get the current and most recent periods to use when performing validation
     * for generating notifications (previously 'alerts'). The periods are
     * filtered against existing (persisted) periods.
     * <p>
     * TODO Consider:
     * This method assumes that the last successful validation run was one day ago.
     * If this is not the case (more than one day ago) adding additional (daily)
     * periods to 'fill the gap' could be considered.
     */
    private Set<Period> extractNotificationPeriods( Set<ValidationRule> rules )
    {
        return rules.stream()
            .map( rule -> periodService.getPeriodTypeByName( rule.getPeriodType().getName() ) )
            .map( periodType -> {
                Period current = periodType.createPeriod(), previous = periodType.getPreviousPeriod( current );
                Date start = previous.getStartDate(), end = current.getEndDate();

                return periodService.getIntersectingPeriodsByPeriodType( periodType, start, end );
            } )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
    }

    /**
     * Gets the event dimension item for the validation rules.
     *
     * @param dimensionItemMap map from UIDs to all dimension items.
     * @return Set with all event dimension items.
     */
    private Set<DimensionalItemObject> getEventItems( Map<String, DimensionalItemObject> dimensionItemMap )
    {
        return dimensionItemMap.values().stream()
            .filter( di -> EVENT_DIM_ITEM_TYPES.contains( di.getDimensionItemType() ) )
            .collect( Collectors.toSet() );
    }

    /**
     * Returns a new Builder with basic configuration based on the input parameters.
     *
     * @param orgUnits organisation units to include in analysis.
     * @param periods periods to include in analysis.
     * @param validationRules rules to include in analysis.
     * @return Builder with basic configuration based on input.
     */
    private ValidationRunContext.Builder getValidationContext( List<OrganisationUnit> orgUnits,
        Collection<Period> periods, Collection<ValidationRule> validationRules )
    {
        User currentUser = currentUserService.getCurrentUser();

        Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap = new HashMap<>();

        addPeriodsToContext( periodTypeExtendedMap, periods );
        Map<String, DimensionalItemObject> dimensionItemMap = addRulesToContext( periodTypeExtendedMap, validationRules );
        removeAnyUnneededPeriodTypes( periodTypeExtendedMap );
        addOrgUnitsToContext( periodTypeExtendedMap, orgUnits );

        ValidationRunContext.Builder builder = ValidationRunContext.newBuilder()
            .withPeriodTypeExtendedMap( periodTypeExtendedMap )
            .withOrgUnits( orgUnits )
            .withEventItems( getEventItems( dimensionItemMap ) )
            .withConstantMap( constantService.getConstantMap() );

        if ( currentUser != null )
        {
            builder
                .withCoDimensionConstraints(
                    categoryService.getCoDimensionConstraints( currentUser.getUserCredentials() ) )
                .withCogDimensionConstraints(
                    categoryService.getCogDimensionConstraints( currentUser.getUserCredentials() ) );
        }

        return builder;
    }

    /**
     * Adds Periods to the context, grouped by period type.
     *
     * @param periodTypeExtendedMap period type map to extended period types.
     * @param periods periods to group and add.
     */
    private void addPeriodsToContext( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( periodTypeExtendedMap,
                period.getPeriodType() );
            periodTypeX.getPeriods().add( period );
        }
    }

    /**
     * Adds validation rules to the context.
     *
     * @param periodTypeExtendedMap period type map to extended period types.
     * @param rules validation rules to add.
     */
    private Map<String, DimensionalItemObject> addRulesToContext( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<ValidationRule> rules )
    {
        // 1. Find all dimensional object IDs in the expressions of the validation rules.

        SetMap<Class<? extends DimensionalItemObject>, String> allItemIds = new SetMap<>();

        SetMap<ValidationRule, String> ruleItemIds = new SetMap<>();

        for ( ValidationRule rule : rules )
        {
            if ( periodTypeExtendedMap.get( rule.getPeriodType() ) == null )
            {
                continue; // Don't include rules for which there are no periods.
            }

            SetMap<Class<? extends DimensionalItemObject>, String> dimensionItemIdentifiers = expressionService.getDimensionalItemIdsInExpression( rule.getLeftSide().getExpression() );
            dimensionItemIdentifiers.putValues( expressionService.getDimensionalItemIdsInExpression( rule.getRightSide().getExpression() ) );

            Set<String> ruleIds = dimensionItemIdentifiers.values().stream().reduce( new HashSet<>(), ( x, y ) -> Sets.union( x, y ) );

            ruleItemIds.putValues( rule, ruleIds );

            allItemIds.putValues( dimensionItemIdentifiers );
        }

        // 2. Get the dimensional objects from the IDs. (Get them all at once for best performance.)

        Map<String, DimensionalItemObject> dimensionItemMap = getDimensionalItemObjects( allItemIds );

        // 3. Save the dimensional objects in the validation context.

        for ( ValidationRule rule : rules )
        {
            PeriodTypeExtended periodTypeX = periodTypeExtendedMap.get( rule.getPeriodType() );

            if ( periodTypeX == null )
            {
                continue;
            }

            ValidationRuleExtended ruleX = new ValidationRuleExtended( rule );

            Set<DimensionalItemObject> ruleDimensionItemObjects = ruleItemIds.get( rule ).stream()
                .map( id -> dimensionItemMap.get( id ) )
                .collect( Collectors.toSet() );

            if ( ruleDimensionItemObjects != null )
            {
                ruleX.setDimensionalItemObjects( ruleDimensionItemObjects );

                Set<DataElementOperand> ruleDataElementOperands = ruleDimensionItemObjects.stream()
                    .filter( o -> o != null && o.getDimensionItemType() == DimensionItemType.DATA_ELEMENT_OPERAND )
                    .map( o -> (DataElementOperand) o )
                    .collect( Collectors.toSet() );

                if ( ruleDataElementOperands != null )
                {
                    ruleX.setDataElementOperands( ruleDataElementOperands );

                    Set<DataElement> ruleDataElements = ruleDataElementOperands.stream()
                        .map( o -> o.getDataElement() )
                        .collect (Collectors.toSet() );

                    ruleX.setDataElements( ruleDataElements );
                }
            }

            periodTypeX.getRuleXs().add( ruleX );

            Set<DataElement> ruleDataElements = ruleX.getDataElements();

            // Add data elements of rule to the period extended
            periodTypeX.getDataElements().addAll( ruleDataElements );

            // Add the allowed period types for data elements of rule
            periodTypeX.getAllowedPeriodTypes().addAll(
                getAllowedPeriodTypesForDataElements( ruleDataElements, rule.getPeriodType() ) );
        }

        return dimensionItemMap;
    }

    /**
     * Gets all required DimensionalItemObjects from their UIDs.
     *
     * @param expressionIdMap UIDs of DimensionalItemObjects to get.
     * @return map of the DimensionalItemObjects.
     */
    private Map<String, DimensionalItemObject> getDimensionalItemObjects(
        SetMap<Class<? extends DimensionalItemObject>, String> expressionIdMap )
    {
        // 1. Get ids for all the individual IdentifiableObjects within the DimensionalItemObjects:

        SetMap<Class<? extends IdentifiableObject>, String> idsToGet = new SetMap<>();

        getIdentifiableObjectIds( idsToGet, expressionIdMap, DataElementOperand.class, DataElement.class, DataElementCategoryOptionCombo.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramDataElementDimensionItem.class, Program.class, DataElement.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramTrackedEntityAttributeDimensionItem.class, Program.class, TrackedEntityAttribute.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramIndicator.class, ProgramIndicator.class );

        // 2. Look up all the IdentifiableObjects (each class all together, for best performance):

        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> idMap = new MapMap<>();

        for ( Map.Entry<Class<? extends IdentifiableObject>, Set<String>> e : idsToGet.entrySet() )
        {
            idMap.putEntries( e.getKey(), idObjectManager.get( e.getKey(), e.getValue() ).stream().collect( Collectors.toMap( o -> o.getUid(), o -> o ) ) );
        }

        // 3. Build the map of DimensionalItemObjects:

        Map<String, DimensionalItemObject> dimObjects = new HashMap<>();

        for ( Map.Entry<Class<? extends DimensionalItemObject>, Set<String>> e : expressionIdMap.entrySet() )
        {
            for ( String id : e.getValue() )
            {
                if ( e.getKey() == DataElementOperand.class )
                {
                    DataElementOperand deo = new DataElementOperand( (DataElement)idMap.getValue( DataElement.class, getIdPart( id, 0 ) ),
                        (DataElementCategoryOptionCombo)idMap.getValue( DataElementCategoryOptionCombo.class, getIdPart( id, 1) ) );

                    if ( deo.getDataElement() != null && ( deo.getCategoryOptionCombo() != null || getIdPart( id, 1 ) == null ) )
                    {
                        dimObjects.put( id, deo );
                    }
                }
                else if ( e.getKey() == ProgramDataElementDimensionItem.class )
                {
                    ProgramDataElementDimensionItem pde = new ProgramDataElementDimensionItem( (Program)idMap.getValue( Program.class, getIdPart( id, 0 ) ),
                        (DataElement)idMap.getValue( DataElement.class, getIdPart( id, 1) ) );

                    if ( pde.getProgram() != null && pde.getDataElement() != null )
                    {
                        dimObjects.put( id, pde );
                    }
                }
                else if ( e.getKey() == ProgramTrackedEntityAttributeDimensionItem.class )
                {
                    ProgramTrackedEntityAttributeDimensionItem pa = new ProgramTrackedEntityAttributeDimensionItem( (Program)idMap.getValue( Program.class, getIdPart( id, 0 ) ),
                        (TrackedEntityAttribute)idMap.getValue( TrackedEntityAttribute.class, getIdPart( id, 1) ) );

                    if ( pa.getProgram() != null && pa.getAttribute() != null )
                    {
                        dimObjects.put( id, pa );
                    }
                }
                else if ( e.getKey() == ProgramIndicator.class )
                {
                    ProgramIndicator pi = (ProgramIndicator)idMap.getValue( ProgramIndicator.class, id );

                    if ( pi != null )
                    {
                        dimObjects.put( id, pi );
                    }
                }
            }
        }

        return dimObjects;
    }

    /**
     * Takes all the identifiers within a dimensional object class, and splits
     * them into identifiers for the identifiable objects that make up
     * the dimensional object.
     *
     * @param idsToGet To add to: identifiable object IDs to look up.
     * @param expressionIdMap Dimensional object IDs from expression.
     * @param dimClass Class of dimensional object
     * @param idClasses Component class(es) of identifiable objects
     */
    @SafeVarargs
    private final void getIdentifiableObjectIds( SetMap<Class<? extends IdentifiableObject>, String> idsToGet,
        SetMap<Class<? extends DimensionalItemObject>, String> expressionIdMap,
        Class<? extends DimensionalItemObject> dimClass,
        Class<? extends IdentifiableObject>... idClasses )
    {
        Set<String> expressionIds = expressionIdMap.get( dimClass );

        if ( expressionIds == null )
        {
            return;
        }

        for ( int i = 0; i < idClasses.length; i++ )
        {
            for ( String expressionId : expressionIds )
            {
                String objectId = getIdPart( expressionId, i );

                if ( objectId != null )
                {
                    idsToGet.putValue( idClasses[ i ], objectId );
                }
            }
        }
    }

    /**
     * Gets part of an object identifier which may be composite.
     *
     * @param id The identifier to parse.
     * @param index Index of the part to return.
     * @return The identifier part.
     */
    private String getIdPart( String id, int index )
    {
        return splitSafe( id, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, index );
    }

    /**
     * Removes any period types that don't have rules assigned to them.
     *
     * @param periodTypeExtendedMap period type map to extended period types.
     */
    private void removeAnyUnneededPeriodTypes( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap )
    {
        Set<PeriodTypeExtended> periodTypeXs = new HashSet<>( periodTypeExtendedMap.values() );

        for ( PeriodTypeExtended periodTypeX : periodTypeXs )
        {
            if ( periodTypeX.getRuleXs().isEmpty() )
            {
                periodTypeExtendedMap.remove( periodTypeX.getPeriodType() );
            }
        }
    }

    /**
     * Adds a collection of organisation units to the validation run context.
     *
     * @param periodTypeExtendedMap period type map to extended period types.
     * @param orgUnits              organisation units to add.
     */
    private void addOrgUnitsToContext(
        Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<OrganisationUnit> orgUnits )
    {
        for ( OrganisationUnit orgUnit : orgUnits )
        {
            Map<PeriodType, Set<DataElement>> orgUnitElementsMap = orgUnit.getDataElementsInDataSetsByPeriodType();

            for ( PeriodTypeExtended periodTypeX : periodTypeExtendedMap.values() )
            {
                periodTypeX.getOrgUnitDataElements().put( orgUnit, new HashSet<>() );

                for ( PeriodType allowedType : periodTypeX.getAllowedPeriodTypes() )
                {
                    Set<DataElement> orgUnitDataElements = orgUnitElementsMap.get( allowedType );

                    if ( orgUnitDataElements != null )
                    {
                        periodTypeX.getOrgUnitDataElements().get( orgUnit ).addAll( orgUnitDataElements );
                    }
                }
            }
        }
    }

    /**
     * Gets the PeriodTypeExtended from the context object. If not found,
     * creates a new PeriodTypeExtended object, puts it into the context object,
     * and returns it.
     *
     * @param periodTypeExtendedMap period type map to extended period types.
     * @param periodType period type to search for
     * @return period type extended from the context object
     */
    private PeriodTypeExtended getOrCreatePeriodTypeExtended( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        PeriodType periodType )
    {
        PeriodTypeExtended periodTypeX = periodTypeExtendedMap.get( periodType );

        if ( periodTypeX == null )
        {
            periodTypeX = new PeriodTypeExtended( periodType );
            periodTypeExtendedMap.put( periodType, periodTypeX );
        }

        return periodTypeX;
    }

    /**
     * Finds all period types that may contain given data elements, whose period
     * type interval is at least as long as the given period type.
     *
     * @param dataElements data elements to look for
     * @param periodType   the minimum-length period type
     * @return all period types that are allowed for these data elements
     */
    private static Set<PeriodType> getAllowedPeriodTypesForDataElements( Collection<DataElement> dataElements,
        PeriodType periodType )
    {
        Set<PeriodType> allowedPeriodTypes = new HashSet<>();

        if ( dataElements != null )
        {
            for ( DataElement dataElement : dataElements )
            {
                for ( DataSet dataSet : dataElement.getDataSets() )
                {
                    if ( dataSet.getPeriodType().getFrequencyOrder() >= periodType.getFrequencyOrder() )
                    {
                        allowedPeriodTypes.add( dataSet.getPeriodType() );
                    }
                }
            }
        }

        return allowedPeriodTypes;
    }
}
