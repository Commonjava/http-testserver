/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (http://github.com/Commonjava/http-testserver)
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
package org.commonjava.test.http.junit5;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.junit5.annotations.Expected;
import org.commonjava.test.http.junit5.expect.ExpectationServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith( ExpectationServerExtension.class )
public class TestHttpServerExtendWithTest
{
    //    @RegisterExtension
    @Expected( base = "repos" )
    private ExpectationServer server;

    @Test
    public void simpleDownload()
            throws Exception
    {
        // final ExpectationServer server = expected.getServer();
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

            if ( request != null )
            {
                request.reset();
            }

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
