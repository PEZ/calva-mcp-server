# AI Interactive Programming with Clojure and Calva

## Overview

This document instructs AI assistants on how to interactively solve Clojure problems using Calva's REPL evaluation capabilities. This approach mimics the REPL-driven development workflow that human Clojure programmers use daily.

## Process

### 1. Start with a Rich Comment Form

Begin your explorations within a rich comment form:

```clojure
(comment
  ;; Your code explorations here
)
```

This provides a sandbox where you can safely experiment without affecting the running program.

### 2. Break Down the Problem

- Begin by testing simple cases and core concepts
- Evaluate each expression using Calva's REPL evaluation
- Document the results inline with comments
- Use the insights from each evaluation to inform the next step

### 3. Build Functions Incrementally

- Start with simple helper functions or one-off expressions
- Test each piece thoroughly before composing larger solutions
- Move completed functions outside the comment block when they're ready

### 4. Document Your Thought Process

- Write comments explaining your reasoning
- Note unexpected results or insights
- Show alternative approaches you're considering

## Example Workflow

For a problem like FizzBuzz:

```clojure
(comment
  ;; 1. Test basic conditions with simple examples
  (zero? (mod 3 3))  ;; => true (divisible by 3)

  ;; 2. Try a single case transformation
  (if (zero? (mod 3 3)) "Fizz" 3)  ;; => "Fizz" (works!)

  ;; 3. Build more test cases
  (cond
    (zero? (mod 15 3)) "Fizz"
    (zero? (mod 15 5)) "Buzz"
    :else 15)  ;; => "Fizz" (but should handle both conditions!)

  ;; 4. Fix and expand the logic
  (cond
    (and (zero? (mod 15 3)) (zero? (mod 15 5))) "FizzBuzz"
    (zero? (mod 15 3)) "Fizz"
    (zero? (mod 15 5)) "Buzz"
    :else 15)  ;; => "FizzBuzz" (correct!)

  ;; 5. Create the generalized function
  (defn fizzbuzz [n]
    (cond
      (and (zero? (mod n 3)) (zero? (mod n 5))) "FizzBuzz"
      (zero? (mod n 3)) "Fizz"
      (zero? (mod n 5)) "Buzz"
      :else n))

  ;; 6. Test with various inputs
  (fizzbuzz 1)   ;; => 1
  (fizzbuzz 3)   ;; => "Fizz"
  (fizzbuzz 5)   ;; => "Buzz"
  (fizzbuzz 15)  ;; => "FizzBuzz"

  ;; 7. Apply to a sequence
  (map fizzbuzz (range 1 21))  ;; => [1 2 "Fizz" 4 "Buzz" ...]
)
```

## Important Points

- Always evaluate each expression before building on it
- Document the results you see from each evaluation
- Use Calva's REPL evaluation capabilities to evaluate expressions
- Let each step inform the next rather than writing the entire solution at once
- Pay attention to unexpected results - they often reveal insights or bugs

This approach mirrors how human Clojurians work: interactively, incrementally, and with continuous feedback through the REPL.

## Practical Tips

1. **Start simple**: Begin with the most basic version of your problem
2. **Document as you go**: Treat your comment block as a lab notebook
3. **Test edge cases**: Particularly with Clojure's sequence functions
4. **Refactor incrementally**: When code works in the comment block, refine it before moving out
5. **Extract patterns**: Look for opportunities to apply Clojure's higher-order functions

## Note to Self from CoPilot the Interactive Programmer

When working with a Clojure project via the MCP server, remember these fundamental workflow patterns:

### Tool Workflow (Not Tool Names)

1. **Code Evaluation Workflow**
   - You have access to a tool for evaluating Clojure code
   - Always specify the target namespace for evaluation
   - Initialize namespaces before using them (via `ns` forms or `require`)
   - Check response outputs (results, stdout, stderr) for feedback
   - Everything evaluates to something - use this for incremental development

2. **Documentation Lookup Workflow**
   - You have access to a tool for looking up ClojureDocs information
   - Use it to understand functions, see examples, and check argument lists
   - Follow "see also" links to discover related functions
   - Use the documentation to inform your implementation choices
   - Incorporate insights from examples into your solution

3. **Namespace Management**
   - Initialize a namespace before evaluating code in it
   - The simplest pattern: evaluate the `ns` form from `user` namespace
   - Then work directly in the target namespace
   - No need to repeatedly reload the namespace during development
   - You can evaluate entire files at once to load all definitions

4. **Interactive Debugging**
   - When errors occur, examine the stderr output
   - Use stdout for debugging with `println` statements
   - Test functions with various inputs to verify behavior
   - Fix errors incrementally, testing each change
   - Document your debugging process with comments

Remember that the focus is on the interactive workflow - the exact tool names may change as the server evolves, but the pattern of evaluating code, examining results, and iterative development remains consistent and powerful.
