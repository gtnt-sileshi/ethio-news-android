package com.ethionews.app.utils;

public class Events {

    private Events() {
        throw new IllegalStateException("Utility class");
    }

    public static class FullScreen {
        private boolean isFullScreen = false;

        public boolean isFullScreen() {
            return isFullScreen;
        }

        public void setFullScreen(boolean fullScreen) {
            isFullScreen = fullScreen;
        }
    }
}
