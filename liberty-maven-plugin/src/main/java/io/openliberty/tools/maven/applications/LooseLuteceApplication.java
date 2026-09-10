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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Determines if the project is set up in an exploded format by checking for monitored directories.
     *
     * @param project the Maven project
     * @return true if the project is in exploded format, false otherwise
     */
    public static boolean isExploded(MavenProject project) {
    	  if (!getWebSourceDirectoriesToMonitor(project).isEmpty()) {
             return true;
         } else {
             return false;
         }
    }  
    /**
     * Checks if the current project instance is in exploded format.
     *
     * @return true if the project is in exploded format, false otherwise
     */
    public boolean isExploded() {
    	return isExploded(project);
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
    private static Path getLuteceSourceDirectory(MavenProject project) {
        Path baseDir = Paths.get(project.getBasedir().getAbsolutePath());
        String webappSourceDir = MavenProjectUtil.getPluginConfiguration(project, "fr.paris.lutece.tools", "lutece-maven-plugin", "webappSourceDirectory");
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
    		dom = project.getGoalConfiguration("fr.paris.lutece.tools", "lutece-maven-plugin", null, null);
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
     * Retrieves a list of paths to web source directories that have filtering enabled.
     *
     * @param project the Maven project
     * @return a list of filtered web resource paths
     */
    public static List<Path> getWebSourceDirectoriesToMonitor(MavenProject project) {

        Set<Path> filteredWebResources = getFilteredWebResourcesConfigurations(project);

        List<Path> retVal = new ArrayList<Path>(filteredWebResources);

        Path warSourceDir = getLuteceSourceDirectory(project);

        // Need to add warSourceDir if DD filtering enabled, unless it's already in the list having its own webResources config
        if (!filteredWebResources.contains(warSourceDir) && isFilteringDeploymentDescriptors(project)) {
            retVal.add(warSourceDir);
        }
        return retVal;
    }

    /**
     * Retrieves configurations for web resources with filtering enabled.
     *
     * @param project the Maven project
     * @return a set of paths to filtered web resources
     */
    private static Set<Path> getFilteredWebResourcesConfigurations(MavenProject project) {
        Set<Path> retVal = new HashSet<Path>();
        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());

        for (Xpp3Dom resource : getWebResourcesConfigurations(project)) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom filtering = resource.getChild("filtering");
            if (dir != null && filtering != null) {
                boolean filtered = Boolean.parseBoolean(filtering.getValue());
                if (filtered) {
                    retVal.add(baseDirPath.resolve(dir.getValue()));
                }
            }
        }

        return retVal;
    }
    
    /**
     * Checks if deployment descriptors are filtered.
     *
     * @param project the Maven project
     * @return true if deployment descriptors are filtered, false otherwise
     */
    private static boolean isFilteringDeploymentDescriptors(MavenProject project) {
        Boolean retVal = false;
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "lutece-maven-plugin", null, null);
        if (dom != null) {
            Xpp3Dom fdd = dom.getChild("filteringDeploymentDescriptors");
            if (fdd != null) {
                retVal = Boolean.parseBoolean(fdd.getValue());
            }
        }
        return retVal;
    }
    /**
     * Checks if deployment descriptors are filtered.
     *
     * @return true if deployment descriptors are filtered, false otherwise
     */
    public boolean isFilteringDeploymentDescriptors() {
        return isFilteringDeploymentDescriptors(project);
    }
    
    /**
     * Get resource configuration values that have "directory" children elements from the Maven WAR plugin
     * @param project the Maven project
     * @return a List of war plugin resource elements that contain a "directory" child element or empty list if none are found
     */
    public static List<Xpp3Dom> getWebResourcesConfigurations(MavenProject project) {
        List<Xpp3Dom> retVal = new ArrayList<Xpp3Dom>();
        Xpp3Dom dom = null;
        if(isLuteceApplication(project.getPackaging())) {
        	//If lutece project
        	dom = project.getGoalConfiguration("fr.paris.lutece.tools", "lutece-maven-plugin", null, null);
        }
        if (dom != null) {
            Xpp3Dom web = dom.getChild("webResources");
            if (web != null) {
                Xpp3Dom resources[] = web.getChildren("resource");
                if (resources != null) {
                    for (int i = 0; i < resources.length; i++) {
                        Xpp3Dom dir = resources[i].getChild("directory");
                        // put dir in List
                        if (dir != null) {
                            retVal.add(resources[i]);
                        }
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Adds all web resources configuration paths to the loose application configuration.
     *
     * @throws DOMException if there is an error manipulating XML configuration
     * @throws IOException  if there is an error accessing file paths
     */
    public void addAllWebResourcesConfigurationPaths() throws DOMException, IOException {
        Set<Path> handled = new HashSet<Path>();

        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());

        for (Xpp3Dom resource : getWebResourcesConfigurations(project)) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom target = resource.getChild("targetPath");
            Path resolvedDir = baseDirPath.resolve(dir.getValue());
            if (handled.contains(resolvedDir)) {
                log.warn("Ignoring webResources dir: " + dir.getValue() + ", already have entry for path: " + resolvedDir);
            } else {
                String targetPath = "/";
                if (target != null) {
                    targetPath = "/" + target.getValue();
                } 
                addOutputDir(getDocumentRoot(), resolvedDir.toFile(), targetPath);
                handled.add(resolvedDir);
            }
        }
    }

    /**
     * Adds non-filtered source and web resources paths to the loose application configuration.
     *
     * @throws DOMException if there is an error manipulating XML configuration
     * @throws IOException  if there is an error accessing file paths
     */
    public void addNonFilteredSourceAndWebResourcesPaths() throws DOMException, IOException {

        // Write the source dir first, out of tradition/precedence
        if (!isFilteringDeploymentDescriptors() && !getFilteredWebResourcesConfigurations(project).contains(warSourceDirectory)) {
            addSourceDir();
        }
        
        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());

        Set<Path> handled = new HashSet<Path>(); // Use to warn for duplicate entries
        for (Xpp3Dom resource : getWebResourcesConfigurations(project)) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom target = resource.getChild("targetPath");
            Xpp3Dom filtering = resource.getChild("filtering");
            Path resolvedDir = baseDirPath.resolve(dir.getValue());
            if (resolvedDir.equals(warSourceDirectory)) {
                // We have already decided to write the source dir or not above
                continue;
            }
            if (filtering != null && Boolean.parseBoolean(filtering.getValue())) {
                continue;
            } else {
                if (handled.contains(resolvedDir)) {
                    log.warn("Ignoring webResources dir: " + dir.getValue() + ", already have entry for path: " + resolvedDir);
                } else {
                    String targetPath = "/";
                     if (target != null) {
                         targetPath = "/" + target.getValue();
                     } 
                     addOutputDir(getDocumentRoot(), resolvedDir.toFile(), targetPath);
                     handled.add(resolvedDir);
                }
            }
        }
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
    	return packaging.equals("lutece-core") || packaging.equals("lutece-plugin") || packaging.equals("lutece-site");
    }
    
    /**
     * Adds the default configuration directory paths to the loose application configuration.
     *
     * @throws DOMException if there is an error manipulating XML configuration
     * @throws IOException  if there is an error accessing file paths
     */
    public void addDefaultConfigurationDirPaths() throws DOMException, IOException  {

        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());
        Xpp3Dom dom = project.getGoalConfiguration("fr.paris.lutece.tools", "lutece-maven-plugin", null, null);        
    	if (dom != null) {
    		Xpp3Dom localDir = dom.getChild("localConfDirectory");
            if ( localDir != null ) {
                    Path resolvedlocalDir = baseDirPath.resolve(localDir.getValue());
                    if(!resolvedlocalDir.toFile().exists()) {
                	    log.warn("Default local configuration directory " + localDir.getValue() + "does not exist");
                    }
                    else {
                    	addOutputDir(getDocumentRoot(), resolvedlocalDir.toFile(), "/WEB-INF/conf");
                }
             }
            Xpp3Dom defaultDir = dom.getChild("defaultConfDirectory");
            if ( defaultDir != null ) {
                Path resolvedDir = baseDirPath.resolve(defaultDir.getValue());
                if(!resolvedDir.toFile().exists()) {
            	    log.warn("Default configuration directory " + defaultDir.getValue() + "does not exist");
                }
                else {
                	addOutputDir(getDocumentRoot(), resolvedDir.toFile(), "/WEB-INF/conf/");
                }
            }          
        }
    }
}
