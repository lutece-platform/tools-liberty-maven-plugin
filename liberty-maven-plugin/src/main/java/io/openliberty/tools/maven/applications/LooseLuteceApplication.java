/**
 * (C) Copyright IBM Corporation 2019, 2023.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven.applications;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.DOMException;

import io.openliberty.tools.maven.utils.MavenProjectUtil;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;

/**
 * Represents a loosely configured Lutece application, extending LooseApplication functionality 
 * for handling project-specific configurations in Lutece.
 */
public class LooseLuteceApplication extends LooseApplication {
    /** The groupId of the Lutece Maven plugin. */
    public static final String LUTECE_PLUGIN_GROUP_ID = "fr.paris.lutece.tools";
    /** The artifactId of the Lutece Maven plugin. */
    public static final String LUTECE_PLUGIN_ARTIFACT_ID = "lutece-maven-plugin";

    /** The Maven project instance associated with this application. */
	protected final MavenProject project;
    /** The source directory for the WAR file within the Lutece application. */
	protected final Path warSourceDirectory;
    /** The logger instance for this application. */
	protected final Log log;

	/**
     * Initializes a new LooseLuteceApplication with the specified project, configuration, and log.
     *
     * @param project the Maven project
     * @param config  the configuration data for this application
     * @param log     the logger instance
     */
	public LooseLuteceApplication(MavenProject project, LooseConfigData config, Log log) {
	    super(project.getBuild().getDirectory(), config);
	    this.project = project;
	    this.warSourceDirectory = getLuteceSourceDirectory(project);
	    this.log = log;
	}

    /**
     * Adds the main source directory to the configuration.
     *
     * @throws IOException if there is an error adding the directory
     */
    public void addSourceDir() throws IOException {
        config.addDir(warSourceDirectory.toFile(), "/");
    }
    /**
     * Retrieves the source directory path for the Lutece application.
     *
     * @param project the Maven project
     * @return the source directory path for the webapp sources
     */
    public static Path getLuteceSourceDirectory(MavenProject project) {
        Path baseDir = Paths.get(project.getBasedir().getAbsolutePath());
        String webappSourceDir = MavenProjectUtil.getPluginConfiguration(project, LUTECE_PLUGIN_GROUP_ID, LUTECE_PLUGIN_ARTIFACT_ID, "webappSourceDirectory");
        if (webappSourceDir == null) {
            // Not configured in the POM: match the lutece-maven-plugin default, ${basedir}/webapp
            webappSourceDir = "webapp";
        }
        // Use java.nio Paths to fix issue with absolute paths on Windows
        return baseDir.resolve(webappSourceDir);
    }
    
    /**
     * Gets the web application directory based on project packaging configurations.
     *
     * @param project the Maven project
     * @return the path to the web application directory
     */
    private Path getWebAppDirectory(MavenProject project) {
    	Xpp3Dom dom= null;
    	if(isLuteceApplication(project.getPackaging())) {
    		dom = project.getGoalConfiguration(LUTECE_PLUGIN_GROUP_ID, LUTECE_PLUGIN_ARTIFACT_ID, null, null);
    	}
    	String webAppDirStr = null;
        if (dom != null) {
            Xpp3Dom webAppDirConfig = dom.getChild("webappDirectory");
            if (webAppDirConfig != null) {
                webAppDirStr = webAppDirConfig.getValue();
            }
        }

        if (webAppDirStr != null) {
            return Paths.get(webAppDirStr);
        } else {
            // Match plugin default (we could get the default programmatically via webAppDirConfig.getAttribute("default-value") but don't
            return Paths.get(project.getBuild().getDirectory(), project.getBuild().getFinalName());
        }
    }

    /**
     * Returns the web source directories that dev mode must monitor in order to re-run the
     * exploded goal.
     *
     * <p>Always empty for Lutece projects: the lutece-maven-plugin performs no Maven resource
     * filtering, so the webapp sources can be mapped live in the loose application configuration
     * and there is nothing to regenerate on change.
     *
     * @param project the Maven project
     * @return an empty list
     */
    public static List<Path> getWebSourceDirectoriesToMonitor(MavenProject project) {
        return Collections.emptyList();
    }

    /**
     * Retrieves the web application directory path.
     *
     * @return the path to the web application directory
     */
    public Path getWebAppDirectory() {
    	return getWebAppDirectory(project);
    }
    
    /**
     * Checks if the specified packaging type represents a Lutece application.
     *
     * @param packaging the packaging type
     * @return true if it is a Lutece application, false otherwise
     */
    public static boolean isLuteceApplication( String packaging) {
    	return "lutece-core".equals(packaging) || "lutece-plugin".equals(packaging) || "lutece-site".equals(packaging);
    }
    
    /**
     * Adds the default configuration directory paths to the loose application configuration.
     *
     * @throws DOMException if there is an error manipulating XML configuration
     * @throws IOException  if there is an error accessing file paths
     */
    public void addDefaultConfigurationDirPaths() throws DOMException, IOException  {

        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());
        Xpp3Dom dom = project.getGoalConfiguration(LUTECE_PLUGIN_GROUP_ID, LUTECE_PLUGIN_ARTIFACT_ID, null, null);        
    	if (dom != null) {
    		Xpp3Dom localDir = dom.getChild("localConfDirectory");
            if ( localDir != null ) {
                    Path resolvedlocalDir = baseDirPath.resolve(localDir.getValue());
                    if(!resolvedlocalDir.toFile().exists()) {
                	    log.warn("Default local configuration directory " + localDir.getValue() + " does not exist");
                    }
                    else {
                    	addOutputDir(getDocumentRoot(), resolvedlocalDir.toFile(), "/WEB-INF/conf");
                }
             }
            Xpp3Dom defaultDir = dom.getChild("defaultConfDirectory");
            if ( defaultDir != null ) {
                Path resolvedDir = baseDirPath.resolve(defaultDir.getValue());
                if(!resolvedDir.toFile().exists()) {
            	    log.warn("Default configuration directory " + defaultDir.getValue() + " does not exist");
                }
                else {
                	addOutputDir(getDocumentRoot(), resolvedDir.toFile(), "/WEB-INF/conf/");
                }
            }          
        }
    }
}
