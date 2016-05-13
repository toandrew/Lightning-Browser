package acr.browser.lightning.activity;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import acr.browser.lightning.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivity2Test {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void testPreconditions() {
        Assert.assertNotNull(mActivityRule.getActivity());
    }


    @Test
    public void testIsIncognito() {
        Assert.assertFalse(mActivityRule.getActivity().isIncognito());
    }

    @Test
    public void testContentDisplayed() {

    }

    @Test
    public void testTextViewDisplay() {
        onView(withId(R.id.toolbar)).perform(click()).check(ViewAssertions.matches(isDisplayed()));
    }
}