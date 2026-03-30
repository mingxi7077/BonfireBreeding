# BonfireBreeding

![License](https://img.shields.io/badge/license-GPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Paper%201.21.x-brightgreen)
![Java](https://img.shields.io/badge/java-21-orange)
![Status](https://img.shields.io/badge/status-active-success)

BonfireBreeding is a Paper plugin for Bonfire's animal husbandry gameplay, covering breeding, growth, harvesting, animal state tracking, and admin tooling.

## Highlights

- Config-driven animal definitions, levels, breeding items, and passive drops.
- Runtime listeners for feeding, mating, growth, harvesting, and pack behavior.
- GUI and command flows for management, transfer, toggle, and inspection actions.
- Optional ecosystem hooks for ItemsAdder, MythicMobs, and MCPets.

## Core Command

- `/bbreeding <menu|reload|give|info|list|transfer|unbind|toggle|help|admin>`

## Build

```powershell
.\mvnw.cmd -q -DskipTests package
```

## Repository Scope

- Source and config content only.
- Maven outputs and deployment artifacts are excluded from Git.

## License

GPL-3.0
