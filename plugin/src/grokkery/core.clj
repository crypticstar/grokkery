(ns grokkery.core
  (:use
    grokkery.util)
  (:import
    [javax.media.opengl GL]))


(let [figs (ref {})]

  (defn get-fig [fignum]
    (@figs fignum))
  
  
  (defn find-unused-plotnum [fig]
    (first
      (filter
        (complement (set (keys (:plots (get-fig 0)))))
        (iterate inc 0))))
  
  
  (defn add-plot [fignum data coords drawfn attrs]
    (dosync
      (let [plotnum (find-unused-plotnum (get-fig fignum))
            plot {:fig fignum, :plot plotnum, :data data, :coords coords, :drawfn drawfn, :attrs attrs}]
        (alter figs
          assoc-in [fignum :plots plotnum] plot)
        plot)))
  
  
  (defn remove-plot [fignum plotnum]
    (dosync
      (alter figs
        update-in [fignum :plots] dissoc plotnum)))
  
  
  (defn update-plot-field [fignum plotnum key f args]
    (dosync
      (alter figs
        update-in [fignum :plots plotnum key] #(apply f % args))))
  
  
  (defn set-plot-field [fignum plotnum key value]
    (dosync
      (alter figs
        assoc-in [fignum :plots plotnum key] value)))
  
  
  ; Awkward to use directly without varags
  (defn update-attr [fignum plotnum attrkey f args]
    (dosync
      (alter figs
        update-in [fignum :plots plotnum :attrs attrkey] #(apply f % args))))
  
  
  (defn set-attr [fignum plotnum attrkey attr]
    (dosync
      (alter figs
        assoc-in [fignum :plots plotnum :attrs attrkey] attr)))
  
  
  (defn update-axes [fignum f args]
    (dosync
      (alter figs
        update-in [fignum :axes] #(apply f % args))))

  
  (defn update-coordlims [fignum coordkey f args]
    (dosync
      (alter figs
        update-in [fignum :limits coordkey] #(apply f % args))))
  
  
  ; Accept lims for multiple coords (set-coordlims 0 :x [0 1] :y [-2 2])
  (defn set-coordlims [fignum coordkey coordlims]
    (dosync
      (alter figs
        assoc-in [fignum :limits coordkey] coordlims))))




(defn get-plot [fig plotnum]
  (get-in fig [:plots plotnum]))


(defn get-coordlims [fig coordkey]
  (get-in fig [:limits coordkey]))


(defn get-coordkey [fig axiskey]
  (get-in fig [:axes axiskey]))




(defn set-data [fignum plotnum data]
  (set-plot-field fignum plotnum :data data))


(defn update-data [fignum plotnum f & args]
  (update-plot-field fignum plotnum :data f args))


(defn update-coords [fignum plotnum f & args]
  (update-plot-field fignum plotnum :coords f args))


(defn def-coord [fignum plotnum coordkey coordfn]
  (update-coords fignum plotnum assoc coordkey coordfn))


(defn set-drawfn [fignum plotnum drawfn]
  (set-plot-field fignum plotnum :drawfn drawfn))


(defn set-attrs [fignum plotnum attrs]
  (set-plot-field fignum plotnum :attrs attrs))


(defn update-attrs [fignum plotnum f & args]
  (update-plot-field fignum plotnum :attrs f args))


(defn set-axes [fignum bottom-coordkey left-coordkey]
  (update-axes fignum merge {:bottom bottom-coordkey, :left left-coordkey}))




(defn get-valid-limits [limits]
  (if
    (and
      (not-empty limits)
      (< (min-of limits) (max-of limits)))
    limits
    [0 1]))


(defn prep-plot [#^GL gl xlim ylim]
  (doto gl
    (.glMatrixMode GL/GL_PROJECTION)
    (.glLoadIdentity)
    (.glOrtho (min-of xlim) (max-of xlim) (min-of ylim) (max-of ylim) -1 1)
    (.glMatrixMode GL/GL_MODELVIEW)
    (.glLoadIdentity)
    
    (.glEnable GL/GL_BLEND)
    (.glBlendFunc GL/GL_SRC_ALPHA GL/GL_ONE_MINUS_SRC_ALPHA)
    
    (.glEnable GL/GL_POINT_SMOOTH)
    (.glHint GL/GL_POINT_SMOOTH_HINT GL/GL_NICEST)
    
    (.glEnable GL/GL_LINE_SMOOTH)
    (.glHint GL/GL_LINE_SMOOTH_HINT GL/GL_NICEST)))


(defn draw-plot [gl fig plotnum axis-coordkeys]
  (let [plot (get-plot fig plotnum)
        
        x-coordkey (:bottom axis-coordkeys)
        x-coordfn (get (:coords plot) x-coordkey)
        x-limits (get-valid-limits (get-coordlims fig x-coordkey))
        
        y-coordkey (:left axis-coordkeys)
        y-coordfn (get (:coords plot) y-coordkey)
        y-limits (get-valid-limits (get-coordlims fig y-coordkey))
        
        drawfn (:drawfn plot)]
    
    (when (and x-coordfn y-coordfn drawfn)
      (gl-push-all gl
        (prep-plot gl x-limits y-limits)
        (drawfn gl (:data plot) x-coordfn y-coordfn (:attrs plot))))))


(defn draw-plots [gl fignum]
  (when-let [fig (get-fig fignum)]
    (let [default-coordkeys {:bottom :x, :left :y}
          axis-coordkeys (merge default-coordkeys (:axes fig))]
      (dorun
        (map
          #(draw-plot gl fig % axis-coordkeys)
          (keys (:plots fig)))))))
