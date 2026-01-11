# VST2 SDK Headers

## Overview

This directory is intended to contain the VST2 SDK headers required to build OwlPlug with VST2 plugin support.

## Why are these headers needed?

JUCE (the audio framework used by OwlPlug) requires the VST2 SDK headers to compile VST2 plugin hosting support. Specifically, it needs:
- `pluginterfaces/vst2.x/aeffect.h`
- `pluginterfaces/vst2.x/aeffectx.h`

## How to obtain the VST2 SDK headers

The Steinberg VST2 SDK is no longer officially distributed due to licensing restrictions. However, you may be able to obtain the headers from:

1. **Older VST3 SDK versions**: VST3 SDK versions prior to June 2018 (such as vstsdk3610_11_06_2018_build_37) included the VST2 headers
2. **Legacy JUCE versions**: JUCE version 5.3.2 and older included a copy of the VST2 headers
3. **Your own archives**: If you previously downloaded the VST2 SDK, you may still have a copy

## Installation Instructions

Once you have obtained the VST2 SDK headers, place them in this directory structure:

```
owlplug-host/external/vst2_sdk/
└── pluginterfaces/
    └── vst2.x/
        ├── aeffect.h
        └── aeffectx.h
```

## Alternative: Disable VST2 Support

If you cannot obtain the VST2 SDK headers, you can disable VST2 support by:

1. Edit `owlplug-host/src/main/juce/OwlPlugHost.jucer`
2. Change `JUCE_PLUGINHOST_VST="1"` to `JUCE_PLUGINHOST_VST="0"`
3. Edit `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`
4. Change `#define JUCE_PLUGINHOST_VST 1` to `#define JUCE_PLUGINHOST_VST 0`

Note: Disabling VST2 support means OwlPlug will not be able to scan or host VST2 plugins. VST3, AU, and LV2 plugins will still work.

## Build Configuration

The build system needs to be configured to include this directory in the include path. This should be done by adding the directory to the compiler's include paths in the JUCE project configuration.

## License Note

The VST2 SDK is proprietary software owned by Steinberg Media Technologies GmbH. You must have a valid license to use the VST2 SDK. This project does not include or distribute the VST2 SDK.
