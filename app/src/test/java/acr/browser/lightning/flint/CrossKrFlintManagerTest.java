package acr.browser.lightning.flint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class CrossKrFlintManagerTest {
    CrossKrFlintManager mCrossKrFlintManager;

    @Mock
    Context mMockContext;

    @Before
    public void setUp() {
        mCrossKrFlintManager = new CrossKrFlintManager(mMockContext);
    }

    @Test
    public void testCanControlVolume() {
        Assert.assertEquals(mCrossKrFlintManager.canControlVolume(), false);
    }
}
