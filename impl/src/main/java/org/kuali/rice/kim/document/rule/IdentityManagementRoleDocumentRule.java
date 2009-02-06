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
package org.kuali.rice.kim.document.rule;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.kim.bo.types.dto.AttributeSet;
import org.kuali.rice.kim.bo.types.impl.KimAttributeImpl;
import org.kuali.rice.kim.bo.types.impl.KimTypeImpl;
import org.kuali.rice.kim.bo.ui.KimDocumentRoleMember;
import org.kuali.rice.kim.bo.ui.KimDocumentRoleQualifier;
import org.kuali.rice.kim.bo.ui.KimDocumentRoleResponsibility;
import org.kuali.rice.kim.bo.ui.KimDocumentRoleResponsibilityAction;
import org.kuali.rice.kim.document.IdentityManagementKimDocument;
import org.kuali.rice.kim.document.IdentityManagementRoleDocument;
import org.kuali.rice.kim.document.authorization.IdentityManagementKimDocumentAuthorizer;
import org.kuali.rice.kim.rule.event.ui.AddDelegationEvent;
import org.kuali.rice.kim.rule.event.ui.AddDelegationMemberEvent;
import org.kuali.rice.kim.rule.event.ui.AddMemberEvent;
import org.kuali.rice.kim.rule.event.ui.AddPermissionEvent;
import org.kuali.rice.kim.rule.event.ui.AddResponsibilityEvent;
import org.kuali.rice.kim.rule.ui.AddDelegationMemberRule;
import org.kuali.rice.kim.rule.ui.AddDelegationRule;
import org.kuali.rice.kim.rule.ui.AddMemberRule;
import org.kuali.rice.kim.rule.ui.AddPermissionRule;
import org.kuali.rice.kim.rule.ui.AddResponsibilityRule;
import org.kuali.rice.kim.rules.ui.KimDocumentMemberRule;
import org.kuali.rice.kim.rules.ui.KimDocumentPermissionRule;
import org.kuali.rice.kim.rules.ui.KimDocumentResponsibilityRule;
import org.kuali.rice.kim.rules.ui.RoleDocumentDelegationMemberRule;
import org.kuali.rice.kim.rules.ui.RoleDocumentDelegationRule;
import org.kuali.rice.kim.service.IdentityService;
import org.kuali.rice.kim.service.KIMServiceLocator;
import org.kuali.rice.kim.service.ResponsibilityService;
import org.kuali.rice.kim.service.support.KimTypeService;
import org.kuali.rice.kim.service.support.impl.KimTypeServiceBase;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.rules.TransactionalDocumentRuleBase;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.kns.util.ErrorMap;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.KNSConstants;
import org.kuali.rice.kns.util.RiceKeyConstants;

/**
 * This is a description of what this class does - shyu don't forget to fill this in. 
 * 
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 *
 */
public class IdentityManagementRoleDocumentRule extends TransactionalDocumentRuleBase implements AddPermissionRule, AddResponsibilityRule, AddMemberRule, AddDelegationRule, AddDelegationMemberRule {

	private AddResponsibilityRule addResponsibilityRule;
	private AddPermissionRule  addPermissionRule;
	private AddMemberRule  addMemberRule;
	private AddDelegationRule addDelegationRule;
	private AddDelegationMemberRule addDelegationMemberRule;
	private BusinessObjectService businessObjectService;
	private ResponsibilityService responsibilityService;
	private Class<? extends AddResponsibilityRule> addResponsibilityRuleClass = KimDocumentResponsibilityRule.class;
	private Class<? extends AddPermissionRule> addPermissionRuleClass = KimDocumentPermissionRule.class;
	private Class<? extends AddMemberRule> addMemberRuleClass = KimDocumentMemberRule.class;
	private Class<? extends AddDelegationRule> addDelegationRuleClass = RoleDocumentDelegationRule.class;
	private Class<? extends AddDelegationMemberRule> addDelegationMemberRuleClass = RoleDocumentDelegationMemberRule.class;

	private IdentityManagementKimDocumentAuthorizer authorizer;
	private IdentityService identityService; 
	
    public IdentityService getIdentityService() {
        if ( identityService == null) {
            identityService = KIMServiceLocator.getIdentityService();
        }
        return identityService;
    }


