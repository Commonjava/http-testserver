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
package org.commonjava.test.http.expect;

import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpectationServlet
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
        String br = baseResource;
        if ( br == null )
        {
            br = "/";
        }
        else if ( !br.startsWith( "/" ) )
        {
            br = "/" + br;
        }
        this.baseResource = br;
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
        final String key = getAccessKey( method, realPath );
        logger.info( "Registering error: {}, code: {}, body:\n{}", key, code, error );
        this.errors.put( key, new ContentResponse( method, realPath, code, error ) );
    }

    public String getAccessKey( final String method, final String path )
    {
        return method.toUpperCase() + " " + path;
    }

    private String getPath( final String testUrl )
    {
        try
        {
            return new URL( testUrl ).getFile(); // path plus query parameters
        }
        catch ( final MalformedURLException e )
        {
            //Do nothing
        }
        return testUrl;
    }

    public void expect( final String method, final String testUrl, final int responseCode, final String body )
    {
        final String path = getPath( testUrl );
        final String key = getAccessKey( method, path );
        logger.info( "Registering expectation: {}, code: {}, body:\n{}", key, responseCode, body );
        expectations.put( key, new ContentResponse( method, path, responseCode, body ) );
    }

    public void expect( final String method, final String testUrl, final int responseCode,
                        final InputStream bodyStream )
    {
        final String path = getPath( testUrl );

        final String key = getAccessKey( method, path );
        logger.info( "Registering expectation: {}, code: {}, body stream:\n{}", key, responseCode, bodyStream );
        expectations.put( key, new ContentResponse( method, path, responseCode, bodyStream ) );
    }

    public void expect( String method, String testUrl, ExpectationHandler handler )
    {
        final String path = getPath( testUrl );

        final String key = getAccessKey( method, path );
        logger.info( "Registering expectation: {}, handler: {}", key, handler );
        expectations.put( key, new ContentResponse( method, path, handler ) );
    }

    @Override
    protected void service( final HttpServletRequest req, final HttpServletResponse resp )
            throws ServletException, IOException
    {
        String wholePath = getWholePath( req );
        String key = getAccessKey( req.getMethod(), wholePath );
        accessesByPath.merge(key, 1, Integer::sum);

        boolean handled = handle( key, req, resp );
        if (!handled)
        {
            // try simple path
            String simplePath = getSimplePath(req);
            if (!simplePath.equals(wholePath))
            {
                key = getAccessKey(req.getMethod(), simplePath);
                handled = handle(key, req, resp);
            }
        }

        if (!handled)
        {
            resp.setStatus( 404 );
        }
    }

    private boolean handle(String key, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException
    {
        logger.info( "Looking for error: '{}' in:\n{}", key, errors );
        if ( errors.containsKey( key ) )
        {
            final ContentResponse error = errors.get( key );
            logger.error( "Returning registered error: {}", error );

            if ( error.handler() != null )
            {
                error.handler().handle( req, resp );
            }
            else if ( error.body() != null )
            {
                resp.sendError( error.code(), error.body() );
            }
            else if ( error.bodyStream() != null )
            {
                resp.setStatus( error.code() );
                IOUtils.copy( error.bodyStream(), resp.getOutputStream() );
            }
            else
            {
                resp.sendError( error.code() );
            }
            return true;
        }

        logger.info( "Looking for expectation: '{}'", key );
        if ( expectations.containsKey( key ) )
        {
            final ContentResponse expectation = expectations.get( key );
            logger.info( "Responding via registered expectation: {}", expectation );

            if ( expectation.handler() != null )
            {
                expectation.handler().handle( req, resp );
                logger.info( "Using handler..." );
            }
            else if ( expectation.body() != null )
            {
                resp.setStatus( expectation.code() );

                logger.info( "Set status: {} with body string", expectation.code() );
                resp.getWriter().write( expectation.body() );
            }
            else if ( expectation.bodyStream() != null )
            {
                resp.setStatus( expectation.code() );

                logger.info( "Set status: {} with body InputStream", expectation.code() );
                IOUtils.copy( expectation.bodyStream(), resp.getOutputStream() );
            }
            else
            {
                resp.setStatus( expectation.code() );
                logger.info( "Set status: {} with no body", expectation.code() );
            }
            return true;
        }

        return false;
    }

    /**
     * Get path with query string.
     */
    private String getWholePath( HttpServletRequest request )
    {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        if ( queryString == null )
        {
            return requestURI;
        }
        else
        {
            return requestURI + "?" + queryString;
        }
    }

    private String getSimplePath( HttpServletRequest request )
    {
        return request.getRequestURI();
    }

    public String getAccessKey( final CommonMethod method, final String path )
    {
        return getAccessKey( method.name(), path );
    }

    public Integer getAccessesFor( final String path )
    {
        return accessesByPath.get( getAccessKey( CommonMethod.GET, path ) );
    }

    public Integer getAccessesFor( final String method, final String path )
    {
        return accessesByPath.get( getAccessKey( method, path ) );
    }

}