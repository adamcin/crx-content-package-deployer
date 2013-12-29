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

import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackIdFilter;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the "Deploy Content Packages to CRX" build step
 */
public class DeployPackagesBuilder extends Builder {

    private String packageIdFilters;
    private String baseUrls;
    private String credentialsId;
    private String localDirectory;
    private String behavior;
    private boolean recursive;
    private int autosave;
    private String acHandling;
    private boolean disableForJobTesting;
    private long requestTimeout;
    private long serviceTimeout;

    @DataBoundConstructor
    public DeployPackagesBuilder(String packageIdFilters, String baseUrls, String credentialsId,
                                 String localDirectory, String behavior, boolean recursive,
                                 int autosave, String acHandling, boolean disableForJobTesting, long requestTimeout,
                                 long serviceTimeout) {
        this.packageIdFilters = packageIdFilters;
        this.baseUrls = baseUrls;
        this.credentialsId = credentialsId;
        this.localDirectory = localDirectory;
        this.behavior = behavior;
        this.recursive = recursive;
        this.autosave = autosave;
        this.acHandling = acHandling;
        this.disableForJobTesting = disableForJobTesting;
        this.requestTimeout = requestTimeout;
        this.serviceTimeout = serviceTimeout;
    }

    public String getPackageIdFilters() {
        //return packageIdFilters;
        if (packageIdFilters != null) {
            return packageIdFilters.trim();
        } else {
            return "";
        }
    }

    public void setPackageIdFilters(String packageIdFilters) {
        this.packageIdFilters = packageIdFilters;
    }

    public String getBaseUrls() {
        //return baseUrls;
        if (baseUrls != null) {
            return baseUrls.trim();
        } else {
            return "";
        }
    }

