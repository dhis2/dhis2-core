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
package org.hisp.dhis.node.types;

import java.util.Objects;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeType;
import org.hisp.dhis.node.exception.InvalidTypeException;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @deprecated No new usage of this class and its children should happen, we should instead directly
 *     use Jackson ObjectMappers or Jackson object factory if we need dynamically created objects.
 */
@Deprecated
public class SimpleNode extends AbstractNode {
  /** Value of this node. */
  private final Object value;

  /** Is this node considered a attribute. */
  private boolean attribute;

  public SimpleNode(String name, Object value) {
    super(name, NodeType.SIMPLE);
    this.value = value;
    this.attribute = false;
  }

  public SimpleNode(String name, Property property, Object value) {
    super(name, NodeType.SIMPLE);
    this.value = value;
    this.attribute = property.isAttribute();
    this.namespace = property.getNamespace();
    this.property = property;
  }

  public SimpleNode(String name, Object value, boolean attribute) {
    this(name, value);
    this.attribute = attribute;
  }

  public Object getValue() {
    return value;
  }

  public boolean isAttribute() {
    return attribute;
  }

  public void setAttribute(boolean attribute) {
    this.attribute = attribute;
  }

  @Override
  public <T extends Node> T addChild(T child) {
    throw new InvalidTypeException();
  }

  @Override
  public <T extends Node> void addChildren(Iterable<T> children) {
    throw new InvalidTypeException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    SimpleNode that = (SimpleNode) o;

    return attribute == that.attribute && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }
}
