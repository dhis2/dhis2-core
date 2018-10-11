package org.hisp.dhis;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.reflect.Method;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public abstract class IntegrationTestBase extends DhisConvenienceTest implements ApplicationContextAware
{
    @Autowired
    protected DbmsManager dbmsManager;

    @ClassRule
    public static PostgreSQLContainer postgresContainer = new PostgreSQLContainer()
            .withDatabaseName("dhis2")
            .withUsername("dhis")
            .withPassword("dhis");

    static {
        postgresContainer.start();
        System.setProperty("test.configuration.connection.url", postgresContainer.getJdbcUrl());
        System.setProperty("test.configuration.connection.username", postgresContainer.getUsername());
        System.setProperty("test.configuration.connection.password", postgresContainer.getPassword());
        System.out.println(  postgresContainer.getJdbcUrl());
    }


    protected ApplicationContext webApplicationContext;

    @Before
    public void before() throws Exception {
        bindSession();
        executeStartupRoutines();
        setUpTest();
    }

    @After
    public void after()
        throws Exception
    {
        tearDownTest();
        unbindSession();
        if ( emptyDatabaseAfterTest() )
        {
            System.out.print("Cleaning up");
            dbmsManager.emptyDatabase();
        }

         postgresContainer.stop();
    }

    private void executeStartupRoutines()
            throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( webApplicationContext.containsBean( id ) )
        {
            Object object = webApplicationContext.getBean( id );
            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );
            method.invoke( object, new Object[0] );
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.webApplicationContext = applicationContext;
    }
    private void bindSession()
    {
        SessionFactory sessionFactory = (SessionFactory) webApplicationContext.getBean( "sessionFactory" );
        Session session = sessionFactory.openSession();

        TransactionSynchronizationManager.bindResource( sessionFactory, new SessionHolder( session ) );
    }

    private void unbindSession()
    {
        SessionFactory sessionFactory = (SessionFactory) webApplicationContext.getBean( "sessionFactory" );

        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource( sessionFactory );

        SessionFactoryUtils.closeSession( sessionHolder.getSession() );
    }

    public abstract boolean emptyDatabaseAfterTest();

    /**
     * Method to override.
     */
    protected void setUpTest()
        throws Exception
    {
    }

    /**
     * Method to override.
     */
    protected void tearDownTest()
        throws Exception
    {
    }


}
