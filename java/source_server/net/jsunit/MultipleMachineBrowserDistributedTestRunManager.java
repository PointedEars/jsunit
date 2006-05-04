package net.jsunit;

import net.jsunit.configuration.Configuration;

import java.net.URL;
import java.util.List;

public class MultipleMachineBrowserDistributedTestRunManager extends DistributedTestRunManager {
    private List<URL> remoteMachineURLs;

    protected MultipleMachineBrowserDistributedTestRunManager(
            RemoteServerHitter hitter, Configuration localConfiguration, List<URL> remoteMachineURLs, String overrideURL) {
        super(hitter, localConfiguration, overrideURL);
        this.remoteMachineURLs = remoteMachineURLs;
    }

    public List<URL> remoteMachineURLs() {
        return remoteMachineURLs;
    }

}
