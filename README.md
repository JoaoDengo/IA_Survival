# ProjetoFinal

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## AISurvivors - Arquitetura MVC

`AISurvivors` sera um jogo inspirado em Vampire Survivors, usando:

- `Ashley` para ECS (entidades, componentes e systems).
- `Box2D` para fisica, colisao e contato.
- `box2d-lights` para iluminacao do cenario e efeitos.
- `gdx-ai` para comportamento dos inimigos.

### Model (estado e regras)

- Entidades e componentes (`Transform`, `Velocity`, `Health`, `Damage`, `Weapon`, `XP`, `EnemyAI`, `Collider`).
- Regras de jogo: spawn de ondas, dano, morte, drops, level up e cooldowns.
- Systems de logica: `PhysicsSystem`, `CollisionSystem`, `AISystem`, `CombatSystem`, `SpawnSystem`, `ProgressionSystem`.

### View (render e interface)

- Telas: `MenuScreen`, `GameScreen`, `PauseScreen`, `GameOverScreen`.
- Render: sprites, animacoes, camera, mapa e luzes (`RayHandler`).
- HUD/UI: vida, XP, timer, wave e feedback visual.

### Controller (entrada e fluxo)

- Entrada do jogador (`InputProcessor`) para mover, mirar, pausar e interagir.
- Converte input em acoes no Model.
- Orquestra o fluxo do jogo e a atualizacao por frame (`delta`).

### Fluxo por frame

`Input -> Controller -> Model (Ashley + Box2D + AI) -> View`

### Estrutura de pastas

```text
core/src/main/java/io/github/AISurvivors/
  model/
    components/
    systems/
    physics/
    ai/
    state/
  view/
    screens/
    render/
    ui/
  controller/
    input/
    game/
```

### O que vai em cada pasta

- `model/`: estado do jogo e regras de negocio (nao renderiza nada).
- `model/components/`: componentes ECS puros de dados (`HealthComponent`, `VelocityComponent`, `XPComponent`).
- `model/systems/`: systems ECS com logica de atualizacao (`CombatSystem`, `SpawnSystem`, `ProgressionSystem`).
- `model/physics/`: integracao com `Box2D` (`World`, criacao de corpos, listeners de contato e filtros de colisao).
- `model/ai/`: logica com `gdx-ai` (steering, perseguicao, tomada de decisao dos inimigos).
- `model/state/`: estados globais da partida (wave atual, timer, score, dificuldade, pausa, game over).

- `view/`: camada visual, le o estado do Model e desenha na tela.
- `view/screens/`: telas do jogo (`MenuScreen`, `GameScreen`, `PauseScreen`, `GameOverScreen`).
- `view/render/`: render systems e pipeline grafico (sprites, animacoes, camera, `RayHandler` do `box2d-lights`).
- `view/ui/`: HUD e interface (vida, XP, wave, botoes, notificacoes e feedback visual).

- `controller/`: orquestracao entre input, estado do jogo e transicao de telas.
- `controller/input/`: mapeamento de controles (`InputProcessor`) e traducao de input em acoes do jogador.
- `controller/game/`: controle do fluxo da partida (iniciar, pausar, retomar, reiniciar, troca de telas e ciclo por frame).

### Regra pratica de separacao

- Se altera regra/estado do jogo, vai em `model/`.
- Se desenha algo na tela, vai em `view/`.
- Se conecta entrada do jogador com acao de jogo, vai em `controller/`.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
