`import Ember from 'ember'`

ApplicationController = Ember.Controller.extend
  init: ->
    @_super(arguments)
    Ember.$("html").on "keypress", (event) =>
      if event.target.nodeName.toLowerCase() != "input"
        event.preventDefault()
        @send 'baseEvent', event
  title: "Mapping Tool"
  routeClass: Ember.computed 'currentRouteName', ->
    "route-"+@get('currentRouteName').replace(/\./g, "-")
  actions:
    logout: ->
      @get('user').logout()
      @set 'showSettings', false
      @transitionToRoute 'login'

`export default ApplicationController`
