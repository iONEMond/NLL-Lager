package de.nll.lager;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class AdminSettingsTest {
    private AdminSettings settings;
    @Before public void setup() {
        RuntimeEnvironment.getApplication().getSharedPreferences("admin-settings",0).edit().clear().commit();
        settings=new AdminSettings(RuntimeEnvironment.getApplication());
    }
    @Test public void passwordPersistsWithoutStoringPlaintextAndCanBeChanged() {
        assertFalse(settings.hasPassword());
        settings.setPassword("Test-password-42".toCharArray());
        AdminSettings reloaded=new AdminSettings(RuntimeEnvironment.getApplication());
        assertTrue(reloaded.verify("Test-password-42".toCharArray()));
        assertFalse(reloaded.verify("Wrong-password".toCharArray()));
        assertFalse(RuntimeEnvironment.getApplication().getSharedPreferences("admin-settings",0).getAll().containsValue("Test-password-42"));
        reloaded.setPassword("Another-password-43".toCharArray());
        assertFalse(reloaded.verify("Test-password-42".toCharArray()));
        assertTrue(reloaded.verify("Another-password-43".toCharArray()));
    }
    @Test public void shortPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,() -> settings.setPassword("short".toCharArray()));
        assertFalse(settings.hasPassword());
    }
    @Test public void fourDigitPinRetainsLeadingZeroAndCanReplaceExistingPassword() {
        settings.setPassword("Long-password".toCharArray());
        settings.setPassword("0042".toCharArray());
        assertTrue(new AdminSettings(RuntimeEnvironment.getApplication()).verify("0042".toCharArray()));
        assertFalse(settings.verify("42".toCharArray()));assertFalse(settings.verify("Long-password".toCharArray()));
        for(String invalid:Arrays.asList("123","12345","abcd","12a4","")) assertThrows(IllegalArgumentException.class,() -> settings.setPassword(invalid.toCharArray()));
        assertTrue(settings.verify("0042".toCharArray()));
    }
    @Test public void locationOrderPersistsAndInvalidEditsDoNotReplaceList() {
        settings.saveLocations(Arrays.asList("Leipzig","Fahrzeug 4"));
        assertEquals(Arrays.asList("Leipzig","Fahrzeug 4"),new AdminSettings(RuntimeEnvironment.getApplication()).locations());
        assertThrows(IllegalArgumentException.class,() -> settings.saveLocations(Arrays.asList("A","a")));
        assertThrows(IllegalArgumentException.class,() -> settings.saveLocations(Arrays.asList("")));
        assertEquals(Arrays.asList("Leipzig","Fahrzeug 4"),settings.locations());
    }
}
