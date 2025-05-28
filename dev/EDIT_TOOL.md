# Form-Aware Edit Tool - Calva Backseat Driver

## Overview

This document describes the implemented form-aware editing tool that leverages Calva's ranges API to provide semantic Clojure editing capabilities for AI agents. The tool addresses the limitation of line-based editing (like `f1e_edit_file`) which doesn't align with Clojure's form-based paradigm.

**Core Value Proposition**: Enable AI agents to edit Clojure code semantically by operating on forms rather than lines, providing automatic bracket balancing, proper formatting, and structural awareness.

## Current Implementation Status

**✅ VS CODE**: The `replace_top_level_form` tool is fully implemented and available in VS Code Language Model integration.

**❌ MCP SERVER**: The `replace_top_level_form` tool is not yet implemented in the MCP server.

**⚠️ LIMITATION IDENTIFIED**: Line comments are not structural forms, causing issues when AI agents try to add or modify top-level comments using the form-aware tool. A dedicated `insert_top_level_comment` tool is needed.

## Problem Statement

### Previous State: Line-Based Editing
- Existing MCP/LSP tools like `f1e_edit_file` operate on line ranges
- Risk of malformed code after edits
- Clojure code is form-based, not line-based
- No semantic understanding of code structure
- **Line comments cannot be handled structurally** - they exist outside the form paradigm

### Current State: Form-Aware Editing ✅
- Edit complete forms as semantic units
- Automatic bracket balancing via Parinfer integration
- Rich comment form support for AI experimentation
- Leverages Calva's existing ranges and editor infrastructure
- **Gap**: No tool for handling top-level line comments (non-structural text)

## Implementation Details

### Core API: `replace_top_level_form` ✅

```clojure
;; Actual implemented interface
(defn apply-form-edit-by-line-with-text-targeting
  "Apply a form edit by line number with text-based targeting for better accuracy.
   Searches for target-line text within a 2-line window around the specified line number."
  [file-path line-number target-line new-form])

;; Legacy interface (still supported)
(defn apply-form-edit-by-line
  "Apply a form edit by line number instead of character position.
   This is the preferred approach for AI agents as they can see and reason about line numbers."
  [file-path line-number new-form])
```

**Parameters:**
- `file-path` (string): Absolute path to the Clojure file
- `line` (integer): Line number (1-indexed) to identify the target form
- `target-line` (string, optional but recommended): The exact text content of the line being targeted (used for validation and accuracy)
- `new-form` (string): The replacement form code

**Returns:**
- Success: Form replacement result with `actual-line-used` when text targeting is used
- Error: Error details with context

**Enhanced Targeting Algorithm:**
1. If `target-line` is provided, search for that exact text (trimmed) within 2 lines above and below the specified line
2. Use the line number where the text is found for the actual form targeting
3. If text is not found, return an error explaining the mismatch
4. If `target-line` is not provided, fall back to the original line-based approach

### Needed API: `insert_top_level_comment` ❌

**Problem**: Line comments (`;; comment text`) are not structural forms and cannot be handled by the form-aware editing tool. AI agents struggle when trying to add documentation or section comments.

**Proposed Solution**: A line-based tool for inserting top-level line comments, consistent with the proven line-based approach used in `replace_top_level_form`.

```clojure
;; Proposed interface - line-based like the form tool
(defn insert-comment-at-line
  "Insert a top-level line comment at the specified line number.
   Uses the same line-based approach that works well for AI agents."
  [file-path line-number comment-text insert-mode])
```

**Parameters:**
- `file-path` (string): Absolute path to the Clojure file
- `line-number` (integer): Line number (1-indexed) for positioning
- `comment-text` (string): The comment text (without the `;; ` prefix, which will be added automatically)
- `insert-mode` (string): "before" (insert before the line) or "after" (insert after the line)

**Returns:**
- Success: Comment insertion result with final line position
- Error: Error details with context

**Design Principles:**
- **Line-based positioning** - AI agents can see and reason about line numbers
- Automatically adds proper comment prefixes (`;;` for single line, `;; ` for text)
- Handles spacing around comments (blank lines before/after when appropriate)
- Works with existing indentation and formatting
- **Consistent with `replace_top_level_form` approach** that's proven to work

### No Insertion Tool Needed

~~The `replace_top_level_form` tool description instructs how to use it for insertion operations, making a separate `insert_text` tool unnecessary at this time.~~

**Update**: While form insertion can be handled by `replace_top_level_form`, **line comment insertion requires a dedicated tool** due to the non-structural nature of comments.

### Implementation Architecture

#### Core Functions (in `api.cljs`) ✅

1. **Form Detection** (Implemented)
   ```clojure
   (defn- get-ranges-form-data-by-line [file-path line-number ranges-fn-key])
   (defn- get-range-and-form [{:keys [ranges-object vscode-document]}])
   ```

2. **Edit Operations** (Implemented)
   ```clojure
   (defn- edit-replace-range [file-path vscode-range new-text])
   ```

