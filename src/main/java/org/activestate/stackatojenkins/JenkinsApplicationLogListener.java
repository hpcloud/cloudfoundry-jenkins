package org.activestate.stackatojenkins;

import hudson.model.BuildListener;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

import java.util.ArrayList;
import java.util.List;

public class JenkinsApplicationLogListener implements ApplicationLogListener {

    private BuildListener listener;

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
