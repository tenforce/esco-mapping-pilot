`import Ember from 'ember'`
`import settings from '../utils/mapping-settings'`

MappingProposalComponent = Ember.Component.extend
  tagName: "li"
  classNameBindings: [":collection-item", ":mapping-proposal", "matchtype", 'visible:visible']
  init: ->
    @_super(arguments...)
    @set 'settings', settings
    Ember.run.next =>
      @sendAction 'registerMatch', this
  ###*
  # Whether or not the more advanced, taxonomist only options are visible
  ###
  advanced: true
  target: Ember.computed "mapping.to.content", ->
    to = @get 'mapping.to'

  # List of mapping options, as a list instead of a tree
  allMappingOptions: [
    {
      label: "Accept as Exact Match"
      id: "yes"
      iconClasses: "fa fa-check"
    },
    {
      label: "Reject Match"
      id: "no"
      iconClasses: "fa fa-times"
    },
    {
      label: "This concept is a narrow match of the concept above (it is more specific)"
      id: "narrow"
      extra: true
      iconClasses: ""
    },
    {
      label: "This concept is closely related to the concept above, but not exactly the same"
      id: "close"
      extra: true
      iconClasses: ""
    },
    {
      label: "This concept is a broader match of the concept above (it is more general)"
      id: "broad"
      extra: true
      iconClasses: ""
    }
  ]
  keypress: (event) ->
    char = String.fromCharCode(event.which)
    if char == " "
      @sendAction "doneAllWeCan", null, null, true
    else if char.toLowerCase() == "y"
      @updateMatchType('yes',1)
    else if char.toLowerCase() == "n"
      @updateMatchType('no', 0)

  updateMatchType: (type, score) ->
    @set 'mapping.matchtype', type
    @set 'mapping.score', score
    @requestFlooding()

    @sendAction 'matchTypeChanged', type, score
  matchtype: Ember.computed 'mapping.matchtype', ->
    @get('mapping.matchtype') or 'todo'
  hasDescription: Ember.computed.notEmpty 'description'
  description: Ember.computed 'target.description', ->
    desc = @get 'target.description'
    if desc
      $("<div>#{desc}</div>").text()

  ###*
  # Whether or not the component is visible, this is necessary
  # to put the display of components to none, and improve rendering times
  # by avoiding the entire list of matches to rerender
  ###
  visible: Ember.computed 'matchtype', 'matchVisibilities', ->
    @get('matchVisibilities').contains(@get('matchtype'))
  labels: Ember.computed 'target.labels', ->
    labelsString = @get 'target.labels'
    if not labelsString
      []
    else
      labelsString.split('|')
  enlabels: Ember.computed 'target.enlabels', ->
    labelsString = @get 'target.enlabels'
    if not labelsString
      []
    else
      labelsString.split('|')
  score: Ember.computed 'mapping.score', ->
    @get('mapping.score').toFixed(2)
  baseMappingOptions: Ember.computed "mapping.matchtype", ->
    options = @get 'allMappingOptions'
    result = []
    options.map (option) =>
      option = Ember.merge {}, option
      Ember.set(option, 'selected', (@get('mapping.matchtype') == option.id))
      result.push(option) if not option.extra
    result
  extraMappingOptions: Ember.computed "mapping.matchtype", ->
    options = @get 'allMappingOptions'
    result = []
    options.map (option) =>
      option = Ember.merge {}, option
      Ember.set(option, 'selected', (@get('mapping.matchtype') == option.id))
      result.push(option) if option.extra
    result
  destroy: ->
    @sendAction 'unregisterMatch', this
    @_super(arguments...)
  requestFlooding: ->
    Ember.$.ajax
      url: "/api/flood_scores"
      type: "POST"
  selectMapping: (options) ->
    selection = Ember.get(options,'id')
    current = @get 'matchtype'
    if current == 'todo'
      @sendAction 'handledTodo'
      @sendAction 'doneAllWeCan', this, 'mapping'
    Ember.run.next =>
      newScore = @get('mapping.originalscore')
      if current == selection
        @updateMatchType(null, newScore)
      else
        if @get('settings.matchTypes').contains(selection)
          newScore = 1
        else if selection == "no"
          newScore = 0

        @updateMatchType(selection, newScore)
  actions:
    selectMapping: (option) ->
      @selectMapping(option)
    
`export default MappingProposalComponent`
