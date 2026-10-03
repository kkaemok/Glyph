param([switch]$Fetch)
$ErrorActionPreference = 'Stop'
$glyphRepo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$glyphBase = (Get-Content -LiteralPath (Join-Path $glyphRepo 'UPSTREAM_BASE') -Raw).Trim()
if ($Fetch) {
    git -C $glyphRepo fetch upstream dev master
    if ($LASTEXITCODE -ne 0) { throw 'Upstream fetch failed' }
}
git -C $glyphRepo log --reverse --oneline "$glyphBase..upstream/dev"
if ($LASTEXITCODE -ne 0) { throw 'Unable to compare upstream base' }
git -C $glyphRepo diff --stat "$glyphBase..upstream/dev"
if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect upstream changes' }
Write-Output 'Review each change against GLYPH_UPSTREAM_NOTES.md. Port relevant behavior into the migrated modules; this script does not merge.'
