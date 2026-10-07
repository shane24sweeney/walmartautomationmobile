# mobile-automation-framework

Appium + Selenium 4 + Java mobile automation for Android and iOS.

## Architecture

Three layers, each depending only on the layer below it:

```
src/main/java/com/shanesweeney/mobile
├── core    Layer 3: driver & run setup
│   ├── Config          every run setting (-Dkey > ENV_VAR > default)
│   ├── DriverFactory   builds the Android/iOS driver and capabilities
│   ├── DevicePool      one device per parallel test class
│   └── AppiumServer    starts Appium once per run
└── pages   Layer 2: page objects, each owning its locators
    ├── BasePage        fluent waits and element actions
    ├── LoginScreen
    └── AlertDialog     native alert, shared by any screen

src/test/java/com/shanesweeney/mobile/tests
├── BaseTest            Layer 1: session lifecycle per test class
├── FailureArtifacts    screenshot, UI tree and device log on failure
└── LoginTest           test cases

src/test/resources/data
└── users.csv           login accounts (email,password), one test run per row
```

Rules that keep it maintainable:
- **Tests** call pages and assert. They never use locators or the driver directly.
- **Pages** describe user actions and return the page the app lands on. They never assert.
- **Locators** are private to the page they belong to, so a UI change touches only that page; tests stay untouched.
- **Platform differences** live in page locators (`byPlatform`) and `DriverFactory` only, so the same test runs on Android and iOS.

### Design patterns

| Pattern | Where | Why |
|---|---|---|
| Page Object | `pages/` | Separates what the user does from how the UI is found |
| Fluent page chaining | `LoginScreen.login()` returns `AlertDialog` | Tests read as user steps |
| Template / base class | `pages/BasePage`, `BaseTest` | Waits and session handling written once |
| Factory | `DriverFactory` | One place picks the platform and capabilities |
| Object pool | `DevicePool` | Safe device sharing across parallel threads |
| Singleton service | `AppiumServer` | One server per run, reused if already running |
| Extension / observer | `FailureArtifacts` (JUnit `TestWatcher`) | Failure capture without touching test code |

### Adding a screen and test

1. Add `pages/<Name>Screen.java` extending `BasePage`, with its locators as `private static final By` fields and one method per user action.
2. Add `tests/<Name>Test.java` extending `BaseTest`, tagged `smoke` or `regression`. Put test data in `src/test/resources/data/` as CSV and load it with `@CsvFileSource`.

## Prerequisites

- Java 21 and Maven
- Node 22 (`nvm use`)
- Android: SDK with the `Pixel_8_API_35` AVD
- iOS: Xcode with an `iPhone 17` simulator

Install Appium, its drivers and the app builds under test (downloaded to `apps/`):

```bash
npm install
```

## Running tests

Android only (default):

```bash
mvn test -Dplatform=android
```

iOS only:

```bash
mvn test -Dplatform=ios
```

The emulator or simulator boots automatically if it isn't running.

### Individual tests

```bash
mvn test -Dtest=LoginTest                             # one class
mvn test -Dtest='LoginTest#opensLoginScreen'          # one method
mvn test -Dtest='LoginTest#opensLoginScreen+logsIn*'  # several methods
mvn test -Dplatform=ios -Dtest=LoginTest              # combine with a platform
```

Tests in `LoginTest` share one app session and run in order, so a later method may depend on an earlier one.

### By tag

```bash
mvn test -Dgroups=smoke              # only smoke tests
mvn test -DexcludedGroups=smoke      # everything else
```

### Settings

All settings live in `core/Config.java`. Each can be set as a `-D` property or an environment variable; `-D` wins.

