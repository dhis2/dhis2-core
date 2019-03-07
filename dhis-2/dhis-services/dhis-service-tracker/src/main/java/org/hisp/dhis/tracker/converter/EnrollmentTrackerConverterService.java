package org.hisp.dhis.tracker.converter;

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

import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class EnrollmentTrackerConverterService
    implements TrackerConverterService<Enrollment, ProgramInstance>
{
    @Override
    public Enrollment to( ProgramInstance programInstance )
    {
        return null;
    }

    @Override
    public Enrollment to( TrackerPreheat preheat, ProgramInstance object )
    {
        return null;
    }

    @Override
    public List<Enrollment> to( List<ProgramInstance> programInstances )
    {
        return null;
    }

    @Override
    public List<Enrollment> to( TrackerPreheat preheat, List<ProgramInstance> programInstances )
    {
        return null;
    }

    @Override
    public ProgramInstance from( Enrollment enrollment )
    {
        return null;
    }

    @Override
    public ProgramInstance from( TrackerPreheat preheat, Enrollment object )
    {
        return null;
    }

    @Override
    public List<ProgramInstance> from( List<Enrollment> enrollments )
    {
        return null;
    }

    @Override
    public List<ProgramInstance> from( TrackerPreheat preheat, List<Enrollment> enrollments )
    {
        return null;
    }
}
