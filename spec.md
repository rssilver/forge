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
The `forge-core` module is the foundational data layer of the application. It defines the static properties of every game object and handles the persistence of card databases.

### Card Definition System (`forge/card`)
*   **Card Database**: `CardDb.java` and `ICardDatabase.java` manage the loading and querying of card data.
*   **Core Definitions**: `CardFace.java` defines the properties of a specific card printing, while `ICardFace.java` provides the interface for these definitions.
*   **Characteristics**: Card types (`CardType.java`), colors (`MagicColor.java`, `ColorSet.java`), and rarities (`CardRarity.java`) are strictly defined here to ensure rule consistency across all modules.
*   **Rule Predicates**: `CardRulesPredicates.java` and `CardFacePredicates.java` implement the logic used by the engine to determine if cards meet specific criteria (e.g., "is a creature", "has flying").

### Deck & Collection Management (`forge/deck`, `forge/item`)
*   **Deck Logic**: `Deck.java` and `DeckBase.java` manage card lists, while `DeckFormat.java` enforces construction rules for different game formats.
*   **I/O Operations**: The `forge/deck/io` package handles the serialization and deserialization of deck files.
*   **Inventory & Sealed Product**: Implements a complex collectible system including:
    *   `BoosterPack.java`, `SealedProduct.java`, and `BoxedProduct.java` for simulating sealed product opening.
    *   `InventoryItem.java` tracks player ownership of specific cards/items.
    *   `TournamentPack.java` and `FatPack.java` handle specialized pack types.

### Utilities & Localization (`forge/util`)
*   **Localization**: `Localizer.java` and `CardTranslation.java` manage multi-language support for the global user base.
*   **Core Helpers**: General purpose utilities like `TextUtil.java`, `FileUtil.java`, and `MyRandom.java` provide consistent behavior across the codebase.




## Game Engine (`forge-game`)
The `forge-game` module is the heart of the application, implementing the complex rules and state management of the game.

### Core Game State & Entities (`forge/game`)
*   **Game Management**: `Game.java`, `GameState.java`, and `Match.java` manage the overall match flow, turn structure, and persistence of the current game state.
*   **Game Objects**: `GameObject.java` is the base class for all entities in the game world, providing identity and tracking capabilities.
*   **Rules Engine**: `GameRules.java` implements the fundamental laws of the game that govern how objects interact.

### Card Runtime Logic (`forge/game/card`)
*   **Dynamic State**: `Card.java` and `CardState.java` track the real-time properties of cards (e.g., current power/toughness, counters) as opposed to their static definitions in `forge-core`.
*   **Lifecycle & Factories**: `CardFactory.java` and `CardFactoryUtil.java` handle the instantiation of game cards from database definitions.
*   **View Layer**: `CardView.java` provides a read-only representation of a card's state for UI rendering, ensuring internal state is not accidentally modified.

### Event & Trigger System (`forge/game/event`, `forge/game/trigger`)
*   **Event Bus**: A comprehensive system where `GameEvent.java` and its numerous subclasses (e.g., `GameEventTurnBegan.java`, `GameEventCardDestroyed.java`) notify the engine of state changes.
*   **Trigger Logic**: The `forge/game/trigger` package contains a vast library of triggers (e.g., `TriggerTaps.java`, `TriggerDamageDone.java`) that allow cards to react to game events automatically.

### Ability & Effect Framework (`forge/game/spellability`, `forge/game/ability`)
*   **Spell Abilities**: `SpellAbility.java` and `ISpellAbility.java` define the logic for activated and triggered abilities, including cost payment and effect resolution.
*   **Static Effects**: `StaticEffect.java` handles continuous changes to the game state that persist as long as the source is present.
*   **Execution Stack**: `MagicStack.java` (in `forge/game/zone`) manages the "stack" of abilities waiting to resolve, enforcing a Last-In-First-Out order.