| Property / env var | Default | Purpose |
|---|---|---|
| `platform` / `PLATFORM` | `android` | `android` or `ios` |
| `avd` / `AVD` | `Pixel_8_API_35` | Android emulator to boot |
| `deviceName` / `DEVICE_NAME` | `iPhone 17` | iOS simulator to boot |
| `devices` / `DEVICES` | the single device above | Comma-separated device pool for parallel runs |
| `platformVersion` / `PLATFORM_VERSION` | newest installed | iOS runtime version |
| `appiumUrl` / `APPIUM_URL` | `http://127.0.0.1:4723` | Appium server to connect to |
| `waitTimeout` / `WAIT_TIMEOUT` | `10` | Seconds each explicit/fluent wait allows |

```bash
DEVICE_NAME="iPhone 17e" mvn test -Dplatform=ios
```

Find or create devices:

```bash
emulator -list-avds                       # Android emulators
adb devices                               # running emulators
xcrun simctl list devices available       # iOS simulators
```

### Parallel execution

Test classes run in parallel, one device per class. Methods inside a class share one app session and run in order. List one device per thread:

```bash
mvn test -Dthreads=2 -Ddevices=Pixel_8_API_35,Pixel_7_API_34
mvn test -Dplatform=ios -Dthreads=2 -Ddevices="iPhone 17,iPhone 17e"
```

Each device gets its own Appium ports (Android `systemPort` 8200+, iOS `wdaLocalPort` 8100+), and one Appium server handles every session. If `threads` is higher than the number of devices, extra classes wait for a free device.

## Locators & capabilities

- Each page declares its own locators at the top of its class. Prefer accessibility IDs (`AppiumBy.accessibilityId`), which work on both platforms; use `byPlatform(android, ios)` only where the platforms differ (e.g. native alerts). Avoid XPath, which is slow and breaks easily.
- Capabilities are set in `core/DriverFactory.java`, using the typed `UiAutomator2Options` / `XCUITestOptions` classes.
- Waits are explicit or fluent only (`pages/BasePage.java`); there are no implicit waits and no `Thread.sleep`.

## Flaky tests

What the framework already does to prevent flakiness:
- Every interaction waits for its element, and taps retry if the screen re-renders mid-tap.
- Parallel sessions never share a device or a port.
- Android permission prompts are granted automatically, so they can't block a screen at random.

To find and diagnose a flaky test:

```bash
# Re-run failures up to 2 times; tests that pass on a re-run are reported as "Flakes"
mvn test -Dsurefire.rerunFailingTestsCount=2

# Run one test repeatedly until it fails
for i in {1..10}; do mvn test -Dtest=LoginTest || break; done
```

Re-runs are for finding flaky tests, not hiding them. Fix the cause using the failure artifacts below.

## Debugging

Every failed test saves its screenshot (`.png`), UI hierarchy (`.xml`) and device log (`.log`, logcat on Android or syslog on iOS) to `target/failure-artifacts/`. The `.xml` shows the element tree Appium saw, which helps when a locator doesn't match.

Attach a debugger: the test JVM pauses until a debugger connects on port 5005 (in VS Code, use a Java "Attach" configuration with `"port": 5005`):

```bash
mvn test -Dmaven.surefire.debug -Dtest=LoginTest
```

Show full stack traces in the console:

```bash
mvn test -DtrimStackTrace=false
```

Follow the Appium server log while tests run:

```bash
tail -f appium.log
```

Per-test results and output are written to `target/surefire-reports/`.

Live device logs:

```bash
adb logcat                                        # Android
xcrun simctl spawn booted log stream --level debug  # iOS simulator
```

To inspect elements and try locators on a running app, use [Appium Inspector](https://github.com/appium/appium-inspector) against `http://127.0.0.1:4723` with the same capabilities as `DriverFactory`.

A debugger paused on a breakpoint keeps the session alive for up to 5 minutes (`newCommandTimeout`).

## Appium server

Tests start a project-local Appium server automatically (logs go to `appium.log`). To run it yourself, for example to watch the logs live:

```bash
npm run appium
```

Tests reuse a server already listening on `http://127.0.0.1:4723`.
