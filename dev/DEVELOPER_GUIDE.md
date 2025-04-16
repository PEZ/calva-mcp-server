# Calva MCP Server - Developer Guide

Welcome, Clojure cult member! This guide provides practical instructions for developing, testing, and contributing to the Calva MCP Server project. Pair programming, data-orientation, and functional thinking are encouraged throughout.

## End-to-End (e2e) Testing

To validate the extension in a real VS Code environment, use the provided Babashka task for e2e tests:

```sh
bb run-e2e-tests-ws
```

This will:
- Set up a test workspace
- Launch VS Code with the extension and Joyride installed
- Run all e2e tests in `.joyride/src` using the Joyride scripting environment
- Report results in the terminal

Task definition: `bb.edn`
Test runner logic: `e2e-test-ws/launch.js`, `e2e-test-ws/runTests.js`

> Use this workflow before merging or publishing to ensure the extension works as expected in a real VS Code environment.

## Development Workflow (Summary)

1. Start the REPL server:
   ```sh
   npm run watch
   ```
2. Connect to the REPL using Calva (human action)
3. Start the Extension Development Host (F5 in VS Code, human action)
4. Activate the extension in the development host (human action)
5. Enjoy hot reloading and interactive development!

## More to Come

Expand this guide with additional workflows, troubleshooting, and contributor tips as the project evolves.