    @Override
    protected boolean processCustomSaveDocumentBusinessRules(Document document) {
        if (!(document instanceof IdentityManagementRoleDocument)) {
            return false;
        }

        IdentityManagementRoleDocument roleDoc = (IdentityManagementRoleDocument)document;
        boolean valid = true;

        GlobalVariables.getErrorMap().addToErrorPath(KNSConstants.DOCUMENT_PROPERTY_NAME);

        //KNSServiceLocator.getDictionaryValidationService().validateDocument(document);
        getDictionaryValidationService().validateDocumentAndUpdatableReferencesRecursively(document, getMaxDictionaryValidationDepth(), true, false);
        valid &= validateRoleQualifier(roleDoc.getMembers(), roleDoc.getKimType());
        
        valid &= validRoleMemberActiveDates(roleDoc.getMembers());

        // all failed at this point.
//        valid &= checkUnassignableRoles(roleDoc);
//        valid &= checkUnpopulatableGroups(roleDoc);
        validRoleResponsibilitiesActions(roleDoc.getResponsibilities());
        validRoleMembersResponsibilityActions(roleDoc.getMembers());
        GlobalVariables.getErrorMap().removeFromErrorPath(KNSConstants.DOCUMENT_PROPERTY_NAME);

        return valid;
    }
    
    private boolean validRoleMemberActiveDates(List<KimDocumentRoleMember> roleMembers) {
    	boolean valid = true;
		int i = 0;
    	for(KimDocumentRoleMember roleMember: roleMembers) {
   			valid &= validateActiveDate("members["+i+"].activeToDate", roleMember.getActiveFromDate(), roleMember.getActiveToDate());
    		i++;
    	}
    	return valid;
    }
    
    private boolean validRoleResponsibilitiesActions(List<KimDocumentRoleResponsibility> roleResponsibilities){
        int i = 0;
        boolean rulePassed = true;
    	for(KimDocumentRoleResponsibility roleResponsibility: roleResponsibilities){
    		if(!getResponsibilityService().areActionsAtAssignmentLevelById(roleResponsibility.getResponsibilityId()))
    			validateRoleResponsibilityAction("responsibilities["+i+"].roleRspAction[0]", roleResponsibility.getRoleRspActions().get(0));
        	i++;
    	}
    	return rulePassed;
    }

    private boolean validRoleMembersResponsibilityActions(List<KimDocumentRoleMember> roleMembers){
        int i = 0;
        int j;
        boolean rulePassed = true;
    	for(KimDocumentRoleMember roleMember: roleMembers){
    		j = 0;
    		if(roleMember.getRoleRspActions()!=null && !roleMember.getRoleRspActions().isEmpty()){
	    		for(KimDocumentRoleResponsibilityAction roleRspAction: roleMember.getRoleRspActions()){
	    			validateRoleResponsibilityAction("members["+i+"].roleRspAction["+j+"]", roleRspAction);
		        	j++;
	    		}
    		}
    		i++;
    	}
    	return rulePassed;
    }

    private boolean validateRoleResponsibilityAction(String errorPath, KimDocumentRoleResponsibilityAction roleRspAction){
    	boolean rulePassed = true;
    	if(StringUtils.isBlank(roleRspAction.getActionPolicyCode())){
    		GlobalVariables.getErrorMap().putError(errorPath, 
    				RiceKeyConstants.ERROR_EMPTY_ENTRY, new String[] {"Action Policy Code"});
    		rulePassed = false;
    	}
    	if(roleRspAction.getPriorityNumber()==null){
    		GlobalVariables.getErrorMap().putError(errorPath, 
    				RiceKeyConstants.ERROR_EMPTY_ENTRY, new String[] {"Priority Number"});
    		rulePassed = false;
    	}
    	if(StringUtils.isBlank(roleRspAction.getActionTypeCode())){
    		GlobalVariables.getErrorMap().putError(errorPath, 
    				RiceKeyConstants.ERROR_EMPTY_ENTRY, new String[] {"Action Type Code"});
    		rulePassed = false;
    	}
    	return rulePassed;
    }
    
    private boolean validateRoleQualifier(List<KimDocumentRoleMember> roleMembers, KimTypeImpl kimType){
        ErrorMap errorMap = GlobalVariables.getErrorMap();
		AttributeSet validationErrors = new AttributeSet();

    	for(KimDocumentRoleMember roleMember: roleMembers) {
	        KimTypeService kimTypeService = (KimTypeServiceBase)KIMServiceLocator.getService(kimType.getKimTypeServiceName());
	        AttributeSet attributes = new AttributeSet();
	        for(KimDocumentRoleQualifier roleQualifier: roleMember.getQualifiers()) {
	        	Map<String, String> attr = new HashMap<String, String>();
	        	KimAttributeImpl attribute = roleQualifier.getKimAttribute();
	        	if(attribute==null){
	        		Map<String, String> criteria = new HashMap<String, String>();
	        		criteria.put("kimAttributeId", roleQualifier.getKimAttrDefnId());
	        		attribute = (KimAttributeImpl)KNSServiceLocator.getBusinessObjectService().findByPrimaryKey(KimAttributeImpl.class, criteria);
	        	}
	        	attr.put(attribute.getAttributeName(), roleQualifier.getAttrVal());
	        	attributes.putAll(attr);
	        }
	        validationErrors.putAll(kimTypeService.validateAttributes(attributes));
    	}
    	if (validationErrors.isEmpty()) {
    		return true;
    	} else {
    		return false;
    	}
    }

