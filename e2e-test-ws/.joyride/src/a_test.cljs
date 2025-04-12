(ns a-test
  (:require [cljs.test :refer [deftest testing is]]))

(deftest hello
  (testing "Test files can be put anywhere in the `src` directory. They just need to end in `_test.cljs`."
    (is (= :foo :foo))))