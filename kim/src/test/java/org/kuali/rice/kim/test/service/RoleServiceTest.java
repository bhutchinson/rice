/*
 * Copyright 2007 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.kim.test.service;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.kuali.rice.core.resourceloader.GlobalResourceLoader;
import org.kuali.rice.kim.bo.types.dto.AttributeSet;
import org.kuali.rice.kim.service.RoleService;
import org.kuali.rice.kim.test.KIMTestCase;

/**
 * Test the RoleService
 * 
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 *
 */
public class RoleServiceTest extends KIMTestCase {

	private RoleService roleService;

	public void setUp() throws Exception {
		super.setUp();
		roleService = (RoleService) getKimService(ServiceTestUtils.getConfigProp("kim.test.namespace.role"),
												  ServiceTestUtils.getConfigProp("kim.test.servicename.role"),
												  ServiceTestUtils.getConfigProp("kim.test.serviceclass.role"));

	}
	
	@Test
	public void testPrincipaHasRoleOfDirectAssignment() {
		List <String>roleIds = new ArrayList<String>();
		roleIds.add("r1");
		assertTrue( "p1 has direct role r1", roleService.principalHasRole("p1", roleIds, null ));	
		//assertFalse( "p4 has no direct/higher level role r1", roleService.principalHasRole("p4", roleIds, null ));	
		AttributeSet qualification = new AttributeSet();
		qualification.put("Attribute 2", "CHEM");
		assertTrue( "p1 has direct role r1 with rp2 attr data", roleService.principalHasRole("p1", roleIds, qualification ));	
		qualification.clear();
		//requested qualification rolls up to a higher element in some hierarchy 
		// method not implemented yet, not quite clear how this works
		qualification.put("Attribute 3", "PHYS");
		assertTrue( "p1 has direct role r1 with rp2 attr data", roleService.principalHasRole("p1", roleIds, qualification ));	
	}

	@Test
	public void testPrincipalHasRoleOfHigherLevel() {
		// "p3" is in "r2" and "r2 contains "r1"
		List <String>roleIds = new ArrayList<String>();
		roleIds.add("r2");
		assertTrue( "p1 has assigned in higher level role r1", roleService.principalHasRole("p1", roleIds, null ));		
	}
	
	@Test
	public void testPrincipalHasRoleContainsGroupAssigned() {
		// "p2" is in "g1" and "g1" assigned to "r2"
		List <String>roleIds = new ArrayList<String>();
		roleIds.add("r2");
		assertTrue( "p2 is assigned to g1 and g1 assigned to r2", roleService.principalHasRole("p2", roleIds, null ));		
	}
	
	/**
	 * This method tries to get a client proxy for the specified KIM service
	 * 
	 * @param  svcName - name of the KIM service desired
	 * @return the proxy object
	 * @throws Exception 
	 */
	protected Object getKimService(String svcNamespace, String... svcNames) throws Exception {
		// TODO: local namespace should be a valid, non-partial namespace (unlike 'KIM')
		return GlobalResourceLoader.getService(new QName("KIM", svcNames[0]));
	}
}
