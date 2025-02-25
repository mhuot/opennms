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
package org.opennms.netmgt.telemetry.distributed.minion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.core.health.api.HealthCheck;
import org.opennms.core.ipc.sink.api.AsyncDispatcher;
import org.opennms.core.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.telemetry.api.TelemetryManager;
import org.opennms.netmgt.telemetry.api.adapter.Adapter;
import org.opennms.netmgt.telemetry.api.receiver.Listener;
import org.opennms.netmgt.telemetry.api.receiver.TelemetryMessage;
import org.opennms.netmgt.telemetry.api.registry.TelemetryRegistry;
import org.opennms.netmgt.telemetry.common.ipc.TelemetrySinkModule;
import org.opennms.netmgt.telemetry.distributed.common.MapBasedListenerDef;
import org.opennms.netmgt.telemetry.distributed.common.PropertyTree;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ManagedServiceFactory} for service pids that contain
 * telemetry listener definitions and manages their lifecycle by starting/updating
 * and stopping them accordingly.
 *
 * See {@link MapBasedListenerDef} for a list of supported properties.
 *
 * @author jwhite
 */
public class ListenerManager implements ManagedServiceFactory, TelemetryManager {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerManager.class);

    private MessageDispatcherFactory messageDispatcherFactory;
    private DistPollerDao distPollerDao;
    private TelemetryRegistry telemetryRegistry;

    private static class Entity {
        private Listener listener;
        private Set<String> queueNames = new HashSet<>();
        private ServiceRegistration<HealthCheck> healthCheck;
    }

    private Map<String, Entity> entities = new LinkedHashMap<>();

    private BundleContext bundleContext;

    @Override
    public String getName() {
        return "Manages telemetry listener lifecycle.";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        if (this.entities.containsKey(pid)) {
            LOG.info("Updating existing listener/dispatcher for pid: {}", pid);
            deleted(pid);
        } else {
            LOG.info("Creating new listener/dispatcher for pid: {}", pid);
        }
        final PropertyTree definition = PropertyTree.from(properties);
        final MapBasedListenerDef listenerDef = new MapBasedListenerDef(definition);
        final ListenerHealthCheck healthCheck = new ListenerHealthCheck(listenerDef);

        final Entity entity = new Entity();
        entity.healthCheck = bundleContext.registerService(HealthCheck.class, healthCheck, null);

        try {
            // Create sink modules for all defined queues
            listenerDef.getParsers().stream()
                    .forEach(parserDef -> {
                        // Ensure that the queues have not yet been created
                        if (telemetryRegistry.getDispatcher(parserDef.getQueueName()) != null) {
                            throw new IllegalArgumentException("A queue with name " + parserDef.getQueueName() + " is already defined. Bailing.");
                        }

                        // Create sink module
                        final TelemetrySinkModule sinkModule = new TelemetrySinkModule(parserDef);
                        sinkModule.setDistPollerDao(distPollerDao);

                        // Create dispatcher
                        final AsyncDispatcher<TelemetryMessage> dispatcher = messageDispatcherFactory.createAsyncDispatcher(sinkModule);
                        final String queueName = Objects.requireNonNull(parserDef.getQueueName());
                        telemetryRegistry.registerDispatcher(queueName, dispatcher);

                        // Remember queue name
                        entity.queueNames.add(parserDef.getQueueName());
                    });

            // Start listener
            entity.listener = telemetryRegistry.getListener(listenerDef);
            entity.listener.start();

            // At this point the listener should be up and running,
            // so we mark the underlying health check as success
            healthCheck.markSucess();

            this.entities.put(pid, entity);
        } catch (Exception e) {
            LOG.error("Failed to build listener.", e);

            // In case of error, we mark the health check as failure as well
            healthCheck.markError(e);

            // Close all already started dispatcher
            stopQueues(entity.queueNames);
        }
        LOG.info("Successfully started listener/dispatcher for pid: {}", pid);
    }

    @Override
    public void deleted(String pid) {
        final Entity entity = this.entities.remove(pid);
        if (entity.healthCheck != null) {
            entity.healthCheck.unregister();
        }
        if (entity.listener != null) {
            LOG.info("Stopping listener for pid: {}", pid);
            try {
                entity.listener.stop();
            } catch (InterruptedException e) {
                LOG.error("Error occurred while stopping listener for pid: {}", pid, e);
            }
        }
        if (entity.queueNames != null) {
            stopQueues(entity.queueNames);
        }
    }

    public void init() {
        LOG.info("ListenerManager started.");
    }

    public void destroy() {
        new ArrayList<>(this.entities.keySet()).forEach(pid -> deleted(pid));
        LOG.info("ListenerManager stopped.");
    }

    public MessageDispatcherFactory getMessageDispatcherFactory() {
        return messageDispatcherFactory;
    }

    public void setMessageDispatcherFactory(MessageDispatcherFactory messageDispatcherFactory) {
        this.messageDispatcherFactory = messageDispatcherFactory;
    }

    public DistPollerDao getDistPollerDao() {
        return distPollerDao;
    }

    public void setDistPollerDao(DistPollerDao distPollerDao) {
        this.distPollerDao = distPollerDao;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setTelemetryRegistry(TelemetryRegistry telemetryRegistry) {
        this.telemetryRegistry = telemetryRegistry;
    }

    private void stopQueues(Set<String> queueNames) {
        Objects.requireNonNull(queueNames);
        for (String queueName : queueNames) {
            try {
                final AsyncDispatcher<TelemetryMessage> dispatcher = telemetryRegistry.getDispatcher(queueName);
                dispatcher.close();
            } catch (Exception ex) {
                LOG.error("Failed to close dispatcher.", ex);
            } finally {
                telemetryRegistry.removeDispatcher(queueName);
            }
        }
    }

    @Override
    public List<Listener> getListeners() {
        return this.entities.values().stream()
                .map(e -> e.listener)
                .collect(Collectors.toList());
    }

    @Override
    public List<Adapter> getAdapters() {
        return Collections.emptyList();
    }
}
