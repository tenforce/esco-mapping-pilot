`import Ember from 'ember'`
`import promiseAll from '../utils/promise-all'`
`import settings from '../utils/mapping-settings'`

TopicMatchTreeComponent = Ember.Component.extend
  classNameBindings: [':topic-tree', ':topic-match-tree', "gotit:gotit", "open:open", "allDone:allDone"]
  init: ->
    @_super(arguments...)
    @set 'matches', Ember.ArrayProxy.create({content: []})
    @set 'settings', settings
  ###*
  # Minimum score a mapping needs before being shown, if <=0 show all mappings
  ###
  open:false
  ###*
  # Whether or not the more advanced taxonomy features are displayed
  ###
  advanced: false
  ###*
  # List of matchtypes to show to the user
  ###
  settings: null
  matchVisibilities: []
  loading: true
  keypress: (event) ->
    char = String.fromCharCode(event.which)
    if char == " "
      if @get('status') == 'todo'
        @clickedGotit()
      else
        @handleDoneAllWeCan(null,null,true)
    event.stopPropagation()
  loadingObserver: Ember.observer 'topics', 'topic.mappingsFrom', ->
    topics = @get 'topics'
    mappings = @get 'topic.mappingsFrom'
    if not (topics and (mappings or not @get('topic.relationships.mappingsFrom')))
      return
    promises = [Ember.get(topics, 'promise')]
    if mappings
      promises.push Ember.get(mappings, 'promise')
    promiseAll(promises).then =>
      try
        @set 'loading', false
        Ember.run.next =>
          if @get('open') and @get('shouldOpenNext')
            @set 'shouldOpenNext', false
            @openNextTopic()
      catch
        #object was destroyed
  openObserver: Ember.observer 'open', ( ->
    if @get 'open'
      # make sure the children are fetched
      @notifyPropertyChange 'topics'
      @notifyPropertyChange 'topic.mappingsFrom'
      @notifyPropertyChange 'mappings'
      @sendAction 'uiTargetChanged', this

  ).on('init')
  hasContent: Ember.computed "hasMappings", "topics.content", ->
    @get('hasMappings') or @get('topics.length') > 0
  noContent: Ember.computed.not 'hasContent'
  allDoneObserver: Ember.observer 'noContent', (->
    if @get 'noContent'
      @set 'status', 'alldone'
    else if (@get('status') == 'alldone') and not @get 'gotitClicked'
      @set 'status', 'todo'
    else if @get 'gotitClicked'
      @set 'status', 'gotit'
  ).on('init')
  hasMappings: Ember.computed.notEmpty "visibleMappings"
  mappings: Ember.computed 'topic.mappingsFrom.content', 'minScore', ->
    mappings = @get('topic.mappingsFrom.content') or []
    mappings = mappings.concat([])
    mappings.sort (a,b) ->
      Ember.get(b, 'originalscore') - Ember.get(a, 'originalscore')
    minScore = @get 'minScore'
    if minScore > 0
      mappings.filter (item) ->
        Math.max(Ember.get(item, 'originalscore'),Ember.get(item, 'score')) > minScore
    else
      mappings
  topics: Ember.computed.oneWay 'topic.topics'
  code: Ember.computed 'topic.code', 'index', 'parentCode', ->
    code = @get 'topic.code'
    if code
      return code

    index = @get('index') + 1
    parentCode = @get 'parentCode'

    if isNaN(index)
      index = null

    code ||= index

    if parentCode
      code = "#{parentCode} #{code}."
    else if code
      code = "#{code}."
  matchesObserver: Ember.observer 'mappings.@each.matchtype', 'minScore', 'matchVisibilities', (->
    visibilities = @get 'matchVisibilities'
    score = @get 'minScore'
    visible = []
    todo = []
    pos = []
    neg = []
    matchTypes = @get('settings.matchTypes') or []

    @get('mappings').map (mapping) ->
      type = Ember.get(mapping,'matchtype') or "todo"
      if matchTypes.contains(type)
        pos.push mapping
      else if type == "no"
        neg.push mapping
      else
        todo.push mapping

      if visibilities.contains type
        visible.push mapping
    @set 'matchesTodo', todo.length
    @set 'matchesPos', pos.length
    @set 'matchesNeg', neg.length
    @set 'visibleMappings', visible
  )
  selectNextChild: (current) ->
    childViews = @get('childViews')
    index = childViews.indexOf(current) + 1
    next = childViews[index]
  selectFirstMatch: ->
    matches = @get('matches') or []
    matches.sortBy 'index'
    filtered = matches.filter (item) ->
      item.get('visible')
    filtered.objectAt(0)
  selectNextMatch: (current) ->
    matches = @get 'matches'
    index = matches.indexOf current
    matches.objectAt index+1
  openNextTopic: (current) ->
    current?.set('open',false)
    current?.set('allDone', true)

    next = @selectNextChild(current)

    if next
      next.set('shouldOpenNext', true)
      next.set('open',true)
      next.sendAction 'uiTargetChanged', next
      Ember.run.later (->
        target = next.$()
        $('html, body').animate({
          scrollTop: target.offset().top - $(window).height()/3
        }, 300);
      ), 300
      next
    else
      false
  clickedGotit: ->
    @set 'status', 'gotit'
    @set 'gotitClicked', true
    @sendAction 'handledTodo'
    Ember.run.next =>
      next = @selectNextMatch()
      if next
        @sendAction 'uiTargetChanged', next
  updateMatchSelection: ->
    first = @selectFirstMatch()
    @sendAction 'uiTargetChanged', (first or this)
  handleDoneAllWeCan: (what,kind,force)->
    Ember.run.next =>
      if kind == 'matchTree'
        opened = @openNextTopic(what)
        @sendAction('doneAllWeCan', this, 'matchTree') if not opened
      else if @get('matchesTodo') <= 1 or force
        @sendAction('doneAllWeCan', this, 'matchTree')
  addMapping: (itemId) ->
    currentmappings = @get 'topic.mappingsFrom'
    found = false
    currentmappings.map (item) ->
      if item.get('to.id') == itemId
        found = true
    if found
      return
    store = @get 'store'
    newMapping = this.container.lookup('model:mappings').create
      isNew: true
      attributes:
        matchtype: "yes"
        score: 1
        originalscore: 1
    newMapping.addRelationship('from', @get('topic.id'))
    newMapping.addRelationship('to', itemId)
