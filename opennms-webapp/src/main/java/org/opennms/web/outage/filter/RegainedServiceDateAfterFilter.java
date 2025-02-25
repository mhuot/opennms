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
package org.opennms.web.outage.filter;

import java.util.Date;

import org.opennms.web.filter.GreaterThanFilter;
import org.opennms.web.filter.SQLType;

/**
 * <p>RegainedServiceDateAfterFilter class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
public class RegainedServiceDateAfterFilter extends GreaterThanFilter<Date> {
    /** Constant <code>TYPE="regainedafter"</code> */
    public static final String TYPE = "regainedafter";

    /**
     * <p>Constructor for RegainedServiceDateAfterFilter.</p>
     *
     * @param date a {@link java.util.Date} object.
     */
    public RegainedServiceDateAfterFilter(Date date) {
        super(TYPE, SQLType.DATE, "OUTAGES.IFREGAINEDSERVICE", "ifRegainedService", date);
    }

    /**
     * <p>Constructor for RegainedServiceDateAfterFilter.</p>
     *
     * @param epochTime a long.
     */
    public RegainedServiceDateAfterFilter(long epochTime) {
        this(new Date(epochTime));
    }

    /**
     * <p>getTextDescription</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getTextDescription() {
        return ("regained service date after \"" + getValue() + "\"");
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return ("<Regained Service Date After Filter: " + this.getDescription() + ">");
    }

    /**
     * <p>getDate</p>
     *
     * @return a {@link java.util.Date} object.
     */
    public Date getDate() {
        return getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof RegainedServiceDateAfterFilter)) return false;
        return (this.toString().equals(obj.toString()));
    }
}
