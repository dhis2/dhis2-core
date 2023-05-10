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
package org.hisp.dhis.relationship;

import java.io.Serializable;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.SoftDeletableObject;

/**
 * @author Abyot Asalefew
 * @author Stian Sandvold
 */
@Auditable( scope = AuditScope.TRACKER )
public class Relationship
    extends SoftDeletableObject
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 3818815755138507997L;

    @AuditAttribute
    private RelationshipType relationshipType;

    @AuditAttribute
    private RelationshipItem from;

    @AuditAttribute
    private RelationshipItem to;

    private ObjectStyle style;

    private String formName;

    private String description;

    /**
     * The key is an aggregated representation of the relationship and its sides
     * based on uids. The format is type_from_to
     */
    private String key;

    /**
     * The inverted key is a key, but with the sides switched. This will make it
     * possible to match a key when it is bidirectional. the format is
     * type_to_from
     */
    private String invertedKey;

    public Relationship()
    {
    }

    /**
     * @return the relationshipType
     */
    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }

    /**
     * @param relationshipType the relationshipType to set
     */
    public void setRelationshipType( RelationshipType relationshipType )
    {
        this.relationshipType = relationshipType;
    }

    public ObjectStyle getStyle()
    {
        return style;
    }

    public void setStyle( ObjectStyle style )
    {
        this.style = style;
    }

    public String getFormName()
    {
        return formName;
    }

    public void setFormName( String formName )
    {
        this.formName = formName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public RelationshipItem getFrom()
    {
        return from;
    }

    public void setFrom( RelationshipItem from )
    {
        this.from = from;
    }

    public RelationshipItem getTo()
    {
        return to;
    }

    public void setTo( RelationshipItem to )
    {
        this.to = to;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getInvertedKey()
    {
        return invertedKey;
    }

    public void setInvertedKey( String invertedKey )
    {
        this.invertedKey = invertedKey;
    }

    @Override
    public String toString()
    {
        return "Relationship{" +
            "id=" + id +
            ", relationshipType=" + relationshipType +
            ", from=" + from +
            ", to=" + to +
            ", style=" + style +
            ", formName='" + formName + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
