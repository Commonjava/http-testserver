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
package org.commonjava.test.http.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class TestHttpServerWithQuarkus
{
    @InjectExpected( base = "repos", port = 9090 )
    private ExpectationServer server;

    @Test
    public void simpleStatus()
            throws Exception
    {
        final String subPath = "/path/to/something.txt";
        final String url = server.formatUrl( subPath );
        server.expect( url, 200, "" );

        final HttpGet request = new HttpGet( url );
        final CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try
        {
            response = client.execute( request );
            assertThat( response.getStatusLine().getStatusCode(), is( 200 ) );
        }
        finally
        {
            if ( response != null && response.getEntity() != null )
            {
                EntityUtils.consumeQuietly( response.getEntity() );
                IOUtils.closeQuietly( response );
            }

            request.reset();

            if ( client != null )
            {
                IOUtils.closeQuietly( client );
            }
        }

    }

    @Test
    public void simpleDownload()
            throws Exception
    {

        final String subPath = "/path/to/something.txt";
        final String content = "this is the content";
        final String url = server.formatUrl( subPath );
        final String path = server.formatPath( subPath );
        server.expect( url, 200, content );

        final HttpGet request = new HttpGet( url );
        final CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( request );
            stream = response.getEntity().getContent();
            final String result = IOUtils.toString( stream );

            assertThat( result, notNullValue() );
            assertThat( result, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            if ( response != null && response.getEntity() != null )
            {
                EntityUtils.consumeQuietly( response.getEntity() );
                IOUtils.closeQuietly( response );
            }

            request.reset();

            if ( client != null )
            {
                IOUtils.closeQuietly( client );
            }
        }

        System.out.println( server.getAccessesByPathKey() );

        final String key = server.getAccessKey( CommonMethod.GET.name(), path );
        System.out.println( "Getting accesses for: '" + key + "'" );
        assertThat( server.getAccessesByPathKey().get( key ), equalTo( 1 ) );
    }

}
