# Short term (days) todos

- [ ] **Fix form edit tool diagnostic timing issues**:
  - [ ] Add proper delay/polling for language server diagnostic updates after edits
  - [ ] Return both `:diagnostics-before-edit` and `:diagnostics-after-edit`
  - [ ] Filter diagnostics to only show clj-kondo sources (exclude other language servers)
- [ ] Figure if the protocol supports asking for permission with each tool invocation
  - [ ] Can we make the Agent show what it is going to evaluate before it does it?
- [ ] Refactor server.cljs and wrapper.cljs and clean up debugging
- [ ] Update e2e tests for the server
- [ ] Cover the server with some unit tests
- [ ] Cover the wrapper with some unit tests
- [ ] Remove the Hello commands and their tests
- [ ] Implement more of the Calva evaluate API (session selection, output and error callbacks)
- [ ] Investigate auto-creating the server config for the user
- [ ] Stabilize and harden server start/stop