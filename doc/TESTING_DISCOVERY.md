# Testing AI Assistant Discovery

This document provides instructions for testing whether AI assistants like GitHub Copilot and Claude can discover and use your MCP server.

## Prerequisites

- The Calva MCP Server is running on http://localhost:3000
- You have access to an AI assistant that supports the Model Context Protocol (MCP)

## Testing with GitHub Copilot

To test if GitHub Copilot can discover and use your MCP server:

1. Make sure your MCP server is running with `(start-server)` in your REPL
2. In a conversation with GitHub Copilot, provide these instructions:

```
I have an MCP server running at http://localhost:3000. Can you:
1. Check its status endpoint (/status)
2. Discover its capabilities (/capabilities)
3. Use the hello-world tool (/hello)
```

3. Observe whether GitHub Copilot can successfully:
   - Connect to the server
   - Retrieve the status
   - Find the available tools
   - Call the hello-world tool

## Testing with Claude

To test if Claude can discover and use your MCP server:

1. Make sure your MCP server is running with `(start-server)` in your REPL
2. In a conversation with Claude, provide these instructions:

```
I have an MCP server running at http://localhost:3000. Can you:
1. Check its status endpoint (/status)
2. Discover its capabilities (/capabilities) 
3. Use the hello-world tool (/hello)
```

3. Observe whether Claude can successfully:
   - Connect to the server
   - Retrieve the status
   - Find the available tools
   - Call the hello-world tool

## Expected Results

Successful discovery will show the AI assistant:
1. Connecting to your server
2. Reading the status information
3. Discovering the hello-world tool
4. Using the tool and returning "Hello, World!"

## Troubleshooting

If the AI assistant cannot connect:
- Verify the server is running (`(start-server)` in your REPL)
- Check that port 3000 is accessible
- Ensure the endpoints return proper JSON (test with curl)
- Check for CORS issues if the AI attempts to connect directly via browser