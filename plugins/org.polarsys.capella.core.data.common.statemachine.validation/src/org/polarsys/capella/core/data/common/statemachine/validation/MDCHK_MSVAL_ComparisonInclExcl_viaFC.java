/*******************************************************************************
 * Copyright (c) 2017 ALTRAN.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Altran - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.core.data.common.statemachine.validation;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.AbstractModelConstraint;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.emf.validation.model.ConstraintStatus;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.model.helpers.BlockArchitectureExt;
import org.polarsys.capella.vp.ms.CSConfiguration;
import org.polarsys.capella.vp.ms.Comparison;
import org.polarsys.capella.vp.ms.Result;
import org.polarsys.capella.vp.ms.Situation;
import org.polarsys.capella.vp.ms.selector_Type;

import ms.configuration.services.cs.CalculatedConfiguration;
import ms.configuration.services.cs.CalculatedConfiguration.ConfList;
import ms.configuration.services.cs.CalculatedConfiguration.LightConfiguration;
// 62
public class MDCHK_MSVAL_ComparisonInclExcl_viaFC extends AbstractModelConstraint {
  /*
   * (non-Javadoc)
   * @see org.eclipse.emf.validation.AbstractModelConstraint#validate(org.eclipse.emf.validation.IValidationContext)
   */
  @Override
  // check that no element (function, component, port, funct chain, child conf) are not
  // included and exluded by the compared configurations
  public IStatus validate(IValidationContext ctx) {
    EObject eObj = ctx.getTarget();
    Collection<IStatus> failureStatusList = new ArrayList<IStatus>();
    ArrayList<String> failureMessageArgument1 = new ArrayList<String>(); // type of the object included & excluded
    ArrayList<String> failureMessageArgument2 = new ArrayList<String>(); // name of the object included & excluded
    if (eObj instanceof Comparison) {
      Comparison comparison = (Comparison)eObj;
      // check that the comparison elements have references to two configuration elements, or one config and one situation
      int ComparisonType = -1; // -1 : comparison element is not correct. 1 : comparison  between two config. 2 : comparison  between one config and one situation
      if ((comparison.getConfiguration1().size() == 1) && (comparison.getConfiguration2().size() == 1)  && (comparison.getSituation().size() == 0) )
        ComparisonType = 1;
      if ((comparison.getConfiguration1().size() == 1) && (comparison.getConfiguration2().size() == 0) && (comparison.getSituation().size() == 1))
        ComparisonType = 2;
      if(ComparisonType == 1) // comparison between two CSConfigurations
      {
        CSConfiguration a = comparison.getConfiguration1().get(0);
        CSConfiguration b = comparison.getConfiguration2().get(0);
        if (!a.getSelector().equals(b.getSelector())) // if both config the same mode (inclusion,exclusion), the rule is not applicable
        {
          // compare functions involved in configuration a with functions listed in config b
          for(int i = 0;i<a.getFunctionalChains().size();i++)
            for(AbstractFunction jObj: a.getFunctionalChains().get(i).getInvolvedFunctions())
              for(AbstractFunction kObj: b.getFunctions())
                if(jObj.equals(kObj)){
                  failureMessageArgument1.add("Function");
                  failureMessageArgument2.add(kObj.getName());
                }
          // compare functions involved in configuration b with functions listed in config a
          for(int i = 0;i<b.getFunctionalChains().size();i++)
            for(AbstractFunction jObj: b.getFunctionalChains().get(i).getInvolvedFunctions())
              for(AbstractFunction kObj: a.getFunctions())
                if(jObj.equals(kObj)){
                  failureMessageArgument1.add("Function");
                  failureMessageArgument2.add(kObj.getName());
                }
        }
      }
      if(ComparisonType == 2) // comparison betwen a CSConfigurations and a calculatedConfiguration
      {
        Situation s = comparison.getSituation().get(0);
        Result result = null; // the comparison will be made on the result element associated to the situation (if this result is found)
        Boolean result_found = false;
        // try to find a result element that refers to the situation used for comparison
        BlockArchitecture archi = BlockArchitectureExt.getRootBlockArchitecture(s);
        TreeIterator<EObject> it  = archi.eAllContents();
        while(it.hasNext())
        {
          EObject eo = it.next();
          if(eo instanceof Result)
          {
            if (((Result)eo).getSituation().get(0).equals(s)){
              result = (Result)eo;
              result_found = true;
            }
          }
        }
        if(result_found)
        {
          CalculatedConfiguration cC = new CalculatedConfiguration(result); // JVS
          ConfList conflist = cC.Calculate(); // JVS
          // elements listed in both configurations :
          CSConfiguration a = comparison.getConfiguration1().get(0);
          boolean a_is_exclusive = false;
          comparison.getConfiguration1().get(0).getSelector();
          if(comparison.getConfiguration1().get(0).getSelector().equals(selector_Type.EXCLUSION))
            a_is_exclusive = true;
          // compare the configuration 1 with the calculated configuration (the list of 'lightconfigurations' that belongs to confList)
          // included and excluded functions
          if(!a_is_exclusive){ // if config a is inclusive, search for excluded elements in calculated configurations
            for(int i = 0;i<a.getFunctionalChains().size();i++)
              for(AbstractFunction jObj: a.getFunctionalChains().get(i).getInvolvedFunctions())
                for(LightConfiguration lc:conflist.getLightConfigurationList())
                  for(AbstractFunction kObj : lc.getExclFct())
                    if(jObj.equals(kObj)){
                      failureMessageArgument1.add("Function");
                      failureMessageArgument2.add(kObj.getName());
                    }
          }
          else{  // if config a is exclusive, search for included elements in calculated configurations
            for(AbstractFunction jObj: a.getFunctions())
              for(LightConfiguration lc:conflist.getLightConfigurationList())
                for(int i = 0;i<lc.getExclFctChain().size();i++)
                  for(AbstractFunction kObj : lc.getExclFctChain().get(i).getInvolvedFunctions())
                    if(jObj.equals(kObj)){
                      failureMessageArgument1.add("Function");
                      failureMessageArgument2.add(kObj.getName());
                    }
          }
        }else{
          System.out.println("not found result = =");
        }
      } // comparison type 2 (Configuration vs. CalculatedConfiguration)
      if(failureMessageArgument1.size() > 0)
      {
        for(int i = 0; i < failureMessageArgument1.size(); i++)
          failureStatusList.add(ctx.createFailureStatus(comparison.getName(),failureMessageArgument1.get(i),failureMessageArgument2.get(i)));
        return ConstraintStatus.createMultiStatus(ctx, failureStatusList);
      }else
        return ctx.createSuccessStatus();
    }
    return null;
  }
}
