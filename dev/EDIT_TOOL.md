# Form-Aware Edit Tool Design - Calva Backseat Driver

## Overview

This document outlines the design for a form-aware editing tool that leverages Calva's ranges API to provide semantic Clojure editing capabilities for AI agents. The tool addresses the limitation of line-based editing (like `f1e_edit_file`) which doesn't align with Clojure's form-based paradigm.

**Core Value Proposition**: Enable AI agents to edit Clojure code semantically by operating on forms rather than lines, providing automatic bracket balancing, proper formatting, and structural awareness.

## Problem Statement

### Current State: Line-Based Editing
- Existing MCP/LSP tools like `f1e_edit_file` operate on line ranges
- Risk of malformed code after edits
- Clojure code is form-based, not line-based
- No semantic understanding of code structure

### Target State: Form-Aware Editing
- Edit complete forms as semantic units
- Automatic bracket balancing via Parinfer integration
- Support for insertion operations at meaningful locations
- Rich comment form support for AI experimentation
- Leverages Calva's existing ranges and editor infrastructure

## Design Specifications

### Core API: `replace_top_level_form`

```clojure
;; Primary tool interface
(defn replace-top-level-form
  "Replace a top-level form in a Clojure file with new code.
   Automatically handles bracket balancing and formatting."
  [{:keys [file-path position new-form]}])
```

**Parameters:**
- `file-path` (string): Absolute path to the Clojure file
- `position` (integer): Character offset within the file to identify the target form
- `new-form` (string): The replacement form code

**Returns:**
- Success: `{:success true :replaced-form "..." :diagnostics [...]}`
- Error: `{:success false :error "..." :details {...}}`

### Insertion API: `insert_text`

```clojure
;; Primary tool interface
(defn insert-text
  "Insert clojure code at a position.
   Automatically handles bracket balancing and formatting."
  [{:keys [file-path position text]}])
```

Same params and returns as `replacde-top-level-form`

### Implementation Architecture

#### Core Functions (in `api.cljs`)

1. **Form Detection** (Existing)
   ```clojure
   (defn- get-ranges-form-data [file-path position ranges-fn-key])
   (defn- get-range-and-form [{:keys [ranges-object vscode-document]}])
   ```

2. **Edit Operations** (Existing)
   ```clojure
   (defn edit-replace-range [file-path vscode-range new-text])
   ```

3. **New Integration Functions** (To Implement)
   ```clojure
   (defn replace-top-level-form [file-path position new-form])
   (defn insert-text [file-path insertion-point text])
   (defn validate-form-syntax [form-string])
   ```

#### Tool Registration

**MCP Tool Definition:**
```clojure
{:name "replace_top_level_form"
 :description "Replace a top-level form in a Clojure file with semantic awareness"
 :inputSchema {:type "object"
               :properties {:file-path {:type "string"}
                           :position {:type "integer"}
                           :new-form {:type "string"}}
               :required ["file-path" "position" "new-form"]}}
```

TODO: inert_text tool

**VS Code Language Model Tool:**
```clojure
{:name "replace_top_level_form"
 :description "Replace a top-level Clojure form with automatic bracket balancing"
 :parametersSchema vscode/LanguageModelToolParametersSchema}
```

TODO: inert_text tool

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
(defn apply-form-edit [file-path position new-form]
  (p/let [form-data (get-ranges-form-data file-path position :currentTopLevelForm)
          balanced-form (balance-brackets new-form)  ; Existing parinfer integration
          editor (get-editor-from-file-path file-path)]
    (edit-replace-range file-path
                       (first (:ranges-object form-data))
                       balanced-form)))
