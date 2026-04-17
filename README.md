# BonfireBreeding

[English](#english) | [简体中文](#简体中文)

BonfireBreeding is Bonfire's Paper breeding and husbandry plugin.

BonfireBreeding 是 Bonfire 的 Paper 养殖与繁育系统插件。

---

## English

BonfireBreeding is a Paper plugin for Bonfire's animal husbandry gameplay, covering breeding, growth, harvesting, animal-state tracking, GUI management, and admin tooling.

### Core Scope

- Config-driven animal definitions, levels, breeding items, and passive drops.
- Runtime listeners for feeding, mating, growth, harvesting, and pack behavior.
- GUI and command flows for transfer, toggle, inspection, and admin control.
- Optional ecosystem hooks for ItemsAdder, MythicMobs, and MCPets.

### Core Command

- `/bbreeding <menu|reload|give|info|list|transfer|unbind|toggle|help|admin>`

### Repository Layout

- `src/`: plugin source code
- `pom.xml`: Maven build definition
- `target/`: local build output, excluded from release tracking

### Build

```powershell
.\mvnw.cmd -q -DskipTests package
```

### License

This repository currently uses the `Bonfire Non-Commercial Source License 1.0`.
See [LICENSE](LICENSE) for the exact terms.

---

## 简体中文

BonfireBreeding 是 Bonfire 的 Paper 养殖玩法插件，覆盖繁殖、成长、采集、动物状态管理、GUI 操作与管理命令。

### 核心范围

- 基于配置的动物定义、等级、繁殖材料与被动产出。
- 运行时监听喂养、交配、成长、采集与群体行为。
- 提供转移、开关、查看与管理端操作的 GUI 与命令流。
- 可选联动 ItemsAdder、MythicMobs 与 MCPets 等生态。

### 主要命令

- `/bbreeding <menu|reload|give|info|list|transfer|unbind|toggle|help|admin>`

### 仓库结构

- `src/`：插件源码
- `pom.xml`：Maven 构建定义
- `target/`：本地构建输出，不纳入发布源码

### 构建方式

```powershell
.\mvnw.cmd -q -DskipTests package
```

### 授权

本仓库当前采用 `Bonfire Non-Commercial Source License 1.0`。
具体条款见 [LICENSE](LICENSE)。
