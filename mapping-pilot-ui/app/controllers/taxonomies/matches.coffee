`import Ember from 'ember'`
`import settings from '../../utils/mapping-settings'`

TaxonomiesMatchesController = Ember.Controller.extend
  init: ->
    @_super(arguments...)
    @set 'settings', settings
  visibilitySettings: Ember.computed 'settings.visibilitySettings', 'settings.visibilities', ->
    result = []
    visibilities = @get 'settings.visibilities'
    visibilities = visibilities.match
    result = []
    @get('settings.visibilitySettings').map (item) ->
       result.push(Ember.merge(Ember.Object.create({value: visibilities[item.get('type')]}),item))
    result
  minScore: Ember.computed.alias 'model.minscore'
  actions:
    toggleSetting: (setting) ->
      Ember.set setting, 'value', not Ember.get(setting, 'value')
      return false
    addStopwords: ->
      @set 'showDialog', true
      @send 'closeSettings'
    closeDialog: ->
      @set 'showDialog', false

  matchVisibilities: Ember.computed 'visibilitySettings.@each.value',  ->
    result = []
    @get('visibilitySettings').map (item) ->
      result = result.concat(item.get('matches')) if item.get('value')
    result


`export default TaxonomiesMatchesController`
