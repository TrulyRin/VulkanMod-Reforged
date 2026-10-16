import hashlib
import io
import json
import re
import shutil
import struct
import tomllib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
props = dict(re.findall(r"^([a-z_]+)\s*=\s*(.*?)\s*$", (root / "gradle.properties").read_text(), re.M))
mc = props["minecraft_version"]
major = 61 if mc == "1.20.1" else 65
boundaries = {
    "1.20.1": ("forge", "[47.1.106,48)", "[1.20.1,1.20.2)"),
    "1.21.1": ("neoforge", "[21.1,21.2)", "[1.21.1,1.21.2)"),
    "1.21.11": ("neoforge", "[21.11,21.12)", "[1.21.11,1.21.12)"),
}
loader, loader_range, mc_range = boundaries[mc]
libs = root / "build/libs"
outputs = [p for p in libs.glob("*.jar") if not p.name.endswith("-sources.jar")]
if len(outputs) != 1:
    raise RuntimeError(f"Expected one production JAR, found {len(outputs)}")
jar = outputs[0]
payloads = {
    "lwjgl-vulkan-3.3.3.jar": "org/lwjgl/vulkan/VK.class",
    "lwjgl-shaderc-3.3.3.jar": "org/lwjgl/util/shaderc/Shaderc.class",
    "lwjgl-vma-3.3.3.jar": "org/lwjgl/util/vma/Vma.class",
}
for module, stem in (("shaderc", "shaderc"), ("vma", "lwjgl_vma")):
    for suffix, platform, extension, prefix in (
        ("windows", "windows/x64", "dll", ""),
        ("linux", "linux/x64", "so", "lib"),
        ("macos", "macos/x64", "dylib", "lib"),
        ("macos-arm64", "macos/arm64", "dylib", "lib"),
    ):
        payloads[f"lwjgl-{module}-3.3.3-natives-{suffix}.jar"] = f"{platform}/org/lwjgl/{module}/{prefix}{stem}.{extension}"
for suffix, arch in (("macos", "x64"), ("macos-arm64", "arm64")):
    payloads[f"lwjgl-vulkan-3.3.3-natives-{suffix}.jar"] = f"macos/{arch}/org/lwjgl/vulkan/libMoltenVK.dylib"
expected = {"META-INF/jarjar/" + name: payload for name, payload in payloads.items()}
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    if len(names) != len(set(names)) or archive.testzip() is not None:
        raise RuntimeError("Invalid or duplicate release entries")
    if any(re.search(r"test.?control|local-validation|(^|/)\.env", n, re.I) for n in names):
        raise RuntimeError("Development files found in release")
    metadata_path = "META-INF/mods.toml" if mc == "1.20.1" else "META-INF/neoforge.mods.toml"
    metadata = tomllib.loads(archive.read(metadata_path).decode())
    mods = metadata.get("mods", [])
    if len(mods) != 1 or mods[0].get("modId") != "vulkanmod" or mods[0].get("displayName") != "VulkanMod Reforged" or mods[0].get("version") != props["mod_version"]:
        raise RuntimeError("Incorrect release mod metadata")
    if metadata.get("modLoader") != "javafml" or metadata.get("license") != "LGPL-3.0":
        raise RuntimeError("Incorrect loader or license metadata")
    dependencies = metadata.get("dependencies", {}).get("vulkanmod", [])
    ranges = {item["modId"]: item.get("versionRange") for item in dependencies}
    if ranges.get(loader) != loader_range or ranges.get("minecraft") != mc_range:
        raise RuntimeError("Unsupported Minecraft/loader dependency boundary")
    for item in dependencies:
        if item["modId"] in (loader, "minecraft"):
            if mc == "1.20.1" and item.get("mandatory") is not True:
                raise RuntimeError("Required Forge dependency is optional")
            if mc != "1.20.1" and item.get("type") != "required":
                raise RuntimeError("Required NeoForge dependency is optional")
    for legal in ("LICENSE_", "COPYING_", "NOTICE_"):
        candidates = [n for n in names if n.startswith(legal)]
        if len(candidates) != 1 or archive.read(candidates[0]) != (root / legal[:-1]).read_bytes():
            raise RuntimeError(f"Missing or mismatched {legal}")
    for name in names:
        if name.endswith(".class"):
            data = archive.read(name)
            if data[:4] != b"\xca\xfe\xba\xbe" or struct.unpack(">H", data[6:8])[0] > major:
                raise RuntimeError(f"Incorrect Java target: {name}")
    nested = {n for n in names if n.startswith("META-INF/jarjar/lwjgl-") and n.endswith(".jar")}
    if nested != set(expected):
        raise RuntimeError("Incorrect bundled LWJGL artifact paths")
    bundled = json.loads(archive.read("META-INF/jarjar/metadata.json"))["jars"]
    identifiers = [(item["identifier"]["group"], item["identifier"]["artifact"]) for item in bundled]
    if len(identifiers) != len(set(identifiers)) or {item["path"] for item in bundled} != nested or len(bundled) != len(nested):
        raise RuntimeError("Incorrect JarJar metadata mapping")
    for item in bundled:
        expected_artifact = Path(item["path"]).name.removesuffix(".jar").replace("-3.3.3", "", 1)
        if item["identifier"] != {"group": "org.lwjgl", "artifact": expected_artifact} or item["version"].get("artifactVersion") != "3.3.3":
            raise RuntimeError("Incorrect nested dependency identity")
    for name, payload in expected.items():
        with zipfile.ZipFile(io.BytesIO(archive.read(name))) as inner:
            entries = inner.namelist()
            if len(entries) != len(set(entries)) or inner.testzip() is not None or payload not in entries or not inner.read(payload):
                raise RuntimeError(f"Invalid nested dependency payload: {name}")
            if any(n == "module-info.class" or re.match(r"META-INF/versions/[^/]+/module-info.class$", n) for n in entries):
                raise RuntimeError(f"Unexpected module descriptor: {name}")
sources = list(libs.glob("*-sources.jar"))
if len(sources) != 1:
    raise RuntimeError("Missing corresponding source JAR")
with zipfile.ZipFile(sources[0]) as source:
    if source.testzip() is not None or any(re.search(r"test.?control|local-validation", n, re.I) for n in source.namelist()):
        raise RuntimeError("Invalid corresponding source JAR")
    for path in (root / "src/main").rglob("*"):
        if not path.is_file():
            continue
        relative = path.relative_to(root / "src/main")
        if relative.parts[0] not in ("java", "resources"):
            continue
        entry = Path(*relative.parts[1:]).as_posix()
        if source.read(entry) != path.read_bytes():
            raise RuntimeError(f"Source JAR mismatch: {entry}")
out = root / "build/release"
out.mkdir(exist_ok=True)
for artifact in (jar, sources[0]):
    shutil.copy2(artifact, out / artifact.name)
print(f"Verified {jar.name}: {hashlib.sha256(jar.read_bytes()).hexdigest()}")
