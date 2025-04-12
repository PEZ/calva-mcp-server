(ns vsc-et.fs
  (:require ["vscode" :as vscode]
            [vsc-et.extension.db :as db]
            [promesa.core :as p]))

(defn- ws-uri [path-or-uri]
  (p/let [^js uri (if (= (type "")
                         (type path-or-uri))
                    (vscode/Uri.file path-or-uri)
                    path-or-uri)]
    (vscode/Uri.joinPath (-> @db/!app-db
                             :workspace/root-path
                             :uri)
                         (.-fsPath uri))))

(defn path-or-uri-exists?+ [path-or-uri]
  (-> (p/let [^js uri (ws-uri path-or-uri)
              _stat (vscode/workspace.fs.stat uri)])
      (p/handle
       (fn [_r, e]
         (if e
           false
           true)))))

(defn vscode-read-uri+ [^js uri-or-path]
  (p/let [^js uri (ws-uri uri-or-path)]
    (-> (p/let [_ (vscode/workspace.fs.stat uri)
                data (vscode/workspace.fs.readFile uri)
                decoder (js/TextDecoder. "utf-8")
                text (.decode decoder data)]
          text))))

(defn extension-path [app-db]
  (-> ^js (:extension/context app-db)
      (.-extensionPath)))