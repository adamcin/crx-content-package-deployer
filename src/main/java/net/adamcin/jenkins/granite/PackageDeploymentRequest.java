package net.adamcin.jenkins.granite;

import java.io.Serializable;

public interface PackageDeploymentRequest extends Serializable {

    String getBaseUrl();

    String getUsername();

    String getPassword();

    boolean isSshKeyLogin();

    ExistingPackageBehavior getExistingPackageBehavior();

    PackageInstallOptions getPackageInstallOptions();

}
