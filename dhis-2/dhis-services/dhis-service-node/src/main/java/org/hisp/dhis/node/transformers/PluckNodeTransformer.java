/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.node.transformers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.Property;
import org.springframework.stereotype.Component;

/**
 * Transforms a collection node with complex nodes to a list with the the first field values of the
 * includes simple nodes.
 *
 * @author Volker Schmidt
 */
@Component
public class PluckNodeTransformer implements NodeTransformer {
  @Override
  public String name() {
    return "pluck";
  }

  @Override
  public Node transform(Node node, List<String> args) {
    checkNotNull(node);
    checkNotNull(node.getProperty());

    Property property = node.getProperty();

    if (property.isCollection()) {
      final String fieldName =
          (args == null || args.isEmpty()) ? null : StringUtils.defaultIfEmpty(args.get(0), null);

      final CollectionNode collectionNode =
          new CollectionNode(node.getName(), node.getUnorderedChildren().size());
      collectionNode.setNamespace(node.getNamespace());

      for (final Node objectNode : node.getUnorderedChildren()) {
        for (final Node fieldNode : objectNode.getUnorderedChildren()) {
          if (fieldNode instanceof SimpleNode
              && (fieldName == null || fieldName.equals(fieldNode.getName()))) {
            final SimpleNode childNode =
                new SimpleNode(fieldNode.getName(), ((SimpleNode) fieldNode).getValue());
            childNode.setProperty(collectionNode.getProperty());
            collectionNode.addChild(childNode);

            break;
          }
        }
      }

      return collectionNode;
    }

    return node;
  }
}