3. **Form Editing Functions** (Implemented)
   ```clojure
   (defn apply-form-edit-by-line [file-path line-number new-form])
   ```

#### Tool Registration ✅

**VS Code Language Model Tool Registration:**
```clojure
;; In tools.cljs - Implemented
(vscode/lm.registerTool "replace_top_level_form" (#'ReplaceTopLevelFormTool dispatch!))
```

**MCP Tool Definition:**
```clojure
;; In requests.cljs - Not yet implemented in MCP server
{:name "replace_top_level_form"
 :description "Replace a top-level form in a Clojure file with semantic awareness"
 :inputSchema {:type "object"
               :properties {:filePath {:type "string"}
                           :line {:type "integer"}
                           :newForm {:type "string"}}
               :required ["filePath" "line" "newForm"]}}

;; Proposed MCP tool for line comments
{:name "insert_comment_at_line"
 :description "Insert a top-level line comment in a Clojure file at a specific line (for non-structural text)"
 :inputSchema {:type "object"
               :properties {:filePath {:type "string"}
                           :lineNumber {:type "integer"}
                           :commentText {:type "string"}
                           :insertMode {:type "string"
                                       :enum ["before" "after"]}}
               :required ["filePath" "lineNumber" "commentText" "insertMode"]}}
```

### Technical Implementation Details

#### Form Type Detection (Internal)
While the public API only exposes top-level form operations, the implementation uses Calva's ranges API to detect form types:

```clojure
;; Internal form type mapping
:currentTopLevelForm     ; Top-level forms
:currentForm             ; Any form at cursor
:currentFunction         ; Function calls
:rangeForDefun           ; Function definitions
```

#### Bracket Balancing Integration
```clojure
(defn apply-form-edit-by-line [file-path line-number new-form]
  (p/let [balance-result (some-> (parinfer/indentMode new-form #js {:partialResult true})
                                 (js->clj :keywordize-keys true))
          balanced-form (if-let [error (:error balance-result)]
                          (do (js/console.warn "[Server] Parinfer error:" error)
                              new-form)
                          (:text balance-result))
          form-data (get-ranges-form-data-by-line file-path line-number :currentTopLevelForm)
          [vscode-range _form-string] (:ranges-object form-data)]
    (edit-replace-range file-path vscode-range balanced-form)))
```

**Note**: The implementation uses Parinfer's `indentMode` for bracket balancing and integrates with Calva's editor API (`edit-replace-range`) for undo/redo support through VS Code's standard edit operations.

#### Post-Edit Diagnostics Integration ⚠️
**Status**: Implemented but has timing issues that need fixing.

**Current Issues**:
- Diagnostics are checked too quickly after edit, missing language server updates
- Only returns post-edit diagnostics, should include before/after comparison
- Returns all diagnostics instead of filtering to relevant ones (clj-kondo)

**Benefits for AI Development**:
- AI gets immediate feedback on edit quality
- Reduces need for separate diagnostic checking
- Enables iterative fixing of issues within single conversation
- Provides context for understanding why edits might have failed

#### Error Handling Strategy
```clojure
;; Validation pipeline
1. Check file exists and is readable
2. Validate position is within file bounds
3. Validate new-form syntax (basic parsing)
4. Apply bracket balancing
5. Perform edit operation
6. Return detailed success/error information
```

**Note on Form Targeting**: Calva's ranges API will almost always identify a valid form at any given position. However, whether it's the form the AI intended to target cannot be programmatically validated. The AI must ensure it provides accurate line coordinates that correspond to the desired form.

### Rich Comment Form Support

Provide the tool description for `replace_top_level_form` with information so that the AI correctly uses it inside rich comment forms (where Calva will treat everything inside a `(comment ...)` as being top level.)

## Integration Points

### VS Code Language Model Integration ✅
- Tool added to `src/calva_backseat_driver/integrations/vscode/tools.cljs`
- Registered with Language Model API
- Follows existing tool registration pattern
- **Missing**: `insert_comment_at_line` tool registration

### MCP Server Integration ❌
- Tool definition not yet added to `src/calva_backseat_driver/mcp/requests.cljs`
- Request handler needs implementation following existing pattern
- Needs addition to tool registry in server initialization
- **Missing**: Both `replace_top_level_form` and `insert_comment_at_line` tools

### Action/Effect System Integration ❌
**Note**: The implementation bypasses the action/effect system and calls API functions directly from tools.

## Usage Examples

### Enhanced Form Replacement with Text Targeting
```clojure
;; Replace a function definition with improved accuracy
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 23,  // Line number (1-indexed) where you think the target form is
  targetLine: "(defn old-function [x]",  // Exact text at the line for validation
  newForm: "(defn new-function [x y] (+ x y))"
})

// Response includes success status and actual line used
{
  success: true,
  actual-line-used: 24,  // The tool found the text at line 24 instead of 23
  // ... diagnostics and other details
}
```

