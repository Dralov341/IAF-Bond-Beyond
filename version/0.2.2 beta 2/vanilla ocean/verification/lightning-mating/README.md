# Lightning and mating regression tests

Requires Python 3 and Java 17. Prepare an isolated test project outside the source directory:

```sh
python3 verification/lightning-mating/prepare.py ../bond-beyond-tests
cd ../bond-beyond-tests
./gradlew clean runGameTestServer -x downloadAssets -PwithSpartan --console=plain --max-workers=2
```

On Windows use `python` and `.\gradlew.bat`. Omit `-PwithSpartan` to test without Spartan Fire/Extras; addon-specific tests then skip. Jade is present for its regression tests. Tests run the Vanilla Ocean profile to avoid world-generation dependencies; production Java is identical in both editions.

Always prepare a fresh test directory when repeating, so no old world, config or compiled test classes can interfere. Do not distribute the isolated test project's JAR. Release builds contain none of these fixtures. Tests capture actual outgoing custom payloads at the connection level, plus real Forge damage, entities and optional addon weapons.