	private boolean validateActiveDate(String errorPath, Timestamp activeFromDate, Timestamp activeToDate) {
		// TODO : do not have detail bus rule yet, so just check this for now.
		boolean valid = true;
		if (activeFromDate != null && activeToDate !=null && activeToDate.before(activeFromDate)) {
	        ErrorMap errorMap = GlobalVariables.getErrorMap();
            errorMap.putError(errorPath, RiceKeyConstants.ERROR_ACTIVE_TO_DATE_BEFORE_FROM_DATE);
            valid = false;
			
		}
		return valid;
	}
	
	/**
	 * @return the addResponsibilityRule
	 */
	public AddResponsibilityRule getAddResponsibilityRule() {
		if(addResponsibilityRule == null){
			try {
				addResponsibilityRule = addResponsibilityRuleClass.newInstance();
			} catch ( Exception ex ) {
				throw new RuntimeException( "Unable to create AddResponsibilityRule instance using class: " + addResponsibilityRuleClass, ex );
			}
		}
		return addResponsibilityRule;
	}

	/**
	 * @return the addPermissionRule
	 */
	public AddPermissionRule getAddPermissionRule() {
		if(addPermissionRule == null){
			try {
				addPermissionRule = addPermissionRuleClass.newInstance();
			} catch ( Exception ex ) {
				throw new RuntimeException( "Unable to create AddPermissionRule instance using class: " + addPermissionRuleClass, ex );
			}
		}
		return addPermissionRule;
	}
	
	/**
	 * @return the addMemberRule
	 */
	public AddMemberRule getAddMemberRule() {
		if(addMemberRule == null){
			try {
				addMemberRule = addMemberRuleClass.newInstance();
			} catch ( Exception ex ) {
				throw new RuntimeException( "Unable to create AddMemberRule instance using class: " + addMemberRuleClass, ex );
			}
		}
		return addMemberRule;
	}

	/**
	 * @return the addDelegationRule
	 */
	public AddDelegationRule getAddDelegationRule() {
		if(addDelegationRule == null){
			try {
				addDelegationRule = addDelegationRuleClass.newInstance();
			} catch ( Exception ex ) {
				throw new RuntimeException( "Unable to create AddDelegationRule instance using class: " + addDelegationRuleClass, ex );
			}
		}
		return addDelegationRule;
	}

	/**
	 * @return the addDelegationMemberRule
	 */
	public AddDelegationMemberRule getAddDelegationMemberRule() {
		if(addDelegationMemberRule == null){
			try {
				addDelegationMemberRule = addDelegationMemberRuleClass.newInstance();
			} catch ( Exception ex ) {
				throw new RuntimeException( "Unable to create AddDelegationMemberRule instance using class: " + addDelegationMemberRuleClass, ex );
			}
		}
		return addDelegationMemberRule;
	}
	
    public boolean processAddPermission(AddPermissionEvent addPermissionEvent) {
        return new KimDocumentPermissionRule().processAddPermission(addPermissionEvent);    
    }

    public boolean processAddResponsibility(AddResponsibilityEvent addResponsibilityEvent) {
        return new KimDocumentResponsibilityRule().processAddResponsibility(addResponsibilityEvent);    
    }

    public boolean processAddMember(AddMemberEvent addMemberEvent) {
        return new KimDocumentMemberRule().processAddMember(addMemberEvent);    
    }

    public boolean processAddDelegation(AddDelegationEvent addDelegationEvent) {
        return new RoleDocumentDelegationRule().processAddDelegation(addDelegationEvent);    
    }

    public boolean processAddDelegationMember(AddDelegationMemberEvent addDelegationMemberEvent) {
        return new RoleDocumentDelegationMemberRule().processAddDelegationMember(addDelegationMemberEvent);    
    }

	public IdentityManagementKimDocumentAuthorizer getAuthorizer(IdentityManagementKimDocument document) {
		if ( authorizer == null ) {
			//authorizer = (IdentityManagementPersonDocumentAuthorizer)KEWServiceLocator.getDocumentTypeService().getDocumentAuthorizer(document);
		}
		return authorizer;
	}

	public ResponsibilityService getResponsibilityService() {
		if(responsibilityService == null){
			responsibilityService = KIMServiceLocator.getResponsibilityService();
		}
		return responsibilityService;
	}

}
