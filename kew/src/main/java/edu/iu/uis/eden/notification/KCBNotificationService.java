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
package edu.iu.uis.eden.notification;

import javax.xml.namespace.QName;

import org.kuali.rice.core.Core;
import org.kuali.rice.kcb.service.MessagingService;
import org.kuali.rice.kcb.vo.MessageVO;
import org.kuali.rice.resourceloader.GlobalResourceLoader;

import edu.iu.uis.eden.actionitem.ActionItem;
import edu.iu.uis.eden.exception.WorkflowRuntimeException;

/**
 * NotificationService implementation that delegates to KCB 
 * 
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 */
public class KCBNotificationService extends DefaultNotificationService {
    @Override
    protected void sendNotification(ActionItem actionItem) {
        super.sendNotification(actionItem);
        // send it off to KCB
        MessagingService ms = (MessagingService) GlobalResourceLoader.getService(new QName(Core.getCurrentContextConfig().getMessageEntity(), "KCB-MessagingService"));
        MessageVO mvo = new MessageVO();
        mvo.setChannel("KEW");
        mvo.setContent("i'm a kew notification");
        mvo.setContentType("KEW notification");
        mvo.setDeliveryType(actionItem.getActionRequestCd());
        mvo.setProducer("kew@localhost");
        mvo.setTitle("i'm a kew notification");
        try {
            // just assume it's a user at this point...
            mvo.setRecipient(actionItem.getUser().getAuthenticationUserId().getId());
            ms.deliver(mvo);
        } catch (Exception e) {
            throw new WorkflowRuntimeException("could not deliver message to KCB");
        }
    }

}
