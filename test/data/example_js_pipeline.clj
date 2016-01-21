(def js-parse-int (js-fn-timeout "function(a) { return parseInt(a); }"))

(def js-add (js-fn-timeout "function(a,b) { return a + b; }"))

(def base-domain (prefixer "http://my-domain.com"))

(def base-graph (prefixer (base-domain "/graph/")))

(def base-id (prefixer (base-domain "/id/")))

(def base-vocab (prefixer (base-domain "/def/")))

(def base-data (prefixer (base-domain "/data/")))

(def make-graph
  (graph-fn [{:keys [a b sum]}]
            (graph (base-graph "example")
                   ["http://test.com/formula"
                    ["http://test.com/argument/a" a]
                    ["http://test.com/argument/b" b]
                    ["http://test.com/argument/sum" sum]])))

(defpipe my-pipe
  "Pipeline to convert tabular persons data into a different tabular format."
  [data-file]
  (-> (read-dataset data-file :format :csv)
      (drop-rows 1)
      (make-dataset [:a :b])
      (mapc {:a js-parse-int :b js-parse-int})
      (derive-column :sum [:a :b] js-add)))

(defgraft my-graft
  "Pipeline to convert the tabular persons data sheet into graph data."
  my-pipe make-graph)
