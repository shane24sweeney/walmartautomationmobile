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
│   ├── Biometrics      enrol Face ID / a fingerprint and simulate scans (simulators, emulators)
│   └── Gestures        swipe, scroll and drag-and-drop on W3C Actions
└── pages   Layer 2: page objects, each owning its locators
    ├── BasePage        fluent waits, element actions, logging + report steps
    ├── TabBar          bottom navigation; entry point to every screen
    ├── LoginScreen     login, sign-up, validation errors, biometric login button
    ├── BiometricPrompt the system Face ID / fingerprint prompt
    ├── WebviewScreen   the Web tab's webview, with native/web context switching
    ├── FormsScreen     text input, switch, dropdown, buttons
    ├── SwipeScreen     carousel and scroll-to-element
    ├── DragScreen      drag-and-drop puzzle
    └── AlertDialog     native alert, shared by any screen

src/test/java/com/shanesweeney/mobile/tests
├── BaseTest            Layer 1: session per class, app restart per test
├── TestArtifacts       logs for every test; screenshot and UI tree on failure
├── LoginTest
├── BiometricLoginTest  Face ID / fingerprint login (simulators and emulators only)
├── FormsTest
├── GesturesTest
└── WebviewTest         hybrid app: web page in a webview, links, back to native

src/test/resources
├── data/users.csv           valid accounts, one test run per row
├── data/invalid-logins.csv  bad input and the error each should show
├── logback-test.xml
└── allure.properties
```

Rules that keep it maintainable:
- **Tests** navigate from `tabBar`, call pages and assert. They never use locators or the driver, except to hand it to a `core` helper for device setup (e.g. `Biometrics.enrol` in `@BeforeAll`).
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
| Extension / observer | `TestArtifacts` (JUnit callbacks) | Log and failure capture without touching test code |
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
| `platformVersion` / `PLATFORM_VERSION` | any | OS version the device must run (iOS: simulator runtime) |
| `app` / `APP` | `apps/` build | App to install: path, URL or BrowserStack `bs://` ID |
| `appiumUrl` / `APPIUM_URL` | `http://127.0.0.1:4723` | Comma-separated Appium servers, shared round robin by the devices; local ones are started, remote ones (e.g. a Selenium Grid) used as is |
| `xcodeOrgId` / `XCODE_ORG_ID` | none | Apple team ID to sign WebDriverAgent on a real iPhone |
| `waitTimeout` / `WAIT_TIMEOUT` | `10` | Seconds each explicit/fluent wait allows |
| `noReset` / `NO_RESET` | `false` | Keep app data between sessions |
| `fullReset` / `FULL_RESET` | `false` | Reinstall the app for every session (not with `noReset`) |
| `language` / `LANGUAGE` | device setting | Device language, e.g. `es` |
| `locale` / `LOCALE` | device setting | Device region, e.g. `ES` |
| `headless` / `HEADLESS` | `false` | Boot emulators/simulators without a window |

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

Each device gets its own Appium ports (Android `systemPort` 8200+, iOS `wdaLocalPort` 8100+), and by default one Appium server handles every session. To give each device its own server, list one per device; device N uses server N modulo the list, and each local server is started on its port:

```bash
mvn test -Dthreads=2 -Ddevices=Pixel_8_API_35,Pixel_8_API_35_2 -DappiumUrl=http://127.0.0.1:4723,http://127.0.0.1:4724
```

If `threads` is higher than the number of devices, extra classes wait for a free device. On two Android emulators the full suite takes about 3 minutes instead of about 9.5 on one.

## Reports and logs

- **Allure:** every user action is a report step. Every test, passed or failed, on Android and iOS, carries its own logs: the test log, the device log (logcat or syslog), and its Appium server and HTTP log lines. Failed tests also carry a screenshot and the UI tree, saved to `target/failure-artifacts/` too.
  ```bash
  npm run report        # build target/allure-report from target/allure-results
  npm run report:open   # open it in a browser
  ```
