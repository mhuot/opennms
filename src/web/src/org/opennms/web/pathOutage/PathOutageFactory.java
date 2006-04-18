//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// Orignal code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

package org.opennms.web.pathOutage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;

import org.opennms.core.resource.Vault;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.EventConstants;

/**
 * The source for all path outage business objects (nodes, critical path IPs,
 * critical path service names). Encapsulates all lookup functionality for 
 * these business objects in one place.
 * 
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class PathOutageFactory extends Object {

    private static final String GET_CRITICAL_PATHS = "SELECT DISTINCT criticalpathip, criticalpathservicename FROM pathoutage ORDER BY criticalpathip, criticalpathservicename";

    private static final String GET_NODES_IN_PATH = "SELECT DISTINCT nodeid FROM pathoutage WHERE criticalpathip=? AND criticalpathservicename=? ORDER BY nodeid";

    private static final String COUNT_MANAGED_SVCS = "SELECT count(*) FROM ifservices WHERE status ='A' and nodeid=?";

    private static final String COUNT_NODES_IN_PATH = "SELECT count(*) FROM pathoutage WHERE criticalpathip=? AND criticalpathservicename=?";

    private static final String GET_NODELABEL_BY_IP = "SELECT nodelabel FROM node WHERE nodeid IN (SELECT nodeid FROM ipinterface WHERE ipaddr=?)";

    private static final String GET_NODEID_BY_IP = "SELECT nodeid FROM ipinterface WHERE ipaddr=? ORDER BY nodeid DESC LIMIT 1";

    private static final String GET_NODELABEL_BY_NODEID = "SELECT nodelabel FROM node WHERE nodeid=?";

    private static final String GET_CRITICAL_PATH_STATUS = "SELECT count(*) FROM outages WHERE ipaddr=? AND ifregainedservice IS NULL AND serviceid=(SELECT serviceid FROM service WHERE servicename=?)";

    private static final String IS_CRITICAL_PATH_MANAGED = "SELECT count(*) FROM ifservices WHERE ipaddr=? AND status='A' AND serviceid=(SELECT serviceid FROM service WHERE servicename=?)";

    private static final String SQL_GET_LATEST_NODE_DOWN_EVENTID = "SELECT eventid FROM events WHERE nodeid=? AND eventuei='uei.opennms.org/nodes/nodeDown' ORDER BY eventid DESC LIMIT 1";

    private static final String SQL_GET_LATEST_NODE_UP_EVENTID = "SELECT eventid FROM events WHERE nodeid=? AND eventuei='uei.opennms.org/nodes/nodeUp' ORDER BY eventid DESC LIMIT 1";

    private static final String SQL_GET_EVENT_PARMS = "SELECT eventparms FROM events WHERE eventid=?";

    /**
     * <p>
     * Retrieve all the critical paths
     * from the database
     */
    public static List getAllCriticalPaths() throws SQLException {
        Connection conn = Vault.getDbConnection();
        List paths = new ArrayList();

        try {
            PreparedStatement stmt = conn.prepareStatement(GET_CRITICAL_PATHS);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String[] path = new String[2];
                path[0] = rs.getString(1);
                path[1] = rs.getString(2);
                paths.add(path);
            }
            rs.close();
            stmt.close();
        } finally {
            Vault.releaseDbConnection(conn);
        }
        return paths;
    }

    /**
     * <p>
     * Retrieve all the nodes in a critical path
     * from the database
     * 
     * @param criticalPathIp
     *            IP address of the critical path
     * @param criticalPathServiceName
     *            service name for the critical path
     */
    public static List getNodesInPath(String criticalPathIp, String criticalPathServiceName) throws SQLException {
        Connection conn = Vault.getDbConnection();
        List pathNodes = new ArrayList();

        try {
            PreparedStatement stmt = conn.prepareStatement(GET_NODES_IN_PATH);
            stmt.setString(1, criticalPathIp);
            stmt.setString(2, criticalPathServiceName);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pathNodes.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
        } finally {
            Vault.releaseDbConnection(conn);
        }
        return pathNodes;
    }

    /**
     * This method is responsible for determining the 
     * color based on the status of the node, and the
     * node label
     * 
     * @param String nodeID
     *            the nodeID of the node being checked
     */
    public static String[] getNodeLabelAndColor(String nodeID) throws SQLException {

        Connection conn = Vault.getDbConnection();
        int count = 0;
        String result[] = new String[3];
        result[1] = "lightblue";
        result[2] = "Unmanaged";

        try {
            PreparedStatement stmt = conn.prepareStatement(GET_NODELABEL_BY_NODEID);
            stmt.setString(1, nodeID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result[0] = rs.getString(1);
            }
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement(COUNT_MANAGED_SVCS);
            stmt.setString(1, nodeID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
            if(count > 0) {
                PreparedStatement stmt1 = conn.prepareStatement(SQL_GET_LATEST_NODE_DOWN_EVENTID);
                PreparedStatement stmt2 = conn.prepareStatement(SQL_GET_LATEST_NODE_UP_EVENTID);
                stmt1.setString(1, nodeID);
                stmt2.setString(1, nodeID);
                ResultSet rs1 = stmt1.executeQuery();
                if (rs1.next()) {
                    int nodeDownEventId = rs1.getInt(1);
                    ResultSet rs2 = stmt2.executeQuery();
                    if (rs2.next()) {
                        if(rs2.getInt(1) > nodeDownEventId) {
                            result[1] = "green";
                            result[2] = "Up";
                        } else {
                            // see if last node down was a path outage
                            PreparedStatement stmt3 = conn.prepareStatement(SQL_GET_EVENT_PARMS);
                            stmt3.setInt(1, nodeDownEventId);
                            ResultSet rs3 = stmt3.executeQuery();
                            if (rs3.next()) {
                                if(rs3.getString(1).indexOf("eventReason=pathOutage") > -1) {
                                    result[1] = "orange";
                                    result[2] = "Path Outage";
                                } else {
                                    result[1] = "red";
                                    result[2] = "Down";
                                }
                            }
                            rs3.close();
                            stmt3.close();
                        }
                    } else {
                        result[1] = "red";
                        result[2] = "Down";
                    } 
                    rs2.close();
                    stmt2.close();
                } else {
                    result[1] = "green";
                    result[2] = "Up";
                }
                rs1.close();
                stmt1.close();
            }
            rs.close();
            stmt.close();
        } finally {
            Vault.releaseDbConnection(conn);
        }
        return result;
    }

    /**
     * This method is responsible for determining the 
     * data related to the critical path:
     * node label, nodeId, the number of nodes
     * dependent on this path, and the managed state
     * of the path
     * 
     * @param String criticalPathIp
     *            the criticalPathIp of the node
     *            being checked
     * 
     * @param String criticalPathServiceName the
     *            criticalPathServiceName of the
     *            node being checked
     */
    public static String[] getCriticalPathData(String criticalPathIp, String criticalPathServiceName) throws SQLException {
        Connection conn = Vault.getDbConnection();
        String[] result = new String[4];
        int nodeCount=0;
        int count = 0;

        try {

            PreparedStatement stmt0 = conn.prepareStatement(GET_NODELABEL_BY_IP);
            stmt0.setString(1, criticalPathIp);

            ResultSet rs0 = stmt0.executeQuery();
            while (rs0.next()) {
                count++;
                result[0] = rs0.getString(1);
            }
            if (count > 1) {
                result[0] = "(" + count + " nodes have this IP)";
            }

            rs0.close();
            stmt0.close();

            count = 0;
            PreparedStatement stmt1 = conn.prepareStatement(GET_NODEID_BY_IP);
            stmt1.setString(1, criticalPathIp);

            ResultSet rs1 = stmt1.executeQuery();
            while (rs1.next()) {
                result[1] = rs1.getString(1);
            }
            rs1.close();
            stmt1.close();

            PreparedStatement stmt2 = conn.prepareStatement(COUNT_NODES_IN_PATH);
            stmt2.setString(1, criticalPathIp);
            stmt2.setString(2, criticalPathServiceName);

            ResultSet rs2 = stmt2.executeQuery();
            while (rs2.next()) {
                nodeCount = rs2.getInt(1);
            }
            result[2] = Integer.toString(nodeCount);
            rs2.close();
            stmt2.close();

            PreparedStatement stmt = conn.prepareStatement(IS_CRITICAL_PATH_MANAGED);
            stmt.setString(1, criticalPathIp);
            stmt.setString(2, criticalPathServiceName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            if(count > 0) {
                PreparedStatement stmt3 = conn.prepareStatement(GET_CRITICAL_PATH_STATUS);
                stmt3.setString(1, criticalPathIp);
                stmt3.setString(2, criticalPathServiceName);

                ResultSet rs3 = stmt3.executeQuery();
                while (rs3.next()) {
                    count = rs3.getInt(1);
                }
                if(count > 0) {
                    result[3] = "red";
                } else {
                    result[3] = "green";
                }
                while (rs3.next()) {
                    result[3] = rs3.getString(1);
                }
                rs3.close();
                stmt3.close();
            } else {
                result[3] = "lightblue";
            }
        } finally {
            Vault.releaseDbConnection(conn);
        }
        return result;
    }
}
