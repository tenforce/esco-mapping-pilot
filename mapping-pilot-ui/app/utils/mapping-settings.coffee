`import Ember from 'ember'`

MappingSettings = Ember.Object.extend
  init: ->
    @_super(arguments...)
    settings = this
    defaults =
      match:
        showTodo: true
        showPositive: false
        showNegative: false
      manage:
        showTodo: true
        showPositive: true
        showNegative: false
    @set 'visibilities', defaults
    Ember.run.next =>
      @set 'visibilitySettings', Ember.ArrayProxy.create content: [
        Ember.Object.create( {
          label: "Unverified Matches"
          type: 'showTodo'
          matches: ["todo"]
        }), Ember.Object.create( {
          label: "Positive Matches"
          type: 'showPositive'
          matches: ["yes", "broad", "narrow", "close"]
        }), Ember.Object.create( {
          label: "Negative Matches"
          type: 'showNegative'
          matches: ["no"]
        })
      ]
  matchTypes: ["yes", "broad", "narrow", "close"]
  minScoreNumber: Ember.computed 'minScore', ->
    parseFloat(@get('minScore'))
  visibilitySettings: []
  visibilities: {}

MappingSettings.instance = MappingSettings.create()

`export default MappingSettings.instance`
