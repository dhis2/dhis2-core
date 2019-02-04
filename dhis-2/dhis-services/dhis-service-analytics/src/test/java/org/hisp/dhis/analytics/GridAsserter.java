/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.common.Grid;

import com.google.common.base.Joiner;

/**
 * @author Luciano Fiandesio
 */
public class GridAsserter
{

    private final static String NULL = "_:";

    public static InnerGridAsserter addGrid( Grid grid )
    {
        InnerGridAsserter innerGridAsserter = new InnerGridAsserter();
        innerGridAsserter.grid = grid;
        return innerGridAsserter;
    }

    public static class KeyValue
    {
        private String key;

        private String value;

        private KeyValue( String key, String value )
        {
            this.key = key;
            this.value = value;
        }

        public static KeyValue tuple( String key, String val )
        {
            return new KeyValue( key, val == null ? NULL : val );
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }

    public static class InnerGridAsserter
    {
        private Grid grid;

        private Optional<Integer> rows = null;

        private Optional<Integer> metadataRows = Optional.empty();

        private Map<String, List<Object>> rowValues = new HashMap<>();

        private Map<String, Map<String, String>> metadataItemValues = new HashMap<>();

        public InnerGridAsserter rowsHasSize( int rowNum )
        {
            rows = Optional.of( rowNum );
            return this;
        }

        public InnerGridAsserter metadataItemsHasSize( int rowNum )
        {
            metadataRows = Optional.of( rowNum );
            return this;
        }

        public InnerGridAsserter rowWithUidContains( String uid, Object... values )
        {
            rowValues.put( uid, Arrays.asList( values ) );
            return this;
        }

        public InnerGridAsserter metadataItemWithUidHas( String uid, GridAsserter.KeyValue... values )
        {
            metadataItemValues.put( uid, Arrays.stream( values )
                .collect( Collectors.toMap( GridAsserter.KeyValue::getKey, GridAsserter.KeyValue::getValue ) ) );

            return this;
        }

        public void verify()
        {
            Map<String, Object> metaDataItemMap = (Map<String, Object>) grid.getMetaData().get( "items" );
            if ( !grid.getRows().isEmpty() )
            {
                assertEquals( "Provided row number does not match", rows.orElse( 0 ),
                    Integer.valueOf( grid.getRows().size() ) );
            }

            assertEquals( "Provided metadata row number does not match", metadataRows.orElse( 0 ),
                Integer.valueOf( metaDataItemMap != null ? metaDataItemMap.size() : 0 ) );

            checkGridValues(grid);

            checkMetadataItemsMap(grid);
        }

        private void checkGridValues(Grid grid) {
            // check values
            if ( !grid.getRows().isEmpty() )
            {
                Set<String> uids = rowValues.keySet();
                for ( String uid : uids )
                {
                    List<Object> row = findRowByUid( uid, grid.getRows() );
                    checkRowValues( rowValues.get( uid ), row );
                }
            }
        }

        private void checkMetadataItemsMap(Grid grid) {

            Map<String, Object> metaDataItemMap = (Map<String, Object>) grid.getMetaData().get( "items" );

            if ( metaDataItemMap != null && !metaDataItemMap.isEmpty() )
            {
                Set<String> uids = metadataItemValues.keySet();
                for ( String uid : uids )
                {
                    Object o = findMetadataRowByUid( uid, metaDataItemMap );
                    if ( o instanceof MetadataItem )
                    {
                        for ( String key : metadataItemValues.get( uid ).keySet() )
                        {
                            String val = metadataItemValues.get( uid ).get( key );
                            if ( val.equals( NULL ) )
                            {
                                assertThat( o, hasProperty( key, is( nullValue() ) ) );
                            }
                            else
                            {
                                assertThat( o, hasProperty( key, is( val ) ) );
                            }
                        }
                    }
                }
            }
        }

        private void checkRowValues( List<Object> expected, List<Object> actual )
        {
            if ( expected.size() != actual.size() )
            {
                fail( "Row value count does not match" );
            }
            if ( !(expected.containsAll( actual ) && actual.containsAll( expected )) )
            {
                fail( "Row values does not match" + "\n" + "expected: " + Joiner.on( "," ).join( expected ) + "\n"
                    + "actual: " + Joiner.on( "," ).join( actual ) + "\n" );
            }
        }

        private Object findMetadataRowByUid( String uid, Map<String, Object> metadataRows )
        {

            return metadataRows.get( uid );
        }

        private List<Object> findRowByUid( String uid, List<List<Object>> rows )
        {
            for ( List<Object> rowItems : rows )
            {
                for ( Object rowItem : rowItems )
                {
                    if ( rowItem.equals( uid ) )
                    {
                        rowItems.remove( uid );
                        return rowItems;
                    }
                }
            }
            fail( "cant find uid" );
            return null;
        }
    }
}