### Cost & Keyword Systems (`forge/game/cost`, `forge/game/keyword`)
*   **Payment Logic**: The `forge/game/cost` package implements every possible way to pay for an ability, from `CostTap.java` and `CostPayLife.java` to complex costs like `CostExile.java`.
*   **Keyword Implementation**: Specialized logic for game keywords (e.g., `Trample.java`, `Hexproof.java`, `Ward.java`) is centralized here to ensure consistent behavior across all cards using them.

### Replacement Effects (`forge/game/replacement`)
*   **Rule Modification**: Implements the "Replacement Effect" layer where one event is substituted for another (e.g., `ReplaceDamage.java` for damage prevention).
The `forge-game` module implements the runtime logic for executing matches and simulating game states.
## Ultima AI System (`forge-ai/ultima`)
The Ultima AI system represents a significant evolution in the application's artificial intelligence, transitioning from purely heuristic-based decision making to a hybrid approach that integrates Large Language Models (LLMs) with traditional game engines.

### Hybrid Decision Engine
*   **Controller**: `UltimaController.java` acts as the primary entry point and orchestrator for AI turns, managing the flow between rule-based analysis and LLM-driven strategic planning.
*   **Search & Optimization**: `AlphaBetaEngine.java` implements a modified Alpha-Beta pruning algorithm to evaluate potential game moves, providing the tactical foundation that the AI uses to simulate outcomes.
*   **Combo Detection**: `ComboDetector.java` analyzes the current game state and available cards to identify synergistic sequences of plays (combos) that can lead to an immediate win or significant advantage.

### LLM Integration Layer
*   **Communication Bridge**: `LLMClient.java` handles the asynchronous communication with external LLM providers, managing API requests and response parsing.
*   **Configuration & State**: `LLMConfig.java` defines the parameters for AI behavior (e.g., temperature, system prompts), while `UltimaLLMResponse.java` structures the strategic advice received from the model.
*   **Strategic Reasoning**: Unlike previous versions, Ultima uses LLMs to reason about high-level game goals and complex board states that are difficult to quantify with raw heuristics.

### Integration with Game Engine
*   **State Representation**: The AI consumes `GameView` and `CardView` objects from `forge-game` to build a comprehensive understanding of the board without modifying the actual game state.
*   **Action Execution**: Once a strategic path is chosen by the LLM and validated by the Alpha-Beta engine, it is converted into standard `GameCommand` sequences for execution within the match loop.


### Gameplay Core
*   **Game State Management**: Tracks the current state of a match, including players, zones, and active effects. `GameState.java` handles detailed snapshots (lines 57-153) and reconstruction of game states from serialized data (lines 161-309).
*   **Rule Execution & Match Loop**: `Game.java` is the central coordinator for a single match. It manages:
    *   **Phase Progression**: Coordinates transitions between phases like Untap, Upkeep, and Combat via `PhaseHandler` (line 91) and specific phase classes (`Untap`, `EndOfTurn`, etc., lines 80-85).
    *   **The Stack**: Manages the sequence of spells and abilities waiting to resolve using `MagicStack` (line 89).
    *   **Event & Trigger System**: Dispatches game events via an internal `EventBus` (line 95) and manages triggered abilities through `TriggerHandler` (line 93).
    *   **Static Effects & Replacements**: Applies continuous rules modifications via `StaticEffects` (line 92) and handles replacement effects using `ReplacementHandler` (line 94).
    *   **Game State Persistence**: Supports snapshotting the game state for undo/redo or analysis through `stashGameState()` (line 201) and `restoreGameState()` (line 209), utilizing `GameSnapshot`.
*   **Entity Tracking**: Manages game objects (cards, tokens) and their properties across different zones (`GameObject.java`, `GameEntity.java`).

## Ultima AI System
The Ultima AI is a sophisticated decision engine that integrates classical search algorithms with modern LLM-based strategic advice.

