package mil.nga.geopackage.map.test;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;

import mil.nga.geopackage.db.metadata.GeoPackageMetadataDb;
import mil.nga.geopackage.map.BuildConfig;
import mil.nga.geopackage.map.GeoPackageMapActivity;

/**
 * Abstract Base Test Case
 *
 * @author osbornb
 */
public abstract class BaseTestCase{

    @Rule
    public ActivityTestRule<GeoPackageMapActivity> rule  = new ActivityTestRule<GeoPackageMapActivity>(GeoPackageMapActivity.class);

    /**
     * Activity
     */
    protected Activity activity = null;

    /**
     * Test context
     */
    protected Context testContext = null;

    /**
     * Constructor
     */
    public BaseTestCase() {

    }

    @Before
    public void baseSetUp() throws Exception {

        // Set the activity and test context
        activity = rule.getActivity();
        testContext = TestUtils.getTestContext(activity);

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 11) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        activity.deleteDatabase(GeoPackageMetadataDb.DATABASE_NAME);
    }

}
