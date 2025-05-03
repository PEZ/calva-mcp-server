# Calva Backseat Driver - Developer Guide

Welcome, Clojure cult member! This guide provides practical instructions for developing, testing, and contributing to the Calva Backseat Driver project. Pair programming, data-orientation, and functional thinking are encouraged throughout.

## Core Development Principles

This project follows these key principles:

### Functional Purity

- **Pure Functions**: Minimize side effects and make them explicit when necessary
- **Immutable Data**: Use immutable data structures and transformations
- **Explicit Context Passing**: Never reach into global state or app-db from helper functions
- **Controlled Side Effects**: Isolate side effects in clearly defined effect handlers
- **Error Handling**: Handle all predictable errors gracefully with proper fallbacks

### Code Style Conventions

#### Function Naming

- **Side Effects**: Functions that perform side effects should end with `!`
  - Example: `setup-socket-handlers!`
- **Promise-returning**: Functions that return promises should end with `+`
  - Example: `evaluate-code+`
- **Both**: Functions that both perform side effects and return promises should end with `!+`
  - Example: `ensure-port-file-dir-exists!+`

#### Function Privacy

- Functions should be private by default (`defn-`) unless they are part of the namespace API
- Every public function should have a docstring that starts with what it returns
- Docstrings should be terse and to the point

### One-Shot Changes

- Each PR should be a focused, atomic change solving a single, well-defined problem
- Avoid drive-by refactors unless they are necessary for the change at hand
- Include tests and documentation updates with functional changes

### Documentation-Driven Development

This project follows these principles:

1. Define the API and interfaces first
2. Document expected behaviors and edge cases
3. Implement against the documentation
4. Validate implementation against documentation
5. Update documentation based on implementation insights
6. Repeat

This approach ensures that we have a clear destination before starting to code, and that our implementation aligns with our design intentions.

## Interactive Development Workflow

The project leverages ClojureScript's interactive development capabilities:
- Use shadow-cljs for continuous hot reloading during development
- Apply changes to the running extension without restarting
- Utilize the REPL to experiment with and test code in real-time

### Complete Workflow

1. Start the REPL server:
   ```sh
   npm run watch
   ```
   - This only starts the server. The following steps must be performed by the human:
2. Connect to the REPL using Calva (<kbd>Ctrl+Alt+C Ctrl+Alt+C</kbd>)
3. Start the Extension Development Host (<kbd>F5</kbd> in VS Code)
4. Activate the extension in the development host (run the command "Calva Backseat Driver: Say hello!" from Command Palette)
5. Enjoy hot reloading and interactive development!

### Human/AI Collaboration Model

- **Initial development phase**: Human developer handles REPL connection and activation steps
- **As functionality matures**: AI gradually leverages the interactive environment
- **Eventually**: The AI assistant will use the very MCP server being developed

This creates a bootstrap process where the tool we're building ultimately empowers the AI collaborating on its development.

## MCP Development and Testing

### Using the MCP Inspector

The Model Context Protocol (MCP) inspector is a valuable tool for testing and debugging the MCP server implementation. It provides a GUI to interact with the server and validate responses against the schema.

To start the MCP inspector:

```sh
npx @modelcontextprotocol/inspector node ./dist/calva-backseat-driver.js ./test-projects/mini-deps
```

This will:
1. Launch a web interface on some localhost port
2. Connect to your MCP server implementation
3. Provide interactive tools to test initialize, ping, tools/list, and tools/call operations
4. Display validation errors when responses don't conform to the schema

The inspector is particularly useful when:
- Testing new tool implementations
- Validating schema compliance
- Debugging connection issues
- Monitoring request/response cycles

### MCP Schema Reference

All responses must conform to the official MCP schema. The current implementation should follow the 2024-11-05 schema version, which can be found at:

```
https://raw.githubusercontent.com/modelcontextprotocol/modelcontextprotocol/refs/heads/main/schema/2024-11-05/schema.ts
```

Common schema validation issues to watch for:
- Response result formats must match expected structures (e.g., ping expects an empty object result)
- Tool definitions must include required fields (name, description, inputSchema)
- Initialize response must include valid capabilities
- tools/call responses must return content in the correct format

When making changes to the server implementation, always validate them with the MCP inspector before committing.

## End-to-End (e2e) Testing

To validate the extension in a real VS Code environment, use the provided Babashka task for e2e tests:

```sh
bb run-e2e-tests-ws
```

- The test runner will automatically download and use VS Code Insiders (it does not need to be installed).
- **Important:** VS Code Insiders must not be running when you start the e2e tests, or the test runner will fail.

This will:
- Set up a test workspace
- Launch VS Code with the extension and Joyride installed
- Run all e2e tests in `.joyride/src` using the Joyride scripting environment
- Report results in the terminal

Task definition: `bb.edn`
Test runner logic: `e2e-test-ws/launch.js`, `e2e-test-ws/runTests.js`

> Use this workflow before merging or publishing to ensure the extension works as expected in a real VS Code environment.

## Testing Strategy

- **Unit tests**: Write tests for core functions (shadow-cljs automatically discovers and runs tests in namespaces ending with `-test`)
- **Integration tests**: Test API endpoints and component interactions
- **End-to-end tests**: Use the Babashka task as described above for full workflow validation
- **Property-based tests**: Consider for robust behavior validation where appropriate

**Error Handling Tests**:
- Include test cases for expected error conditions
- Validate that errors are handled gracefully
- Ensure that systems degrade predictably under failure

## Commit and Documentation Practices

- Make small, frequent commits with descriptive messages that tell the story of development
- Keep code and documentation changes in sync
- Ensure that each commit represents a coherent unit of work
- Document design decisions and their rationale
- When making significant architectural choices, update relevant documentation
- Be super aware of when a decision point is reached

## Decision Logging

For significant design or implementation decisions:
1. Document the problem being solved
2. Outline the options considered
3. Explain the reasoning behind the chosen solution
4. Update relevant documentation to reflect the decision

This practice creates an architectural decision record that helps future contributors understand why things are implemented in certain ways.

## Deployment and Distribution

- Automated publishing workflow is configured in the project
  - It includes linting, formatting check, unit testing and end-to-end testing
- The extension will be available through VS Code Marketplace
- Distribution occurs through standard VS Code extension mechanisms
- Version compatibility information will be included in documentation

## References

- For architectural and protocol details, see `dev/MCP_OVERVIEW.md` and `dev/EVENT_LOOP_ARCHITECTURE.md`
- For project requirements and philosophy, see `dev/PROJECT_REQUIREMENTS.md`
- For template-based setup and onboarding, see `doc/TEMPLATE_README.md`

## More to Come

Expand this guide with additional workflows, troubleshooting, and contributor tips as the project evolves.
