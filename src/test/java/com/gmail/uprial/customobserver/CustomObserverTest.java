package com.gmail.uprial.customobserver;

import com.gmail.uprial.customobserver.helpers.TestConfigBase;
import org.junit.Test;

public class CustomObserverTest extends TestConfigBase {
    @Test
    public void testLoadException() throws Exception {
        CustomObserver.loadConfig(getPreparedConfig(""), getCustomLogger());
    }
}