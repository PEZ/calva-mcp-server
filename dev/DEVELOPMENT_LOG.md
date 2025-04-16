# Calva MCP Server - Development Log

This document tracks the development progress of the Calva MCP Server extension. It serves as a historical record of decisions, challenges, and insights gained throughout the development process.

## 2024-04-13: Project Initialization

### Accomplishments
- Renamed extension from vsc-et template to calva-mcp-server
- Created directory structure for new namespace
- Updated package.json with appropriate metadata
- Successfully verified build and REPL connection
- Ran end-to-end tests to confirm correct renaming
- Created Project Requirements Document (PRD)
- Established decision log for tracking significant decisions ([DECISION_LOG.md](./DECISION_LOG.md))
- Created MCP overview document ([MCP_OVERVIEW.md](./MCP_OVERVIEW.md))
- Added tagline: "Make CoPilot an Interactive Programmer"

### Decisions
- Decided to follow Documentation Driven Development approach
- Opted to implement REPL evaluation capabilities as the MVP
- Determined that a development log would complement the PRD
- Created structured decision log for documenting decision-making process
- [Decided on MCP server implementation approach](./DECISION_LOG.md#2025-04-13-mcp-server-implementation-approach) (stdio/stdout transport)

### Insights
- The extension template provides a solid foundation for extension development
- Maintaining a development log alongside the PRD helps track both the destination and the journey
- Documentation-first approach encourages thoughtful API design before implementation
- Capturing decision rationale preserves context for future development
- Modex (Clojure MCP library) provides valuable insights for our implementation approach
- JSON-RPC 2.0 is the wire format for MCP, with specific message types for requests, responses, and notifications

### Next Steps
- Design core data structures for our MCP server implementation
- Implement basic stdio/stdout transport mechanism
- Create tool definitions for REPL evaluation
- Connect tool implementations to Calva's Extension API
- Test with an MCP client like Claude Desktop

## 2025-04-13: MCP Integration Approach Research

### Accomplishments
- Researched Model Context Protocol implementation approaches
- Studied the Modex Clojure MCP implementation for reference
- Investigated VS Code MCP server discovery mechanisms
- Created a spike implementation plan for VS Code MCP integration
- Updated decision log with transport mechanism considerations

### Decisions
- [Decided to implement an exploratory spike for MCP server integration](./DECISION_LOG.md#2025-04-13-mcp-server-integration-approach-for-vs-code) to determine the best approach for VS Code

### Insights
- VS Code's approach to MCP server discovery is still evolving
- Multiple transport options exist (stdio, HTTP, VS Code API)
- Each transport approach has distinct tradeoffs for our use case
- A spike implementation will provide concrete information about VS Code's MCP integration before committing to a specific approach
- We need to understand how GitHub Copilot discovers and interacts with MCP servers

### Next Steps
- Implement the exploratory spike code
- Test different approaches to MCP server registration with VS Code
- Document findings about VS Code's MCP integration mechanisms
- Make a final decision on our transport approach based on spike results

## 2025-04-13: Spike Implementation for MCP Server Integration

### Accomplishments
- Created directory structure for MCP implementation
- Implemented core MCP tools in a testable, VS Code-independent namespace
- Developed spike implementation that attempts multiple registration approaches
- Set up tests for core MCP functionality
- Updated shadow-cljs configuration to handle VS Code dependencies in tests
- Integrated with the Calva extension activation process
- Implemented exploration of Copilot's availability in the current environment

### Decisions
- Decided to separate core MCP functionality from VS Code-specific code
- Adopted a dependency injection approach for improved testability
- Used a structured registration attempt strategy to discover VS Code's MCP API

### Insights
- Testing VS Code extensions requires special handling of the vscode module
- Shadow-cljs configuration can be enhanced to properly mock external modules
- Interactive development with hot reloading provides rapid feedback
- The bootstrap process can enable the AI assistant to gradually gain capabilities:
  1. Human developer operates the REPL initially
  2. As functionality matures, AI gradually uses the interactive environment
  3. Eventually, the AI assistant will use the very MCP server being developed

### Next Steps
- Launch the extension in a development host
- Monitor logs to determine which MCP registration method succeeds
- Document findings about VS Code's MCP API
- Continue developing the MCP server with lessons learned from spike
- Implement actual REPL evaluation via Calva's API

## 2025-04-13: Development Workflow Refinement

### Accomplishments
- Clarified the complete interactive development workflow with explicit steps
- Updated project documentation to reflect the actual REPL connection process
- Enhanced AI assistant instructions with workflow awareness

### Insights
- The `npm run watch` command only starts the nREPL server but doesn't connect to it
- The complete development workflow requires explicit human steps:
  1. Starting the REPL server with `npm run watch`
  2. Connecting to the REPL using Calva
  3. Starting the Extension Development Host
  4. Activating the extension in the development host
  5. Only then is the extension running with hot reloading
- Tests may not automatically run in the shadow-cljs watch process without explicit REPL connection
- Clear communication about workflow state is essential for effective human-AI collaboration

### Next Steps
- Proceed with implementing the MCP server spike with proper workflow awareness
- Ensure the AI assistant explicitly requests human intervention when REPL connection is needed
- Continue to refine our development communication process

## 2025-04-13: Ex Framework Implementation Strategy

### Accomplishments
- Created Ex Architecture document based on the Ex framework from example projects
- Analyzed both the fleet-designer and todomvc example implementations
- Established a testing-first approach for the Ex framework implementation
- Determined the need for a "hello world" minimal implementation with tests

### Decisions
- Decided to implement a minimal version of the Ex framework before integrating with MCP
- Opted for a testing-first approach to enable autonomous development feedback
- Chose to separate the core Ex framework from MCP-specific implementations

### Insights
- Tests provide crucial autonomous feedback until REPL access is established
- The Ex framework from fleet-designer offers a more powerful architecture than the todomvc version:
  - Action dispatches enable more complex behavior composition
  - Better separation of concerns through dedicated domain namespaces
  - Consistent naming convention with the `ex/` prefix
  - More sophisticated effect types including timing-related ones
- A minimal "hello world" implementation can validate the architecture before adding complexity
- Test-driven development ensures the implementation meets requirements even without REPL interaction

### Next Steps
- Create the core Ex framework modules (ax.cljs, fx.cljs, event_handler.cljs)
- Implement a simple "hello world" action and effect
- Write comprehensive tests for the minimal implementation
- Once validated, expand to implement MCP-specific actions and effects
- Integrate with Calva's API for REPL evaluation

## 2025-04-13: Testing Workflow Clarifications

### Insights
- Shadow-cljs serves as the test runner; no separate test runner file is needed
- The shadow-cljs configuration automatically picks up test namespaces ending with "-test"
- Clojure-lsp automatically adds namespace declarations to new files after a short delay
- When creating new files, either:
  - Pause briefly after file creation to allow clojure-lsp to add the namespace, then modify it
  - Or edit the file after creation and handle any duplicate namespace declarations
- Clojure file naming convention requires:
  - Namespaces use kebab-case with hyphens (e.g., `calva-mcp-server.ex.ax-test`)
  - File paths use snake_case with underscores (e.g., `calva_mcp_server/ex/ax_test.cljs`)
  - Mismatched conventions will result in compilation errors

### Next Steps
- Continue implementing tests for the Ex framework components
- Run tests using shadow-cljs to validate the implementation
- Proceed with implementing MCP-specific components once the core framework is validated

---
*Note: This log will be updated regularly during development sessions to track progress, decisions, and insights.*