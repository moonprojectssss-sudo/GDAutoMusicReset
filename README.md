# GD Auto Music Reset

Android app that plays your own MP3 next to Geometry Dash and restarts it
from 0:00 when a playtest starts or when you die and respawn in the editor.

No root, no Geode, no modified APK. It only uses the screen capture
permission (MediaProjection). Frames are analyzed on the phone and are
never saved or uploaded.

## How to use

1. Open the app and tap PICK MP3.
2. Tap START DETECTION. Android asks for screen capture: choose
   "Entire screen" (not a single app) and allow it.
3. Tap LAUNCH GEOMETRY DASH (it also asks for capture if detection is off).
4. In the editor, press playtest:
   - playtest starts -> music restarts from 0:00
   - you die and respawn -> music restarts from 0:00
   - playtest ends -> music pauses (can be turned off in settings)
5. In Geometry Dash, set Music volume to 0 so only your MP3 is heard.

## How detection works

- Playtest is detected by the round green STOP button that appears at the
  left side of the screen only during a playtest.
- A respawn is detected when the screen drifts away from the first frame of
  the attempt and then matches that frame again. Flashes and screen shake
  alone do not trigger a reset.
- The reset happens at respawn, when the new attempt starts, not at the
  exact moment of death.

## Settings

- Sensitivity: higher = looser matching. Raise it if resets never fire,
  lower it if they fire too often.
- Cooldown: minimum time between two resets.

## Limits

- Tuned for a 2400x1080 landscape screen (Redmi Note 10 Pro). Other screens
  may need the button position in Detector.kt adjusted.
- Levels with a bright green background can confuse playtest detection.
- Normal (non-editor) level gameplay is not supported yet.

## Build

Push to GitHub. The workflow in .github/workflows/build.yml builds the APK.
Download it from the Actions tab (artifact GDAutoMusicReset-debug).
