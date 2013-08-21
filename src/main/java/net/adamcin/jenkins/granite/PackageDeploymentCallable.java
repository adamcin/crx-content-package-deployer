package net.adamcin.jenkins.granite;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.ning.http.client.AsyncHttpClient;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.granite.client.packman.async.AsyncPackageManagerClient;
import net.adamcin.sshkey.api.Signer;
import net.adamcin.sshkey.api.SignerException;
import net.adamcin.sshkey.api.SignerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class PackageDeploymentCallable implements FilePath.FileCallable<Boolean>, ResponseProgressListener {

    private final PackageDeploymentRequest request;
    private final AHCFactory ahcFactory;
    private final String baseUrl;
    private final PackId packId;
    private final TaskListener listener;
    private final PackageInstallOptions options;
    private final ExistingPackageBehavior behavior;
    private final long requestTimeout;
    private final long serviceTimeout;

    public PackageDeploymentCallable(PackageDeploymentRequest request, AHCFactory ahcFactory, String baseUrl, PackId packId, TaskListener listener, long requestTimeout, long serviceTimeout) {
        this.request = request;
        this.ahcFactory = ahcFactory;
        this.options = request.getPackageInstallOptions();
        this.behavior = request.getExistingPackageBehavior();
        this.baseUrl = baseUrl;
        this.packId = packId;
        this.listener = listener;
        this.requestTimeout = requestTimeout;
        this.serviceTimeout = serviceTimeout;
    }

    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

        AsyncHttpClient ahcClient = null;

        try {
            ahcClient = ahcFactory.newInstance();
            AsyncPackageManagerClient client = new AsyncPackageManagerClient(ahcClient);
            client.setBaseUrl(baseUrl);
            client.setRequestTimeout(requestTimeout);
            client.setServiceTimeout(serviceTimeout);

            listener.getLogger().printf("Deploying %s to %s%n", f,
                                        client.getConsoleUiUrl(packId));
            if (login(client)) {

                client.waitForService();
                if (client.existsOnServer(packId)) {
                    listener.getLogger().println("Found existing package.");
                    if (!this.handleExisting(client, packId)) {
                        return false;
                    }
                }

                client.waitForService();
                listener.getLogger().println("Will attempt to upload package.");

                SimpleResponse r_upload = client.upload(f, behavior == ExistingPackageBehavior.OVERWRITE, packId);
                if (r_upload.isSuccess()) {
                    this.onLog(r_upload.getMessage());
                    listener.getLogger().println("Will attempt to install package.");

                    DetailedResponse r_install = client.install(packId,
                                                                options.isRecursive(),
                                                                options.getAutosave(),
                                                                options.getAcHandling(),
                                                                this);
                    if (r_install.isSuccess()) {
                        this.onLog(r_upload.getMessage());
                    } else {
                        listener.fatalError("%s", r_install.getMessage());
                    }

                    return r_install.isSuccess();
                } else {
                    listener.fatalError(r_upload.getMessage());
                }
            } else {
                listener.fatalError("Failed to login to %s", baseUrl);
            }
        } catch (Exception e) {
            e.printStackTrace(listener.fatalError("Failed to deploy package: %s", e.getMessage()));
        } finally {
            if (ahcClient != null) {
                ahcClient.close();
            }
        }

        return false;
    }

    private boolean handleExisting(AsyncPackageManagerClient client, PackId packId) throws Exception {
        if (behavior == ExistingPackageBehavior.IGNORE || behavior == ExistingPackageBehavior.OVERWRITE) {
            listener.getLogger().println("Ignoring existing package...");
            return true;
        }

        if (this.behavior == ExistingPackageBehavior.UNINSTALL) {
            client.waitForService();
            ListResponse r_list = client.list(packId, false);
            if (!r_list.getResults().isEmpty() && r_list.getResults().get(0).isHasSnapshot()) {
                this.listener.getLogger().println("Will attempt to uninstall package.");
                DetailedResponse r_uninstall = client.uninstall(packId, this);
                if (r_uninstall.isSuccess()) {
                    this.onLog(r_uninstall.getMessage());
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
                this.onLog(r_delete.getMessage());
            } else {
                this.listener.fatalError("%s", r_delete.getMessage());
                return false;
            }
        }

        return true;
    }

    private boolean login(AsyncPackageManagerClient client) throws SignerException, IOException {
        if (request.isSshKeyLogin()) {
            Signer signer = SignerFactory.getFactoryInstance().getInstance();
            List<SSHUserPrivateKey> keys = CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class);
            for (SSHUserPrivateKey key : keys) {
                byte[] passphrase = null;
                if (key.getPassphrase() != null) {
                    passphrase = key.getPassphrase().getEncryptedValue().getBytes("UTF-8");
                }

                signer.addLocalKey(key.getUsername(), key.getPrivateKey().getBytes("UTF-8"), passphrase);
            }

            return client.login(request.getUsername(), signer);
        } else {
            return client.login(request.getUsername(), request.getPassword());
        }
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
