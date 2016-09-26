package org.hisp.dhis.node.transformers;

/*
 * Copyright (c) 2004-2016, University of Oslo
 *  All rights reserved.
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

import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class SizeNodeTransformer implements NodeTransformer
{
    @Override
    public String name()
    {
        return "size";
    }

    @Override
    public Node transform( Node node, List<String> args )
    {
        checkNotNull( node );
        checkNotNull( node.getProperty() );

        Property property = node.getProperty();

        if ( property.isCollection() )
        {
            return new SimpleNode( property.getCollectionName(), node.getChildren().size(), property.isAttribute() );
        }
        else if ( property.is( PropertyType.TEXT ) || property.is( PropertyType.TEXT ) )
        {
            return new SimpleNode( property.getName(), ((String) ((SimpleNode) node).getValue()).length(), property.isAttribute() );
        }
        else if ( property.is( PropertyType.INTEGER ) || property.is( PropertyType.NUMBER ) )
        {
            return new SimpleNode( property.getName(), ((SimpleNode) node).getValue(), property.isAttribute() );
        }

        return node;
    }
}
