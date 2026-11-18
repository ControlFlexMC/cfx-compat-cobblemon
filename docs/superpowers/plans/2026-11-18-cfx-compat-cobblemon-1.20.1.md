# cfx-compat-cobblemon 1.20.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ControlFlex ↔ Cobblemon bridge to Minecraft 1.20.1 Forge and Fabric, compiled against Cobblemon 1.5.2, with 18 battle actions, per-tile hints, a cursor-driven confirm page, and the interaction-wheel radial entry.

**Architecture:** `common/` holds a pure page model (`BattleSlots`), a typed `BattleGui` adapter (`BattleScreen`), selection/ticker/action-handler classes, and the HUD. `forge/` and `fabric/` only carry entry points, event wiring, and metadata. Cobblemon is a `compileOnly`/`modCompileOnly` dependency; there is no reflection in the gateway.

**Tech Stack:** Java 17, Gradle 8.8, Forge 1.20.1-47.4.4 via `net.neoforged.moddev.legacyforge` 2.0.141, Fabric Loom 1.6, Cobblemon 1.5.2+1.20.1, ControlFlex API 0.9.2.2, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-18-cfx-compat-cobblemon-1.20.1-design.md`

## Global Constraints

- Minecraft `1.20.1`; Forge `47.4.4`; Fabric loader `0.15.0` and API `0.83.0+1.20.1`; Java 17.
- Cobblemon `1.5.2+1.20.1` only, as `compileOnly` (Forge) / `modCompileOnly` (Fabric). Never bundle it.
- ControlFlex `>=0.9.0`, API `0.9.2.2`.
- No reflection in `BattleScreen`; it is the only class importing Cobblemon.
- 18 actions; no `battle_request_*` actions; no `BattleConfigureGUI` compat entry.
- `BattleSlots`, `HudLayout`, and `BattleSelection` must not import Minecraft or Cobblemon.
- Client-only mod; no mixins.
- `libs/` is gitignored and populated by `tools/fetch-cobblemon.sh`.
- Unit tests must run without the Cobblemon jar on the test runtime classpath.
- The `1.21.1` branch is not touched.

---

### Task 1: Build wiring and Cobblemon compile spike

**Files:**
- Create: `tools/fetch-cobblemon.sh`
- Modify: `.gitignore`
- Modify: `gradle.properties`
- Modify: `settings.gradle`
- Create: `forge/build.gradle`
- Create: `forge/src/main/java/com/ifels/cfx/cobblemon/forge/CompileSpike.java`
- Modify: `fabric/build.gradle`
- Create: `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CompileSpike.java`
- Delete: `neoforge/` (whole directory), `tools/build-neoforge.sh`, `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CfxCobblemonFabric.java`

**Interfaces:**
- Produces: a working Forge/Fabric Gradle setup whose `compileJava` sees Cobblemon 1.5.2.
- Produces: `tools/fetch-cobblemon.sh` downloads `libs/Cobblemon-forge-1.5.2+1.20.1.jar` and `libs/Cobblemon-fabric-1.5.2+1.20.1.jar` with SHA-1 verification.

- [ ] **Step 1: Write the fetch script**

Create `tools/fetch-cobblemon.sh`:

```bash
#!/usr/bin/env bash
# Fetch the two Cobblemon 1.5.2 production jars used as compile-only dependencies.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LIBS_DIR="${PROJECT_DIR}/libs"
mkdir -p "${LIBS_DIR}"

FORGE_NAME="Cobblemon-forge-1.5.2+1.20.1.jar"
FABRIC_NAME="Cobblemon-fabric-1.5.2+1.20.1.jar"
FORGE_URL="https://cdn.modrinth.com/data/MdwFAVRL/versions/vm5zUZAg/Cobblemon-forge-1.5.2%2B1.20.1.jar"
FABRIC_URL="https://cdn.modrinth.com/data/MdwFAVRL/versions/EVozVxCq/Cobblemon-fabric-1.5.2%2B1.20.1.jar"
FORGE_SHA1="02601f79f0c442b75463538da7a3c7f260fda42e"
FABRIC_SHA1="2465e767f2659240f1c16549e602cb6e8b9145db"

force=false
if [[ "${1:-}" == "--force" ]]; then force=true; fi

sha1_of() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 1 "$1" | awk '{print $1}'
    else
        sha1sum "$1" | awk '{print $1}'
    fi
}

install_jar() {
    local name="$1" url="$2" sha1="$3" env_var="$4"
    local out="${LIBS_DIR}/${name}"

    if [[ -n "${!env_var:-}" ]]; then
        local source="${!env_var}"
        if [[ ! -f "${source}" ]]; then
            echo "missing ${env_var}: ${source}" >&2
            exit 1
        fi
        cp "${source}" "${out}"
    elif [[ -f "${out}" && "${force}" == false && "$(sha1_of "${out}")" == "${sha1}" ]]; then
        echo "ok ${name}"
        return
    else
        echo "downloading ${name}"
        curl -L --fail --retry 3 --connect-timeout 20 -o "${out}.tmp" "${url}"
        mv "${out}.tmp" "${out}"
    fi

    local got
    got="$(sha1_of "${out}")"
    if [[ "${got}" != "${sha1}" ]]; then
        rm -f "${out}"
        echo "SHA-1 mismatch for ${name}: expected ${sha1}, got ${got}" >&2
        exit 1
    fi
    echo "ok ${name}"
}

install_jar "${FORGE_NAME}" "${FORGE_URL}" "${FORGE_SHA1}" COBBLEMON_FORGE_JAR
install_jar "${FABRIC_NAME}" "${FABRIC_URL}" "${FABRIC_SHA1}" COBBLEMON_FABRIC_JAR
```

Run `chmod +x tools/fetch-cobblemon.sh` and `./tools/fetch-cobblemon.sh`. Expect both files in `libs/` and `ok` lines. The Forge jar is ~104 MB and the Fabric jar ~110 MB; this is a one-time download.

- [ ] **Step 2: Ignore `libs/` and switch the version matrix**

Append to `.gitignore`:

```gitignore
# Cobblemon compile-only jars, fetched by tools/fetch-cobblemon.sh
libs/
```

Replace `gradle.properties` with:

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
# Gradle 8.8 cannot run on a JDK newer than 22. Use a Java 17+ toolchain; the build compiles 17.

# Mod
mod_id=cfx_compat_cobblemon
mod_version=0.1.0
mod_name=Control Flex x Cobblemon
mod_authors=ifels
mod_license=MIT
mod_description=Battle screen action bridge for ControlFlex
maven_group=com.ifels

# Minecraft / loaders
minecraft_version=1.20.1
minecraft_version_range=[1.20.1,1.20.2)
forge_version=47.4.4
forge_version_range=[47,)
fabric_loader_version=0.15.0
fabric_loader_min=>=0.15.0
fabric_api_version=0.83.0+1.20.1
fabric_api_min=>=0.83.0+1.20.1
java_version=17

# Runtime dependency mod ids.
controlflex_mod_id=controlflex
controlflex_version_range=>=0.9.0
controlflex_version_range_forge=[0.9.0,)
cobblemon_mod_id=cobblemon
cobblemon_version_range=>=1.5.2
cobblemon_version_range_forge=[1.5.2,)

# ControlFlex API (plain Java, loader-agnostic), resolved from mavenLocal.
controlflex_api_version=0.9.2.2
```

- [ ] **Step 3: Rewrite `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'FabricMC'
            url = 'https://maven.fabricmc.net/'
        }
        maven {
            name = 'MinecraftForge'
            url = 'https://maven.minecraftforge.net/'
        }
        maven {
            name = 'SpongePowered'
            url = 'https://repo.spongepowered.org/repository/maven-public/'
        }
        maven {
            name = 'NeoForge'
            url = 'https://maven.neoforged.net/releases'
        }
    }

    plugins {
        id 'fabric-loom' version '1.6-SNAPSHOT'
        id 'net.neoforged.moddev.legacyforge' version '2.0.141'
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}

include 'fabric', 'forge'

rootProject.name = 'cfx-compat-cobblemon'
```

- [ ] **Step 4: Create the Forge module with a temporary spike-only source set**

Delete `neoforge/` and `tools/build-neoforge.sh`, then create `forge/build.gradle`:

