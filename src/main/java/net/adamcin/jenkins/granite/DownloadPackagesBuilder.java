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

package net.adamcin.jenkins.granite;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.adamcin.granite.client.packman.PackId;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class DownloadPackagesBuilder extends Builder {
    private String packageIds;
    private String baseUrl;
    private String username;
    private String password;
    private boolean signatureLogin;
    private String localDirectory;
    private boolean ignoreErrors;
    private long requestTimeout;
    private long serviceTimeout;

    @DataBoundConstructor
    public DownloadPackagesBuilder(String packageIds, String baseUrl, String username, String password,
                                   boolean signatureLogin, String localDirectory, boolean ignoreErrors,
                                   long requestTimeout,
                                   long serviceTimeout) {
        this.packageIds = packageIds;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.signatureLogin = signatureLogin;
        this.localDirectory = localDirectory;
        this.ignoreErrors = ignoreErrors;
        this.requestTimeout = requestTimeout;
        this.serviceTimeout = serviceTimeout;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Result result = build.getResult();
        if (result == null) {
            result = Result.SUCCESS;
        }

        GraniteClientConfig clientConfig = new GraniteClientConfig(
                getBaseUrl(build, listener),
                getUsername(build, listener),
                getPassword(build, listener),
                isSignatureLogin(),
                requestTimeout > 0L ? requestTimeout : -1L,
                serviceTimeout > 0L ? serviceTimeout : -1L
        );

        PackageDownloadCallable callable = new PackageDownloadCallable(clientConfig, listener,
                                                                       listPackIds(build, listener),
                                                                       ignoreErrors);

        final String fLocalDirectory = getLocalDirectory(build, listener);
        result = result.combine(build.getWorkspace().child(fLocalDirectory).act(callable));

        return result.isBetterOrEqualTo(Result.UNSTABLE);
    }

    public String getPackageIds() {
        if (this.packageIds != null) {
            return this.packageIds.trim();
        } else {
            return "";
        }
    }

    public String getPackageIds(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        try {
            return TokenMacro.expandAll(build, listener, getPackageIds());
        } catch (MacroEvaluationException e) {
            listener.error("Failed to expand macros in Package ID: %s", getPackageIds());
            return getPackageIds();
        }
    }

    private String getBaseUrl(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getBaseUrl());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getBaseUrl());
        }
        return getBaseUrl();
    }

    private String getUsername(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getUsername());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getUsername());
        }
        return getUsername();
    }

    private String getPassword(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getPassword());
        } catch (Exception e) {
            listener.error("failed to expand tokens in password.%n", getPassword());
        }
        return getPassword();
    }

    private String getLocalDirectory(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getLocalDirectory());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getLocalDirectory());
        }
        return getLocalDirectory();
    }

    public List<PackId> listPackIds(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        List<PackId> packIds = new ArrayList<PackId>();

        for (String packageId : getPackageIds(build, listener).split("\\r?\\n")) {
            PackId packId = PackId.parsePid(packageId);
            if (packId != null) {
                packIds.add(packId);
            }
        }

        return Collections.unmodifiableList(packIds);
    }

    public String getBaseUrl() {
        if (this.baseUrl != null) {
            return this.baseUrl.trim();
        } else {
            return "";
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSignatureLogin() {
        return signatureLogin;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public String getLocalDirectory() {
        if (localDirectory == null || localDirectory.trim().isEmpty()) {
            return ".";
        } else {
            return localDirectory;
        }
    }

    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    public void setPackageIds(String packageIds) {
        this.packageIds = packageIds;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSignatureLogin(boolean signatureLogin) {
        this.signatureLogin = signatureLogin;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public void setServiceTimeout(long serviceTimeout) {
        this.serviceTimeout = serviceTimeout;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "[Granite] Download Packages";
        }
    }

}
