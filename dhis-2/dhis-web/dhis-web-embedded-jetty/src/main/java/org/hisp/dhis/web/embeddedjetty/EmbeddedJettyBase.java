package org.hisp.dhis.web.embeddedjetty;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnectionStatistics;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

public abstract class EmbeddedJettyBase
{

    // Startup variable constants
    private static final String PUBLIC_PATH_KEY = "publicPath";

    // OBS: Important to not set the 'log' variable here to static,
    // this will cause logback to boot before default properties set in
    // setDefaultProperties()
    private final Logger log = LoggerFactory.getLogger( EmbeddedJettyBase.class );

    private static final String DEFAULT_PROFILE = "dev";

    private static final int REQUIRED_KEY_LENGTH = 256;

    // Config dir MUST end with a slash (/)
    private static final String DEFAULT_CONFIG_DIR = "config/";

    private static final String DEV_DEFAULT_CONFIG_DIR = "src/test/resources/";

    public EmbeddedJettyBase()
    {
        Thread.currentThread().setUncaughtExceptionHandler( EmbeddedJettyUncaughtExceptionHandler.systemExit( log ) );
        canonicalizePublicPath();
    }

    private static void canonicalizePublicPath()
    {
        String publicPath = System.getProperty( PUBLIC_PATH_KEY, "" ).trim();
        if ( publicPath.isEmpty() || "/".equals( publicPath ) )
        {
            // Set to NULL
            System.clearProperty( PUBLIC_PATH_KEY );
        }
        else
        {
            if ( publicPath.endsWith( "/" ) )
            {
                publicPath = publicPath.substring( 0, publicPath.length() - 1 );
            }
            if ( publicPath.startsWith( "/" ) )
            {
                publicPath = publicPath.substring( 1 );
            }

            System.setProperty( PUBLIC_PATH_KEY, "/" + publicPath + "/" );
        }
    }

    public String getPublicPath()
    {
        return System.getProperty( PUBLIC_PATH_KEY, "/" );
    }

    public static String getDefaultConfigDir()
    {
        if ( !Files.isDirectory( Paths.get( DEFAULT_CONFIG_DIR ) ) )
        {
            return DEV_DEFAULT_CONFIG_DIR;
        }
        else
        {
            return DEFAULT_CONFIG_DIR;
        }
    }

    public void startJetty()
        throws Exception
    {
        checkCipherKeyLength();

        // MeterRegistry meterRegistry = statsDMeterRegistry();
        // addMetrics(meterRegistry);

        Integer queueSize = getIntSystemProperty( "jetty.thread.queue", 6000 );
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( queueSize );

        Integer maxThreads = getIntSystemProperty( "jetty.threads.max", 200 );
        QueuedThreadPool threadPool = new InstrumentedQueuedThreadPool(
            null,
            maxThreads,
            10,
            60000,
            queue );

        threadPool.setDetailedDump( getBooleanSystemProperty( "jetty.detailedDump", false ) );

        Server server = new Server( threadPool );
        server.addBean( new org.eclipse.jetty.util.thread.ScheduledExecutorScheduler() );

        server.setHandler( getServletContextHandler() );

        // configureConnectionStatistics(meterRegistry, server);

        final HttpConfiguration http_config = getHttpConfiguration();

        addHttpConnector( server, http_config );
//        addSslConnector( server, http_config );

        server.setStopAtShutdown( true );
        server.setStopTimeout( 5000 );
        server.setDumpBeforeStop( getBooleanSystemProperty( "jetty.dumpBeforeStop", false ) );
        server.setDumpAfterStart( getBooleanSystemProperty( "jetty.dumpBeforeStart", false ) );

        server.start();
        server.join();


        log.info( "DHIS2 Server stopped!" );
    }

    // private Properties loadProperties() throws IOException {
    // Properties properties = new Properties();
    // String statsdProperties = getDefaultConfigDir() + CONFIG_FILE;
    // if (Files.exists(Paths.get(statsdProperties))) {
    // try (FileInputStream inputStream = new FileInputStream(statsdProperties))
    // {
    // properties.loadFromXML(inputStream);
    // }
    // }
    // return properties;
    // }

    private void configureConnectionStatistics( MeterRegistry meterRegistry, Server server )
    {
        StatisticsHandler statisticsHandler = new StatisticsHandler();
        statisticsHandler.setHandler( server.getHandler() );
        server.setHandler( statisticsHandler );
        ServerConnectionStatistics.addToAllConnectors( server );

        JettyStatisticsMetrics.monitor( meterRegistry, statisticsHandler );
    }

