(ns markdowneditor.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            ["showdown" :as showdown]))

(defn copy-to-clipboard [text]
  (let [el (.createElement js/document "textarea")
        selected (when (pos? (-> js/document .getSelection .-rangeCount))
                   (-> js/document .getSelection (.getRangeAt 0)))]
    (set! (-> el .-value) text)
    (.setAttribute el "readonly" "")
    (set! (-> el .-style .-position) "absolute")
    (set! (-> el .-style .-left) "-9999px")
    (-> js/document .-body (.appendChild el))

    (.select el)
    (.execCommand js/document "copy")
    (-> js/document .-body (.removeChild el))
    (if (nil? selected)
      (-> js/document .getSelection .removeAllRanges)
      (-> js/document .getSelection (.addRange selected)))))

(defonce editor-state (r/atom {:format :md
                               :value ""}))

(defonce flash-messages (r/atom {}))

(defonce showdown-converter (showdown/Converter.))

(defn add-message [text]
  (let [id (str (random-uuid))]
    (swap! flash-messages assoc id
           {:id id
            :text text
            :state :adding})))

(defn show-message [msg]
  (assoc msg :state :showing))

(defn hide-message [msg]
  (assoc msg :state :hiding))

(defn md->html [md]
  (.makeHtml showdown-converter md))

(defn html->md [html]
  (.makeMarkdown showdown-converter html))

(defn ->html [state]
  (case (:format state)
    :html (:value state)
    :md (md->html (:value state))))

(defn ->md [state]
  (case (:format state)
    :md (:value state)
    :html (html->md (:value state))))

(defn message [msg]
  (when (= (:state msg) :adding)
    (js/setTimeout
     #(swap! flash-messages update (:id msg) show-message)
     0))
  (fn [msg]
    [:div {:on-click #(swap! flash-messages update (:id msg) hide-message)
           :style {:background-color "#c0ff01"
                   :padding "5px 10px"
                   :cursor :pointer
                   :border-left "2px solid #01ffc4"
                   :transform (case (:state msg)
                                :adding "translateX(100px)"
                                :hiding "translateX(100px)"
                                "translateX(0)")
                   :transition "transform .3s cubic-bezier(0.18, 0.89, 0.32, 1.28)"}
           :on-transition-end (fn [e]
                                (when (= (:state msg) :showing)
                                  (swap! flash-messages assoc-in
                                         [(:id msg) :timeout]
                                         (js/setTimeout
                                          #(swap! flash-messages update (:id msg)
                                                  hide-message)
                                          5000)))

                                (when (= (:state msg) :hiding)
                                  (println (:id msg))
                                  (js/clearTimeout (:timeout msg))
                                  (swap! flash-messages dissoc (:id msg))))}
     (:text msg)]))

(defn messages []
  [:div {:style {
                 :position :absolute
                 :right 0
                 :top 0
                 :gap 5
                 :display :flex
                 :flex-direction :column}}
   (doall
    (for [id (keys @flash-messages)]
      ^{:key id} [message (get @flash-messages id)]))])

(defn md-editor []
  [:div
   [:h2 "MD Editor"]
   [:textarea
    {:on-change (fn [e]
                  (reset! editor-state {:format :md
                                        :value (-> e .-target .-value)}))
     :value (->md @editor-state)
     :style {:resize :none
             :width "100%"
             :min-height "500px"}}]
   [:button {:on-click (fn [e]
                         (add-message "md copied")
                         (copy-to-clipboard (->md @editor-state)))}
    "copy"]])

(defn html-editor []
  [:div
   [:h2 "HTML editor"]
   [:textarea
    {:on-change (fn [e]
                  (reset! editor-state {:format :html
                                        :value (-> e .-target .-value)}))
     :value (->html @editor-state)
     :style {:resize :none
             :width "100%"
             :min-height "500px"}}]])

(defn html-preview []
  [:div
   [:h2 "HTML Preview"]
   [:div {:dangerouslySetInnerHTML {:__html (->html @editor-state)}}]
   [:button {:on-click #(copy-to-clipboard (->html @editor-state))}
    "copy"]])

(defn app []
  [:div {:style {:position :relative}}
   [:h1 "Markdown editor"]
   [messages]
   [:div {:style {:display :flex :gap 10}}
    [:div {:style {:flex 1}}
     [md-editor]]
    [:div {:style {:flex 1}}
     [html-editor]]
    [:div {:style {:flex 1}}
     [html-preview]]]])

(defn mount! []
  (rd/render [app]
            (.getElementById js/document "app")))

(defn main! []
  (mount!))

(defn reload! []
  (mount!))
