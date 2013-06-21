package net.adamcin.jenkins.granite;

import net.adamcin.granite.client.packman.ACHandling;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 6/20/13
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class PackageInstallOptions implements Serializable {

    private final boolean recursive;
    private final int autosave;
    private final ACHandling acHandling;

    public PackageInstallOptions(boolean recursive, int autosave, ACHandling acHandling) {
        this.recursive = recursive;
        this.autosave = autosave;
        this.acHandling = acHandling == null ? ACHandling.IGNORE : acHandling;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public int getAutosave() {
        return autosave;
    }

    public ACHandling getAcHandling() {
        return acHandling;
    }
}
