# SessionStart hook for superpowers plugin
# PowerShell version for Windows compatibility

$ErrorActionPreference = "Stop"

# Determine plugin root directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PluginRoot = Split-Path -Parent $ScriptDir

# Check if legacy skills directory exists and build warning
$warningMessage = ""
$legacySkillsDir = Join-Path $env:USERPROFILE ".config/superpowers/skills"
if (Test-Path $legacySkillsDir) {
    $warningMessage = "`n`n<important-reminder>IN YOUR FIRST REPLY AFTER SEEING THIS MESSAGE YOU MUST TELL THE USER:⚠️ **WARNING:** Superpowers now uses Claude Code's skills system. Custom skills in ~/.config/superpowers/skills will not be read. Move custom skills to ~/.claude/skills instead. To make this message go away, remove ~/.config/superpowers/skills</important-reminder>"
}

# Read using-superpowers content
$usingSuperpowersPath = Join-Path $PluginRoot "skills/using-superpowers/SKILL.md"
try {
    $usingSuperpowersContent = Get-Content -Path $usingSuperpowersPath -Raw -ErrorAction Stop
} catch {
    $usingSuperpowersContent = "Error reading using-superpowers skill"
}

# Escape string for JSON embedding
function Escape-ForJson {
    param([string]$InputString)
    
    $escaped = $InputString `
        -replace '\\', '\\' `
        -replace '"', '\"' `
        -replace "`n", '\n' `
        -replace "`r", '\r' `
        -replace "`t", '\t'
    
    return $escaped
}

$usingSuperpowersEscaped = Escape-ForJson $usingSuperpowersContent
$warningEscaped = Escape-ForJson $warningMessage
$sessionContext = "<EXTREMELY_IMPORTANT>`nYou have superpowers.`n`n**Below is the full content of your 'superpowers:using-superpowers' skill - your introduction to using skills. For all other skills, use the 'Skill' tool:**`n`n$usingSuperpowersEscaped`n`n$warningEscaped`n</EXTREMELY_IMPORTANT>"

# Output context injection as JSON
$output = @{
    additional_context = $sessionContext
    hookSpecificOutput = @{
        hookEventName = "SessionStart"
        additionalContext = $sessionContext
    }
} | ConvertTo-Json -Depth 10 -Compress

Write-Output $output
exit 0