    public void setBaseUrls(String baseUrls) {
        this.baseUrls = baseUrls;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getLocalDirectory() {
        if (localDirectory == null || localDirectory.trim().isEmpty()) {
            return ".";
        } else {
            return localDirectory.trim();
        }
    }

    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public int getAutosave() {
        return autosave;
    }

    public void setAutosave(int autosave) {
        this.autosave = autosave;
    }

    public String getAcHandling() {
        return acHandling;
    }

    public void setAcHandling(String acHandling) {
        this.acHandling = acHandling;
    }

    public boolean isDisableForJobTesting() {
        return disableForJobTesting;
    }

    public void setDisableForJobTesting(boolean disableForJobTesting) {
        this.disableForJobTesting = disableForJobTesting;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public void setServiceTimeout(long serviceTimeout) {
        this.serviceTimeout = serviceTimeout;
    }

    public PackageInstallOptions getPackageInstallOptions() {
        ACHandling _acHandling = ACHandling.IGNORE;
        if (getAcHandling() != null) {
            if ("merge".equalsIgnoreCase(getAcHandling())) {
                _acHandling = ACHandling.MERGE;
            } else if ("overwrite".equalsIgnoreCase(getAcHandling())) {
                _acHandling = ACHandling.OVERWRITE;
            } else if ("clear".equalsIgnoreCase(getAcHandling())) {
                _acHandling = ACHandling.CLEAR;
            }
        }

        return new PackageInstallOptions(isRecursive(), getAutosave(), _acHandling);
    }

    public ExistingPackageBehavior getExistingPackageBehavior() {
        ExistingPackageBehavior _behavior = ExistingPackageBehavior.IGNORE;
        if (getBehavior() != null) {
            if ("uninstall".equalsIgnoreCase(getBehavior())) {
                _behavior = ExistingPackageBehavior.UNINSTALL;
            } else if ("delete".equalsIgnoreCase(getBehavior())) {
                _behavior = ExistingPackageBehavior.DELETE;
            } else if ("overwrite".equalsIgnoreCase(getBehavior())) {
                _behavior = ExistingPackageBehavior.OVERWRITE;
            } else if ("skip".equalsIgnoreCase(getBehavior())) {
                _behavior = ExistingPackageBehavior.SKIP;
            }
        }
        return _behavior;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Result result = build.getResult();
        if (result == null) {
            result = Result.SUCCESS;
        }

        if (disableForJobTesting) {
            listener.getLogger().println("DEBUG: *** package deployment disabled for testing ***");
        }

        for (String baseUrl : listBaseUrls(build, listener)) {
            if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
                listener.getLogger().printf("Deploying packages to %s%n", baseUrl);
                for (Map.Entry<PackId, FilePath> selectedPackage : selectPackages(build, listener).entrySet()) {
                    if (!result.isBetterOrEqualTo(Result.UNSTABLE)) {
                        return false;
                    }
                    FilePath.FileCallable<Result> callable = null;
                    if (disableForJobTesting) {
                        callable = new DebugPackageCallable(selectedPackage.getKey(), listener);
                    } else {
                        GraniteClientConfig clientConfig =
                                new GraniteClientConfig(baseUrl, credentialsId, requestTimeout, serviceTimeout);

                        callable = new PackageDeploymentCallable(
                                clientConfig, listener,
                                selectedPackage.getKey(), getPackageInstallOptions(), getExistingPackageBehavior());
                    }

                    result = result.combine(selectedPackage.getValue().act(callable));
                    build.setResult(result);
                }
            }
        }

        return result.isBetterOrEqualTo(Result.UNSTABLE);
    }

    private Map<PackId, FilePath> selectPackages(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        Map<PackId, FilePath> found = new HashMap<PackId, FilePath>();

        final String fLocalDirectory = getLocalDirectory(build, listener);
        FilePath dir = build.getWorkspace().child(fLocalDirectory);

        try {
            List<FilePath> listed = new ArrayList<FilePath>();
            //listed.addAll(build.getWorkspace().list());
            listed.addAll(Arrays.asList(dir.list("**/*.jar")));
            listed.addAll(Arrays.asList(dir.list("**/*.zip")));

            Collections.sort(
                    listed, Collections.reverseOrder(
                    new Comparator<FilePath>() {
                        public int compare(FilePath left, FilePath right) {
                            try {
                                return Long.valueOf(left.lastModified()).compareTo(right.lastModified());
                            } catch (Exception e) {
                                listener.error("Failed to compare a couple files: %s", e.getMessage());
                            }
                            return 0;
                        }
                    }
            ));

            for (FilePath path : listed) {
                PackId packId = path.act(new FilePath.FileCallable<PackId>() {
                    public PackId invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                        return PackId.identifyPackage(f);
                    }
                });

                if (packId != null && !found.containsKey(packId)) {
                    found.put(packId, path);
                }
            }
        } catch (Exception e) {
            listener.error("Failed to list package files: %s", e.getMessage());
        }

        Map<PackId, FilePath> selected = new LinkedHashMap<PackId, FilePath>();
        for (Map.Entry<String, PackIdFilter> filterEntry : listPackageFilters(build, listener).entrySet()) {
            boolean matched = false;
            for (Map.Entry<PackId, FilePath> entry : found.entrySet()) {
                if (filterEntry.getValue().includes(entry.getKey())) {
                    matched = true;

                    if (!selected.containsKey(entry.getKey())) {
                        selected.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (!matched) {
                throw new IOException("No package found matching filter " + filterEntry.getKey());
            }
        }

        Map<String, List<PackId>> groupings = new HashMap<String, List<PackId>>();
        for (PackId packId : found.keySet()) {
            String groupKey = packId.getGroup() + ":" + packId.getName();
            if (!groupings.containsKey(groupKey)) {
                groupings.put(groupKey, new ArrayList<PackId>());
            }
            groupings.get(groupKey).add(packId);
        }

        Set<PackId> maxes = new HashSet<PackId>();
        for (List<PackId> grouping : groupings.values()) {
            Collections.sort(grouping, Collections.reverseOrder());
            maxes.add(grouping.get(0));
        }

        selected.keySet().retainAll(maxes);

        return Collections.unmodifiableMap(selected);
    }

    public String getPackageIdFilters(AbstractBuild<?, ?> build, TaskListener listener) throws Exception {
        return TokenMacro.expandAll(build, listener, getPackageIdFilters());
    }

    private Map<String, PackIdFilter> listPackageFilters(AbstractBuild<?, ?> build, TaskListener listener) {
        Map<String, PackIdFilter> filters = new LinkedHashMap<String, PackIdFilter>();
        try {
            for (String filter : getPackageIdFilters(build, listener).split("(\\r)?\\n")) {
                if (filter.trim().length() > 0) {
                    filters.put(filter, DefaultPackIdFilter.parse(filter));
                }
            }
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %n%s", getPackageIdFilters());
        }
        return Collections.unmodifiableMap(filters);
    }

    private List<String> listBaseUrls(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return parseBaseUrls(TokenMacro.expandAll(build, listener, getBaseUrls()));
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getBaseUrls());
        }
        return parseBaseUrls(getBaseUrls());
    }


    private List<String> listBaseUrls() {
        return parseBaseUrls(getBaseUrls());
    }

    private static List<String> parseBaseUrls(String value) {
         List<String> _baseUrls = new ArrayList<String>();
        for (String url : value.split("(\\r)?\\n")) {
            if (url.trim().length() > 0) {
                _baseUrls.add(url);
            }
        }
        return Collections.unmodifiableList(_baseUrls);
    }

    private String getLocalDirectory(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getLocalDirectory());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getLocalDirectory());
        }
        return getLocalDirectory();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy Content Packages to CRX";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json.getJSONObject("graniteDeployPackages"));
            save();
            return true;
        }

        public AbstractIdCredentialsListBoxModel doFillCredentialsIdItems(@QueryParameter String baseUrls) {
            List<String> _baseUrls = parseBaseUrls(baseUrls);

            if (_baseUrls != null && !_baseUrls.isEmpty()) {
                return GraniteCredentialsListBoxModel.fillItems(_baseUrls.iterator().next());
            } else {
                return GraniteCredentialsListBoxModel.fillItems();
            }
        }

        public FormValidation doCheckBaseUrls(@QueryParameter String value, @QueryParameter String credentialsId,
                                              @QueryParameter long requestTimeout, @QueryParameter long serviceTimeout) {
            for (String baseUrl : parseBaseUrls(value)) {
                try {
                    if (!GraniteClientExecutor.checkLogin(
                            new GraniteClientConfig(baseUrl, credentialsId, requestTimeout, serviceTimeout))) {
                        return FormValidation.error("Failed to login to " + baseUrl);
                    }
                } catch (IOException e) {
                    return FormValidation.error(e.getCause(), e.getMessage());
                }
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillAcHandlingItems() {
            return new ListBoxModel().add("Ignore").add("Merge").add("Clear").add("Overwrite");
        }

        public ListBoxModel doFillBehaviorItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("Uninstall and delete", "Uninstall");
            model.add("Delete", "Delete");
            model.add("Overwrite", "Overwrite");
            model.add("Skip", "Skip");
            model.add("Ignore", "Ignore");
            return model;
        }

    }

    static class DebugPackageCallable implements FilePath.FileCallable<Result> {
        final PackId packId;
        final BuildListener listener;

        DebugPackageCallable(PackId packId, BuildListener listener) {
            this.packId = packId;
            this.listener = listener;
        }

        public Result invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            listener.getLogger().printf("DEBUG: %s identified as %s.%n", f.getPath(), packId.toString());
            return Result.SUCCESS;
        }
    }

}
