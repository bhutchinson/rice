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
package org.kuali.rice.kns.web.spring.form;

import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.kuali.rice.core.service.EncryptionService;
import org.kuali.rice.kns.bo.BusinessObject;
import org.kuali.rice.kns.bo.Exporter;
import org.kuali.rice.kns.datadictionary.BusinessObjectEntry;
import org.kuali.rice.kns.exception.UnknownBusinessClassAttributeException;
import org.kuali.rice.kns.inquiry.Inquirable;
import org.kuali.rice.kns.service.BusinessObjectAuthorizationService;
import org.kuali.rice.kns.service.BusinessObjectMetaDataService;
import org.kuali.rice.kns.service.DataDictionaryService;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.kns.service.KualiModuleService;
import org.kuali.rice.kns.service.ModuleService;
import org.kuali.rice.kns.util.KNSConstants;

/**
 * This class is the model for Inquiries. 
 * 
 * @author Kuali Rice Team (rice.collab@kuali.org)
 *
 */
public class InquiryForm extends UifFormBase {
    
    private static final Logger LOG = Logger.getLogger(InquiryForm.class);
    
    private String businessObjectClassName;
    private boolean canExport;
    
    /**
     * The following map is used to pass primary key values between invocations of the inquiry screens after the start method has been called.  Values in this map will remain encrypted
     * if the value was passed in as encrypted
     */
    private Map<String, String> inquiryPrimaryKeys;

    private Map<String, String> inquiryDecryptedPrimaryKeys;
    
    
    private BusinessObject bo;
    private Inquirable inquirable;
    
    
    protected Inquirable getInquirable(String boClassName) {
        try {
            Class<? extends Inquirable> customInquirableClass = null;

            try {
                customInquirableClass = KNSServiceLocator.getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(boClassName).getInquiryDefinition().getInquirableClass();
            }
            catch (Exception e) {
                LOG.error("Unable to correlate business object class with maintenance document entry");
            }

            Inquirable kualiInquirable;

            if (customInquirableClass != null) {
                Class<?>[] defaultConstructor = new Class[] {};
                Constructor<? extends Inquirable> cons = customInquirableClass.getConstructor(defaultConstructor);
                kualiInquirable = cons.newInstance();
            }
            else {
                kualiInquirable = KNSServiceLocator.getKualiInquirable();
            }

            kualiInquirable.setBusinessObjectClass(Class.forName(boClassName));

            return kualiInquirable;
        }
        catch (Exception e) {
            LOG.error("Error attempting to retrieve inquirable.", e);
            throw new RuntimeException("Error attempting to retrieve inquirable.");
        }
    }

    public void populate(HttpServletRequest request) {

        // if the action is download attachment then skip the following populate logic
        if (!KNSConstants.DOWNLOAD_BO_ATTACHMENT_METHOD.equals(getMethodToCall())) {
            inquirable = getInquirable(getBusinessObjectClassName());

            // the following variable is true if the method to call is not start, meaning that we already called start
            boolean passedFromPreviousInquiry = !KNSConstants.START_METHOD.equals(getMethodToCall()) && !KNSConstants.CONTINUE_WITH_INQUIRY_METHOD_TO_CALL.equals(getMethodToCall()) && !KNSConstants.DOWNLOAD_CUSTOM_BO_ATTACHMENT_METHOD.equals(getMethodToCall());

            // There is no requirement that an inquiry screen must display the primary key values. However, when clicking on
            // hide/show (without javascript) and
            // hide/show inactive, the PK values are needed to allow the server to properly render results after the user
            // clicks on a hide/show button that results
            // in server processing. This line will populate the form with the PK values so that they may be used in
            // subsequent requests. Note that encrypted
            // values will remain encrypted in this map.
            this.inquiryPrimaryKeys = new HashMap<String, String>();
            this.inquiryDecryptedPrimaryKeys = new HashMap<String, String>();

            populatePKFieldValues(request, getBusinessObjectClassName(), passedFromPreviousInquiry);

            //populateInactiveRecordsInIntoInquirable(inquirable, request);
            populateExportCapabilities(getBusinessObjectClassName());
        }
    }
    
