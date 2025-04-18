# Make CoPilot an Interactive Programmer

> Calva MCP Server

[![VS Code Extension](https://img.shields.io/visual-studio-marketplace/v/betterthantomorrow.calva-mcp-server)](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-mcp-server)
[![Issues](https://img.shields.io/github/issues/BetterThanTomorrow/calva-mcp-server)](https://github.com/BetterThanTomorrow/calva-mcp-server/issues)
[![License](https://img.shields.io/github/license/BetterThanTomorrow/calva-mcp-server)](https://github.com/BetterThanTomorrow/calva-mcp-server/blob/master/LICENSE.txt)

A Model Context Protocol server extension for [Calva](https://calva.io), the Clojure/ClojureScript extension for VS Code, enabling AI assistants to harness the power of the REPL.

## Why Calva MCP Server?

"I wish Copilot could actually run my Clojure code instead of just guessing what it might do."

The Calva MCP Server transforms AI coding assistants from static code generators into interactive programming partners by giving them access to your REPL. (Please be mindful about the implications of that before you start using it.)

### Turn your AI Agent into an Interactive Programming partner

Tired of AI tools that write plausible-looking Clojure that falls apart at runtime? Calva MCP Server lets your AI assistant:

- **Evaluate code in your actual environment** - No more "this might work" guesses
- **See real data structures**, not just predict their shape
- **Test functions with real inputs** before suggesting them
- **Debug alongside you** with access to runtime errors
- **Learn from your codebase's actual behavior**

### For Clojurians who value Iiteractive Programming

As Clojure developers, we know the REPL isn't just a console - it's the center of our workflow. Now your AI assistant can join that workflow, understanding your data and functions as they actually exist, not just as they appear in static code.

## Key Features

- Seamless integration between GitHub Copilot and your Calva REPL
- AI-driven code evaluation with full access to your project's runtime (⚠️)
- Interactive data exploration for smarter code suggestions
- REPL-powered debugging assistance
- Works with your existing Clojure/ClojureScript projects

## Getting Started

### Prerequisites

- [VS Code](https://code.visualstudio.com/)
- [Calva](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva)
- An AI coding assistant (e.g., GitHub Copilot)
- Any Clojure environment dependencies your project has (e.g. Clojure, Babashka, etc)

### Installation

1. Install Calva MCP Server from the Extensions pane in VS Code
1. Start the Calva MCP Server
1. Add the MCP server from the CoPilot chat in Agent mode (further instructions TBD)
1. Stop the Calva MCP Server (it's a habit to consider, at least)

### Using

1. Connect Calva to your Clojure/ClojureScript project
1. Start the Calva MCP Server
3. Start using your AI Agent with REPL superpowers!

## How It Works

Calva MCP Server implements the [Model Context Protocol](https://modelcontextprotocol.io) (MCP), creating a bridge between AI assistants and your REPL:

1. When your AI assistant needs to understand your code better, it can execute it in your REPL
2. The results flow back to the AI, giving it insight into actual data shapes and function behavior
3. This creates a powerful feedback loop where suggestions improve based on runtime information
4. You remain in control of this process, benefiting from an AI partner that truly understands your running code

```mermaid
flowchart TD
    subgraph InteractiveProgrammers["Interactive Programmers"]
        User([You])
        AIAgent([AI Agent])
        User <--> AIAgent
    end

    subgraph VSCode["VS Code"]

        MCP["Calva MCP Server"]

        subgraph Calva["Calva"]
            REPLClient["REPL Client"]
        end

        subgraph Project["Clojure Project"]

            subgraph RunningApp["Running Application"]
                SourceCode["Source Code"]
                REPL["REPL"]
            end
        end
    end

    User --> SourceCode
    User --> Calva
    REPLClient --> REPL
    AIAgent --> SourceCode
    AIAgent --> MCP
    MCP --> Calva

    classDef users fill:#ffffff,stroke:#63b132,stroke-width:1px,color:#63b132;
    classDef programmers fill:#63b132,stroke:#000000,stroke-width:2px,color:#ffffff;
    classDef vscode fill:#0078d7,stroke:#000000,stroke-width:1px,color:#ffffff;
    classDef calva fill:#df793b,stroke:#ffffff,stroke-width:1px,color:#ffffff;
    classDef highlight fill:#ffffff,stroke:#000000,stroke-width:1px,color:#000000;
    classDef dark fill:#333333,stroke:#ffffff,stroke-width:1px,color:#ffffff;
    classDef repl fill:#5881d8,stroke:#ffffff,stroke-width:1px,color:#ffffff;
    classDef running fill:#63b132,stroke:#ffffff,stroke-width:1px,color:#ffffff;
    classDef project fill:#888888,stroke:#ffffff,stroke-width:1px,color:#ffffff;

    class User,AIAgent users;
    class VSCode vscode;
    class Calva,MCP calva;
    class REPLClient repl;
    class Project project;
    class SourceCode dark;
    class RunningApp running;
    class REPL repl;
    class InteractiveProgrammers programmers;
```

## Contributing

Contributions are welcome! See our [Contribution Guidelines](CONTRIBUTING.md) for more information.

## License


## License 🍻🗽

[MIT](LICENSE.txt)


## Sponsor my open source work ♥️

You are welcome to show me you like my work using this link:

* https://github.com/sponsors/PEZ