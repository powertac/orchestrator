/*
 * Copyright (c) 2011 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.tournament.services;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Static methods to access the Spring application context. It is set up
 * as a service so Spring will create and initialize it.
 *
 * @author John Collins
 */
@Service
public class SpringApplicationContext implements ApplicationContextAware
{
  private static ApplicationContext context;

  @Override
  public void setApplicationContext (ApplicationContext appContext)
      throws BeansException
  {
    context = appContext;
  }

  /**
   * Returns the Spring bean, if any, with the given name.
   */
  public static Object getBean (String beanName)
  {
    return context.getBean(beanName);
  }
}