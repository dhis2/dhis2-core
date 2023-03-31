/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.hibernate;

import static java.util.Arrays.asList;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

/**
 * This class allow injecting {@link javax.persistence.EntityManager} using
 * Constructor.
 */
public class EntityManagerBeanDefinitionRegistrarPostProcessor implements BeanFactoryPostProcessor
{
    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory )
        throws BeansException
    {
        if ( !(beanFactory instanceof BeanDefinitionRegistry) )
        {
            return;
        }

        for ( String emfName : getEntityManagerFactoryBeanNames( beanFactory ) )
        {
            if ( emfName.equals( "sessionFactory" ) )
            {
                continue;
            }
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .rootBeanDefinition( "org.springframework.orm.jpa.SharedEntityManagerCreator" );
            builder.setFactoryMethod( "createSharedEntityManager" );
            builder.addConstructorArgReference( emfName );

            AbstractBeanDefinition emBeanDefinition = builder.getRawBeanDefinition();
            AbstractBeanDefinition emfBeanDefinition = (AbstractBeanDefinition) beanFactory
                .getBeanDefinition( emfName );

            emBeanDefinition.addQualifier( new AutowireCandidateQualifier( Qualifier.class, emfName ) );
            emBeanDefinition.setScope( emfBeanDefinition.getScope() );
            emBeanDefinition.setSource( emfBeanDefinition.getSource() );

            BeanDefinitionReaderUtils.registerWithGeneratedName( emBeanDefinition,
                (BeanDefinitionRegistry) beanFactory );
        }
    }

    /**
     * Return all bean names for bean definitions that will result in an
     * {@link EntityManagerFactory} eventually. We're checking for
     * {@link EntityManagerFactory} and the well-known factory beans here to
     * avoid eager initialization of the factory beans. The double lookup is
     * necessary especially for JavaConfig scenarios as people might declare an
     * {@link EntityManagerFactory} directly.
     *
     * @param beanFactory
     * @return
     */
    private static Iterable<String> getEntityManagerFactoryBeanNames( ListableBeanFactory beanFactory )
    {

        Set<String> names = new HashSet<String>();
        names.addAll(
            asList( beanNamesForTypeIncludingAncestors( beanFactory, EntityManagerFactory.class, true, false ) ) );

        for ( String factoryBeanName : beanNamesForTypeIncludingAncestors( beanFactory,
            AbstractEntityManagerFactoryBean.class, true, false ) )
        {
            names.add( factoryBeanName.substring( 1 ) );
        }

        return names;
    }
}
