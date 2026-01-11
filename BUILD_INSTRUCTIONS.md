# Building OwlPlug Without VST2 SDK

## Important Notice

**VST2 support has been disabled in this build configuration.** This means:

- ✅ The project will build successfully without VST2 SDK headers
- ✅ VST3, AU, and LV2 plugin support remain fully functional
- ❌ **The application will NOT be able to scan or load VST2 plugins** (.dll, .vst files in VST2 format)

If you need VST2 plugin support, you must:
1. Obtain the VST2 SDK headers (see `owlplug-host/external/vst2_sdk/README.md`)
2. Re-enable VST2 support in the configuration files
3. Rebuild the native components

## Building the Native Host Component

The native host component is a JNI library (JUCE-based) that handles plugin scanning and loading. This must be built separately from the Java application.

### Prerequisites

1. **JUCE and Projucer**: Download Projucer first
   ```bash
   ./build/download-projucer.sh
   ```

2. **Platform-specific build tools**:
   - **Linux**: GCC/G++, make, and development libraries
   - **macOS**: Xcode and Command Line Tools
   - **Windows**: Visual Studio 2019 or later with C++ tools

3. **Java Development Kit**: JDK 25 or later
   - Set `JAVA_HOME` environment variable to your JDK installation

### Build Commands

#### Linux
```bash
./build/build-host-linux.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/LinuxMakefile/build/libowlplug-host.so
```

#### macOS
```bash
./build/build-host-osx.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/MacOSX/build/Release/libowlplug-host-1.5.0.dylib
```

#### Windows
```bash
./build/build-host-win.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/VisualStudio2019/x64/Release/owlplug-host-1.5.0.dll
```

## Replacing Files in Existing Installation

After building the native component, you need to replace the library file in your existing OwlPlug installation.

### Finding the Installation Directory

**Windows:**
- Default location: `C:\Program Files\OwlPlug\`
- The native library should be in the `app` subdirectory

**macOS:**
- Default location: `/Applications/OwlPlug.app/`
- Right-click the app and select "Show Package Contents"
- Navigate to `Contents/app/`

**Linux:**
- Installed via `.deb`: `/opt/owlplug/` or `/usr/lib/owlplug/`
- Check: `dpkg -L owlplug` to see all installed files

### Locating the Native Library in Installation

The native library file will be named:
- **Linux**: `libowlplug-host.so` or `libowlplug-host-1.5.0.so`
- **macOS**: `libowlplug-host-1.5.0.dylib` or `libowlplug-host.dylib`
- **Windows**: `owlplug-host-1.5.0.dll` or `owlplug-host.dll`

Look for it in:
```
<installation-directory>/app/
```

### Backup and Replace

1. **Backup the original library** (recommended):
   ```bash
   # Example for Linux
   cp /opt/owlplug/app/libowlplug-host.so /opt/owlplug/app/libowlplug-host.so.backup
   ```

2. **Copy the newly built library**:
   ```bash
   # Example for Linux
   sudo cp owlplug-host/src/main/juce/Builds/LinuxMakefile/build/libowlplug-host.so /opt/owlplug/app/
   ```

3. **Set proper permissions** (Linux/macOS):
   ```bash
   # Example for Linux
   sudo chmod 755 /opt/owlplug/app/libowlplug-host.so
   ```

4. **Restart OwlPlug** and verify it works

### Verification

After replacing the library:
1. Launch OwlPlug
2. Try scanning a plugin directory
3. Verify that VST3, AU, or LV2 plugins are detected
4. **Note**: VST2 plugins will NOT be detected with this build

## Building the Complete Application

If you want to build the entire OwlPlug application (not just the native component):

```bash
# Build native components first (see above)
./build/build-host-linux.sh    # or build-host-osx.sh or build-host-win.sh

# Build Java application
mvn clean install

# Build runnable JAR
cd owlplug-client
mvn clean install spring-boot:repackage

# Run OwlPlug
mvn spring-boot:run
```

## Troubleshooting

### "Library not found" or "JNI error"
- Ensure the library file is in the correct location
- Check that file permissions are correct (755 for executable)
- Verify the library was built for the correct architecture (x64)

### "Cannot load VST2 plugins"
- This is expected! VST2 support has been disabled
- Only VST3, AU, and LV2 plugins will work

### Build fails with VST2 SDK errors
- This shouldn't happen after the changes
- If it does, verify that:
  - `JUCE_PLUGINHOST_VST="0"` in `owlplug-host/src/main/juce/OwlPlugHost.jucer`
  - `JUCE_PLUGINHOST_VST 0` in `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`
  - VST2 SDK path has been removed from headerPath in `.jucer` file

## Re-enabling VST2 Support

If you later obtain the VST2 SDK and want to enable VST2 support:

1. Place VST2 SDK headers in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
   - Required files: `aeffect.h` and `aeffectx.h`

2. Edit `owlplug-host/src/main/juce/OwlPlugHost.jucer`:
   - Change `JUCE_PLUGINHOST_VST="0"` to `JUCE_PLUGINHOST_VST="1"`
   - Add VST2 SDK path back to headerPath for all platforms:
     - macOS: `../../../../external/vst2_sdk`
     - Linux: `../../../../external/vst2_sdk`
     - Windows: `..\..\..\..\external\vst2_sdk`

3. Edit `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`:
   - Change `#define   JUCE_PLUGINHOST_VST 0` to `#define   JUCE_PLUGINHOST_VST 1`
   - Note: Keep the existing spacing format in the file

4. Rebuild the native components using the build scripts above