```

**Note**: Using Calva's editor API (`edit-replace-range`) automatically provides undo/redo integration through VS Code's standard edit operations.

#### Post-Edit Diagnostics Integration
```clojure
(defn get-file-diagnostics [file-path]
  "Get current diagnostics (linting errors, type errors, etc.) for a file."
  (let [uri (vscode/Uri.file file-path)
        diagnostics (vscode/languages.getDiagnostics uri)]
    (map (fn [diagnostic]
           {:message (.-message diagnostic)
            :severity (.-severity diagnostic)  ; 0=Error, 1=Warning, 2=Info, 3=Hint
            :range {:start {:line (.-line (.-start (.-range diagnostic)))
                           :character (.-character (.-start (.-range diagnostic)))}
                    :end {:line (.-line (.-end (.-range diagnostic)))
                         :character (.-character (.-end (.-range diagnostic)))}}
            :source (.-source diagnostic)})
         diagnostics)))

(defn apply-form-edit-with-diagnostics [file-path position new-form]
  (p/let [edit-result (apply-form-edit file-path position new-form)
          ;; Brief delay to allow language servers to process the change
          _ (js/setTimeout (fn []) 100)
          diagnostics (get-file-diagnostics file-path)]
    (merge edit-result
           {:diagnostics diagnostics)})))
```

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

**Note on Form Targeting**: Calva's ranges API will almost always identify a valid form at any given position. However, whether it's the form the AI intended to target cannot be programmatically validated. The AI must ensure it provides accurate position coordinates that correspond to the desired form.

### Rich Comment Form Support

Provide the tool description for `replace_top_level_form` with information so that the AI correctly uses it inside rich comment forms (where Calva will treat everything inside a `(comment ...)` as being top level.)

## Integration Points

### VS Code Language Model Integration
- Add tool to `src/calva_backseat_driver/integrations/vscode/tools.cljs`
- Register with Language Model API
- Follow existing tool registration pattern

### MCP Server Integration
- Add tool definition to `src/calva_backseat_driver/mcp/requests.cljs`
- Implement request handler following existing pattern (note that this pattern includes reading the tool descriptions from the manifest)
- Add to tool registry in server initialization

### Action/Effect System Integration
```clojure
;; New action for form editing
[:calva/ax.replace-form {:file-path "..." :position 123 :new-form "..."}]

;; Corresponding effect
[:calva/fx.replace-form context]
```

## Usage Examples

### Basic Form Replacement with Diagnostics
```clojure
;; Replace a function definition
replace_top_level_form({
  file_path: "/path/to/file.clj",
  position: 450,  // Character offset within the function
  new_form: "(defn new-function [x y] (+ x y))"
})

// Response includes diagnostics feedback
{
  success: true,
  form_range: [445, 478],
  replaced_form: "(defn old-function [a] a)",
  diagnostics: [],
}
```

### Rich Comment Experimentation
```clojure
;; Add experiment to rich comment
replace_top_level_form({
  file_path: "/path/to/file.clj",
  position: 800  // Character offset within the form to replace,
  new_form: "(def test-data [1 2 3])"
})
```

### Context-Aware Insertion
```clojure
;; Insert at current selection
replace_form({
  file_path: "/path/to/file.clj",
  position: 210,
  new_form: "(def new-var 42)\n"
})
```

## Implementation Phases

### Phase 1: Core Implementation
1. Implement `replace-top-level-form` function
2. Add VS Code Language Model tool + MCP tool registration
3. Integrate bracket balancing
4. Add error handling and validation

### Phase 2: Enhanced Features
1. Add insertion operations with special form key (`:selection`)
2. Implement rich comment form support
3. Enhance error reporting and diagnostics
4. **Post-edit diagnostics feedback** - Include linting errors and problems in tool response

### Phase 3: Advanced Capabilities
1. Batch operations support
2. Form-aware refactoring operations
3. Integration with existing ranges API extensions
4. Performance optimizations for large files

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

### Integration Tests
- End-to-end form replacement workflows
- MCP protocol compliance
- VS Code API integration
- Rich comment form handling

### Interactive Testing
- Use tool to improve its own implementation
- Test with various Clojure code patterns
- Validate with real-world codebases

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

This form-aware editing tool represents a significant step toward making AI agents truly effective at Clojure development by respecting the language's fundamental form-based nature while leveraging Calva's powerful structural editing capabilities.
