# LemonFlow

A personal, offline Android workday planner: pick items from a catalog onto today or
tomorrow, track them, and see a live schedule of what still fits before the day closes.
All data stays on the device in an encrypted database; the app has no network access.

## Build

Requires JDK 21 and the Android SDK (platform 37).

```sh
./gradlew :domain:test :app:assembleDebug :app:lintDebug
```

## License

Copyright (c) 2026 Valentyn Lukianets.

Licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or
  https://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or https://opensource.org/licenses/MIT)

at your option.

Unless you explicitly state otherwise, any contribution intentionally submitted for
inclusion in the work by you, as defined in the Apache-2.0 license, shall be dual licensed
as above, without any additional terms or conditions.
