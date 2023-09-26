/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/http-testserver)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.test.http.quarkus.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.quarkus.InjectExpected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectationServerCallBack
        implements QuarkusTestAfterConstructCallback, QuarkusTestAfterEachCallback, QuarkusTestBeforeEachCallback
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private static final Map<Object, ExpectationServer> injectServers = new ConcurrentHashMap<>();

    @Override
    public void afterConstruct( Object testInstance )
    {
        Class<?> current = testInstance.getClass();
        while ( current.getSuperclass() != null )
        {
            for ( Field field : current.getDeclaredFields() )
            {
                InjectExpected injectMockAnnotation = field.getAnnotation( InjectExpected.class );
                if ( injectMockAnnotation != null && field.getType().equals( ExpectationServer.class ) )
                {
                    final String baseResource = injectMockAnnotation.base();
                    final int port = injectMockAnnotation.port();
                    logger.trace( "Found field with @InjectExpected annotation, base resource is {}, port is {}",
                                  baseResource, port );

                    ExpectationServer server = new ExpectationServer( baseResource, port );
                    try
                    {
                        logger.trace( "Injecting the field {} with server instance", field.getName() );
                        field.setAccessible( true );
                        field.set( testInstance, server );
                        injectServers.put( testInstance, server );
                    }
                    catch ( IllegalAccessException e )
                    {
                        throw new RuntimeException( e );
                    }
                    return;
                }
            }
            current = current.getSuperclass();
        }
    }

    @Override
    public void beforeEach( QuarkusTestMethodContext context )
    {
        ExpectationServer server = injectServers.get( context.getTestInstance() );
        if ( server != null )
        {
            server.start();
        }
    }

    @Override
    public void afterEach( QuarkusTestMethodContext context )
    {
        ExpectationServer server = injectServers.get( context.getTestInstance() );
        if ( server != null )
        {
            server.stop();
        }
    }

}
