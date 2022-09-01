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
package org.hisp.dhis.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.association.jdbc.JdbcOrgUnitAssociationsStore;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.dataset.DataSetService" )
public class DefaultDataSetService
    implements DataSetService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final DataSetStore dataSetStore;

    private final LockExceptionStore lockExceptionStore;

    private final DataApprovalService dataApprovalService;

    @Qualifier( "jdbcDataSetOrgUnitAssociationsStore" )
    private final JdbcOrgUnitAssociationsStore jdbcOrgUnitAssociationsStore;

    private final CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addDataSet( DataSet dataSet )
    {
        dataSetStore.save( dataSet );
        return dataSet.getId();
    }

    @Override
    @Transactional
    public void updateDataSet( DataSet dataSet )
    {
        dataSetStore.update( dataSet );
    }

    @Override
    @Transactional
    public void deleteDataSet( DataSet dataSet )
    {
        dataSetStore.delete( dataSet );
    }

    @Override
    @Transactional( readOnly = true )
    public DataSet getDataSet( long id )
    {
        return dataSetStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public DataSet getDataSet( String uid )
    {
        return dataSetStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public DataSet getDataSetNoAcl( String uid )
    {
        return dataSetStore.getByUidNoAcl( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getDataSetsByDataEntryForm( DataEntryForm dataEntryForm )
    {
        return dataSetStore.getDataSetsByDataEntryForm( dataEntryForm );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getAllDataSets()
    {
        return dataSetStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getDataSetsByPeriodType( PeriodType periodType )
    {
        return dataSetStore.getDataSetsByPeriodType( periodType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getUserDataRead( User user )
    {
        if ( user == null )
        {
            return Lists.newArrayList();
        }

        return user.isSuper() ? getAllDataSets() : dataSetStore.getDataReadAll( user );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getUserDataWrite( User user )
    {
        if ( user == null )
        {
            return Lists.newArrayList();
        }

        return user.isSuper() ? getAllDataSets() : dataSetStore.getDataWriteAll( user );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        return dataSetStore.getDataSetsNotAssignedToOrganisationUnits();
    }

    // -------------------------------------------------------------------------
    // DataSet LockExceptions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addLockException( LockException lockException )
    {
        lockExceptionStore.save( lockException );
        return lockException.getId();
    }

    @Override
    @Transactional
    public void updateLockException( LockException lockException )
    {
        lockExceptionStore.update( lockException );
    }

    @Override
    @Transactional
    public void deleteLockException( LockException lockException )
    {
        lockExceptionStore.delete( lockException );
    }

    @Override
    @Transactional( readOnly = true )
    public LockException getLockException( long id )
    {
        return lockExceptionStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public int getLockExceptionCount()
    {
        return lockExceptionStore.getCount();
    }

    @Override
    @Transactional( readOnly = true )
    public List<LockException> getAllLockExceptions()
    {
        return lockExceptionStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<LockException> getDataWriteLockExceptions()
    {
        return lockExceptionStore.getLockExceptions( dataSetStore.getDataWriteAll() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<LockException> getLockExceptionCombinations()
    {
        return lockExceptionStore.getLockExceptionCombinations();
    }

    @Override
    @Transactional( readOnly = true )
    public LockStatus getLockStatus( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, Date now )
    {
        if ( dataApprovalService.isApproved( dataSet.getWorkflow(), period, organisationUnit, attributeOptionCombo ) )
        {
            return LockStatus.APPROVED;
        }

        if ( isLocked( user, dataSet, period, organisationUnit, now ) )
        {
            return LockStatus.LOCKED;
        }

        return LockStatus.OPEN;
    }

    @Override
    @Transactional( readOnly = true )
    public LockStatus getLockStatus( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, Date now, boolean useOrgUnitChildren )
    {
        if ( !useOrgUnitChildren )
        {
            return getLockStatus( user, dataSet, period, organisationUnit, attributeOptionCombo, now );
        }

        if ( organisationUnit == null || !organisationUnit.hasChild() )
        {
            return LockStatus.OPEN;
        }

        for ( OrganisationUnit child : organisationUnit.getChildren() )
        {
            LockStatus childLockStatus = getLockStatus( user, dataSet, period, child, attributeOptionCombo, now );
            if ( !childLockStatus.isOpen() )
            {
                return childLockStatus;
            }
        }

        return LockStatus.OPEN;
    }

    @Override
    @Transactional( readOnly = true )
    public LockStatus getLockStatus( User user, DataElement dataElement, Period period,
        OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, Date now )
    {
        if ( user == null || !user.isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            now = now != null ? now : new Date();

            boolean expired = dataElement.isExpired( period, now );

            if ( expired && lockExceptionStore.getCount( dataElement, period, organisationUnit ) == 0L )
            {
                return LockStatus.LOCKED;
            }
        }

        DataSet dataSet = dataElement.getApprovalDataSet();

        if ( dataSet == null )
        {
            return LockStatus.OPEN;
        }

        if ( dataApprovalService.isApproved( dataSet.getWorkflow(), period, organisationUnit, attributeOptionCombo ) )
        {
            return LockStatus.APPROVED;
        }

        return LockStatus.OPEN;
    }

    @Override
    @Transactional
    public void deleteLockExceptionCombination( DataSet dataSet, Period period )
    {
        lockExceptionStore.deleteLockExceptions( dataSet, period );
    }

    @Override
    @Transactional
    public void deleteLockExceptionCombination( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        lockExceptionStore.deleteLockExceptions( dataSet, period, organisationUnit );
    }

    @Override
    @Transactional
    public void deleteLockExceptions( OrganisationUnit organisationUnit )
    {
        lockExceptionStore.deleteLockExceptions( organisationUnit );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, Date now )
    {
        return dataSet.isLocked( user, period, now )
            && lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L;
    }

    @Override
    @Transactional
    public List<LockException> filterLockExceptions( List<String> filters )
    {
        List<LockException> lockExceptions = getAllLockExceptions();
        Set<LockException> returnList = new HashSet<>( lockExceptions );

        for ( String filter : filters )
        {
            String[] split = filter.split( ":" );

            if ( split.length != 3 )
            {
                throw new QueryParserException( "Invalid filter: " + filter );
            }

            if ( "organisationUnit.id".equalsIgnoreCase( split[0] ) )
            {
                returnList.retainAll( getLockExceptionByOrganisationUnit( split[1], split[2], returnList ) );
            }

            if ( "dataSet.id".equalsIgnoreCase( split[0] ) )
            {
                returnList.retainAll( getLockExceptionByDataSet( split[1], split[2], returnList ) );
            }

            if ( "period".equalsIgnoreCase( split[0] ) )
            {
                returnList.retainAll( getLockExceptionByPeriod( split[1], split[2], returnList ) );
            }
        }

        return new ArrayList<>( returnList );
    }

    @Override
    @Transactional( readOnly = true )
    public SetValuedMap<String, String> getDataSetOrganisationUnitsAssociations()
    {
        Set<String> uids = getUserDataWrite( currentUserService.getCurrentUser() ).stream()
            .map( DataSet::getUid )
            .collect( Collectors.toSet() );

        return jdbcOrgUnitAssociationsStore.getOrganisationUnitsAssociationsForCurrentUser( uids );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<LockException> getLockExceptionByOrganisationUnit( String operator, String orgUnitIds,
        Collection<LockException> lockExceptions )
    {

        List<String> ids = parseIdFromString( orgUnitIds, operator );

        return lockExceptions.stream()
            .filter( lockException -> ids.contains( lockException.getOrganisationUnit().getUid() ) )
            .collect( Collectors.toList() );
    }

    private List<LockException> getLockExceptionByDataSet( String operator, String dataSetIds,
        Collection<LockException> lockExceptions )
    {
        List<String> ids = parseIdFromString( dataSetIds, operator );

        return lockExceptions.stream()
            .filter( lockException -> ids.contains( lockException.getDataSet().getUid() ) )
            .collect( Collectors.toList() );
    }

    private List<LockException> getLockExceptionByPeriod( String operator, String periods,
        Collection<LockException> lockExceptions )
    {
        List<String> ids = parseIdFromString( periods, operator );

        return lockExceptions.stream()
            .filter( lockException -> ids.contains( lockException.getPeriod().getIsoDate() ) )
            .collect( Collectors.toList() );
    }

    private List<String> parseIdFromString( String input, String operator )
    {
        List<String> ids = new ArrayList<>();

        if ( "in".equalsIgnoreCase( operator ) )
        {
            if ( input.startsWith( "[" ) && input.endsWith( "]" ) )
            {
                String[] split = input.substring( 1, input.length() - 1 ).split( "," );
                Collections.addAll( ids, split );
            }
            else
            {
                throw new QueryParserException( "Invalid query: " + input );
            }
        }
        else if ( "eq".equalsIgnoreCase( operator ) )
        {
            ids.add( input );
        }
        return ids;
    }
}
