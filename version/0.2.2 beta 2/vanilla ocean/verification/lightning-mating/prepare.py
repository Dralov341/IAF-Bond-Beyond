from pathlib import Path
import shutil,sys
root=Path(__file__).resolve().parents[2]
here=Path(__file__).resolve().parent
out=Path(sys.argv[1]).resolve()
if out==root or root in out.parents: raise SystemExit("Choose a test directory outside the source release.")
if out.exists(): raise SystemExit("Test directory already exists; choose a fresh empty path.")
out.mkdir(parents=True)
for n in ["src","gradle"]:shutil.copytree(root/n,out/n)
for n in ["build.gradle","settings.gradle","gradle.properties","gradlew","gradlew.bat"]:shutil.copyfile(root/n,out/n)
(out/"gradlew").chmod(0o755)
shutil.copytree(here/"gametest",out/"gametest")
shutil.copytree(here/"resources",out/"src/main/resources",dirs_exist_ok=True)
res=out/"src/main/resources"
(res/"bond_beyond_ocean.properties").write_text("profile=vanilla-ocean\n")
mods=res/"META-INF/mods.toml";mods.write_text(mods.read_text().split("# Tectonic supplies")[0])
with (out/"build.gradle").open("a") as f:f.write("""
sourceSets.main.java.srcDir('gametest')
minecraft.runs { gameTestServer {
 workingDirectory project.file('build/gametest-world')
 property 'forge.enabledGameTestNamespaces', 'iceandfire_bond_beyond'
 property 'forge.logging.console.level', 'info'
 jvmArgs '-Xmx1024m', '-XX:ActiveProcessorCount=2'
} }
repositories { maven { url = 'https://api.modrinth.com/maven' } }
if (project.hasProperty('withSpartan')) { dependencies {
 runtimeOnly fg.deobf('maven.modrinth:spartan-weaponry:OcmgIP8o')
 runtimeOnly fg.deobf('maven.modrinth:spartan-weaponry-addon-toolkit:8B6WKhQP')
 runtimeOnly fg.deobf('maven.modrinth:spartan-weaponry-ice-and-fire:vIHI69Ae')
 runtimeOnly fg.deobf('curse.maven:ice-and-fire-spartan-weaponry-extras-1191816:6150005')
} }
""")
print("Prepared:",out)
