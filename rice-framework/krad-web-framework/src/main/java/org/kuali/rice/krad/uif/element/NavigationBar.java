/**
 * Copyright 2005-2014 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.krad.uif.element;

import org.kuali.rice.krad.uif.component.Component;
import org.kuali.rice.krad.uif.container.Group;

import java.util.List;

/**
 * Component that renders a navigation bar, including a branding and navigation group.
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class NavigationBar extends ContentElementBase {
    private static final long serialVersionUID = -2061519100931559642L;

    private String brandText;
    private Image brandImage;

    private Group navigationBarGroup;

    public NavigationBar() {
        super();
    }

    /**
     * @see org.kuali.rice.krad.uif.component.ComponentBase#getComponentsForLifecycle()
     */
    @Override
    public List<Component> getComponentsForLifecycle() {
        List<Component> components = super.getComponentsForLifecycle();

        components.add(brandImage);
        components.add(navigationBarGroup);

        return components;
    }

    /**
     * Header text to use for the application branding.
     *
     * <p>Note either the branding text, or the {@link NavigationBar#getBrandImage()} should be set, but
     * not both</p>
     *
     * @return String text to use for branding
     */
    public String getBrandText() {
        return brandText;
    }

    /**
     * @see NavigationBar#getBrandText()
     */
    public void setBrandText(String brandText) {
        this.brandText = brandText;
    }

    /**
     * Image component instance to use for the application branding.
     *
     * <p>Note either the branding image, or the {@link NavigationBar#getBrandText()} should be set, but
     * not both</p>
     *
     * @return Image component to use for branding
     */
    public Image getBrandImage() {
        return brandImage;
    }

    /**
     * @see NavigationBar#getBrandImage()
     */
    public void setBrandImage(Image brandImage) {
        this.brandImage = brandImage;
    }

    /**
     * Group instance that holds the navigation items (such as links) for the navigation bar.
     *
     * @return Group instance for navigation
     */
    public Group getNavigationBarGroup() {
        return navigationBarGroup;
    }

    /**
     * @see NavigationBar#getNavigationBarGroup()
     */
    public void setNavigationBarGroup(Group navigationBarGroup) {
        this.navigationBarGroup = navigationBarGroup;
    }

    /**
     * Convenience setter that sets the given items onto the {@link NavigationBar#getNavigationBarGroup()}.
     *
     * @param items list of items for the navigation group
     */
    public void setItems(List<Component> items) {
        if (this.navigationBarGroup == null) {
            throw new RuntimeException("Unable to set navigation items because navigation group is null");
        } else {
            this.navigationBarGroup.setItems(items);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> void copyProperties(T component) {
        super.copyProperties(component);

        NavigationBar navbarCopy = (NavigationBar) component;

        navbarCopy.setBrandText(this.brandText);

        if (this.brandImage != null) {
            navbarCopy.setBrandImage((Image) this.brandImage.copy());
        }

        if (this.navigationBarGroup != null) {
            navbarCopy.setNavigationBarGroup((Group) this.navigationBarGroup.copy());
        }
    }
}