`import Ember from 'ember'`

ApplicationRoute = Ember.Route.extend
  currentUITarget: null
  actions:
    updateTitle: (title) ->
      @set 'controller.title', title
    baseEvent: (event) ->
      @get('currentUITarget')?[event.type]?(event)
    uiTargetChanged: (target) ->
      @set 'currentUITarget', target
    toggleSettings: ->
      current = @get('controller.showSettings')
      Ember.run.next =>
        @set 'controller.showSettings', not current
        return false
    closeSettings: ->
      @set 'controller.showSettings', false


`export default ApplicationRoute`
