/* OwlPlug
 * Copyright (C) 2021 Arthur <dropsnorz@gmail.com>
 *
 * This file is part of OwlPlug.
 *
 * OwlPlug is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OwlPlug is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OwlPlug.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.owlplug.project.tasks.discovery.flstudio;

import com.owlplug.core.utils.FileUtils;
import com.owlplug.plugin.model.PluginFormat;
import com.owlplug.project.model.DawApplication;
import com.owlplug.project.model.DawPlugin;
import com.owlplug.project.model.DawProject;
import com.owlplug.project.tasks.discovery.ProjectExplorer;
import com.owlplug.project.tasks.discovery.ProjectExplorerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Explorer for FL Studio project files (.flp).
 * Extracts project metadata and plugin references from binary FL Studio projects.
 */
public class FLStudioProjectExplorer implements ProjectExplorer {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public boolean canExploreFile(File file) {
    return file.isFile() && file.getAbsolutePath().toLowerCase().endsWith(".flp");
  }

  @Override
  public DawProject explore(File file) throws ProjectExplorerException {

    if (!canExploreFile(file)) {
      return null;
    }

    log.debug("Starting exploring FL Studio file {}", file.getAbsoluteFile());

    try {
      DawProject project = new DawProject();
      project.setApplication(DawApplication.FL_STUDIO);
      project.setPath(FileUtils.convertPath(file.getAbsolutePath()));
      project.setName(FilenameUtils.removeExtension(file.getName()));

      // Set file timestamps
      project.setLastModifiedAt(new Date(file.lastModified()));
      BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      FileTime fileTime = attr.creationTime();
      project.setCreatedAt(Date.from(fileTime.toInstant()));

      // Parse the binary FL Studio file
      FLStudioParser parser = new FLStudioParser();
      List<FLStudioParser.FLPlugin> flPlugins = parser.parseProject(file);

      // Convert version to human-readable format
      int version = parser.getProjectVersion();
      String versionString = formatFlStudioVersion(version);
      project.setAppFullName("FL Studio " + versionString);
      project.setFormatVersion(String.valueOf(version));

      // Convert FL plugins to DawPlugin objects
      for (FLStudioParser.FLPlugin flPlugin : flPlugins) {
        DawPlugin dawPlugin = new DawPlugin();
        dawPlugin.setProject(project);

        // Set plugin name
        if (flPlugin.getName() != null && !flPlugin.getName().isEmpty()) {
          dawPlugin.setName(flPlugin.getName());
        } else if (flPlugin.getPath() != null && !flPlugin.getPath().isEmpty()) {
          // Extract name from path if name is not available
          dawPlugin.setName(FilenameUtils.getBaseName(flPlugin.getPath()));
        } else {
          continue; // Skip plugins without name or path
        }

        // Set plugin path
        if (flPlugin.getPath() != null) {
          dawPlugin.setPath(flPlugin.getPath());
        }

        // Determine plugin format from file extension
        dawPlugin.setFormat(determinePluginFormat(flPlugin.getPath()));

        project.getPlugins().add(dawPlugin);
        log.debug("Added plugin: {} ({})", dawPlugin.getName(), dawPlugin.getFormat());
      }

      log.info("Explored FL Studio project: {} with {} plugins", 
               project.getName(), project.getPlugins().size());

      return project;

    } catch (IOException e) {
      throw new ProjectExplorerException("Error while reading FL Studio project file: " 
                                         + file.getAbsolutePath(), e);
    }
  }

  /**
   * Formats FL Studio version number to human-readable string.
   * Version format: Major * 100 + Minor * 10 + Patch
   * Examples: 
   *   2400 -> "24.0" (FL Studio 24)
   *   2411 -> "24.1.1"
   *   2100 -> "21.0"
   *   2008 -> "20.8"
   * 
   * This algorithm is future-proof and supports all FL Studio versions
   * from 12.x through 24.x and beyond (25+, 26+, etc.)
   */
  private String formatFlStudioVersion(int version) {
    if (version >= 1000) {
      int major = version / 100;
      int minor = (version % 100) / 10;
      int patch = version % 10;

      if (patch == 0 && minor == 0) {
        return String.valueOf(major);
      } else if (patch == 0) {
        return String.format("%d.%d", major, minor);
      } else {
        return String.format("%d.%d.%d", major, minor, patch);
      }
    }
    // Fallback for older or unknown version formats
    return String.valueOf(version);
  }

  /**
   * Determines plugin format based on file path extension.
   */
  private PluginFormat determinePluginFormat(String path) {
    if (path == null || path.isEmpty()) {
      return PluginFormat.VST2; // Default to VST2 if unknown
    }

    String lowerPath = path.toLowerCase();

    if (lowerPath.contains(".vst3") || lowerPath.endsWith(".vst3")) {
      return PluginFormat.VST3;
    } else if (lowerPath.contains(".component")) {
      return PluginFormat.AU;
    } else {
      // Default to VST2 for .dll, .vst, .so, or unknown
      return PluginFormat.VST2;
    }
  }
}
