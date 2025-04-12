const vscode = require('vscode');

exports.run = async () => {
  return vscode.commands.executeCommand(
    'joyride.runCode',
    "(require '[e2e.test-runner :as runner]) (runner/run-all-tests \".joyride/src\")"
  );
};