- **Logs:** console and `target/test.log`, with the thread name on every line so parallel runs show which device did what. `LOG_LEVEL=DEBUG mvn test` for more detail. Typed text is never logged, since it may be a password.
- **Appium server logs:** `appium-<port>.log`, one per local server, including the server's `[HTTP]` request/response lines.
- **Client HTTP log:** `target/appium-http.log` has every command the tests sent to Appium and its response (status, duration, body; large bodies shortened, BrowserStack access key masked). `HTTP_LOG_LEVEL=INFO` turns it off.
- **JUnit results:** `target/surefire-reports/`.
- **Reading logs in VS Code:** the free [Log File Highlighter](https://marketplace.visualstudio.com/items?itemName=emilast.LogFileHighlighter) extension colours timestamps, levels and errors in `.log` files.

### ReportPortal

[ReportPortal](https://reportportal.io) (free, open source, self-hosted) keeps the history of every run, groups failures with the same cause, and its auto-analysis labels new failures as product bug, automation bug or system issue from past decisions. The `reportportal` Maven profile sends each test, its log lines and the failure artifacts above; without it nothing changes.

```bash
# Start a local server (needs Docker with ~8 GB of memory); UI at http://localhost:8080, superadmin / erebus
curl -LO https://raw.githubusercontent.com/reportportal/reportportal/master/docker-compose.yml
docker compose -p reportportal up -d

# Create an API key under your profile in the UI, then
RP_API_KEY=... mvn test -Preportportal
```

Settings are in [`reportportal.properties`](src/test/resources/reportportal.properties); override any with `-D`, e.g. `-Drp.endpoint=https://rp.example.com -Drp.project=mobile`. Each launch is tagged with its platform. `docker compose -p reportportal down` stops the server and keeps its data.

## CI

[`.github/workflows/tests.yml`](.github/workflows/tests.yml) runs the Android suite on every push and pull request, on an Ubuntu runner with a KVM-accelerated emulator. The iOS job runs on a macOS runner with whichever iPhone simulator the image ships; since macOS minutes count 10x against a private repo's quota, it runs only when the workflow is started manually from the Actions tab, which can also limit the run to tags such as `smoke`. Each job publishes its JUnit results as a check (each failed test with its message and stack trace on the run's summary page) and uploads the Allure report, failure artifacts, Appium server logs and the HTTP log.

## Locators & capabilities

- Prefer accessibility IDs (`AppiumBy.accessibilityId`); in React Native, `testID`/`accessibilityLabel` map to them on both platforms.
- Use `byPlatform(android, ios)` only where the platforms differ: native alerts (`android:id/...` vs an iOS class chain), Android resource IDs vs iOS names.
- Text without an ID (validation messages) uses `byText`: a UiSelector on Android, the accessibility ID on iOS.
- No XPath: it is slow on both platforms and breaks when layout changes.
- Capabilities use the typed `UiAutomator2Options` / `XCUITestOptions` classes and are all chained in `DriverFactory`: `commonOptions` for both platforms (device, app, reset, session), then one chain per platform (UiAutomator2 / WebDriverAgent timeouts, animations, alerts, keyboard), then the few that depend on the target (emulator, real device, BrowserStack). Unset optional values are left out of the session request.
- Not set: browser, Chrome/Safari and webview capabilities (native app only), and alert auto-accept/dismiss (tests assert on the app's alerts).

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
| Taps on SIGN UP / Active fail in CI only | CI's 320x640 emulator puts them below the fold, and Android leaves off-screen elements out of the tree | Pages `scrollTo` lower buttons before tapping |
| Random permission dialogs | Android runtime permissions | `autoGrantPermissions` |
| Parallel sessions collide | Shared device or port | `DevicePool` gives each thread its own device and ports |

To find and diagnose a flaky test:

```bash
mvn test -Dsurefire.rerunFailingTestsCount=2                  # passes on re-run are reported as "Flakes"
for i in {1..10}; do mvn test -Dtest=FormsTest || break; done  # repeat until it fails
```

Re-runs are for finding flaky tests, not hiding them. Fix the cause using the failure artifacts.

## Debugging

Every failed test saves its screenshot (`.png`), UI hierarchy (`.xml`), test log, device log (logcat on Android or syslog on iOS), and Appium server and HTTP log lines to `target/failure-artifacts/` and attaches them to the Allure report. The `.xml` shows the element tree Appium saw, which helps when a locator doesn't match.

```bash
mvn test -Dmaven.surefire.debug -Dtest=LoginTest   # JVM waits for a debugger on port 5005
mvn test -DtrimStackTrace=false                    # full stack traces
tail -f appium-4723.log                            # Appium server log
tail -f target/appium-http.log                     # client-side HTTP commands
adb logcat                                         # live Android log
xcrun simctl spawn booted log stream --level debug # live iOS simulator log
```

To inspect elements and try locators on a running app, use [Appium Inspector](https://github.com/appium/appium-inspector) against `http://127.0.0.1:4723` with the same capabilities as `DriverFactory`. A debugger paused on a breakpoint keeps the session alive for up to 5 minutes (`newCommandTimeout`).

## Appium server

The framework runs on Appium 3 (pinned in `package.json`). Tests start a project-local Appium server automatically (log in `appium-<port>.log`) and reuse one already listening on `appiumUrl`, failing fast if that local server is not Appium 3. To run it yourself, for example to watch the log live:

```bash
npm run appium
```

## Webview and biometric tests

- **Webview** (`WebviewTest`): the Web tab shows the live https://webdriver.io site, so these tests need network access. `WebviewScreen` switches the session into the webview only for each call and always back to the native app, even on failure. On Android, Appium downloads the Chromedriver matching the device's WebView itself; `.appiumrc.json` allows only that one opt-in feature (`uiautomator2:chromedriver_autodownload`).
- **Biometrics** (`BiometricLoginTest`): runs on simulators and emulators only, and is skipped with a reason on real devices and BrowserStack. It changes the device for the length of the class and puts it back afterwards:
  - **iOS:** enrols Face ID on the simulator, then unenrols it.
  - **Android:** sets a screen-lock PIN (`1111`), enrols a fingerprint through Settings (the emulator must be in English), then clears the PIN, which also removes the fingerprint. If a run is killed part-way, the next run clears the leftover PIN before enrolling.

## Known limitations

- The iOS build's dropdown picker does not open under XCUITest (element click, W3C tap and `mobile: tap` all fail), so `selectsDropdownOption` runs on Android only and is skipped on iOS.
- The demo app's carousel sometimes stops responding after several swipes in a row, and occasionally ignores a single swipe; tests only swipe from a freshly started app, and `SwipeScreen` swipes once more (logging a warning) if the carousel hasn't moved after 5 seconds.
- Run one `mvn test` at a time per checkout. A second run reuses the first run's Appium server, which stops when the first run ends (failing the second with `UnreachableBrowserException`), and both write the same `target/` logs and reports. To run Android and iOS side by side, use separate clones or `npm run appium` started on its own.
