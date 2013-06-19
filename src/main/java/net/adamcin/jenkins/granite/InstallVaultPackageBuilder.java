package net.adamcin.jenkins.granite;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUser;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.plugins.asynchttpclient.AHC;
import net.adamcin.granite.client.packman.AbstractCrxPackageClient;
import net.adamcin.granite.client.packman.async.AsyncCrxPackageClient;
import net.adamcin.sshkey.clientauth.async.AsyncUtil;
import net.adamcin.sshkey.commons.Signer;
import net.adamcin.sshkey.commons.SignerException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 6/18/13
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstallVaultPackageBuilder extends Builder {

    private final String baseUrl;
    private final String username;
    private final String password;
    private final boolean loginUsingSshKey;

    @DataBoundConstructor
    public InstallVaultPackageBuilder(String baseUrl, String username, String password, boolean loginUsingSshKey) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.loginUsingSshKey = loginUsingSshKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLoginUsingSshKey() {
        return loginUsingSshKey;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        AsyncCrxPackageClient client = new AsyncCrxPackageClient(AHC.instance());
        client.setBaseUrl(baseUrl);
        // client.setBasicCredentials(username, password);
        Signer signer = null;
        try {
            signer = new Signer();
            List<SSHUserPrivateKey> keys = CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class);
            for (SSHUserPrivateKey key : keys) {
                byte[] passphrase = null;
                if (key.getPassphrase() != null) {
                    passphrase = key.getPassphrase().getEncryptedValue().getBytes("UTF-8");
                }
                signer.addLocalKey(key.getUsername(), key.getPrivateKey().getBytes("UTF-8"), passphrase);
            }

            if (this.isLoginUsingSshKey()) {
                Response response = AsyncUtil.login(getBaseUrl() + "/index.html",
                            signer, username, client.getClient(), true, 60000L);

                for (Cookie cookie : response.getCookies()) {
                    listener.error("cookie: %s, path: %s, value: %s", cookie.getName(), cookie.getPath(), cookie.getValue());
                }
                client.setCookies(response.getCookies());
            } else {
                client.setBasicCredentials(username, password);
            }

            client.waitForService(40000);
            return true;
        } catch (Exception e) {
            listener.error("Failed to waitForService: %s", e.getMessage());
            return false;
        } finally {
            if (signer != null) {
                try {
                    signer.clear();
                } catch (SignerException e) {
                    listener.error("Failed to cleanup SSHKey Signer: %s", e.getMessage());
                }
            }
        }
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Install Vault Package";
        }
    }
}
