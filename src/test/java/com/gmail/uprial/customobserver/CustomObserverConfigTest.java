package com.gmail.uprial.customobserver;

import com.gmail.uprial.customobserver.config.InvalidConfigException;
import com.gmail.uprial.customobserver.helpers.TestConfigBase;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class CustomObserverConfigTest extends TestConfigBase {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void testEmptyDebug() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'debug' flag. Use default value false");
        CustomObserverConfig.isDebugMode(getPreparedConfig(""), getDebugFearingCustomLogger());
    }

    @Test
    public void testNormalDebug() throws Exception {
        assertTrue(CustomObserverConfig.isDebugMode(getPreparedConfig("debug: true"), getDebugFearingCustomLogger()));
    }

    @Test
    public void testEmpty() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'enabled' flag. Use default value true");
        loadConfig(getDebugFearingCustomLogger(), "");
    }

    @Test
    public void testNotMap() throws Exception {
        e.expect(InvalidConfigurationException.class);
        e.expectMessage("Top level is not a Map.");
        loadConfig("x");
    }

    @Test
    public void testWrongEnabled() throws Exception {
        e.expect(InvalidConfigException.class);
        e.expectMessage("Invalid 'enabled' flag");
        loadConfig("enabled: v");
    }


    @Test
    public void testNormalConfig() throws Exception {
        assertEquals(
                "enabled: true",
                loadConfig("debug: true",
                        "enabled: true").toString());
    }
}