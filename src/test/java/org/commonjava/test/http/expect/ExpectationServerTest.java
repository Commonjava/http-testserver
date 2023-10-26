/**
 * Copyright (C) 2015-2022 Red Hat, Inc. (http://github.com/Commonjava/http-testserver)
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.commonjava.test.http.common.CommonMethod;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExpectationServerTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    private final String subPath = "/path/to/something";

    @Test
    public void downloadWithQueryParams()
            throws Exception
    {
        final String path1 = subPath + "?version=1.0";
        final String path2 = subPath + "?version=2.0";

        final String url1 = server.formatUrl(path1);
        final String url2 = server.formatUrl(path2);

        final String content1 = "this is a test version 1";
        final String content2 = "this is a test version 2";

        server.expect(url1, 200, content1);
        server.expect(url2, 200, content2);

        String result = getHttpContent(url1);
        assertThat(result, equalTo(content1));

        result = getHttpContent(url2);
        assertThat(result, equalTo(content2));

    }

    private String getHttpContent(String url) throws IOException
    {
        final HttpGet request = new HttpGet( url );
        final CloseableHttpClient client = HttpClients.createDefault();

        try(CloseableHttpResponse response = client.execute( request ))
        {
            InputStream stream = response.getEntity().getContent();
            return IOUtils.toString( stream );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }
}
