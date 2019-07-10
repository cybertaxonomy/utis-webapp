/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.logging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;

/**
 * @author a.kohlbecker
 * @since Jul 10, 2019
 *
 */
public class ContextDependentInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String contextPath = context.getContextPath();
        // in production the utis webapp will have a context path, therefore the default is names utis-dev
        System.setProperty("contextPath", contextPath.isEmpty() ? "utis-dev" : contextPath);

        if(System.getProperty("logFolder") == null){
            List<File> folderCandidates = new ArrayList<>();
            folderCandidates.add(new File("/var/log/utis"));
            if(System.getProperty("jetty.home") != null){
                folderCandidates.add(new File(System.getProperty("jetty.home"), "log"));
            }
            if(System.getProperty("cataline.home") != null){
                folderCandidates.add(new File(System.getProperty("cataline.home"), "log"));
            }
            folderCandidates.add(new File(FileUtils.getUserDirectory(), ".utis"));

            for(File f : folderCandidates){
                if(!f.exists()){
                    if(!f.mkdirs()){
                        continue;
                    } else {

                    }
                }
                if(f.canWrite()){
                    System.setProperty("logFolder", f.getPath());
                    break;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing to do here

    }
}