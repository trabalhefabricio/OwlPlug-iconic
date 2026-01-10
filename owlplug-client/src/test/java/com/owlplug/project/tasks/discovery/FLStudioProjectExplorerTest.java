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

package com.owlplug.project.tasks.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.owlplug.plugin.model.PluginFormat;
import com.owlplug.project.model.DawApplication;
import com.owlplug.project.model.DawProject;
import com.owlplug.project.tasks.discovery.ProjectExplorerException;
import com.owlplug.project.tasks.discovery.flstudio.FLStudioProjectExplorer;
import java.io.File;
import org.junit.jupiter.api.Test;

public class FLStudioProjectExplorerTest {

  @Test
  public void flStudio20ValidProject() throws ProjectExplorerException {
    FLStudioProjectExplorer explorer = new FLStudioProjectExplorer();

    File file = new File(this.getClass().getClassLoader()
            .getResource("projects/flstudio/test_project.flp").getFile());

    DawProject project = explorer.explore(file);
    
    // Verify project metadata
    assertNotNull(project, "Project should not be null");
    assertEquals("test_project", project.getName());
    assertEquals(DawApplication.FL_STUDIO, project.getApplication());
    assertEquals("FL Studio 20.8", project.getAppFullName());
    assertEquals("2008", project.getFormatVersion());
    assertNotNull(project.getCreatedAt());
    assertNotNull(project.getLastModifiedAt());
    
    // Verify plugins were extracted
    assertTrue(project.getPlugins().size() >= 2, 
               "Project should contain at least 2 plugins, found: " + project.getPlugins().size());

    // Verify plugin details
    boolean foundVst2 = false;
    boolean foundVst3 = false;
    
    for (var plugin : project.getPlugins()) {
      if (plugin.getName() != null) {
        if (plugin.getName().contains("TestSynth")) {
          foundVst2 = true;
          assertEquals(PluginFormat.VST2, plugin.getFormat());
        } else if (plugin.getName().contains("SuperDelay") || plugin.getName().contains("Reverb")) {
          if (plugin.getFormat() == PluginFormat.VST3) {
            foundVst3 = true;
          }
        }
      }
    }
    
    assertTrue(foundVst2 || foundVst3, "Should find at least one VST plugin");
  }

  @Test
  public void flStudio24ValidProject() throws ProjectExplorerException {
    FLStudioProjectExplorer explorer = new FLStudioProjectExplorer();

    File file = new File(this.getClass().getClassLoader()
            .getResource("projects/flstudio/test_project_fl24.flp").getFile());

    DawProject project = explorer.explore(file);
    
    // Verify project metadata for FL Studio 24
    assertNotNull(project, "Project should not be null");
    assertEquals("test_project_fl24", project.getName());
    assertEquals(DawApplication.FL_STUDIO, project.getApplication());
    assertEquals("FL Studio 24.1", project.getAppFullName());
    assertEquals("2410", project.getFormatVersion());
    
    // Verify at least one plugin was found
    assertTrue(project.getPlugins().size() >= 1, 
               "FL Studio 24 project should contain at least 1 plugin");
  }

  @Test
  public void canExploreFlpFile() {
    FLStudioProjectExplorer explorer = new FLStudioProjectExplorer();
    
    File flpFile = new File("/path/to/project.flp");
    assertTrue(explorer.canExploreFile(flpFile));
    
    File FlpUpperCase = new File("/path/to/project.FLP");
    assertTrue(explorer.canExploreFile(FlpUpperCase));
    
    File nonFlpFile = new File("/path/to/project.als");
    assertTrue(!explorer.canExploreFile(nonFlpFile));
  }
}
