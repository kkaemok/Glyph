"""Compile the original API and a consumer, then check/run with only Glyph's jar.

Run after Gradle build and :glyph-paper-api:writeCompatibilityClasspath.
The upstream classes are intentionally absent from the runtime classpath.
"""
from pathlib import Path
import argparse
import os
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    args = parser.parse_args()
    java = args.java_home / 'bin' if args.java_home else Path('')
    suffix = '.exe' if os.name == 'nt' else ''
    javac, jvm = str(java / ('javac' + suffix)), str(java / ('java' + suffix))
    base = (ROOT / 'UPSTREAM_BASE').read_text().strip()
    work = ROOT / '.validation/api-compatibility'
    sources, baseline, probe = (work / name for name in ('upstream-sources', 'upstream-classes', 'consumer-classes'))
    for directory in (sources, baseline, probe): directory.mkdir(parents=True, exist_ok=True)
    dependencies = (ROOT / 'glyph-paper-api/build/reports/compatibility-classpath.txt').read_text().split(os.pathsep)
    # Prevent Glyph's classes from leaking into the upstream/consumer compilation.
    dependencies = [p for p in dependencies if Path(p).is_file() and not Path(p).resolve().is_relative_to(ROOT)]
    cp = os.pathsep.join(dependencies)

    def run(executable, arguments):
        argfile = work / 'java.args'
        argfile.write_text('\n'.join('"' + str(a).replace('\\', '/') + '"' for a in arguments), encoding='utf-8')
        subprocess.run([executable, '@' + str(argfile)], cwd=ROOT, check=True)

    original_sources = []
    for prefix in ('api/src/main/java/', 'api/bukkit-api/src/main/java/', 'api/velocity-api/src/main/java/'):
        names = subprocess.check_output(['git','ls-tree','-r','--name-only',base,prefix], cwd=ROOT, text=True).splitlines()
        for name in names:
            target = sources / name[len(prefix):]
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(subprocess.check_output(['git','show',f'{base}:{name}'],cwd=ROOT))
            original_sources.append(target)
    lombok = next((p for p in dependencies if Path(p).name.startswith('lombok-')), None)
    if not lombok: raise RuntimeError('Lombok processor missing from exported compile classpath')
    run(javac, ['-cp',cp,'-processorpath',lombok,'-d',baseline,*original_sources])
    run(javac, ['-cp',str(baseline)+os.pathsep+cp,'-d',probe,ROOT/'tools/api/LegacyApiConsumer.java'])
    jars = list((ROOT / 'build/libs').glob('Glyph-paper-*.jar'))
    if len(jars) != 1: raise RuntimeError(f'Expected one Paper distribution, got {jars}')
    paper = jars[0]
    runtime = os.pathsep.join([str(paper),str(probe),cp])
    run(javac, ['-cp',runtime,'-d',probe,ROOT/'tools/api/VerifyBinaryApi.java',ROOT/'tools/api/RunLegacyConsumer.java'])
    paper_baseline = work/'paper-baseline'
    for file in baseline.rglob('*.class'):
        relative = file.relative_to(baseline)
        if 'velocity' in relative.parts: continue
        target = paper_baseline/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(file.read_bytes())
    run(jvm, ['-cp',runtime,'VerifyBinaryApi',paper_baseline])
    run(jvm, ['-cp',runtime,'RunLegacyConsumer',paper,work/'data'])
    velocity = list((ROOT/'build/libs').glob('Glyph-velocity-*.jar'))
    if len(velocity) != 1: raise RuntimeError('Expected one Velocity distribution')
    # Velocity exports the neutral and Velocity API, not Bukkit-specific contracts.
    velocity_baseline = work/'velocity-baseline'
    for file in baseline.rglob('*.class'):
        relative = file.relative_to(baseline)
        if 'bukkit' in relative.parts: continue
        target = velocity_baseline/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(file.read_bytes())
    run(jvm, ['-cp',os.pathsep.join([str(velocity[0]),str(probe),cp]),'VerifyBinaryApi',velocity_baseline])


if __name__ == '__main__': main()
