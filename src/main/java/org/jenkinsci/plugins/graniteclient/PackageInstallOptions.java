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