#    @get('topic.mappingsFrom').pushObject(newMapping)
    store.createResource 'mappings', newMapping
  status: 'todo'
  gotit: Ember.computed 'status', ->
    @get('status') == 'gotit'
  actions:
    uiTargetChanged: (target) ->
      @sendAction 'uiTargetChanged', target
    toggleOpen: ->
      @set 'open', not @get('open')
    gotit: ->
      @clickedGotit()
    handledTodo: ->
      @sendAction 'handledTodo'
    matchTypeChanged: ->
      @updateMatchSelection()
    doneAllWeCan: (what, kind, force) ->
      @handleDoneAllWeCan(what, kind, force)
    registerMatch: (match) ->
      @get('matches').pushObject(match)
      Ember.run.debounce this, @updateMatchSelection, 200
    unregisterMatch: (match) ->
      @get('matches').removeObject(match)
      Ember.run.debounce this, @updateMatchSelection, 200
    selectedMappings: (selection) ->
      if selection.length <=0
        @set 'showPopup', false
        return
      promises = []
      selection.map (item) =>
        promises.push(@addMapping(item))
      @set 'showPopup', false
      promiseAll(promises).then =>
        @get('store').find('topics', @get('topic.id')).then (result) =>
          @set 'topic', result
    addMapping: ->
      @set 'showPopup', true

`export default TopicMatchTreeComponent`
