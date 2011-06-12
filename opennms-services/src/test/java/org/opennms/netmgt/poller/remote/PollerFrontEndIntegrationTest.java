//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2007 Aug 24: Use mockEventIpcManager.xml Spring context and remove commented-out code. - dj@opennms.org
// 2007 Apr 16: Don't use test.overridden.properties; use beans and override them instead. - dj@opennms.org
// 2007 Apr 06: Use DaoTestConfigBean to setup system properties. - dj@opennms.org
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.poller.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.netmgt.dao.db.OpenNMSConfigurationExecutionListener;
import org.opennms.netmgt.dao.db.TemporaryDatabase;
import org.opennms.netmgt.dao.db.TemporaryDatabaseAware;
import org.opennms.netmgt.dao.db.TemporaryDatabaseExecutionListener;
import org.opennms.test.FileAnticipator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
    OpenNMSConfigurationExecutionListener.class,
    TemporaryDatabaseExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/applicationContext-pollerBackEnd.xml",
        "classpath:/META-INF/opennms/applicationContext-exportedPollerBackEnd-rmi.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/org/opennms/netmgt/poller/remote/applicationContext-configOverride.xml"
})
@JUnitTemporaryDatabase(tempDbClass=TemporaryDatabase.class)
public class PollerFrontEndIntegrationTest implements InitializingBean, TemporaryDatabaseAware<TemporaryDatabase> {
    @Autowired
    private DatabasePopulator m_populator;

    private FileAnticipator m_fileAnticipator;
    private PollerFrontEnd m_frontEnd;
    private PollerSettings m_settings;
    private ClassPathXmlApplicationContext m_frontEndContext;

    private TemporaryDatabase m_database;

    public void setTemporaryDatabase(TemporaryDatabase database) {
        m_database = database;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assertNotNull(m_populator);
    }

    @After
    public void afterTest() throws Throwable {
        m_frontEndContext.stop();
        m_frontEndContext.close();

        if (m_fileAnticipator.isInitialized()) {
            m_fileAnticipator.deleteExpected();
        }

        m_fileAnticipator.tearDown();
    }

    @Before
    public void onSetUpInTransactionIfEnabled() throws Exception {
        m_fileAnticipator = new FileAnticipator();

        String filename = m_fileAnticipator.expecting("remote-poller.configuration").getCanonicalPath();
        filename = filename.replace("+", "%2B");
        System.setProperty("opennms.poller.configuration.resource", "file://" + filename);

        m_populator.populateDatabase();

        /**
         * We complete and end the transaction here so that the populated
         * database gets committed.  If we don't do this, the poller back
         * end (setup with the contexts in getConfigLocations) won't see
         * the populated database because it's working in another
         * transaction.  This will cause one of the asserts in testRegister
         * to fail because no services will be monitored by the remote
         * poller.
         */
        /*
        setComplete();
        endTransaction();
         */

        m_frontEndContext = new ClassPathXmlApplicationContext(
                                                               new String[] { 
                                                                       "classpath:/META-INF/opennms/applicationContext-remotePollerBackEnd-rmi.xml",
                                                                       "classpath:/META-INF/opennms/applicationContext-pollerFrontEnd.xml",
                                                               },
                                                               false
        );

        Properties props = new Properties();
        props.setProperty("configCheckTrigger.repeatInterval", "1000");

        PropertyOverrideConfigurer testPropertyConfigurer = new PropertyOverrideConfigurer();
        testPropertyConfigurer.setProperties(props);
        m_frontEndContext.addBeanFactoryPostProcessor(testPropertyConfigurer);

        m_frontEndContext.refresh();
        m_frontEnd = (PollerFrontEnd)m_frontEndContext.getBean("pollerFrontEnd");
        m_settings = (PollerSettings)m_frontEndContext.getBean("pollerSettings");
    }

    @Test
    public void testRegister() throws Exception {

        // Check preconditions
        assertFalse(m_frontEnd.isRegistered());
        assertEquals(0, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitors"));
        assertEquals(0, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitor_details"));
        assertTrue("There were unexpected poll results", 0 == m_database.getJdbcTemplate().queryForInt("select count(*) from location_specific_status_changes"));

        // Start up the remote poller
        m_frontEnd.register("RDU");
        Integer monitorId = m_settings.getMonitorId();

        assertTrue(m_frontEnd.isRegistered());
        assertEquals(1, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitors where id=?", monitorId));
        assertEquals(5, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitor_details where locationMonitorId = ?", monitorId));

        assertEquals(System.getProperty("os.name"), m_database.getJdbcTemplate().queryForObject("select propertyValue from location_monitor_details where locationMonitorId = ? and property = ?", String.class, monitorId, "os.name"));

        Thread.sleep(20000);

        assertEquals(1, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitors where id=?", monitorId));
        assertEquals(0, m_database.getJdbcTemplate().queryForInt("select count(*) from location_monitors where status='DISCONNECTED' and id=?", monitorId));

        assertTrue("Could not find any pollResults", 0 < m_database.getJdbcTemplate().queryForInt("select count(*) from location_specific_status_changes where locationMonitorId = ?", monitorId));

        m_frontEnd.stop();
    }
}
