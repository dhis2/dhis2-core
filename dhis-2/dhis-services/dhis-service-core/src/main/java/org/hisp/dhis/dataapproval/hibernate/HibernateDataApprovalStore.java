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
package org.hisp.dhis.dataapproval.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.dataapproval.DataApprovalState.ACCEPTED_HERE;
import static org.hisp.dhis.dataapproval.DataApprovalState.APPROVED_ABOVE;
import static org.hisp.dhis.dataapproval.DataApprovalState.APPROVED_HERE;
import static org.hisp.dhis.dataapproval.DataApprovalState.UNAPPROVABLE;
import static org.hisp.dhis.dataapproval.DataApprovalState.UNAPPROVED_ABOVE;
import static org.hisp.dhis.dataapproval.DataApprovalState.UNAPPROVED_READY;
import static org.hisp.dhis.dataapproval.DataApprovalState.UNAPPROVED_WAITING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalState;
import org.hisp.dhis.dataapproval.DataApprovalStatus;
import org.hisp.dhis.dataapproval.DataApprovalStore;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Jim Grace
 */
@Slf4j
@Repository( "org.hisp.dhis.dataapproval.DataApprovalStore" )
public class HibernateDataApprovalStore
    extends HibernateGenericStore<DataApproval>
    implements DataApprovalStore
{
    private static final int MAX_APPROVAL_LEVEL = 100000000;

    private static final String SQL_CONCAT = "-";

    private static final String SQL_CAT = StatementBuilder.QUOTE + SQL_CONCAT + StatementBuilder.QUOTE;

    private final Cache<Boolean> isApprovedCache;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final PeriodService periodService;

    private final PeriodStore periodStore;

    private final CurrentUserService currentUserService;

    private final CategoryService categoryService;

    private final SystemSettingManager systemSettingManager;

    private final StatementBuilder statementBuilder;

    private final OrganisationUnitService organisationUnitService;

    public HibernateDataApprovalStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CacheProvider cacheProvider, PeriodService periodService,
        PeriodStore periodStore, CurrentUserService currentUserService, CategoryService categoryService,
        SystemSettingManager systemSettingManager,
        StatementBuilder statementBuilder, OrganisationUnitService organisationUnitService )
    {
        super( sessionFactory, jdbcTemplate, publisher, DataApproval.class, false );

        checkNotNull( cacheProvider );
        checkNotNull( periodService );
        checkNotNull( periodStore );
        checkNotNull( currentUserService );
        checkNotNull( categoryService );
        checkNotNull( systemSettingManager );
        checkNotNull( statementBuilder );
        checkNotNull( organisationUnitService );

        this.periodService = periodService;
        this.periodStore = periodStore;
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
        this.systemSettingManager = systemSettingManager;
        this.statementBuilder = statementBuilder;
        this.isApprovedCache = cacheProvider.createIsDataApprovedCache();
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // DataApproval
    // -------------------------------------------------------------------------

    @Override
    public void addDataApproval( DataApproval dataApproval )
    {
        isApprovedCache.invalidateAll();

        dataApproval.setPeriod( periodService.reloadPeriod( dataApproval.getPeriod() ) );

        save( dataApproval );
    }

    @Override
    public void updateDataApproval( DataApproval dataApproval )
    {
        isApprovedCache.invalidateAll();

        dataApproval.setPeriod( periodService.reloadPeriod( dataApproval.getPeriod() ) );

        update( dataApproval );
    }

    @Override
    public void deleteDataApproval( DataApproval dataApproval )
    {
        isApprovedCache.invalidateAll();

        dataApproval.setPeriod( periodService.reloadPeriod( dataApproval.getPeriod() ) );

        delete( dataApproval );
    }

    @Override
    public void deleteDataApprovals( OrganisationUnit organisationUnit )
    {
        isApprovedCache.invalidateAll();

        String hql = "delete from DataApproval d where d.organisationUnit = :unit";

        getSession().createQuery( hql ).setParameter( "unit", organisationUnit ).executeUpdate();
    }

    @Override
    public DataApproval getDataApproval( DataApproval dataApproval )
    {
        return getDataApproval( dataApproval.getDataApprovalLevel(), dataApproval.getWorkflow(),
            dataApproval.getPeriod(), dataApproval.getOrganisationUnit(), dataApproval.getAttributeOptionCombo() );
    }

    @Override
    public DataApproval getDataApproval( DataApprovalLevel dataApprovalLevel, DataApprovalWorkflow workflow,
        Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
    {
        Period storedPeriod = periodService.reloadPeriod( period );

        CriteriaBuilder builder = getCriteriaBuilder();

        return getSingleResult( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "dataApprovalLevel" ), dataApprovalLevel ) )
            .addPredicate( root -> builder.equal( root.get( "workflow" ), workflow ) )
            .addPredicate( root -> builder.equal( root.get( "period" ), storedPeriod ) )
            .addPredicate( root -> builder.equal( root.get( "organisationUnit" ), organisationUnit ) )
            .addPredicate( root -> builder.equal( root.get( "attributeOptionCombo" ), attributeOptionCombo ) ) );
    }

    @Override
    public List<DataApproval> getDataApprovals( Collection<DataApprovalLevel> dataApprovalLevels,
        Collection<DataApprovalWorkflow> workflows,
        Collection<Period> periods, Collection<OrganisationUnit> organisationUnits,
        Collection<CategoryOptionCombo> attributeOptionCombos )
    {
        List<Period> storedPeriods = periods.stream().map( p -> periodService.reloadPeriod( p ) )
            .collect( Collectors.toList() );

        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> root.get( "dataApprovalLevel" ).in( dataApprovalLevels ) )
            .addPredicate( root -> root.get( "workflow" ).in( workflows ) )
            .addPredicate( root -> root.get( "period" ).in( storedPeriods ) )
            .addPredicate( root -> root.get( "organisationUnit" ).in( organisationUnits ) )
            .addPredicate( root -> root.get( "attributeOptionCombo" ).in( attributeOptionCombos ) ) );
    }

    @Override
    public boolean dataApprovalExists( DataApproval dataApproval )
    {
        return isApprovedCache.get( dataApproval.getCacheKey(), key -> dataApprovalExistsInternal( dataApproval ) );
    }

    private boolean dataApprovalExistsInternal( DataApproval dataApproval )
    {
        Period storedPeriod = periodStore.reloadPeriod( dataApproval.getPeriod() );

        if ( storedPeriod == null )
        {
            return false;
        }

        String sql = "select dataapprovalid " +
            "from dataapproval " +
            "where dataapprovallevelid = " + dataApproval.getDataApprovalLevel().getId() + " " +
            "and workflowid = " + dataApproval.getWorkflow().getId() + " " +
            "and periodid  = " + storedPeriod.getId() + " " +
            "and organisationunitid = " + dataApproval.getOrganisationUnit().getId() + " " +
            "and attributeoptioncomboid = " + dataApproval.getAttributeOptionCombo().getId() + " " +
            "limit 1";

        return jdbcTemplate.queryForList( sql ).size() > 0;
    }

    @Override
    public List<DataApprovalStatus> getDataApprovalStatuses( DataApprovalWorkflow workflow,
        Period period, Collection<OrganisationUnit> orgUnits, int orgUnitLevel, OrganisationUnit orgUnitFilter,
        CategoryCombo attributeCombo, Set<CategoryOptionCombo> attributeOptionCombos,
        List<DataApprovalLevel> userApprovalLevels, Map<Integer, DataApprovalLevel> levelMap )
    {
        // ---------------------------------------------------------------------
        // Get validation criteria
        // ---------------------------------------------------------------------

        final User user = currentUserService.getCurrentUser();
        final String strArrayUserGroups = CollectionUtils.isEmpty( user.getGroups() ) ? null
            : "{" + String.join( ",", user.getGroups().stream().map( group -> group.getUid() ).collect(
                Collectors.toList() ) ) + "}";
        final String co_group_sharing_check_query = strArrayUserGroups != null
            ? " and (not " + JsonbFunctions.HAS_USER_GROUP_IDS + "( co.sharing, '" + strArrayUserGroups + "') or not " +
                JsonbFunctions.CHECK_USER_GROUPS_ACCESS + "( co.sharing, '" + AclService.LIKE_READ_METADATA + "', '"
                + strArrayUserGroups + "') )"
            : "";

        List<DataApprovalLevel> approvalLevels = workflow.getSortedLevels();

        Set<OrganisationUnit> userOrgUnits = user.getDataViewOrganisationUnitsWithFallback();

        boolean isDefaultCombo = attributeOptionCombos != null && attributeOptionCombos.size() == 1
            && categoryService.getDefaultCategoryOptionCombo().equals( attributeOptionCombos.toArray()[0] );

        boolean maySeeDefaultCategoryCombo = (CollectionUtils
            .isEmpty( user.getCogsDimensionConstraints() )
            && CollectionUtils.isEmpty( user.getCatDimensionConstraints() ));

        // ---------------------------------------------------------------------
        // Validate
        // ---------------------------------------------------------------------

        if ( isDefaultCombo && !maySeeDefaultCategoryCombo )
        {
            log.warn( "DefaultCategoryCombo selected but user " + user.getUsername() + " lacks permission to see it." );

            return new ArrayList<>(); // Unapprovable.
        }

        if ( CollectionUtils.isEmpty( approvalLevels ) )
        {
            log.warn( "No approval levels configured for workflow " + workflow.getName() );

            return new ArrayList<>(); // Unapprovable.
        }

        if ( CollectionUtils.isEmpty( userApprovalLevels ) )
        {
            log.warn( "No user approval levels for user " + user.getUsername() + ", workflow " + workflow.getName() );

            return new ArrayList<>(); // Unapprovable.
        }

        if ( orgUnits != null )
        {
            for ( OrganisationUnit orgUnit : orgUnits )
            {
                if ( !organisationUnitService.isDescendant( orgUnit, userOrgUnits ) )
                {
                    log.debug( "User " + user.getUsername() + " can't see orgUnit " + orgUnit.getName() );

                    return new ArrayList<>(); // Unapprovable.
                }
            }
        }

        // ---------------------------------------------------------------------
        // Get other information
        // ---------------------------------------------------------------------

        boolean acceptanceRequiredForApproval = systemSettingManager
            .getBoolSetting( SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL );

        final boolean isSuperUser = currentUserService.currentUserIsSuper();

        final String startDate = DateUtils.getMediumDateString( period.getStartDate() );
        final String endDate = DateUtils.getMediumDateString( period.getEndDate() );

        DataApprovalLevel highestApprovalLevel = approvalLevels.get( 0 );
        DataApprovalLevel highestUserApprovalLevel = userApprovalLevels.get( 0 );

        DataApprovalLevel lowestApprovalLevelForOrgUnit = null;
        DataApprovalLevel approvalLevelAboveOrgUnit = null;
        DataApprovalLevel approvalLevelBelowOrgUnit = null;
        DataApprovalLevel approvalLevelAboveUser = null;

        if ( orgUnits == null )
        {
            orgUnitLevel = approvalLevels.get( approvalLevels.size() - 1 ).getOrgUnitLevel();
        }

        for ( DataApprovalLevel dal : approvalLevels )
        {
            int dalOrgUnitLevel = dal.getOrgUnitLevel();

            if ( dal.getLevel() < highestUserApprovalLevel.getLevel() )
            {
                approvalLevelAboveUser = dal;
            }

            if ( dalOrgUnitLevel < orgUnitLevel )
            {
                approvalLevelAboveOrgUnit = dal;
            }
            else if ( dal.getOrgUnitLevel() == orgUnitLevel )
            {
                lowestApprovalLevelForOrgUnit = dal;
            }
            else // dal.getOrgUnitLevel() > orgUnitLevel
            {
                approvalLevelBelowOrgUnit = dal;
                break;
            }
        }

        DataApprovalLevel approvedAboveLevel = null;

        if ( highestUserApprovalLevel.getLevel() != highestApprovalLevel.getLevel() &&
            (orgUnits == null || orgUnitLevel == highestUserApprovalLevel.getOrgUnitLevel()) )
        {
            approvedAboveLevel = approvalLevelAboveUser;
        }
        else if ( orgUnits != null && orgUnitLevel != highestUserApprovalLevel.getOrgUnitLevel() )
        {
            approvedAboveLevel = approvalLevelAboveOrgUnit;
        }

        log.debug( "Workflow '" + workflow.getName() + "' levels: " + approvalLevels.size() +
            ", user levels: " + userApprovalLevels.size() +
            ", lowestApprovalLevelForOrgUnit: "
            + (lowestApprovalLevelForOrgUnit == null ? "-" : lowestApprovalLevelForOrgUnit.getLevel()) +
            ", approvalLevelAboveOrgUnit: "
            + (approvalLevelAboveOrgUnit == null ? "-" : approvalLevelAboveOrgUnit.getLevel()) +
            ", approvalLevelBelowOrgUnit: "
            + (approvalLevelBelowOrgUnit == null ? "-" : approvalLevelBelowOrgUnit.getLevel()) +
            ", approvalLevelAboveUser: " + (approvalLevelAboveUser == null ? "-" : approvalLevelAboveUser.getLevel()) +
            ", approvedAboveLevel: " + (approvedAboveLevel == null ? "-" : approvedAboveLevel.getLevel()) );

        // ---------------------------------------------------------------------
        // Construct query
        // ---------------------------------------------------------------------

        String userOrgUnitRestrictions = "";

        if ( !isSuperUser && !userOrgUnits.isEmpty() )
        {
            for ( OrganisationUnit ou : userOrgUnits )
            {
                userOrgUnitRestrictions += (userOrgUnitRestrictions.length() == 0 ? " and ( " : " or ")
                    + statementBuilder.position( "'" + ou.getUid() + "'", "o.path" ) + " <> 0";
            }
            userOrgUnitRestrictions += " )";
        }

        String highestApprovedOrgUnitJoin = "";
        String highestApprovedOrgUnitCompare;
        String orgUnitIds = null;

        if ( orgUnits != null )
        {
            orgUnitIds = StringUtils.join( IdentifiableObjectUtils.getIdentifiers( orgUnits ), "," );

            highestApprovedOrgUnitCompare = "da.organisationunitid in (" + orgUnitIds + ") ";
        }
        else
        {
            highestApprovedOrgUnitJoin = "join organisationunit dao on dao.organisationunitid = da.organisationunitid ";

            highestApprovedOrgUnitCompare = statementBuilder.position( "dao.uid", "o.path" ) + " <> 0 ";
        }

        if ( orgUnitFilter != null )
        {
            orgUnitIds = String.valueOf( orgUnitFilter.getId() );
        }

        String userApprovalLevelRestrictions = "";

        if ( !isSuperUser && userApprovalLevels.size() != approvalLevels.size() )
        {
            for ( DataApprovalLevel dal : userApprovalLevels )
            {
                userApprovalLevelRestrictions += (userApprovalLevelRestrictions.length() == 0
                    ? "and dal.dataapprovallevelid in ( "
                    : ", ") + dal.getId() + " ";
            }
            userApprovalLevelRestrictions += ") ";
        }

        String coEndDateExtension = workflow.getSqlCoEndDateExtension();

        String approvedAboveSubquery = "false"; // Not approved above if this is
                                               // the highest (lowest number)
                                               // approval orgUnit level.

        if ( approvedAboveLevel != null )
        {
            approvedAboveSubquery = "exists ( " +
                "select 1 " +
                "from dataapproval da " +
                "join period p on p.periodid = da.periodid " +
                "join organisationunit dao on dao.organisationunitid = da.organisationunitid " +
                "where " + statementBuilder.position( "dao.uid", "o.path" ) + " = "
                + pathPositionAtLevel( approvedAboveLevel ) + " " +
                "and '" + endDate + "' >= p.startdate and '" + endDate + "' <= p.enddate " +
                "and da.dataapprovallevelid = " + approvedAboveLevel.getId() + " " +
                "and da.workflowid = " + workflow.getId() + " " +
                "and da.attributeoptioncomboid = coc.categoryoptioncomboid " +
                ")";
        }

        // Ready below if this is the lowest (highest number) approval level
        String readyBelowSubquery = "true";

        if ( approvalLevelBelowOrgUnit != null )
        {
            // Ready if nothing expected below is unapproved(/unaccepted)
            readyBelowSubquery = "not exists ( " +
                "select 1 " +
                // Lower Data Approval OrgUnit (DAO) where approval is required
                "from organisationunit dao " +
                "where " + statementBuilder.position( "o.uid", "dao.path" ) + " = "
                + pathPositionAtLevel( orgUnitLevel ) + " " +
                "and dao.hierarchylevel = " + approvalLevelBelowOrgUnit.getOrgUnitLevel() + " " +
                "and exists ( " + // Data for this workflow is collected somewhere at or below DAO
                "select 1 from organisationunit child " +
                "where " + statementBuilder.position( "dao.uid", "child.path" ) + " <> 0 " +
                "and child.organisationunitid in ( " +
                "select distinct sourceid " +
                "from datasetsource dss " +
                "join dataset ds on ds.datasetid = dss.datasetid " +
                "where ds.workflowid = " + workflow.getId() +
                ") " +
                ") " +
                (isDefaultCombo ? ""
                    : // Default combo options never have an organisation unit mapping.
                    "and not exists (" + // No AOCs without all attribute options valid for org unit.
                        "select 1 " +
                        "from categoryoptioncombos_categoryoptions cc1 " +
                        "where cc1.categoryoptioncomboid = coc.categoryoptioncomboid " +
                        "and ( " +
                        "exists ( " + // If there are orgUnit mappings...
                        "select 1 " +
                        "from categoryoption_organisationunits co1 " +
                        "where co1.categoryoptionid = cc1.categoryoptionid ) " +
                        "and not exists (" + // then one of them should map to this orgUnit.
                        "select 1 " +
                        "from categoryoption_organisationunits co1 " +
                        "join organisationunit o1 on o1.organisationunitid = co1.organisationunitid " +
                        "where co1.categoryoptionid = cc1.categoryoptionid " +
                        "and " + statementBuilder.position( "o1.uid", "dao.path" ) +
                        " between 2 and " + pathPositionAtLevel( approvalLevelBelowOrgUnit ) + " " +
                        ") " +
                        ") " +
                        ") ")
                +
                "and not exists (" + // Data not approved(/accepted) below where it needs to be if ready.
                "select 1 from dataapproval da " +
                "join period p on p.periodid = da.periodid " +
                "where da.organisationunitid = dao.organisationunitid " +
                "and da.dataapprovallevelid = " + approvalLevelBelowOrgUnit.getId() + " " +
                "and '" + endDate + "' >= p.startdate and '" + endDate + "' <= p.enddate " +
                "and da.workflowid = " + workflow.getId() + " " +
                "and da.attributeoptioncomboid = coc.categoryoptioncomboid " +
                (acceptanceRequiredForApproval ? "and da.accepted " : "") +
                ") " +
                ") ";
        }

        final String sql = "select coc.uid as cocuid, o.uid as ouuid, o.name as ouname, " +
            "(select min("
            + statementBuilder.concatenate( MAX_APPROVAL_LEVEL + " + dal.level", SQL_CAT, "da.accepted", SQL_CAT,
                "da.organisationunitid" )
            + ") " +
            "from dataapproval da " +
            "join dataapprovallevel dal on dal.dataapprovallevelid = da.dataapprovallevelid " +
            highestApprovedOrgUnitJoin +
            "where da.workflowid = " + workflow.getId() + " " +
            "and da.periodid = " + getWorkflowPeriodId( workflow, endDate ) + " " +
            "and da.attributeoptioncomboid = coc.categoryoptioncomboid " +
            "and " + highestApprovedOrgUnitCompare + userApprovalLevelRestrictions +
            ") as highest_approved, " +
            readyBelowSubquery + " as ready_below, " +
            approvedAboveSubquery + " as approved_above " +
            "from categoryoptioncombo coc " +
            "join organisationunit o on "
            + (orgUnitIds != null ? "o.organisationunitid in (" + orgUnitIds + ") "
                : "o.hierarchylevel = " + orgUnitLevel + userOrgUnitRestrictions + " ")
            +
            // Exclude any attribute option combo (COC) that is linked (1 to
            // many) to an unwanted attribute option (CO):
            "where not exists ( " +
            "select 1 " +
            "from categoryoptioncombos_categoryoptions cocco " +
            "join dataelementcategoryoption co on co.categoryoptionid = cocco.categoryoptionid " +
            "where cocco.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "and ( " +

            // CO start date too late
            "(co.startdate is not null and co.startdate > '" + endDate + "') " +

            // CO end date too early
            "or (co.enddate is not null and co.enddate" + coEndDateExtension + " < '" + startDate + "') " +

            "or ( " +
            "exists ( " + // This CO has orgunit mapping
            "select 1 " +
            "from categoryoption_organisationunits coo " +
            "where coo.categoryoptionid = co.categoryoptionid " +
            ") and not exists (" + // and not mapped to an orgunit we are looking for
            "select 1 " +
            "from categoryoption_organisationunits coo " +
            "join organisationunit o2 on o2.organisationunitid = coo.organisationunitid " +
            "where coo.categoryoptionid = co.categoryoptionid " +
            "and ( " +
            statementBuilder.position( "o.uid", "o2.path" ) + " <> 0  or " +
            statementBuilder.position( "o2.uid", "o.path" ) + " <> 0 " +
            ") " +
            ") " +
            ") " +
            (isSuperUser ? ""
                : // Filter out COs the user doesn't have permission to see:
                "or ( ( co.sharing->>'public' is null or left(co.sharing->>'public', 1) != 'r' )"
                    + " and ( co.sharing->>'owner' is null or co.sharing->>'owner' != '" + user.getUid() + "' )" +
                    " and ( not " + JsonbFunctions.HAS_USER_ID + "( co.sharing, '" + user.getUid() + "') or not " +
                    JsonbFunctions.CHECK_USER_ACCESS + "( co.sharing, '" + user.getUid() + "', '"
                    + AclService.LIKE_READ_METADATA + "') )"
                    + co_group_sharing_check_query + " )")
            +
            ") " +
            ") " +
            (attributeCombo == null ? ""
                : "and coc.categoryoptioncomboid in (select c9.categoryoptioncomboid from categorycombos_optioncombos c9 where c9.categorycomboid = "
                    + attributeCombo.getId() + " ) ")
            +
            (attributeOptionCombos == null || attributeOptionCombos.isEmpty() ? ""
                : "and coc.categoryoptioncomboid in (" +
                    StringUtils.join( IdentifiableObjectUtils.getIdentifiers( attributeOptionCombos ), "," ) + ") ")
            + // Filter AOCs if specified.
            "and exists ( " + // Include orgUnits, and their ancestors, that are
                             // mapped to a dataset of the workflow.
            "select 1 from organisationunit o3 " +
            "where o3.path like o.path || '%' and o3.organisationunitid in ( " +
            "select distinct sourceid " +
            "from datasetsource dss " +
            "join dataset ds on ds.datasetid = dss.datasetid " +
            "where ds.workflowid = " + workflow.getId() + ") " +
            ")";

        log.debug( "User " + user.getUsername() + " superuser " + isSuperUser
            + " workflow " + workflow.getName() + " period " + period.getIsoDate()
            + " orgUnits " + (orgUnits == null ? "null" : orgUnits)
            + " attributeCombo " + (attributeCombo == null ? "null" : attributeCombo.getName()) );

        log.debug( "Get approval SQL: " + sql );

        // ---------------------------------------------------------------------
        // Fetch query results and process them
        // ---------------------------------------------------------------------

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        List<DataApprovalStatus> statusList = new ArrayList<>();

        while ( rowSet.next() )
        {
            final String aocUid = rowSet.getString( 1 );
            final String ouUid = rowSet.getString( 2 );
            final String ouName = rowSet.getString( 3 );
            final String highestApproved = rowSet.getString( 4 );
            final boolean readyBelow = rowSet.getBoolean( 5 );
            boolean approvedAbove = rowSet.getBoolean( 6 );

            final String[] approved = highestApproved == null ? null : highestApproved.split( SQL_CONCAT );
            final int level = approved == null ? 0 : Integer.parseInt( approved[0] ) - MAX_APPROVAL_LEVEL;
            final boolean accepted = approved == null ? false : approved[1].substring( 0, 1 ).equalsIgnoreCase( "t" );
            final int approvedOrgUnitId = approved == null ? 0 : Integer.parseInt( approved[2] );

            // null if not approved
            DataApprovalLevel approvedLevel = (level == 0 ? null : levelMap.get( level ));
            DataApprovalLevel actionLevel = (approvedLevel == null ? lowestApprovalLevelForOrgUnit : approvedLevel);

            if ( approvedAbove && accepted && acceptanceRequiredForApproval
                && approvedAboveLevel == approvalLevelAboveUser )
            {
                approvedAbove = false; // Hide higher-level approval from user.
            }

            if ( ouUid != null )
            {
                DataApprovalState state = (approvedAbove ? APPROVED_ABOVE
                    : approvedLevel == null
                        ? lowestApprovalLevelForOrgUnit == null
                            ? approvalLevelAboveOrgUnit == null ? UNAPPROVABLE : UNAPPROVED_ABOVE
                            : readyBelow ? UNAPPROVED_READY : UNAPPROVED_WAITING
                        : accepted ? ACCEPTED_HERE : APPROVED_HERE);

                statusList.add( DataApprovalStatus.builder()
                    .state( state )
                    .approvedLevel( approvedLevel )
                    .approvedOrgUnitId( approvedOrgUnitId )
                    .actionLevel( actionLevel )
                    .organisationUnitUid( ouUid )
                    .organisationUnitName( ouName )
                    .attributeOptionComboUid( aocUid )
                    .accepted( accepted )
                    .build() );
            }
        }

        return statusList;
    }

    /**
     * Get the id for the workflow period that spans the given end date. The
     * workflow period may or may not be the same as the period for which we are
     * checking data validity. The workflow period will have a period type that
     * matches the workflow period type, and it will contain the end date of the
     * period for which we are checking data validity.
     *
     * Returns zero if there is no such workflow period.
     *
     * It turns out that this is much faster done as a separate query in
     * postgresql than imbedding this as a subquery in the larger query above.
     *
     * @param workflow workflow we are checking
     * @param endDate end date of the period we are checking approval for,
     *        formatted as a string for a SQL query.
     * @return id of the workflow period which overlaps with the endDate
     */
    private int getWorkflowPeriodId( DataApprovalWorkflow workflow, String endDate )
    {
        final String sql = "select periodid from period where '" + endDate + "' >= startdate and '" + endDate
            + "' <= enddate and periodtypeid = " + workflow.getPeriodType().getId();

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        if ( rowSet.next() )
        {
            return rowSet.getInt( 1 );
        }

        return 0;
    }

    // TODO: Should we move these two methods to static methods in
    // OrganisationUnit?
    /**
     * Returns the position within an orgUnit path at which the orgUnit UID will
     * be found for a given orgUnitLevel.
     *
     * @param orgUnitLevel organization unit level.
     * @return position within path for this org unit level.
     */
    private int pathPositionAtLevel( int orgUnitLevel )
    {
        return (orgUnitLevel - 1) * 12 + 2;
    }

    /**
     * Returns the position within an orgUnit path at which the orgUnit UID will
     * be found for a given data approval level.
     *
     * @param level data approval level.
     * @return position within path for this org unit level.
     */
    private int pathPositionAtLevel( DataApprovalLevel level )
    {
        return pathPositionAtLevel( level.getOrgUnitLevel() );
    }
}
