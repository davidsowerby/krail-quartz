/*
 *
 *  * Copyright (c) 2016. David Sowerby
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */
package uk.q3c.krail.quartz.service;

import com.google.inject.Inject;
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import com.vaadin.server.VaadinService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.q3c.krail.core.config.ApplicationConfigurationModule;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.core.services.ServicesModel;
import uk.q3c.krail.core.services.ServicesModule;
import uk.q3c.krail.quartz.job.DefaultJobModule;
import uk.q3c.krail.quartz.scheduler.DefaultSchedulerModule;
import uk.q3c.krail.quartz.scheduler.KrailScheduler;
import uk.q3c.krail.quartz.scheduler.SchedulerProvider;
import uk.q3c.krail.testutil.guice.uiscope.TestUIScopeModule;
import uk.q3c.krail.testutil.i18n.TestI18NModule;
import uk.q3c.krail.testutil.option.TestOptionModule;
import uk.q3c.krail.testutil.persist.TestPersistenceModule;
import uk.q3c.krail.util.UtilsModule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({TestI18NModule.class, TestOptionModule.class, TestPersistenceModule.class, DefaultSchedulerModule.class, ApplicationConfigurationModule.class,
        DefaultJobModule.class, ServicesModule.class, EventBusModule.class, TestUIScopeModule.class, VaadinSessionScopeModule.class, UtilsModule.class})
public class DefaultQuartzServiceTest {

    static File iniDir = new File("src/test/java");
    static VaadinService vaadinService;
    @Inject
    DefaultQuartzService service;
    @Inject
    SchedulerProvider provider;
    @Inject
    ServicesModel servicesModel;


    @BeforeClass
    public static void setupClass() {
        vaadinService = mock(VaadinService.class);
        when(vaadinService.getBaseDirectory()).thenReturn(iniDir);
        VaadinService.setCurrent(vaadinService);
    }

    @Test
    public void defaultSingleScheduler() throws Exception {

        // given
        servicesModel.addService(service.getServiceKey());
        // when
        service.start();
        // then
        KrailScheduler scheduler = provider.get();
        assertThat(scheduler.isStarted()).isTrue();
        assertThat(scheduler.getMetaData()
                            .getSchedulerName()).isEqualTo("default");
    }


}
