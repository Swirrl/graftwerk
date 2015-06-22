
(def make-graph
  (graph-fn [{:keys [foo bar baz]}]))

(defpipe haxorz-teh-syst3mz
  "A pipeline that tries to do something naughty like call System/exit, should
  raise a SecurityException when interpreted in the jail."
  [data-file]
  (System/exit 1))

(defgraft my-graft
  "Pipeline to convert the tabular persons data sheet into graph data."
  haxorz-teh-syst3mz make-graph)
