# walmartautomationmobile

[![tests](https://github.com/shane24sweeney/walmartautomationmobile/actions/workflows/tests.yml/badge.svg)](https://github.com/shane24sweeney/walmartautomationmobile/actions/workflows/tests.yml)

Appium + Selenium 4 + Java 21 + JUnit 6 mobile automation for Android and iOS. One test suite runs unchanged on both platforms, on emulators/simulators, real devices or BrowserStack, one device per thread in parallel.

The app under test is the open-source [WebdriverIO demo app](https://github.com/webdriverio/native-demo-app) (React Native): login and sign-up forms, form controls, a swipeable carousel and a drag-and-drop puzzle.

## Architecture

Three layers, each depending only on the layer below it:

```
src/main/java/com/shanesweeney/mobile
├── core    Layer 3: driver & run setup (no app knowledge except AppUnderTest)
│   ├── Config          every run setting (-Dkey > ENV_VAR > default)
│   ├── DriverFactory   builds the Android/iOS session for the chosen target
│   ├── DevicePool      one device per parallel test class
│   ├── AppiumServer    starts Appium once per run, or uses a running/remote one
│   ├── AppUnderTest    app IDs, builds, and restart between tests
│   └── Gestures        swipe, scroll and drag-and-drop on W3C Actions
└── pages   Layer 2: page objects, each owning its locators
    ├── BasePage        fluent waits, element actions, logging + report steps
    ├── TabBar          bottom navigation; entry point to every screen
    ├── LoginScreen     login, sign-up, validation errors
    ├── FormsScreen     text input, switch, dropdown, buttons
    ├── SwipeScreen     carousel and scroll-to-element
    ├── DragScreen      drag-and-drop puzzle
    └── AlertDialog     native alert, shared by any screen

src/test/java/com/shanesweeney/mobile/tests
├── BaseTest            Layer 1: session per class, app restart per test
├── FailureArtifacts    screenshot, UI tree and device log on failure
├── LoginTest
├── FormsTest
└── GesturesTest

src/test/resources
├── data/users.csv           valid accounts, one test run per row
├── data/invalid-logins.csv  bad input and the error each should show
├── logback-test.xml
└── allure.properties
```

Rules that keep it maintainable:
- **Tests** navigate from `tabBar`, call pages and assert. They never use locators or the driver.
- **Pages** describe user actions and return the page the app lands on. They never assert.
- **Locators** are private to their page, so a UI change touches one file.
- **Platform differences** stay inside pages (`byPlatform`) and `DriverFactory`, so tests are platform-neutral.
- **Tests are independent.** The app restarts before every test, so tests run in any order, alone or in parallel.

### Design patterns

| Pattern | Where | Why |
|---|---|---|
| Page Object | `pages/` | Separates what the user does from how the UI is found |
| Fluent page chaining | `tabBar.openLogin().login(...)` returns `AlertDialog` | Tests read as user steps |
| Component object | `TabBar`, `AlertDialog` | UI shared by many screens is modelled once |
| Template / base class | `BasePage`, `BaseTest` | Waits, retries and session handling written once |
| Factory | `DriverFactory` | One place picks platform, target and capabilities |
| Object pool | `DevicePool` | Safe device sharing across parallel threads |
| Singleton service | `AppiumServer` | One server per run, reused if already running |
| Extension / observer | `FailureArtifacts` (JUnit `TestWatcher`) | Failure capture without touching test code |
| Data-driven tests | `@CsvFileSource` | New cases are new CSV rows, not new code |

### Adding a screen and test

1. Add `pages/<Name>Screen.java` extending `BasePage`: locators as `private static final By` fields, one public method per user action.
2. Add an `open<Name>()` method to `TabBar` (or return the new page from the action that leads to it).
3. Add `tests/<Name>Test.java` extending `BaseTest`, tagged `regression` and, for critical paths, `smoke`. Put test data in `src/test/resources/data/` as CSV.

## Setup

- Java 21 and Maven
- Node 22 (`nvm use`)
- Android: SDK with the `Pixel_8_API_35` AVD
- iOS: Xcode with an `iPhone 17` simulator

One command installs Appium, its UiAutomator2 and XCUITest drivers (into `.appium/`) and downloads the app builds (into `apps/`, not committed; the APK is over GitHub's file size limit):

```bash
npm install
```

## Running tests

```bash
mvn test                       # Android (default)
mvn test -Dplatform=ios        # iOS
```

The emulator or simulator boots automatically if it isn't running.

```bash
mvn test -Dtest=LoginTest                             # one class
mvn test -Dtest='LoginTest#signsUp'                   # one method
mvn test -Dtest='LoginTest#opensLoginScreen+logsIn*'  # several methods
mvn test -Dgroups=smoke                               # only smoke tests
mvn test -DexcludedGroups=smoke                       # everything else
```

### Settings

All settings live in `core/Config.java`. Each can be set as a `-D` property or an environment variable; `-D` wins.

| Property / env var | Default | Purpose |
|---|---|---|
| `platform` / `PLATFORM` | `android` | `android` or `ios` |
| `target` / `TARGET` | `emulator` | `emulator`, `device` (real devices by UDID) or `browserstack` |
| `avd` / `AVD` | `Pixel_8_API_35` | Android emulator to boot |
| `deviceName` / `DEVICE_NAME` | `iPhone 17` | iOS simulator to boot |
| `devices` / `DEVICES` | the single device above | Comma-separated device pool for parallel runs |
| `platformVersion` / `PLATFORM_VERSION` | newest installed | iOS simulator runtime |
| `app` / `APP` | `apps/` build | App to install: path, URL or BrowserStack `bs://` ID |
| `appiumUrl` / `APPIUM_URL` | `http://127.0.0.1:4723` | Appium server; a remote URL (e.g. a Selenium Grid) is used as is |
| `xcodeOrgId` / `XCODE_ORG_ID` | none | Apple team ID to sign WebDriverAgent on a real iPhone |
| `waitTimeout` / `WAIT_TIMEOUT` | `10` | Seconds each explicit/fluent wait allows |

### Devices

```bash
emulator -list-avds                    # Android emulators
adb devices                            # running emulators and connected phones (UDIDs)
xcrun simctl list devices available    # iOS simulators
xcrun xctrace list devices             # connected iPhones (UDIDs)
```

Real devices are selected by UDID:

```bash
mvn test -Dtarget=device -Ddevices=R58M123ABC                       # Android phone
mvn test -Dplatform=ios -Dtarget=device -Ddevices=<udid> \
  -Dapp=/path/to/signed.ipa -DxcodeOrgId=<team id>                   # iPhone
```

BrowserStack (needs an App Automate account; this path has not been run, as no account was available):

```bash
export BROWSERSTACK_USERNAME=... BROWSERSTACK_ACCESS_KEY=...
mvn test -Dtarget=browserstack -Dapp=bs://<app id> -Ddevices="Google Pixel 8:14.0"
```

### Parallel execution

Test classes run in parallel, one device per class; tests inside a class run one after another on that device. List one device per thread:

```bash
mvn test -Dthreads=2 -Ddevices=Pixel_8_API_35,Pixel_8_API_35_2
mvn test -Dplatform=ios -Dthreads=2 -Ddevices="iPhone 17,iPhone 17e"
```

Each device gets its own Appium ports (Android `systemPort` 8200+, iOS `wdaLocalPort` 8100+), and one Appium server handles every session. If `threads` is higher than the number of devices, extra classes wait for a free device. On two Android emulators the full suite takes about 3 minutes instead of about 9.5 on one.

## Reports and logs

- **Allure:** every user action is a report step, and failed tests carry their screenshot, UI tree and device log.
  ```bash
  npm run report        # build target/allure-report from target/allure-results
  npm run report:open   # open it in a browser
  ```
- **Logs:** console and `target/test.log`, with the thread name on every line so parallel runs show which device did what. `LOG_LEVEL=DEBUG mvn test` for more detail. Typed text is never logged, since it may be a password.
- **Appium server log:** `appium.log`.
- **JUnit results:** `target/surefire-reports/`.

## CI

[`.github/workflows/tests.yml`](.github/workflows/tests.yml) runs the Android suite on every push and pull request, on an Ubuntu runner with a KVM-accelerated emulator. The iOS job runs on a macOS runner with whichever iPhone simulator the image ships; since macOS minutes count 10x against a private repo's quota, it runs only when the workflow is started manually from the Actions tab, which can also limit the run to tags such as `smoke`. Each job uploads the Allure report, failure artifacts and logs.

## Locators & capabilities

- Prefer accessibility IDs (`AppiumBy.accessibilityId`); in React Native, `testID`/`accessibilityLabel` map to them on both platforms.
- Use `byPlatform(android, ios)` only where the platforms differ: native alerts (`android:id/...` vs an iOS class chain), Android resource IDs vs iOS names.
- Text without an ID (validation messages) uses `byText`: a UiSelector on Android, the accessibility ID on iOS.
- No XPath: it is slow on both platforms and breaks when layout changes.
- Capabilities use the typed `UiAutomator2Options` / `XCUITestOptions` classes in `DriverFactory`.

## Flaky tests

Waits are explicit or fluent only (`BasePage`): no implicit waits, no `Thread.sleep`. Each one names what it waited for, so a timeout reads `waiting for AppiumBy.accessibilityId: input-email to be visible` rather than naming a lambda.

Issues found running this suite, and how the framework handles them:

| Symptom | Cause | Fix |
|---|---|---|
| Later tests fail after an earlier one fails | Tests shared app state | App restarts before every test (`AppUnderTest.restart`) |
| First test on a device can't find the tab bar | Cold launch held the splash screen past the 10 s wait (two emulators sharing a CPU) | `BaseTest` waits up to 60 s for the tab bar after each restart |
| A parallel run loses a device mid-run | A first-boot emulator drops its adb connection while finishing setup | Boot new AVDs once before relying on them; failures name the device in the log |
| iOS drops typed characters (`new.user` → `neuser`) | Keys sent while the keyboard slides in | `type()` waits for the keyboard on iOS, reads the value back and retypes |
| iOS sign-up fields stay empty | iOS shows "Use Strong Password?" instead of the keyboard | `type()` closes the sheet and retries |
| Taps miss buttons after typing | Keyboard covers them; `hideKeyboard()` doesn't work on iOS | `hideKeyboard(element)` taps a non-interactive element until it closes |
| Scrolling stops partway | The swipe landed on a horizontal carousel, which captured it | `scrollDown` swipes in the free band above or below the carousel |
| Carousel swipe ignored | Swipe started in the gap between cards | Swipes start 70% across the card |
| Assertion reads the old carousel card | Read during the snap animation | `swipeCarouselLeft` waits until the active card changes |
| Drag-and-drop finds the wrong piece | Remaining pieces re-layout after each drop | Wait for the dropped piece to disappear before the next drag |
| Random permission dialogs | Android runtime permissions | `autoGrantPermissions` |
| Parallel sessions collide | Shared device or port | `DevicePool` gives each thread its own device and ports |

To find and diagnose a flaky test:

```bash
mvn test -Dsurefire.rerunFailingTestsCount=2                  # passes on re-run are reported as "Flakes"
for i in {1..10}; do mvn test -Dtest=FormsTest || break; done  # repeat until it fails
```

Re-runs are for finding flaky tests, not hiding them. Fix the cause using the failure artifacts.

## Debugging

Every failed test saves its screenshot (`.png`), UI hierarchy (`.xml`) and device log (`.log`, logcat on Android or syslog on iOS) to `target/failure-artifacts/` and attaches them to the Allure report. The `.xml` shows the element tree Appium saw, which helps when a locator doesn't match.

```bash
mvn test -Dmaven.surefire.debug -Dtest=LoginTest   # JVM waits for a debugger on port 5005
mvn test -DtrimStackTrace=false                    # full stack traces
tail -f appium.log                                 # Appium server log
adb logcat                                         # live Android log
xcrun simctl spawn booted log stream --level debug # live iOS simulator log
```

To inspect elements and try locators on a running app, use [Appium Inspector](https://github.com/appium/appium-inspector) against `http://127.0.0.1:4723` with the same capabilities as `DriverFactory`. A debugger paused on a breakpoint keeps the session alive for up to 5 minutes (`newCommandTimeout`).

## Appium server

Tests start a project-local Appium server automatically (log in `appium.log`) and reuse one already listening on `appiumUrl`. To run it yourself, for example to watch the log live:

```bash
npm run appium
```

## Known limitations

- The iOS build's dropdown picker does not open under XCUITest (element click, W3C tap and `mobile: tap` all fail), so `selectsDropdownOption` runs on Android only and is skipped on iOS.
- The demo app's carousel sometimes stops responding after several swipes in a row; tests only swipe from a freshly started app.
