# Calva MCP Server - Project Requirements Document

## 1. Project Overview

### 1.1 Purpose
The Calva MCP Server extension provides a Model Context Protocol server for Calva, enabling AI assistants and language models to interact with Calva's REPL and other functionality. This creates a powerful synergy between Clojure's REPL-driven development model and AI-assisted coding.

**Tagline: Make CoPilot an Interactive Programmer**

### 1.2 Key Objectives
- Create a Model Context Protocol (MCP) server that exposes Calva's API
- Enable AI coding assistants to leverage Calva's REPL connection
- Provide a seamless experience for Clojure developers using AI tools
- Maintain the functional and data-oriented philosophy of Clojure

### 1.3 MVP Scope
- REPL code evaluation capabilities via the Calva API
- Basic error handling and response formatting
- Protocol implementation for AI tool communication

## 2. Technical Specifications

### 2.1 Architecture Overview
The extension will:
1. Connect to Calva's Extension API
2. Implement the Model Context Protocol server
3. Provide translation between MCP requests and Calva API calls
4. Return formatted results to the requesting AI

### 2.2 Dependencies
- VS Code Extension API
- Calva Extension API
- Model Context Protocol specification

### 2.3 API Design Principles
- Pure functions where possible
- Immutable data structures
- Clear separations of concerns
- Descriptive function and parameter names
- Consistent error handling

## 3. API Specification (MVP)

### 3.1 REPL Evaluation Endpoint

```
POST /evaluate
```

#### Request:
```json
{
  "code": "string",          // Clojure/ClojureScript code to evaluate
  "session": "string",       // "clj" or "cljs" (optional, defaults to current)
  "namespace": "string",     // Target namespace (optional, defaults to current or user)
  "timeout": "number"        // Evaluation timeout in ms (optional)
}
```

#### Response:
```json
{
  "result": "string",        // The evaluation result
  "output": "string",        // stdout output (if any)
  "errorOutput": "string",   // stderr output (if any)
  "namespace": "string",     // The namespace after evaluation
  "success": "boolean"       // Whether the evaluation succeeded
}
```

### 3.2 Status Endpoint

```
GET /status
```

#### Response:
```json
{
  "connected": "boolean",    // Whether Calva is connected to a REPL
  "session": "string",       // Current session ("clj" or "cljs")
  "namespace": "string",     // Current namespace
  "version": "string"        // Version of the MCP server
}
```

## 4. Future Expansions

### 4.1 Planned API Extensions
- Document information (namespace retrieval, current forms)
- Editor ranges (current/enclosing/top-level forms)
- Pretty printing capabilities
- Command execution
- Document symbol provider

### 4.2 Long-term Vision
- Complete coverage of Calva's API
- Specialized AI assistants for Clojure development
- Integration with other Clojure development tools

## 5. Implementation Plan

### 5.1 Phase 1: MVP
- Implement basic MCP server infrastructure
- Connect to Calva's REPL API
- Create evaluate endpoint
- Implement status endpoint
- Basic error handling and logging

### 5.2 Phase 2: Enhanced Features
- Implement document information endpoints
- Add editor ranges endpoints
- Create pretty printing endpoints

### 5.3 Phase 3: Complete API
- Implement remaining Calva API endpoints
- Add comprehensive documentation
- Optimize performance
- Enhance error handling and feedback

## 6. MCP Protocol Implementation Details

### 6.1 Server Setup
The server will implement the Model Context Protocol specification, allowing AI assistants to:
- Discover available capabilities
- Request REPL evaluations
- Process results in a structured format

### 6.2 Command Structure
All MCP commands will follow a consistent structure:
- Command name (e.g., "evaluate", "status")
- Parameters (as a map/object)
- Response format (structured data)

### 6.3 Error Handling
Errors will be handled gracefully and returned in a structured format:
- Error code
- Human-readable message
- Suggestions for resolution (when applicable)
- Original context (when helpful for debugging)

## 7. Development Approach

### 7.1 Documentation Driven Development
Following the principles of Documentation Driven Development:
1. Define the API and interfaces first
2. Document expected behaviors and edge cases
3. Implement against the documentation
4. Validate implementation against documentation
5. Update documentation based on implementation insights
6. Repeat

Alongside this approach, the project will:
- Make frequent, small commits with descriptive messages
- Chain git commands (add, commit, push) for efficiency
- Keep code and documentation changes in sync
- Ensure that each commit represents a coherent unit of work
- Maintain history that clearly tells the story of development

### 7.2 Development Log
The project will maintain a detailed development log to:
- Track progress and decisions made during implementation
- Record challenges encountered and solutions applied
- Document insights gained throughout the development process
- Serve as a historical record for future reference
- Complement the PRD by documenting the actual path taken

The development log will be maintained in `dev/DEVELOPMENT_LOG.md` and updated regularly during development sessions.

### 7.3 Testing Strategy
- Unit tests for core functions
- Integration tests for API endpoints
- End-to-end tests for full workflows
- Property-based tests for robust behavior validation

### 7.4 Deployment and Distribution
- Available through VS Code Marketplace
- Clear installation and setup instructions
- Version compatibility information
- Updates managed through standard VS Code extension mechanisms