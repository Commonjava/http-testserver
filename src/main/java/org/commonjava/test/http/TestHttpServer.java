/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.test.http;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.util.PortFinder;
import org.commonjava.test.http.util.UrlUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHttpServer
    extends ExternalResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Integer port;

    private final ExpectationServlet servlet;

    private Undertow server;

    public TestHttpServer()
    {
        this( null );
    }

    public TestHttpServer( final String baseResource )
    {
        servlet = new ExpectationServlet( baseResource );
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public void after()
    {
        if ( server != null )
        {
            server.stop();
            logger.info( "STOPPED Test HTTP Server on 127.0.0.1:" + port );
        }
    }

    @Override
    public void before()
        throws Exception
    {
        final ServletInfo si = Servlets.servlet( "TEST", ExpectationServlet.class )
                                       .addMapping( "*" )
                                       .addMapping( "/*" )
                                       .setLoadOnStartup( 1 );

        si.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        final DeploymentInfo di = new DeploymentInfo().addServlet( si )
                                                      .setDeploymentName( "TEST" )
                                                      .setContextPath( "/" )
                                                      .setClassLoader( Thread.currentThread()
                                                                             .getContextClassLoader() );

        final DeploymentManager dm = Servlets.defaultContainer()
                                             .addDeployment( di );
        dm.deploy();

        port = PortFinder.findOpenPort( 16 );
        server = Undertow.builder()
                         .setHandler( dm.start() )
                         .addHttpListener( port, "127.0.0.1" )
                         .build();

        server.start();
        logger.info( "STARTED Test HTTP Server on 127.0.0.1:" + port );
    }

    public static final class ExpectationServlet
        extends HttpServlet
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private static final long serialVersionUID = 1L;

        private final String baseResource;

        private final Map<String, ContentResponse> expectations = new HashMap<>();

        private final Map<String, Integer> accessesByPath = new HashMap<>();

        private final Map<String, ContentResponse> errors = new HashMap<>();

        public ExpectationServlet()
        {
            logger.error( "Default constructor not actually supported!!!" );
            this.baseResource = "/";
        }

        public ExpectationServlet( final String baseResource )
        {
            this.baseResource = baseResource == null ? "/" : baseResource;
        }

        public Map<String, Integer> getAccessesByPath()
        {
            return accessesByPath;
        }

        public Map<String, ContentResponse> getRegisteredErrors()
        {
            return errors;
        }

        public String getBaseResource()
        {
            return baseResource;
        }

        public void registerException( final String method, final String path, final int code, final String error )
        {
            final String realPath = getPath( path );
            final String key = method.toUpperCase() + " " + realPath;
            logger.info( "Registering error: {}, code: {}, body:\n{}", key, code, error );
            this.errors.put( key, new ContentResponse( method, realPath, code, error ) );
        }

        private String getPath( final String path )
        {
            String realPath = path;
            try
            {
                final URL u = new URL( path );
                realPath = u.getPath();
            }
            catch ( final MalformedURLException e )
            {
            }

            return realPath;
        }

        public void expect( final String method, final String testUrl, final int responseCode, final String body )
            throws Exception
        {
            final String path = getPath( testUrl );
            final String key = method.toUpperCase() + " " + path;
            logger.info( "Registering expectation: {}, code: {}, body:\n{}", key, responseCode, body );
            expectations.put( key, new ContentResponse( method, path, responseCode, body ) );
        }

        public void expect( final String method, final String testUrl, final int responseCode,
                            final InputStream bodyStream )
            throws Exception
        {
            final String path = getPath( testUrl );

            final String key = method.toUpperCase() + " " + path;
            logger.info( "Registering expectation: {}, code: {}, body stream:\n{}", key, responseCode, bodyStream );
            expectations.put( key, new ContentResponse( method, path, responseCode,
                                                                                      bodyStream ) );
        }

        @Override
        protected void service( final HttpServletRequest req, final HttpServletResponse resp )
            throws ServletException, IOException
        {
            String wholePath;
            try
            {
                wholePath = new URI( req.getRequestURI() ).getPath();
            }
            catch ( final URISyntaxException e )
            {
                throw new ServletException( "Cannot parse request URI", e );
            }

            String path = wholePath;
            if ( path.length() > 1 )
            {
                path = path.substring( 1 );
            }

            final String method = req.getMethod()
                                     .toUpperCase();

            final String key = method + " " + wholePath;

            logger.info( "Looking up expectation for: {}", key );

            final Integer i = accessesByPath.get( key );
            if ( i == null )
            {
                accessesByPath.put( key, 1 );
            }
            else
            {
                accessesByPath.put( key, i + 1 );
            }

            logger.info( "Looking for error: '{}' in:\n{}", key, errors );
            if ( errors.containsKey( key ) )
            {
                final ContentResponse error = errors.get( key );
                logger.error( "Returning registered error: {}", error );
                resp.sendError( error.code() );

                if ( error.body() != null )
                {
                    resp.getWriter()
                        .write( error.body() );
                }

                return;
            }

            logger.info( "Looking for expectation: '{}'", key );
            final ContentResponse expectation = expectations.get( key );
            if ( expectation != null )
            {
                logger.info( "Responding via registered expectation: {}", expectation );

                resp.setStatus( expectation.code() );

                if ( expectation.body() != null )
                {
                    resp.getWriter()
                        .write( expectation.body() );
                }
                else if ( expectation.bodyStream() != null )
                {
                    IOUtils.copy( expectation.bodyStream(), resp.getOutputStream() );
                }

                return;
            }

            resp.setStatus( 404 );
        }

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

    public Map<String, Integer> getAccessesByPath()
    {
        return servlet.getAccessesByPath();
    }

    public Map<String, ContentResponse> getRegisteredErrors()
    {
        return servlet.getRegisteredErrors();
    }

    public void registerException( final String url, final String error )
    {
        servlet.registerException( "GET", url, 500, error );
    }

    public void registerException( final String method, final String url, final String error )
    {
        servlet.registerException( method, url, 500, error );
    }

    public void registerException( final String url, final String error, final int responseCode )
    {
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

    public void expect( final String method, final String testUrl, final int responseCode, final InputStream bodyStream )
        throws Exception
    {
        servlet.expect( method, testUrl, responseCode, bodyStream );
    }

}
