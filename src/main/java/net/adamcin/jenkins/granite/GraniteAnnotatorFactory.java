package net.adamcin.jenkins.granite;

import hudson.Extension;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleAnnotatorFactory;
import hudson.model.Run;
import org.jvnet.hudson.plugins.collapsingconsolesections.CollapsingSectionAnnotator;
import org.jvnet.hudson.plugins.collapsingconsolesections.CollapsingSectionNote;
import org.jvnet.hudson.plugins.collapsingconsolesections.CollapsingSectionsConfiguration;
import org.jvnet.hudson.plugins.collapsingconsolesections.SectionDefinition;

@Extension(optional = true)
public class GraniteAnnotatorFactory extends ConsoleAnnotatorFactory<Class<Run>> {

    @Override
    public ConsoleAnnotator newInstance(Class<Run> context) {
        CollapsingSectionNote uninstallSection = new CollapsingSectionNote(
                "[Granite] Uninstalling package",
                "Uninstalling content",
                "Package uninstalled", false);
        CollapsingSectionNote installSection = new CollapsingSectionNote(
                "[Granite] Installing package",
                "Installing content",
                "Package uploaded", false);
        return new CollapsingSectionAnnotator(
                new CollapsingSectionsConfiguration(
                        new CollapsingSectionNote[]{uninstallSection, installSection}, false));
    }
}
