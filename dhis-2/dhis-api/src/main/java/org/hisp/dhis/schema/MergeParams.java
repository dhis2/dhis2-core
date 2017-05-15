package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hisp.dhis.common.MergeMode;

import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class MergeParams<T>
{
    private final T source;

    private final T target;

    private MergeMode mergeMode = MergeMode.REPLACE;

    private boolean skipSharing;

    public MergeParams( T source, T target )
    {
        this.source = Objects.requireNonNull( source );
        this.target = Objects.requireNonNull( target );
    }

    public T getSource()
    {
        return source;
    }

    public T getTarget()
    {
        return target;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public MergeParams<T> setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
        return this;
    }

    public boolean isSkipSharing()
    {
        return skipSharing;
    }

    public MergeParams<T> setSkipSharing( boolean skipSharing )
    {
        this.skipSharing = skipSharing;
        return this;
    }
}
