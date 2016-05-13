package acr.browser.lightning.activity.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import acr.browser.lightning.activity.MainActivity2Test;
import acr.browser.lightning.activity.MainActivityTest;

/**
 * Test suite that runs all tests, unit + instrumentation tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({MainActivityTest.class, MainActivity2Test.class})
public class FlintTestSuite {}