    // private MeterRegistry statsDMeterRegistry() throws IOException {
    // Properties properties = loadProperties();
    //
    // StatsdConfig statsdConfig = new StatsdConfig() {
    // @Override
    // public String get(@NotNull String key) {
    // return properties.getProperty(key);
    // }
    //
    // @Override
    // public boolean enabled() {
    // String value = get(prefix() + ".enabled");
    // return value != null && Boolean.valueOf(value);
    // }
    // };
    //
    // MeterRegistry registry = new StatsdMeterRegistry(statsdConfig,
    // Clock.SYSTEM);
    // Set<Tag> tags = Sets.newHashSet(
    // Tag.of("hostname", System.getProperty("jetty.host")),
    // Tag.of("server_version", "latest"),
    // Tag.of("service_name", "Dhis2")
    // );
    // registry.config().commonTags(tags);
    // ((StatsdMeterRegistry) registry).start();
    //
    // // Add to global registry so the static initializers can be used
    // Metrics.addRegistry(registry);
    // return registry;
    // }

    private void addMetrics( MeterRegistry registry )
    {
        new ClassLoaderMetrics().bindTo( registry );
        new JvmMemoryMetrics().bindTo( registry );
        new JvmGcMetrics().bindTo( registry );
        new JvmThreadMetrics().bindTo( registry );
        new ProcessorMetrics().bindTo( registry );
    }

    private void addSslConnector( Server server, HttpConfiguration http_config )
    {
        setDefaultPropertyValue( "jetty.port", System.getProperty( "jetty.ssl.port" ) );
        server.addConnector( setupSSLConnector(
            server, http_config,
            getStringSystemProperty( "jetty.keystore", null ),
            getStringSystemProperty( "jetty.keystore.pw", null ),
            getStringSystemProperty( "jetty.keyManager.pw", null ),
            getStringSystemProperty( "jetty.truststore", null ),
            getStringSystemProperty( "jetty.truststore.pw", null ) ) );
    }

    private void addHttpConnector( Server server, HttpConfiguration http_config )
    {
        setDefaultPropertyValue( "jetty.port", System.getProperty( "jetty.http.port" ) );
        server.addConnector( setupHTTPConnector( server, http_config ) );
    }

    public void setResourceBase( ServletContextHandler context, String webResourceRootPath )
    {
        URL webRootLocation = this.getClass().getResource( webResourceRootPath );
        if ( webRootLocation == null )
        {
            throw new IllegalStateException( "Unable to determine webroot URL location; path=" + webResourceRootPath );
        }
        context.setBaseResource( Resource.newResource( webRootLocation ) );
    }

    public static String getDefaultSpringProfile()
    {
        return DEFAULT_PROFILE;
    }

    protected void printBanner( String name )
    {
        String msg = "Starting: " + name;
        try (
            final InputStream resourceStream = new FileInputStream( System.getProperty( "config.dir" ) + "banner.txt" );
            final InputStreamReader inputStreamReader = new InputStreamReader( resourceStream, "UTF-8" );
            final BufferedReader bufferedReader = new BufferedReader( inputStreamReader ) )
        {
            final String banner = bufferedReader
                .lines()
                .collect( Collectors.joining( String.format( "%n" ) ) );
            msg = String.format( "Starting: %n%s", banner );
        }
        catch ( IllegalArgumentException | IOException ignored )
        {
        }
        log.info( msg );
    }

    public void checkCipherKeyLength()
        throws Exception
    {
        if ( !(Cipher.getMaxAllowedKeyLength( "AES" ) >= REQUIRED_KEY_LENGTH) )
        {
            throw new Exception( "Java Virtual Machine error! The current Java Virtual Machine doesn't "
                + "support the crypto cipher key length required by the DHIS2 software. "
                + "Tip: You probably need to install the 'Java Cryptography Extension'." );
        }
    }

    public static void setDefaultSSLProperties( String keystorePath, String trustStorePath, String keystorePw,
        String keyManagerPw, String trustStorePw )
    {
        setDefaultPropertyValue( "jetty.keyManager.pw", keyManagerPw );
        setDefaultPropertyValue( "jetty.keystore", keystorePath );
        setDefaultPropertyValue( "jetty.keystore.pw", keystorePw );
        setDefaultPropertyValue( "jetty.truststore", trustStorePath );
        setDefaultPropertyValue( "jetty.truststore.pw", trustStorePw );
    }

    public static Boolean getBooleanSystemProperty( String key, Boolean defaultValue )
    {
        Preconditions.checkNotNull( key, "Key can not be NULL!" );
        return Boolean.valueOf( System.getProperty( key, defaultValue.toString() ) );
    }

    public static String getStringSystemProperty( String key, String defaultValue )
    {
        Preconditions.checkNotNull( key, "'key' can not be NULL!" );
        return System.getProperty( key, defaultValue );
    }

    public static Integer getIntSystemProperty( String key, Integer defaultValue )
    {
        Preconditions.checkNotNull( key, "Key can not be NULL!" );
        return Integer.valueOf( System.getProperty( key, String.valueOf( defaultValue ) ) );
    }

