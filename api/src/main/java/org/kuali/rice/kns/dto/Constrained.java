/*
 * Copyright 2011 The Kuali Foundation
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
package org.kuali.rice.kns.dto;

import java.util.List;

/**
 * This interface defines methods that must be implemented by classes that you want to participate in dictionary validation as for example 'attribute-level' object structure
 * definitions. 
 * 
 * @author James Renfro, University of Washington 
 */
public interface Constrained extends ExistenceConstrained, QuantityConstrained, HierarchicallyConstrained, LengthConstrained, SizeConstrained {

	public CaseConstraint getCaseConstraint();
	
	public DataType getDataType();
	
	public String getLabel();
	
	// FIXME: rename to getLookupConstraint()
	public LookupConstraint getLookupDefinition();
	
	// FIXME: rename to getMustOccursConstraints()
	public List<MustOccurConstraint> getOccursConstraint();
	
	public String getName();
	
	// FIXME: rename to getRequiredConstraints()
	public List<RequiredConstraint> getRequireConstraint();
	
	// FIXME: rename to getValidCharactersConstraint()
	public ValidCharsConstraint getValidChars();
	
}
