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
package org.opennms.features.topology.app.internal.gwt.client.ui;

import org.opennms.features.topology.app.internal.gwt.client.SearchSuggestion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SearchTokenField extends Composite {

    public interface CollapseCallback{
        void onCollapse(SearchSuggestion searchSuggestion);
    }

    public interface RemoveCallback{
        void onRemove(SearchSuggestion searchSuggestion);
    }

    public interface CenterOnSuggestionCallback{
        void onCenter(SearchSuggestion searchSuggestion);
    }

    private static SearchTokenFieldUiBinder uiBinder = GWT.create(SearchTokenFieldUiBinder.class);
    public interface SearchTokenFieldUiBinder extends UiBinder<Widget, SearchTokenField>{}

    @UiField
    FlowPanel m_namespace;

    @UiField
    FlowPanel m_label;

    @UiField
    Anchor m_closeBtn;

    @UiField
    Anchor m_centerSuggestionBtn;

    @UiField
    Anchor m_collapseBtn;

    @UiField
    HorizontalPanel m_iconPanel;

    @UiField
    HorizontalPanel m_tokenContainer;

    private final SearchSuggestion m_suggestion;
    private CollapseCallback m_collapseCallback;
    private RemoveCallback m_removeCallback;
    private CenterOnSuggestionCallback m_centerOnCallback;

    public SearchTokenField(SearchSuggestion searchSuggestion) {
        initWidget(uiBinder.createAndBindUi(this));
        m_suggestion = searchSuggestion;
        init();
    }

    @Override
    protected void onLoad() {
        super.onLoad();

    }

    private void init() {
        log(getClass().getSimpleName() + ".init(): " + m_suggestion.toString());

        m_closeBtn.setTitle("Remove from focus");
        m_closeBtn.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        m_closeBtn.getElement().getStyle().setPaddingLeft(5, Style.Unit.PX);

        m_centerSuggestionBtn.setTitle("Center on map");
        m_centerSuggestionBtn.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        m_centerSuggestionBtn.getElement().getStyle().setPaddingLeft(5, Style.Unit.PX);

        if (m_suggestion.isCollapsible()) {
            m_collapseBtn.getElement().getStyle().setCursor(Style.Cursor.POINTER);

            // If the suggestion is already collapsed, then switch the icon to the "+" icon
            m_collapseBtn.getElement().removeClassName("icon-plus");
            m_collapseBtn.getElement().removeClassName("icon-minus");
            if (m_suggestion.isCollapsed()) {
                m_collapseBtn.getElement().addClassName("icon-plus");
                m_collapseBtn.setTitle("Expand category");
            } else {
                m_collapseBtn.getElement().addClassName("icon-minus");
                m_collapseBtn.setTitle("Collapse category");
            }
        } else {
            m_collapseBtn.setVisible(false);
        }

        setLabel(m_suggestion.getLabel());
        setNamespace(m_suggestion.getNamespace());

    }

    public void setCollapseCallback(CollapseCallback callback) {
        m_collapseCallback = callback;
    }

    public void setRemoveCallback(RemoveCallback callback) {
        m_removeCallback = callback;
    }

    public void setCenterOnCallback(CenterOnSuggestionCallback callback){
        m_centerOnCallback = callback;
    }

    public void setNamespace(String namespace) {
        final String capitalized = namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
        m_namespace.getElement().setInnerText(capitalized + ": ");
    }

    public void setLabel(String label) {
        m_label.getElement().setInnerText(label);
    }

    @UiHandler("m_collapseBtn")
    void handleCollapse(ClickEvent event) {
        log(getClass().getSimpleName() + ".handleCollapse(): " + m_suggestion.toString());
        
        if (m_collapseCallback != null) {
            m_collapseCallback.onCollapse(m_suggestion);
        }
        // Toggle the icon on the button
        if (m_suggestion.isCollapsed()) {
            m_collapseBtn.getElement().removeClassName("icon-minus");
            m_collapseBtn.getElement().addClassName("icon-plus");
            m_collapseBtn.setTitle("Expand group");
        } else {
            m_collapseBtn.getElement().removeClassName("icon-plus");
            m_collapseBtn.getElement().addClassName("icon-minus");
            m_collapseBtn.setTitle("Collapse group");
        }
    }

    @UiHandler("m_closeBtn")
    void handleClick(ClickEvent event) {
        if (m_removeCallback != null) {
            m_removeCallback.onRemove(m_suggestion);
        }
    }

    @UiHandler("m_centerSuggestionBtn")
    void handleCenterOnClick(ClickEvent event){
        if(m_centerOnCallback != null){
            m_centerOnCallback.onCenter(m_suggestion);
        }
    }

    private static native void log(Object message) /*-{
        $wnd.console.debug(message);
    }-*/;
}
