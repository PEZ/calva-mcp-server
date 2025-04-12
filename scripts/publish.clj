(ns publish
  (:require [clojure.string :as string]
            [cheshire.core :as json]
            util))

(def changelog-filename "CHANGELOG.md")

(defn release-pattern [version]
  (re-pattern (str "## \\[" (string/replace version "." "\\.") "\\].*")))

(defn get-changelog-text-for-version [version]
  (let [pattern (release-pattern version)]
    (some-> (slurp changelog-filename)
            (string/split pattern)
            second
            (string/split #"##")
            first
            string/trim)))

(comment
  (get-changelog-text-for-version "v1.1.12")
  :rcf)

(defn new-changelog-text
  [changelog-text version]
  (let [utc-date (-> (java.time.Instant/now)
                     .toString
                     (clojure.string/split #"T")
                     (nth 0))
        new-header (format "## [v%s] - %s" version utc-date)
        new-text (string/replace-first
                  changelog-text
                  (release-pattern "Unreleased")
                  (format "## [Unreleased]\n\n%s" new-header))]
    new-text))

(defn commit-changelog [file-name message dry-run?]
  (println "Committing")
  (util/shell dry-run? "git" "add" file-name)
  (util/shell dry-run?
              "git" "commit"
              "-m" message
              "-o" file-name))

(defn tag [version dry-run?]
  (println "Tagging with version" version)
  (util/shell dry-run?
              "git" "tag"
              "-a" (str "v" version)
              "-m" (str "Version " version)))

(defn push [dry-run?]
  (println "Pushing")
  (util/shell dry-run? "git" "push" "--follow-tags"))

(defn tag-and-push! [version dry-run?]
  (tag version dry-run?)
  (push dry-run?)
  (println "Open to follow the progress of the release:")
  (println "  https://github.com/PEZ/vsc-et/actions"))

(defn publish! [dry-run?]
  (let [unreleased-changelog-text (get-changelog-text-for-version "Unreleased")
        changelog-text (slurp changelog-filename)
        extension-version (-> (slurp "package.json")
                              json/parse-string
                              (get "version"))]
    (if (empty? unreleased-changelog-text)
      (do
        (println "Publishing without updating the changelog.")
        (tag-and-push! extension-version dry-run?))
      (let [updated-changelog-text (new-changelog-text changelog-text extension-version)]
        (println "Updating changelog")
        (if-not dry-run?
          (spit changelog-filename updated-changelog-text)
          (println "Would write to changelog: " changelog-filename (subs updated-changelog-text 0 200) "..."))
        (commit-changelog changelog-filename
                          (str "Add changelog section for v" extension-version)
                          dry-run?)
        (tag-and-push! extension-version dry-run?)))))

(defn- get-git-situation []
  (println "Checking git status")
  (let [git-status (:out (util/sh false "git" "status"))
        [_ branch] (re-find #"^On branch (\S+)\n" git-status)
        up-to-date (re-find #"Your branch is up to date" git-status)
        clean (re-find #"nothing to commit, working tree clean" git-status)]
    (cond-> #{}
      (not= "master" branch) (conj :not-on-master)
      (not up-to-date) (conj :not-up-to-date)
      (not clean) (conj :branch-not-clean))))

(defn- collect-situation []
  (let [unreleased-changelog-text (get-changelog-text-for-version "Unreleased")
        git-situation (get-git-situation)]
    (if (seq unreleased-changelog-text)
      (do
        (println "Unreleased changelog entry:\n" unreleased-changelog-text)
        git-situation)
      (do (println "There are no unreleased changes in the changelog.")
          (conj git-situation :no-unreleased-changelog)))))

(defn yolo! [{:keys [dry]}]
  (println "dry-run?" dry)
  (let [situation (collect-situation)]
    (if (seq situation)
      (do
        (println "These issues with publishing were found:" situation)
        (println "Release anyway? YES/NO: ")
        (flush)
        (let [answer (str (read))]
          (if-not (= "YES" answer)
            (println "Aborting publish.")
            (publish! dry))))
      (publish! dry))))
