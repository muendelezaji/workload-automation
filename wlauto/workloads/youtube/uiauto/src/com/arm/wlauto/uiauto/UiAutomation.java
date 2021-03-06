/*    Copyright 2014-2016 ARM Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.wlauto.uiauto.youtube;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_ID;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_TEXT;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_DESC;

public class UiAutomation extends UxPerfUiAutomation {

    public static final String CLASS_BUTTON = "android.widget.Button";
    public static final String CLASS_FRAME_LAYOUT = "android.widget.FrameLayout";
    public static final String CLASS_TEXT_VIEW = "android.widget.TextView";
    public static final String CLASS_VIEW_GROUP = "android.view.ViewGroup";

    public static final int WAIT_TIMEOUT_1MS = 1000;
    public static final int WAIT_TIMEOUT_5MS = 5000;
    public static final int VIDEO_SLEEP_SECONDS = 3;
    public static final int LIST_SWIPE_COUNT = 5;
    public static final String SOURCE_MY_VIDEOS = "my_videos";
    public static final String SOURCE_SEARCH = "search";
    public static final String SOURCE_TRENDING = "trending";
    public static final String[] STREAM_QUALITY = {
        "Auto", "144p", "240p", "360p", "480p", "720p", "1080p"
    };

    protected LinkedHashMap<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected Bundle parameters;
    protected boolean dumpsysEnabled;
    protected String outputDir;
    protected String packageName;
    protected String packageID;
    protected String searchTerm;

    public void runUiAutomation() throws Exception {
        parameters = getParams();
        dumpsysEnabled = Boolean.parseBoolean(parameters.getString("dumpsys_enabled"));
        packageName = parameters.getString("package");
        outputDir = parameters.getString("output_dir");
        packageID = packageName + ":id/";
        searchTerm = parameters.getString("search_term");
        if (searchTerm != null) {
            searchTerm = searchTerm.replaceAll("_", " ");
        }

        setScreenOrientation(ScreenOrientation.NATURAL);

        clearFirstRunDialogues();
        disableAutoplay();
        testPlayVideo(parameters.getString("video_source"), searchTerm);
        writeResultsToFile(results, parameters.getString("output_file"));

        unsetScreenOrientation();
    }

    public void clearFirstRunDialogues() throws Exception {
        UiObject laterButton = new UiObject(new UiSelector().textContains("Later").className(CLASS_TEXT_VIEW));
        if (laterButton.waitForExists(WAIT_TIMEOUT_1MS)) {
           laterButton.click();
        }
        UiObject cancelButton = new UiObject(new UiSelector().textContains("Cancel").className(CLASS_BUTTON));
        if (cancelButton.waitForExists(WAIT_TIMEOUT_1MS)) {
            cancelButton.click();
        }
        UiObject skipButton = new UiObject(new UiSelector().textContains("Skip").className(CLASS_TEXT_VIEW));
        if (skipButton.waitForExists(WAIT_TIMEOUT_1MS)) {
            skipButton.click();
        }
        UiObject gotItButton = new UiObject(new UiSelector().textContains("Got it").className(CLASS_BUTTON));
        if (gotItButton.waitForExists(WAIT_TIMEOUT_1MS)) {
            gotItButton.click();
        }
    }

    public void disableAutoplay() throws Exception {
        clickUiObject(BY_DESC, "More options");
        startMeasurements();
        clickUiObject(BY_TEXT, "Settings", true);
        endMeasurements("goto_settings");
        startMeasurements();
        clickUiObject(BY_TEXT, "General", true);
        endMeasurements("goto_settings_general");
        clickUiObject(BY_TEXT, "Autoplay");
        getUiDevice().pressBack();
        // Tablet devices use a split with General in the left pane and Autoplay in the right so no
        // need to click back twice
        UiObject generalButton = new UiObject(new UiSelector().textContains("General").className(CLASS_TEXT_VIEW));
        if (generalButton.exists()) {
            getUiDevice().pressBack();
        }
    }

    public void testPlayVideo(String source, String searchTerm) throws Exception {
        if (SOURCE_MY_VIDEOS.equalsIgnoreCase(source)) {
            startMeasurements();
            clickUiObject(BY_DESC, "Account");
            endMeasurements("goto_account");
            startMeasurements();
            clickUiObject(BY_TEXT, "My Videos", true);
            endMeasurements("goto_my_videos");
            startMeasurements();
            clickUiObject(BY_ID, packageID + "thumbnail", true);
            endMeasurements("play_from_my_videos");
        } else if (SOURCE_SEARCH.equalsIgnoreCase(source)) {
            startMeasurements();
            clickUiObject(BY_DESC, "Search");
            endMeasurements("goto_search");
            startTimer();
            UiObject textField = getUiObjectByResourceId(packageID + "search_edit_text");
            textField.setText(searchTerm);
            endTimer("search_video");
            getUiDevice().pressEnter();
            startMeasurements();
            clickUiObject(BY_ID, packageID + "thumbnail", true);
            endMeasurements("play_from_search");
        } else if (SOURCE_TRENDING.equalsIgnoreCase(source)) {
            startMeasurements();
            clickUiObject(BY_DESC, "Trending");
            endMeasurements("goto_trending");
            startMeasurements();
            clickUiObject(BY_ID, packageID + "thumbnail", true);
            endMeasurements("play_from_trending");
        } else { // homepage videos
            UiScrollable list = new UiScrollable(new UiSelector().resourceId(packageID + "results"));
            if (list.exists()) {
                list.scrollForward();
            }
            startMeasurements();
            clickUiObject(BY_ID, packageID + "thumbnail", true);
            endMeasurements("play_from_home");
        }
        dismissAdvert();
        seekForward();
        checkVideoInfo();
        scrollRelated();
    }

    public void dismissAdvert() throws Exception {
        UiObject advert = new UiObject(new UiSelector().textContains("Visit advertiser"));
        if (advert.exists()) {
            UiObject skip = new UiObject(new UiSelector().textContains("Skip ad"));
            if (skip.waitForExists(WAIT_TIMEOUT_5MS)) {
                skip.click();
                sleep(VIDEO_SLEEP_SECONDS);
            }
        }
    }

    public void seekForward() throws Exception {
        UiObject player = getUiObjectByResourceId(packageID + "player_fragment_container", CLASS_FRAME_LAYOUT);
        repeatClickUiObject(player, 2, 100);
        startMeasurements();
        UiObject timebar = clickUiObject(BY_ID, packageID + "time_bar");
        endMeasurements("player_seekbar_touch");
        player.click();
        startDumpsys();
        sleep(VIDEO_SLEEP_SECONDS);
        endDumpsys("player_video_windowed");
    }

    public void checkVideoInfo() throws Exception {
        // Expand video info
        startTimer();
        clickUiObject(BY_ID, packageID + "expand_button");
        endTimer("info_card_expand");
        SystemClock.sleep(500); // short delay to simulate user action
        startTimer();
        clickUiObject(BY_ID, packageID + "expand_button");
        endTimer("info_card_collapse");
    }

    public void scrollRelated() throws Exception {
        // ListView of related videos and (maybe) comments
        UiScrollable list = new UiScrollable(new UiSelector().resourceId(packageID + "watch_list"));
        if (list.isScrollable()) {
            startMeasurements();
            list.flingToEnd(LIST_SWIPE_COUNT);
            endMeasurements("watch_list_fling_down");
            startMeasurements();
            list.flingToBeginning(LIST_SWIPE_COUNT);
            endMeasurements("watch_list_fling_up");
        }
        // After flinging, give the window enough time to settle down before
        // the next step, or else UiAutomator fails to find views in time
        sleep(VIDEO_SLEEP_SECONDS);
    }

    protected void startDumpsys() throws Exception {
        if (dumpsysEnabled) {
            initDumpsysSurfaceFlinger(packageName);
            initDumpsysGfxInfo(packageName);
        }
    }

    protected void endDumpsys(String testTag) throws Exception {
        if (dumpsysEnabled) {
            exitDumpsysSurfaceFlinger(packageName, new File(outputDir, testTag + "_surfFlinger.log"));
            exitDumpsysGfxInfo(packageName, new File(outputDir, testTag + "_gfxInfo.log"));
        }
    }

    protected void startTimer() {
        timer = new Timer();
        timer.start();
    }

    protected void endTimer(String testTag) {
        timer.end();
        results.put(testTag, timer);
    }

    protected void startMeasurements() throws Exception {
        startDumpsys();
        startTimer();
    }

    protected void endMeasurements(String testTag) throws Exception {
        endTimer(testTag);
        endDumpsys(testTag);
    }
}
