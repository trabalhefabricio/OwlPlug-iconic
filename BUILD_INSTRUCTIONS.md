# Building OwlPlug With VST2 SDK

## Important Notice

**VST2 support is enabled in this build configuration.** This means:

- ✅ VST2, VST3, AU, and LV2 plugin support are all functional
- ⚠️ **You must provide the VST2 SDK headers to build the native components**
- ❌ The project will NOT build without the VST2 SDK headers

To build successfully, you must:
1. Obtain the VST2 SDK headers (see `owlplug-host/external/vst2_sdk/README.md`)
2. Place them in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
3. Build the native components

## Building the Native Host Component

The native host component is a JNI library (JUCE-based) that handles plugin scanning and loading. This must be built separately from the Java application.

### Prerequisites

1. **VST2 SDK Headers**: Required for building
   - Obtain `aeffect.h` and `aeffectx.h` from older VST3 SDK versions (pre-June 2018) or legacy JUCE versions (5.3.2 or older)
   - Place in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
   - See `owlplug-host/external/vst2_sdk/README.md` for detailed instructions

2. **JUCE and Projucer**: Download Projucer first
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
3. Verify that VST2, VST3, AU, and LV2 plugins are detected
4. **Note**: All plugin formats should be working with this build

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

### Build fails with VST2 SDK errors
- Make sure you have placed the VST2 SDK headers in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
- Required files: `aeffect.h` and `aeffectx.h`
- See `owlplug-host/external/vst2_sdk/README.md` for instructions on obtaining the headers

## Disabling VST2 Support (Optional)

If you want to build without VST2 support (to avoid needing the SDK headers):

1. Edit `owlplug-host/src/main/juce/OwlPlugHost.jucer`:
   - Change `JUCE_PLUGINHOST_VST="1"` to `JUCE_PLUGINHOST_VST="0"`
   - Remove VST2 SDK path from headerPath for all platforms (remove `../../../../external/vst2_sdk` or `..\..\..\..\external\vst2_sdk`)

2. Edit `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`:
   - Change `#define   JUCE_PLUGINHOST_VST 1` to `#define   JUCE_PLUGINHOST_VST 0`

3. Rebuild the native components using the build scripts above

Note: With VST2 disabled, the application will not be able to scan or load VST2 plugins. VST3, AU, and LV2 will still work.
