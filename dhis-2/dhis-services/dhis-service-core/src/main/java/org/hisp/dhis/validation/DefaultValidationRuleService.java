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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hisp.dhis.commons.util.TextUtils.LN;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Transactional
public class DefaultValidationRuleService
    implements ValidationRuleService
{
    private static final Log log = LogFactory.getLog( DefaultValidationRuleService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleStore validationRuleStore;

    public void setValidationRuleStore( ValidationRuleStore validationRuleStore )
    {
        this.validationRuleStore = validationRuleStore;
    }

    private GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore;

    public void setValidationRuleGroupStore( GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore )
    {
        this.validationRuleGroupStore = validationRuleGroupStore;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private ConstantService constantService;

    public void setConstantService( ConstantService constantService )
    {
        this.constantService = constantService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }
    
    @Autowired
    private ApplicationContext applicationContext;

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public Collection<ValidationResult> validate( Date startDate, Date endDate, Collection<OrganisationUnit> sources,
        DataElementCategoryOptionCombo attributeCombo, ValidationRuleGroup group, boolean sendAlerts, I18nFormat format )
    {
        log.info( "Validate start:" + startDate + " end: " + endDate + " sources: " + sources.size() + " group: " + group );

        List<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );
        Collection<ValidationRule> rules = group != null ? group.getMembers() : getAllValidationRules();

        User user = currentUserService.getCurrentUser();
        
        Collection<ValidationResult> results = Validator.validate( ValidationRunContext.getNewContext( 
            sources, periods, rules, attributeCombo, 
            null, ValidationRunType.SCHEDULED, constantService.getConstantMap(), 
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ),
            false ), applicationContext );

        formatPeriods( results, format );

        if ( sendAlerts )
        {
            Set<ValidationResult> resultsToAlert = new HashSet<>( results );
            FilterUtils.filter( resultsToAlert, new ValidationResultToAlertFilter() );
            postAlerts( resultsToAlert, new Date() );
        }

        return results;
    }

    @Override
    public Collection<ValidationResult> validate( Date startDate, Date endDate, OrganisationUnit source )
    {
        log.info( "Validate start: " + startDate + " end: " + endDate + " source: " + source.getName() );

        Collection<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );
        Collection<ValidationRule> rules = getAllValidationRules();
        Collection<OrganisationUnit> sources = new HashSet<>();
        sources.add( source );

        User user = currentUserService.getCurrentUser();
        
        return Validator.validate( ValidationRunContext.getNewContext( 
            sources, periods, rules, null, null,
            ValidationRunType.SCHEDULED, constantService.getConstantMap(), 
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );
    }

    @Override
    public Collection<ValidationResult> validate( DataSet dataSet, Period period, OrganisationUnit source,
        DataElementCategoryOptionCombo attributeCombo )
    {
        log.debug( "Validate data set: " + dataSet.getName() + " period: " + period.getPeriodType().getName() + " "
            + period.getStartDate() + " " + period.getEndDate() + " source: " + source.getName()
            + " attribute combo: " + ( attributeCombo == null ? "[none]" : attributeCombo.getName() ) );

        Collection<Period> periods = new ArrayList<>();
        periods.add( period );

        Collection<ValidationRule> rules = getValidationTypeRulesForDataElements( dataSet.getDataElements() );

        log.debug( "Using validation rules: " + rules.size() );

        Collection<OrganisationUnit> sources = new HashSet<>();
        sources.add( source );

        User user = currentUserService.getCurrentUser();
        
        return Validator.validate( ValidationRunContext.getNewContext( 
            sources, periods, rules, attributeCombo, null,
            ValidationRunType.SCHEDULED, constantService.getConstantMap(), 
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );
    }

    @Override
    public void scheduledRun()
    {
        log.info( "Starting scheduled monitoring task" );

        // Find all the rules belonging to groups that will send alerts to user roles.

        Set<ValidationRule> rules = getAlertRules();

        Collection<OrganisationUnit> sources = organisationUnitService.getAllOrganisationUnits();

        Set<Period> periods = getAlertPeriodsFromRules( rules );

        Date lastScheduledRun = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_MONITORING_RUN );

        // Any database changes after this moment will contribute to the next run.

        Date thisRun = new Date();

        log.info( "Scheduled monitoring run sources: " + sources.size() + ", periods: " + periods.size() + ", rules:" + rules.size()
            + ", last run: " + (lastScheduledRun == null ? "[none]" : lastScheduledRun) );

        User user = currentUserService.getCurrentUser();
        
        Collection<ValidationResult> results = Validator.validate( ValidationRunContext.getNewContext( 
            sources, periods, rules, null, lastScheduledRun,
            ValidationRunType.SCHEDULED, constantService.getConstantMap(), 
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );

        log.info( "Validation run result count: " + results.size() );

        if ( !results.isEmpty() )
        {
            postAlerts( results, thisRun );
        }

        log.info( "Posted alerts, monitoring task done" );

        systemSettingManager.saveSystemSetting( SettingKey.LAST_MONITORING_RUN, thisRun );
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period, OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( DataElementCategoryOptionCombo co : de.getCategoryOptionCombos() )
                {
                    DataValue dv = dataValueService.getDataValue( de, period, organisationUnit, co, attributeOptionCombo );

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

    @Override
    public Collection<ValidationRule> getValidationTypeRulesForDataElements( Set<DataElement> dataElements )
    {
        Set<ValidationRule> rulesForDataElements = new HashSet<>();

        for ( ValidationRule validationRule : getAllValidationRules() )
        {
            Set<DataElement> validationRuleElements = new HashSet<>();
                validationRuleElements.addAll( validationRule.getLeftSide().getDataElementsInExpression() );
                validationRuleElements.addAll( validationRule.getRightSide().getDataElementsInExpression() );

                if ( dataElements.containsAll( validationRuleElements ) )
                {
                    rulesForDataElements.add( validationRule );
                }
        }

        return rulesForDataElements;
    }

    // -------------------------------------------------------------------------
    // Supportive methods - scheduled run
    // -------------------------------------------------------------------------

    /**
     * Gets all the validation rules that could generate alerts.
     *
     * @return rules that will generate alerts
     */
    private Set<ValidationRule> getAlertRules()
    {
        Set<ValidationRule> rules = new HashSet<>();

        for ( ValidationRuleGroup validationRuleGroup : getAllValidationRuleGroups() )
        {
            if ( validationRuleGroup.hasUserGroupsToAlert() )
            {
                rules.addAll( validationRuleGroup.getMembers() );
            }
        }

        return rules;
    }

    /**
     * Gets the current and most recent periods to search, based on
     * the period types from the rules to run.
     * <p>
     * For each period type, return the period containing the current date
     * (if any), and the most recent previous period. Add whichever of
     * these periods actually exist in the database.
     * <p>
     * TODO If the last successful daily run was more than one day ago, we might
     * add some additional periods of type DailyPeriodType not to miss any
     * alerts.
     *
     * @param rules the ValidationRules to be evaluated on this run
     * @return periods to search for new alerts
     */
    private Set<Period> getAlertPeriodsFromRules( Set<ValidationRule> rules )
    {
        Set<Period> periods = new HashSet<>();

        Set<PeriodType> rulePeriodTypes = getPeriodTypesFromRules( rules );

        for ( PeriodType periodType : rulePeriodTypes )
        {
            Period currentPeriod = periodType.createPeriod();
            Period previousPeriod = periodType.getPreviousPeriod( currentPeriod );
            periods.addAll( periodService.getIntersectingPeriodsByPeriodType( periodType,
                previousPeriod.getStartDate(), currentPeriod.getEndDate() ) );
        }

        return periods;
    }

    /**
     * At the end of a scheduled monitoring run, post messages to the users who
     * want to see the results.
     * <p>
     * Create one message for each set of users who receive the same
     * subset of results. (Not necessarily the same as the set of users who
     * receive alerts from the same subset of validation rules -- because
     * some of these rules may return no results.) This saves on message
     * storage space.
     * <p>
     * The message results are sorted into their natural order.
     * <p>
     * TODO: Internationalize the messages according to the user's
     * preferred language, and generate a message for each combination of
     * ( target language, set of results ).
     *
     * @param validationResults the set of validation error results
     * @param scheduledRunStart the date/time when this scheduled run started
     */
    private void postAlerts( Collection<ValidationResult> validationResults, Date scheduledRunStart )
    {
        SortedSet<ValidationResult> results = new TreeSet<>( validationResults );

        Map<SortedSet<ValidationResult>, Set<User>> messageMap = getMessageMap( results );

        for ( Map.Entry<SortedSet<ValidationResult>, Set<User>> entry : messageMap.entrySet() )
        {
            sendAlertmessage( entry.getKey(), entry.getValue(), scheduledRunStart );
        }
    }

    /**
     * Gets the Set of period types found in a set of rules.
     * <p>
     * Note that that we have to get periodType from periodService,
     * otherwise the ID will not be present.)
     *
     * @param rules validation rules of interest
     * @return period types contained in those rules
     */
    private Set<PeriodType> getPeriodTypesFromRules( Collection<ValidationRule> rules )
    {
        Set<PeriodType> rulePeriodTypes = new HashSet<>();

        for ( ValidationRule rule : rules )
        {
            rulePeriodTypes.add( periodService.getPeriodTypeByName( rule.getPeriodType().getName() ) );
        }

        return rulePeriodTypes;
    }

    /**
     * Returns a map where the key is a sorted list of validation results
     * to assemble into a message, and the value is the set of users who
     * should receive this message.
     *
     * @param results all the validation run results, in a sorted set
     * @return map of result sets to users
     */
    private Map<SortedSet<ValidationResult>, Set<User>> getMessageMap( SortedSet<ValidationResult> results )
    {
        Map<User, SortedSet<ValidationResult>> userResults = getUserResults( results );

        Map<SortedSet<ValidationResult>, Set<User>> messageMap = new HashMap<>();

        for ( Map.Entry<User, SortedSet<ValidationResult>> userResultEntry : userResults.entrySet() )
        {
            Set<User> users = messageMap.get( userResultEntry.getValue() );

            if ( users == null )
            {
                users = new HashSet<>();

                messageMap.put( userResultEntry.getValue(), users );
            }

            users.add( userResultEntry.getKey() );
        }

        return messageMap;
    }

    /**
     * Returns a map where the key is a user and the value is a naturally-sorted
     * list of results they should receive.
     *
     * @param results all the validation run results, in a sorted set
     * @return map of users to results
     */
    private Map<User, SortedSet<ValidationResult>> getUserResults( SortedSet<ValidationResult> results )
    {
        Map<User, SortedSet<ValidationResult>> userResults = new HashMap<>();

        for ( ValidationResult result : results )
        {
            for ( ValidationRuleGroup ruleGroup : result.getValidationRule().getGroups() )
            {
                if ( ruleGroup.hasUserGroupsToAlert() )
                {
                    for ( UserGroup userGroup : ruleGroup.getUserGroupsToAlert() )
                    {
                        for ( User user : userGroup.getMembers() )
                        {
                            if ( !ruleGroup.isAlertByOrgUnits() || canUserAccessSource( user, result.getOrgUnit() ) )
                            {
                                SortedSet<ValidationResult> resultSet = userResults.get( user );

                                if ( resultSet == null )
                                {
                                    resultSet = new TreeSet<>();

                                    userResults.put( user, resultSet );
                                }

                                resultSet.add( result );
                            }
                        }
                    }
                }
            }
        }
        return userResults;
    }

    /**
     * Determines whether a user can access an organisation unit,
     * based on the organisation units to which the user has been assigned.
     *
     * @param user   user to test
     * @param source organisation unit to which the user may have access
     * @return whether the user has acceess to the organisation unit
     */
    private boolean canUserAccessSource( User user, OrganisationUnit source )
    {
        for ( OrganisationUnit o : user.getOrganisationUnits() )
        {
            if ( source == o || source.getAncestors().contains( o ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate and send an alert message containing a list of validation
     * results to a set of users.
     *
     * @param results           results to put in this message
     * @param users             users to receive these results
     * @param scheduledRunStart date/time when the scheduled run started
     */
    private void sendAlertmessage( SortedSet<ValidationResult> results, Set<User> users, Date scheduledRunStart )
    {
        StringBuilder builder = new StringBuilder();

        Map<Importance, Integer> importanceCountMap = countResultsByImportanceType( results );

        String subject = "Alerts as of " + DateUtils.getLongDateString( scheduledRunStart ) + ": High "
            + (importanceCountMap.get( Importance.HIGH ) == null ? 0 : importanceCountMap.get( Importance.HIGH )) + ", Medium "
            + (importanceCountMap.get( Importance.MEDIUM ) == null ? 0 : importanceCountMap.get( Importance.MEDIUM )) + ", Low "
            + (importanceCountMap.get( Importance.LOW ) == null ? 0 : importanceCountMap.get( Importance.LOW ));

        //TODO use velocity template for message

        for ( ValidationResult result : results )
        {
            ValidationRule rule = result.getValidationRule();

            builder.append( result.getOrgUnit().getName() ).append( " " ).append( result.getPeriod().getName() ).
                append( result.getAttributeOptionCombo().isDefault() ? "" : " " + result.getAttributeOptionCombo().getName() ).append( LN ).
                append( rule.getName() ).append( " (" ).append( rule.getImportance() ).append( ") " ).append( LN ).
                append( rule.getLeftSide().getDescription() ).append( ": " ).append( result.getLeftsideValue() ).append( LN ).
                append( rule.getRightSide().getDescription() ).append( ": " ).append( result.getRightsideValue() ).append( LN ).append( LN );
        }

        log.info( "Alerting users: " + users.size() + ", subject: " + subject );

        messageService.sendMessage( subject, builder.toString(), null, users );
    }

    // -------------------------------------------------------------------------
    // Supportive methods - monitoring
    // -------------------------------------------------------------------------

    /**
     * Counts the results of each importance type, for all the importance
     * types that are found within the results.
     *
     * @param results results to analyze
     * @return Mapping between importance type and result counts.
     */
    private Map<Importance, Integer> countResultsByImportanceType( Set<ValidationResult> results )
    {
        Map<Importance, Integer> importanceCountMap = new HashMap<>();

        for ( ValidationResult result : results )
        {
            Integer importanceCount = importanceCountMap.get( result.getValidationRule().getImportance() );

            importanceCountMap.put( result.getValidationRule().getImportance(), importanceCount == null ? 1 : importanceCount + 1 );
        }

        return importanceCountMap;
    }

    /**
     * Formats and sets name on the period of each result.
     *
     * @param results the collection of validation results.
     * @param format  the i18n format.
     */
    private void formatPeriods( Collection<ValidationResult> results, I18nFormat format )
    {
        if ( format != null )
        {
            for ( ValidationResult result : results )
            {
                if ( result != null && result.getPeriod() != null )
                {
                    result.getPeriod().setName( format.formatPeriod( result.getPeriod() ) );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ValidationRule CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int saveValidationRule( ValidationRule validationRule )
    {
        return validationRuleStore.save( validationRule );
    }

    @Override
    public void updateValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.update( validationRule );
    }

    @Override
    public void deleteValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.delete( validationRule );
    }

    @Override
    public ValidationRule getValidationRule( int id )
    {
        return validationRuleStore.get( id );
    }

    @Override
    public ValidationRule getValidationRule( String uid )
    {
        return validationRuleStore.getByUid( uid );
    }

    @Override
    public ValidationRule getValidationRuleByName( String name )
    {
        return validationRuleStore.getByName( name );
    }

    @Override
    public List<ValidationRule> getAllValidationRules()
    {
        return validationRuleStore.getAll();
    }

    @Override
    public List<ValidationRule> getValidationRulesByDataElements( Collection<DataElement> dataElements )
    {
        return validationRuleStore.getValidationRulesByDataElements( dataElements );
    }

    @Override
    public int getValidationRuleCount()
    {
        return validationRuleStore.getCount();
    }

    @Override
    public int getValidationRuleCountByName( String name )
    {
        return validationRuleStore.getCountLikeName( name );
    }

    @Override
    public List<ValidationRule> getValidationRulesBetween( int first, int max )
    {
        return validationRuleStore.getAllOrderedName( first, max ) ;
    }

    @Override
    public List<ValidationRule> getValidationRulesBetweenByName( String name, int first, int max )
    {
        return validationRuleStore.getAllLikeName( name, first, max ) ;
    }

    // -------------------------------------------------------------------------
    // ValidationRuleGroup CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int addValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        return validationRuleGroupStore.save( validationRuleGroup );
    }

    @Override
    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.delete( validationRuleGroup );
    }

    @Override
    public void updateValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.update( validationRuleGroup );
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroup( int id )
    {
        return validationRuleGroupStore.get( id );
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroup( String uid )
    {
        return validationRuleGroupStore.getByUid( uid );
    }

    @Override
    public List<ValidationRuleGroup> getAllValidationRuleGroups()
    {
        return validationRuleGroupStore.getAll();
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroupByName( String name )
    {
        return validationRuleGroupStore.getByName( name );
    }

    @Override
    public int getValidationRuleGroupCount()
    {
        return validationRuleGroupStore.getCount();
    }

    @Override
    public int getValidationRuleGroupCountByName( String name )
    {
        return validationRuleGroupStore.getCountLikeName( name ) ;
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetween( int first, int max )
    {
        return validationRuleGroupStore.getAllOrderedName( first, max );
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetweenByName( String name, int first, int max )
    {
        return validationRuleGroupStore.getAllLikeName( name, first, max ) ;
    }
}
