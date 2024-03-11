package org.hisp.dhis.resourcetable;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.resourcetable.table.*;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class DefaultResourceTableService
    implements ResourceTableService
{
    private static final Log log = LogFactory.getLog( DefaultResourceTableService.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ResourceTableStore resourceTableStore;

    public void setResourceTableStore( ResourceTableStore resourceTableStore )
    {
        this.resourceTableStore = resourceTableStore;
    }
    
    private IdentifiableObjectManager idObjectManager;

    public void setIdObjectManager( IdentifiableObjectManager idObjectManager )
    {
        this.idObjectManager = idObjectManager;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private SqlViewService sqlViewService;

    public void setSqlViewService( SqlViewService sqlViewService )
    {
        this.sqlViewService = sqlViewService;
    }

    private DataApprovalLevelService dataApprovalLevelService;

    public void setDataApprovalLevelService( DataApprovalLevelService dataApprovalLevelService )
    {
        this.dataApprovalLevelService = dataApprovalLevelService;
    }
    
    private CategoryService categoryService;
    
    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private StatementBuilder statementBuilder;
    
    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }
    
    // -------------------------------------------------------------------------
    // ResourceTableService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateOrganisationUnitStructures()
    {
        resourceTableStore.generateResourceTable( new OrganisationUnitStructureResourceTable( 
            null, organisationUnitService, organisationUnitService.getNumberOfOrganisationalLevels() ) );
    }
    
    @Override
    @Transactional
    public void generateDataSetOrganisationUnitCategoryTable()
    {
        resourceTableStore.generateResourceTable( new DataSetOrganisationUnitCategoryResourceTable( 
            idObjectManager.getAllNoAcl( DataSet.class ), categoryService.getDefaultCategoryOptionCombo() ) );
    }
    
    @Override
    @Transactional
    public void generateCategoryOptionComboNames()
    {
        resourceTableStore.generateResourceTable( new CategoryOptionComboNameResourceTable( 
            idObjectManager.getAllNoAcl( CategoryCombo.class ) ) );
    }

    @Override
    @Transactional
    public void generateDataElementGroupSetTable()
    {
        resourceTableStore.generateResourceTable( new DataElementGroupSetResourceTable(
            idObjectManager.getDataDimensionsNoAcl( DataElementGroupSet.class ) ) );
    }

    @Override
    @Transactional
    public void generateIndicatorGroupSetTable()
    {
        resourceTableStore.generateResourceTable( new IndicatorGroupSetResourceTable(
            idObjectManager.getAllNoAcl( IndicatorGroupSet.class ) ) );
    }

    @Override
    @Transactional
    public void generateOrganisationUnitGroupSetTable()
    {
        resourceTableStore.generateResourceTable( new OrganisationUnitGroupSetResourceTable(
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class ),
            statementBuilder.supportsPartialIndexes(), organisationUnitService.getNumberOfOrganisationalLevels() ) );
    }

    @Override
    @Transactional
    public void generateCategoryTable()
    {
        resourceTableStore.generateResourceTable( new CategoryResourceTable( 
            idObjectManager.getDataDimensionsNoAcl( Category.class ),
            idObjectManager.getDataDimensionsNoAcl( CategoryOptionGroupSet.class ) ) );
    }

    @Override
    @Transactional
    public void generateDataElementTable()
    {
        resourceTableStore.generateResourceTable( new DataElementResourceTable( 
            idObjectManager.getAllNoAcl( DataElement.class ) ) );
    }

    @Override
    public void generateDatePeriodTable()
    {
        resourceTableStore.generateResourceTable( new DatePeriodResourceTable( null ) );
    }

    @Override
    @Transactional
    public void generatePeriodTable()
    {
        resourceTableStore.generateResourceTable( new PeriodResourceTable( periodService.getAllPeriods() ) );
    }

    @Override
    @Transactional
    public void generateCategoryOptionComboTable()
    {
        resourceTableStore.generateResourceTable( new CategoryOptionComboResourceTable( null ) );            
    }

    @Override
    public void generateDataApprovalRemapLevelTable()
    {
        resourceTableStore.generateResourceTable( new DataApprovalRemapLevelResourceTable( null ) );
    }

    @Override
    public void generateDataApprovalMinLevelTable()
    {
        List<OrganisationUnitLevel> orgUnitLevels = Lists.newArrayList(
            dataApprovalLevelService.getOrganisationUnitApprovalLevels() );

        if ( orgUnitLevels.size() > 0 )
        {
            resourceTableStore.generateResourceTable( new DataApprovalMinLevelResourceTable( orgUnitLevels ) );
        }
    }

    // -------------------------------------------------------------------------
    // SQL Views. Each view is created/dropped in separate transactions so that
    // process continues even if individual operations fail.
    // -------------------------------------------------------------------------

    @Override
    public void createAllSqlViews()
    {
        List<SqlView> views = new ArrayList<>( sqlViewService.getAllSqlViewsNoAcl() );
        Collections.sort( views );
        
        for ( SqlView view : views )
        {
            if ( !view.isQuery() )
            {
                try
                {
                    sqlViewService.createViewTable( view );
                }
                catch ( IllegalQueryException ex )
                {
                    log.warn( String.format( "Ignoring SQL view which failed validation: %s, %s, message: %s", 
                        view.getUid(), view.getName(), ex.getMessage() ) );
                }
            }
        }
    }

    @Override
    public void dropAllSqlViews()
    {
        List<SqlView> views = new ArrayList<>( sqlViewService.getAllSqlViewsNoAcl() );
        Collections.sort( views );
        Collections.reverse( views );

        for ( SqlView view : views )
        {
            if ( !view.isQuery() )
            {
                sqlViewService.dropViewTable( view );
            }
        }
    }
}
