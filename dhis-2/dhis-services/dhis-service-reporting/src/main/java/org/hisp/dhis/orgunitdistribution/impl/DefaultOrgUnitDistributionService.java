package org.hisp.dhis.orgunitdistribution.impl;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionService;
import org.hisp.dhis.system.grid.ListGrid;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DefaultOrgUnitDistributionService
    implements OrgUnitDistributionService
{
    private static final String TITLE_SEP = " - ";
    private static final String FIRST_COLUMN_TEXT = "Organisation unit";
    private static final String HEADER_TOTAL = "Total";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;
    
    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }
    
    private ChartService chartService;
    
    public void setChartService( ChartService chartService )
    {
        this.chartService = chartService;
    }

    // -------------------------------------------------------------------------
    // OrgUnitDistributionService implementation
    // -------------------------------------------------------------------------

    @Override
    public JFreeChart getOrganisationUnitDistributionChart( OrganisationUnitGroupSet groupSet, OrganisationUnit organisationUnit )
    {
        Map<String, Double> categoryValues = new HashMap<>();
        
        Grid grid = getOrganisationUnitDistribution( groupSet, organisationUnit, true );
        
        if ( grid == null || grid.getHeight() != 1 )
        {
            return null;
        }
        
        for ( int i = 1; i < grid.getWidth() - 2; i++ ) // Skip name, none and total column
        {
            categoryValues.put( grid.getHeaders().get( i ).getName(), Double.valueOf( String.valueOf( grid.getRow( 0 ).get( i ) ) ) );
        }
        
        String title = groupSet.getName() + TITLE_SEP + organisationUnit.getName();
        
        JFreeChart chart = chartService.getJFreeChart( title, PlotOrientation.VERTICAL, CategoryLabelPositions.DOWN_45, categoryValues );
        
        return chart;
    }
    
    @Override
    @Transactional
    public Grid getOrganisationUnitDistribution( OrganisationUnitGroupSet groupSet, OrganisationUnit organisationUnit, boolean organisationUnitOnly  )
    {
        Grid grid = new ListGrid();
        grid.setTitle( groupSet.getName() + TITLE_SEP + organisationUnit.getName() );
        
        List<OrganisationUnit> units = organisationUnitOnly ? Arrays.asList( organisationUnit ) : new ArrayList<>( organisationUnit.getChildren() );
        List<OrganisationUnitGroup> groups = new ArrayList<>( groupSet.getOrganisationUnitGroups() );
        
        Collections.sort( units );
        Collections.sort( groups );
        
        if ( !organisationUnitOnly )
        {
            units.add( organisationUnit ); // Add parent itself to the end to get the total
        }
        
        grid.addHeader( new GridHeader( FIRST_COLUMN_TEXT, FIRST_COLUMN_TEXT, null, false, true ) );
        
        for ( OrganisationUnitGroup group : groups )
        {
            grid.addHeader( new GridHeader( group.getName(), false, false )  );
        }

        grid.addHeader( new GridHeader( HEADER_TOTAL, false, false ) );
        
        for ( OrganisationUnit unit : units )
        {            
            grid.addRow();
            grid.addValue( unit.getName() );
            
            int totalGroup = 0;
            
            Set<OrganisationUnit> subTree = new HashSet<>( organisationUnitService.getOrganisationUnitWithChildren( unit.getId() ) ); 
            
            for ( OrganisationUnitGroup group : groups )
            {
                Set<OrganisationUnit> result = Sets.intersection( subTree, group.getMembers() );
                
                int count = result != null ? result.size() : 0;
                
                grid.addValue( count );
                
                totalGroup += count;
            }

            grid.addValue( totalGroup );            
        }
        
        return grid;
    }
}
