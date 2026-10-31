# Developer Notes

Fabric is a Bring Your Own type situation, where you have to install the mod yourself. For forge, it installs the mod itself. I don't know why they won't play nicely together, but they don't. I'm just dealing with two children here and none of them are mine. If you are reading this, hi.

## Running from VS Code

`.vscode/launch.json` and `.vscode/tasks.json` are written by hand, not generated.

**Debugging (F5, Run and Debug view):**

| Configuration | What it does |
| --- | --- |
| `Forge Client` / `Forge Server` / `Forge Data` | Builds `:forge:classes`, then launches the game directly with the debugger attached. |
| `Fabric Client (debug)` / `Fabric Server (debug)` | Runs `:fabric:runClient -Pdebug` as a background task, which starts the game suspended on port 5005, then attaches to it. |

**Just running, no debugger:** use the `fabric: runClient` / `fabric: runServer` tasks from Terminal > Run Task. Forge has no plain task variant because its F5 configuration is already a direct launch.

### Why the Forge configurations are hand written

ModDevGradle's own VS Code generator points `MOD_CLASSES` at the Eclipse/JDT output folder, `forge/bin/main`. The Java extension copies `src/main/resources` into that folder verbatim, so `META-INF/mods.toml` still holds its `${mod_id}` placeholders and Forge dies during mod discovery with:

```
com.electronwill.nightconfig.core.io.ParsingException: Invalid bare key: ${mod_id}
```

`disableIdeRun()` in `forge/build.gradle` switches that generator off, and `launch.json` points at Gradle's processed output instead - the same folders the IntelliJ run configs use:

- `forge/build/classes/java/main` - compiled classes
- `forge/build/resources/main` - resources with the placeholders expanded

Two things worth knowing:

- The generator rewrites `.vscode/launch.json` whenever VS Code syncs the Gradle project. It keeps configurations it did not create, but it strips `//` comments and reformats the file, so explanations live here rather than in `launch.json`.
- `MOD_CLASSES` entries are separated by `File.pathSeparator`: `:` on Linux/macOS, `;` on Windows. These configurations use `:`.

### Why Fabric goes through Gradle

Fabric Loom has no VS Code integration, so there is no generated argument file to reuse. The Gradle task owns the classpath and Loom's dev JVM arguments; `-Pdebug` (see `fabric/build.gradle`) adds the JDWP agent and `launch.json` attaches to it.