/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.harness.exec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.hub.Hub;
import net.neilcsmith.praxis.settings.Settings;
import org.netbeans.spi.sendopts.Env;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class Main {

    private final static Logger LOG = Logger.getLogger(Main.class.getName());
    private final static String CODE_NAME_BASE = "org.praxislive.harness.exec";
    private Env env;

    private Main(Env env) {
        this.env = env;
    }

    private void exec() {
        List<String> scripts = findScripts();
        if (scripts.isEmpty()) {
            env.getErrorStream().println("No projects found - exiting harness.");
            return;
        }

        initSettings();

        Hub hub = Hub.builder()
                .addExtension(new HarnessPlayer(scripts))
                .build();

        try {
            hub.start();
        } catch (Exception ex) {
            env.getErrorStream().println("Error starting hub.");
            env.getErrorStream().println(ex.toString());
        }
        try {
            hub.await();
        } catch (Exception ex) {
            env.getErrorStream().println("Error waiting for hub.");
            env.getErrorStream().println(ex.toString());
        }
    }

    private List<String> findScripts() {
        List<FileObject> pxps = findProjectFiles();
        if (pxps.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> scripts = new ArrayList<>(pxps.size());
        for (FileObject pxp : pxps) {
            LOG.log(Level.FINE, "Loading script : {0}", pxp.getPath());
            try {
                scripts.add(loadScript(pxp));
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error loading script : " + pxp.getPath(), ex);
            }
        }
        return scripts;
    }

    private List<FileObject> findProjectFiles() {

        File projects = InstalledFileLocator.getDefault().locate("../projects", CODE_NAME_BASE, false);
        if (projects == null || !projects.isDirectory()) {
            LOG.log(Level.FINE, "No projects directory found");
            return Collections.emptyList();
        }

        List<FileObject> files = new ArrayList<>(1);
        for (File project : projects.listFiles()) {
            if (project.isDirectory()) {
                FileObject pxp = findProjectFile(FileUtil.toFileObject(project));
                if (pxp != null) {
                    files.add(pxp);
                }
            }
        }

        return files;
    }

    private FileObject findProjectFile(FileObject projectDir) {
        LOG.log(Level.FINE, "Searching project directory : {0}", projectDir);
        ArrayList<FileObject> files = new ArrayList<>(1);
        for (FileObject file : projectDir.getChildren()) {
            if (file.hasExt("pxp")) {
                files.add(file);
            }
        }
        if (files.size() == 1) {
            FileObject file = files.get(0);
            LOG.log(Level.FINE, "Found project file : {0}", file.getPath());
            return file;
        } else {
            for (FileObject file : files) {
                LOG.log(Level.FINE, "Checking file : {0}", file.getPath());
                if (file.getName().equals(projectDir.getName())) {
                    LOG.log(Level.FINE, "Found project file : {0}", file.getPath());
                    return file;
                }
            }
        }
        return null;
    }

    private String loadScript(FileObject pxp) throws IOException {
        String script = pxp.asText();
        script = "set _PWD " + FileUtil.toFile(pxp.getParent()).toURI() + "\n" + script;
        return script;
    }

    private void initSettings() {
        File settings = InstalledFileLocator.getDefault().locate("config/praxis.properties", CODE_NAME_BASE, false);
        if (settings != null) {
            LOG.log(Level.FINE, "Found settings file, loading");
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(settings));
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Failed to load praxis.properties", ex);
            }
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (System.getProperty("praxis." + key) != null) {
                    LOG.log(Level.FINE, "Property {0} is set on command line - skipping!", key);
                    continue;
                }
                LOG.log(Level.FINE, "Setting property : {0} -- {1}", new Object[]{key, value});
                Settings.put(key, value, false);
            }
        }

    }

    static void exec(Env env) {
        Main main = new Main(env);
        main.exec();
    }
}
