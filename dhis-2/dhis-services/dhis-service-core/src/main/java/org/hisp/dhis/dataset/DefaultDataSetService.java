package org.hisp.dhis.dataset;

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

import static org.hisp.dhis.i18n.I18nUtils.getCountByName;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetween;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetweenByName;
import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
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
        return dataSetStore.save( dataSet );
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
        return i18n( i18nService, dataSetStore.get( id ) );
    }

    @Override
    public DataSet getDataSet( String uid )
    {
        return i18n( i18nService, dataSetStore.getByUid( uid ) );
    }

    @Override
    public DataSet getDataSetNoAcl( String uid )
    {
        return i18n( i18nService, dataSetStore.getByUidNoAcl( uid ) );
    }

    @Override
    public DataSet getDataSet( int id, boolean i18nDataElements, boolean i18nIndicators, boolean i18nOrgUnits )
    {
        return getDataSet( id, i18nDataElements, i18nIndicators, i18nOrgUnits, false );
    }

    @Override
    public DataSet getDataSet( int id, boolean i18nDataElements, boolean i18nIndicators, boolean i18nOrgUnits, boolean i18nSections )
    {
        DataSet dataSet = getDataSet( id );

        if ( dataSet != null )
        {
            if ( i18nDataElements )
            {
                i18n( i18nService, dataSet.getDataElements() );
            }
    
            if ( i18nIndicators )
            {
                i18n( i18nService, dataSet.getIndicators() );
            }
    
            if ( i18nOrgUnits )
            {
                i18n( i18nService, dataSet.getSources() );
            }
    
            if ( i18nSections && dataSet.hasSections() )
            {
                i18n( i18nService, dataSet.getSections() );
    
                for ( Section section : dataSet.getSections() )
                {
                    i18n( i18nService, section.getDataElements() );
                }
            }
        }

        return dataSet;
    }

    @Override
    public DataSet getDataSet( String id, boolean i18nDataElements, boolean i18nIndicators, boolean i18nOrgUnits, boolean i18nSections )
    {
        DataSet dataSet = getDataSet( id );
        
        return dataSet != null ? getDataSet( dataSet.getId(), i18nDataElements, i18nIndicators, i18nOrgUnits, i18nSections ) : null;
    }

    @Override
    public List<DataSet> getDataSetByName( String name )
    {
        return new ArrayList<>( i18n( i18nService, dataSetStore.getAllEqName( name ) ) );
    }

    @Override
    public List<DataSet> getDataSetByShortName( String shortName )
    {
        return new ArrayList<>( i18n( i18nService, dataSetStore.getAllEqShortName( shortName ) ) );
    }

    @Override
    public DataSet getDataSetByCode( String code )
    {
        return i18n( i18nService, dataSetStore.getByCode( code ) );
    }

    @Override
    public List<DataSet> getDataSetsBySources( Collection<OrganisationUnit> sources )
    {
        return i18n( i18nService, dataSetStore.getDataSetsBySources( sources ) );
    }

    @Override
    public List<DataSet> getDataSetsByDataEntryForm( DataEntryForm dataEntryForm )
    {
        return i18n( i18nService, dataSetStore.getDataSetsByDataEntryForm( dataEntryForm ) );
    }

    @Override
    public List<DataSet> getAllDataSets()
    {
        return i18n( i18nService, dataSetStore.getAll() );
    }

    @Override
    public List<DataSet> getDataSetsByPeriodType( PeriodType periodType )
    {
        return i18n( i18nService, dataSetStore.getDataSetsByPeriodType( periodType ) );
    }

    @Override
    public List<DataSet> getDataSetsByUid( Collection<String> uids )
    {
        return dataSetStore.getByUid( uids );
    }
    
    @Override
    public List<DataSet> getDataSetsByUidNoAcl( Collection<String> uids )
    {
        return dataSetStore.getByUidNoAcl( uids );
    }

    @Override
    public Set<DataElement> getDataElements( DataSet dataSet )
    {
        return i18n( i18nService, dataSet.getDataElements() );
    }

    @Override
    public List<DataSet> getDataSetsForMobile( OrganisationUnit source )
    {
        return i18n( i18nService, dataSetStore.getDataSetsForMobile( source ) );
    }

    @Override
    public List<DataSet> getDataSetsForMobile()
    {
        return i18n( i18nService, dataSetStore.getDataSetsForMobile() );
    }

    @Override
    public int getDataSetCount()
    {
        return dataSetStore.getCount();
    }

    @Override
    public int getDataSetCountByName( String name )
    {
        return getCountByName( i18nService, dataSetStore, name );
    }

    @Override
    public List<DataSet> getDataSetsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, dataSetStore, first, max );
    }

    @Override
    public List<DataSet> getDataSetsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, dataSetStore, name, first, max );
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
            return i18n( i18nService, Lists.newArrayList( user.getUserCredentials().getAllDataSets() ) );
        }
    }
    
    // -------------------------------------------------------------------------
    // DataSet LockExceptions
    // -------------------------------------------------------------------------

    @Override
    public int addLockException( LockException lockException )
    {
        return lockExceptionStore.save( lockException );
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
    public boolean isLockedPeriod( DataSet dataSet, Period period, OrganisationUnit organisationUnit, Date now )
    {
        return dataSet.isLocked( period, now ) && lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L;
    }

    @Override
    public boolean isLocked( DataSet dataSet, Period period, OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo, Date now )
    {
        return isLockedPeriod( dataSet, period, organisationUnit, now ) ||
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
    public void mergeWithCurrentUserOrganisationUnits( DataSet dataSet, Collection<OrganisationUnit> mergeOrganisationUnits )
    {
        Set<OrganisationUnit> selectedOrgUnits = Sets.newHashSet( dataSet.getSources() );
        
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( currentUserService.getCurrentUser().getOrganisationUnits() );

        Set<OrganisationUnit> userOrganisationUnits = Sets.newHashSet( organisationUnitService.getOrganisationUnitsByQuery( params ) );

        selectedOrgUnits.removeAll( userOrganisationUnits );
        selectedOrgUnits.addAll( mergeOrganisationUnits );

        dataSet.updateOrganisationUnits( selectedOrgUnits );

        updateDataSet( dataSet );
    }
}
