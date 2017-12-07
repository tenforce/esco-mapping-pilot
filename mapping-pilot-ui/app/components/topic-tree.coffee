`import Ember from 'ember'`

TopicTreeComponent = Ember.Component.extend
  init: ->
    @_super(arguments...)
    @set 'selected', @get('selection').contains(@get('topic.id'))
  classNames: ['topic-tree']
  loading: false
  loadingObserver: Ember.observer 'topics', ->
    @set 'loading', false
    @get('topics')?.then =>
      @set 'loading', true
  open: false
  topics: Ember.computed.oneWay 'topic.topics'
  selected: false
  hasTopics: Ember.computed.notEmpty 'topics'
  selectedObserver: Ember.observer 'selected', ->
    id = @get('topic.id')
    selected = @get 'selected'
    selection = @get 'selection'
    if selected and not selection.contains id
      selection.addObject id
    else if not selected
      selection.removeObject id 
  actions:
    toggleOpen: ->
      @toggleProperty('open')
    

`export default TopicTreeComponent`
