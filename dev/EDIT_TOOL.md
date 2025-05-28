# Form-Aware Edit Tool - Calva Backseat Driver

## Overview

Form-aware editing tools that leverage Calva's ranges API for semantic Clojure editing. Enables AI agents to edit Clojure code by operating on forms rather than lines, with automatic bracket balancing and structural awareness.

## Status

| Tool | VS Code | MCP | Description |
|------|---------|-----|-------------|
| `replace_top_level_form` | ✅ | ❌ | Replace structural forms with text targeting |
| `insert_top_level_comment` | ❌ | ❌ | Insert top-level comments (non-structural) |

**Key Limitation**: Top-level comments exist outside the form paradigm and need dedicated tooling.

## Tool APIs

Both tools use line-based positioning with text targeting for accuracy:

### `replace_top_level_form`
```clojure
(defn apply-form-edit-by-line-with-text-targeting
  [file-path line-number target-line-text new-form])
```

### `insert_top_level_comment` (Needed)
```clojure
(defn insert-comment-at-line
  [file-path line-number target-line-text comment-text ])
```

**Common Parameters:**
- `file-path`: Absolute path to Clojure file
- `line-number`: Line number (1-indexed) to identify target
- `target-line-text`: Exact text content for validation (searches ±2 lines)
- `new-form`/`comment-text`: Replacement/insertion content

**Common Features:**
- Text targeting with fuzzy line matching (±2 lines)
- Automatic Parinfer bracket balancing (forms only)
- Rich comment form support (forms inside `(comment ...)` treated as top-level)
- Post-edit diagnostics


## Usage Examples

### Basic Usage
```clojure
;; Replace form
replace_top_level_form({
  filePath: "/path/to/file.clj",
  line: 23,
  targetLine: "(defn old-function [x]",
  newForm: "(defn new-function [x y] (+ x y))"
})

;; Insert top-level comment
insert_top_level_comment({
  filePath: "/path/to/file.clj",
  lineNumber: 45,
  commentText: ";; Helper functions for data processing",
  targetLine: "(defn process-data"
})
```

### Error Handling
```clojure
// When target text is not found
{
  success: false,
  error: "Target line text not found. Expected: '(defn wrong-function [x]' near line 23"
}
```


## Security & Testing

TODO!

**Security**: Validate file paths, check permissions, sanitize input, respect REPL security model.

**Testing**: Unit tests for accuracy/error handling, integration tests for workflows, interactive testing with real codebases.

---

This toolset enables AI agents to edit Clojure code effectively by respecting the language's form-based nature while providing complementary tools for non-structural top-level comments.