```groovy
plugins {
    id 'net.neoforged.moddev.legacyforge'
}

base {
    archivesName = 'cfx-compat-cobblemon'
}

version = "${mod_version}-mc${minecraft_version}-forge"
group = maven_group

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java_version as int)
}

legacyForge {
    version = "${minecraft_version}-${forge_version}"

    runs {
        client {
            client()
        }
    }

    mods {
        "${mod_id}" {
            sourceSet sourceSets.main
        }
    }
}

// Task 1 compiles only the spike. Task 2 removes this guard and adds common/.
if (!rootProject.hasProperty('spikeOnly')) {
    sourceSets {
        main {
            java.srcDir(rootProject.file('common/src/main/java'))
            resources.srcDir(rootProject.file('common/src/main/resources'))
        }
        test {
            java.srcDir(rootProject.file('common/src/test/java'))
            compileClasspath += sourceSets.main.output
            runtimeClasspath += sourceSets.main.output
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

def cobblemonJar = rootProject.file('libs/Cobblemon-forge-1.5.2+1.20.1.jar')
if (!cobblemonJar.exists()) {
    throw new GradleException("Missing ${cobblemonJar}. Run ./tools/fetch-cobblemon.sh first.")
}

dependencies {
    compileOnly files(cobblemonJar)
    compileOnly "com.github.ControlFlexMC:control-flex-api:${controlflex_api_version}"
    compileOnly "org.jetbrains.kotlin:kotlin-stdlib:1.9.24"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.slf4j:slf4j-api:2.0.9'
    testImplementation "com.github.ControlFlexMC:control-flex-api:${controlflex_api_version}"
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test', Test).configure {
    useJUnitPlatform()
}

processResources {
    def expandProps = [
        'mod_id'                    : mod_id,
        'mod_name'                  : mod_name,
        'mod_version'               : mod_version,
        'mod_authors'               : mod_authors,
        'mod_license'               : mod_license,
        'mod_description'           : mod_description,
        'minecraft_version_range'   : minecraft_version_range,
        'forge_version_range'       : forge_version_range,
        'controlflex_mod_id'        : controlflex_mod_id,
        'controlflex_version_range_forge': controlflex_version_range_forge,
        'cobblemon_mod_id'          : cobblemon_mod_id,
        'cobblemon_version_range_forge': cobblemon_version_range_forge,
    ]

    filesMatching(['pack.mcmeta', 'META-INF/mods.toml']) {
        expand expandProps
    }

    inputs.properties(expandProps)
}

jar {
    from(rootProject.file('LICENSE')) { rename { "${it}_${mod_id}" } }
}
```

Create `forge/src/main/java/com/ifels/cfx/cobblemon/forge/CompileSpike.java`:

```java
package com.ifels.cfx.cobblemon.forge;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Throwaway compile gate: proves the Cobblemon jar's Minecraft-typed members are usable. */
public final class CompileSpike {

    private CompileSpike() {
    }

    public static void probe(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return;
        }
        if (gui.getCurrentActionSelection() instanceof BattleGeneralActionSelection root) {
            BattleOptionTile tile = root.getTiles().get(0);
            ResourceLocation texture = tile.getResource();
            Component label = tile.getText();
            System.out.println(texture + " " + label.getString());
        }
        screen.mouseClicked(0, 0, 0);
    }

    public static void scroll(BattleMessagePane pane) {
        AbstractSelectionList<?> list = pane;
        list.setScrollAmount(list.getScrollAmount() + 30);
    }
}
```

- [ ] **Step 5: Rewrite the Fabric module with a temporary spike-only source set**

Replace `fabric/build.gradle` with:

```groovy
plugins {
    id 'fabric-loom'
}

base {
    archivesName = 'cfx-compat-cobblemon'
}

version = "${mod_version}-mc${minecraft_version}-fabric"
group = maven_group

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java_version as int)
}

loom {
    runs {
        client {
            client()
            runDir 'run-fabric'
        }
    }
}

// Task 1 compiles only the spike. Task 2 removes this guard and adds common/.
if (!rootProject.hasProperty('spikeOnly')) {
    sourceSets {
        main {
            java.srcDir(rootProject.file('common/src/main/java'))
            resources.srcDir(rootProject.file('common/src/main/resources'))
        }
        test {
            java.srcDir(rootProject.file('common/src/test/java'))
            compileClasspath += sourceSets.main.output
            runtimeClasspath += sourceSets.main.output
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

def cobblemonJar = rootProject.file('libs/Cobblemon-fabric-1.5.2+1.20.1.jar')
if (!cobblemonJar.exists()) {
    throw new GradleException("Missing ${cobblemonJar}. Run ./tools/fetch-cobblemon.sh first.")
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()

    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"

    modCompileOnly files(cobblemonJar)
    compileOnly "com.github.ControlFlexMC:control-flex-api:${controlflex_api_version}"
    compileOnly "org.jetbrains.kotlin:kotlin-stdlib:1.9.24"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.slf4j:slf4j-api:2.0.9'
    testImplementation "com.github.ControlFlexMC:control-flex-api:${controlflex_api_version}"
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test', Test).configure {
    useJUnitPlatform()
}

processResources {
    def expandProps = [
        'mod_id'                   : mod_id,
        'mod_name'                 : mod_name,
        'mod_version'              : mod_version,
        'mod_authors'              : mod_authors,
        'mod_license'              : mod_license,
        'mod_description'          : mod_description,
        'minecraft_version'        : minecraft_version,
        'fabric_loader_version'    : fabric_loader_min,
        'fabric_api_version'       : fabric_api_min,
        'java_version'             : java_version,
        'controlflex_version_range': controlflex_version_range,
        'cobblemon_version_range'  : cobblemon_version_range,
    ]

    filesMatching(['fabric.mod.json']) {
        expand expandProps
    }

    inputs.properties(expandProps)
}

jar {
    from(rootProject.file('LICENSE')) { rename { "${it}_${mod_id}" } }
}
```

Delete the old `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CfxCobblemonFabric.java` (it references the old 1.21.1 classes; the new entry point is Task 4).

Create `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CompileSpike.java`:

```java
package com.ifels.cfx.cobblemon.fabric;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Throwaway compile gate: proves the Cobblemon jar's Minecraft-typed members are usable. */
public final class CompileSpike {

    private CompileSpike() {
    }

    public static void probe(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return;
        }
        if (gui.getCurrentActionSelection() instanceof BattleGeneralActionSelection root) {
            BattleOptionTile tile = root.getTiles().get(0);
            ResourceLocation texture = tile.getResource();
            Component label = tile.getText();
            System.out.println(texture + " " + label.getString());
        }
        screen.mouseClicked(0, 0, 0);
    }

    public static void scroll(BattleMessagePane pane) {
        AbstractSelectionList<?> list = pane;
        list.setScrollAmount(list.getScrollAmount() + 30);
    }
}
```

- [ ] **Step 6: Run the compile spike**

Run:

```bash
./gradlew -PspikeOnly :forge:compileJava :fabric:compileJava
```

Expected: `BUILD SUCCESSFUL`.

If Fabric fails resolving Minecraft types from the Cobblemon jar, Loom is not remapping the file dependency. Fall back in `fabric/build.gradle` to a local Maven repository:

```groovy
repositories {
    maven { url = uri(rootProject.file('libs/maven')) }
}
dependencies {
    modCompileOnly "cfx.local:cobblemon-fabric:1.5.2"
}
```

and create `libs/maven/cfx/local/cobblemon-fabric/1.5.2/` with the jar renamed
`cobblemon-fabric-1.5.2.jar` and a minimal POM. Re-run the spike. Keep whichever form passes.

If Forge fails, check that `forge/build.gradle` uses `legacyforge` (not `moddev`) and that
`minecraft_version`/`forge_version` are `1.20.1`/`47.4.4`.

- [ ] **Step 7: Delete the spikes and commit the wiring**

```bash
rm -f forge/src/main/java/com/ifels/cfx/cobblemon/forge/CompileSpike.java
rm -f fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CompileSpike.java
rmdir forge/src/main/java/com/ifels/cfx/cobblemon/forge fabric/src/main/java/com/ifels/cfx/cobblemon/fabric 2>/dev/null || true
git add .gitignore gradle.properties settings.gradle forge fabric tools/fetch-cobblemon.sh
git add -A neoforge tools/build-neoforge.sh
git commit -m "chore(1.20.1): retarget the build to Forge and pin Cobblemon 1.5.2"
```

Keep the `spikeOnly` guards in both build files for now; Task 2 removes them.

