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
package org.opennms.netmgt.dao.mock;

import java.util.Collection;
import java.util.List;

import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.OnmsResourceType;
import org.opennms.netmgt.model.ResourceId;

public class MockResourceDao implements ResourceDao {

    @Override
    public Collection<OnmsResourceType> getResourceTypes() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public OnmsResource getResourceById(ResourceId id) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<OnmsResource> findTopLevelResources() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public OnmsResource getResourceForNode(OnmsNode node) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public boolean deleteResourceById(final ResourceId resourceId) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public ResourceId getResourceId(CollectionResource resource, long nodeId) {
        if (nodeId > 0 && resource.getResourceTypeName().equals("node")) {
            return ResourceId.get(resource.getResourceTypeName(), String.valueOf(nodeId));
        }
        return ResourceId.get(resource.getResourceTypeName(), resource.getInstance());
    }
}
