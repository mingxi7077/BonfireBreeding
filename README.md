# BonfireBreeding

![License](https://img.shields.io/badge/license-BNSL--1.0-red)
![Commercial Use](https://img.shields.io/badge/commercial-use%20by%20written%20permission%20only-critical)
![Platform](https://img.shields.io/badge/platform-Paper%201.21.x-brightgreen)
![Java](https://img.shields.io/badge/java-21-orange)
![Status](https://img.shields.io/badge/status-active-success)

BonfireBreeding is a Paper plugin for Bonfire's animal husbandry gameplay, covering breeding, growth, harvesting, animal state tracking, and admin tooling.

> Non-commercial source-available. Commercial use requires prior written permission via `mingxi7707@qq.com`.

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

Bonfire Non-Commercial Source License 1.0

Commercial use is prohibited unless you first obtain written permission from `mingxi7707@qq.com`.