    // Sets property value to defaultValue if property is not set... i.e. is
    // NULL
    public static void setDefaultPropertyValue( String key, String defaultValue )
    {
        Preconditions.checkNotNull( key );
        Preconditions.checkNotNull( defaultValue );
        System.setProperty( key, System.getProperty( key, defaultValue ) );
    }

    private HttpConfiguration getHttpConfiguration()
    {
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme( "https" );
        http_config.setSecurePort( getIntSystemProperty( "jetty.ssl.port", -1 ) );
        http_config.setOutputBufferSize( 32768 );
        http_config.setRequestHeaderSize( 8192 );
        http_config.setResponseHeaderSize( 8192 );
        http_config.setSendServerVersion( true );
        http_config.setSendDateHeader( false );
        http_config.setHeaderCacheSize( 512 );
        return http_config;
    }

    private Connector setupHTTPConnector( Server server, HttpConfiguration http_config )
    {
        ServerConnector httpConnector = new ServerConnector(
            server,
            getIntSystemProperty( "jetty.http.acceptors", -1 ),
            getIntSystemProperty( "jetty.http.selectors", -1 ),
            new HttpConnectionFactory( http_config ) );

        httpConnector.setHost( getStringSystemProperty( "jetty.host", null ) );
        httpConnector.setPort( getIntSystemProperty( "jetty.http.port", -1 ) );
        httpConnector.setIdleTimeout( getIntSystemProperty( "jetty.http.idleTimeout", 300000 ) );

        return httpConnector;
    }

    private Connector setupSSLConnector(
        Server server,
        HttpConfiguration http_config,
        String keyStorePath,
        String keyStorePassword,
        String keyManagerPassword,
        String trustStorePath,
        String trustStorePassword )
    {

        // SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath( keyStorePath );
        sslContextFactory.setKeyStorePassword( keyStorePassword );
        sslContextFactory.setKeyManagerPassword( keyManagerPassword );
        sslContextFactory.setTrustStorePath( trustStorePath );
        sslContextFactory.setTrustStorePassword( trustStorePassword );
        sslContextFactory.setIncludeCipherSuites(
            ".*_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            ".*_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            ".*_DHE_RSA_WITH_AES_256_GCM_SHA384",
            ".*_DHE_RSA_WITH_AES_256_CBC_SHA256",
            ".*_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            ".*_DHE_RSA_WITH_AES_256_CBC_SHA",
            ".*_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            ".*_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            ".*_DHE_RSA_WITH_AES_128_GCM_SHA256",
            ".*_DHE_RSA_WITH_AES_128_CBC_SHA256",
            ".*_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            ".*_DHE_RSA_WITH_AES_128_CBC_SHA" );
        sslContextFactory.setExcludeCipherSuites(
            ".*NULL.*",
            ".*MD5.*",
            ".*DSS.*" );
        sslContextFactory.setIncludeProtocols(
            "TLSv1",
            "TLSv1.1",
            "TLSv1.2" );
        sslContextFactory.setExcludeProtocols(
            "SSL",
            "SSLv2",
            "SSLv2Hello",
            "SSLv3" );
        sslContextFactory.setRenegotiationAllowed( false );
        sslContextFactory.setUseCipherSuitesOrder( true );

        HttpConfiguration https_config = new HttpConfiguration( http_config );
        https_config.addCustomizer( new ForwardedRequestCustomizer() );

        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        // Server Name Indication (SNI) host check is an undocumented feature.
        secureRequestCustomizer.setSniHostCheck( Boolean.parseBoolean(
            getStringSystemProperty( "jetty.sniHostCheck", "FALSE" ) ) );
        secureRequestCustomizer
            .setStsMaxAge( getIntSystemProperty( "jetty.strictTransportSecurity.maxAge", 15768000 ) );
        secureRequestCustomizer.setStsIncludeSubDomains( Boolean.parseBoolean(
            getStringSystemProperty( "jetty.strictTransportSecurity.includeSubDomains", "TRUE" ) ) );
        https_config.addCustomizer( secureRequestCustomizer );

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(
            server,
            getIntSystemProperty( "jetty.ssl.acceptors", -1 ),
            getIntSystemProperty( "jetty.ssl.selectors", -1 ),
            new SslConnectionFactory( sslContextFactory, HttpVersion.HTTP_1_1.asString() ),
            new HttpConnectionFactory( https_config ) );

        sslConnector.setHost( getStringSystemProperty( "jetty.host", null ) );
        sslConnector.setPort( getIntSystemProperty( "jetty.ssl.port", -1 ) );
        sslConnector.setIdleTimeout( getIntSystemProperty( "jetty.ssl.idleTimeout", 30000 ) );
        sslConnector.setAcceptorPriorityDelta( getIntSystemProperty( "jetty.ssl.acceptorPriorityDelta", 0 ) );
        sslConnector.setAcceptQueueSize( getIntSystemProperty( "jetty.ssl.acceptQueueSize", 0 ) );

        return sslConnector;
    }

    public abstract ServletContextHandler getServletContextHandler();

}
