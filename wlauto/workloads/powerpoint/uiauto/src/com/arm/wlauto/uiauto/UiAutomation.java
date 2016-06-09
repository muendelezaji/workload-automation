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

package com.arm.wlauto.uiauto.powerpoint;

import android.os.Bundle;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiObjectNotFoundException;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;

public class UiAutomation extends UxPerfUiAutomation {

    public static String TAG = "uxperf_powerpoint";

    private Bundle parameters;
    private int viewTimeoutSecs = 10;
    private long viewTimeout =  TimeUnit.SECONDS.toMillis(viewTimeoutSecs);
    private LinkedHashMap<String, Timer> timingResults = new LinkedHashMap<String, Timer>();

    public void runUiAutomation() throws Exception {
        parameters = getParams();

        String templateName = parameters.getString("slide_template").replace("_", " ");
        String titleText = parameters.getString("title_name").replace("_", " ");

        setScreenOrientation(ScreenOrientation.NATURAL);
        confirmAccess();
        skipSignInView();

        newPresentation();
        createInTestFolder();
        selectTemplate(templateName);
        dismissToolTip();
        editTitle(titleText);
        clickNewSlide();
        setSlideLayout();
        addImage();

        // go back to first slide before presenting
        clickThumbNail(0);
        presentSlides();

        unsetScreenOrientation();
        writeResultsToFile(timingResults, parameters.getString("output_file"));
    }

    private void skipSignInView() throws Exception {
        UiObject skipSignIn = getUiObjectByText("Skip", "android.widget.TextView");
        skipSignIn.click();
    }

    private void createInTestFolder() throws Exception {
        UiObject docLocation =
            getUiObjectByText("This device > Documents", "android.widget.ToggleButton");
        docLocation.click();

        UiObject selectLocation =
            getUiObjectByText("Select a different location...", "android.widget.TextView");
        selectLocation.click();

        UiObject deviceLocation = getUiObjectByText("This device", "android.widget.TextView");
        deviceLocation.click();

        UiObject storageLocation =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.powerpoint:id/list_entry_title")
                                         .text("Storage"));
        storageLocation.click();

        UiScrollable scrollView =
            new UiScrollable(new UiSelector().className("android.widget.ScrollView"));

        UiObject folderName =
            new UiObject(new UiSelector().className("android.widget.TextView").text("wa-working"));

        while (!folderName.exists()) {
            scrollView.scrollForward();
        }

        folderName.click();

        UiObject selectButton = getUiObjectByText("Select", "android.widget.Button");
        selectButton.click();
    }

    private void newPresentation() throws Exception {
        UiObject newButton = getUiObjectByText("New", "android.widget.Button");
        newButton.click();
    }

    private void selectTemplate(final String template) throws Exception {
        String testTag = "select_slide_template";
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        UiScrollable gridView =
            new UiScrollable(new UiSelector().className("android.widget.GridView"));

        UiObject templateName =
            new UiObject(new UiSelector().className("android.widget.TextView").text(template));

        while (!templateName.exists()) {
            gridView.scrollForward();
        }

        logger.stop();
        templateName.click();

        UiObject view =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.powerpoint:id/slideContainerLayout"));

        if (!view.waitForExists(viewTimeout)) {
            throw new UiObjectNotFoundException("Could not find \"slide view\".");
        }

        timingResults.put(testTag, logger.result());
    }

    private void dismissToolTip() throws Exception {
        UiObject gotItButton = getUiObjectByText("Got it!", "android.widget.Button");
        gotItButton.click();
    }

    // Helper to select text boxes within slide view
    private void editTitle(final String title) throws Exception {
        UiObject titleBox =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.powerpoint:id/slideAirspaceEditView")
                                         .childSelector(new UiSelector().className("android.view.ViewGroup")));
        titleBox.click();
        titleBox.setText(title);

        // Click on the top left of the outer layout to exit the titlebox
        UiObject slideEditView =
            getUiObjectByResourceId("com.microsoft.office.powerpoint:id/slideEditView",
                                    "android.widget.LinearLayout");
        slideEditView.clickTopLeft();
    }

    private void clickThumbNail(final int index) throws Exception {
        UiObject slideThumbnail =
            new UiObject(new UiSelector()
                    .resourceId("com.microsoft.office.powerpoint:id/thumbnailList")
                    .childSelector(new UiSelector().className("android.widget.HorizontalScrollView")
                    .childSelector(new UiSelector().className("android.view.ViewGroup")
                    .childSelector(new UiSelector().index(index)
                    .focusable(true)))));

        slideThumbnail.click();
    }

    private void clickNewSlide() throws Exception {
        UiObject newSlideButton =
            getUiObjectByResourceId("com.microsoft.office.powerpoint:id/newSlideButton",
                                    "android.widget.Button");
        newSlideButton.click();
    }

    private void setSlideLayout() throws Exception {
        String testTag = "change_slide_layout";
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        UiObject layoutToggle = getUiObjectByDescription("Layout", "android.widget.ToggleButton");
        layoutToggle.click();

        // Use blank slides for simplicity
        UiObject layoutView =
            new UiObject(new UiSelector().className("android.widget.TextView").text("Blank"));

        UiScrollable scrollView =
            new UiScrollable(new UiSelector().className("android.widget.ScrollView"));

        while (!layoutView.exists()) {
            scrollView.scrollForward();
        }

        layoutView.click();

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void addImage() throws Exception {
        UiObject photoButton = getUiObjectByDescription("Photos", "android.widget.Button");
        photoButton.click();

        UiObject openFromImages = getUiObjectByText("Images", "android.widget.TextView");
        openFromImages.click();

        UiObject workloadFolder = getUiObjectByText("wa-working", "android.widget.TextView");
        workloadFolder.click();

        // Select the first image
        UiObject image =
            new UiObject(new UiSelector().className("android.widget.GridView")
                                         .childSelector(new UiSelector()
                                         .index(0)));
        image.click();
    }

    private void presentSlides() throws Exception {
        String testTag = "present_slides";

        UiObject presentButton = getUiObjectByDescription("Present", "android.widget.Button");
        presentButton.click();

        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);

        // Two swipes for each slide plus one to exit presentation
        for (int i = 0; i < 3; ++i) {
            sleep(3); // simulate user pauses between swipe actions
            logger.start();
            uiDeviceSwipe(Direction.LEFT, 20);
            logger.stop();
            timingResults.put(testTag + "_" + i, logger.result());
        }
    }
}
