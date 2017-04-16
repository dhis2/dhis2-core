package org.hisp.dhis.dataset;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultDataSetService
    implements DataSetService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetStore dataSetStore;

    public void setDataSetStore( DataSetStore dataSetStore )
    {
        this.dataSetStore = dataSetStore;
    }

    private LockExceptionStore lockExceptionStore;

    public void setLockExceptionStore( LockExceptionStore lockExceptionStore )
    {
        this.lockExceptionStore = lockExceptionStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private DataApprovalService dataApprovalService;

    public void setDataApprovalService( DataApprovalService dataApprovalService )
    {
        this.dataApprovalService = dataApprovalService;
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Override
    public int addDataSet( DataSet dataSet )
    {
        dataSetStore.save( dataSet );
        return dataSet.getId();
    }

    @Override
    public void updateDataSet( DataSet dataSet )
    {
        dataSetStore.update( dataSet );
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        dataSetStore.delete( dataSet );
    }

    @Override
    public DataSet getDataSet( int id )
    {
        return dataSetStore.get( id );
    }

    @Override
    public DataSet getDataSet( String uid )
    {
        return dataSetStore.getByUid( uid );
    }

    @Override
    public DataSet getDataSetNoAcl( String uid )
    {
        return dataSetStore.getByUidNoAcl( uid );
    }

    @Override
    public List<DataSet> getDataSetsByDataEntryForm( DataEntryForm dataEntryForm )
    {
        return dataSetStore.getDataSetsByDataEntryForm( dataEntryForm );
    }

    @Override
    public List<DataSet> getAllDataSets()
    {
        return dataSetStore.getAll();
    }

    @Override
    public List<DataSet> getDataSetsByPeriodType( PeriodType periodType )
    {
        return dataSetStore.getDataSetsByPeriodType( periodType );
    }

    @Override
    public List<DataSet> getDataSetsByUid( Collection<String> uids )
    {
        return dataSetStore.getByUid( uids );
    }

    @Override
    public List<DataSet> getDataSetsForMobile( OrganisationUnit source )
    {
        return dataSetStore.getDataSetsForMobile( source );
    }

    @Override
    public List<DataSet> getCurrentUserDataSets()
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            return Lists.newArrayList();
        }

        if ( user.isSuper() )
        {
            return getAllDataSets();
        }
        else
        {
            return Lists.newArrayList( user.getUserCredentials().getAllDataSets() );
        }
    }

    // -------------------------------------------------------------------------
    // DataSet LockExceptions
    // -------------------------------------------------------------------------

    @Override
    public int addLockException( LockException lockException )
    {
        lockExceptionStore.save( lockException );
        return lockException.getId();
    }

    @Override
    public void updateLockException( LockException lockException )
    {
        lockExceptionStore.update( lockException );
    }

    @Override
    public void deleteLockException( LockException lockException )
    {
        lockExceptionStore.delete( lockException );
    }

    @Override
    public LockException getLockException( int id )
    {
        return lockExceptionStore.get( id );
    }

    @Override
    public int getLockExceptionCount()
    {
        return lockExceptionStore.getCount();
    }

    @Override
    public List<LockException> getAllLockExceptions()
    {
        return lockExceptionStore.getAll();
    }

    @Override
    public List<LockException> getLockExceptionsBetween( int first, int max )
    {
        return lockExceptionStore.getAllOrderedName( first, max );
    }

    @Override
    public List<LockException> getLockExceptionCombinations()
    {
        return lockExceptionStore.getCombinations();
    }

    @Override
    public void deleteLockExceptionCombination( DataSet dataSet, Period period )
    {
        lockExceptionStore.deleteCombination( dataSet, period );
    }

    @Override
    public void deleteLockExceptionCombination( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        lockExceptionStore.deleteCombination( dataSet, period, organisationUnit );
    }

    @Override
    public boolean isLocked( DataSet dataSet, Period period, OrganisationUnit organisationUnit, Date now )
    {
        return dataSet.isLocked( period, now ) && lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L;
    }

    @Override
    public boolean isLocked( DataSet dataSet, Period period, OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo, Date now )
    {
        return isLocked( dataSet, period, organisationUnit, now ) ||
            dataApprovalService.isApproved( dataSet.getWorkflow(), period, organisationUnit, attributeOptionCombo );
    }

    @Override
    public boolean isLocked( DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        DataElementCategoryOptionCombo attributeOptionCombo, Date now, boolean useOrgUnitChildren )
    {
        if ( !useOrgUnitChildren )
        {
            return isLocked( dataSet, period, organisationUnit, attributeOptionCombo, now );
        }

        if ( organisationUnit == null || !organisationUnit.hasChild() )
        {
            return false;
        }

        for ( OrganisationUnit child : organisationUnit.getChildren() )
        {
            if ( isLocked( dataSet, period, child, attributeOptionCombo, now ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isLocked( DataElement dataElement, Period period, OrganisationUnit organisationUnit,
        DataElementCategoryOptionCombo attributeOptionCombo, Date now )
    {
        now = now != null ? now : new Date();

        boolean expired = dataElement.isExpired( period, now );

        if ( expired && lockExceptionStore.getCount( dataElement, period, organisationUnit ) == 0L )
        {
            return true;
        }

        DataSet dataSet = dataElement.getApprovalDataSet();

        if ( dataSet == null )
        {
            return false;
        }

        return dataApprovalService.isApproved( dataSet.getWorkflow(), period, organisationUnit, attributeOptionCombo );
    }

    @Override
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

    private List<LockException> getLockExceptionByOrganisationUnit( String operator, String orgUnitIds, Collection<LockException> lockExceptions )
    {

        List<String> ids = parseIdFromString( orgUnitIds, operator );

        return lockExceptions.stream()
            .filter( lockException -> ids.contains( lockException.getOrganisationUnit().getUid() ) )
            .collect( Collectors.toList() );
    }

    private List<LockException> getLockExceptionByDataSet( String operator, String dataSetIds, Collection<LockException> lockExceptions )
    {
        List<String> ids = parseIdFromString( dataSetIds, operator );

        return lockExceptions.stream()
            .filter( lockException -> ids.contains( lockException.getDataSet().getUid() ) )
            .collect( Collectors.toList() );
    }

    private List<LockException> getLockExceptionByPeriod( String operator, String periods, Collection<LockException> lockExceptions )
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
