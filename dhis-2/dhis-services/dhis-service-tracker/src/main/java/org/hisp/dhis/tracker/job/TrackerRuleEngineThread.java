package org.hisp.dhis.tracker.job;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.programrule.engine.RuleActionImplementer;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * Class represents a thread which will be triggered as soon as tracker rule engine consumer consumes a message from
 * tracker rule engine queue.
 *
 * @author Zubair Asghar
 */
@Component
@Scope( BeanDefinition.SCOPE_PROTOTYPE )
public class TrackerRuleEngineThread extends SecurityContextRunnable
{
    private final List<RuleActionImplementer> ruleActionImplementers;

    private TrackerSideEffectDataBundle sideEffectDataBundle;

    public TrackerRuleEngineThread( List<RuleActionImplementer> ruleActionImplementers )
    {
        this.ruleActionImplementers = ruleActionImplementers;
    }

    @Override
    public void call()
    {
        if ( sideEffectDataBundle != null )
        {
            Map<Enrollment, List<RuleEffect>> enrollmentRuleEffects = sideEffectDataBundle.getEnrollmentRuleEffects();
            Map<Event, List<RuleEffect>> eventRuleEffects = sideEffectDataBundle.getEventRuleEffects();

            for ( RuleActionImplementer ruleActionImplementer : ruleActionImplementers )
            {
                for ( Map.Entry<Enrollment, List<RuleEffect>> entry : enrollmentRuleEffects.entrySet() )
                {
                    entry.getValue()
                            .parallelStream()
                            .filter( effect -> ruleActionImplementer.accept( effect.ruleAction() ) )
                            .forEach( effect -> ruleActionImplementer.implementEnrollmentAction( effect, entry.getKey().getEnrollment() ) );
                }

                for ( Map.Entry<Event, List<RuleEffect>> entry : eventRuleEffects.entrySet() )
                {
                    entry.getValue()
                            .parallelStream()
                            .filter( effect -> ruleActionImplementer.accept( effect.ruleAction() ) )
                            .forEach( effect -> ruleActionImplementer.implementEventAction( effect, entry.getKey().getEvent() ) );
                }
            }
        }
    }

    public void setSideEffectDataBundle( TrackerSideEffectDataBundle sideEffectDataBundle )
    {
        this.sideEffectDataBundle = sideEffectDataBundle;
    }
}
