/*    Copyright 2013-2016 ARM Limited
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

package com.arm.wlauto.uiauto;

import android.os.Build;
import android.os.SystemClock;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

import com.arm.wlauto.uiauto.BaseUiAutomation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;

public class UxPerfUiAutomation extends BaseUiAutomation {

    private Logger logger = Logger.getLogger(UxPerfUiAutomation.class.getName());
    public long timeout = TimeUnit.SECONDS.toMillis(4);

    public enum Direction { UP, DOWN, LEFT, RIGHT, NULL };
    public enum GestureType { UIDEVICE_SWIPE, UIOBJECT_SWIPE, PINCH };
    public enum PinchType { IN, OUT, NULL };

    public class Timer {
        private long startTime = 0;
        private long endTime = 0;
        private long duration = 0;

        public void start(){
            this.startTime = System.currentTimeMillis();
        }

        public void end() {
            this.endTime   = System.currentTimeMillis();
            this.duration = this.endTime - this.startTime;
        }

        public long getStart() {
            return this.startTime;
        }

        public long getFinish() {
            return this.endTime;
        }

        public long getDuration() {
            return this.duration;
        }
    }

    public void initDumpsysSurfaceFlinger(String appPackage, String view) {
        String packageView = String.format(appPackage + "/" + view);
        List<String> command = Arrays.asList("dumpsys", "SurfaceFlinger", "--latency-clear"
                                                      , packageView);
        initDumpsys(command);
    }

    public void exitDumpsysSurfaceFlinger(String appPackage, String view, File  filename) {
      String packageView = String.format(appPackage + "/" + view);
        List<String> command = Arrays.asList("dumpsys", "SurfaceFlinger", "--latency", packageView);
        exitDumpsys(command,  filename);
    }

    public void initDumpsysGfxInfo(String appPackage) {
        List<String> command;
        if (Build.VERSION.SDK_INT >= 22) {
            command = Arrays.asList("dumpsys", "gfxinfo", appPackage, "framestats", "reset");
        } else {
            command = Arrays.asList("dumpsys", "gfxinfo", appPackage, "reset");
        }
        initDumpsys(command);
    }

    public void exitDumpsysGfxInfo(String appPackage, File  filename) {
        List<String> command;
        if (Build.VERSION.SDK_INT >= 22) {
            command = Arrays.asList("dumpsys", "gfxinfo", appPackage, "framestats");
        } else {
            command = Arrays.asList("dumpsys", "gfxinfo", appPackage);
        }
        exitDumpsys(command, filename);
    }

    public void initDumpsys(List<String> command) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            Process process = builder.start();
            process.waitFor();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to reset dumpsys", exception);
        }
    }

    public void exitDumpsys(List<String> command, File  filename) {
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            Process process = processBuilder.start();
            fileWriter = new FileWriter(filename);
            bufferedReader = new BufferedReader(
                             new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fileWriter.append(line);
                fileWriter.append(System.getProperty("line.separator"));
            }
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new Exception("Error while taking dumpsys, exitCode=" +
                    process.exitValue());
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to take a dumpsys", exception);
        } finally {
            if (fileWriter != null) {
                try { fileWriter.close(); } catch (Exception e) { e.printStackTrace(); }
            }
            if (bufferedReader != null) {
                try { bufferedReader.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    public Timer uiDeviceSwipeTest(Direction direction, int steps) throws Exception {
        Timer results = new Timer();
        results.start();
        switch (direction) {
            case UP:
                uiDeviceSwipeUp(steps);
                break;
            case DOWN:
                uiDeviceSwipeDown(steps);
                break;
            case LEFT:
                uiDeviceSwipeLeft(steps);
                break;
            case RIGHT:
                uiDeviceSwipeRight(steps);
                break;
            case NULL:
                throw new Exception("No direction specified");
            default:
                break;
        }
        results.end();
        return results;
    }

    public Timer uiObjectSwipeTest(UiObject view, Direction direction, int steps) throws Exception {
        Timer results = new Timer();
        results.start();
        switch (direction) {
            case UP:
                view.swipeUp(steps);
                break;
            case DOWN:
                view.swipeDown(steps);
                break;
            case LEFT:
                view.swipeLeft(steps);
                break;
            case RIGHT:
                view.swipeRight(steps);
                break;
            case NULL:
                throw new Exception("No direction specified");
            default:
                break;
        }
        results.end();
        return results;
    }

    public Timer uiObjectPinchTest(UiObject view, PinchType direction, int steps,
                                  int percent) throws Exception {
        Timer results = new Timer();
        results.start();
        if (direction.equals(PinchType.IN)) {
            view.pinchIn(percent, steps);
        } else if (direction.equals(PinchType.OUT)) {
            view.pinchOut(percent, steps);
        }
        results.end();
        return results;
    }
}
