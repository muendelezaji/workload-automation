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
import android.os.Bundle;

import android.util.Pair;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiSelector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UxPerfUiAutomation extends BaseUiAutomation {

    private Logger logger = Logger.getLogger(UxPerfUiAutomation.class.getName());

    public enum GestureType { UIDEVICE_SWIPE, UIOBJECT_SWIPE, PINCH };

    public class SurfaceLogger {

        private Bundle parameters;
        private String gfxInfologName;
        private String surfFlingerlogName;
        private Timer result;

        public SurfaceLogger(String testTag, Bundle parameters) {
            this.parameters = parameters;
            this.gfxInfologName = String.format(testTag + "_gfxInfo.log");
            this.surfFlingerlogName = String.format(testTag + "_surfFlinger.log");
            this.result = new Timer();
        }

        public void start() {
            startDumpsysGfxInfo(parameters);
            startDumpsysSurfaceFlinger(parameters);
            result.start();
        }

        public void stop() throws Exception {
            result.end();
            stopDumpsysSurfaceFlinger(parameters, surfFlingerlogName);
            stopDumpsysGfxInfo(parameters, gfxInfologName);
        }

        public Timer result() {
            return result;
        }
    }

    public static class Timer {
        private long startTime = 0;
        private long endTime = 0;
        private long duration = 0;

        public void start() {
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

    public String getSurfaceFlingerView(String appPackage) {
        BufferedReader bufferedReader = null;
        List<String> surfaceFlingerList = new ArrayList<String>();
        String packageView = "";
        try {
            List<String> command =
                Arrays.asList("dumpsys", "SurfaceFlinger", "--list");

            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            Process process = builder.start();
            process.waitFor();
            bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(appPackage)) {
                    surfaceFlingerList.add(line);
                }
            }

            if (surfaceFlingerList.size() != 0) {
                packageView = surfaceFlingerList.get(surfaceFlingerList.size() - 1);
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to list SurfaceFlinger views in dumpsys", exception);
        }

        return packageView;
    }

    public void initDumpsysSurfaceFlinger(String appPackage) {
        initDumpsysSurfaceFlinger(appPackage, getSurfaceFlingerView(appPackage));
    }

    public void initDumpsysSurfaceFlinger(String appPackage, String packageView) {
            List<String> command = Arrays.asList("dumpsys", "SurfaceFlinger", "--latency-clear",
                                                 packageView);
            executeCommand(command);
    }

    public void exitDumpsysSurfaceFlinger(String appPackage, File filename) {
        exitDumpsysSurfaceFlinger(appPackage, getSurfaceFlingerView(appPackage), filename);
    }

    public void exitDumpsysSurfaceFlinger(String appPackage, String packageView, File filename) {
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
        executeCommand(command);
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

    public Pair<Integer, String> executeCommand(List<String> command) {
        return executeCommand(command, false);
    }

    public Pair<Integer, String> executeCommand(List<String> command, boolean readOutput) {
        StringBuilder stringBuilder = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder();
        BufferedReader bufferedReader = null;
        int exitValue = -1;
        String output = "Unable to execute command\n" + Arrays.toString(command.toArray());

        try {
            processBuilder.command(command);
            Process process = processBuilder.start();
            exitValue = process.waitFor();

            if (readOutput) {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                String lineSeparator = System.getProperty("line.separator");
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(lineSeparator);
                }
            }

            output = stringBuilder.toString();

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to execute command", exception);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return Pair.create(exitValue, output);
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
                throw new Exception("Error while taking dumpsys, exitCode="
                        + process.exitValue());
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to take a dumpsys", exception);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class GestureTestParams {
        public GestureType gestureType;
        public Direction gestureDirection;
        public PinchType pinchType;
        public int percent;
        public int steps;

        public GestureTestParams(GestureType gesture, Direction direction, int steps) {
            this.gestureType = gesture;
            this.gestureDirection = direction;
            this.pinchType = PinchType.NULL;
            this.steps = steps;
            this.percent = 0;
        }

        public GestureTestParams(GestureType gesture, PinchType pinchType, int steps, int percent) {
            this.gestureType = gesture;
            this.gestureDirection = Direction.NULL;
            this.pinchType = pinchType;
            this.steps = steps;
            this.percent = percent;
        }
    }

    public void writeResultsToFile(Map<String, Timer> results, String file) throws Exception {
        // Write out the key/value pairs to the instrumentation log file
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        long start, finish, duration;
        Timer timer;
        for (Map.Entry<String, Timer> entry : results.entrySet()) {
            timer = entry.getValue();
            start = timer.getStart();
            finish = timer.getFinish();
            duration = timer.getDuration();
            // Format used to parse out results in workload's update_result function
            out.write(String.format("%s %d %d %d\n", entry.getKey(), start, finish, duration));
        }
        out.close();
    }

    public void confirmAccess() throws Exception {
        // First time run requires confirmation to allow access to local files
        UiObject allowButton = new UiObject(new UiSelector().textContains("Allow")
                .className("android.widget.Button"));

        if (allowButton.exists()) {
            // Some devices request multiple permisson rights so clear them all here
            do {
                allowButton.clickAndWaitForNewWindow(uiAutoTimeout);
            } while (allowButton.waitForExists(TimeUnit.SECONDS.toMillis(1)));
        }
    }

    public void startDumpsysSurfaceFlinger(Bundle parameters) {
        if (Boolean.parseBoolean(parameters.getString("dumpsys_enabled"))) {
            initDumpsysSurfaceFlinger(parameters.getString("package"));
        }
    }

    public void stopDumpsysSurfaceFlinger(Bundle parameters, String filename) throws Exception {
        if (Boolean.parseBoolean(parameters.getString("dumpsys_enabled"))) {
            File outFile = new File(parameters.getString("output_dir"), filename);
            exitDumpsysSurfaceFlinger(parameters.getString("package"), outFile);
        }
    }

    public void startDumpsysGfxInfo(Bundle parameters) {
        if (Boolean.parseBoolean(parameters.getString("dumpsys_enabled"))) {
            initDumpsysGfxInfo(parameters.getString("package"));
        }
    }

    public void stopDumpsysGfxInfo(Bundle parameters, String filename) throws Exception {
        if (Boolean.parseBoolean(parameters.getString("dumpsys_enabled"))) {
            File outFile = new File(parameters.getString("output_dir"), filename);
            exitDumpsysGfxInfo(parameters.getString("package"), outFile);
        }
    }
}
