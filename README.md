# Simulating remote HTTP servers for functional testing

This is a test fixture, which provides a very basic servlet that registers expected requests and logs access counts for each requested method/pathParts combination. If no expectation is registered for a particular method/pathParts, 404 is returned.

Usage is pretty simple: 

####For junit 4 Rule based

    @Rule
    public ExpectationServerRule serverRule = new ExpectationServerRule( "repos" );

    @Test
    public void run()
        throws Exception
    {
        final ExpectationServer server = serverRule.getServer();
        final String pathParts = "/repos/pathParts/to/something.txt";
        final String content = "this is the content";
        final String url = server.formatUrl( pathParts );
        server.expect( url, 200, content );
        // Do any assertions....
        .......
    }


####For junit 5 Extension Based:

    @ExtendWith(ExpectationServerExtension.class)
    pulic class ExpectaionTest{

        @Expected("repos")
        public ExpectationServer server;
     
        @Test
        public void run()
            throws Exception
        {
            final String pathParts = "/repos/pathParts/to/something.txt";
            final String content = "this is the content";
            final String url = server.formatUrl( pathParts );
            server.expect( url, 200, content );
            // Do any assertions....
            .......
        }
    }
or:

    pulic class ExpectaionTest{

        @RegisterExtension
        public ExpectationServerExtension extension = new ExpectationServerExtension("repos");
     
        @Test
        public void run()
            throws Exception
        {
            final ExpectationServer server = extension.getServer();
            final String pathParts = "/repos/pathParts/to/something.txt";
            final String content = "this is the content";
            final String url = server.formatUrl( pathParts );
            server.expect( url, 200, content );
            // Do any assertions....
            .......
        }
    }