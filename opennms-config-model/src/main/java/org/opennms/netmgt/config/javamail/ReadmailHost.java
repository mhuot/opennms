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
package org.opennms.netmgt.config.javamail;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.core.xml.ValidateUsing;
import org.opennms.netmgt.config.utils.ConfigUtils;

/**
 * The Class ReadmailHost.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@XmlRootElement(name="readmail-host", namespace="http://xmlns.opennms.org/xsd/config/javamail-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("javamail-configuration.xsd")
public class ReadmailHost implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlAttribute(name="host")
    private String m_host;

    /** The port. */
    @XmlAttribute(name="port")
    private Integer m_port;

    /**
     * Basically any attributes that help setup the javamailer's confusing set of properties.
     */
    @XmlElement(name="readmail-protocol", required=true)
    private ReadmailProtocol m_readmailProtocol;

    public ReadmailHost() {
    }

    @Override()
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof ReadmailHost) {
            final ReadmailHost that = (ReadmailHost)obj;
            return Objects.equals(this.m_host, that.m_host)
                    && Objects.equals(this.m_port, that.m_port)
                    && Objects.equals(this.m_readmailProtocol, that.m_readmailProtocol);
        }
        return false;
    }

    public String getHost() {
        return m_host == null ? "127.0.0.1" : m_host;
    }

    public Integer getPort() {
        return m_port == null ? 110 : m_port;
    }

    public ReadmailProtocol getReadmailProtocol() {
        return m_readmailProtocol;
    }

    @Override()
    public int hashCode() {
        return Objects.hash(m_host, m_port, m_readmailProtocol);
    }

    public void setHost(final String host) {
        m_host = ConfigUtils.normalizeString(host);
    }

    public void setPort(final Integer port) {
        m_port = port;
    }

    public void setReadmailProtocol(final ReadmailProtocol readmailProtocol) {
        m_readmailProtocol = ConfigUtils.assertNotNull(readmailProtocol, "readmail-protocol");
    }

}