---

### Task 2: `BattleSlots` pure model, tests, and enabling the common source set

**Files:**
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleSlots.java`
- Create: `common/src/test/java/com/ifels/cfx/cobblemon/BattleSlotsTest.java`
- Modify: `common/src/main/java/com/ifels/cfx/cobblemon/BattleActions.java`
- Modify: `common/src/test/java/com/ifels/cfx/cobblemon/BattleActionsTest.java`
- Delete: `BattleScreen.java`, `BattleTicker.java`, `BattleActionHandler.java`, `BattleScreenHandler.java`, `BattleHud.java`, `HudLayout.java`, `CfxCobblemonPlugin.java`
- Delete: `BattleRequestAction.java`, `BattleRequestActions.java`, `BattleRequestHandler.java`, `BattleRequestHud.java`, `BattleRequestScreen.java`, `BattleRequestScreenHandler.java`
- Delete: `BattleRequestActionsTest.java`, `BattleScreenBackArrowTest.java`, `BattleScreenConfirmTest.java`, `HudLayoutTest.java`

**Interfaces:**
- Produces: `BattleSlots.BATTLE_GUI_CLASS`, `BattleSlots.PageKind`, `BattleSlots.Slot`, `BattleSlots.Page`, `rootActionFor(String)`, `moveAction(int)`, `partyAction(int)`, `canGoBack(PageKind)`, `backArrowShown(PageKind)`.
- Produces: `BattleActions.register(IActionRegistrar)` still registers 18 actions, now targeting `BattleSlots.BATTLE_GUI_CLASS`.

- [ ] **Step 1: Remove the old runtime classes and enable `common/`**

Delete the files listed above. In both `forge/build.gradle` and `fabric/build.gradle`, remove the
`if (!rootProject.hasProperty('spikeOnly')) { ... }` wrapper so the `common/` source dirs are always
included.

- [ ] **Step 2: Write the failing `BattleSlotsTest`**

Create `common/src/test/java/com/ifels/cfx/cobblemon/BattleSlotsTest.java`:

```java
package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSlotsTest {

    private static final String FIGHT = "textures/gui/battle/battle_menu_fight.png";
    private static final String SWITCH = "textures/gui/battle/battle_menu_switch.png";
    private static final String BAG = "textures/gui/battle/battle_menu_bag.png";
    private static final String RUN = "textures/gui/battle/battle_menu_run.png";

    @Test
    void rootTilesAreIdentifiedByIconNotPosition() {
        assertEquals(BattleAction.ROOT_FIGHT, BattleSlots.rootActionFor(FIGHT));
        assertEquals(BattleAction.ROOT_SWITCH, BattleSlots.rootActionFor(SWITCH));
        assertEquals(BattleAction.ROOT_CAPTURE, BattleSlots.rootActionFor(BAG));
        assertEquals(BattleAction.ROOT_RUN, BattleSlots.rootActionFor(RUN));
        assertEquals(BattleAction.ROOT_RUN,
                BattleSlots.rootActionFor("textures/gui/battle/battle_menu_forfeit.png"));
        assertNull(BattleSlots.rootActionFor("textures/gui/battle/battle_menu_unknown.png"));
        assertNull(BattleSlots.rootActionFor(null));
    }

    @Test
    void trappedMenuKeepsSemanticMeaningsWhenPositionsShift() {
        // Cobblemon drops the switch tile when trapped; the remaining tiles shift left.
        List<String> tiles = List.of(FIGHT, BAG, RUN);
        assertEquals(List.of(BattleAction.ROOT_FIGHT, BattleAction.ROOT_CAPTURE,
                        BattleAction.ROOT_RUN),
                tiles.stream().map(BattleSlots::rootActionFor).toList());
    }

    @Test
    void disabledMoveKeepsItsOriginalNumber() {
        assertEquals(BattleAction.MOVE_1, BattleSlots.moveAction(0));
        assertEquals(BattleAction.MOVE_2, BattleSlots.moveAction(1));
        assertEquals(BattleAction.MOVE_3, BattleSlots.moveAction(2));
        assertEquals(BattleAction.MOVE_4, BattleSlots.moveAction(3));
    }

    @Test
    void partySlotsAreNumberedInOrder() {
        assertEquals(BattleAction.PARTY_1, BattleSlots.partyAction(0));
        assertEquals(BattleAction.PARTY_6, BattleSlots.partyAction(5));
    }

    @Test
    void backIsAvailableOnEverySubPageButNotTheMenu() {
        assertFalse(BattleSlots.canGoBack(BattleSlots.PageKind.ROOT));
        assertFalse(BattleSlots.canGoBack(BattleSlots.PageKind.OTHER));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.MOVE));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.SWITCH));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.CONFIRM));
    }

    @Test
    void backArrowIsDrawnWhereCobblemonDrawsItsBackButton() {
        assertFalse(BattleSlots.backArrowShown(BattleSlots.PageKind.ROOT));
        assertFalse(BattleSlots.backArrowShown(BattleSlots.PageKind.OTHER));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.MOVE));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.SWITCH));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.CONFIRM));
    }

    @Test
    void confirmSlotHasNoActionOfItsOwn() {
        BattleSlots.Slot slot = new BattleSlots.Slot(null, 115f, 150f, 90f, 26f, 0, 0);
        assertEquals("(confirm button)", slot.displayName());
        assertEquals(160.0, slot.centerX());
        assertEquals(163.0, slot.centerY());
    }

    @Test
    void emptyPageHasNoIdAndNoSlots() {
        BattleSlots.Page page = BattleSlots.Page.empty();
        assertNull(page.id());
        assertEquals(BattleSlots.PageKind.OTHER, page.kind());
        assertTrue(page.slots().isEmpty());
    }

    @Test
    void slotCentreIsTheClickPoint() {
        BattleSlots.Slot slot = new BattleSlots.Slot(BattleAction.MOVE_2, 105f, 500f, 92f, 24f, 0, 0);
        assertEquals(151.0, slot.centerX());
        assertEquals(512.0, slot.centerY());
        assertEquals(BattleAction.MOVE_2.id(), slot.displayName());
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :forge:test --tests com.ifels.cfx.cobblemon.BattleSlotsTest
```

Expected: compilation failure because `BattleSlots` does not exist yet.

- [ ] **Step 4: Implement `BattleSlots`**

Create `common/src/main/java/com/ifels/cfx/cobblemon/BattleSlots.java`:

```java
package com.ifels.cfx.cobblemon;

import java.util.List;

/**
 * Pure page model for Cobblemon 1.5.2's battle screen.
 *
 * <p>No Minecraft or Cobblemon types: the typed adapter in {@link BattleScreen} builds these
 * records, and the HUD, selection state, and tests consume them.</p>
 */
public final class BattleSlots {

    public static final String BATTLE_GUI_CLASS =
            "com.cobblemon.mod.common.client.gui.battle.BattleGUI";

    public enum PageKind {
        ROOT, MOVE, SWITCH, CONFIRM, OTHER
    }

    public record Slot(BattleAction action, float x, float y, float width, float height,
                       int row, int labelWidth) {
        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }

        public String displayName() {
            return action == null ? "(confirm button)" : action.id();
        }
    }

    public record Page(Object id, PageKind kind, List<Slot> slots) {
        public Page {
            slots = List.copyOf(slots);
        }

        public static Page empty() {
            return new Page(null, PageKind.OTHER, List.of());
        }
    }

    private BattleSlots() {
    }

    /** Root tiles are identified by icon texture: a trapped party removes the switch tile. */
    static BattleAction rootActionFor(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.endsWith("battle_menu_fight.png")) {
            return BattleAction.ROOT_FIGHT;
        }
        if (resourcePath.endsWith("battle_menu_switch.png")) {
            return BattleAction.ROOT_SWITCH;
        }
        if (resourcePath.endsWith("battle_menu_bag.png")) {
            return BattleAction.ROOT_CAPTURE;
        }
        if (resourcePath.endsWith("battle_menu_run.png")
                || resourcePath.endsWith("battle_menu_forfeit.png")) {
            return BattleAction.ROOT_RUN;
        }
        return null;
    }

    static BattleAction moveAction(int index) {
        return BattleAction.move(index + 1);
    }

    static BattleAction partyAction(int index) {
        return BattleAction.party(index + 1);
    }

    static boolean canGoBack(PageKind kind) {
        return kind == PageKind.MOVE || kind == PageKind.SWITCH || kind == PageKind.CONFIRM;
    }

    /** 1.5.2 draws its back button on all three sub-pages and never on the action menu. */
    static boolean backArrowShown(PageKind kind) {
        return canGoBack(kind);
    }
}
```

Update `BattleActions.java`:

```java
.targetClass(BattleSlots.BATTLE_GUI_CLASS);
```

(replacing `.targetClass(BattleScreen.GUI_CLASS)`), and `BattleActionsTest.java`:

```java
assertEquals(List.of(BattleSlots.BATTLE_GUI_CLASS), registration.targetClasses());
```

- [ ] **Step 5: Run the tests to verify they pass**

```bash
./gradlew :forge:test :fabric:test
```

Expected: `BUILD SUCCESSFUL`; `BattleSlotsTest`, `BattleActionsTest`, `BattleActionTest`, and
`BattleSelectionTest` pass. The Cobblemon jar is compile-only, so this is evidence the pure model
really is independent of it.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/ifels/cfx/cobblemon/BattleSlots.java \
        common/src/test/java/com/ifels/cfx/cobblemon/BattleSlotsTest.java \
        common/src/main/java/com/ifels/cfx/cobblemon/BattleActions.java \
        common/src/test/java/com/ifels/cfx/cobblemon/BattleActionsTest.java \
        forge/build.gradle fabric/build.gradle
git add -u common/src/main/java common/src/test/java
git commit -m "refactor(bridge): add the pure BattleSlots page model"
```

