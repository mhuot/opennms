<%--

    Licensed to The OpenNMS Group, Inc (TOG) under one or more
    contributor license agreements.  See the LICENSE.md file
    distributed with this work for additional information
    regarding copyright ownership.

    TOG licenses this file to You under the GNU Affero General
    Public License Version 3 (the "License") or (at your option)
    any later version.  You may not use this file except in
    compliance with the License.  You may obtain a copy of the
    License at:

         https://www.gnu.org/licenses/agpl-3.0.txt

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied.  See the License for the specific
    language governing permissions and limitations under the
    License.

--%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Category")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Category", "admin/categories.htm")
          .breadcrumb("Show")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<script type="text/javascript">
var nodesToAddAuto = [
  <c:forEach items="${model.nodes}" var="node"><c:if test="${node.foreignSource == null}">{ "id": "${node.id}", "label": "${node.label}" },</c:if>
  </c:forEach>
];

var nodesToDeleteAuto = [
  <c:forEach items="${model.sortedMemberNodes}" var="node"><c:if test="${node.foreignSource == null}">{ "id": "${node.id}", "label": "${node.label}" },</c:if>
  </c:forEach>
];

var nodesToAddReq = [
  <c:forEach items="${model.nodes}" var="node"><c:if test="${node.foreignSource != null}">{ "id": "${node.id}", "label": "${node.label}", "foreignSource": "${node.foreignSource}", "foreignId": "${node.foreignId}" },</c:if>
  </c:forEach>
];

var nodesToDeleteReq = [
  <c:forEach items="${model.sortedMemberNodes}" var="node"><c:if test="${node.foreignSource != null}">{ "id": "${node.id}", "label": "${node.label}", "foreignSource": "${node.foreignSource}", "foreignId": "${node.foreignId}" },</c:if>
  </c:forEach>
];

function populateOptGroupFromList(elementName, list) {
	var optgroupElem = document.getElementById(elementName);
	if (optgroupElem == null) {
		return;
	}
	while (optgroupElem.childElementCount > 0) {
		optgroupElem.remove(0);
	}
	
	for (var i=0, len=list.length, nodeSpec; i < len; i++) {
		nodeSpec = list[i];
		var optionElem = document.createElement("option");
		optionElem.value = nodeSpec["id"];
		optionElem.textContent = nodeSpec["label"];
		optionElem.title = "Node ID " + nodeSpec["id"];
		if (!!nodeSpec["foreignId"]) {
			optionElem.title += " / " + nodeSpec["foreignSource"] + ":" + nodeSpec["foreignId"];
		}
		optgroupElem.appendChild(optionElem);
	}
}

function populateAutoNodes() {
	populateOptGroupFromList("toAddAutoGroup", nodesToAddAuto);
	populateOptGroupFromList("toDeleteAutoGroup", nodesToDeleteAuto);
}

function populateReqNodes() {
	populateOptGroupFromList("toAddReqGroup", nodesToAddReq);
	populateOptGroupFromList("toDeleteReqGroup", nodesToDeleteReq);
}

function toggleReqNodes() {
	var addGroup = document.getElementById("toAddReqGroup");
	var deleteGroup = document.getElementById("toDeleteReqGroup");
	addGroup.disabled = !(addGroup.disabled);
	deleteGroup.disabled = !(deleteGroup.disabled);
}
</script>

<div class="card">
  <div class="card-header">
    <span>Edit Surveillance Category ${fn:escapeXml(model.category.name)}</span>
  </div>
  <div class="card-body">
    <p>
    Category '${fn:escapeXml(model.category.name)}' has ${fn:length(model.sortedMemberNodes)} nodes
    </p>
    
    <form action="admin/categories.htm" method="get">
      <input type="hidden" name="categoryid" value="${model.category.id}"/>
      <input type="hidden" name="edit" value=""/>

      <div class="form-group form-row">
        <div class="col-md-5">
          <label for="toAdd">Available nodes</label>
          <select id="toAdd" class="form-control" name="toAdd" size="20" multiple>
            <optgroup id="toAddAutoGroup" label="Auto-Provisioned Nodes"></optgroup>
            <optgroup id="toAddReqGroup" disabled="disabled" label="Requisitioned Nodes"></optgroup>
          </select>
        </div> <!-- column -->

        <div class="col-md-2 form-group text-center my-auto mx-auto">
            <button type="submit" class="btn btn-secondary" name="action" value="Add &#155;&#155;">Add &#155;&#155;</button>
            <button type="submit" class="btn btn-secondary" name="action" value="&#139;&#139; Remove">&#139;&#139; Remove</button>
        </div>

        <div class="col-md-5">
          <label for="toDelete">Nodes on category</label>
          <select id="toDelete" class="form-control" name="toDelete" size="20" multiple>
            <optgroup id="toDeleteAutoGroup" label="Auto-Provisioned Nodes"></optgroup>
            <optgroup id="toDeleteReqGroup" disabled="disabled" label="Requisitioned Nodes"></optgroup>
          </select>
        </div>
      </div>

      <div class="form-group">
        <input id="toggleCheckbox" type="checkbox" onchange="javascript:toggleReqNodes()" />
        <label for="toggleCheckbox">Check this box to enable requisitioned nodes (changes <strong>will</strong> be lost on next synchronization)</label>
      </div>
    </form>
  </div> <!-- card-body -->
</div> <!-- panel -->

<script type="text/javascript">
populateAutoNodes();
populateReqNodes();
</script>

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false"/>
