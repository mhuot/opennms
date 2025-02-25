/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.protocols.xml.collector;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jrobin.core.Datasource;
import org.jrobin.core.RrdDb;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.opennms.core.collection.test.MockCollectionAgent;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.http.JUnitHttpServerExecutionListener;
import org.opennms.core.test.http.annotations.JUnitHttpServer;
import org.opennms.core.test.http.annotations.Webapp;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.CollectionSetVisitor;
import org.opennms.netmgt.collection.api.CollectionStatus;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.collection.persistence.rrd.RrdPersisterFactory;
import org.opennms.netmgt.config.DataCollectionConfigFactory;
import org.opennms.netmgt.config.DefaultDataCollectionConfigDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.support.FilesystemResourceStorageDao;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.netmgt.rrd.RrdStrategy;
import org.opennms.netmgt.rrd.jrobin.JRobinRrdStrategy;
import org.opennms.protocols.xml.config.XmlDataCollection;
import org.opennms.protocols.xml.config.XmlDataCollectionConfig;
import org.opennms.protocols.xml.config.XmlRrd;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * The Test Class for NMS-7963
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
    JUnitHttpServerExecutionListener.class
})
public class NMS7963IT {

    @Rule
    public TemporaryFolder m_temporaryFolder = new TemporaryFolder();

    /** The collection agent. */
    private CollectionAgent m_collectionAgent;

    /** The OpenNMS Node DAO. */
    private NodeDao m_nodeDao;

    private RrdStrategy<?, ?> m_rrdStrategy;

    private FilesystemResourceStorageDao m_resourceStorageDao;

    private RrdPersisterFactory m_persisterFactory;

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        DefaultDataCollectionConfigDao dao = new DefaultDataCollectionConfigDao();
        dao.setConfigDirectory("src/test/resources/etc/datacollection");
        dao.setConfigResource(new FileSystemResource("src/test/resources/etc/datacollection-config.xml"));
        dao.afterPropertiesSet();
        DataCollectionConfigFactory.setInstance(dao);

        m_rrdStrategy = new JRobinRrdStrategy();
        m_resourceStorageDao = new FilesystemResourceStorageDao();
        m_resourceStorageDao.setRrdDirectory(m_temporaryFolder.getRoot());
        m_temporaryFolder.newFolder("snmp");

        m_persisterFactory = new RrdPersisterFactory();
        m_persisterFactory.setResourceStorageDao(m_resourceStorageDao);
        m_persisterFactory.setRrdStrategy(m_rrdStrategy);

        m_collectionAgent = new MockCollectionAgent(1, "mynode.local", InetAddressUtils.addr("127.0.0.1"));

        m_nodeDao = mock(NodeDao.class);
        OnmsNode node = new OnmsNode();
        node.setId(1);
        node.setLabel("mynode.local");
        node.setAssetRecord(new OnmsAssetRecord());
        when(m_nodeDao.get(1)).thenReturn(node);
    }

    /**
     * Tear down.
     *
     * @throws Exception the exception
     */
    @After
    public void tearDown() throws Exception {
        verify(m_nodeDao, atLeastOnce()).get(1);
        MockLogAppender.assertNoWarningsOrGreater();
    }

    /**
     * Test HTTP Data Collection with XPath
     *
     * @throws Exception the exception
     */
    @Test
    @JUnitHttpServer(port=10342, https=false, webapps={
            @Webapp(context="/junit", path="src/test/resources/test-webapp")
    })
    public void testHttpCollection() throws Exception {
        File configFile = new File("src/test/resources/http-datacollection-config.xml");
        XmlDataCollectionConfig config = JaxbUtils.unmarshal(XmlDataCollectionConfig.class, configFile);
        XmlDataCollection collection = config.getDataCollectionByName("NMS-7963");
        RrdRepository repository = createRrdRepository(collection.getXmlRrd());

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("collection", "NMS-7963");

        DefaultXmlCollectionHandler collector = new DefaultXmlCollectionHandler();
        collector.setRrdRepository(repository);
        collector.setServiceName("HTTP");

        CollectionSet collectionSet = XmlCollectorTestUtils.doCollect(m_nodeDao, collector, m_collectionAgent, collection, parameters);
        Assert.assertEquals(CollectionStatus.SUCCEEDED, collectionSet.getStatus());

        ServiceParameters serviceParams = new ServiceParameters(new HashMap<String,Object>());
        CollectionSetVisitor persister = m_persisterFactory.createGroupPersister(serviceParams, repository, false, false);
        collectionSet.visit(persister);

        RrdDb jrb = new RrdDb(new File(getSnmpRoot(), "1/xml-retrv-wipo-data.jrb"));
        Assert.assertNotNull(jrb);
        Assert.assertEquals(1, jrb.getDsCount());
        Datasource ds = jrb.getDatasource("xml-wipo-paco");
        Assert.assertNotNull(ds);
        Assert.assertEquals(Double.valueOf(903), Double.valueOf(ds.getLastValue()));
    }

    /**
     * Creates the RRD repository.
     *
     * @return the RRD repository
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private RrdRepository createRrdRepository(XmlRrd rrd) throws IOException {
        RrdRepository repository = new RrdRepository();
        repository.setRrdBaseDir(getSnmpRoot());
        repository.setHeartBeat(rrd.getStep() * 2);
        repository.setStep(rrd.getStep());
        repository.setRraList(rrd.getXmlRras());
        return repository;
    }

    public File getSnmpRoot() {
        return new File(m_temporaryFolder.getRoot(), "snmp");
    }
}
