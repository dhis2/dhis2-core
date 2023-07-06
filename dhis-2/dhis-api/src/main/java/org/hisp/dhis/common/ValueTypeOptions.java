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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;

/**
 * An abstract class that have sub classes representing any number of options for one or more
 * ValueTypes e.g. {@link ValueType}.FILE_RESOURCE
 *
 * <p>The sub classes will be serialized and persisted with Hibernate as a jsonb Postgres value
 * type, with the field <strong>'type'</strong> as the sub class discriminator field, see: {@link
 * JsonTypeInfo#property()} below.
 *
 * <p>See {@link org.hisp.dhis.hibernate.jsonb.type.SafeJsonBinaryType} for the implementation of
 * the custom Hibernate value type representing this object as the jsonb value type in Postgres.
 *
 * <p>To make a new option class, extend this class and add a {@link
 * com.fasterxml.jackson.annotation.JsonSubTypes.Type} line in the {@link JsonSubTypes#value()} list
 * in this file. Point to the implementation in the {@link JsonSubTypes.Type#value()} attribute and
 * what discriminator value it has in the {@link JsonSubTypes.Type#name()} attribute. The new option
 * class must be annotated with {@link JsonTypeName#value()} and have the same value as the: {@link
 * JsonSubTypes.Type#name()} attribute in this file.
 *
 * <p>See: {@link FileTypeValueOptions} for an example of a sub class of this file.
 *
 * <p>See {@link org.hisp.dhis.dataelement.DataElement} for an example of a class that uses
 * ValueTypeOptions to complement it's ValueType.
 *
 * @see FileTypeValueOptions
 * @see ValueType
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@JsonInclude()
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = FileTypeValueOptions.class),
})
@JacksonXmlRootElement(localName = "valueTypeOptions", namespace = DxfNamespaces.DXF_2_0)
public abstract class ValueTypeOptions implements Serializable {}
