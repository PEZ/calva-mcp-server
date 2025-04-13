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

---
*Note: This log will be updated regularly during development sessions to track progress, decisions, and insights.*