/**
 * ©Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Copyright (c) ActiveState 2014 - ALL RIGHTS RESERVED.
 */

package com.hpe.cloudfoundryjenkins;

import hudson.model.BuildListener;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

public class JenkinsApplicationLogListener implements ApplicationLogListener {

    private final BuildListener listener;

    public JenkinsApplicationLogListener(BuildListener listener) {
        this.listener = listener;
    }

    public void onMessage(ApplicationLog applicationLog) {
        listener.getLogger().println(applicationLog.getMessage());
    }

    public void onComplete() {
    }

    public void onError(Throwable throwable) {
        listener.getLogger().println("ERROR: Could not retrieve staging logs via websocket");
    }
}
