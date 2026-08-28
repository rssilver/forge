# Forge Specification Document

## Table of Contents
1. [Introduction](#introduction) - High-level overview of the application and its purpose.
2. [High-Level Architecture](#high-level-architecture) - Overview of the modular structure (core, game, ai, gui).
3. [Core Logic (`forge-core`)](#core-logic) - Documentation of game rules, card definitions, and state management.
4. [Game Engine (`forge-game`)](#game-engine) - Documentation of gameplay execution and simulation logic.
5. [AI Systems (`forge-ai` & `forge-lda`)](#ai-systems) - Detailed analysis of AI engines, including the **Ultima AI system**.
6. [User Interfaces (GUI Modules)](#user-interfaces) - Analysis of desktop, mobile, and iOS implementations.
7. [Support Tools](#support-tools) - Documentation of Adventure Editor and Installer.
8. [Project Structure & File Map](#project-structure) - Comprehensive mapping of files to features.

---

## Introduction
Forge is a comprehensive digital trading card game (TCG) simulator designed for high fidelity to original game rules, supporting multiple platforms including Desktop, Android, and iOS.

## High-Level Architecture
The application follows a modular architecture to separate game logic from presentation layers:

*   **`forge-core`**: The source of truth for all game data and rule enforcement.
*   **`forge-game`**: The execution layer that manages the flow of a match.
*   **`forge-ai`**: The decision-making engine for automated opponents.
*   **GUI Modules (`forge-gui`, `forge-gui-desktop`, etc.)**: Platform-specific wrappers that provide the user interface and interact with the core/game layers.


## Core Logic (`forge-core`)
The `forge-core` module serves as the foundation of the application, containing all static data and fundamental rule definitions.

### Card System
*   **Card Data & Definitions**: Manages how cards are defined and stored. Key files include `CardDb.java`, `CardFace.java`, and `ICardDatabase.java`.
*   **Characteristics & Types**: Defines card types, colors, and characteristics (e.g., `CardType.java`, `MagicColor.java`).
*   **Rule Predicates**: Implements logic to determine if a card or game state meets certain criteria (`CardRulesPredicates.java`, `CardFacePredicates.java`).

### Deck Management
*   **Deck Structure**: Handles deck composition and validation (`Deck.java`, `DeckBase.java`).
*   **Deck Formats**: Defines the rules for different deck construction formats (`DeckFormat.java`).
*   **I/O Operations**: Manages saving and loading decks (`forge/deck/io`).

### Item & Inventory System
*   **Collectibles**: Implements a system for booster packs, sealed products, and individual card ownership (`BoosterPack.java`, `SealedProduct.java`, `InventoryItem.java`).
*   **Generation**: Logic for generating random cards from packs (`forge/item/generation`).

### Utilities
*   **Core Helpers**: General purpose utility classes for text, files, and randomization (`TextUtil.java`, `FileUtil.java`, `MyRandom.java`).
*   **Localization**: Handles multi-language support for card names and rules (`Localizer.java`, `CardTranslation.java`).



## Game Engine (`forge-game`)
The `forge-game` module implements the runtime logic for executing matches and simulating game states.

### Gameplay Core
*   **Game State Management**: Tracks the current state of a match, including players, zones, and active effects (`GameState.java`, `Match.java`).
*   **Rule Execution**: Enforces the sequence of phases and turns, handling action resolution (`Game.java`, `GameRules.java`).
*   **Entity Tracking**: Manages game objects (cards, tokens) and their properties across different zones (`GameObject.java`, `GameEntity.java`).

### Interaction & Mechanics
*   **Action System**: Defines how players interact with the game through commands and actions (`GameAction.java`, `GameCommand.java`).
*   **Ability System**: Implements the logic for activated, triggered, and static abilities (`forge/game/ability`, `StaticEffect.java`).
*   **Combat & Zones**: Handles combat resolution and movement between zones like hand, battlefield, graveyard, and exile (`forge/game/combat`, `forge/game/zone`).

### Logging & State Snapshots
*   **Game Log**: Maintains a detailed history of all events occurring during a match for audit and replay purposes (`GameLog.java`, `GameLogEntry.java`).
*   **Snapshots**: Capable of taking snapshots of the game state for undo/redo or analysis functionality (`GameSnapshot.java`).

### Trackables
*   **Reactive Properties**: Implements a system for tracking objects and properties that can be observed for changes (`forge/trackable/Tracker.java`, `TrackableObject.java`).



## AI Systems (`forge-ai` & `forge-lda`)
The `forge-ai` module provides the decision-making logic for computer-controlled players.

### Standard AI Framework
*   **AI Controllers**: Manages high-level player actions and decisions (`AiController.java`, `PlayerControllerAi.java`).
*   **Simulation Engine**: Uses game state copying to simulate possible future moves and evaluate their outcomes (`forge/ai/simulation/GameSimulator.java`, `GameStateEvaluator.java`).
*   **Ability Handlers**: A vast library of specialized AI logic for different types of card abilities (e.g., `DrawAi.java`, `DamageDealAi.java`, `MillAi.java` in `forge/ai/ability`).
*   **Decision Utilities**: Helpers for calculating costs, mana usage, and target selection (`ComputerUtilCost.java`, `ComputerUtilMana.java`).

### **Ultima AI System**
The Ultima AI is an advanced engine that combines traditional search with modern LLM consultation.
*   **Alpha-Beta Search Engine**: Implements a minimax algorithm with alpha-beta pruning to find optimal moves (`AlphaBetaEngine.java`).
*   **Combo Detection**: Specialized logic to identify and prioritize high-impact card combinations (`ComboDetector.java`).
*   **LLM Integration**: 
    *   **Client & Config**: Manages communication with external Large Language Models (`LLMClient.java`, `LLMConfig.java`).
    *   **Response Handling**: Parses LLM output to translate strategic suggestions into game actions (`UltimaLLMResponse.java`).
*   **Coordination**: The `UltimaController.java` orchestrates the search engine, combo detector, and LLM consultation to make a final decision.

### AI Performance & Caching
*   **Evaluation Caching**: Implements caching for creature and permanent evaluations to optimize performance (`AiCache.java`).

### Latent Dirichlet Allocation (`forge-lda`)
The `forge-lda` module implements a topic modeling system used for analyzing game data or AI behavior patterns.
*   **Dataset Management**: Handles vocabularies and bag-of-words representations of text/game events (`Vocabulary.java`, `BagOfWords.java`).
*   **LDA Engine**: Implements the core Latent Dirichlet Allocation algorithm to discover latent topics in the dataset (`LDA.java`, `LDAModelGenerator.java`).
*   **Inference Logic**: Provides mechanisms for inferring topics from new data using various inference methods (`Inference.java`, `InferenceFactory.java`).



## User Interfaces (GUI Modules)
Forge provides a cross-platform user experience through multiple specialized GUI modules. All GUIs interact with the `forge-core` and `forge-game` layers to render the game state.

### Desktop Interface (`forge-gui`, `forge-gui-desktop`)
*   **Primary GUI**: The main desktop application implementing the visual representation of the card game, including the board, hand, and menu systems.
*   **Resources (`res/`)**: Contains extensive assets for the UI, including:
    *   `defaults/`: Layout definitions for the home screen, match view, and editor.
    *   `puzzle/`: Puzzle mode configuration files.
    *   `sound/`: Audio assets for game events (e.g., `draw.mp3`, `shuffle.mp3`).
    *   `editions/`: Metadata for various card set editions.
*   **Tooling (`tools/`)**: Includes Python scripts for data scraping, deck conversion, and translation management (e.g., `oracleScraper.py`, `deckConversionTools`).

### Mobile Interfaces (`forge-gui-android`, `forge-gui-mobile`, `forge-gui-ios`)
*   **Android Implementation**: A native Android wrapper utilizing LibGDX for rendering (`AndroidManifest.xml`, `res/layout/main.xml`).
*   **iOS Implementation**: An iOS port using RoboVM to bridge Java to the Apple ecosystem (`robovm.xml`, `oslog_wrapper`).
*   **Mobile Shared Logic (`forge-gui-mobile`)**: Common mobile-specific rendering and input logic, handling aspects like touch interactions and optimized graphics (`GuiMobile.java`, `Graphics.java`).

### Visual & Asset Management
*   **Skins/Themes**: Support for custom visual skins to change the appearance of the interface (e.g., `fallback_skin` in mobile modules).
*   **Asset Pipelines**: Tools for managing large quantities of card images and audio files across different platforms.



## Support Tools
Forge is accompanied by auxiliary applications for content creation and distribution.

### Adventure Editor (`adventure-editor`)
*   **Purpose**: A dedicated tool for designing adventure campaigns, creating narrative events, and configuring quest logic.


## Project Structure & File Map
This section provides a mapping of the physical directory structure to the functional components described above.

### Root Directory
*   `pom.xml`: Main Maven project configuration.
*   `build.sh`/`build.bat`: Build scripts for different environments.
*   `run-forge.sh`/`run-forge.bat`: Execution scripts to launch the application.

### Module Mapping
*   **Core**: `/forge-core` $\rightarrow$ See [Core Logic](#core-logic).
*   **Game Engine**: `/forge-game` $\rightarrow$ See [Game Engine](#game-engine).
*   **AI Systems**: `/forge-ai`, `/forge-lda` $\rightarrow$ See [AI Systems](#ai-systems).
*   **User Interfaces**: 
    *   Desktop: `/forge-gui`, `/forge-gui-desktop`.
    *   Android: `/forge-gui-android`.
    *   iOS: `/forge-gui-ios`.
    *   Mobile Shared: `/forge-gui-mobile`.
*   **Tools**: 
    *   Adventure Editor: `/adventure-editor`.
    *   Installer: `/forge-installer`.

### Documentation & Assets
*   `/docs`: Contains comprehensive user guides, API references, and installation instructions.
*   `/forge-gui/res`: The central repository for UI layouts, sounds, and set metadata.

*   **Implementation**: Built as a separate module with its own configuration and resource management to avoid bloating the main client.

### Forge Installer (`forge-installer`)
*   **Purpose**: Handles the installation process of the application across different operating systems.
*   **Components**: Uses XML-based installation scripts (`install.xml`) and handles dependency libraries required for the initial setup.

