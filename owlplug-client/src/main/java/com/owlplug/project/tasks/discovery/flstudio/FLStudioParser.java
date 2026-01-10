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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for FL Studio project files (.flp).
 * Reads binary format to extract plugin information.
 */
public class FLStudioParser {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  // FL Studio event IDs for plugin information
  private static final int EVENT_PLUGIN_NAME = 0xC9;     // 201
  private static final int EVENT_PLUGIN_PATH = 0xCA;     // 202
  private static final int EVENT_PLUGIN_VENDOR = 0xCB;   // 203
  private static final int EVENT_VST_PLUGIN_INFO = 0x35; // 53
  private static final int EVENT_PLUGIN_FILENAME = 0x58; // 88
  private static final int EVENT_TEXT = 0xC0;            // 192 - Generic text event
  private static final int EVENT_DATA = 0xD0;            // 208 - Generic data event

  // Plugin name filters
  private static final String[] EXCLUDED_PLUGIN_NAMES = {"pattern", "mixer", "master"};

  // Plugin file extensions
  private static final String EXT_DLL = ".dll";
  private static final String EXT_VST = ".vst";
  private static final String EXT_VST3 = ".vst3";
  private static final String EXT_SO = ".so";
  private static final String EXT_COMPONENT = ".component";

  private int projectVersion;

  public static class FLPlugin {
    private String name;
    private String path;
    private String vendor;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getVendor() {
      return vendor;
    }

    public void setVendor(String vendor) {
      this.vendor = vendor;
    }
  }

  /**
   * Parse FL Studio project file and extract plugins.
   *
   * @param file FL Studio .flp file
   * @return List of plugins found in the project
   * @throws IOException if file cannot be read
   */
  public List<FLPlugin> parseProject(File file) throws IOException {
    List<FLPlugin> plugins = new ArrayList<>();
    FLPlugin currentPlugin = null;

    try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {

      // Read and validate header
      byte[] header = new byte[4];
      dis.readFully(header);
      String headerStr = new String(header, StandardCharsets.US_ASCII);

      if (!"FLhd".equals(headerStr)) {
        throw new IOException("Invalid FL Studio project file: missing FLhd header");
      }

      // Read header chunk size (4 bytes, little-endian)
      int headerChunkSize = readInt32LE(dis);

      // Read format/version (2 bytes, little-endian)
      projectVersion = readInt16LE(dis);
      log.debug("FL Studio project version: {}", projectVersion);

      // Skip rest of header
      if (headerChunkSize > 2) {
        dis.skipBytes(headerChunkSize - 2);
      }

      // Read data chunk header
      byte[] dataHeader = new byte[4];
      dis.readFully(dataHeader);
      String dataHeaderStr = new String(dataHeader, StandardCharsets.US_ASCII);

      if (!"FLdt".equals(dataHeaderStr)) {
        throw new IOException("Invalid FL Studio project file: missing FLdt data header");
      }

      // Read data chunk size
      int dataChunkSize = readInt32LE(dis);
      log.debug("Data chunk size: {}", dataChunkSize);

      // Parse events
      int bytesRead = 0;
      while (bytesRead < dataChunkSize && dis.available() > 0) {
        int eventId = dis.readUnsignedByte();
        bytesRead++;

        // Determine event size and read data
        byte[] eventData;
        int eventSize;

        if (eventId < 64) {
          // 1-byte event (no additional data)
          eventSize = 0;
          eventData = new byte[0];
        } else if (eventId < 128) {
          // Variable length text/data event
          int length = dis.readUnsignedByte();
          bytesRead++;
          eventSize = length;
          eventData = new byte[length];
          dis.readFully(eventData);
          bytesRead += length;
        } else if (eventId < 192) {
          // 4-byte DWORD event
          eventSize = 4;
          eventData = new byte[4];
          dis.readFully(eventData);
          bytesRead += 4;
        } else {
          // Variable length event with DWORD size
          int length = readInt32LE(dis);
          bytesRead += 4;
          eventSize = length;
          eventData = new byte[length];
          dis.readFully(eventData);
          bytesRead += length;
        }

        // Process events related to plugins
        if (eventSize > 0) {
          processEvent(eventId, eventData, plugins);
        }
      }
    }

    return plugins;
  }