### Architecture (`forge-ai/src/main/java/forge/ai/ultima`)
*   **UltimaController.java**: The central orchestrator (lines 20-316). It follows a multi-stage decision process:
    1.  **Candidate Generation**: Uses `SpellAbilityPicker` to find all legal moves.
    2.  **Alpha-Beta Search**: Employs `AlphaBetaEngine` (line 59) to simulate future game states and identify the move with the highest evaluative score, supporting opponent modeling if enabled (lines 62-66).
    3.  **Combo Detection**: Integrates `ComboDetector` (line 69) to prioritize moves that trigger high-severity synergy patterns (lines 70-76).
    4.  **LLM Consultation**: At critical junctures—such as Turn 1, tutor decisions, or end-of-turn checks (lines 137-142)—it queries an external LLM via `LLMClient` to refine its strategy.
*   **AlphaBetaEngine.java**: Implements the min-max search with alpha-beta pruning to explore the game tree up to a configurable depth (`AiProps.ULTIMA_ALPHA_BETA_DEPTH`).
*   **ComboDetector.java**: Analyzes the current board and hand for known synergy patterns, providing "alerts" that can override or boost specific moves in the controller's decision loop (lines 69-76 of `UltimaController`).
*   **LLM Integration (`LLMClient.java`, `LLMConfig.java`)**:
    *   **State Serialization**: `buildGameStateDescription()` (lines 218-280) converts the current game state into a human-readable summary for the LLM.
    *   **Caching**: Uses a `ConcurrentHashMap` (`responseCache`, line 33) to store and reuse LLM responses based on a hash of the game state, reducing API latency and cost (lines 176-194).
    *   **Advice Integration**: Recommended actions from the LLM are matched against legal candidates; if the LLM's suggestion is reasonably close in score to the alpha-beta best move, it may be selected (lines 81-92).

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

#### A. Decision Orchestration (`UltimaController.java`)
*   **Main Loop**: The `chooseBestSpell()` method (lines 44-95) coordinates the AI's decision process in four stages:
    1.  Candidate generation via standard `SpellAbilityPicker` (lines 46-47).
    2.  Deterministic search using the Alpha-Beta Engine (lines 59-66).
    3.  Combo override check via `ComboDetector` (lines 69-76).
    4.  LLM consultation for strategic guidance at critical junctures (lines 79-92).
*   **LLM Trigger Logic**: The system selectively consults an LLM in `shouldConsultLLM()` (lines 125-144) during key events: Turn 1, when tutor options exist, end of turn, or when combo threats are flagged.
*   **State Hashing & Caching**: To minimize API costs and latency, game states are hashed in `buildStateHash()` (lines 197-215) and cached in a `ConcurrentHashMap` (line 33).
*   **LLM Integration**: Manages API requests through `LLMClient` using configurations from `LLMConfig` (lines 164-171), providing the LLM with a human-readable game summary generated in `buildGameStateDescription()` (lines 218-280).

#### B. Alpha-Beta Search Engine (`AlphaBetaEngine.java`)
*   **Search Strategy**: Implements alpha-beta pruning to explore potential future game states while discarding suboptimal branches.
*   **Standard Search**: The `search()` method (lines 45-77) evaluates candidate moves and returns the one with the highest score from `GameStateEvaluator`.
*   **Opponent Modeling**: The `searchWithOpponentResponse()` method (lines 83-116) simulates a "minimax" scenario, anticipating how an opponent will respond to a move before committing.
*   **Simulation Loop**: Uses `GameCopier` (line 126) and recursive calls in `simulateAndSearch()` (lines 119-165) to traverse the game tree up to a configurable depth (default max 3, line 20).
*   **Branching Factor Control**: To maintain performance, the engine ranks candidates by heuristic score and limits evaluation to the top 8 moves per node (`MAX_CANDIDATES_PER_NODE`, line 21).

#### C. Combo Detection (`ComboDetector.java`)
*   **Pattern Recognition**: Identifies high-synergy card combinations that lead to immediate wins or significant advantages.
*   **Severity Scoring**: Assigns a severity score (0-100) to detected combos; `UltimaController` prioritizes moves that set up combos with severity $\ge 70$ (line 108).


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

