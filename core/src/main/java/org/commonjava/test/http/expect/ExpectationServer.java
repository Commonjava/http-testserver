/**
 * Copyright (C) 2011-2024 Red Hat, Inc. (https://github.com/Commonjava/http-testserver)
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
package org.commonjava.test.http.expect;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.common.HttpServerFixture;
import org.commonjava.test.http.util.PortFinder;
import org.commonjava.test.http.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings( "unused" )
public class ExpectationServer
        implements HttpServerFixture<ExpectationServer>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Integer port;

    private final ExpectationServlet servlet;

    private Undertow server;

    public ExpectationServer()
    {
        this( null );
    }

    public ExpectationServer( final int port )
    {
        this( null, port );
    }

    public ExpectationServer( final String baseResource )
    {
        servlet = new ExpectationServlet( baseResource );
    }

    public ExpectationServer( final String baseResource, final int port )
    {
        servlet = new ExpectationServlet( baseResource );
        if ( port >= 80 )
        {
            this.port = port;
        }
    }

    public int getPort()
    {
        return port;
    }

    public void stop()
    {
        if ( server != null )
        {
            server.stop();
            logger.info( "STOPPED Test HTTP Server on 127.0.0.1:" + port );
        }
    }

    public ExpectationServer start()
    {
        final ServletInfo si = Servlets.servlet( "TEST", ExpectationServlet.class )
                                       .addMapping( "*" )
                                       .addMapping( "/*" )
                                       .setLoadOnStartup( 1 );

        si.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        final DeploymentInfo di = new DeploymentInfo().addServlet( si )
                                                      .setDeploymentName( "TEST" )
                                                      .setContextPath( "/" )
                                                      .setClassLoader( Thread.currentThread().getContextClassLoader() );

        final DeploymentManager dm = Servlets.defaultContainer().addDeployment( di );
        dm.deploy();

        boolean started = false;
        if ( this.port != null && this.port >= 80 )
        {
            try
            {
                server = Undertow.builder().setHandler( dm.start() ).addHttpListener( this.port, "127.0.0.1" ).build();
                server.start();
                started = true;
            }
            catch ( Exception e )
            {
                logger.warn( "The port {} is occupied, will find another port to try!", this.port );
            }
        }
        if ( !started )
        {
            final AtomicReference<Integer> foundPort = new AtomicReference<>();
            server = PortFinder.findPortFor( 16, p -> {
                foundPort.set( p );
                try
                {
                    Undertow s = Undertow.builder().setHandler( dm.start() ).addHttpListener( p, "127.0.0.1" ).build();
                    s.start();

                    return s;
                }
                catch ( ServletException e )
                {
                    throw new IOException( "Failed to start: " + e.getMessage(), e );
                }
            } );

            this.port = foundPort.get();
        }

        logger.info( "STARTED Test HTTP Server on 127.0.0.1:{}", port );

        return this;
    }

    public String formatUrl( final String... subpath )
    {
        try
        {
            return UrlUtils.buildUrl( "http://127.0.0.1:" + port, servlet.getBaseResource(), subpath );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to build url to: " + Arrays.toString( subpath ), e );
        }
    }

    public String formatPath( final String... subpath )
    {
        try
        {
            return UrlUtils.buildPath( servlet.getBaseResource(), subpath );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to build url to: " + Arrays.toString( subpath ), e );
        }
    }

    public String getBaseUri()
    {
        try
        {
            return UrlUtils.buildUrl( "http://127.0.0.1:" + port, servlet.getBaseResource() );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to build base-URI.", e );
        }
    }

    public String getUrlPath( final String url )
            throws MalformedURLException
    {
        return new URL( url ).getPath();
    }

    public Map<String, Integer> getAccessesByPathKey()
    {
        return servlet.getAccessesByPath();
    }

    public Map<String, ContentResponse> getRegisteredErrors()
    {
        return servlet.getRegisteredErrors();
    }

    public void registerException( final String url, final String error )
    {
        servlet.registerException( "HEAD", url, 500, error );
        servlet.registerException( "GET", url, 500, error );
    }

    public void registerException( final String method, final String url, final String error )
    {
        servlet.registerException( method, url, 500, error );
    }

    public void registerException( final String url, final String error, final int responseCode )
    {
        servlet.registerException( "HEAD", url, responseCode, error );
        servlet.registerException( "GET", url, responseCode, error );
    }

    public void registerException( final String method, final String url, final int responseCode, final String error )
    {
        servlet.registerException( method, url, responseCode, error );
    }

    public void expect( final String testUrl, final int responseCode, final String body )
            throws Exception
    {
        servlet.expect( "GET", testUrl, responseCode, body );
        servlet.expect( "HEAD", testUrl, responseCode, (String) null );
    }

    public void expect( final String method, final String testUrl, final int responseCode, final String body )
            throws Exception
    {
        servlet.expect( method, testUrl, responseCode, body );
    }

    public void expect( final String testUrl, final int responseCode, final InputStream bodyStream )
            throws Exception
    {
        servlet.expect( "GET", testUrl, responseCode, bodyStream );
        servlet.expect( "HEAD", testUrl, responseCode, (String) null );
    }

    public void expect( final String method, final String testUrl, final int responseCode,
                        final InputStream bodyStream )
            throws Exception
    {
        servlet.expect( method, testUrl, responseCode, bodyStream );
    }

    public void expect( final String method, final String testUrl, ExpectationHandler handler )
    {
        servlet.expect( method, testUrl, handler );
    }

    public String getAccessKey( final CommonMethod method, final String path )
    {
        return servlet.getAccessKey( method, path );
    }

    public String getAccessKey( final String method, final String path )
    {
        return servlet.getAccessKey( method, path );
    }

    public Integer getAccessesFor( final String path )
    {
        return servlet.getAccessesFor( path );
    }

    public Integer getAccessesFor( final String method, final String path )
    {
        return servlet.getAccessesFor( method, path );
    }
}
