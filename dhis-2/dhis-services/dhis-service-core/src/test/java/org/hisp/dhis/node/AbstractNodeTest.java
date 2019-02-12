package org.hisp.dhis.node;

import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractNodeTest
{

        @Test
        public void testEquals()
        {
                //Instantiating object 1
                ComplexNode complexNode1 = createComplexNode( "node1" );

                //Instantiating object 2
                ComplexNode complexNode2 = createComplexNode( "node1" );

                assertEquals( complexNode1, complexNode2);
        }


         @Test
        public void testNotEquals()
        {
                //Instantiating object 1
                ComplexNode complexNode1 = createComplexNode( "node1" );

                //Instantiating object 2
                ComplexNode complexNode2 = createComplexNode( "node2" );

                assertNotEquals( complexNode1, complexNode2);
        }



        private ComplexNode createComplexNode( String node1 )
        {
                ComplexNode complexNode1;
                        List<Node> children1 = new ArrayList<>();
                        children1.add( new SimpleNode( "id", node1 ) );
                        complexNode1 = new ComplexNode( "dataElement" );
                        complexNode1.setMetadata( false );
                        complexNode1.setChildren( children1 );

                        return complexNode1;
        }

}