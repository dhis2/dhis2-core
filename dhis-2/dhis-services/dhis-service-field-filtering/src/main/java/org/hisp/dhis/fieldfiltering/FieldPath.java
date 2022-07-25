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
package org.hisp.dhis.fieldfiltering;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen
 */
@Data
@RequiredArgsConstructor
public class FieldPath
{
    public static final String FIELD_PATH_SEPARATOR = ".";

    /**
     * Name of field (excluding path).
     */
    private final String name;

    /**
     * Path to reach field name, can be empty for root level fields.
     */
    private final List<String> path;

    /**
     * True if field path should be excluded (removed) from the set of paths to
     * include. In the API this is exposed as "?fields=id,name,!do_not_include"
     * where "!" marks the path as not to be included.
     */
    private final boolean exclude;

    /**
     * True if the field path should be handled as a preset path, which means we
     * have to expand before going into the filtering process. For example
     * ":owner" would be expanded to include all properties where
     * "property.owner=true".
     */
    private final boolean preset;

    /**
     * Transformers to apply to field, can be empty.
     */
    private final List<FieldPathTransformer> transformers;

    /**
     * Schema Property if present (added by {@link FieldPathHelper}).
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Property property;

    /**
     * Fully calculated dot separated path for FieldPath.
     */
    private String fullPath;

    public FieldPath( String name, List<String> path )
    {
        this.name = name;
        this.path = path;
        this.exclude = false;
        this.preset = false;
        this.transformers = new ArrayList<>();
    }

    public FieldPath( String name, List<String> path, boolean exclude, boolean preset )
    {
        this.name = name;
        this.path = path;
        this.exclude = exclude;
        this.preset = preset;
        this.transformers = new ArrayList<>();
    }

    /**
     * @return Dot separated path + field name (i.e. path.to.field)
     */
    public String toFullPath()
    {
        if ( fullPath == null )
        {
            fullPath = path.isEmpty() ? name : toPath() + FIELD_PATH_SEPARATOR + name;
        }

        return fullPath;
    }

    public String toPath()
    {
        return StringUtils.join( path, FIELD_PATH_SEPARATOR );
    }

    /**
     * @return true if we have at least one field path transformer
     */
    public boolean isTransformer()
    {
        return transformers != null && !transformers.isEmpty();
    }

    /**
     * @return true if name is the root of the path
     */
    public boolean isRoot()
    {
        return path.isEmpty();
    }
}
