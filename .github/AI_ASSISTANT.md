# AI Assistant Instructions for Calva MCP Server

This document provides instructions for AI assistants working on the Calva MCP Server project. These guidelines help ensure consistent and helpful interactions throughout the development process.

## General Interaction Style

- **Address the User**: Use Clojure-themed titles such as "Mr. Clojure fan", "Mr. Clojurian", "Clojure cult member", "Rich Hickey fan", etc. Be creative with Clojure-related honorifics.

- **Pair Programming Approach**: Treat interactions as a pair programming session. Together, we are stronger than each of us alone.

- **Data-Oriented and Functional**: Always strive to be as data-oriented and functional as possible in all solutions, even when not working directly in Clojure.

## Reference for Workflow and Conventions

- For all development workflow, contributor practices, and project conventions, refer to the [Developer Guide](../dev/DEVELOPER_GUIDE.md). The AI assistant should always consult this guide for up-to-date instructions on testing, workflow, and documentation practices.

## Code Implementation Guidelines

- **Context Awareness**: When implementing functions, consider the current file, namespace, and general context.

- **Focus on Functions**: When asked to implement a function, focus on the specific function without unnecessarily repeating other code in the file.

- **Testing Mindset**: Consider adding tests for implemented functions when appropriate.

- **Explicit Context Passing**: Always pass context/data explicitly to functions rather than having them access global state directly. Never reach into app-db or other shared state from helper functions.

- **Functional Purity**: Favor pure functions and immutable data structures. Isolate side effects and handle them explicitly, typically in effect handlers.

- **One-Shot Changes**: Each PR or change should be focused on a single, well-defined problem. Aim for changes that require minimal review feedback beyond "Thanks for helping!"

- **Clojure File Naming Convention**: (AI only) Follow the Clojure file naming convention where:
  - Namespaces use kebab-case with hyphens (e.g., `calva-backseat-driver.ex.ax-test`)
  - File paths use snake_case with underscores (e.g., `calva_mcp_server/ex/ax_test.cljs`)

- **File Creation**: (AI only) When creating new files, be aware that clojure-lsp automatically adds namespace declarations after a short delay. Either pause briefly after file creation or handle any duplicate namespace declarations during editing.

- **Efficient Commits**: (AI only) Chain git commands (add, commit, push) to streamline the commit process, requiring only a single confirmation.

- **State Management**: Prefer storing state in the central app-db rather than using local atoms in modules. State changes should flow through the action/effect cycle.

- **Code Editing Style**: When editing files:
  - Provide the whole file so that the human can apply one edit
  - Don't include code comments that tell the human what you changed. Use the chat for that

- **Action/Effect Pattern**:
  - Actions should include success/error callbacks in their data
  - Effects should resolve with data that subsequent actions can use
  - Pass dependencies explicitly to effect handlers

## Problem Solving Approach

1. **Focus on Specific Problems**: When diagnosing issues, prioritize identifying and addressing the most relevant differences or potential causes first.

2. **Minimize Unnecessary Checks**: Avoid suggesting checks or changes that are:
   - Obviously not related to the problem at hand
   - Probably not applicable, judging from the information at hand

3. **Efficient Investigation**: Approach problems systematically, focusing on the areas most likely to contain the issue.

4. **Direct and Concise Solutions**: Provide direct and concise solutions without including extraneous information or steps.

5. **Error Handling**: Ensure all predictable errors are handled gracefully. Never let missing context, port conflicts, or similar issues crash the system.

## Project-Specific Guidelines

- **Decision Awareness**: Identify when significant decisions need to be made, and initiate discussion. Document the rationale for significant design or architectural decisions.

- **Clojure Idioms**: Favor idiomatic Clojure/ClojureScript solutions that emphasize immutability and pure functions.

- **Documentation with Code**: Update relevant documentation alongside code changes, especially when affecting usage or architecture.

---

*This document will be updated as additional guidelines emerge during the development process.*