---

### Task 3: Typed gateway, state, HUD, and layout

**Files:**
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleScreen.java`
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleTicker.java`
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleActionHandler.java`
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleScreenHandler.java`
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/BattleHud.java`
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/HudLayout.java`
- Create: `common/src/test/java/com/ifels/cfx/cobblemon/HudLayoutTest.java`

**Interfaces:**
- Consumes: `BattleSlots.Page`, `PageKind`, `Slot`, `canGoBack`, `backArrowShown`.
- Produces: `BattleScreen.isBattleScreen(Screen)`, `page(Screen)`, `click(Screen, Slot)`, `canGoBack(Screen)`, `back(Screen)`, `scrollLog(Screen, int)`.
- Produces: `BattleScreenHandler` implements `IScreenHandler` and registers against `BattleSlots.BATTLE_GUI_CLASS`.
- Produces: `BattleHud.render(GuiGraphics, Screen)` and `BattleHud.setVisible(boolean)`.

- [ ] **Step 1: Write the failing HUD-layout test**

Create `common/src/test/java/com/ifels/cfx/cobblemon/HudLayoutTest.java`:

```java
package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final BattleSlots.PageKind ROOT = BattleSlots.PageKind.ROOT;
    private static final BattleSlots.PageKind MOVE = BattleSlots.PageKind.MOVE;
    private static final BattleSlots.PageKind SWITCH = BattleSlots.PageKind.SWITCH;
    private static final BattleSlots.PageKind CONFIRM = BattleSlots.PageKind.CONFIRM;

    private static BattleSlots.Slot slot(float x, float y, float w, float h, int row) {
        return slot(x, y, w, h, row, 0);
    }

    private static BattleSlots.Slot slot(float x, float y, float w, float h, int row,
                                         int labelWidth) {
        return new BattleSlots.Slot(BattleAction.ROOT_FIGHT, x, y, w, h, row, labelWidth);
    }

    private static HudLayout.Anchor single(BattleSlots.Slot slot, BattleSlots.PageKind kind,
                                           int badgeWidth) {
        return HudLayout.anchorsFor(List.of(slot), kind, List.of(badgeWidth)).get(0);
    }

    private static List<HudLayout.Anchor> page(List<BattleSlots.Slot> slots,
                                               BattleSlots.PageKind kind, int badgeWidth) {
        return HudLayout.anchorsFor(slots, kind, Collections.nCopies(slots.size(), badgeWidth));
    }

    private static boolean isOutside(HudLayout.Anchor a, float top, float height) {
        return a.y() < top || a.y() >= top + height;
    }

    @Test
    void rootBadgeSitsInsideAfterAShortLabel() {
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 26), ROOT, 10);
        assertEquals(12 + HudLayout.ROOT_TEXT_INSET + 26 + HudLayout.ROOT_LABEL_GAP, a.x());
        assertEquals(400 + HudLayout.ROOT_LABEL_Y, a.y());
        assertTrue(a.x() + 10 <= 12 + 90 - HudLayout.ROOT_ICON_RESERVE);
    }

    @Test
    void oneLongLabelSendsTheWholePageOutside() {
        List<BattleSlots.Slot> slots = List.of(
                slot(12f, 400f, 90f, 26f, 0, 26),
                slot(105f, 400f, 90f, 26f, 0, 62),
                slot(12f, 429f, 90f, 26f, 1, 18),
                slot(105f, 429f, 90f, 26f, 1, 26));
        List<HudLayout.Anchor> anchors = page(slots, ROOT, 26);
        for (int i = 0; i < slots.size(); i++) {
            assertTrue(isOutside(anchors.get(i), slots.get(i).y(), slots.get(i).height()),
                    "slot " + i + " must fall back with the rest of the page");
        }
    }

    @Test
    void rootRowsAlternateForTheOutsideRule() {
        assertEquals(400 - 10 - 2, single(slot(12f, 400f, 90f, 26f, 0), ROOT, 10).y());
        assertEquals(429 + 26 + 2, single(slot(12f, 429f, 90f, 26f, 1), ROOT, 10).y());
    }

    @Test
    void moveBadgeSitsInsideTheBottomLeft() {
        HudLayout.Anchor a = single(slot(20f, 500f, 92f, 24f, 0), MOVE, 10);
        assertEquals(20 + HudLayout.INSET, a.x());
        assertEquals(500 + 24 - 10 - 2, a.y());
        assertTrue(a.x() >= 20 + HudLayout.MOVE_ICON_OVERHANG);
        assertTrue(a.y() + 10 <= 524);
    }

    @Test
    void switchBadgeSitsInsideTheBottomRight() {
        HudLayout.Anchor a = single(slot(12f, 100f, 131f, 40f, 0), SWITCH, 10);
        assertEquals(12 + 131 - 10 - HudLayout.SWITCH_INSET, a.x());
        assertEquals(100 + 40 - 10 - HudLayout.SWITCH_INSET, a.y());
        assertTrue(a.x() + 10 <= 12 + 131);
        assertTrue(a.y() + 10 <= 140);
    }

    @Test
    void confirmBadgeCentresUnderTheSingleButton() {
        BattleSlots.Slot button = new BattleSlots.Slot(null, 115f, 150f, 90f, 26f, 0, 0);
        HudLayout.Anchor a = single(button, CONFIRM, 10);
        assertEquals(115 + (90 - 10) / 2, a.x());
        assertEquals(150 + 26 + HudLayout.CONFIRM_BADGE_DROP, a.y());
    }

    @Test
    void moveHighlightIncludesTheIconThatSticksOutLeft() {
        HudLayout.Rect r = HudLayout.highlightFor(slot(20f, 500f, 92f, 24f, 0), MOVE);
        assertEquals(20 - HudLayout.MOVE_ICON_OVERHANG, r.x());
        assertEquals(20 + 92, r.right());
        assertEquals(92 + HudLayout.MOVE_ICON_OVERHANG, r.width());
    }

    @Test
    void rootAndSwitchHighlightsAreExactlyTheTileRect() {
        HudLayout.Rect root = HudLayout.highlightFor(slot(12f, 400f, 90f, 26f, 0), ROOT);
        assertEquals(12, root.x());
        assertEquals(90, root.width());
        HudLayout.Rect swap = HudLayout.highlightFor(slot(12f, 100f, 131f, 40f, 0), SWITCH);
        assertEquals(131, swap.width());
        assertEquals(40, swap.height());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :forge:test --tests com.ifels.cfx.cobblemon.HudLayoutTest
```

Expected: compilation failure because the new `HudLayout` API (`SWITCH_INSET`, accepted
`BattleSlots.Slot`) does not exist yet.

- [ ] **Step 3: Implement `HudLayout`**

Create `common/src/main/java/com/ifels/cfx/cobblemon/HudLayout.java`:

