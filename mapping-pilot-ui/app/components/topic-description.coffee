`import Ember from 'ember'`

TopicDescriptionComponent = Ember.Component.extend
  hasDescription: Ember.computed.notEmpty 'description'
  infoTitle: Ember.computed 'matchesTodo', 'matchesPos', 'matchesNeg', ->
    todo = @get 'matchesTodo'
    pos = @get 'matchesPos'
    neg = @get 'matchesNeg'

    todotext = if todo > 0 then "still #{todo}" else "no more"
    "There are #{todotext} matches to be decided upon, there are #{pos} confirmed and #{neg} rejected matches"
  description: Ember.computed 'topic.description', 'labels', ->
    desc = @get 'topic.description'
    labels = @get('labels')
    if desc
      $("<div>#{desc}</div>").text()
    else if not labels or labels.length == 0
      "Nothing else is known about this concept..."
  labels: Ember.computed 'topic.labels', ->
    labelsString = @get('topic.labels')
    if not labelsString
      []
    else
      labelsString.split('|')
  enlabels: Ember.computed 'topic.enlabels', ->
    labelsString = @get('topic.enlabels')
    if not labelsString
      []
    else
      labelsString.split('|')
  gotit: Ember.computed 'status', ->
    @get('status') == 'gotit'
  allDone: Ember.computed 'status', ->
    @get('status') == 'alldone'
`export default TopicDescriptionComponent`