  private void processEvent(int eventId, byte[] data, List<FLPlugin> plugins) {
    try {
      switch (eventId) {
        case EVENT_TEXT:
        case EVENT_PLUGIN_NAME:
          // Plugin name - store as UTF-16LE string
          if (data.length > 0) {
            String name = decodeString(data);
            if (isPluginName(name)) {
              FLPlugin plugin = new FLPlugin();
              plugin.setName(name);
              plugins.add(plugin);
              log.debug("Found plugin name: {}", name);
            }
          }
          break;

        case EVENT_PLUGIN_PATH:
        case EVENT_PLUGIN_FILENAME:
          // Plugin path/filename
          if (data.length > 0) {
            String path = decodeString(data);
            if (isPluginPath(path)) {
              // Try to associate with last plugin or create new one
              if (!plugins.isEmpty()) {
                FLPlugin lastPlugin = plugins.get(plugins.size() - 1);
                if (lastPlugin.getPath() == null) {
                  lastPlugin.setPath(path);
                  log.debug("Added path to plugin: {}", path);
                } else {
                  // Create new plugin entry
                  FLPlugin plugin = new FLPlugin();
                  plugin.setPath(path);
                  plugins.add(plugin);
                  log.debug("Found plugin path: {}", path);
                }
              } else {
                FLPlugin plugin = new FLPlugin();
                plugin.setPath(path);
                plugins.add(plugin);
                log.debug("Found plugin path: {}", path);
              }
            }
          }
          break;

        case EVENT_PLUGIN_VENDOR:
          // Plugin vendor
          if (data.length > 0 && !plugins.isEmpty()) {
            String vendor = decodeString(data);
            FLPlugin lastPlugin = plugins.get(plugins.size() - 1);
            lastPlugin.setVendor(vendor);
            log.debug("Added vendor to plugin: {}", vendor);
          }
          break;

        default:
          // Ignore other events
          break;
      }
    } catch (Exception e) {
      log.debug("Error processing event {}: {}", eventId, e.getMessage());
    }
  }

  private String decodeString(byte[] data) {
    // FL Studio uses UTF-16LE for most strings
    // Try UTF-16LE first, fall back to UTF-8
    try {
      // Check if it looks like UTF-16LE (null bytes alternating)
      boolean hasNullBytes = false;
      for (int i = 1; i < Math.min(data.length, 10); i += 2) {
        if (data[i] == 0) {
          hasNullBytes = true;
          break;
        }
      }

      if (hasNullBytes) {
        // Likely UTF-16LE
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return StandardCharsets.UTF_16LE.decode(buffer).toString().trim();
      } else {
        // Try UTF-8
        return new String(data, StandardCharsets.UTF_8).trim();
      }
    } catch (Exception e) {
      // Fallback to ASCII
      return new String(data, StandardCharsets.US_ASCII).trim();
    }
  }

  private boolean isPluginName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    // Filter out empty strings and common non-plugin names
    String lower = name.toLowerCase();
    for (String excluded : EXCLUDED_PLUGIN_NAMES) {
      if (lower.contains(excluded) || lower.equals(excluded)) {
        return false;
      }
    }
    return name.length() > 1;
  }

  private boolean isPluginPath(String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    // Check if it's a VST/VST3 path
    String lower = path.toLowerCase();
    return lower.contains(EXT_DLL) 
        || lower.contains(EXT_VST) 
        || lower.contains(EXT_VST3)
        || lower.contains(EXT_SO)
        || lower.contains(EXT_COMPONENT);
  }

  private int readInt32LE(DataInputStream dis) throws IOException {
    byte[] bytes = new byte[4];
    dis.readFully(bytes);
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  private int readInt16LE(DataInputStream dis) throws IOException {
    byte[] bytes = new byte[2];
    dis.readFully(bytes);
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
  }

  public int getProjectVersion() {
    return projectVersion;
  }
}