```java
package com.ifels.cfx.cobblemon;

import java.util.ArrayList;
import java.util.List;

/** Pure placement rules for the HUD badges and the selection ring. */
public final class HudLayout {

    public static final int GLYPH_SIZE = 10;
    public static final int GAP = 2;

    public static final int MOVE_ICON_OVERHANG = 9;
    public static final int INSET = 12;

    public static final int ROOT_TEXT_INSET = 6;
    public static final int ROOT_LABEL_Y = 8;
    public static final int ROOT_ICON_RESERVE = 20;
    public static final int ROOT_LABEL_GAP = 4;

    public static final int CONFIRM_BADGE_DROP = 10;
    public static final int SWITCH_INSET = 2;

    private HudLayout() {
    }

    public record Anchor(int x, int y) {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    public static List<Anchor> anchorsFor(List<BattleSlots.Slot> slots,
                                          BattleSlots.PageKind kind,
                                          List<Integer> badgeWidths) {
        List<Anchor> anchors = new ArrayList<>(slots.size());
        boolean inside = kind == BattleSlots.PageKind.ROOT && allRootBadgesFit(slots, badgeWidths);
        for (int i = 0; i < slots.size(); i++) {
            anchors.add(anchorFor(slots.get(i), kind, width(badgeWidths, i), inside));
        }
        return anchors;
    }

    private static boolean allRootBadgesFit(List<BattleSlots.Slot> slots,
                                            List<Integer> badgeWidths) {
        for (int i = 0; i < slots.size(); i++) {
            if (!rootFits(slots.get(i), width(badgeWidths, i))) {
                return false;
            }
        }
        return true;
    }

    private static int width(List<Integer> badgeWidths, int index) {
        return index < badgeWidths.size() ? badgeWidths.get(index) : GLYPH_SIZE;
    }

    private static Anchor anchorFor(BattleSlots.Slot slot, BattleSlots.PageKind kind,
                                    int badgeWidth, boolean rootInsideAllowed) {
        if (kind == BattleSlots.PageKind.MOVE) {
            int x = (int) slot.x() + INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - GAP;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.SWITCH) {
            int x = (int) (slot.x() + slot.width()) - badgeWidth - SWITCH_INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - SWITCH_INSET;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.CONFIRM) {
            int x = (int) slot.x() + ((int) slot.width() - badgeWidth) / 2;
            int y = (int) (slot.y() + slot.height()) + CONFIRM_BADGE_DROP;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.ROOT && rootInsideAllowed) {
            return new Anchor(rootInsideX(slot), (int) slot.y() + ROOT_LABEL_Y);
        }
        int x = (int) slot.x();
        int y = slot.row() % 2 == 0
                ? (int) slot.y() - GLYPH_SIZE - GAP
                : (int) (slot.y() + slot.height()) + GAP;
        return new Anchor(x, y);
    }

    private static boolean rootFits(BattleSlots.Slot slot, int badgeWidth) {
        if (slot.labelWidth() <= 0) {
            return false;
        }
        return rootInsideX(slot) + badgeWidth
                <= (int) slot.x() + (int) slot.width() - ROOT_ICON_RESERVE;
    }

    private static int rootInsideX(BattleSlots.Slot slot) {
        return (int) slot.x() + ROOT_TEXT_INSET + slot.labelWidth() + ROOT_LABEL_GAP;
    }

    public static Rect highlightFor(BattleSlots.Slot slot, BattleSlots.PageKind kind) {
        int x = (int) slot.x();
        int y = (int) slot.y();
        int width = (int) slot.width();
        int height = (int) slot.height();
        if (kind == BattleSlots.PageKind.MOVE) {
            x -= MOVE_ICON_OVERHANG;
            width += MOVE_ICON_OVERHANG;
        }
        return new Rect(x, y, width, height);
    }
}
```

- [ ] **Step 4: Run the layout test to verify it passes**

```bash
./gradlew :forge:test --tests com.ifels.cfx.cobblemon.HudLayoutTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Implement `BattleScreen`**

Create `common/src/main/java/com/ifels/cfx/cobblemon/BattleScreen.java` with the code from the
plan's Appendix A. Key points, already fixed by the spec review:

- `screen instanceof BattleGUI`; `getCurrentActionSelection()`.
- Typed `instanceof` per page; `getTiles()`, `getMoveTiles()`, `getForfeitButton()`.
- `slot()` sizes from `BattleOptionTile.OPTION_WIDTH/HEIGHT`,
  `BattleMoveSelection.MOVE_WIDTH/MOVE_HEIGHT`, `BattleOverlay.TILE_WIDTH/TILE_HEIGHT`.
- Root label width: `Minecraft.getInstance().font.width(tile.getText())`.
- Click: `screen.mouseClicked(slot.centerX(), slot.centerY(), 0)`.
- Back: `gui.changeActionSelection(null)`.
- Scroll: `AbstractSelectionList<?> list = pane; list.setScrollAmount(list.getScrollAmount()
  + lines * BattleMessagePane.LINE_HEIGHT)`.
- `RuntimeException` guard and one warning.

- [ ] **Step 6: Implement `BattleTicker`, `BattleActionHandler`, `BattleScreenHandler`**

Use Appendix B, C, and D. They only consume `BattleSlots.Page`/`Slot`; `BattleScreenHandler`
must not import any Cobblemon class.

- [ ] **Step 7: Implement `BattleHud`**

Copy the 1.21.1 `BattleHud.java` and apply these exact changes:

1. Replace imports of `BattleScreen.Slot`/`BattleScreen.PageKind` with
   `BattleSlots.Slot`/`BattleSlots.PageKind`.
2. In `render`, replace the `currentSelection`/`slots(selection)`/`kindOf(selection)` block with:
   ```java
   BattleSlots.Page page = BattleScreen.page(screen);
   List<BattleSlots.Slot> slots = page.slots();
   if (slots.isEmpty()) {
       return;
   }
   BattleSlots.PageKind kind = page.kind();
   ```
   and pass `kind`/`page` where the old code passed `selection`.
3. `drawHudToggleHint(..., BattleSlots.backArrowShown(kind))`.
4. `BattleSelection.INSTANCE.sync(page.id(), slots.size(), kind == BattleSlots.PageKind.ROOT)`.
5. `drawHighlight(graphics, rect, page.id(), selected)` (the page field is an `Object`).
6. In `drawGlyphRow`, replace `ResourceLocation.parse(glyph.getTextureId())` with
   `new ResourceLocation(glyph.getTextureId())`.
7. Keep `HUD_TOGGLE_ACTION = "hud_toggle"` and `CONFIRM_ACTION = "gui_press"`.
8. Keep `diagnoseOnce`, `resolveForSlot`, `rowWidth`, colors, and the toggle/nav hints, but change
   every remaining `BattleScreen.Slot`/`BattleScreen.PageKind` (including the `badged` list, the
   `badgeGlyphs`/`badgeWidths` parallel lists, `diagnoseOnce`, and `resolveForSlot`) to
   `BattleSlots.Slot`/`BattleSlots.PageKind`. There must be no `BattleScreen.Slot` reference left
   in `BattleHud`.
9. Remove the `import com.ifels.cfx.cobblemon.BattleScreen.Slot` / `.PageKind` style imports; the
   new types are top-level in `BattleSlots`.

- [ ] **Step 8: Compile and run all tests**

```bash
./gradlew :forge:test :fabric:test
```

Expected: `BUILD SUCCESSFUL`. This compiles `BattleScreen` against the real Cobblemon jar and runs
the pure tests without loading it.

- [ ] **Step 9: Commit**

```bash
git add common/src/main/java/com/ifels/cfx/cobblemon \
        common/src/test/java/com/ifels/cfx/cobblemon
