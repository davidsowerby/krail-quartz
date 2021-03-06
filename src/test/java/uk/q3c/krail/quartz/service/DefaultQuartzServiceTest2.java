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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.quartz.*;
import org.quartz.listeners.BroadcastSchedulerListener;
import uk.q3c.krail.core.config.ApplicationConfigurationModule;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.core.services.ServicesModel;
import uk.q3c.krail.core.services.ServicesModule;
import uk.q3c.krail.quartz.job.JobModuleBase;
import uk.q3c.krail.quartz.scheduler.*;
import uk.q3c.krail.quartz.service.DefaultQuartzServiceTest2.TestJobModule;
import uk.q3c.krail.quartz.service.DefaultQuartzServiceTest2.TestSchedulerModule;
import uk.q3c.krail.testutil.guice.uiscope.TestUIScopeModule;
import uk.q3c.krail.testutil.i18n.TestI18NModule;
import uk.q3c.krail.testutil.option.TestOptionModule;
import uk.q3c.krail.testutil.persist.TestPersistenceModule;
import uk.q3c.krail.util.UtilsModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({TestI18NModule.class, TestOptionModule.class, TestPersistenceModule.class, DefaultSchedulerModule.class, ApplicationConfigurationModule.class,
        TestSchedulerModule.class, TestJobModule.class, ServicesModule.class, EventBusModule.class, TestUIScopeModule.class, VaadinSessionScopeModule.class,
        UtilsModule.class})
public class DefaultQuartzServiceTest2 {

    static JobKey jobKey = new JobKey("wiggly", "blob");
    @Inject
    DefaultQuartzService service;
    @Inject
    SchedulerProvider provider;
    @Inject
    TestJobMonitor monitor;
    @Inject
    ServicesModel servicesModel;

    @Mock
    VaadinService vaadinService;

    @Before
    public void setup() {
        VaadinService.setCurrent(vaadinService);
        servicesModel.addService(service.getServiceKey());
    }

    @Test
    public void secondScheduler() throws Exception {

        // given

        // when
        service.start();
        // then
        KrailScheduler defaultScheduler = provider.get();
        assertThat(defaultScheduler.isStarted()).isTrue();
        assertThat(defaultScheduler.getMetaData()
                                   .getSchedulerName()).isEqualTo("default");

        KrailScheduler testScheduler = provider.get("test");
        assertThat(testScheduler).isNotNull();
        assertThat(testScheduler.isStarted()).isFalse();
        assertThat(testScheduler.getMetaData()
                                .getSchedulerName()).isEqualTo("test");
        assertThat(testScheduler.getListenerManager()
                                .getSchedulerListeners()).hasSize(1);
        SchedulerListener schedulerListener = testScheduler.getListenerManager()
                                                           .getSchedulerListeners()
                                                           .get(0);
        assertThat(schedulerListener).isInstanceOf(BroadcastSchedulerListener.class);

        assertThat(testScheduler.getListenerManager()
                                .getTriggerListeners()).hasSize(1);
        TriggerListener triggerListener = testScheduler.getListenerManager()
                                                       .getTriggerListeners()
                                                       .get(0);
        assertThat(triggerListener).isInstanceOf(TestTriggerListener.class);
        assertThat(triggerListener.getName()).isEqualTo("splug");
    }

    @Test
    public void job() throws Exception {

        // given
        KrailScheduler testScheduler = provider.get("test");
        testScheduler.deleteJob(jobKey);
        // when
        service.start();
        testScheduler.start();
        Thread.sleep(1500);
        // then
        assertThat(monitor.getEntry("Job")).isEqualTo("fired");
        assertThat(monitor.getEntry("Listener")).isEqualTo("was executed");

    }

    public static class TestTriggerListener extends KrailTriggerListenerSupport {

    }

    public static class TestSchedulerModule extends SchedulerModuleBase {

        @Override
        protected void addConfigurations() {
            addConfiguration("test", false);
        }

        @Override
        protected void addSchedulerListeners() {
            addSchedulerListener("test", BroadcastSchedulerListener.class);
        }

        @Override
        protected void addTriggerListeners() {
            addTriggerListener("test", "splug", TestTriggerListener.class);
        }
    }

    public static class TestJobModule extends JobModuleBase {

        @Override
        protected void addJobs() {
            JobBuilder jobBuilder = newJob().ofType(TestJob.class)
                                            .withIdentity(jobKey);
            simpleSchedule();
            TriggerBuilder<SimpleTrigger> triggerBuilder = newTrigger().startNow()
                                                                       .withSchedule(SimpleScheduleBuilder.repeatSecondlyForTotalCount(5));
            addJob("test", jobBuilder, triggerBuilder);
            addJobListener("test", TestJobListener.class, jobKey);
        }

    }
}
