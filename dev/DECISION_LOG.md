# Calva MCP Server - Decision Log

This document tracks significant decisions made during the development of the Calva MCP Server extension. Each decision is documented with context, options considered, and the rationale for the chosen path.

## Decision Format

Each decision is documented using the following format:

```
## [YYYY-MM-DD] Decision Title

### Context
Brief description of the situation, problem, or opportunity that requires a decision.

### Options Considered
1. Option A
   - Pros: ...
   - Cons: ...
2. Option B
   - Pros: ...
   - Cons: ...
3. Option C
   - Pros: ...
   - Cons: ...

### Decision
Description of the chosen option and the primary reasons for selecting it.

### Consequences
Expected outcomes, both positive and negative, resulting from this decision.
```

## [2025-04-13] MCP Server Implementation Approach

### Context
After researching the Model Context Protocol and examining existing implementations like Modex, we need to decide on our implementation approach for the Calva MCP Server. Key considerations include the transport mechanism, message format, and how to structure our server code.

### Options Considered
1. Use the stdio/stdout transport as implemented in Modex
   - Pros: Simpler implementation, works with Claude Desktop and similar clients
   - Cons: Might require specific configuration for different clients

2. Implement Server-Sent Events (SSE) transport
   - Pros: Works in more restricted networks, potential for better streaming
   - Cons: More complex, requires additional proxy for some clients

3. Hybrid approach supporting both transport mechanisms
   - Pros: Maximum compatibility with clients
   - Cons: Higher implementation complexity, more code to maintain

#### Deeper consideration

We considered folling Modex's example with stdout/stdio transport.

- We would implement the stdio/stdout transport initially, following the pattern established by Modex, but structure our code to potentially support SSE in the future if needed.
- For tool definition, we'd create a similar abstraction to Modex's Tool and Parameter records, but adapted for ClojureScript and the VS Code extension environment.

We saw these consequences:

- This approach provides a simple path to a working MCP server that can be used with Claude Desktop and GitHub Copilot
- We'd need to document the configuration process for connecting clients to our server
- The code structure would need to accommodate potential future transport mechanisms

### Decision

Nothing conclusive.

### Consequences

We realized we need to consider what requirements VS Code brings to the table

---

## [2025-04-13] MCP Server Integration Approach for VS Code

### Context
We need to determine how our Calva MCP Server should integrate with VS Code and AI assistants like GitHub Copilot. The transport mechanism (stdio, HTTP, VS Code APIs) and discovery method are crucial architectural decisions that will impact our implementation approach.

### Options Considered
1. Standalone stdio/stdout transport (like Modex)
   - Pros: Simple implementation pattern, works with many MCP clients
   - Cons: Doesn't align well with VS Code extension architecture, requires clients to spawn processes

2. HTTP/WebSockets server
   - Pros: Standard for networked services, well-supported in ClojureScript
   - Cons: Requires port management, security considerations, potential VS Code restrictions

3. VS Code Extension API integration
   - Pros: Native integration with VS Code, seamless for GitHub Copilot
   - Cons: Relies on evolving APIs, may limit compatibility with non-VS Code clients

4. Hybrid approach (VS Code API + optional HTTP)
   - Pros: Maximum flexibility, works with both VS Code and external clients
   - Cons: More complex implementation, maintains two integration paths

### Decision
We will implement a spike to explore VS Code native integration first, focusing on how extensions register as MCP servers with GitHub Copilot. The spike will help us understand the current state of VS Code's MCP integration mechanisms before committing to a specific approach.

Based on findings from the spike, we'll decide whether to:
1. Use VS Code Extension API exclusively
2. Implement a standalone server (stdio/HTTP)
3. Create a hybrid solution

### Consequences
- This approach prioritizes understanding the evolving VS Code MCP integration
- The spike will provide practical insights to guide our architectural decisions
- We may need to be flexible as VS Code's MCP support continues to evolve
- Documentation will be vital to capture our findings for future development

---

*Note: As decisions are made throughout the development process, they will be added to this log and referenced in the development log.*