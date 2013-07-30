package net.adamcin.jenkins.granite;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.asynchttpclient.AHCUtils;
import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackIdFilter;
import net.sf.json.JSONObject;
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

public class DeployPackagesBuilder extends Builder implements PackageDeploymentRequest {

    private String packageIdFilters;
    private String baseUrls;
    private String username;
    private String password;
    private boolean sshKeyLogin;
    private String behavior;
    private boolean recursive;
    private int autosave;
    private String acHandling;
    private boolean disableForJobTesting;
    private long requestTimeout;
    private long serviceTimeout;

    @DataBoundConstructor
    public DeployPackagesBuilder(String packageIdFilters, String baseUrls, String username, String password,
                                 boolean sshKeyLogin, String behavior, boolean recursive, int autosave,
                                 String acHandling, boolean disableForJobTesting, long requestTimeout,
                                 long serviceTimeout) {
        this.packageIdFilters = packageIdFilters;
        this.baseUrls = baseUrls;
        this.username = username;
        this.password = password;
        this.sshKeyLogin = sshKeyLogin;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSshKeyLogin() {
        return sshKeyLogin;
    }

    public void setSshKeyLogin(boolean sshKeyLogin) {
        this.sshKeyLogin = sshKeyLogin;
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
            }
        }
        return _behavior;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        boolean success = true;

        if (disableForJobTesting) {
            listener.getLogger().println("DEBUG: *** package deployment disabled for testing ***");
        }

        for (String baseUrl : listBaseUrls()) {
            listener.getLogger().printf("Deploying packages to %s%n", baseUrl);
            for (Map.Entry<PackId, FilePath> selectedPackage : selectPackages(build, listener).entrySet()) {
                FilePath.FileCallable<Boolean> callable = null;
                if (disableForJobTesting) {
                    callable = new DebugPackageCallable(selectedPackage.getKey(), listener);
                } else {
                    callable = new PackageDeploymentCallable(this, getDescriptor(), baseUrl,
                                                             selectedPackage.getKey(), listener,
                                                             requestTimeout > 0L ? requestTimeout : -1L,
                                                             serviceTimeout > 0L ? serviceTimeout : -1L);
                }
                success = success && selectedPackage.getValue().act(callable);
            }
        }
        return success;
    }

    private Map<PackId, FilePath> selectPackages(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        Map<PackId, FilePath> found = new HashMap<PackId, FilePath>();

        try {
            List<FilePath> listed = new ArrayList<FilePath>();
            //listed.addAll(build.getWorkspace().list());
            listed.addAll(Arrays.asList(build.getWorkspace().list("**/*.jar")));
            listed.addAll(Arrays.asList(build.getWorkspace().list("**/*.zip")));

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
        for (Map.Entry<String, PackIdFilter> filterEntry : listPackageFilters().entrySet()) {
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

    private Map<String, PackIdFilter> listPackageFilters() {
        Map<String, PackIdFilter> filters = new LinkedHashMap<String, PackIdFilter>();
        for (String filter : getPackageIdFilters().split("(\\r)?\\n")) {
            if (filter.trim().length() > 0) {
                filters.put(filter, DefaultPackIdFilter.parse(filter));
            }
        }
        return Collections.unmodifiableMap(filters);
    }

    private List<String> listBaseUrls() {
        List<String> _baseUrls = new ArrayList<String>();
        for (String url : getBaseUrls().split("(\\r)?\\n")) {
            if (url.trim().length() > 0) {
                _baseUrls.add(url);
            }
        }
        return Collections.unmodifiableList(_baseUrls);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements AHCFactory {

        private static final AsyncHttpClientConfig DEFAULT_CONFIG = new AsyncHttpClientConfig.Builder().build();

        private int idleConnectionTimeoutInMs = DEFAULT_CONFIG.getIdleConnectionTimeoutInMs();

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "[Granite] Deploy Packages";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json.getJSONObject("graniteDeployPackages"));
            save();
            return true;
        }

        public int getIdleConnectionTimeoutInMs() {
            return idleConnectionTimeoutInMs;
        }

        public void setIdleConnectionTimeoutInMs(int idleConnectionTimeoutInMs) {
            this.idleConnectionTimeoutInMs = idleConnectionTimeoutInMs;
        }

        public FormValidation doCheckBaseUrls(@QueryParameter String value) {
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
            model.add("Ignore", "Ignore");
            return model;
        }

        public AsyncHttpClient newInstance() {
            return new AsyncHttpClient(
                    new AsyncHttpClientConfig.Builder()
                            .setProxyServer(AHCUtils.getProxyServer())
                            .setIdleConnectionTimeoutInMs(this.idleConnectionTimeoutInMs)
                            .build());
        }
    }

    static class DebugPackageCallable implements FilePath.FileCallable<Boolean> {
        final PackId packId;
        final BuildListener listener;

        DebugPackageCallable(PackId packId, BuildListener listener) {
            this.packId = packId;
            this.listener = listener;
        }

        public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            listener.getLogger().printf("DEBUG: %s identified as %s.%n", f.getPath(), packId.toString());
            return true;
        }
    }

}
