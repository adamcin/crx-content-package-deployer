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

import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackageManagerClient;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;

import java.io.File;
import java.io.IOException;

public final class PackageDeploymentCallable extends AbstractClientFileCallable<Result> {

    private final PackId packId;
    private final PackageInstallOptions options;
    private final ExistingPackageBehavior behavior;
    private final ResponseProgressListener progressListener;

    public PackageDeploymentCallable(GraniteClientConfig clientConfig, TaskListener listener, PackId packId, PackageInstallOptions options, ExistingPackageBehavior behavior) {
        super(clientConfig, listener);
        this.progressListener = new JenkinsResponseProgressListener(this.listener);
        this.options = options;
        this.behavior = behavior;
        this.packId = packId;
    }

    private class Execution implements GraniteClientCallable<Result> {
        private final File file;

        private Execution(File file) {
            this.file = file;
        }

        public Result doExecute(PackageManagerClient client) throws Exception {
            listener.getLogger().printf("Deploying %s to %s%n", file, client.getConsoleUiUrl(packId));
            client.waitForService();
            if (client.existsOnServer(packId)) {
                listener.getLogger().println("Found existing package.");
                if (!PackageDeploymentCallable.this.handleExisting(client, packId)) {
                    return Result.FAILURE;
                } else if (behavior == ExistingPackageBehavior.SKIP) {
                    listener.getLogger().println("Will skip package upload and return SUCCESS.");
                    return Result.SUCCESS;
                }
            }

            client.waitForService();
            listener.getLogger().println("Will attempt to upload package.");

            SimpleResponse r_upload = client.upload(file, behavior == ExistingPackageBehavior.OVERWRITE, packId);
            if (r_upload.isSuccess()) {
                progressListener.onLog(r_upload.getMessage());
                listener.getLogger().println("Will attempt to install package.");

                DetailedResponse r_install = client.install(packId,
                                                            options.isRecursive(),
                                                            options.getAutosave(),
                                                            options.getAcHandling(),
                                                            progressListener);
                if (r_install.isSuccess()) {
                    progressListener.onLog(r_upload.getMessage());
                    if (r_install.hasErrors()) {
                        //listener.getLogger().println("should be unstable");
                        return Result.UNSTABLE;
                    } else {
                        return Result.SUCCESS;
                    }
                } else {
                    listener.fatalError("%s", r_install.getMessage());
                    return Result.FAILURE;
                }

            } else {
                listener.fatalError(r_upload.getMessage());
                return Result.FAILURE;
            }
        }
    }

    public Result invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            return GraniteClientExecutor.execute(new Execution(f), clientConfig, listener);
        } catch (Exception e) {
            e.printStackTrace(listener.fatalError("Failed to deploy package: %s", e.getMessage()));
        }

        return Result.FAILURE;
    }

    private boolean handleExisting(PackageManagerClient client, PackId packId) throws Exception {
        if (behavior == ExistingPackageBehavior.IGNORE
                || behavior == ExistingPackageBehavior.OVERWRITE
                || behavior == ExistingPackageBehavior.SKIP) {
            listener.getLogger().println("Ignoring existing package...");
            return true;
        }

        if (this.behavior == ExistingPackageBehavior.UNINSTALL) {
            client.waitForService();
            ListResponse r_list = client.list(packId, false);
            if (!r_list.getResults().isEmpty() && r_list.getResults().get(0).isHasSnapshot()) {
                this.listener.getLogger().println("Will attempt to uninstall package.");
                DetailedResponse r_uninstall = client.uninstall(packId, progressListener);
                if (r_uninstall.isSuccess()) {
                    progressListener.onLog(r_uninstall.getMessage());
                } else {
                    this.listener.fatalError("Failed to uninstall package: %s", r_uninstall.getMessage());
                    return false;
                }
            } else {
                this.listener.getLogger().println("Existing package has not been installed. Skipping uninstallation...");
            }
        }

        if (this.behavior == ExistingPackageBehavior.UNINSTALL || this.behavior == ExistingPackageBehavior.DELETE) {
            client.waitForService();
            this.listener.getLogger().println("Will attempt to delete package.");
            SimpleResponse r_delete = client.delete(packId);
            if (r_delete.isSuccess()) {
                progressListener.onLog(r_delete.getMessage());
            } else {
                this.listener.fatalError("%s", r_delete.getMessage());
                return false;
            }
        }

        return true;
    }
}
