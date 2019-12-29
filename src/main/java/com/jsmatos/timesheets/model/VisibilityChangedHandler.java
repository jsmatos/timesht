package com.jsmatos.timesheets.model;

@FunctionalInterface
public interface VisibilityChangedHandler {
    void onVisibilityChanged(boolean visible);
}