    protected void populatePKFieldValues(HttpServletRequest request, String boClassName, boolean passedFromPreviousInquiry) {
        try {
            EncryptionService encryptionService = KNSServiceLocator.getEncryptionService();
            DataDictionaryService dataDictionaryService = KNSServiceLocator.getDataDictionaryService();
            BusinessObjectAuthorizationService businessObjectAuthorizationService = KNSServiceLocator.getBusinessObjectAuthorizationService();
            BusinessObjectMetaDataService businessObjectMetaDataService = KNSServiceLocator.getBusinessObjectMetaDataService();

            @SuppressWarnings("unchecked")
            Class<? extends BusinessObject> businessObjectClass = (Class<? extends BusinessObject>) Class.forName(boClassName);

            // build list of key values from request, if all keys not given throw error
            List<String> boPKeys = businessObjectMetaDataService.listPrimaryKeyFieldNames(businessObjectClass);
            final List<List<String>> altKeys = this.getAltkeys(businessObjectClass);

            altKeys.add(boPKeys);
            boolean bFound = false;
            for (List<String> boKeys : altKeys) {
                if (bFound)
                    break;
                int keyCount = boKeys.size();
                int foundCount = 0;
                for (String boKey : boKeys) {
                    String pkParamName = boKey;
                    if (passedFromPreviousInquiry) {
                        pkParamName = KNSConstants.INQUIRY_PK_VALUE_PASSED_FROM_PREVIOUS_REQUEST_PREFIX + pkParamName;
                    }

                    if (request.getParameter(pkParamName) != null) {
                        foundCount++;
                        String parameter = request.getParameter(pkParamName);

                        Boolean forceUppercase = Boolean.FALSE;
                        try {
                            forceUppercase = dataDictionaryService.getAttributeForceUppercase(businessObjectClass, boKey);
                        } catch (UnknownBusinessClassAttributeException ex) {
                            // swallowing exception because this check for ForceUppercase would
                            // require a DD entry for the attribute. it is only checking keys
                            // so most likely there should be an entry.
                            LOG.warn("BO class " + businessObjectClassName + " property " + boKey + " should probably have a DD definition.", ex);
                        }

                        if (forceUppercase.booleanValue()) {
                            parameter = parameter.toUpperCase();
                        }

                        inquiryPrimaryKeys.put(boKey, parameter);
                        if (businessObjectAuthorizationService.attributeValueNeedsToBeEncryptedOnFormsAndLinks(businessObjectClass, boKey)) {
                            try {
                                inquiryDecryptedPrimaryKeys.put(boKey, encryptionService.decrypt(parameter));
                            } catch (GeneralSecurityException e) {
                                LOG.error("BO class " + businessObjectClassName + " property " + boKey + " should have been encrypted, but there was a problem decrypting it.");
                                throw e;
                            }
                        } else {
                            inquiryDecryptedPrimaryKeys.put(boKey, parameter);
                        }
                    }
                }
                if (foundCount == keyCount) {
                    bFound = true;
                }
            }
            if (!bFound) {
                LOG.error("All keys not given to lookup for bo class name " + businessObjectClass.getName());
                throw new RuntimeException("All keys not given to lookup for bo class name " + businessObjectClass.getName());
            }
        } catch (ClassNotFoundException e) {
            LOG.error("Can't instantiate class: " + boClassName, e);
            throw new RuntimeException("Can't instantiate class: " + boClassName);
        } catch (GeneralSecurityException e) {
            LOG.error("Can't decrypt value", e);
            throw new RuntimeException("Can't decrypt value");
        }
    }
    
    /**
     * Examines the BusinessObject's data dictionary entry to determine if it supports
     * XML export or not and set's canExport appropriately.
     */
    protected void populateExportCapabilities(String boClassName) {
        setCanExport(false);
        BusinessObjectEntry businessObjectEntry = KNSServiceLocator.getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(boClassName);
        Class<? extends Exporter> exporterClass = businessObjectEntry.getExporterClass();
        if (exporterClass != null) {
            try {
                Exporter exporter = exporterClass.newInstance();
                if (exporter.getSupportedFormats(businessObjectEntry.getBusinessObjectClass()).contains(KNSConstants.XML_FORMAT)) {
                    setCanExport(true);
                }
            } catch (Exception e) {
                LOG.error("Failed to locate or create exporter class: " + exporterClass, e);
                throw new RuntimeException("Failed to locate or create exporter class: " + exporterClass);
            }
        }
    }
    
    /**
     * Gets the alt keys for a class.  Will not return null but and empty list if no keys exist.
     *
     * @param clazz the class.
     * @return the alt keys
     */
    private List<List<String>> getAltkeys(Class<?> clazz) {
        final KualiModuleService kualiModuleService = KNSServiceLocator.getKualiModuleService();
        final ModuleService moduleService = kualiModuleService.getResponsibleModuleService(clazz);

        List<List<String>> altKeys = null;
        if (moduleService != null) {
            altKeys = moduleService.listAlternatePrimaryKeyFieldNames(clazz);
        }

        return altKeys != null ? altKeys : new ArrayList<List<String>>();
    }

    public String getBusinessObjectClassName() {
        return this.businessObjectClassName;
    }
    
    public void setBusinessObjectClassName(String businessObjectClassName) {
        this.businessObjectClassName = businessObjectClassName;
    }
    
    public BusinessObject getBo() {
        return this.bo;
    }
    
    public void setBo(BusinessObject bo) {
        this.bo = bo;
    }

    public Inquirable getInquirable() {
        return this.inquirable;
    }

    public void setInquirable(Inquirable inquirable) {
        this.inquirable = inquirable;
    }

    public boolean isCanExport() {
        return this.canExport;
    }

    public void setCanExport(boolean canExport) {
        this.canExport = canExport;
    }

    public Map<String, String> getInquiryPrimaryKeys() {
        return this.inquiryPrimaryKeys;
    }

    /**
     * Gets the map used to pass primary key values between invocations of the inquiry screens after the start method has been called.  All fields will be decrypted
     *
     * Purposely not named as a getter, to hide from binders
     *
     * @return
     */
    public Map<String, String> retrieveInquiryDecryptedPrimaryKeys() {
        return this.inquiryDecryptedPrimaryKeys;
    }
    
    

}
