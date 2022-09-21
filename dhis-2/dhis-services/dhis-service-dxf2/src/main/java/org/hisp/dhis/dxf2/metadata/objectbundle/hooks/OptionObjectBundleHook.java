/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

<<<<<<< HEAD
=======
import java.util.ArrayList;
import java.util.List;

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.springframework.stereotype.Component;

/**
 * @author Volker Schmidt
 */
@Component
public class OptionObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errors = new ArrayList<>();

<<<<<<< HEAD
        if ( !(object instanceof Option) ) return new ArrayList<>();
=======
        if ( !(object instanceof Option) )
            return new ArrayList<>();
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        final Option option = (Option) object;

        if ( option.getOptionSet() != null )
        {
            OptionSet optionSet = bundle.getPreheat().get( bundle.getPreheatIdentifier(), OptionSet.class,
                option.getOptionSet() );

            errors.addAll( checkDuplicateOption( optionSet, option ) );
        }

        return errors;
    }

    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        if ( !(object instanceof Option) )
        {
            return;
        }

        final Option option = (Option) object;

<<<<<<< HEAD
        // if the bundle contains also the option set there is no need to add the option here
        // (will be done automatically later and option set may contain raw value already)
=======
        // if the bundle contains also the option set there is no need to add
        // the option here
        // (will be done automatically later and option set may contain raw
        // value already)
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        if ( option.getOptionSet() != null && !bundle.containsObject( option.getOptionSet() ) )
        {
<<<<<<< HEAD
            OptionSet optionSet = bundle.getPreheat().get( bundle.getPreheatIdentifier(), OptionSet.class, option.getOptionSet() );
=======
            OptionSet optionSet = bundle.getPreheat().get( bundle.getPreheatIdentifier(), OptionSet.class,
                option.getOptionSet() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

            if ( optionSet != null )
            {
                optionSet.addOption( option );
            }
        }
    }

    /**
     * Check for duplication of Option's name OR code within given OptionSet
     *
     * @param listOptions
     * @param checkOption
     * @return
     */
    private List<ErrorReport> checkDuplicateOption( OptionSet optionSet, Option checkOption )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( optionSet == null || optionSet.getOptions().isEmpty() || checkOption == null )
        {
            return errors;
        }

        for ( Option option : optionSet.getOptions() )
        {
            if ( option == null || option.getName() == null || option.getCode() == null )
            {
                continue;
            }

<<<<<<< HEAD
            if ( ObjectUtils.allNotNull( option.getUid(), checkOption.getUid() ) && option.getUid().equals( checkOption.getUid() ) )
=======
            if ( ObjectUtils.allNotNull( option.getUid(), checkOption.getUid() )
                && option.getUid().equals( checkOption.getUid() ) )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
            {
                continue;
            }

            if ( option.getName().equals( checkOption.getName() ) || option.getCode().equals( checkOption.getCode() ) )
            {
                errors.add( new ErrorReport( OptionSet.class, ErrorCode.E4028, optionSet.getUid(), option.getUid() ) );
            }
        }

        return errors;
    }
}