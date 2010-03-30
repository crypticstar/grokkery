(ns grokkery
  (:use
    grokkery.util
    grokkery.core)
  (:import
    [javax.media.opengl GL]))


(def default-point-size 7)


(def default-point-color [0.84 0.14 0.03 1])


(defn scatter-point-drawfn [#^GL gl x-coordfn y-coordfn attrs]
  (if-let [colorfn (:point-colorfn attrs)]
    #(do
       (gl-set-color gl (colorfn %))
       (.glVertex2f gl (x-coordfn %) (y-coordfn %)))
    #(.glVertex2f gl (x-coordfn %) (y-coordfn %))))


(defn draw-scatter [#^GL gl data x-coordfn y-coordfn attrs]
  (.glPointSize gl (or (:point-size attrs) default-point-size))
  (gl-set-color gl (or (:point-color attrs) default-point-color))
  (gl-draw gl GL/GL_POINTS
    (dorun
      (map (scatter-point-drawfn gl x-coordfn y-coordfn attrs) data))))


(defn scatter-plot [fignum data]
  (add-plot fignum data {:x first, :y second} draw-scatter {}))