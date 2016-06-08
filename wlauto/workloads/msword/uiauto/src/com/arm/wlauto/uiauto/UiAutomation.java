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

package com.arm.wlauto.uiauto.msword;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;

// Import the uiautomator libraries
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

    public static String TAG = "msword";

    public static final int WAIT_TIMEOUT_1MS = 1000;
    public static final int WAIT_TIMEOUT_5MS = 5000;
    public static final String CLASS_BUTTON = "android.widget.Button";
    public static final String CLASS_EDIT_TEXT = "android.widget.EditText";

    protected LinkedHashMap<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected boolean dumpsysEnabled;
    protected String outputDir;
    protected String packageName;
    protected String packageID;
    protected String documentName;
    protected String loginEmail;
    protected String loginPass;

    public void runUiAutomation() throws Exception {
        Bundle parameters = getParams();
        dumpsysEnabled = Boolean.parseBoolean(parameters.getString("dumpsys_enabled"));
        packageName = parameters.getString("package");
        outputDir = parameters.getString("output_dir");
        packageID = packageName + ":id/";
        documentName = parameters.getString("document_name");
        loginEmail = parameters.getString("login_email");
        loginPass = parameters.getString("login_pass");
        waitForProgress(WAIT_TIMEOUT_5MS * 2); // initial setup time
        if ("cloud".equalsIgnoreCase(parameters.getString("test_type"))) {
            testCloudDocument();
        } else {
            testLocalDocument();
        }
        writeResultsToFile(results, parameters.getString("output_file"));
    }

    public void testCloudDocument() throws Exception {
        // signIn();
    }

    public void testLocalDocument() throws Exception {
        clickUiObject(BY_TEXT, "Skip", true); // skip welcome screen
        openDocument(documentName);
        clickUiObject(BY_TEXT, "Got it", CLASS_BUTTON); // dismiss tooltip
    }

    protected void signIn() throws Exception {
        clickUiObject(BY_TEXT, "Sign in", true);
        UiObject emailField = new UiObject(new UiSelector().className(CLASS_EDIT_TEXT));
        emailField.clearTextField();
        emailField.setText(loginEmail + "@"); // deliberately incorrect
        clickUiObject(BY_DESC, "Next", CLASS_BUTTON, true);
        waitForProgress(WAIT_TIMEOUT_5MS);

        // When login email is not valid, app is redirected to another screen
        // to choose between Microsoft account or work-provided account
        // Full text says: We're having trouble locating your account.
        UiObject troublePage = new UiObject(new UiSelector().descriptionContains("trouble locating"));
        if (!troublePage.exists()) {
            throw new UiObjectNotFoundException("Not found: Login problem page");
        }
        clickUiObject(BY_DESC, "Work account", CLASS_BUTTON, true);
        waitForProgress(WAIT_TIMEOUT_5MS);
        // Here the views inside the WebView can now be accessed as normal
        UiObject webview = new UiObject(
            new UiSelector().className("android.webkit.WebView").descriptionContains("Sign in"));
        if (!webview.waitForExists(WAIT_TIMEOUT_5MS)) {
            throw new UiObjectNotFoundException("Not found: Sign-in WebView");
        }
        emailField = new UiObject(new UiSelector().className(CLASS_EDIT_TEXT).instance(0));
        int textLength = 30;
        emailField.clickBottomRight();
        while (textLength > 0) {
            getUiDevice().pressDelete();
            SystemClock.sleep(10);
        }
        getUiDevice().waitForIdle();
        emailField.click();
        emailField.setText(loginEmail);
        UiObject passwordField = new UiObject(new UiSelector().className(CLASS_EDIT_TEXT).instance(1));
        passwordField.setText(loginPass);
        clickUiObject(BY_DESC, "Sign in", CLASS_BUTTON, true);
    }

    public void openDocument(String document) throws Exception {
        clickUiObject(BY_TEXT, "Open", true);
        clickUiObject(BY_TEXT, "This device");
        clickUiObject(BY_TEXT, "Documents");
        clickUiObject(BY_TEXT, document, true);
    }

    private boolean waitForProgress(int timeout) throws Exception {
        UiObject progress = new UiObject(new UiSelector().className("android.widget.ProgressBar"));
        if (progress.exists()) {
            return progress.waitUntilGone(timeout);
        } else {
            return false;
        }
    }

}