git commit -m "refactor(bridge): replace the reflection gateway with a typed BattleGUI adapter"
```

---

### Task 4: Plugin, resources, loader entry points, metadata, and deploy scripts

**Files:**
- Create: `common/src/main/java/com/ifels/cfx/cobblemon/CfxCobblemonPlugin.java`
- Modify: `common/src/main/resources/assets/cfx_compat_cobblemon/compat/cfx_compat_cobblemon.json`
- Modify: `common/src/main/resources/assets/cfx_compat_cobblemon/lang/en_us.json`
- Modify: `common/src/main/resources/assets/cfx_compat_cobblemon/lang/zh_cn.json`
- Create: `forge/src/main/java/com/ifels/cfx/cobblemon/forge/CfxCobblemonForge.java`
- Create: `forge/src/main/resources/META-INF/mods.toml`
- Create: `forge/src/main/resources/pack.mcmeta`
- Create: `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CfxCobblemonFabric.java`
- Modify: `fabric/src/main/resources/fabric.mod.json`
- Create: `tools/build-forge.sh`
- Modify: `tools/build-fabric.sh`

**Interfaces:**
- Consumes: `BattleActions`, `BattleScreenHandler`, `BattleActionHandler`, `BattleTicker`, `BattleHud`.
- Produces: a Forge `@Mod` and a Fabric `ClientModInitializer`, both wired to the ticker and HUD.
- Produces: compat JSON with `BattleGUI` and the wheel radius 49; no `BattleConfigureGUI`.

- [ ] **Step 1: Write the compatibility plugin**

Create `CfxCobblemonPlugin.java` (no request imports):

```java
package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionTrigger;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.ICompatAssetInstaller;
import com.ifels.controlflex.api.IControlFlexPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CfxCobblemonPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    public static final String COMPAT_FILE_NAME = "cfx_compat_cobblemon.json";
    public static final String COMPAT_RESOURCE =
            "/assets/cfx_compat_cobblemon/compat/" + COMPAT_FILE_NAME;

    @Override
    public String getModId() {
        return BattleAction.MOD_ID;
    }

    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        if (!installer.install(COMPAT_RESOURCE, COMPAT_FILE_NAME)) {
            LOGGER.warn("[cfx-compat-cobblemon] failed to install {} — BattleGUI will not be listed "
                    + "under Cobblemon → Screen 适配", COMPAT_FILE_NAME);
        }
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
        BattleActions.register(registrar);
        LOGGER.info("[cfx-compat-cobblemon] declared {} battle actions",
                BattleAction.values().length);
    }

    @Override
    public void onActionTriggered(ActionTrigger trigger) {
        BattleAction action = BattleActions.byId(trigger.actionId());
        if (action != null) {
            BattleActionHandler.handle(action);
        }
    }

    @Override
    public void onControlFlexReady() {
        LOGGER.info("[cfx-compat-cobblemon] ControlFlex ready; {} battle actions available",
                BattleAction.values().length);
        BattleScreenHandler.register();
    }
}
```

- [ ] **Step 2: Update the compat JSON**

Replace `common/src/main/resources/assets/cfx_compat_cobblemon/compat/cfx_compat_cobblemon.json`:

```json
{
  "mod_id": "cobblemon",
  "mod_name": "Cobblemon",
  "home_page": "https://modrinth.com/mod/cobblemon",
  "config_version": "1.0.0",
  "mod_versions": [
    {
      "version": "",
      "screen": {
        "com.cobblemon.mod.common.client.gui.battle.BattleGUI": {},
        "com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelGUI": {
          "rightStick": {
            "behavior": "RADIAL_PATH",
            "threshold": 0.5,
            "cursorRadius": 49
          }
        }
      }
    }
  ]
}
```

- [ ] **Step 3: Remove the request-screen language keys**

In both `en_us.json` and `zh_cn.json`, delete the five keys whose names start with
`key.cfx_compat_cobblemon.battle_request_` (`prev`, `next`, `level_prev`, `level_next`, `confirm`).
Keep the 18 battle actions and `cfx_compat_cobblemon.hud.*`.

- [ ] **Step 4: Add the Forge entry point**

Create `forge/src/main/java/com/ifels/cfx/cobblemon/forge/CfxCobblemonForge.java`:

```java
package com.ifels.cfx.cobblemon.forge;

import com.ifels.cfx.cobblemon.BattleHud;
import com.ifels.cfx.cobblemon.BattleTicker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(CfxCobblemonForge.MOD_ID)
@Mod.EventBusSubscriber(modid = CfxCobblemonForge.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CfxCobblemonForge {

    public static final String MOD_ID = "cfx_compat_cobblemon";

    public CfxCobblemonForge() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BattleTicker.tick();
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        BattleHud.render(event.getGuiGraphics(), event.getScreen());
    }
}
```

- [ ] **Step 5: Add the Forge metadata**

Create `forge/src/main/resources/META-INF/mods.toml`:

```toml
modLoader = "javafml"
loaderVersion = "${forge_version_range}"
license = "${mod_license}"
issueTrackerURL = "https://github.com/ControlFlexMC/cfx-compat-cobblemon/issues"

[[mods]]
modId = "${mod_id}"
version = "${mod_version}"
displayName = "${mod_name}"
authors = "${mod_authors}"
description = '''${mod_description}'''
side = "CLIENT"
displayTest = "IGNORE_ALL_VERSION"

[[dependencies.${mod_id}]]
modId = "forge"
mandatory = true
versionRange = "${forge_version_range}"
ordering = "NONE"
side = "CLIENT"

[[dependencies.${mod_id}]]
modId = "minecraft"
mandatory = true
versionRange = "${minecraft_version_range}"
ordering = "NONE"
side = "CLIENT"

[[dependencies.${mod_id}]]
modId = "${controlflex_mod_id}"
mandatory = true
versionRange = "${controlflex_version_range_forge}"
ordering = "AFTER"
side = "CLIENT"

[[dependencies.${mod_id}]]
modId = "${cobblemon_mod_id}"
mandatory = true
versionRange = "${cobblemon_version_range_forge}"
ordering = "AFTER"
side = "CLIENT"
```

Create `forge/src/main/resources/pack.mcmeta`:

```json
{
  "pack": {
    "description": "cfx-compat-cobblemon",
    "pack_format": 15
  }
}
```

- [ ] **Step 6: Add the Fabric entry point and metadata**

Create `fabric/src/main/java/com/ifels/cfx/cobblemon/fabric/CfxCobblemonFabric.java`:

```java
package com.ifels.cfx.cobblemon.fabric;

import com.ifels.cfx.cobblemon.BattleHud;
import com.ifels.cfx.cobblemon.BattleTicker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public final class CfxCobblemonFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> BattleTicker.tick());
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register(
                        (rendered, graphics, mouseX, mouseY, tickDelta) ->
                                BattleHud.render(graphics, rendered)));
    }
}
```

Replace `fabric/src/main/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "${mod_id}",
  "version": "${mod_version}",
  "name": "${mod_name}",
  "description": "${mod_description}",
  "authors": ["${mod_authors}"],
  "license": "${mod_license}",
  "environment": "client",
  "entrypoints": {
    "client": ["com.ifels.cfx.cobblemon.fabric.CfxCobblemonFabric"]
  },
  "depends": {
    "fabricloader": "${fabric_loader_version}",
    "fabric-api": "${fabric_api_version}",
    "minecraft": "${minecraft_version}",
    "java": ">=${java_version}",
    "controlflex": "${controlflex_version_range}",
    "cobblemon": "${cobblemon_version_range}"
  }
}
```

- [ ] **Step 7: Add the deploy scripts**

Create `tools/build-forge.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

"${PROJECT_DIR}/tools/fetch-cobblemon.sh"

"${PROJECT_DIR}/gradlew" :forge:build "$@"

JAR="$(ls -t "${PROJECT_DIR}"/forge/build/libs/cfx-compat-cobblemon-*-forge.jar | head -1)"

INSTANCE="${MC_1_20_1_FORGE_PATH:-/Users/ifels/.prism/instances/cobblemon-1.20.1-forge/minecraft}"
MODS="${INSTANCE}/mods"
mkdir -p "${MODS}"
rm -f "${MODS}"/cfx-compat-cobblemon-*-forge.jar
cp "${JAR}" "${MODS}/"
if [[ -d "${INSTANCE}/logs" ]]; then
    rm -rf "${INSTANCE:?}/logs/"*
fi
echo "deployed $(basename "${JAR}") to ${MODS}"
```

Create `tools/build-fabric.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

"${PROJECT_DIR}/tools/fetch-cobblemon.sh"

"${PROJECT_DIR}/gradlew" :fabric:build "$@"

JAR="$(ls -t "${PROJECT_DIR}"/fabric/build/libs/cfx-compat-cobblemon-*-fabric.jar | head -1)"

INSTANCE="${MC_1_20_1_FABRIC_PATH:-}"
if [[ -z "${INSTANCE}" ]]; then
    echo "MC_1_20_1_FABRIC_PATH is not set; artifact left at ${JAR}"
    exit 0
fi