### Top-Level Comment Insertion (Proposed)
```clojure
;; Add a comment before a specific function
insert_comment_at_line({
  filePath: "/path/to/file.clj",
  lineNumber: 45,  // Line number where the function starts
  insertMode: "before",  // Insert before this line
  commentText: "Helper functions for data processing"
})

// Add a comment after the namespace declaration
insert_comment_at_line({
  filePath: "/path/to/file.clj",
  lineNumber: 3,  // Line with ns declaration
  insertMode: "after",  // Insert after this line
  commentText: "This module handles user authentication\nand session management"
})

// Add a comment at the top of the file (before line 1)
insert_comment_at_line({
  filePath: "/path/to/file.clj",
  lineNumber: 1,
  insertMode: "before",
  commentText: "Core business logic functions"
})
```

### Combined Workflow Example
```clojure
;; 1. First, add documentation comment
insert_comment_at_line({
  filePath: "/path/to/file.clj",
  lineNumber: 15,
  insertMode: "before",
  commentText: "Updated to handle edge cases better"
})

// 2. Then, replace the actual function (line number stays same since comment inserted before)
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 15,  // Same line number - comment was inserted before it
  targetLine: "(defn process-data [input]",
  newForm: "(defn process-data [input]\n  (when (valid-input? input)\n    (transform input)))"
})
```

### Fallback to Legacy Line-Based Targeting
```clojure
;; Replace without text targeting (legacy mode)
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 23,  // Line number (1-indexed) where the target form is located
  newForm: "(defn new-function [x y] (+ x y))"
})

// Response includes success status
{
  success: true,
  // ... implementation details
}
```

### Error Handling for Text Mismatch
```clojure
;; When target text is not found
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 23,
  targetLine: "(defn wrong-function [x]",  // This text doesn't exist near line 23
  newForm: "(defn new-function [x y] (+ x y))"
})

// Response includes error details
{
  success: false,
  error: "Target line text not found. Expected: '(defn wrong-function [x]' near line 23"
}
```

### Rich Comment Experimentation
```clojure
;; Add experiment to rich comment with text targeting
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 45,  // Line number within the comment form
  targetLine: "(def old-test-data [1 2])",  // Exact text for validation
  newForm: "(def test-data [1 2 3])"
})
```

## Implementation Phases

### Phase 1: Core Implementation ✅ + ❌
1. ✅ Implement `replace-top-level-form` function
2. ✅ Add VS Code Language Model tool registration
3. ✅ Integrate bracket balancing
4. ✅ Add error handling and validation
5. ❌ Add MCP tool registration
6. **❌ URGENT: Implement `insert-comment-at-line` function**
7. **❌ URGENT: Add `insert_comment_at_line` tool registration for VS Code**

### Phase 2: Enhanced Features
1. Rich comment form support (working via existing implementation)
2. **Fix diagnostic timing and filtering issues**:
   - **URGENT**: Add proper delay/polling for language server diagnostic updates
   - **Return both `:diagnostics-before-edit` and `:diagnostics-after-edit`**
   - **Filter diagnostics to only clj-kondo sources** (exclude other language servers)
   - Consider using VS Code's `onDidChangeDiagnostics` event for more reliable timing
3. Enhance error reporting and diagnostics
4. **Complete MCP server integration for both tools**

### Phase 3: Advanced Capabilities
1. Batch operations support
2. Form-aware refactoring operations
3. Integration with existing ranges API extensions
4. Performance optimizations for large files
5. **Enhanced comment tools** (multi-line comments, comment blocks, etc.)

## Security Considerations

### File Access
- Validate file paths are within workspace
- Check file permissions before modification
- Sanitize input to prevent path traversal

### Code Validation
- Basic syntax checking before applying edits
- Bracket balancing verification
- Option to preview changes before applying

### User Control
- Respect existing REPL evaluation security model
- Provide workspace-level configuration options
- Clear user feedback for all edit operations

## Testing Strategy

### Unit Tests
- Form detection accuracy
- Bracket balancing integration
- Error handling scenarios
- Position resolution logic
- **Comment insertion accuracy and formatting**
- **Integration between form replacement and comment insertion**

### Integration Tests
- End-to-end form replacement workflows
- MCP protocol compliance
- VS Code API integration
- Rich comment form handling
- **Combined comment + form editing workflows**
- **Comment positioning and spacing validation**

### Interactive Testing
- Use tool to improve its own implementation
- Test with various Clojure code patterns
- Validate with real-world codebases
- **Test comment insertion in various file contexts**

## Future Enhancements

### Advanced Form Operations
- Form extraction and movement
- Multi-form batch operations
- Structural refactoring support

### AI-Specific Features
- Form suggestion based on context
- Pattern-based form generation
- Integration with code completion

### Developer Experience
- Visual feedback for form boundaries
- Real-time validation feedback

---

This form-aware editing toolset represents a significant step toward making AI agents truly effective at Clojure development by respecting the language's fundamental form-based nature while providing complementary tools for non-structural text (comments). The combination of `replace_top_level_form` for structural edits and `insert_comment_at_line` for documentation creates a complete editing solution that leverages Calva's powerful structural editing capabilities, both using the proven line-based approach that AI agents can handle effectively.
