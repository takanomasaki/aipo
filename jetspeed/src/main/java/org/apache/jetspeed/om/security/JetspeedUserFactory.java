/*
 * Copyright 2000-2001,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jetspeed.om.security;

import org.apache.jetspeed.services.security.JetspeedSecurityService;
import org.apache.jetspeed.services.security.UserException;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.resources.ResourceService;

/**
 * Factory class for creating Jetspeed Users. The user class is configured in
 * the JR.p
 * 
 * @author <a href="mailto:taylor@apache.org">David Sean Taylor</a>
 * @version $Id: JetspeedUserFactory.java,v 1.4 2004/02/23 03:14:12 jford Exp $
 */
public class JetspeedUserFactory {
  private static final String CONFIG_USER_CLASSNAME = "user.class";

  private static String userClassName = null;

  private static Class userClass = null;

  /**
   * Factory method to create JetspeedUser instances.
   * 
   * 
   * @throws UnknownEntityException
   *           when the user instance cant be created.
   * @return JetspeedUser a new created user.
   */
  public static JetspeedUser getInstance() throws UserException {
    return getInstance(true);
  }

  public static JetspeedUser getInstance(boolean isNew) throws UserException {
    JetspeedUser user = null;

    if (null == userClassName) {
      try {
        ResourceService serviceConf =
          ((TurbineServices) TurbineServices.getInstance())
            .getResources(JetspeedSecurityService.SERVICE_NAME);
        userClassName = serviceConf.getString(CONFIG_USER_CLASSNAME);
        userClass = Class.forName(userClassName);
      } catch (Exception e) {
        throw new UserException(
          "JetspeedUserFactory: Failed to create a Class object for User implementation: "
            + e.toString());
      }
    }

    try {
      if (userClass != null) {
        user = (JetspeedUser) userClass.newInstance();
      }
      if (user instanceof BaseJetspeedUser) {
        ((BaseJetspeedUser) user).setNew(isNew);
      }
    } catch (Exception e) {
      throw new UserException(
        "Failed instantiate an User implementation object: " + e.toString());
    }

    return user;
  }

}