MODS="${INSTANCE}/mods"
mkdir -p "${MODS}"
rm -f "${MODS}"/cfx-compat-cobblemon-*-fabric.jar
cp "${JAR}" "${MODS}/"
if [[ -d "${INSTANCE}/logs" ]]; then
    rm -rf "${INSTANCE:?}/logs/"*
fi
echo "deployed $(basename "${JAR}") to ${MODS}"
```

Mark both executable.

- [ ] **Step 8: Build both jars and run all tests**

```bash
./gradlew :forge:build :fabric:build
./gradlew :forge:test :fabric:test
```

Expected: both builds succeed; both tests pass. Check the jars:

```bash
for jar in forge/build/libs/*-forge.jar fabric/build/libs/*-fabric.jar; do
  echo "== ${jar}"
  unzip -l "${jar}" | grep -c 'com/cobblemon/' || true
  unzip -l "${jar}" | grep -E 'mods.toml|fabric.mod.json|pack.mcmeta' || true
done
```

Expected: zero `com/cobblemon/` entries; each jar has its metadata and `pack.mcmeta` (Forge) or
`fabric.mod.json` (Fabric).

- [ ] **Step 9: Commit**

```bash
git add common/src/main/java/com/ifels/cfx/cobblemon/CfxCobblemonPlugin.java \
        common/src/main/resources \
        forge/src/main/java forge/src/main/resources \
        fabric/src/main/java fabric/src/main/resources \
        tools/build-forge.sh tools/build-fabric.sh
git commit -m "feat(1.20.1): wire the Forge and Fabric entry points and drop the request screen"
```

---

### Task 5: Forge deploy, load verification, and manual acceptance

**Files:**
- No source changes unless acceptance finds a defect.

**Interfaces:**
- Consumes: the Forge jar from Task 4.
- Produces: a deployed jar in the Prism instance and a verified load log.

- [ ] **Step 1: Deploy**

```bash
./tools/build-forge.sh
```

Expected: `cfx-compat-cobblemon-0.1.0-mc1.20.1-forge.jar` in the instance's `mods/`, logs cleared.

- [ ] **Step 2: Launch and inspect the load**

Launch the instance (Prism or the instance's launcher). Wait for the title screen, then inspect:

```bash
grep -E "cfx-compat-cobblemon|cfx_compat_cobblemon|ControlFlex" \
  /Users/ifels/.prism/instances/cobblemon-1.20.1-forge/minecraft/logs/latest.log | tail -40
```

Expected: the mod loads with Cobblemon 1.5.2 and ControlFlex 0.9.0.603; `declared 18 battle
actions` and `screen handler ... registered=true` appear. No `NoSuchMethodError`,
`NoClassDefFoundError`, or missing-dependency message.

- [ ] **Step 3: Run the in-game scenarios**

Perform the §13.3 checklist from the spec:

1. Wild battle: bind Fight/Switch/Capture/Run, press each; A activates the highlighted tile.
2. Move page: four move actions; a disabled move is skipped; B returns to the root; LB/RB moves the
   highlight and the cursor.
3. Trainer battle: Forfeit opens the confirmation; A forfeits; B cancels.
4. Forced switch: party actions select the intended Pokémon.
5. Log scroll up/down by three rows.
6. `hud_toggle` hides/shows the badges, ring, and hints; the state survives a new battle.
7. Interaction wheel: right stick parks on each of the four sectors.

Record any failure with the log line and the page; fix in Task 4/3 files and repeat.

- [ ] **Step 4: Commit any acceptance fix**

```bash
git add -A
git commit -m "fix(1.20.1): correct layout or input behaviour found in acceptance"
```

Only if a fix was needed.

---

### Task 6: Fabric verification, README, and final review

**Files:**
- Modify: `README.md`
- Modify: `common/src/main/resources/assets/cfx_compat_cobblemon/lang/*.json` (only if a key is missing)
- No other source changes.

- [ ] **Step 1: Build and test Fabric**

```bash
./gradlew :fabric:build :fabric:test
```

Expected: `BUILD SUCCESSFUL`; the Fabric jar has no Cobblemon classes. Runtime is not claimed:
there is no Fabric test instance and the ControlFlex 0.9.0 Fabric jar is not built locally.

- [ ] **Step 2: Rewrite `README.md`**

Cover, in this order:

- What the bridge does on 1.20.1 and the compile dependency on Cobblemon 1.5.2 (the final 1.20.1
  release), with the trade-off stated.
- Requirements: MC 1.20.1, Forge 47.4.4 or Fabric, Cobblemon 1.5.2, ControlFlex >=0.9.0.
- The 18-action table, defaults, and the confirm-page `gui_press` flow.
- The wheel `cursorRadius = 49` and how to tune it.
- Build: `./tools/fetch-cobblemon.sh`, `./tools/build-forge.sh`,
  `./tools/build-fabric.sh`; note `libs/` is gitignored.
- Differences from 1.21.1: no request screen, no reflection, 18 vs 23 actions, same ids for the
  shared actions.
- Out of scope: gimmick buttons, spectator back, Cobblemon 1.6+.
- Project layout: `common/`, `forge/`, `fabric/`, `tools/`.

- [ ] **Step 3: Final diff review**

```bash
git status --short
git diff --stat HEAD~6..HEAD
git diff HEAD~1 -- README.md
```

Check: no `neoforge/` references remain, no `BattleRequest` references remain, no `BattleScreen.GUI_CLASS`
references remain, no `ResourceLocation.parse` remains, no 1.21.1 version strings remain.

- [ ] **Step 4: Commit the README**

```bash
git add README.md
git commit -m "docs: describe the 1.20.1 bridge and its build"
```

---

## Appendix A: `BattleScreen.java`

```java
package com.ifels.cfx.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection.MoveTile;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection.SwitchTile;
import com.cobblemon.mod.common.client.gui.battle.subscreen.ForfeitConfirmationSelection;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed adapter to Cobblemon 1.5.2's battle screen. The only class importing Cobblemon.
 */
