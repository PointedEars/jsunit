package net.jsunit;

import net.jsunit.configuration.Configuration;
import net.jsunit.model.Browser;
import net.jsunit.model.BrowserResult;
import net.jsunit.model.HeterogenousBrowserGroup;
import net.jsunit.model.TestRunResult;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestRunManager implements TestRunListener {

    private BrowserTestRunner testRunner;
    private TestRunResult testRunResult;
    private final String overrideUrl;
    private List<HeterogenousBrowserGroup> groups;
    private HeterogenousBrowserGroup currentGroup;
    private int currentGroupResultCount;

    public static void main(String[] args) throws Exception {
        JsUnitStandardServer server = new JsUnitStandardServer(Configuration.resolve(args), true);
        int port = Integer.parseInt(args[args.length - 1]);
        if (noLogging(args))
            shutOffAllLogging();
        server.addTestRunListener(new TestRunNotifierServer(server, port));
        server.start();
        TestRunManager manager = new TestRunManager(server);
        manager.runTests();
        if (server.isAlive())
            server.dispose();
    }

    private static void shutOffAllLogging() {
        Logger.getLogger("net.jsunit").setLevel(Level.OFF);
        Logger.getLogger("org.mortbay").setLevel(Level.OFF);
        Logger.getLogger("com.opensymphony").setLevel(Level.OFF);
    }

    private static boolean noLogging(String[] arguments) {
        for (String string : arguments)
            if (string.equals("-noLogging"))
                return true;
        return false;
    }

    public TestRunManager(BrowserTestRunner testRunner) {
        this(testRunner, null);
    }

    public TestRunManager(BrowserTestRunner testRunner, String overrideUrl) {
        this.testRunner = testRunner;
        this.overrideUrl = overrideUrl;
        setBrowsers(testRunner.getBrowsers());
    }

    private void setBrowsers(List<Browser> browsers) {
        groups = HeterogenousBrowserGroup.createFrom(browsers);
    }

    public void runTests() {
        initializeTestRunResult();
        testRunner.addTestRunListener(this);
        testRunner.logStatus("Starting Test Run");
        testRunner.startTestRun();
        try {
            for (HeterogenousBrowserGroup group : groups) {
                newGroup(group);
                for (Browser browser : group) {
                    BrowserLaunchSpecification launchSpec = new BrowserLaunchSpecification(browser, overrideUrl);
                    testRunner.launchBrowserTestRun(launchSpec);
                    testRunner.logStatus("Waiting for " + browser.getDisplayName() + " to submit result");
                }
                waitForResultsToBeSubmitted();
            }
        } finally {
            testRunner.removeTestRunListener(this);
            testRunner.finishTestRun();
        }
        testRunner.logStatus("Test Run Completed");
    }

    private void newGroup(HeterogenousBrowserGroup group) {
        currentGroup = group;
        currentGroupResultCount = 0;
    }

    private void initializeTestRunResult() {
        testRunResult = new TestRunResult();
        testRunResult.initializeProperties();
    }

    private void waitForResultsToBeSubmitted() {
        long secondsWaited = 0;
        while (testRunner.isAlive() && currentGroupResultCount < currentGroup.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            secondsWaited++;
            if (secondsWaited > testRunner.timeoutSeconds() + 3)
                throw new RuntimeException("Server not responding");
        }
    }

    public TestRunResult getTestRunResult() {
        return testRunResult;
    }

    public void limitToBrowsers(List<Browser> browsers) {
        setBrowsers(browsers);
    }

    public boolean isReady() {
        return true;
    }

    public void testRunStarted() {
    }

    public void testRunFinished() {
    }

    public void browserTestRunStarted(Browser browser) {
    }

    public void browserTestRunFinished(Browser browser, BrowserResult result) {
        testRunResult.addBrowserResult(result);
        currentGroupResultCount ++;
    }

    public List<HeterogenousBrowserGroup> getGroups() {
        return groups;
    }
}
