package acr.browser.lightning.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mActivity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        // Espresso does not start the Activity for you we need to do this manually here.
        mActivity = getActivity();
    }

    @Test
    public void testPreconditions() {
        Assert.assertNotNull(mActivity);
    }


    @Test
    public void testIsIncognito() {
        Assert.assertFalse(mActivity.isIncognito());
    }

    @Test
    public void testContentDisplayed() {

    }
}