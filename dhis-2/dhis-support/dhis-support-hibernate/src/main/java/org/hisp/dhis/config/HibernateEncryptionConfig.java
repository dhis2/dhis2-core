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
package org.hisp.dhis.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;
import org.hisp.dhis.hibernate.encryption.HibernateEncryptorRegistry;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.StringFixedSaltGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class HibernateEncryptionConfig
{
    public static final String TRIPLE_DES_STRING_ENCRYPTOR = "tripleDesStringEncryptor";

    public static final String AES_128_STRING_ENCRYPTOR = "aes128StringEncryptor";

    @Autowired
    private HibernateConfigurationProvider hibernateConfigurationProvider;

    private String password;

    @PostConstruct
    public void init()
    {
        password = (String) getConnectionProperty( "encryption.password", "J7GhAs287hsSQlKd9g5" );
    }

    // Used only for SystemSettings (due to bug with JCE policy restrictions in
    // Jasypt)
    @Bean( TRIPLE_DES_STRING_ENCRYPTOR )
    public PooledPBEStringEncryptor tripleDesStringEncryptor()
    {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setAlgorithm( "PBEWithSHA1AndDESede" );
        encryptor.setPassword( password );
        encryptor.setPoolSize( 4 );
        encryptor.setSaltGenerator( new StringFixedSaltGenerator( "H7g0oLkEw3wf52fs52g3hbG" ) );
        return encryptor;
    }

    // AES string encryptor, requires BouncyCastle and JCE extended policy (due
    // to issue mentioned above)
    @Bean( AES_128_STRING_ENCRYPTOR )
    public PooledPBEStringEncryptor aes128StringEncryptor()
    {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setAlgorithm( "PBEWITHSHA256AND128BITAES-CBC-BC" );
        encryptor.setPassword( password );
        encryptor.setPoolSize( 4 );
        encryptor.setSaltGenerator( new RandomSaltGenerator() );
        return encryptor;
    }

    @Bean( "hibernateEncryptors" )
    public HibernateEncryptorRegistry hibernateEncryptors()
    {
        HibernateEncryptorRegistry registry = HibernateEncryptorRegistry.getInstance();
        registry.setEncryptors( ImmutableMap.of( AES_128_STRING_ENCRYPTOR, aes128StringEncryptor() ) );
        return registry;
    }

    @Bean
    public MethodInvokingFactoryBean addProvider()
    {
        MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
        methodInvokingFactoryBean.setStaticMethod( "java.security.Security.addProvider" );
        methodInvokingFactoryBean.setArguments( new BouncyCastleProvider() );
        return methodInvokingFactoryBean;
    }

    private Object getConnectionProperty( String key, String defaultValue )
    {
        String value = hibernateConfigurationProvider.getConfiguration().getProperty( key );

        return StringUtils.defaultIfEmpty( value, defaultValue );
    }
}
