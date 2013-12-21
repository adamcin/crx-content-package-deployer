/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package org.jenkinsci.plugins.graniteclient;

import hudson.model.TaskListener;
import net.adamcin.granite.client.packman.ResponseProgressListener;

/**
 */
public class JenkinsResponseProgressListener implements ResponseProgressListener {

    final TaskListener listener;

    public JenkinsResponseProgressListener(TaskListener listener) {
        this.listener = listener;
    }

    public void onStart(String title) {
        listener.getLogger().printf("%s%n", title);
    }

    public void onLog(String message) {
        listener.getLogger().printf("%s%n", message);
    }

    public void onMessage(String message) {
        listener.getLogger().printf("M %s%n", message);
    }

    public void onProgress(String action, String path) {
        listener.getLogger().printf("%s %s%n", action, path);
    }

    public void onError(String path, String error) {
        listener.error("E %s (%s)", path, error);
    }

}
