package com.arm.wlauto.uiauto.youtube;

import android.os.Bundle;
import android.os.SystemClock;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import android.util.Log;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UiAutomation extends UxPerfUiAutomation {

    public static final String TAG = "youtube";
    public static final int WAIT_FOR_EXISTS_TIMEOUT = 1000;

    protected long networkTimeout =  TimeUnit.SECONDS.toMillis(20);
    protected String[] streamQuality = {
        "Auto", "144p", "240p", "360p", "480p", "720p", "1080p"
    };

    protected LinkedHashMap<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Bundle parameters;
    protected boolean dumpsysEnabled;
    protected String outputDir;
    protected String packageID;

    public void runUiAutomation() throws Exception {
        parameters = getParams();
        packageID = parameters.getString("package") + ":id/";
        clearFirstRunDialogues();
        selectFirstVideo();
        seekForward();
        // changeQuality(streamQuality[1]);
        makeFullscreen();
        if (false) {
            writeResultsToFile(results, parameters.getString("output_file"));
        }
    }

    public void clearFirstRunDialogues() throws Exception {
        UiObject laterButton = getUiObjectByResourceId(packageID + "later_button",
                                                       "android.widget.TextView");
        if (laterButton.waitForExists(WAIT_FOR_EXISTS_TIMEOUT)) {
           laterButton.click();
        }
        UiObject skipButton = getUiObjectByText("Skip", "android.widget.TextView");
        if (skipButton.waitForExists(WAIT_FOR_EXISTS_TIMEOUT)) {
            skipButton.click();
        }
        UiObject gotItButton = getUiObjectByText("Got it", "android.widget.Button");
        if (gotItButton.waitForExists(WAIT_FOR_EXISTS_TIMEOUT)) {
            gotItButton.click();
        }
    }

    public void selectFirstVideo() throws Exception {
        UiObject navigateUpButton = getUiObjectByDescription("Navigate up", "android.widget.ImageButton");
        UiObject myAccount = getUiObjectByDescription("Account", "android.widget.Button");
        if (navigateUpButton.exists()) {
            navigateUpButton.click();
            UiObject uploads = getUiObjectByText("Uploads", "android.widget.TextView");
            waitObject(uploads, 4);
            uploads.click();
            UiObject firstEntry = new UiObject(new UiSelector().resourceId(packageID + "paged_list")
                                                                .className("android.widget.ListView")
                                                                .childSelector(new UiSelector()
                                                                .index(0).className("android.widget.LinearLayout")));
            waitObject(firstEntry, 4);
            firstEntry.click();
        } else {
            waitObject(myAccount, 4);
            myAccount.click();
            UiObject myVideos = getUiObjectByText("My videos", "android.widget.TextView");
            waitObject(myVideos, 4);
            myVideos.click();
            UiObject firstEntry = getUiObjectByResourceId(packageID + "compact_video_item", "android.widget.LinearLayout");
            waitObject(firstEntry, 4);
            firstEntry.click();
        }
        sleep(4);
    }

    public void makeFullscreen() throws Exception {
        UiObject fullscreenButton = getUiObjectByResourceId(packageID + "fullscreen_button",
                                                            "android.widget.ImageView");
        UiObject viewGroup =  getUiObjectByResourceId(packageID + "player_fragment", "android.widget.FrameLayout");
        viewGroup.click();
        waitObject(fullscreenButton, 4);
        fullscreenButton.click();
        sleep(4);
    }

    public void seekForward() throws Exception {
        UiObject timebar = getUiObjectByResourceId(packageID + "time_bar", "android.view.View");
        UiObject viewGroup =  getUiObjectByResourceId(packageID + "player_fragment", "android.widget.FrameLayout");
        viewGroup.click();
        waitObject(timebar, 4);
        timebar.click();
        sleep(4);
        // timebar.swipeRight(20);
        // sleep(2);
    }

    public void changeQuality(String quality) throws Exception {
        UiObject viewGroup =  getUiObjectByResourceId(packageID + "player_fragment", "android.widget.FrameLayout");
        viewGroup.click();
        UiObject moreOptions =  getUiObjectByResourceId(packageID + "player_overflow_button", "android.widget.ImageView");
        UiObject miniPlayerViewGroup =  getUiObjectByResourceId(packageID + "watch_player", "android.view.ViewGroup");
        UiObject miniPlayerViewLayout =  getUiObjectByResourceId(packageID + "watch_player", "android.widget.FrameLayout");

        // UiObject qualityButton =  getUiObjectByResourceId(packageID + "quality_button_text", "android.widget.TextView");

        // UiObject qualityButton =  new UiObject(new UiSelector().resourceId(packageID + "watch_player")
        //                                              .className("android.view.ViewGroup")
        //                                                 .childSelector(new UiSelector()
        //                                                 .index(1).className("android.widget.FrameLayout")
        //                                                 .childSelector(new UiSelector()
        //                                                 .index(0).className("android.widget.FrameLayout")
        //                                                 .childSelector(new UiSelector()
        //                                                 .index(0).className("android.widget.RelativeLayout")
        //                                                 .childSelector(new UiSelector()
        //                                                 .index(1).className("android.widget.RelativeLayout")
        //                                                 .childSelector(new UiSelector()
        //                                                 .index(1).className("android.widget.ImageView")))))));

        UiObject qualityButton =  new UiObject(new UiSelector().descriptionContains("Show video quality menu"));
        UiObject qualitySetting =  getUiObjectByResourceId(quality, "android.widget.CheckedTextView");
        Log.v(TAG, String.format("MADE IT HERE"));
        waitObject(moreOptions, 4);
        moreOptions.click();

        if (miniPlayerViewGroup.exists()) {
            // MATE 8
            // miniPlayerViewGroup.click();
            UiObject frameLayout =  miniPlayerViewGroup.getChild(new UiSelector()
                                                            .index(1).className("android.widget.FrameLayout")
                                                            .childSelector(new UiSelector()));
        } else {
            // ZENFONE
            if (qualityButton.exists()) {
                qualityButton.click();
            }
            UiObject frameLayout =  miniPlayerViewLayout.getChild(new UiSelector()
                                                            .index(1).className("android.widget.FrameLayout")
                                                            .childSelector(new UiSelector()));
            int count = frameLayout.getChildCount();
            Log.v(TAG, String.format("ChildCount: %s", count));
            for (int i = 0; i < count ; i++) {
                String className = frameLayout.getChild(new UiSelector().index(i)).getClassName();
                String description = frameLayout.getChild(new UiSelector().index(i)).getContentDescription();
                Log.v(TAG, String.format("Child %s ClassName: %s %s", i, className, description));
            }
            throw new UiObjectNotFoundException(String.format("child count: %s", count));
        }
        // waitObject(qualityButton, 4);
        // qualityButton.click();
        // waitObject(qualitySetting, 4);
        // qualitySetting.click();
        // seekForward();
    }

}
