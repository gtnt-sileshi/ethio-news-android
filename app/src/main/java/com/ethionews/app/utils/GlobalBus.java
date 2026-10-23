package com.ethionews.app.utils;

import org.greenrobot.eventbus.EventBus;

public class GlobalBus {

    private GlobalBus() {
        throw new IllegalStateException("Utility class");
    }

    private static EventBus sBus;
    public static EventBus getBus() {
        if (sBus == null)
            sBus = EventBus.getDefault();
        return sBus;
    }
}