public final class BattleScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static boolean loggedFailure;

    private BattleScreen() {
    }

    public static boolean isBattleScreen(Screen screen) {
        return screen instanceof BattleGUI;
    }

    public static Page page(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return Page.empty();
        }
        try {
            BattleActionSelection selection = gui.getCurrentActionSelection();
            if (selection == null) {
                return Page.empty();
            }
            if (selection instanceof BattleGeneralActionSelection root) {
                return rootPage(selection, root);
            }
            if (selection instanceof BattleMoveSelection move) {
                return movePage(selection, move);
            }
            if (selection instanceof BattleSwitchPokemonSelection swap) {
                return switchPage(selection, swap);
            }
            if (selection instanceof ForfeitConfirmationSelection confirm) {
                return confirmPage(selection, confirm);
            }
            return new Page(selection, PageKind.OTHER, List.of());
        } catch (RuntimeException e) {
            logFailure("read page", e);
            return Page.empty();
        }
    }

    public static boolean click(Screen screen, Slot slot) {
        if (!(screen instanceof BattleGUI) || slot == null) {
            return false;
        }
        try {
            return screen.mouseClicked(slot.centerX(), slot.centerY(), 0);
        } catch (RuntimeException e) {
            logFailure("click " + slot.displayName(), e);
            return false;
        }
    }

    public static boolean canGoBack(Screen screen) {
        Page page = page(screen);
        return page.id() != null && BattleSlots.canGoBack(page.kind());
    }

    public static boolean back(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return false;
        }
        Page page = page(screen);
        if (page.id() == null || !BattleSlots.canGoBack(page.kind())) {
            return false;
        }
        try {
            gui.changeActionSelection(null);
            return true;
        } catch (RuntimeException e) {
            logFailure("back", e);
            return false;
        }
    }

    public static void scrollLog(Screen screen, int lines) {
        if (screen == null || lines == 0) {
            return;
        }
        try {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof BattleMessagePane pane) {
                    AbstractSelectionList<?> list = pane;
                    list.setScrollAmount(list.getScrollAmount()
                            + (double) lines * BattleMessagePane.LINE_HEIGHT);
                    return;
                }
            }
        } catch (RuntimeException e) {
            logFailure("scroll log", e);
        }
    }

    private static Page rootPage(Object id, BattleGeneralActionSelection root) {
        List<BattleOptionTile> tiles = root.getTiles();
        List<Slot> slots = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size(); i++) {
            BattleOptionTile tile = tiles.get(i);
            BattleAction action = BattleSlots.rootActionFor(tile.getResource().getPath());
            if (action == null) {
                continue;
            }
            slots.add(new Slot(action, tile.getX(), tile.getY(),
                    BattleOptionTile.OPTION_WIDTH, BattleOptionTile.OPTION_HEIGHT,
                    i / 2, labelWidth(tile)));
        }
        return new Page(id, PageKind.ROOT, slots);
    }

    private static Page movePage(Object id, BattleMoveSelection move) {
        List<MoveTile> tiles = move.getMoveTiles();
        List<Slot> slots = new ArrayList<>(4);
        for (int i = 0; i < tiles.size() && i < 4; i++) {
            MoveTile tile = tiles.get(i);
            if (!tile.getSelectable()) {
                continue;
            }
            slots.add(new Slot(BattleSlots.moveAction(i), tile.getX(), tile.getY(),
                    BattleMoveSelection.MOVE_WIDTH, BattleMoveSelection.MOVE_HEIGHT,
                    i / 2, 0));
        }
        return new Page(id, PageKind.MOVE, slots);
    }

    private static Page switchPage(Object id, BattleSwitchPokemonSelection swap) {
        List<SwitchTile> tiles = swap.getTiles();
        List<Slot> slots = new ArrayList<>(6);
        for (int i = 0; i < tiles.size() && i < 6; i++) {
            SwitchTile tile = tiles.get(i);
            slots.add(new Slot(BattleSlots.partyAction(i), tile.getX(), tile.getY(),
                    BattleOverlay.TILE_WIDTH, BattleOverlay.TILE_HEIGHT, i / 2, 0));
        }
        return new Page(id, PageKind.SWITCH, slots);
    }

    private static Page confirmPage(Object id, ForfeitConfirmationSelection confirm) {
        BattleOptionTile button = confirm.getForfeitButton();
        if (button == null) {
            return Page.empty();
        }
        return new Page(id, PageKind.CONFIRM, List.of(new Slot(null,
                button.getX(), button.getY(),
                BattleOptionTile.OPTION_WIDTH, BattleOptionTile.OPTION_HEIGHT, 0, 0)));
    }

    private static int labelWidth(BattleOptionTile tile) {
        return Minecraft.getInstance().font.width(tile.getText());
    }

    private static void logFailure(String what, RuntimeException e) {
        if (loggedFailure) {
            return;
        }
        loggedFailure = true;
        LOGGER.warn("[cfx-compat-cobblemon] battle gateway failed to {}; "
                + "actions stay unavailable until restart", what, e);
    }
}
```

## Appendix B: `BattleTicker.java`

```java
package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BattleTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static Object parkedOn;

    private BattleTicker() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (!BattleScreen.isBattleScreen(screen)) {
            BattleSelection.INSTANCE.reset();
            parkedOn = null;
            return;
        }

        Page page = BattleScreen.page(screen);
        if (page.id() == null) {
            parkedOn = null;
            return;
        }
        if (page.slots().isEmpty()) {
            BattleSelection.INSTANCE.sync(page.id(), 0, page.kind() == PageKind.ROOT);
            parkedOn = null;
            return;
        }

        int index = BattleSelection.INSTANCE.sync(page.id(), page.slots().size(),
                page.kind() == PageKind.ROOT);

        if (page.id() != parkedOn) {
            parkedOn = page.id();
            warp(page.slots().get(index));
            LOGGER.info("[cfx-compat-cobblemon] page opened: cursor parked on {} ({}/{})",
                    page.slots().get(index).displayName(), index + 1, page.slots().size());
        }
    }

    private static void warp(BattleSlots.Slot slot) {
        try {
            ICursorController cursor = ControlFlexApi.getCursorController();
            if (cursor != null) {
                cursor.warpToGui(slot.centerX(), slot.centerY());
            }
        } catch (Throwable ignored) {
            // older ControlFlex: no cursor API
        }
    }
}
```

## Appendix C: `BattleActionHandler.java`

```java
package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BattleActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static final int LOG_LINES_PER_PRESS = 3;

    private BattleActionHandler() {
    }

    public static void handle(BattleAction action) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (!BattleScreen.isBattleScreen(screen)) {
            return;
        }

        if (action == BattleAction.LOG_UP) {
            BattleScreen.scrollLog(screen, -LOG_LINES_PER_PRESS);
            return;
        }
        if (action == BattleAction.LOG_DOWN) {
            BattleScreen.scrollLog(screen, LOG_LINES_PER_PRESS);
            return;
        }

        Page page = BattleScreen.page(screen);
        if (page.slots().isEmpty()) {
            return;
        }

        BattleSelection state = BattleSelection.INSTANCE;
        state.sync(page.id(), page.slots().size(), page.kind() == PageKind.ROOT);

        if (action == BattleAction.PREV || action == BattleAction.NEXT) {
            int moved = state.move(action == BattleAction.PREV ? -1 : 1, page.slots().size());
            Slot slot = page.slots().get(moved);
            warp(slot);
            LOGGER.info("[cfx-compat-cobblemon] highlight -> {} ({}/{})",
                    slot.displayName(), moved + 1, page.slots().size());
            return;
        }

        for (int i = 0; i < page.slots().size(); i++) {
            Slot slot = page.slots().get(i);
            if (slot.action() != action) {
                continue;
            }
            state.select(i);
            boolean sent = BattleScreen.click(screen, slot);
            LOGGER.info("[cfx-compat-cobblemon] {} at ({}, {}) -> sent={}",
                    slot.displayName(), (int) slot.centerX(), (int) slot.centerY(), sent);
            return;
        }
    }

    private static void warp(Slot slot) {
        try {
            ICursorController cursor = ControlFlexApi.getCursorController();
            if (cursor != null) {
                cursor.warpToGui(slot.centerX(), slot.centerY());
            }
        } catch (Throwable ignored) {
            // older ControlFlex: no cursor API
        }
    }
}
```

Note: `state.move(...)` already updates the index; no extra `state.select(...)` call is needed.

## Appendix D: `BattleScreenHandler.java`

```java
package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.HudStateReason;
import com.ifels.controlflex.api.IScreenHandler;
import com.ifels.controlflex.api.IScreenHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BattleScreenHandler implements IScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static boolean done;

    private BattleScreenHandler() {
    }

    public static void register() {
        if (done) {
            return;
        }
        try {
            IScreenHandlerRegistry registry = ControlFlexApi.getScreenHandlerRegistry();
            if (registry == null) {
                LOGGER.info("[cfx-compat-cobblemon] screen handler registry unavailable");
                return;
            }
            done = registry.register(BattleSlots.BATTLE_GUI_CLASS, new BattleScreenHandler());
            LOGGER.info("[cfx-compat-cobblemon] screen handler for {} registered={}",
                    BattleSlots.BATTLE_GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register screen handler: {}", t.toString());
        }
    }

    @Override
    public boolean onBack() {
        Screen screen = Minecraft.getInstance().screen;
        if (!BattleScreen.isBattleScreen(screen) || !BattleScreen.canGoBack(screen)) {
            return false;
        }
        boolean switched = BattleScreen.back(screen);
        if (switched) {
            BattleSelection.INSTANCE.markReturnToMenu();
        }
        LOGGER.info("[cfx-compat-cobblemon] B -> {}", switched ? "previous page" : "no change");
        return switched;
    }

    @Override
    public boolean ownsHud() {
        return true;
    }

    @Override
    public boolean onHudState(boolean visible, HudStateReason reason) {
        BattleHud.setVisible(visible);
        return true;
    }
}
```

## Self-review notes

- Spec §5 is covered by Task 1; §6 by Task 4; §7–§9 by Tasks 2–3; §10–§11 by Tasks 3–4; §13 by
  Tasks 2–6; §14 by Tasks 1–4 and 6.
- `BattleHud` is ported from the 1.21.1 file with the eight edits in Task 3, Step 7; it is the only
  file whose diff is described rather than copied, because it is 363 lines and unchanged except
  for those edits.
- All type names used in later tasks match Appendix A–D: `BattleSlots.Page.id()`, `.kind()`,
  `.slots()`, `Slot.centerX()`, `.centerY()`, `.displayName()`, `BattleScreen.page(Screen)`,
  `.click(Screen, Slot)`, `.canGoBack(Screen)`, `.back(Screen)`, `.scrollLog(Screen, int)`.
- Task 5 is manual; if the environment cannot open the game, report the build/log evidence and the
  unverified scenarios instead of claiming them.
