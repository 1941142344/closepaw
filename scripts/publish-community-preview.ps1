param(
    [Parameter(Mandatory=$true)][string]$AssetsDir,
    [string]$GhPath = 'gh'
)
$ErrorActionPreference = 'Stop'
$repository = '1941142344/closepaw'
$expectedAccount = '1941142344'
$tagName = 'v0.1.0-codex-compat.1'
$sourceRoot = Split-Path -Parent $PSScriptRoot
$assetsRoot = (Resolve-Path -LiteralPath $AssetsDir).Path
$assetNames = @(
    'ClosePaw-ChatGPT-Plus-debug.apk', 'SHA256SUMS.txt', 'INSTALL.zh-CN.md',
    'VALIDATION.md', 'DISTRIBUTION.md', 'bilibili-demo.md',
    'gradle-verification.log', 'lint-results-debug.html', 'SOURCE_COMMIT.txt'
)
foreach ($assetName in $assetNames) {
    if (!(Test-Path -LiteralPath (Join-Path $assetsRoot $assetName) -PathType Leaf)) {
        throw "Missing release asset: $assetName"
    }
}
$apkHash = (Get-FileHash -LiteralPath (Join-Path $assetsRoot $assetNames[0]) -Algorithm SHA256).Hash.ToLowerInvariant()
if ($apkHash -ne '58f722e2e83cf9d8260a92f338d7c41180dfa209b9069354715e632a08a55d6c') {
    throw 'APK hash differs from the verified preview artifact. Stop and re-validate.'
}

Push-Location $sourceRoot
try {
    $account = & $GhPath api user --jq .login
    if ($LASTEXITCODE -ne 0) { throw 'Complete gh auth login on GitHub before publishing.' }
    if ($account.Trim() -ne $expectedAccount) { throw "Authenticated account must be $expectedAccount." }
    $workingChanges = & git status --porcelain
    if ($LASTEXITCODE -ne 0 -or $workingChanges) { throw 'Commit and review source changes before publication.' }
    $sourceCommit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Could not resolve source commit.' }
    $manifestCommit = (Get-Content -LiteralPath (Join-Path $assetsRoot 'SOURCE_COMMIT.txt') -Raw).Trim()
    if ($manifestCommit -ne $sourceCommit) { throw 'Asset manifest does not match the committed source.' }

    # Fork preserves upstream history and attribution. Never overwrite an unrelated repository.
    & $GhPath repo fork imoonkey/closepaw --clone=false --default-branch-only
    if ($LASTEXITCODE -ne 0) { throw 'Could not create or locate the user fork.' }
    $repositoryJson = & $GhPath api "repos/$repository"
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify fork metadata.' }
    $repositoryInfo = $repositoryJson | ConvertFrom-Json
    if (!$repositoryInfo.fork -or $repositoryInfo.parent.full_name -ne 'imoonkey/closepaw' -or $repositoryInfo.private) {
        throw 'Destination must be the public fork of imoonkey/closepaw.'
    }
    $remoteUrl = "https://github.com/$repository.git"
    $existingRemote = & git remote get-url community 2>$null
    if ($LASTEXITCODE -ne 0) {
        & git remote add community $remoteUrl
    } elseif ($existingRemote.Trim() -ne $remoteUrl) {
        throw 'The community remote points to a different repository.'
    }
    if ($LASTEXITCODE -ne 0) { throw 'Could not prepare the community remote.' }
    & $GhPath auth setup-git --hostname github.com
    if ($LASTEXITCODE -ne 0) { throw 'Could not set up standard GitHub CLI git authentication.' }

    # Explicit refspec and no force: a changed remote branch fails safely for review.
    & git push community HEAD:refs/heads/main
    if ($LASTEXITCODE -ne 0) { throw 'Push rejected. Review remote changes; do not force-push.' }
    & git rev-parse --verify --quiet "refs/tags/$tagName" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        & git -c user.name=1941142344 -c user.email=186886359+1941142344@users.noreply.github.com tag -a $tagName -m 'Community Codex compatibility preview 1'
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the preview tag.' }
    }
    $tagCommit = (& git rev-list -n 1 $tagName).Trim()
    if ($tagCommit -ne $sourceCommit) { throw 'Existing preview tag points to a different commit.' }
    & git push community "refs/tags/$tagName"
    if ($LASTEXITCODE -ne 0) { throw 'Could not push the preview tag.' }

    $existingRelease = & $GhPath release view $tagName --repo $repository --json url,isDraft,assets 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Output $existingRelease
        throw 'Release already exists; inspect and resume its uploads explicitly. No assets overwritten.'
    }
    $assetPaths = @($assetNames | ForEach-Object { Join-Path $assetsRoot $_ })
    & $GhPath release create $tagName @assetPaths --repo $repository --verify-tag --prerelease --latest=false --title 'ClosePaw ChatGPT/Codex 社区兼容修复预览 1' --notes-file (Join-Path $sourceRoot 'doc/release/community/RELEASE_NOTES.md')
    if ($LASTEXITCODE -ne 0) { throw 'Release creation/upload did not finish. Inspect remote status before retrying.' }
    & $GhPath release view $tagName --repo $repository --json url,isDraft,isPrerelease,tagName,targetCommitish,assets
    if ($LASTEXITCODE -ne 0) { throw 'Release submitted but final verification failed.' }
} finally {
    Pop-Location
}
