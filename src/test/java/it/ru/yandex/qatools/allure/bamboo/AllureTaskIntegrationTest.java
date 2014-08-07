package it.ru.yandex.qatools.allure.bamboo;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.atlassian.sal.api.ApplicationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.qatools.allure.bamboo.AllureTask;

@RunWith(AtlassianPluginsTestRunner.class)
public class AllureTaskIntegrationTest
{
    private final ApplicationProperties applicationProperties;
    private final AllureTask allureTask;

    public AllureTaskIntegrationTest(ApplicationProperties applicationProperties, AllureTask allureTask)
    {
        this.applicationProperties = applicationProperties;
        this.allureTask = allureTask;
    }

    @Test
    public void testMyName()
    {
        //To be implemented!
    }
}