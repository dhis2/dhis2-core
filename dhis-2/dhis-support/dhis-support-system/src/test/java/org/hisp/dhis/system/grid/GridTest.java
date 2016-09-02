package org.hisp.dhis.system.grid;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class GridTest
{
    private Grid gridA;
    private Grid gridB;
    
    private GridHeader headerA;
    private GridHeader headerB;
    private GridHeader headerC;
    
    @Before
    public void setUp()
    {
        gridA = new ListGrid();
        gridB = new ListGrid();
        
        headerA = new GridHeader( "ColA", "colA", String.class.getName(), false, true );
        headerB = new GridHeader( "ColB", "colB", String.class.getName(), false, true );
        headerC = new GridHeader( "ColC", "colC", String.class.getName(), true, false );
        
        gridA.addHeader( headerA );
        gridA.addHeader( headerB );
        gridA.addHeader( headerC );
        
        gridA.addRow();        
        gridA.addValue( 11 );
        gridA.addValue( 12 );
        gridA.addValue( 13 );

        gridA.addRow();        
        gridA.addValue( 21 );
        gridA.addValue( 22 );
        gridA.addValue( 23 );

        gridA.addRow();        
        gridA.addValue( 31 );
        gridA.addValue( 32 );
        gridA.addValue( 33 );

        gridA.addRow();        
        gridA.addValue( 41 );
        gridA.addValue( 42 );
        gridA.addValue( 43 );

        gridB.addRow();        
        gridB.addValue( 11 );
        gridB.addValue( 12 );
        gridB.addValue( 13 );
    }
    
    @Test
    public void testAddGrid()
    {
        gridA.addRows( gridB );
        
        assertEquals( 5, gridA.getHeight() );
    }
    
    @Test
    public void testSubstituteMetaData()
    {
        Map<Object, Object> metaData = new HashMap<>();
        metaData.put( 11, "Eleven" );
        metaData.put( 12, "Twelve" );
        metaData.put( 21, "TwentyOne" );
        metaData.put( 22, "TwentyTwo" );
                
        assertEquals( 11, gridA.getValue( 0, 0 ) );
        assertEquals( 12, gridA.getValue( 0, 1 ) );
        assertEquals( 21, gridA.getValue( 1, 0 ) );
        assertEquals( 22, gridA.getValue( 1, 1 ) );
        
        gridA.substituteMetaData( metaData );

        assertEquals( "Eleven", gridA.getValue( 0, 0 ) );
        assertEquals( "Twelve", gridA.getValue( 0, 1 ) );
        assertEquals( "TwentyOne", gridA.getValue( 1, 0 ) );
        assertEquals( "TwentyTwo", gridA.getValue( 1, 1 ) );
    }

    @Test
    public void testSubstituteMetaDataForIndexA()
    {
        Map<Object, Object> metaData = new HashMap<>();
        metaData.put( 11, "Eleven" );
        metaData.put( 12, "Twelve" );
        metaData.put( 21, "TwentyOne" );
        metaData.put( 22, "TwentyTwo" );
                
        assertEquals( 11, gridA.getValue( 0, 0 ) );
        assertEquals( 12, gridA.getValue( 0, 1 ) );
        assertEquals( 21, gridA.getValue( 1, 0 ) );
        assertEquals( 22, gridA.getValue( 1, 1 ) );
        
        gridA.substituteMetaData( 1, 1, metaData );

        assertEquals( 11, gridA.getValue( 0, 0 ) );
        assertEquals( "Twelve", gridA.getValue( 0, 1 ) );
        assertEquals( 21, gridA.getValue( 1, 0 ) );
        assertEquals( "TwentyTwo", gridA.getValue( 1, 1 ) );
    }

    @Test
    public void testSubstituteMetaDataForIndexB()
    {
        Map<Object, Object> metaData = new HashMap<>();
        metaData.put( 11, "Twelve" );
        metaData.put( 21, "TwentyTwo" );
        metaData.put( 31, "ThirtyTwo" );
        metaData.put( 41, "FourtyTwo" );

        assertEquals( 11, gridA.getValue( 0, 0 ) );
        assertEquals( 21, gridA.getValue( 1, 0 ) );
        assertEquals( 31, gridA.getValue( 2, 0 ) );
        assertEquals( 41, gridA.getValue( 3, 0 ) );
        
        assertEquals( 12, gridA.getValue( 0, 1 ) );
        assertEquals( 22, gridA.getValue( 1, 1 ) );
        assertEquals( 32, gridA.getValue( 2, 1 ) );
        assertEquals( 42, gridA.getValue( 3, 1 ) );
        
        gridA.substituteMetaData( 0, 1, metaData );

        assertEquals( 11, gridA.getValue( 0, 0 ) );
        assertEquals( 21, gridA.getValue( 1, 0 ) );
        assertEquals( 31, gridA.getValue( 2, 0 ) );
        assertEquals( 41, gridA.getValue( 3, 0 ) );

        assertEquals( "Twelve", gridA.getValue( 0, 1 ) );
        assertEquals( "TwentyTwo", gridA.getValue( 1, 1 ) );
        assertEquals( "ThirtyTwo", gridA.getValue( 2, 1 ) );
        assertEquals( "FourtyTwo", gridA.getValue( 3, 1 ) );
    }
    
    @Test
    public void testGetHeight()
    {
        assertEquals( 4, gridA.getHeight() );
    }
    
    @Test
    public void testGetWidth()
    {
        assertEquals( 3, gridA.getWidth() );
    }
        
    @Test
    public void testGetRow()
    {
        List<Object> rowA = gridA.getRow( 0 );
        
        assertTrue( rowA.size() == 3 );
        assertTrue( rowA.contains( 11 ) );
        assertTrue( rowA.contains( 12 ) );
        assertTrue( rowA.contains( 13 ) );
        
        List<Object> rowB = gridA.getRow( 1 );
        
        assertTrue( rowB.size() == 3 );
        assertTrue( rowB.contains( 21 ) );
        assertTrue( rowB.contains( 22 ) );
        assertTrue( rowB.contains( 23 ) );
    }

    @Test
    public void testGetHeaders()
    {
        assertEquals( 3, gridA.getHeaders().size() );
    }
    
    @Test
    public void tetsGetVisibleHeaders()
    {
        assertEquals( 2, gridA.getVisibleHeaders().size() );
        assertTrue( gridA.getVisibleHeaders().contains( headerA ) );
        assertTrue( gridA.getVisibleHeaders().contains( headerB ) );
    }

    @Test
    public void testGetRows()
    {
        assertEquals( 4, gridA.getRows().size() );
        assertEquals( 3, gridA.getWidth() );
    }

    @Test
    public void testGetGetVisibleRows()
    {
        assertEquals( 4, gridA.getVisibleRows().size() );
        assertEquals( 2, gridA.getVisibleRows().get( 0 ).size() );
        assertEquals( 2, gridA.getVisibleRows().get( 1 ).size() );
        assertEquals( 2, gridA.getVisibleRows().get( 2 ).size() );
        assertEquals( 2, gridA.getVisibleRows().get( 3 ).size() );
    }
    
    @Test
    public void testGetColumn()
    {        
        List<Object> column1 = gridA.getColumn( 1 );
        
        assertEquals( 4, column1.size() );
        assertTrue( column1.contains( 12 ) );
        assertTrue( column1.contains( 22 ) );
        assertTrue( column1.contains( 32 ) );
        assertTrue( column1.contains( 42 ) );

        List<Object> column2 = gridA.getColumn( 2 );
        
        assertEquals( 4, column2.size() );
        assertTrue( column2.contains( 13 ) );
        assertTrue( column2.contains( 23 ) );
        assertTrue( column2.contains( 33 ) );
        assertTrue( column2.contains( 43 ) );
    }
    
    @Test
    public void testAddColumn()
    {
        List<Object> columnValues = new ArrayList<>();
        columnValues.add( 14 );
        columnValues.add( 24 );
        columnValues.add( 34 );
        columnValues.add( 44 );
        
        gridA.addColumn( columnValues );
        
        List<Object> column3 = gridA.getColumn( 3 );
        
        assertEquals( 4, column3.size() );
        assertTrue( column3.contains( 14 ) );
        assertTrue( column3.contains( 24 ) );
        assertTrue( column3.contains( 34 ) );
        assertTrue( column3.contains( 44 ) );
        
        List<Object> row2 = gridA.getRow( 1 );
        
        assertEquals( 4, row2.size() );
        assertTrue( row2.contains( 21 ) );
        assertTrue( row2.contains( 22 ) );
        assertTrue( row2.contains( 23 ) );
        assertTrue( row2.contains( 24 ) );
    }
    
    @Test
    public void testAddAndPopulateColumn()
    {
        assertEquals( 3, gridA.getWidth() );
        
        gridA.addAndPopulateColumn( 81 );
        
        assertEquals( 4, gridA.getWidth() );
        
        List<Object> col = gridA.getColumn( 3 );
        
        assertEquals( 4, col.size() );
        
        for ( Object val : col )
        {
            assertEquals( 81, val );
        }        
    }

    @Test
    public void testAddAndPopulateColumns()
    {
        assertEquals( 3, gridA.getWidth() );
        
        gridA.addAndPopulateColumns( 2, 91 );
        
        assertEquals( 5, gridA.getWidth() );
        
        List<Object> col3 = gridA.getColumn( 3 );
        List<Object> col4 = gridA.getColumn( 4 );
        
        assertEquals( 4, col3.size() );
        assertEquals( 4, col4.size() );
        
        for ( Object val : col3 )
        {
            assertEquals( 91, val );
        }

        for ( Object val : col4 )
        {
            assertEquals( 91, val );
        }
    }
    
    @Test
    public void testRemoveColumn()
    {
        assertEquals( 3, gridA.getWidth() );
        
        gridA.removeColumn( 2 );
        
        assertEquals( 2, gridA.getWidth() );
    }
    
    @Test
    public void testRemoveColumnByHeader()
    {
        assertEquals( 3, gridA.getWidth() );
        
        gridA.removeColumn( headerB );
        
        assertEquals( 2, gridA.getWidth() );
    }
    
    @Test
    public void testRemoveCurrentWriteRow()
    {
        assertEquals( 4, gridA.getRows().size() );
        
        gridA.addRow();
        gridA.addValue( 51 );
        gridA.addValue( 52 );
        gridA.addValue( 53 );

        assertEquals( 5, gridA.getRows().size() );
        
        gridA.removeCurrentWriteRow();

        assertEquals( 4, gridA.getRows().size() );

        gridA.addRow();
        gridA.addValue( 51 );
        gridA.addValue( 52 );
        gridA.addValue( 53 );

        assertEquals( 5, gridA.getRows().size() );        
    }

    @Test
    public void testLimit()
    {
        assertEquals( 4, gridA.getRows().size() );
        
        gridA.limitGrid( 2 );
        
        assertEquals( 2, gridA.getRows().size() );
        
        List<Object> rowA = gridA.getRow( 0 );
        assertTrue( rowA.contains( 11 ) );

        List<Object> rowB = gridA.getRow( 1 );        
        assertTrue( rowB.contains( 21 ) );
        
        gridA.limitGrid( 0 );
        
        assertEquals( 2, gridA.getRows().size() );
    }
    
    @Test
    public void testLimitShortList()
    {
        assertEquals( 4, gridA.getRows().size() );
        
        gridA.limitGrid( 6 );
        
        assertEquals( 4, gridA.getRows().size() );

        gridA.limitGrid( 4 );
        
        assertEquals( 4, gridA.getRows().size() );
    }
    
    @Test
    public void testLimits()
    {
        assertEquals( 4, gridA.getRows().size() );
        
        gridA.limitGrid( 1, 3 );
        
        assertEquals( 2, gridA.getRows().size() );

        List<Object> rowA = gridA.getRow( 0 );
        assertTrue( rowA.contains( 21 ) );

        List<Object> rowB = gridA.getRow( 1 );        
        assertTrue( rowB.contains( 31 ) );        
    }
    
    @Test
    public void testSortA()
    {
        Grid grid = new ListGrid();
        
        grid.addRow().addValue( 1 ).addValue( "a" );
        grid.addRow().addValue( 2 ).addValue( "b" );
        grid.addRow().addValue( 3 ).addValue( "c" );
        
        grid.sortGrid( 2, 1 );

        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( "c" ) );

        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( "b" ) );
        
        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( "a" ) );
    }

    @Test
    public void testSortB()
    {
        Grid grid = new ListGrid();
        
        grid.addRow().addValue( 3 ).addValue( "a" );
        grid.addRow().addValue( 2 ).addValue( "b" );
        grid.addRow().addValue( 1 ).addValue( "c" );
        
        grid.sortGrid( 1, -1 );

        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( 1 ) );

        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( 2 ) );
        
        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( 3 ) );       
    }

    @Test
    public void testSortC()
    {
        Grid grid = new ListGrid();

        grid.addRow().addValue( 1 ).addValue( "c" );
        grid.addRow().addValue( 3 ).addValue( "a" );
        grid.addRow().addValue( 2 ).addValue( "b" );
        
        grid.sortGrid( 1, 1 );

        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( 3 ) );

        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( 2 ) );
        
        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( 1 ) );
    }
    
    @Test
    public void testSortD()
    {
        Grid grid = new ListGrid();
        
        grid.addRow().addValue( "a" ).addValue( "a" ).addValue( 5.2 );
        grid.addRow().addValue( "b" ).addValue( "b" ).addValue( 0.0 );
        grid.addRow().addValue( "c" ).addValue( "c" ).addValue( 108.1 );
        grid.addRow().addValue( "d" ).addValue( "d" ).addValue( 45.0 );
        grid.addRow().addValue( "e" ).addValue( "e" ).addValue( 4043.9 );
        grid.addRow().addValue( "f" ).addValue( "f" ).addValue( 0.1 );
        
        grid = grid.sortGrid( 3, 1 );
        
        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( 4043.9 ) );

        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( 108.1 ) );
        
        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( 45.0 ) );

        List<Object> row4 = grid.getRow( 3 );
        assertTrue( row4.contains( 5.2 ) );

        List<Object> row5 = grid.getRow( 4 );
        assertTrue( row5.contains( 0.1 ) );

        List<Object> row6 = grid.getRow( 5 );
        assertTrue( row6.contains( 0.0 ) );    
    }

    @Test
    public void testSortE()
    {
        Grid grid = new ListGrid();

        grid.addRow().addValue( "two" ).addValue( 2 );
        grid.addRow().addValue( "null" ).addValue( null );
        grid.addRow().addValue( "three" ).addValue( 3 );
        
        grid.sortGrid( 2, 1 );

        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( "three" ) );

        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( "two" ) );
        
        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( "null" ) );
    }

    @Test
    public void testSortF()
    {
        Grid grid = new ListGrid();

        grid.addRow().addValue( "two" ).addValue( 2 );
        grid.addRow().addValue( "null" ).addValue( null );
        grid.addRow().addValue( "one" ).addValue( 1 );
        
        grid.sortGrid( 2, -1 );

        List<Object> row1 = grid.getRow( 0 );
        assertTrue( row1.contains( "null" ) );
        
        List<Object> row2 = grid.getRow( 1 );
        assertTrue( row2.contains( "one" ) );

        List<Object> row3 = grid.getRow( 2 );
        assertTrue( row3.contains( "two" ) );        
    }
    
    @Test
    public void testGridRowComparator()
    {
        List<List<Object>> lists = new ArrayList<>();
        List<Object> l1 = getList( "b", "b", 50 );
        List<Object> l2 = getList( "c", "c", 400 );
        List<Object> l3 = getList( "a", "a", 6 );
        lists.add( l1 );
        lists.add( l2 );
        lists.add( l3 );
        
        Comparator<List<Object>> comparator = new ListGrid.GridRowComparator( 2, -1 );
        Collections.sort( lists, comparator );
                
        assertEquals( l3, lists.get( 0 ) );
        assertEquals( l1, lists.get( 1 ) );
        assertEquals( l2, lists.get( 2 ) );
    }
    
    @Test
    public void testAddRegressionColumn()
    {
        gridA = new ListGrid();

        gridA.addRow();        
        gridA.addValue( 10.0 );
        gridA.addRow();        
        gridA.addValue( 50.0 );
        gridA.addRow();        
        gridA.addValue( 20.0 );
        gridA.addRow();        
        gridA.addValue( 60.0 );
        
        gridA.addRegressionColumn( 0, true );
        
        List<Object> column = gridA.getColumn( 1 );
        
        assertTrue( column.size() == 4 );
        assertTrue( column.contains( 17.0 ) );
        assertTrue( column.contains( 29.0 ) );
        assertTrue( column.contains( 41.0 ) );
        assertTrue( column.contains( 53.0 ) );
    }
    
    @Test
    public void testAddCumulativeColumn()
    {
        gridA = new ListGrid();

        gridA.addRow();        
        gridA.addValue( 10.0 );
        gridA.addRow();        
        gridA.addValue( 50.0 );
        gridA.addRow();        
        gridA.addValue( 20.0 );
        gridA.addRow();        
        gridA.addValue( 60.0 );
        
        gridA.addCumulativeColumn( 0, true );

        List<Object> column = gridA.getColumn( 1 );
        
        assertTrue( column.size() == 4 );
        assertTrue( column.contains( 10.0 ) );
        assertTrue( column.contains( 60.0 ) );
        assertTrue( column.contains( 80.0 ) );
        assertTrue( column.contains( 140.0 ) );
    }

    @Test
    public void testGetMetaColumnIndexes()
    {
        List<Integer> expected = new ArrayList<>();
        expected.add( 0 );
        expected.add( 1 );
        
        assertEquals( expected, gridA.getMetaColumnIndexes() );
    }

    @Test
    public void testGetUniqueValues()
    {
        gridA.addRow();
        gridA.addValue( 11 );
        gridA.addValue( 12 );
        gridA.addValue( 13 );
        
        Set<Object> expected = new HashSet<>();
        expected.add( 12 );
        expected.add( 22 );
        expected.add( 32 );
        expected.add( 42 );
        
        assertEquals( expected, gridA.getUniqueValues( "ColB" ) );
    }
    
    @Test
    public void testGetAsMap()
    {
        Map<String, Integer> map = gridA.getAsMap( 2, "-" );
        
        assertEquals( 4, map.size() );        
        assertEquals( Integer.valueOf( 13 ), map.get( "11-12" ) );
        assertEquals( Integer.valueOf( 23 ), map.get( "21-22" ) );
        assertEquals( Integer.valueOf( 33 ), map.get( "31-32" ) );
        assertEquals( Integer.valueOf( 43 ), map.get( "41-42" ) );
    }
    
    @Test
    public void testJRDataSource() throws Exception
    {
        assertTrue( gridA.next() );
        assertEquals( 11, gridA.getFieldValue( new MockJRField( "colA" ) ) );
        assertEquals( 12, gridA.getFieldValue( new MockJRField( "colB" ) ) );
        assertEquals( 13, gridA.getFieldValue( new MockJRField( "colC" ) ) );

        assertTrue( gridA.next() );
        assertEquals( 21, gridA.getFieldValue( new MockJRField( "colA" ) ) );
        assertEquals( 22, gridA.getFieldValue( new MockJRField( "colB" ) ) );
        assertEquals( 23, gridA.getFieldValue( new MockJRField( "colC" ) ) );

        assertTrue( gridA.next() );
        assertEquals( 31, gridA.getFieldValue( new MockJRField( "colA" ) ) );
        assertEquals( 32, gridA.getFieldValue( new MockJRField( "colB" ) ) );
        assertEquals( 33, gridA.getFieldValue( new MockJRField( "colC" ) ) );

        assertTrue( gridA.next() );
        assertEquals( 41, gridA.getFieldValue( new MockJRField( "colA" ) ) );
        assertEquals( 42, gridA.getFieldValue( new MockJRField( "colB" ) ) );
        assertEquals( 43, gridA.getFieldValue( new MockJRField( "colC" ) ) );
        
        assertFalse( gridA.next() );
    }

    @Test
    public void testAddValuesAsList()
    {
        Grid grid = new ListGrid();
        
        grid.addRow().addValuesAsList( Lists.newArrayList( "colA1", "colB1", "colC1" ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "colA2", "colB2", "colC2" ) );
        
        assertEquals( 2, grid.getHeight() );
        assertEquals( 3, grid.getWidth() );
        assertEquals( "colB1", grid.getRow( 0 ).get( 1 ) );
        assertEquals( "colC2", grid.getRow( 1 ).get( 2 ) );
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static List<Object> getList( Object... items )
    {
        List<Object> list = new ArrayList<>();
        
        for ( Object item : items )
        {
            list.add( item );
        }
        
        return list;
    }
}
