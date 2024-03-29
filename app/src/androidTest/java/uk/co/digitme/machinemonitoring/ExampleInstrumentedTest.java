package uk.co.digitme.machinemonitoring;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.runner.AndroidJUnit4;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4ClassRunner.class)
public class ExampleInstrumentedTest {
//    @Rule
//    public ActivityTestRule<MainActivity> mActivityRule =
//            new ActivityTestRule<>(MainActivity);
//
//    public ActivityTestRule<MainActivity> getmActivityRule() {
//        return mActivityRule;
//    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
//        Context appContext = getApplicationContext();
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("uk.samban.machinemonitoring", appContext.getPackageName());
    }
}
